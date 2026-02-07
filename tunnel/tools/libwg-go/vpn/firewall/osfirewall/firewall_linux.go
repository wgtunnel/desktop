// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 WG Tunnel.
// Adapted from Tailscale

//go:build linux && !android

package osfirewall

import (
	"encoding/binary"
	"errors"
	"fmt"
	"net/netip"
	"reflect"
	"sync/atomic"

	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/google/nftables"
	"github.com/google/nftables/expr"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall/mark"
	"golang.org/x/net/nettest"
	"golang.org/x/sys/unix"
)

const (
	baseChainInput       = "INPUT"
	baseChainOutput      = "OUTPUT"
	baseChainPostrouting = "POSTROUTING"
	baseChainForward     = "FORWARD"
	chainNameForward     = "wgtunnel-forward"
	chainNameInput       = "wgtunnel-input"
	chainNamePostrouting = "wgtunnel-postrouting"
	chainNameOutput      = "wgtunnel-output"
	chainTypeRegular     = ""
)

type LinuxFirewall struct {
	conn *nftables.Conn
	nft4 *nftable // IPv4 tables, never nil
	nft6 *nftable // IPv6 tables or nil if no IPv6 support

	v6Available bool

	tunnelPort uint16

	killSwitchEnabled atomic.Bool
	persistKillSwitch atomic.Bool
	logger            *device.Logger

	localAddrRules []*nftables.Rule            // For tracking AllowedLocalNetworks rules
	tunnelRules    map[string][]*nftables.Rule // For tracking iface tunnel bypass rules
}

func (f *LinuxFirewall) IsPersistent() bool {
	return f.persistKillSwitch.Load()
}

func (f *LinuxFirewall) SetPersist(enabled bool) {
	f.persistKillSwitch.Store(enabled)
}

func New(logger *device.Logger) (firewall.Firewall, error) {
	conn, err := nftables.New()
	if err != nil {
		return nil, fmt.Errorf("nftables connection: %w", err)
	}

	nft4 := &nftable{Proto: nftables.TableFamilyIPv4}

	supportsV6 := nettest.SupportsIPv6()

	var nft6 *nftable
	if supportsV6 {
		nft6 = &nftable{Proto: nftables.TableFamilyIPv6}
	}
	logger.Verbosef("nftables mode, v6 support: %v", supportsV6)

	f := &LinuxFirewall{
		conn:        conn,
		nft4:        nft4,
		nft6:        nft6,
		v6Available: supportsV6,
		logger:      logger,
		tunnelRules: make(map[string][]*nftables.Rule),
	}
	return f, nil
}

func (f *LinuxFirewall) AddTunnelBypasses(iface string) error {
	if !f.IsEnabled() {
		return errors.New("kill switch must be enabled to add tunnel bypasses")
	}

	// remove old rules
	_ = f.RemoveTunnelBypasses(iface)

	var newRules []*nftables.Rule

	for _, table := range f.getTables() {
		outputChain, err := getChainFromTable(f.conn, table.Filter, chainNameOutput)
		inputChain, _ := getChainFromTable(f.conn, table.Filter, chainNameInput)
		if err != nil {
			return fmt.Errorf("get output chain: %w", err)
		}

		// apply tunnel mark
		bootstrapRule := createFwmarkRule(table.Filter, outputChain, mark.LinuxBootstrapMarkNum)
		f.conn.InsertRule(bootstrapRule)
		newRules = append(newRules, bootstrapRule)

		// allow input for DNS boostrap
		stateRule := &nftables.Rule{
			Table: table.Filter,
			Chain: inputChain,
			Exprs: []expr.Any{
				&expr.Ct{Key: expr.CtKeySTATE, Register: 1},
				&expr.Bitwise{
					SourceRegister: 1,
					DestRegister:   1,
					Len:            4,
					Mask:           []byte{0x06, 0x00, 0x00, 0x00}, // ESTABLISHED (2) | RELATED (4)
					Xor:            []byte{0x00, 0x00, 0x00, 0x00},
				},
				&expr.Cmp{
					Op:       expr.CmpOpNeq,
					Register: 1,
					Data:     []byte{0x00, 0x00, 0x00, 0x00},
				},
				&expr.Counter{},
				&expr.Verdict{Kind: expr.VerdictAccept},
			},
		}
		f.conn.InsertRule(stateRule)
		newRules = append(newRules, stateRule)

		// add tunnel interface bypass rule
		tunnelBypassRule := &nftables.Rule{
			Table: table.Filter,
			Chain: outputChain,
			Exprs: []expr.Any{
				&expr.Meta{
					Key:      expr.MetaKeyOIFNAME,
					Register: 1,
				},
				&expr.Cmp{
					Op:       expr.CmpOpEq,
					Register: 1,
					Data:     []byte(iface + "\x00"),
				},
				&expr.Counter{},
				&expr.Verdict{Kind: expr.VerdictAccept},
			},
		}
		existing, _ := findRule(f.conn, tunnelBypassRule)
		if existing == nil {
			f.conn.InsertRule(tunnelBypassRule)
			newRules = append(newRules, tunnelBypassRule)
		}
	}

	if err := f.conn.Flush(); err != nil {
		return fmt.Errorf("flush after adding tunnel bypasses: %w", err)
	}

	if f.tunnelRules == nil {
		f.tunnelRules = make(map[string][]*nftables.Rule)
	}
	f.tunnelRules[iface] = newRules

	f.logger.Verbosef("Added/Updated tunnel bypasses for iface %s", iface)
	return nil
}

