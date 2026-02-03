// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 WG Tunnel.
// Adapted from Tailscale

//go:build windows

package osfirewall

import (
	"fmt"
	"net/netip"
	"os"
	"sync"
	"sync/atomic"

	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall"
	"golang.org/x/net/nettest"
	"golang.org/x/sys/windows"
	"inet.af/wf"
	"tailscale.com/net/netaddr"
)

type WindowsFirewall struct {
	mu sync.Mutex // protect shared state

	logger *device.Logger

	session *wf.Session

	providerID wf.ProviderID
	sublayerID wf.SublayerID

	iface string

	luid uint64

	killSwitchEnabled atomic.Bool

	tunRules        []*wf.Rule
	localAddrRules  []*wf.Rule
	permittedRoutes map[netip.Prefix][]*wf.Rule
}

type weight uint64

const (
	weightDaemonTraffic weight = 15
	weightKnownTraffic  weight = 12
	weightCatchAll      weight = 0
)

type protocol int

const (
	protocolV4 protocol = iota
	protocolV6
	protocolAll
)

type direction int

const (
	directionInbound direction = iota
	directionOutbound
	directionBoth
)

// Known addresses.
var (
	linkLocalRange           = netip.MustParsePrefix("fe80::/10")
	linkLocalDHCPMulticast   = netip.MustParseAddr("ff02::1:2")
	siteLocalDHCPMulticast   = netip.MustParseAddr("ff05::1:3")
	linkLocalRouterMulticast = netip.MustParseAddr("ff02::2")
)

func New(logger *device.Logger) (firewall.Firewall, error) {
	session, err := wf.New(&wf.Options{
		Name:        "WG Tunnel firewall",
		Description: "Manages WG Tunnel firewall rules",
		Dynamic:     true, // Removes rules on close
	})

	if err != nil {
		return nil, fmt.Errorf("create WFP session: %w", err)
	}

	guid, err := windows.GenerateGUID()
	if err != nil {
		return nil, err
	}

	providerID := wf.ProviderID(guid)
	if err := session.AddProvider(&wf.Provider{
		ID:   providerID,
		Name: "WG Tunnel provider",
	}); err != nil {
		return nil, err
	}

	guid, err = windows.GenerateGUID()
	if err != nil {
		return nil, err
	}

	sublayerID := wf.SublayerID(guid)
	if err := session.AddSublayer(&wf.Sublayer{
		ID:     sublayerID,
		Name:   "WG Tunnel permissive and blocking filters",
		Weight: uint16(weightCatchAll),
	}); err != nil {
		return nil, err
	}

	f := &WindowsFirewall{
		logger:          logger,
		session:         session,
		providerID:      providerID,
		sublayerID:      sublayerID,
		permittedRoutes: make(map[netip.Prefix][]*wf.Rule),
		tunRules:        make([]*wf.Rule, 0),
	}
	return f, nil
}

// addPermissiveRulesForPrefixes is a helper to add permissive rules for a list of prefixes
func (f *WindowsFirewall) addPermissiveRulesForPrefixes(prefixes []netip.Prefix, namePrefix string) (map[netip.Prefix][]*wf.Rule, error) {
	f.mu.Lock()
	defer f.mu.Unlock()

	addedByPrefix := make(map[netip.Prefix][]*wf.Rule)
	var partialAdds []netip.Prefix // rollback tracking
	for _, prefix := range prefixes {
		if prefix.Addr().Is6() && !nettest.SupportsIPv6() {
			continue
		}
		conditions := []*wf.Match{
			{
				Field: wf.FieldIPRemoteAddress,
				Op:    wf.MatchTypeEqual,
				Value: prefix,
			},
		}
		var p protocol
		if prefix.Addr().Is4() {
			p = protocolV4
		} else {
			p = protocolV6
		}
		rules, err := f.addRules(namePrefix+prefix.String(), weightKnownTraffic, conditions, wf.ActionPermit, p, directionBoth)
		if err != nil {
			for _, addedPrefix := range partialAdds {
				if delErr := f.removeRules(addedByPrefix[addedPrefix]); delErr != nil {
					f.logger.Errorf("Failed to delete partial rules for %v during rollback: %v", addedPrefix, delErr)
				}
			}
			return nil, fmt.Errorf("add permissive rules for %v: %w", prefix, err)
		}
		addedByPrefix[prefix] = rules
		partialAdds = append(partialAdds, prefix)
	}
	return addedByPrefix, nil
}

