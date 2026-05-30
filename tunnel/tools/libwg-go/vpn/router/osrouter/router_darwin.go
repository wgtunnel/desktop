// SPDX-License-Identifier: Apache-2.0
//
// Copyright © 2026 WG Tunnel.

//go:build darwin

package osrouter

import (
	"fmt"
	"net/netip"
	"os/exec"
	"strings"

	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/amnezia-vpn/amneziawg-go/tun"
	"github.com/wgtunnel/desktop/tunnel/vpn/dns"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall"
	"github.com/wgtunnel/desktop/tunnel/vpn/router"
)

type darwinRouter struct {
	iface      string
	logger     *device.Logger
	prevConfig *router.Config
	// savedGateway holds the original default gateway (IP and interface) saved
	// before we install VPN routes, so we can restore it on Close.
	savedGateway    string
	savedGatewayIf  string
}

func New(iface string, _ firewall.Firewall, _ tun.Device, logger *device.Logger) (router.Router, error) {
	return &darwinRouter{
		iface:  iface,
		logger: logger,
	}, nil
}

func (r *darwinRouter) Set(c *router.Config) error {
	newC := c
	if newC == nil {
		newC = &router.Config{}
	}

	if newC.Equal(r.prevConfig) {
		r.logger.Verbosef("Config unchanged, skipping")
		return nil
	}

	// Bring the utun interface up
	if err := exec.Command("ifconfig", r.iface, "up").Run(); err != nil {
		r.logger.Verbosef("ifconfig up: %v", err)
	}

	// Assign tunnel addresses
	for _, addr := range newC.TunnelAddrs {
		if err := r.addAddress(addr); err != nil {
			r.logger.Errorf("add address %v: %v", addr, err)
		}
	}

	// Save the current default gateway so we can restore it on Close.
	// Do this once: if prevConfig is already set we already saved it.
	if r.prevConfig == nil {
		r.savedGateway, r.savedGatewayIf = getDefaultGatewayAndIface()
		r.logger.Verbosef("Saved default gateway: %s via %s", r.savedGateway, r.savedGatewayIf)
	}

	// Add specific host routes for peer endpoints via physical gateway BEFORE general routes.
	// This prevents routing loops when AllowedIPs includes 0.0.0.0/0: without these host
	// routes, WireGuard's own UDP packets to the server would be sent through the tunnel.
	physGw := r.savedGateway
	for _, ep := range newC.PeerEndpoints {
		if err := r.addHostRoute(ep.Addr(), physGw); err != nil {
			r.logger.Verbosef("add peer host route %v via %v: %v", ep.Addr(), physGw, err)
		}
	}

	// Set routes. For the default route (0.0.0.0/0 or ::/0) we delete the existing
	// default first so our utun route takes precedence.
	for _, route := range newC.Routes {
		if route == (netip.PrefixFrom(netip.IPv4Unspecified(), 0)) ||
			route == (netip.PrefixFrom(netip.IPv6Unspecified(), 0)) {
			r.replaceDefaultRoute(route)
		} else {
			if err := r.addRoute(route); err != nil {
				r.logger.Verbosef("add route %v: %v", route, err)
			}
		}
	}

	// Configure DNS on the physical interface (savedGatewayIf, e.g. "en0").
	// utun interfaces are VPN tunnels not listed in networksetup, so we target
	// the physical service that had the default route before VPN routes were set.
	if len(newC.DNS) > 0 {
		if err := dns.SetDns(r.savedGatewayIf, newC.DNS, newC.SearchDomains, newC.HasAnyDefaultRoute(), r.logger); err != nil {
			r.logger.Errorf("set DNS: %v", err)
		}
	}

	r.prevConfig = newC.Clone()
	return nil
}