func (f *LinuxFirewall) RemoveTunnelBypasses(iface string) error {
	if !f.IsEnabled() {
		f.logger.Verbosef("Firewall is not enabled, skipping")
		return nil
	}

	rules, ok := f.tunnelRules[iface]
	if !ok {
		return nil
	}

	for _, rule := range rules {
		f.conn.DelRule(rule)
	}

	if err := f.conn.Flush(); err != nil {
		return fmt.Errorf("flush after removing tunnel bypasses: %w", err)
	}

	delete(f.tunnelRules, iface)

	f.logger.Verbosef("Removed tunnel bypasses for iface %s", iface)
	return nil
}

func (f *LinuxFirewall) Disable() error {
	if !f.IsEnabled() {
		f.logger.Verbosef("Firewall is not enabled, skipping")
		return nil
	}

	// remove hooks
	if err := f.deleteCustomHooks(); err != nil {
		f.logger.Errorf("del hooks: %v", err)
	}

	// flush base rules
	if err := f.flushCustomChains(); err != nil {
		f.logger.Errorf("del base: %v", err)
	}

	// delete chains
	if err := f.deleteCustomChains(); err != nil {
		f.logger.Errorf("del chains: %v", err)
	}

	// delete tables
	for _, family := range []nftables.TableFamily{nftables.TableFamilyIPv4, nftables.TableFamilyIPv6} {
		if err := deleteTableIfExists(f.conn, family, "filter"); err != nil {
			f.logger.Errorf("delete filter table (%v): %v", family, err)
		}
		if err := deleteTableIfExists(f.conn, family, "nat"); err != nil {
			f.logger.Errorf("delete nat table (%v): %v", family, err)
		}
	}

	if err := f.conn.Flush(); err != nil {
		return fmt.Errorf("final flush: %w", err)
	}

	f.localAddrRules = nil
	f.tunnelRules = make(map[string][]*nftables.Rule)

	f.killSwitchEnabled.Store(false)

	f.logger.Verbosef("Firewall cleaned up and kill switch disabled")
	return nil
}

func (f *LinuxFirewall) AllowLocalNetworks(prefixes []netip.Prefix) error {
	if !f.IsEnabled() {
		return errors.New("kill switch must be enabled to allow local networks")
	}

	// remove any old rules
	for _, rule := range f.localAddrRules {
		f.conn.DelRule(rule)
	}
	f.localAddrRules = nil

	// add bypass rules for each prefix
	for _, table := range f.getTables() {
		outputChain, err := getChainFromTable(f.conn, table.Filter, chainNameOutput)
		if err != nil {
			return fmt.Errorf("get output chain: %w", err)
		}
		for _, prefix := range prefixes {
			if prefix.Addr().Is6() && !f.v6Available {
				continue
			}
			rule, err := createRangeRule(table.Filter, outputChain, prefix, expr.VerdictAccept)
			if err != nil {
				return fmt.Errorf("create bypass rule for %v: %w", prefix, err)
			}
			existing, _ := findRule(f.conn, rule)
			if existing == nil {
				f.conn.AddRule(rule)
				f.localAddrRules = append(f.localAddrRules, rule)
			}
		}
	}
	if err := f.conn.Flush(); err != nil {
		return fmt.Errorf("flush after bypassing local addrs: %w", err)
	}
	f.logger.Verbosef("Bypassed local addrs: %v", prefixes)
	return nil
}

func (f *LinuxFirewall) IsEnabled() bool {
	return f.killSwitchEnabled.Load()
}

