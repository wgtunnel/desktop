//go:build windows

package dns

import (
	"context"
	"net"
)

// GetBypassDialer returns a standard dialer for Windows.
// Since the process is already bypassed in the Windows Firewall,
// no special socket marking or binding is required.
func GetBypassDialer(preferIpv6 bool) (*net.Dialer, error) {
	return &net.Dialer{}, nil
}

// CustomResolver returns a standard net.Resolver for Windows.
func CustomResolver(preferIpv6 bool) *net.Resolver {
	return &net.Resolver{
		PreferGo: true,
		Dial: func(ctx context.Context, network, address string) (net.Conn, error) {
			d, _ := GetBypassDialer(preferIpv6)
			return d.DialContext(ctx, network, address)
		},
	}
}