func (r *darwinRouter) Close() error {
	if r.prevConfig != nil {
		if err := dns.RevertDns(r.savedGatewayIf, r.logger); err != nil {
			r.logger.Verbosef("revert DNS: %v", err)
		}
	}

	// Remove routes we added
	if r.prevConfig != nil {
		for _, route := range r.prevConfig.Routes {
			if route == (netip.PrefixFrom(netip.IPv4Unspecified(), 0)) ||
				route == (netip.PrefixFrom(netip.IPv6Unspecified(), 0)) {
				// Restore original default route instead of just deleting ours
				r.restoreDefaultRoute(route)
			} else {
				r.deleteRoute(route)
			}
		}
		for _, addr := range r.prevConfig.TunnelAddrs {
			r.deleteAddress(addr)
		}
		// Remove peer endpoint host routes
		for _, ep := range r.prevConfig.PeerEndpoints {
			r.deleteHostRoute(ep.Addr())
		}
	}

	r.savedGateway = ""
	r.savedGatewayIf = ""
	r.logger.Verbosef("Darwin router closed")
	return nil
}

func (r *darwinRouter) GetPhysicalInterfaceIndex() uint32 {
	return 0
}

func (r *darwinRouter) addAddress(prefix netip.Prefix) error {
	addr := prefix.Addr()
	if addr.Is4() {
		out, err := exec.Command("ifconfig", r.iface, addr.String(), addr.String()).CombinedOutput()
		if err != nil && !strings.Contains(string(out), "File exists") {
			return fmt.Errorf("ifconfig inet: %w (output: %s)", err, out)
		}
	} else {
		out, err := exec.Command("ifconfig", r.iface, "inet6", addr.String(), "prefixlen", fmt.Sprintf("%d", prefix.Bits())).CombinedOutput()
		if err != nil && !strings.Contains(string(out), "File exists") {
			return fmt.Errorf("ifconfig inet6: %w (output: %s)", err, out)
		}
	}
	return nil
}

func (r *darwinRouter) deleteAddress(prefix netip.Prefix) {
	addr := prefix.Addr()
	if addr.Is4() {
		exec.Command("ifconfig", r.iface, "-alias", addr.String()).Run()
	} else {
		exec.Command("ifconfig", r.iface, "inet6", addr.String(), "delete").Run()
	}
}

func (r *darwinRouter) addRoute(prefix netip.Prefix) error {
	var args []string
	if prefix.Addr().Is4() {
		args = []string{"-n", "add", "-net", prefix.String(), "-interface", r.iface}
	} else {
		args = []string{"-n", "add", "-inet6", prefix.String(), "-interface", r.iface}
	}
	out, err := exec.Command("route", args...).CombinedOutput()
	if err != nil && !strings.Contains(string(out), "File exists") && !strings.Contains(string(out), "exists") {
		return fmt.Errorf("route add: %w (output: %s)", err, out)
	}
	return nil
}

func (r *darwinRouter) deleteRoute(prefix netip.Prefix) {
	var args []string
	if prefix.Addr().Is4() {
		args = []string{"-n", "delete", "-net", prefix.String(), "-interface", r.iface}
	} else {
		args = []string{"-n", "delete", "-inet6", prefix.String(), "-interface", r.iface}
	}
	exec.Command("route", args...).Run()
}

// addHostRoute adds a /32 (or /128) host route for addr via the given gateway IP.
func (r *darwinRouter) addHostRoute(addr netip.Addr, gw string) error {
	if gw == "" {
		return fmt.Errorf("no default gateway found")
	}
	var args []string
	if addr.Is4() {
		args = []string{"-n", "add", "-host", addr.String(), gw}
	} else {
		args = []string{"-n", "add", "-inet6", "-host", addr.String(), gw}
	}
	out, err := exec.Command("route", args...).CombinedOutput()
	if err != nil && !strings.Contains(string(out), "exists") {
		return fmt.Errorf("route add host: %w (output: %s)", err, out)
	}
	return nil
}