type nftable struct {
	Proto  nftables.TableFamily
	Filter *nftables.Table
	Nat    *nftables.Table
}

type chainInfo struct {
	table         *nftables.Table
	name          string
	chainType     nftables.ChainType
	chainHook     *nftables.ChainHook
	chainPriority *nftables.ChainPriority
	chainPolicy   *nftables.ChainPolicy
}

var ErrChainNotFound = errors.New("chain not found")

type errorChainNotFound struct {
	chainName string
	tableName string
}

func (e errorChainNotFound) Error() string {
	return fmt.Sprintf("chain %s not found in table %s", e.chainName, e.tableName)
}

func (e errorChainNotFound) Is(target error) bool {
	return target == ErrChainNotFound
}

// SetTunnelPort adds punch rules for inbound UDP on the port.
func (f *LinuxFirewall) SetTunnelPort(port uint16) error {
	for _, table := range f.getTables() {
		inputChain, err := getChainFromTable(f.conn, table.Filter, chainNameInput)
		if err != nil {
			return fmt.Errorf("get input chain: %w", err)
		}
		if err := addAcceptOnPortRule(f.conn, table.Filter, inputChain, port); err != nil {
			return fmt.Errorf("add accept on port rule: %w", err)
		}
	}
	if err := f.conn.Flush(); err != nil {
		return fmt.Errorf("flush after adding port punch: %w", err)
	}
	f.tunnelPort = port
	f.logger.Verbosef("Added tunnel port punch for UDP port %d", port)
	return nil
}

// addAcceptOnPortRule adds the rule if not exist
func addAcceptOnPortRule(conn *nftables.Conn, table *nftables.Table, chain *nftables.Chain, port uint16) error {
	rule := createAcceptOnPortRule(table, chain, port)
	existing, err := findRule(conn, rule)
	if err != nil {
		return fmt.Errorf("find rule: %w", err)
	}
	if existing != nil {
		return nil // Already exists
	}
	conn.InsertRule(rule)
	return nil // Flush called outside
}

// createAcceptOnPortRule creates ACCEPT rule for UDP dport.
func createAcceptOnPortRule(table *nftables.Table, chain *nftables.Chain, port uint16) *nftables.Rule {
	portBytes := make([]byte, 2)
	// for network byte order
	binary.BigEndian.PutUint16(portBytes, port)
	return &nftables.Rule{
		Table: table,
		Chain: chain,
		Exprs: []expr.Any{
			// load layer 4 protocol (for UDP/TCP) in register 1 temp storage
			&expr.Meta{
				Key:      expr.MetaKeyL4PROTO,
				Register: 1,
			},
			// check if loaded register 1 storage is UDP
			&expr.Cmp{
				Op:       expr.CmpOpEq,
				Register: 1,
				Data:     []byte{unix.IPPROTO_UDP},
			},
			// load the destination port from register 1
			newLoadDportExpr(1),
			// check if the port matches our wg listener port
			&expr.Cmp{
				Op:       expr.CmpOpEq,
				Register: 1,
				Data:     portBytes,
			},
			&expr.Counter{},
			// allow it on the firewall
			&expr.Verdict{
				Kind: expr.VerdictAccept,
			},
		},
	}
}

// newLoadDportExpr loads dport to register
func newLoadDportExpr(destReg uint32) expr.Any {
	return &expr.Payload{
		DestRegister: destReg,
		Base:         expr.PayloadBaseTransportHeader,
		Offset:       2,
		Len:          2,
	}
}

// deleteTableIfExists deletes a nftables table if it exists.
func deleteTableIfExists(c *nftables.Conn, family nftables.TableFamily, name string) error {
	t, err := getTableIfExists(c, family, name)
	if err != nil {
		return fmt.Errorf("get table: %w", err)
	}
	if t == nil {
		return nil // Not exist
	}
	c.DelTable(t)
	if err := c.Flush(); err != nil {
		return fmt.Errorf("del table: %w", err)
	}
	return nil
}

// getTableIfExists returns the table if it exists.
func getTableIfExists(c *nftables.Conn, family nftables.TableFamily, name string) (*nftables.Table, error) {
	tables, err := c.ListTables()
	if err != nil {
		return nil, fmt.Errorf("get tables: %w", err)
	}
	for _, table := range tables {
		if table.Name == name && table.Family == family {
			return table, nil
		}
	}
	return nil, nil
}

