//go:build !android

package vpn

/*
#include <stdint.h>
typedef void (*StatusCodeCallback)(int32_t handle, int32_t status);
*/
import "C"
import (
	"context"
	"errors"
	"net"
	"net/netip"
	"os"
	"os/signal"
	"sync"
	"sync/atomic"
	"syscall"

	"github.com/amnezia-vpn/amneziawg-go/conn"
	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/amnezia-vpn/amneziawg-go/tun"
	wireproxyawg "github.com/artem-russkikh/wireproxy-awg"
	"github.com/wgtunnel/desktop/tunnel/constants"
	"github.com/wgtunnel/desktop/tunnel/dns"
	"github.com/wgtunnel/desktop/tunnel/ipc"
	"github.com/wgtunnel/desktop/tunnel/shared"
	"github.com/wgtunnel/desktop/tunnel/util"
	bind2 "github.com/wgtunnel/desktop/tunnel/vpn/bind"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall/osfirewall/firewallmgr"
	"github.com/wgtunnel/desktop/tunnel/vpn/router"
	"github.com/wgtunnel/desktop/tunnel/vpn/router/osrouter"
)

type TunnelHandle struct {
	device         *device.Device
	uapi           net.Listener
	router         router.Router
	cancel         context.CancelFunc
	needsResolving atomic.Bool
}

var (
	tag              = "AwgVPN"
	tunnelHandles    = make(map[int32]*TunnelHandle)
	resolvingHandles = sync.Map{}
	logger           = shared.NewLogger(tag)
)

func init() {
	// handle shutdown signals
	go handleSignals()
}

func handleSignals() {
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)
	<-sigs
	awgTurnOffAll()
	os.Exit(0)
}

//export awgTurnOn
func awgTurnOn(settings *C.char, callback C.StatusCodeCallback) C.int {
	handleID, err := util.GenerateHandle(tunnelHandles)
	if err != nil {
		shared.LogError(tag, "Unable to find empty handle", err)
		return C.int(-1)
	}

	shared.StoreTunnelCallback(handleID, shared.StatusCodeCallback(callback))

	h := &TunnelHandle{}
	var success bool

	defer func() {
		if !success {
			shared.LogDebug(tag, "Startup failed, cleaning up partial resources for handle %d", handleID)
			h.close()
			resolvingHandles.Delete(handleID)
		}
	}()

	goSettings := C.GoString(settings)
	conf, err := wireproxyawg.ParseConfigString(goSettings)
	if err != nil {
		shared.LogError(tag, "Invalid config file", err)
		return C.int(-1)
	}

	// Create a context to manage resolution goroutines
	tunnelCtx, tunnelCancel := context.WithCancel(context.Background())
	h.cancel = tunnelCancel

	// Check for peers needing resolution, but we wait to start resolution until the firewall bypasses are set
	type peerToResolve struct {
		index int
		host  string
	}
	var resolutionQueue []peerToResolve

	for i := range conf.Device.Peers {
		peer := &conf.Device.Peers[i]
		if peer.NeedsResolution() {
			host, port, err := net.SplitHostPort(*peer.Endpoint)
			if err != nil {
				shared.LogError(tag, "Failed to parse endpoint", err)
				continue
			}
			// set dummy, non-routable address with original port
			dummyEndpoint := constants.DummyAddress + ":" + port
			peer.Endpoint = &dummyEndpoint

			resolutionQueue = append(resolutionQueue, peerToResolve{i, host})
		}
	}

	tunnel, err := tun.CreateTUN(constants.IfaceName, conf.Device.MTU)
	if err != nil {
		shared.LogError(tag, "Create TUN failed", err)
		return C.int(-1)
	}

	bind := conn.NewDefaultBind()
	if err := bind2.SetupBind(logger, bind); err != nil {
		tunnel.Close()
		return C.int(-1)
	}

	statusCB := func(code device.StatusCode) {
		go shared.NotifyStatusCode(handleID, int32(code))
	}

	h.device = device.NewDevice(tunnel, bind, logger, false, statusCB)

	var listenPort uint16 = 0
	if conf.Device.ListenPort != nil {
		listenPort = uint16(*conf.Device.ListenPort)
	}

	_, port, err := h.device.Bind().Open(listenPort)
	if err != nil {
		shared.LogError(tag, "Failed to open bind", err)
		return C.int(-1)
	}

	ifaceName, _ := tunnel.Name()
	uapi, err := ipc.SetupIPC(ifaceName)
	if err != nil {
		shared.LogError(tag, "Setup IPC failed", err)
		return C.int(-1)
	}
	h.uapi = uapi

	go func(d *device.Device, l net.Listener) {
		for {
			connection, err := l.Accept()
			if err != nil {
				return
			}
			go d.IpcHandle(connection)
		}
	}(h.device, h.uapi)

	ipcRequest, err := wireproxyawg.CreateIPCRequest(conf.Device, false)
	if err != nil {
		return C.int(-1)
	}
	if err := h.device.IpcSet(ipcRequest.IpcRequest); err != nil {
		return C.int(-1)
	}

	fw, err := newFirewall()
	if err != nil {
		return C.int(-1)
	}

	r, err := newRouter(ifaceName, fw, tunnel)
	if err != nil {
		return C.int(-1)
	}
	h.router = r

	if err := h.device.Up(); err != nil {
		return C.int(-1)
	}

	// parse config to router config for router/fw
	routerCfg, err := parseToRouterConfig(conf, port)
	if err != nil {
		return C.int(-1)
	}
	if err := h.router.Set(routerCfg); err != nil {
		return C.int(-1)
	}

	// try to resolve DNS to replace our dummy endpoints
	for _, p := range resolutionQueue {
		go resolveAndUpdatePeer(tunnelCtx, handleID, conf, p.index, p.host)
	}

	success = true
	tunnelHandles[handleID] = h
	shared.LogDebug(tag, "Device started successfully; DNS bypasses active for handle %d", handleID)

	return C.int(handleID)
}

