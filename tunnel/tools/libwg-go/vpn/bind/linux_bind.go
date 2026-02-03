//go:build linux && !android

package bind

import (
	"fmt"
	"syscall"

	"github.com/amnezia-vpn/amneziawg-go/conn"
	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall/mark"
	"golang.org/x/sys/unix"
)

func SetupBind(logger *device.Logger, bind conn.Bind) error {
	stdBind, ok := bind.(*conn.StdNetBind)
	if !ok {
		return fmt.Errorf("failed to cast to StdNetBind")
	}
	stdBind.SetControl(func(network, address string, c syscall.RawConn) error {
		var opErr error
		err := c.Control(func(fd uintptr) {
			logger.Verbosef("Control called on socket FD %d - setting fwmark...", fd)
			if err := unix.SetsockoptInt(int(fd), unix.SOL_SOCKET, unix.SO_MARK, mark.LinuxBypassMarkNum); err != nil {
				opErr = err
				logger.Errorf("Failed to set fwmark on FD %d: %v", fd, err)
			} else {
				logger.Verbosef("Fwmark %d set on FD %d", mark.LinuxBypassMarkNum, fd)
			}
		})
		if err != nil {
			return err
		}
		return opErr
	})
	logger.Verbosef("Set control func on bind to apply fwmark on socket ops")
	return nil
}
