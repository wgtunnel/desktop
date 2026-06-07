// SPDX-License-Identifier: Apache-2.0
//
// Copyright © 2026 WG Tunnel.

//go:build darwin

package dns

import (
	"context"
	"net"
)

// GetBypassDialer returns a plain dialer on macOS.
// SO_MARK is Linux-specific; on macOS the bypass is handled at the routing
// layer rather than via socket marks.
func GetBypassDialer(_ bool, _ uint32) (*net.Dialer, error) {
	return &net.Dialer{}, nil
}

// CustomResolver returns a standard resolver on macOS.
func CustomResolver(preferIpv6 bool, physicalIfIndex uint32) *net.Resolver {
	return &net.Resolver{
		PreferGo: true,
		Dial: func(ctx context.Context, network, address string) (net.Conn, error) {
			d, err := GetBypassDialer(preferIpv6, physicalIfIndex)
			if err != nil {
				return nil, err
			}
			return d.DialContext(ctx, network, address)
		},
	}
}
