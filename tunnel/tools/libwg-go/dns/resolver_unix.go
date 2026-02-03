//go:build unix

package dns

import (
	"context"
	"net"
	"syscall"

	"github.com/wgtunnel/desktop/tunnel/vpn/firewall/mark"
)

// GetBypassDialer returns a dialer that bypasses the VPN via SO_MARK
func GetBypassDialer(preferIpv6 bool) (*net.Dialer, error) {
	return &net.Dialer{
		Control: func(network, address string, c syscall.RawConn) error {
			var opErr error
			err := c.Control(func(fd uintptr) {
				opErr = syscall.SetsockoptInt(int(fd), syscall.SOL_SOCKET, syscall.SO_MARK, mark.LinuxBootstrapMarkNum)
			})
			if err != nil {
				return err
			}
			return opErr
		},
	}, nil
}

// CustomResolver is still needed for the dnsproxy Bootstrap field
func CustomResolver(preferIpv6 bool) *net.Resolver {
	return &net.Resolver{
		PreferGo: true,
		Dial: func(ctx context.Context, network, address string) (net.Conn, error) {
			d, err := GetBypassDialer(preferIpv6)
			if err != nil {
				return nil, err
			}
			return d.DialContext(ctx, network, address)
		},
	}
}
