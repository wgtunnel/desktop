// Copyright (c) 2026 Zane Schepke / WG Tunnel
// Portions derived from Tailscale; see NOTICE and BSD-3-Clause.txt.
//go:build linux && !android

package main

import (
	"encoding/binary"
	"fmt"
	"github.com/google/nftables"
	"github.com/google/nftables/expr"
	"net"
	"net/netip"
	"reflect"
	"sync"
)

const dnsMark = 0x300000

var firewall LinuxFirewall

type LinuxFirewall struct {
	mu           sync.Mutex
	whitelist    []netip.Prefix
	whitelistDNS bool
}
type nftable struct {
	Filter *nftables.Table
	Proto  nftables.TableFamily
}

func activeTables() ([]nftable, error) {
	c := &nftables.Conn{}
	all, err := c.ListTables()
	if err != nil {
		return nil, err
	}
	var out []nftable
	for _, t := range all {
		if t.Name == "wgtunnel" && (t.Family == nftables.TableFamilyIPv4 || t.Family == nftables.TableFamilyIPv6) {
			out = append(out, nftable{t, t.Family})
		}
	}
	return out, nil
}
func (f *LinuxFirewall) reconcile() error {
	f.mu.Lock()
	defer f.mu.Unlock()
	tables, err := activeTables()
	if err != nil {
		return err
	}
	c := &nftables.Conn{}
	for _, t := range tables {
		for base, name := range map[string]string{"input": chainNameDirectInput, "output": chainNameDirect} {
			ch, err := getChainFromTable(c, t.Filter, base)
			if err != nil {
				return err
			}
			hook, err := findRule(c, createHookRule(t.Filter, ch, name))
			if err != nil {
				return err
			}
			if hook == nil {
				return f.installDirectWhitelist()
			}
		}
	}
	return nil
}

func getChainFromTable(c *nftables.Conn, t *nftables.Table, name string) (*nftables.Chain, error) {
	chains, err := c.ListChains()
	if err != nil {
		return nil, err
	}
	for _, ch := range chains {
		if ch.Table.Name == t.Name && ch.Table.Family == t.Family && ch.Name == name {
			return ch, nil
		}
	}
	return nil, fmt.Errorf("chain %s missing", name)
}
func ensureChain(c *nftables.Conn, t *nftables.Table, name string) (*nftables.Chain, error) {
	ch, err := getChainFromTable(c, t, name)
	if err == nil {
		return ch, nil
	}
	ch = c.AddChain(&nftables.Chain{Table: t, Name: name})
	return ch, c.Flush()
}
func createHookRule(t *nftables.Table, c *nftables.Chain, name string) *nftables.Rule {
	return &nftables.Rule{Table: t, Chain: c, UserData: []byte("wgtunnel-direct-whitelist"), Exprs: []expr.Any{&expr.Verdict{Kind: expr.VerdictJump, Chain: name}}}
}
func findRule(c *nftables.Conn, r *nftables.Rule) (*nftables.Rule, error) {
	rules, err := c.GetRules(r.Table, r.Chain)
	if err != nil {
		return nil, err
	}
	for _, existing := range rules {
		if string(existing.UserData) == string(r.UserData) && reflect.DeepEqual(existing.Exprs, r.Exprs) {
			return existing, nil
		}
	}
	return nil, nil
}
func createRangeRule(t *nftables.Table, c *nftables.Chain, p netip.Prefix, _ expr.VerdictKind) (*nftables.Rule, error) {
	p = p.Masked()
	addr := p.Addr().AsSlice()
	offset := uint32(16)
	if p.Addr().Is6() {
		offset = 24
	}
	mask := net.CIDRMask(p.Bits(), p.Addr().BitLen())
	return &nftables.Rule{Table: t, Chain: c, Exprs: []expr.Any{
		&expr.Payload{DestRegister: 1, Base: expr.PayloadBaseNetworkHeader, Offset: offset, Len: uint32(len(addr))},
		&expr.Bitwise{SourceRegister: 1, DestRegister: 1, Len: uint32(len(addr)), Mask: mask, Xor: make([]byte, len(addr))},
		&expr.Cmp{Op: expr.CmpOpEq, Register: 1, Data: addr}, &expr.Verdict{Kind: expr.VerdictAccept}}}, nil
}
func createFwmarkRule(t *nftables.Table, c *nftables.Chain, m uint32) *nftables.Rule {
	mask := make([]byte, 4)
	binary.NativeEndian.PutUint32(mask, 0xff0000)
	value := make([]byte, 4)
	binary.NativeEndian.PutUint32(value, m)
	return &nftables.Rule{Table: t, Chain: c, Exprs: []expr.Any{&expr.Meta{Key: expr.MetaKeyMARK, Register: 1},
		&expr.Bitwise{SourceRegister: 1, DestRegister: 1, Len: 4, Mask: mask, Xor: make([]byte, 4)},
		&expr.Cmp{Op: expr.CmpOpEq, Register: 1, Data: value}, &expr.Verdict{Kind: expr.VerdictAccept}}}
}