// createTableIfNotExist creates a nftables table if not exist.
func createTableIfNotExist(c *nftables.Conn, family nftables.TableFamily, name string) (*nftables.Table, error) {
	if t, err := getTableIfExists(c, family, name); err != nil {
		return nil, fmt.Errorf("get table: %w", err)
	} else if t != nil {
		return t, nil
	}
	t := c.AddTable(&nftables.Table{
		Family: family,
		Name:   name,
	})
	if err := c.Flush(); err != nil {
		return nil, fmt.Errorf("add table: %w", err)
	}
	return t, nil
}

// getChainFromTable returns the chain if it exists.
func getChainFromTable(c *nftables.Conn, table *nftables.Table, name string) (*nftables.Chain, error) {
	chains, err := c.ListChainsOfTableFamily(table.Family)
	if err != nil {
		return nil, fmt.Errorf("list chains: %w", err)
	}
	for _, chain := range chains {
		if chain.Table.Name == table.Name && chain.Name == name {
			return chain, nil
		}
	}
	return nil, errorChainNotFound{chainName: name, tableName: table.Name}
}

// createChainIfNotExist creates a chain if not exist.
func createChainIfNotExist(c *nftables.Conn, cinfo chainInfo) error {
	_, err := getOrCreateChain(c, cinfo)
	return err
}

func getOrCreateChain(c *nftables.Conn, cinfo chainInfo) (*nftables.Chain, error) {
	chain, err := getChainFromTable(c, cinfo.table, cinfo.name)
	if err != nil && !errors.Is(err, ErrChainNotFound) {
		return nil, fmt.Errorf("get chain: %w", err)
	} else if err == nil {
		// Existing chain; check compatibility if needed
		return chain, nil
	}

	chain = c.AddChain(&nftables.Chain{
		Name:     cinfo.name,
		Table:    cinfo.table,
		Type:     cinfo.chainType,
		Hooknum:  cinfo.chainHook,
		Priority: cinfo.chainPriority,
		Policy:   cinfo.chainPolicy,
	})

	if err := c.Flush(); err != nil {
		return nil, fmt.Errorf("add chain: %w", err)
	}

	return chain, nil
}

// deleteChainIfExists deletes a chain if it exists.
func deleteChainIfExists(c *nftables.Conn, table *nftables.Table, name string) error {
	chain, err := getChainFromTable(c, table, name)
	if err != nil && !errors.Is(err, errorChainNotFound{table.Name, name}) {
		return fmt.Errorf("get chain: %w", err)
	} else if err != nil {
		return nil // Not exist
	}

	c.FlushChain(chain)
	c.DelChain(chain)

	if err := c.Flush(); err != nil {
		return fmt.Errorf("flush and delete chain: %w", err)
	}

	return nil
}

// getTables returns v4/v6 tables based on system support.
func (f *LinuxFirewall) getTables() []*nftable {
	if f.v6Available {
		return []*nftable{f.nft4, f.nft6}
	}
	return []*nftable{f.nft4}
}

// getNFTByAddr selects v4/v6 table by addr family.
func (f *LinuxFirewall) getNFTByAddr(addr netip.Addr) (*nftable, error) {
	if addr.Is6() && !f.v6Available {
		return nil, fmt.Errorf("nftables for IPv6 not available")
	}
	if addr.Is6() {
		return f.nft6, nil
	}
	return f.nft4, nil
}

// findRule finds a rule by matching expressions.
func findRule(conn *nftables.Conn, rule *nftables.Rule) (*nftables.Rule, error) {
	rules, err := conn.GetRules(rule.Table, rule.Chain)
	if err != nil {
		return nil, fmt.Errorf("get rules: %w", err)
	}
	for _, r := range rules {
		if len(r.Exprs) != len(rule.Exprs) {
			continue
		}
		match := true
		for i, e := range r.Exprs {
			if _, ok := e.(*expr.Counter); ok {
				continue // Skip counters
			}
			if !reflect.DeepEqual(e, rule.Exprs[i]) {
				match = false
				break
			}
		}
		if match {
			return r, nil
		}
	}
	return nil, nil
}

