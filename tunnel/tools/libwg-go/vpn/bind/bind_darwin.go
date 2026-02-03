//go:build darwin

package bind

import (
	"github.com/amnezia-vpn/amneziawg-go/conn"
	"github.com/amnezia-vpn/amneziawg-go/device"
)

func SetupBind(logger *device.Logger, bind conn.Bind) error {

	return nil // No fwmark on non-Linux; no-op
}