const chainNameDirect = "wgtunnel-direct"
const chainNameDirectInput = "wgtunnel-direct-input"

// SetDirectWhitelist swaps only the dedicated child chain, never the kill switch
// or tunnel bypasses. nftables applies the replacement as a single transaction.
func (f *LinuxFirewall) SetDirectWhitelist(prefixes []netip.Prefix, dns bool) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	old := f.whitelist
	oldDNS := f.whitelistDNS
	f.whitelistDNS = dns
	f.whitelist = append([]netip.Prefix(nil), prefixes...)
	{
		if err := f.installDirectWhitelist(); err != nil {
			f.whitelist = old
			f.whitelistDNS = oldDNS
			return err
		}
	}
	return nil
}

func (f *LinuxFirewall) installDirectWhitelist() error {
	tables, err := activeTables()
	if err != nil {
		return err
	}
	// Use a fresh connection so a construction error cannot leave queued changes
	// on the connection used by the rest of the firewall.
	prep := &nftables.Conn{}
	for _, table := range tables {
		for _, name := range []string{chainNameDirect, chainNameDirectInput} {
			if _, err := ensureChain(prep, table.Filter, name); err != nil {
				return err
			}
		}
	}
	c := &nftables.Conn{}
	for _, table := range tables {
		chain, err := getChainFromTable(c, table.Filter, chainNameDirect)
		if err != nil {
			return err
		}
		inputDirect, err := getChainFromTable(c, table.Filter, chainNameDirectInput)
		if err != nil {
			return err
		}
		input, err := getChainFromTable(c, table.Filter, "input")
		if err != nil {
			return err
		}
		inputHook := createHookRule(table.Filter, input, chainNameDirectInput)
		inputExisting, err := findRule(c, inputHook)
		if err != nil {
			return err
		}
		if inputExisting == nil {
			c.InsertRule(inputHook)
		}
		c.FlushChain(inputDirect)
		c.FlushChain(chain)
		output, err := getChainFromTable(c, table.Filter, "output")
		if err != nil {
			return err
		}
		hook := createHookRule(table.Filter, output, chainNameDirect)
		existing, err := findRule(c, hook)
		if err != nil {
			return err
		}
		if existing == nil {
			c.InsertRule(hook)
		}
		if f.whitelistDNS {
			// Only privileged sockets created by this process receive this mark.
			// DNS refresh continues while a persistent kill switch has no tunnel.
			c.AddRule(createFwmarkRule(table.Filter, chain, dnsMark))
		}
		if table.Proto == nftables.TableFamilyIPv6 {
			hasV6 := false
			for _, p := range f.whitelist {
				hasV6 = hasV6 || p.Addr().Is6()
			}
			if hasV6 {
				// IPv6 needs neighbor discovery, which is not ESTABLISHED traffic.
				// Only link-local control packets (hop limit 255) are allowed.
				for _, kind := range []byte{133, 135, 136} {
					c.AddRule(neighborRule(table.Filter, chain, kind))
				}
				for _, kind := range []byte{134, 135, 136} {
					c.AddRule(neighborRule(table.Filter, inputDirect, kind))
				}
			}
		}
		for _, prefix := range f.whitelist {
			if prefix.Addr().Is4() != (table.Proto == nftables.TableFamilyIPv4) {
				continue
			}
			rule, err := createRangeRule(table.Filter, chain, prefix.Masked(), expr.VerdictAccept)
			if err != nil {
				return fmt.Errorf("whitelist rule: %w", err)
			}
			c.AddRule(rule)
		}
	}
	return c.Flush()
}

func neighborRule(table *nftables.Table, chain *nftables.Chain, kind byte) *nftables.Rule {
	return &nftables.Rule{Table: table, Chain: chain, Exprs: []expr.Any{
		&expr.Meta{Key: expr.MetaKeyL4PROTO, Register: 1},
		&expr.Cmp{Op: expr.CmpOpEq, Register: 1, Data: []byte{58}},
		&expr.Payload{DestRegister: 1, Base: expr.PayloadBaseNetworkHeader, Offset: 7, Len: 1},
		&expr.Cmp{Op: expr.CmpOpEq, Register: 1, Data: []byte{255}},
		&expr.Payload{DestRegister: 1, Base: expr.PayloadBaseTransportHeader, Offset: 0, Len: 1},
		&expr.Cmp{Op: expr.CmpOpEq, Register: 1, Data: []byte{kind}},
		&expr.Verdict{Kind: expr.VerdictAccept},
	}}
}