// removeRules is a helper to remove a list of rules
func (f *WindowsFirewall) removeRules(rules []*wf.Rule) error {
	f.mu.Lock()
	defer f.mu.Unlock()

	for _, rule := range rules {
		if err := f.session.DeleteRule(rule.ID); err != nil {
			f.logger.Errorf("Failed to delete rule %s: %v", rule.Name, err)
			// Continue to try deleting others
		}
	}
	return nil
}

func (f *WindowsFirewall) AllowLocalNetworks(addrs []netip.Prefix) error {
	// cleanup old local addr rules
	if err := f.removeRules(f.localAddrRules); err != nil {
		f.logger.Errorf("Failed to remove old local addr rules: %v", err)
	}
	f.mu.Lock()
	f.localAddrRules = nil
	f.mu.Unlock()

	// add new rules
	addedByPrefix, err := f.addPermissiveRulesForPrefixes(addrs, "bypass for local addr ")
	if err != nil {
		return err
	}
	f.mu.Lock()
	f.localAddrRules = nil
	for _, rules := range addedByPrefix {
		f.localAddrRules = append(f.localAddrRules, rules...)
	}
	f.mu.Unlock()
	f.logger.Verbosef("Bypassed local addrs in firewall")
	return nil
}

func (f *WindowsFirewall) UpdatePermittedRoutes(newRoutes []netip.Prefix) error {
	f.mu.Lock()
	// routes to remove
	var routesToRemove []netip.Prefix
	for existing := range f.permittedRoutes {
		found := false
		for _, newRoute := range newRoutes {
			if existing == newRoute {
				found = true
				break
			}
		}
		if !found {
			routesToRemove = append(routesToRemove, existing)
		}
	}
	f.mu.Unlock()
	for _, r := range routesToRemove {
		f.mu.Lock()
		rules := f.permittedRoutes[r]
		f.mu.Unlock()
		if err := f.removeRules(rules); err != nil {
			f.logger.Errorf("Failed to remove permitted route %v: %v", r, err)
		}
		f.mu.Lock()
		delete(f.permittedRoutes, r)
		f.mu.Unlock()
	}

	// routes to add
	var routesToAdd []netip.Prefix
	f.mu.Lock()
	for _, newRoute := range newRoutes {
		if _, exists := f.permittedRoutes[newRoute]; !exists {
			routesToAdd = append(routesToAdd, newRoute)
		}
	}
	f.mu.Unlock()

	// add new rules
	addedByPrefix, err := f.addPermissiveRulesForPrefixes(routesToAdd, "permitted route - ")
	if err != nil {
		return err
	}
	f.mu.Lock()
	for prefix, rules := range addedByPrefix {
		f.permittedRoutes[prefix] = rules
	}
	f.mu.Unlock()

	f.logger.Verbosef("Updated permitted routes: %v", newRoutes)
	return nil
}

// permitDaemon allows the daemon process through firewall
func (f *WindowsFirewall) permitDaemon(w weight) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	currentFile, err := os.Executable()
	if err != nil {
		return err
	}

	appID, err := wf.AppID(currentFile)
	if err != nil {
		return fmt.Errorf("could not get app id for %q: %w", currentFile, err)
	}
	conditions := []*wf.Match{
		{
			Field: wf.FieldALEAppID,
			Op:    wf.MatchTypeEqual,
			Value: appID,
		},
	}
	_, err = f.addRules("unrestricted traffic for daemon", w, conditions, wf.ActionPermit, protocolAll, directionBoth)
	return err
}

func (f *WindowsFirewall) BypassTunnel(luid uint64, listenPort uint16) error {
	f.mu.Lock()
	f.luid = luid
	f.mu.Unlock()
	if err := f.permitTunInterface(weightDaemonTraffic); err != nil {
		return fmt.Errorf("permitTunInterface failed: %w", err)
	}
	if err := f.permitListenPort(weightDaemonTraffic, listenPort); err != nil {
		return fmt.Errorf("permitListenPort failed: %w", err)
	}
	return nil
}