func (f *LinuxFirewall) Enable() error {
	if f.IsEnabled() {
		f.logger.Verbosef("Kill switch already active, skipping activation")
		return nil
	}

	polAccept := nftables.ChainPolicyAccept
	for _, table := range f.getTables() {
		// Create filter table
		filter, err := createTableIfNotExist(f.conn, table.Proto, "filter")
		if err != nil {
			return fmt.Errorf("create filter table: %w", err)
		}
		table.Filter = filter

		_, err = getOrCreateChain(f.conn, chainInfo{filter, baseChainForward, nftables.ChainTypeFilter, nftables.ChainHookForward, nftables.ChainPriorityFilter, &polAccept})
		if err != nil {
			return fmt.Errorf("create FORWARD chain: %w", err)
		}
		_, err = getOrCreateChain(f.conn, chainInfo{filter, baseChainInput, nftables.ChainTypeFilter, nftables.ChainHookInput, nftables.ChainPriorityFilter, &polAccept})
		if err != nil {
			return fmt.Errorf("create INPUT chain: %w", err)
		}
		_, err = getOrCreateChain(f.conn, chainInfo{filter, baseChainOutput, nftables.ChainTypeFilter, nftables.ChainHookOutput, nftables.ChainPriorityFilter, &polAccept})
		if err != nil {
			return fmt.Errorf("create OUTPUT chain: %w", err)
		}

		// Custom chains (regular, jumped to from conventional)
		if err = createChainIfNotExist(f.conn, chainInfo{filter, chainNameForward, chainTypeRegular, nil, nil, nil}); err != nil {
			return fmt.Errorf("create wgtunnel-forward chain: %w", err)
		}
		if err = createChainIfNotExist(f.conn, chainInfo{filter, chainNameInput, chainTypeRegular, nil, nil, nil}); err != nil {
			return fmt.Errorf("create wgtunnel-input chain: %w", err)
		}
		if err = createChainIfNotExist(f.conn, chainInfo{filter, chainNameOutput, chainTypeRegular, nil, nil, nil}); err != nil {
			return fmt.Errorf("create wgtunnel-output chain: %w", err)
		}

		nat, err := createTableIfNotExist(f.conn, table.Proto, "nat")
		if err != nil {
			return fmt.Errorf("create nat table: %w", err)
		}
		table.Nat = nat

		_, err = getOrCreateChain(f.conn, chainInfo{nat, baseChainPostrouting, nftables.ChainTypeNAT, nftables.ChainHookPostrouting, nftables.ChainPriorityNATSource, &polAccept})
		if err != nil {
			return fmt.Errorf("create POSTROUTING chain: %w", err)
		}
		if err = createChainIfNotExist(f.conn, chainInfo{nat, chainNamePostrouting, chainTypeRegular, nil, nil, nil}); err != nil {
			return fmt.Errorf("create wgtunnel-postrouting chain: %w", err)
		}
	}

	if err := f.conn.Flush(); err != nil {
		return fmt.Errorf("flush after chain creation: %w", err)
	}

	if err := f.addHooks(); err != nil {
		return fmt.Errorf("add hooks: %w", err)
	}

	if err := f.addKillSwitchRules(); err != nil {
		return fmt.Errorf("add kill switch rules: %w", err)
	}

	f.killSwitchEnabled.Store(true)
	return nil
}

// addHooks adds jump rules from conventional chains to custom ones.
func (f *LinuxFirewall) addHooks() error {
	conn := f.conn

	for _, table := range f.getTables() {
		inputChain, err := getChainFromTable(conn, table.Filter, baseChainInput)
		if err != nil {
			return fmt.Errorf("get INPUT chain: %w", err)
		}
		if err = addHookRule(conn, table.Filter, inputChain, chainNameInput); err != nil {
			return fmt.Errorf("add INPUT hook: %w", err)
		}

		forwardChain, err := getChainFromTable(conn, table.Filter, baseChainForward)
		if err != nil {
			return fmt.Errorf("get FORWARD chain: %w", err)
		}
		if err = addHookRule(conn, table.Filter, forwardChain, chainNameForward); err != nil {
			return fmt.Errorf("add FORWARD hook: %w", err)
		}

		outputChain, err := getChainFromTable(conn, table.Filter, baseChainOutput)
		if err != nil {
			return fmt.Errorf("get OUTPUT chain: %w", err)
		}
		if err = addHookRule(conn, table.Filter, outputChain, chainNameOutput); err != nil {
			return fmt.Errorf("add OUTPUT hook: %w", err)
		}

		postroutingChain, err := getChainFromTable(conn, table.Nat, baseChainPostrouting)
		if err != nil {
			return fmt.Errorf("get POSTROUTING chain: %w", err)
		}
		if err = addHookRule(conn, table.Nat, postroutingChain, chainNamePostrouting); err != nil {
			return fmt.Errorf("add POSTROUTING hook: %w", err)
		}
	}
	return nil
}

// createHookRule creates a jump rule.
func createHookRule(table *nftables.Table, fromChain *nftables.Chain, toChainName string) *nftables.Rule {
	return &nftables.Rule{
		Table: table,
		Chain: fromChain,
		Exprs: []expr.Any{
			&expr.Counter{},
			&expr.Verdict{
				Kind:  expr.VerdictJump,
				Chain: toChainName,
			},
		},
	}
}

