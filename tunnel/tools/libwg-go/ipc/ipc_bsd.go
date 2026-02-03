//go:build darwin || freebsd || openbsd

package ipc

import (
	"net"

	"github.com/amnezia-vpn/amneziawg-go/ipc"
	"github.com/wgtunnel/desktop/tunnel/shared"
)

func SetupIPC(name string) (net.Listener, error) {
	var socketDirectory = "/run/wgtunnel"

	uapiFile, err := ipc.UAPIOpen(socketDirectory, name)
	if err != nil {
		shared.LogError("IPC", "UAPIOpen: %v", err)
		return nil, err
	}

	uapi, err := ipc.UAPIListen(socketDirectory, name, uapiFile)
	if err != nil {
		uapiFile.Close()
		shared.LogError("IPC", "UAPIListen: %v", err)
		return nil, err
	}

	return uapi, nil
}