func (f *WindowsFirewall) Enable() error {
	f.mu.Lock()
	if f.killSwitchEnabled.Load() {
		f.mu.Unlock()
		f.logger.Verbosef("Kill switch already active, skipping activation")
		return nil
	}
	f.mu.Unlock()
	if err := f.permitDaemon(weightDaemonTraffic); err != nil {
		return fmt.Errorf("permitTailscaleService failed: %w", err)
	}
	if err := f.permitLoopback(weightDaemonTraffic); err != nil {
		return fmt.Errorf("permitLoopback failed: %w", err)
	}
	if err := f.permitDHCPv4(weightKnownTraffic); err != nil {
		return fmt.Errorf("permitDHCPv4 failed: %w", err)
	}

	if nettest.SupportsIPv6() {
		if err := f.permitDHCPv6(weightKnownTraffic); err != nil {
			return fmt.Errorf("permitDHCPv6 failed: %w", err)
		}

		if err := f.permitNDP(weightKnownTraffic); err != nil {
			return fmt.Errorf("permitNDP failed: %w", err)
		}
	}

	if err := f.blockAll(weightCatchAll); err != nil {
		return fmt.Errorf("blockAll failed: %w", err)
	}

	f.killSwitchEnabled.Store(true)
	return nil
}

func (f *WindowsFirewall) IsEnabled() bool {
	return f.killSwitchEnabled.Load()
}

func (f *WindowsFirewall) RemoveTunnelRules() error {
	f.mu.Lock()
	tunRulesCopy := make([]*wf.Rule, len(f.tunRules))
	copy(tunRulesCopy, f.tunRules)
	f.tunRules = nil
	f.mu.Unlock()
	if err := f.removeRules(tunRulesCopy); err != nil {
		f.logger.Errorf("Failed to remove tun rules: %v", err)
	}

	f.mu.Lock()
	permittedCopy := make(map[netip.Prefix][]*wf.Rule, len(f.permittedRoutes))
	for k, v := range f.permittedRoutes {
		permittedCopy[k] = v
	}
	f.permittedRoutes = make(map[netip.Prefix][]*wf.Rule)
	f.mu.Unlock()
	for prefix, rules := range permittedCopy {
		if err := f.removeRules(rules); err != nil {
			f.logger.Errorf("Failed to remove permitted route %s: %v", prefix, err)
		}
	}

	f.logger.Verbosef("Tunnel rules and permitted routes removed")
	return nil
}

func (f *WindowsFirewall) Disable() error {
	f.mu.Lock()
	defer f.mu.Unlock()
	if err := f.session.Close(); err != nil {
		f.logger.Errorf("Failed to close WFP session: %v", err)
	}
	f.killSwitchEnabled.Store(false)
	f.logger.Verbosef("Firewall rules and kill switch cleaned up")
	return nil
}

func (f *WindowsFirewall) permitLoopback(w weight) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	condition := []*wf.Match{
		{
			Field: wf.FieldFlags,
			Op:    wf.MatchTypeFlagsAllSet,
			Value: wf.ConditionFlagIsLoopback,
		},
	}
	_, err := f.addRules("on loopback", w, condition, wf.ActionPermit, protocolAll, directionBoth)
	return err
}

func (f *WindowsFirewall) permitListenPort(w weight, listenPort uint16) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	conditions := []*wf.Match{
		{Field: wf.FieldIPLocalInterface, Op: wf.MatchTypeEqual, Value: f.luid},
		{Field: wf.FieldIPProtocol, Op: wf.MatchTypeEqual, Value: wf.IPProtoUDP},
		{Field: wf.FieldIPLocalPort, Op: wf.MatchTypeEqual, Value: listenPort},
	}
	rules, err := f.addRules("WireGuard UDP", w, conditions, wf.ActionPermit, protocolAll, directionInbound)
	if err != nil {
		return err
	}
	f.tunRules = append(f.tunRules, rules...)
	return nil
}

func (f *WindowsFirewall) permitDHCPv6(w weight) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	var dhcpConditions = func(remoteAddrs ...any) []*wf.Match {
		conditions := []*wf.Match{
			{
				Field: wf.FieldIPProtocol,
				Op:    wf.MatchTypeEqual,
				Value: wf.IPProtoUDP,
			},
			{
				Field: wf.FieldIPLocalAddress,
				Op:    wf.MatchTypeEqual,
				Value: linkLocalRange,
			},
			{
				Field: wf.FieldIPLocalPort,
				Op:    wf.MatchTypeEqual,
				Value: uint16(546),
			},
			{
				Field: wf.FieldIPRemotePort,
				Op:    wf.MatchTypeEqual,
				Value: uint16(547),
			},
		}
		for _, a := range remoteAddrs {
			conditions = append(conditions, &wf.Match{
				Field: wf.FieldIPRemoteAddress,
				Op:    wf.MatchTypeEqual,
				Value: a,
			})
		}
		return conditions
	}
	conditions := dhcpConditions(linkLocalDHCPMulticast, siteLocalDHCPMulticast)
	if _, err := f.addRules("DHCP request", w, conditions, wf.ActionPermit, protocolV6, directionOutbound); err != nil {
		return err
	}
	conditions = dhcpConditions(linkLocalRange)
	if _, err := f.addRules("DHCP response", w, conditions, wf.ActionPermit, protocolV6, directionInbound); err != nil {
		return err
	}
	return nil
}