// addHookRule inserts a jump rule at the top.
func addHookRule(conn *nftables.Conn, table *nftables.Table, fromChain *nftables.Chain, toChainName string) error {
	rule := createHookRule(table, fromChain, toChainName)
	conn.InsertRule(rule)
	return conn.Flush()
}

// addKillSwitchRules adds bypass for fwmark and DROP at end (private helper).
func (f *LinuxFirewall) addKillSwitchRules() error {
	f.logger.Verbosef("Adding kill switch rules...")

	for _, table := range f.getTables() {

		inputChain, err := getChainFromTable(f.conn, table.Filter, chainNameInput)
		if err != nil {
			return fmt.Errorf("get input chain: %w", err)
		}

		// allow loopback
		if err := f.addLoopbackRule(table.Filter, inputChain); err != nil {
			return err
		}

		// allow Established/Related traffic for reply
		if err := f.addEstablishedRule(table.Filter, inputChain); err != nil {
			return err
		}

		// drop everything else
		dropRule := createDropRule(table.Filter, inputChain)
		f.conn.AddRule(dropRule)

		outputChain, err := getChainFromTable(f.conn, table.Filter, chainNameOutput)
		if err != nil {
			return fmt.Errorf("get output chain: %w", err)
		}

		// allow loopback on output
		if err := f.addLoopbackRule(table.Filter, outputChain); err != nil {
			return err
		}

		// allow the marked tunnel traffic
		bypassRule := createFwmarkRule(table.Filter, outputChain, mark.LinuxBypassMarkNum)
		f.conn.InsertRule(bypassRule)

		// drop everything else
		dropRule = createDropRule(table.Filter, outputChain)
		f.conn.AddRule(dropRule)

		forwardChain, err := getChainFromTable(f.conn, table.Filter, chainNameForward)
		if err != nil {
			return fmt.Errorf("get forward chain: %w", err)
		}

		// drop all forwarded traffic
		dropRule = createDropRule(table.Filter, forwardChain)
		f.conn.AddRule(dropRule)
	}

	if err := f.conn.Flush(); err != nil {
		return fmt.Errorf("flush after adding kill switch: %w", err)
	}
	f.logger.Verbosef("Kill switch rules added.")
	return nil
}

// addTunnelInterfaceRule adds a rule to let our tun interface escape firewall
func (f *LinuxFirewall) addTunnelInterfaceRule(iface string, table *nftables.Table, chain *nftables.Chain) error {
	tunnelBypassRule := &nftables.Rule{
		Table: table,
		Chain: chain,
		Exprs: []expr.Any{
			&expr.Meta{
				Key:      expr.MetaKeyOIFNAME,
				Register: 1,
			},
			&expr.Cmp{
				Op:       expr.CmpOpEq,
				Register: 1,
				Data:     []byte(iface + "\x00"),
			},
			&expr.Counter{},
			&expr.Verdict{Kind: expr.VerdictAccept},
		},
	}
	existing, _ := findRule(f.conn, tunnelBypassRule)
	if existing == nil {
		f.conn.InsertRule(tunnelBypassRule)
	}
	return nil
}

func (f *LinuxFirewall) addLoopbackRule(table *nftables.Table, chain *nftables.Chain) error {
	loRule := &nftables.Rule{
		Table: table,
		Chain: chain,
		Exprs: []expr.Any{
			&expr.Meta{
				Key:      getIfKeyForChain(chain),
				Register: 1,
			},
			// Compare Register 1 to "lo"
			&expr.Cmp{
				Op:       expr.CmpOpEq,
				Register: 1,
				Data:     []byte("lo\x00"), // Null-terminated string
			},
			&expr.Counter{},
			&expr.Verdict{Kind: expr.VerdictAccept},
		},
	}
	f.conn.InsertRule(loRule)
	return nil
}

// Helper to determine if we should look at Input or Output interface
func getIfKeyForChain(chain *nftables.Chain) expr.MetaKey {
	if chain.Name == chainNameInput {
		return expr.MetaKeyIIFNAME
	}
	return expr.MetaKeyOIFNAME
}

