//go:build windows

package ipc

import (
	"net"

	"github.com/amnezia-vpn/amneziawg-go/ipc"
	"github.com/wgtunnel/desktop/tunnel/shared"
)

func SetupIPC(name string) (net.Listener, error) {
	uapi, err := ipc.UAPIListen(name)
	if err != nil {
		shared.LogError("IPC", "UAPIListen: %v", err)
		return nil, err
	}

	return uapi, nil
}