func (f *WindowsFirewall) permitDHCPv4(w weight) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	var dhcpConditions = func(remoteAddrs ...any) []*wf.Match {
		conditions := []*wf.Match{
			{
				Field: wf.FieldIPProtocol,
				Op:    wf.MatchTypeEqual,
				Value: wf.IPProtoUDP,
			},
			{
				Field: wf.FieldIPLocalPort,
				Op:    wf.MatchTypeEqual,
				Value: uint16(68),
			},
			{
				Field: wf.FieldIPRemotePort,
				Op:    wf.MatchTypeEqual,
				Value: uint16(67),
			},
		}
		for _, a := range remoteAddrs {
			conditions = append(conditions, &wf.Match{
				Field: wf.FieldIPRemoteAddress,
				Op:    wf.MatchTypeEqual,
				Value: a,
			})
		}
		return conditions
	}
	conditions := dhcpConditions(netaddr.IPv4(255, 255, 255, 255))
	if _, err := f.addRules("DHCP request", w, conditions, wf.ActionPermit, protocolV4, directionOutbound); err != nil {
		return err
	}

	conditions = dhcpConditions()
	if _, err := f.addRules("DHCP response", w, conditions, wf.ActionPermit, protocolV4, directionInbound); err != nil {
		return err
	}
	return nil
}

func (f *WindowsFirewall) permitNDP(w weight) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	// These are aliased according to:
	// https://social.msdn.microsoft.com/Forums/azure/en-US/eb2aa3cd-5f1c-4461-af86-61e7d43ccc23/filtering-icmp-by-type-code?forum=wfp
	fieldICMPType := wf.FieldIPLocalPort
	fieldICMPCode := wf.FieldIPRemotePort

	var icmpConditions = func(t, c uint16, remoteAddress any) []*wf.Match {
		conditions := []*wf.Match{
			{
				Field: wf.FieldIPProtocol,
				Op:    wf.MatchTypeEqual,
				Value: wf.IPProtoICMPV6,
			},
			{
				Field: fieldICMPType,
				Op:    wf.MatchTypeEqual,
				Value: t,
			},
			{
				Field: fieldICMPCode,
				Op:    wf.MatchTypeEqual,
				Value: c,
			},
		}
		if remoteAddress != nil {
			conditions = append(conditions, &wf.Match{
				Field: wf.FieldIPRemoteAddress,
				Op:    wf.MatchTypeEqual,
				Value: linkLocalRouterMulticast,
			})
		}
		return conditions
	}
	/* TODO: actually handle the hop limit somehow! The rules should vaguely be:
	 *  - icmpv6 133: must be outgoing, dst must be FF02::2/128, hop limit must be 255
	 *  - icmpv6 134: must be incoming, src must be FE80::/10, hop limit must be 255
	 *  - icmpv6 135: either incoming or outgoing, hop limit must be 255
	 *  - icmpv6 136: either incoming or outgoing, hop limit must be 255
	 *  - icmpv6 137: must be incoming, src must be FE80::/10, hop limit must be 255
	 */

	//
	// Router Solicitation Message
	// ICMP type 133, code 0. Outgoing.
	//
	conditions := icmpConditions(133, 0, linkLocalRouterMulticast)
	if _, err := f.addRules("NDP type 133", w, conditions, wf.ActionPermit, protocolV6, directionOutbound); err != nil {
		return err
	}

	//
	// Router Advertisement Message
	// ICMP type 134, code 0. Incoming.
	//
	conditions = icmpConditions(134, 0, linkLocalRange)
	if _, err := f.addRules("NDP type 134", w, conditions, wf.ActionPermit, protocolV6, directionInbound); err != nil {
		return err
	}

	//
	// Neighbor Solicitation Message
	// ICMP type 135, code 0. Bi-directional.
	//
	conditions = icmpConditions(135, 0, nil)
	if _, err := f.addRules("NDP type 135", w, conditions, wf.ActionPermit, protocolV6, directionBoth); err != nil {
		return err
	}

	//
	// Neighbor Advertisement Message
	// ICMP type 136, code 0. Bi-directional.
	//
	conditions = icmpConditions(136, 0, nil)
	if _, err := f.addRules("NDP type 136", w, conditions, wf.ActionPermit, protocolV6, directionBoth); err != nil {
		return err
	}

	//
	// Redirect Message
	// ICMP type 137, code 0. Incoming.
	//
	conditions = icmpConditions(137, 0, linkLocalRange)
	if _, err := f.addRules("NDP type 137", w, conditions, wf.ActionPermit, protocolV6, directionInbound); err != nil {
		return err
	}
	return nil
}