func (f *LinuxFirewall) addEstablishedRule(table *nftables.Table, chain *nftables.Chain) error {
	rule := &nftables.Rule{
		Table: table,
		Chain: chain,
		Exprs: []expr.Any{
			// Load Connection Tracking State
			&expr.Ct{
				Key:      expr.CtKeySTATE,
				Register: 1,
			},
			// Bitwise check for Established (0x02) | Related (0x04) = 0x06
			&expr.Bitwise{
				SourceRegister: 1,
				DestRegister:   1,
				Len:            4,
				Mask:           []byte{0x06, 0x00, 0x00, 0x00}, // Bits 1 and 2 (Est/Rel)
				Xor:            []byte{0x00, 0x00, 0x00, 0x00},
			},
			&expr.Cmp{
				Op:       expr.CmpOpNeq, // If result is NOT 0, it matched one of the bits
				Register: 1,
				Data:     []byte{0x00, 0x00, 0x00, 0x00},
			},
			&expr.Counter{},
			&expr.Verdict{Kind: expr.VerdictAccept},
		},
	}

	f.conn.InsertRule(rule)
	return nil
}

// delKillSwitchRules removes kill switch by flushing chains
func (f *LinuxFirewall) delKillSwitchRules() error {
	f.logger.Verbosef("Removing kill switch rules...")

	for _, table := range f.getTables() {
		if outputChain, err := getChainFromTable(f.conn, table.Filter, chainNameOutput); err == nil {
			f.conn.FlushChain(outputChain)
		}

		if inputChain, err := getChainFromTable(f.conn, table.Filter, chainNameInput); err == nil {
			f.conn.FlushChain(inputChain)
		}

		if forwardChain, err := getChainFromTable(f.conn, table.Filter, chainNameForward); err == nil {
			f.conn.FlushChain(forwardChain)
		}
	}

	if err := f.conn.Flush(); err != nil {
		return fmt.Errorf("flush after deleting kill switch: %w", err)
	}

	f.logger.Verbosef("Kill switch rules removed.")

	return nil
}

// createDropRule creates a simple DROP rule with counter
func createDropRule(table *nftables.Table, chain *nftables.Chain) *nftables.Rule {
	return &nftables.Rule{
		Table: table,
		Chain: chain,
		Exprs: []expr.Any{
			&expr.Counter{},
			&expr.Verdict{Kind: expr.VerdictDrop},
		},
	}
}

// createRangeRule creates ACCEPT for dst IP in prefix/range (adapted for daddr).
func createRangeRule(
	table *nftables.Table,
	chain *nftables.Chain,
	rng netip.Prefix,
	decision expr.VerdictKind,
) (*nftables.Rule, error) {
	var loadExpr expr.Any
	var maskLen uint32
	var mask []byte
	var xor []byte
	var err error

	if rng.Addr().Is4() {
		loadExpr, err = newLoadDaddrExpr(nftables.TableFamilyIPv4, 1)
		if err != nil {
			return nil, fmt.Errorf("newLoadDaddrExpr: %w", err)
		}
		maskLen = 4
		mask = maskOf(rng)
		xor = []byte{0x00, 0x00, 0x00, 0x00}
	} else {
		loadExpr, err = newLoadDaddrExpr(nftables.TableFamilyIPv6, 1)
		if err != nil {
			return nil, fmt.Errorf("newLoadDaddrExpr: %w", err)
		}
		maskLen = 16
		bits := rng.Bits()
		mask = make([]byte, 16)
		for i := 0; i < bits/8; i++ {
			mask[i] = 0xff
		}
		if bits%8 != 0 {
			mask[bits/8] = 0xff << (8 - uint(bits%8))
		}
		xor = make([]byte, 16)
	}

	netip := rng.Addr().AsSlice()
	rule := &nftables.Rule{
		Table: table,
		Chain: chain,
		Exprs: []expr.Any{
			loadExpr,
			&expr.Bitwise{
				SourceRegister: 1,
				DestRegister:   1,
				Len:            maskLen,
				Mask:           mask,
				Xor:            xor,
			},
			&expr.Cmp{
				Op:       expr.CmpOpEq,
				Register: 1,
				Data:     netip,
			},
			&expr.Counter{},
			&expr.Verdict{
				Kind: decision,
			},
		},
	}
	return rule, nil
}

// newLoadDaddrExpr loads destination addr into register.
func newLoadDaddrExpr(proto nftables.TableFamily, destReg uint32) (expr.Any, error) {
	switch proto {
	case nftables.TableFamilyIPv4:
		return &expr.Payload{
			DestRegister: destReg,
			Base:         expr.PayloadBaseNetworkHeader,
			Offset:       16, // IPv4 offset
			Len:          4,
		}, nil
	case nftables.TableFamilyIPv6:
		return &expr.Payload{
			DestRegister: destReg,
			Base:         expr.PayloadBaseNetworkHeader,
			Offset:       24, // IPv6 offset
			Len:          16,
		}, nil
	default:
		return nil, fmt.Errorf("unsupported family %v", proto)
	}
}

