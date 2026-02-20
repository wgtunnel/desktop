//go:build windows

package dns

import (
	"context"
	"encoding/binary"
	"net"
	"strings"
	"syscall"
	"unsafe"

	"golang.org/x/sys/windows"
)

const (
	IP_UNICAST_IF   = 0x1f
	IPV6_UNICAST_IF = 0x1f
)

// GetBypassDialer returns a net.Dialer that forces outbound DNS queries
// to leave via the physical interface on Windows for tunnel bootstrapping to prevent request from getting
// routed back into the tun.
func GetBypassDialer(preferIPv6 bool, physicalIfIndex uint32) (*net.Dialer, error) {
	// TODO handle prefer ipv6
	d := &net.Dialer{
		Control: func(network, address string, c syscall.RawConn) error {
			return c.Control(func(fd uintptr) {
				if physicalIfIndex == 0 {
					return
				}
				handle := windows.Handle(fd)

				if strings.HasPrefix(network, "tcp4") || strings.HasPrefix(network, "udp4") {
					// IP_UNICAST_IF for IPv4
					idx := physicalIfIndex
					var b [4]byte
					binary.BigEndian.PutUint32(b[:], idx)
					idx = *(*uint32)(unsafe.Pointer(&b[0]))
					windows.SetsockoptInt(handle, windows.IPPROTO_IP, IP_UNICAST_IF, int(idx))
				} else if strings.HasPrefix(network, "tcp6") || strings.HasPrefix(network, "udp6") {
					windows.SetsockoptInt(handle, windows.IPPROTO_IPV6, IPV6_UNICAST_IF, int(physicalIfIndex))
				}
			})
		},
	}
	return d, nil
}

// CustomResolver returns a standard net.Resolver for Windows.
func CustomResolver(preferIpv6 bool, physicalIfIndex uint32) *net.Resolver {
	return &net.Resolver{
		PreferGo: true,
		Dial: func(ctx context.Context, network, address string) (net.Conn, error) {
			d, _ := GetBypassDialer(preferIpv6, physicalIfIndex)
			return d.DialContext(ctx, network, address)
		},
	}
}