func (r *darwinRouter) deleteHostRoute(addr netip.Addr) {
	var args []string
	if addr.Is4() {
		args = []string{"-n", "delete", "-host", addr.String()}
	} else {
		args = []string{"-n", "delete", "-inet6", "-host", addr.String()}
	}
	exec.Command("route", args...).Run()
}

// getDefaultGatewayAndIface returns the physical default IPv4 gateway IP and interface name.
// It ignores routes via utun* interfaces (stale VPN routes from ungraceful shutdown).
func getDefaultGatewayAndIface() (gw string, iface string) {
	out, err := exec.Command("route", "-n", "get", "default").Output()
	if err == nil {
		var parsedGw, parsedIface string
		for _, line := range strings.Split(string(out), "\n") {
			line = strings.TrimSpace(line)
			if strings.HasPrefix(line, "gateway:") {
				parsedGw = strings.TrimSpace(strings.TrimPrefix(line, "gateway:"))
			}
			if strings.HasPrefix(line, "interface:") {
				parsedIface = strings.TrimSpace(strings.TrimPrefix(line, "interface:"))
			}
		}
		// Only use this result if the default route is via a physical interface
		// (not a utun* tunnel interface which would indicate a stale VPN route).
		if parsedGw != "" && !strings.HasPrefix(parsedIface, "utun") {
			return parsedGw, parsedIface
		}
	}
	// Fallback: scan netstat output for the first default route via a physical interface.
	// This handles the case where a previous VPN session left a stale utun default route.
	return getPhysicalGatewayFromNetstat()
}

// getPhysicalGatewayFromNetstat finds the default gateway by scanning the routing table,
// skipping any routes via utun* interfaces or link-layer entries.
func getPhysicalGatewayFromNetstat() (gw string, iface string) {
	out, err := exec.Command("netstat", "-rn", "-f", "inet").Output()
	if err != nil {
		return "", ""
	}
	for _, line := range strings.Split(string(out), "\n") {
		fields := strings.Fields(line)
		if len(fields) < 4 || fields[0] != "default" {
			continue
		}
		netif := fields[3]
		gwAddr := fields[1]
		if strings.HasPrefix(netif, "utun") || strings.HasPrefix(gwAddr, "link#") {
			continue
		}
		return gwAddr, netif
	}
	return "", ""
}

// restoreDefaultRoute deletes the tunnel default route and restores the original gateway.
// For IPv6, we only saved an IPv4 gateway so we just remove the VPN route without restoring.
func (r *darwinRouter) restoreDefaultRoute(prefix netip.Prefix) {
	if prefix.Addr().Is4() {
		exec.Command("route", "-n", "delete", "default").Run()
		if r.savedGateway == "" {
			return
		}
		out, err := exec.Command("route", "-n", "add", "default", r.savedGateway).CombinedOutput()
		if err != nil {
			r.logger.Verbosef("restore default route (ipv4): %v (%s)", err, out)
		}
	} else {
		// Just remove the VPN IPv6 default route; we don't track the original IPv6 gateway.
		exec.Command("route", "-n", "delete", "-inet6", "default").Run()
	}
}

// replaceDefaultRoute deletes the existing default route and adds one via the tunnel.
// This ensures our route takes precedence over the physical default.
func (r *darwinRouter) replaceDefaultRoute(prefix netip.Prefix) {
	// Delete old default
	if prefix.Addr().Is4() {
		exec.Command("route", "-n", "delete", "default").Run()
		out, err := exec.Command("route", "-n", "add", "-net", "default", "-interface", r.iface).CombinedOutput()
		if err != nil {
			r.logger.Verbosef("replace default route (ipv4): %v (%s)", err, out)
		}
	} else {
		exec.Command("route", "-n", "delete", "-inet6", "default").Run()
		out, err := exec.Command("route", "-n", "add", "-inet6", "default", "-interface", r.iface).CombinedOutput()
		if err != nil {
			r.logger.Verbosef("replace default route (ipv6): %v (%s)", err, out)
		}
	}
}