// createFwmarkRule generates a rule for a specific mark within our mask
func createFwmarkRule(table *nftables.Table, chain *nftables.Chain, markVal uint32) *nftables.Rule {
	maskBytes := make([]byte, 4)
	binary.LittleEndian.PutUint32(maskBytes, mark.LinuxFwmarkMaskNum)

	markBytes := make([]byte, 4)
	binary.LittleEndian.PutUint32(markBytes, markVal)

	return &nftables.Rule{
		Table: table,
		Chain: chain,
		Exprs: []expr.Any{
			&expr.Meta{Key: expr.MetaKeyMARK, Register: 1},
			&expr.Bitwise{
				SourceRegister: 1,
				DestRegister:   1,
				Len:            4,
				Mask:           maskBytes,
				Xor:            []byte{0x00, 0x00, 0x00, 0x00},
			},
			&expr.Cmp{
				Op:       expr.CmpOpEq,
				Register: 1,
				Data:     markBytes,
			},
			&expr.Counter{},
			&expr.Verdict{Kind: expr.VerdictAccept},
		},
	}
}

// maskOf returns CIDR mask bytes
func maskOf(pfx netip.Prefix) []byte {
	mask := make([]byte, 4)
	binary.BigEndian.PutUint32(mask, ^(uint32(0xffffffff) >> pfx.Bits()))
	return mask
}

// deleteCustomHooks removes jump rules from base to custom chains
func (f *LinuxFirewall) deleteCustomHooks() error {
	conn := f.conn
	for _, table := range f.getTables() {
		if table == nil || table.Filter == nil {
			continue // skip if table or filter not initialized
		}
		inputChain, err := getChainFromTable(conn, table.Filter, baseChainInput)
		if err == nil && inputChain != nil {
			deleteHookRule(conn, table.Filter, inputChain, chainNameInput)
		}

		forwardChain, err := getChainFromTable(conn, table.Filter, baseChainForward)
		if err == nil && forwardChain != nil {
			deleteHookRule(conn, table.Filter, forwardChain, chainNameForward)
		}

		outputChain, err := getChainFromTable(conn, table.Filter, baseChainOutput)
		if err == nil && outputChain != nil {
			deleteHookRule(conn, table.Filter, outputChain, chainNameOutput)
		}

		if table.Nat == nil {
			continue
		}
		postroutingChain, err := getChainFromTable(conn, table.Nat, baseChainPostrouting)
		if err == nil && postroutingChain != nil {
			deleteHookRule(conn, table.Nat, postroutingChain, chainNamePostrouting)
		}
	}
	return conn.Flush()
}

// deleteHookRule deletes a specific jump rule if it exists
func deleteHookRule(conn *nftables.Conn, table *nftables.Table, fromChain *nftables.Chain, toChainName string) error {
	rule := createHookRule(table, fromChain, toChainName)
	existing, err := findRule(conn, rule)
	if err != nil || existing == nil {
		return err // Or nil if not found
	}
	conn.DelRule(existing)
	return nil
}

// deleteCustomChains deletes custom chains
func (f *LinuxFirewall) deleteCustomChains() error {
	for _, table := range f.getTables() {
		deleteChainIfExists(f.conn, table.Filter, chainNameForward)
		deleteChainIfExists(f.conn, table.Filter, chainNameInput)
		deleteChainIfExists(f.conn, table.Filter, chainNameOutput)
		deleteChainIfExists(f.conn, table.Nat, chainNamePostrouting)
	}
	return f.conn.Flush()
}

// flushCustomChains flushes rules from custom chains
func (f *LinuxFirewall) flushCustomChains() error {
	for _, table := range f.getTables() {
		inputChain, err := getChainFromTable(f.conn, table.Filter, chainNameInput)
		if err == nil {
			f.conn.FlushChain(inputChain)
		}

		forwardChain, err := getChainFromTable(f.conn, table.Filter, chainNameForward)
		if err == nil {
			f.conn.FlushChain(forwardChain)
		}

		outputChain, err := getChainFromTable(f.conn, table.Filter, chainNameOutput)
		if err == nil {
			f.conn.FlushChain(outputChain)
		}

		postrouteChain, err := getChainFromTable(f.conn, table.Nat, chainNamePostrouting)
		if err == nil {
			f.conn.FlushChain(postrouteChain)
		}
	}
	return f.conn.Flush()
}