// resolveAndUpdatePeer resolves the host and updates the peer's endpoint if successful.
func resolveAndUpdatePeer(ctx context.Context, tunnelHandle int32, conf *wireproxyawg.Configuration, peerIndex int, host string) {

	resolvingHandles.Store(tunnelHandle, true)
	shared.NotifyStatusCode(tunnelHandle, shared.StatusResolvingDNS)

	select {
	case <-ctx.Done():
		shared.LogDebug(tag, "Tunnel context cancelled, stopping resolver for %s", host)
		resolvingHandles.Delete(tunnelHandle)
		return
	default:
	}

	opts := dns.DefaultOptions()
	// TODO make configurable by user
	preferIPv6 := false

	resolved, err := dns.ResolveWithBackoff(ctx, host, opts, preferIPv6, logger)
	if err != nil {
		shared.LogError(tag, "Permanent failure resolving %s: %v", host, err)
		return
	}
	shared.LogDebug(tag, "Successfully resolved the tunnel peer endpoints..")

	var ip netip.Addr
	if preferIPv6 && len(resolved.V6) > 0 {
		ip = resolved.V6[0]
		shared.LogDebug(tag, "Successfully set peer endpoint to preferred resolved ipv6..")
	} else if len(resolved.V4) > 0 {
		ip = resolved.V4[0]
		shared.LogDebug(tag, "Successfully set peer endpoint to resolved ipv4..")
	} else {
		shared.LogError(tag, "No suitable IP resolved for %s", host)
		return
	}

	shared.LogDebug(tag, "Updating config with resolved peer endpoints..")
	// Update the peer config's peer endpoint from dummy
	peer := &conf.Device.Peers[peerIndex]
	if err := peer.UpdateEndpointIP(ip); err != nil {
		shared.LogError(tag, "Failed to update endpoint for peer %s: %v", peer.PublicKey, err)
		return
	}

	// Update peers via UAPI
	ipcRequest, err := wireproxyawg.CreatePeerIPCRequest(conf.Device)
	if err != nil {
		shared.LogError(tag, "CreatePeerIPCRequest: %v", err)
		return
	}

	handle, ok := tunnelHandles[tunnelHandle]
	if !ok || handle.cancel == nil {
		shared.LogDebug(tag, "Tunnel down, skipping update for %s", host)
		return
	}
	if err := handle.device.IpcSet(ipcRequest.IpcRequest); err != nil {
		shared.LogError(tag, "Failed to update peers: %v", err)
		return
	}

	shared.LogDebug(tag, "Successfully updated peer with resolved endpoint for %s", host)
	resolvingHandles.Delete(tunnelHandle)
}

func (h *TunnelHandle) close() {
	if h == nil {
		return
	}

	// stop all goroutines
	if h.cancel != nil {
		h.cancel()
	}

	// close UAPI listener
	if h.uapi != nil {
		_ = h.uapi.Close()
	}

	// close router to clean up router and firewall rules
	if h.router != nil {
		_ = h.router.Close()
	}

	// close tun device
	if h.device != nil {
		h.device.Close()
	}
}

//export awgTurnOff
func awgTurnOff(tunnelHandle C.int) {
	id := int32(tunnelHandle)
	handle, ok := tunnelHandles[id]
	if !ok {
		shared.LogError(tag, "Tunnel is not up")
		return
	}

	delete(tunnelHandles, id)
	handle.close()
	resolvingHandles.Delete(id)
}

//export awgGetConfig
func awgGetConfig(tunnelHandle C.int) *C.char {
	goTunnelHandle := int32(tunnelHandle)
	handle, ok := tunnelHandles[goTunnelHandle]
	if !ok {
		return nil
	}
	settings, err := handle.device.IpcGet()
	if err != nil {
		shared.LogError(tag, "Failed to get device config: %v", err)
		return nil
	}
	return C.CString(settings)
}

//export awgTurnOffAll
func awgTurnOffAll() {
	for handle := range tunnelHandles {
		awgTurnOff(C.int(handle))
	}
	tunnelHandles = make(map[int32]*TunnelHandle)
}

func newRouter(iface string, fw firewall.Firewall, tunnel tun.Device) (router.Router, error) {
	return osrouter.New(iface, fw, tunnel, shared.NewLogger("Router"))
}

func newFirewall() (firewall.Firewall, error) {
	return firewallmgr.Get()
}

func parseToRouterConfig(conf *wireproxyawg.Configuration, listenPort uint16) (*router.Config, error) {
	device := conf.Device
	if device == nil {
		return nil, errors.New("no [Interface] section found in config")
	}

	cfg := &router.Config{
		MTU: device.MTU,
	}

	// Normalize and add tunnel addresses for router
	for _, addr := range device.Address {
		bitLen := 32
		if addr.Is6() {
			bitLen = 128
		}
		prefix := netip.PrefixFrom(addr, bitLen).Masked()
		cfg.TunnelAddrs = append(cfg.TunnelAddrs, prefix)
	}

	cfg.DNS = device.DNS
	cfg.SearchDomains = device.SearchDomains
	cfg.ListenPort = listenPort

	// Add peer routes (AllowedIPs) to router routes
	for _, peer := range device.Peers {
		cfg.Routes = append(cfg.Routes, peer.AllowedIPs...)
	}

	return cfg, nil
}