func (f *WindowsFirewall) blockAll(w weight) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	_, err := f.addRules("all", w, nil, wf.ActionBlock, protocolAll, directionBoth)
	return err
}

// addRules adds WFP rules with the given parameters
func (f *WindowsFirewall) addRules(name string, w weight, conditions []*wf.Match, action wf.Action, p protocol, d direction) ([]*wf.Rule, error) {
	f.mu.Lock()
	defer f.mu.Unlock()
	var rules []*wf.Rule
	for _, layer := range p.getLayers(d) {
		r, err := f.newRule(name, w, layer, conditions, action)
		if err != nil {
			return nil, err
		}
		if err := f.session.AddRule(r); err != nil {
			return nil, err
		}
		rules = append(rules, r)
	}
	return rules, nil
}

// getLayers returns the wf.LayerIDs where the rules should be added based on the protocol and direction.
func (p protocol) getLayers(d direction) []wf.LayerID {
	var layers []wf.LayerID
	if p == protocolAll || p == protocolV4 {
		if d == directionBoth || d == directionInbound {
			layers = append(layers, wf.LayerALEAuthRecvAcceptV4)
		}
		if d == directionBoth || d == directionOutbound {
			layers = append(layers, wf.LayerALEAuthConnectV4)
		}
	}
	if p == protocolAll || p == protocolV6 {
		if d == directionBoth || d == directionInbound {
			layers = append(layers, wf.LayerALEAuthRecvAcceptV6)
		}
		if d == directionBoth || d == directionOutbound {
			layers = append(layers, wf.LayerALEAuthConnectV6)
		}
	}
	return layers
}

func (f *WindowsFirewall) newRule(name string, w weight, layer wf.LayerID, conditions []*wf.Match, action wf.Action) (*wf.Rule, error) {
	f.mu.Lock()
	defer f.mu.Unlock()
	id, err := windows.GenerateGUID()
	if err != nil {
		return nil, err
	}
	return &wf.Rule{
		Name:       "WGTunnel-" + ruleName(action, layer, name),
		ID:         wf.RuleID(id),
		Provider:   f.providerID,
		Sublayer:   f.sublayerID,
		Layer:      layer,
		Weight:     uint64(w),
		Conditions: conditions,
		Action:     action,
	}, nil
}

func ruleName(action wf.Action, layerID wf.LayerID, name string) string {
	switch layerID {
	case wf.LayerALEAuthConnectV4:
		return fmt.Sprintf("%s outbound %s (IPv4)", action, name)
	case wf.LayerALEAuthConnectV6:
		return fmt.Sprintf("%s outbound %s (IPv6)", action, name)
	case wf.LayerALEAuthRecvAcceptV4:
		return fmt.Sprintf("%s inbound %s (IPv4)", action, name)
	case wf.LayerALEAuthRecvAcceptV6:
		return fmt.Sprintf("%s inbound %s (IPv6)", action, name)
	}
	return ""
}

// permitTunInterface allows tun interface through firewall, requires luid to be set
func (f *WindowsFirewall) permitTunInterface(w weight) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	condition := []*wf.Match{
		{
			Field: wf.FieldIPLocalInterface,
			Op:    wf.MatchTypeEqual,
			Value: f.luid,
		},
	}
	rules, err := f.addRules("on TUN", w, condition, wf.ActionPermit, protocolAll, directionBoth)
	if err != nil {
		return err
	}
	f.tunRules = append(f.tunRules, rules...)
	return nil
}
