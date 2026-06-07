// SPDX-License-Identifier: Apache-2.0
//
// Copyright © 2026 WG Tunnel.

//go:build darwin

package osfirewall

import (
	"net/netip"
	"sync/atomic"

	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall"
)

// DarwinFirewall is a no-op firewall stub for macOS.
// Kill-switch is not yet implemented on macOS.
type DarwinFirewall struct {
	enabled    atomic.Bool
	persistent atomic.Bool
	localNets  []netip.Prefix
}

func New(_ *device.Logger) (firewall.Firewall, error) {
	return &DarwinFirewall{}, nil
}

func (f *DarwinFirewall) SetPersist(enabled bool) {
	f.persistent.Store(enabled)
}

func (f *DarwinFirewall) IsPersistent() bool {
	return f.persistent.Load()
}

func (f *DarwinFirewall) Enable() error {
	f.enabled.Store(true)
	return nil
}

func (f *DarwinFirewall) IsEnabled() bool {
	return f.enabled.Load()
}

func (f *DarwinFirewall) Disable() error {
	f.enabled.Store(false)
	f.localNets = nil
	return nil
}

func (f *DarwinFirewall) AllowLocalNetworks(prefixes []netip.Prefix) error {
	f.localNets = prefixes
	return nil
}

func (f *DarwinFirewall) RemoveLocalNetworks() error {
	f.localNets = nil
	return nil
}

func (f *DarwinFirewall) IsAllowLocalNetworksEnabled() bool {
	return len(f.localNets) > 0
}
