//go:build !android

package proxy

/*
#include <stdint.h>
typedef void (*StatusCodeCallback)(int32_t handle, int32_t status);
*/
import "C"
import (
	"context"
	"sync"
	"syscall"

	"os"
	"os/signal"

	"github.com/amnezia-vpn/amneziawg-go/conn"
	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/amnezia-vpn/amneziawg-go/tun/netstack"
	wireproxyawg "github.com/artem-russkikh/wireproxy-awg"
	ipc "github.com/wgtunnel/desktop/tunnel/ipc"
	"github.com/wgtunnel/desktop/tunnel/shared"
	"github.com/wgtunnel/desktop/tunnel/util"
)

var (
	tag                  = "AwgProxy"
	virtualTunnelHandles = make(map[int32]*wireproxyawg.VirtualTun)
	ctx                  context.Context
	cancelFunc           context.CancelFunc
)

func init() {
	// Handle signals for clean shutdown
	go handleSignals()
}

func handleSignals() {
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)
	<-sigs
	awgProxyTurnOffAll()
	os.Exit(0)
}

//export awgProxyTurnOn
func awgProxyTurnOn(config *C.char, callback C.StatusCodeCallback) C.int {
	handle, err2 := util.GenerateHandle(virtualTunnelHandles)
	if err2 != nil {
		shared.LogError(tag, "Unable to find empty handle", err2)
		return C.int(-1)
	}

	shared.StoreTunnelCallback(handle, shared.StatusCodeCallback(callback))

	goConfig := C.GoString(config)

	conf, err := wireproxyawg.ParseConfigString(goConfig)
	if err != nil {
		shared.LogError(tag, "Invalid config file", err)
		return C.int(-1)
	}

	setting, err := wireproxyawg.CreateIPCRequest(conf.Device, false)
	if err != nil {
		shared.LogError(tag, "Create IPC request failed", err)
		return C.int(-1)
	}

	tun, tnet, err := netstack.CreateNetTUN(setting.DeviceAddr, setting.DNS, setting.MTU)
	if err != nil {
		shared.LogError(tag, "Create TUN failed", err)
		return C.int(-1)
	}

	name, err := tun.Name()
	if err != nil {
		shared.LogError(tag, "Get TUN name failed", err)
		return C.int(-1)
	}

	bind := conn.NewDefaultBind()

	statusCB := func(code device.StatusCode) {
		// use goroutine to avoid any blocking from JNA
		go shared.NotifyStatusCode(handle, int32(code))
	}

	dev := device.NewDevice(tun, bind, shared.NewLogger("Tun/"+name), false, statusCB)

	err = dev.IpcSet(setting.IpcRequest)
	if err != nil {
		shared.LogError(tag, "Ipc setting failed", err)
		return C.int(-1)
	}

	uapi, _ := ipc.SetupIPC(name)

	go func() {
		for {
			connection, err := uapi.Accept()
			if err != nil {
				return
			}
			go dev.IpcHandle(connection)
		}
	}()

	err = dev.Up()
	if err != nil {
		shared.LogError(tag, "Failed to bring up device", err)
		uapi.Close()
		dev.Close()
		return C.int(-1)
	}

	virtualTun := &wireproxyawg.VirtualTun{
		Tnet:           tnet,
		Dev:            dev,
		Logger:         shared.NewLogger("Proxy"),
		Uapi:           uapi,
		Conf:           conf.Device,
		PingRecord:     make(map[string]uint64),
		PingRecordLock: new(sync.Mutex),
	}

	virtualTunnelHandles[handle] = virtualTun

	// Create cancellable context
	ctx, cancelFunc = context.WithCancel(context.Background())

	// Spawn all routines with context
	for _, spawner := range conf.Routines {
		shared.LogDebug(tag, "Spawning routine..")
		go func(s wireproxyawg.RoutineSpawner) {
			if err := s.SpawnRoutine(ctx, virtualTun); err != nil {
				shared.LogError(tag, "Routine failed: %v", err)
			}
		}(spawner)
	}

	shared.LogDebug(tag, "Done starting proxy and tunnel")
	return C.int(handle)
}

func awgUpdateProxyTunnelPeers(tunnelHandle int32, settings string) int32 {
	handle, ok := virtualTunnelHandles[tunnelHandle]
	if !ok {
		shared.LogError(tag, "Tunnel is not up")
		return -1
	}

	conf, err := wireproxyawg.ParseConfigString(settings)
	if err != nil {
		shared.LogError(tag, "Invalid config file", err)
		return -1
	}

	ipcRequest, err := wireproxyawg.CreatePeerIPCRequest(conf.Device)
	if err != nil {
		shared.LogError(tag, "CreateIPCRequest: %v", err)
		return -1
	}

	err = handle.Dev.IpcSet(ipcRequest.IpcRequest)
	if err != nil {
		shared.LogError(tag, "IpcSet: %v", err)
		return -1
	}

	shared.LogDebug(tag, "Configuration updated successfully")
	return 0
}

//export awgProxyGetConfig
func awgProxyGetConfig(tunnelHandle C.int) *C.char {
	goTunnelHandle := int32(tunnelHandle)
	handle, ok := virtualTunnelHandles[goTunnelHandle]
	if !ok {
		shared.LogError(tag, "Tunnel is not up")
		return nil
	}
	settings, err := handle.Dev.IpcGet()
	if err != nil {
		shared.LogError(tag, "Failed to get device config: %v", err)
		return nil
	}
	return C.CString(settings)
}

//export awgProxyTurnOffAll
func awgProxyTurnOffAll() {
	if cancelFunc != nil {
		shared.LogDebug(tag, "Stopping proxy routines..")
		cancelFunc()
		cancelFunc = nil
	}
	handles := make([]int32, 0, len(virtualTunnelHandles))
	for h := range virtualTunnelHandles {
		handles = append(handles, h)
	}
	for _, handle := range handles {
		awgProxyTurnOff(C.int(handle))
	}
	virtualTunnelHandles = make(map[int32]*wireproxyawg.VirtualTun)
	shared.LogDebug(tag, "Proxy fully reset: %d handles closed", len(handles))
}

//export awgProxyTurnOff
func awgProxyTurnOff(virtualTunnelHandle C.int) {
	goVirtualTunnelHandle := int32(virtualTunnelHandle)
	virtualTun, ok := virtualTunnelHandles[goVirtualTunnelHandle]
	if !ok {
		shared.LogError(tag, "Tunnel handle %d not found", goVirtualTunnelHandle)
		return
	}
	shared.LogDebug(tag, "Tearing down tunnel %d", goVirtualTunnelHandle)

	// Disable UAPI listener and underlying file
	if virtualTun.Uapi != nil {
		virtualTun.Uapi.Close()
	}

	if virtualTun.Dev != nil {
		virtualTun.Dev.Close()
	}

	delete(virtualTunnelHandles, goVirtualTunnelHandle)
	shared.LogDebug(tag, "Tunnel %d fully closed (UAPI/Dev/Bind purged)", goVirtualTunnelHandle)
}
