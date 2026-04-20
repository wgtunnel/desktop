// SPDX-License-Identifier: Apache-2.0
//
// Copyright © 2026 WG Tunnel.

//go:build darwin

package dns

import (
	"fmt"
	"net/netip"
	"os/exec"
	"strings"

	"github.com/amnezia-vpn/amneziawg-go/device"
)

// SetDns configures DNS servers on macOS using networksetup.
func SetDns(iface string, dns []netip.Addr, searchDomains []string, _ bool, logger *device.Logger) error {
	if len(dns) == 0 {
		return nil
	}

	var addrs []string
	for _, d := range dns {
		addrs = append(addrs, d.String())
	}

	// Apply DNS to the utun interface via scutil or networksetup.
	// networksetup requires the service name, not the interface name, so we
	// look it up dynamically.
	service, err := serviceForInterface(iface)
	if err != nil {
		logger.Verbosef("dns_darwin: could not find service for %s, skipping DNS config: %v", iface, err)
		return nil
	}

	args := append([]string{"-setdnsservers", service}, addrs...)
	if out, err := exec.Command("networksetup", args...).CombinedOutput(); err != nil {
		return fmt.Errorf("networksetup -setdnsservers: %w (output: %s)", err, out)
	}

	if len(searchDomains) > 0 {
		sdArgs := append([]string{"-setsearchdomains", service}, searchDomains...)
		if out, err := exec.Command("networksetup", sdArgs...).CombinedOutput(); err != nil {
			logger.Verbosef("dns_darwin: -setsearchdomains failed: %v (output: %s)", err, out)
		}
	}

	return nil
}

// RevertDns restores DNS to automatic (DHCP) configuration.
func RevertDns(iface string, logger *device.Logger) error {
	service, err := serviceForInterface(iface)
	if err != nil {
		logger.Verbosef("dns_darwin: could not find service for %s, skipping DNS revert: %v", iface, err)
		return nil
	}

	if out, err := exec.Command("networksetup", "-setdnsservers", service, "empty").CombinedOutput(); err != nil {
		logger.Verbosef("dns_darwin: revert DNS failed: %v (output: %s)", err, out)
	}
	return nil
}

// serviceForInterface returns the networksetup service name for a given BSD
// interface name (e.g. "utun3" → "WG Tunnel VPN").
func serviceForInterface(iface string) (string, error) {
	out, err := exec.Command("networksetup", "-listallhardwareports").CombinedOutput()
	if err != nil {
		return "", fmt.Errorf("listallhardwareports: %w", err)
	}

	var lastService string
	for _, line := range strings.Split(string(out), "\n") {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "Hardware Port:") {
			lastService = strings.TrimPrefix(line, "Hardware Port:")
			lastService = strings.TrimSpace(lastService)
		} else if strings.HasPrefix(line, "Device:") {
			dev := strings.TrimSpace(strings.TrimPrefix(line, "Device:"))
			if dev == iface {
				return lastService, nil
			}
		}
	}
	return "", fmt.Errorf("interface %s not found in networksetup output", iface)
}
