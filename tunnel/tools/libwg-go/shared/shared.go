package shared

/*
#include <stdint.h>
typedef void (*StatusCodeCallback)(int32_t handle, int32_t status);

void callStatusCallback(StatusCodeCallback cb, int32_t handle, int32_t status) {
    if (cb) cb(handle, status);
}
*/
import "C"
import (
	"fmt"
	"log"
	"runtime"

	"github.com/amnezia-vpn/amneziawg-go/device"
)

var tag = "AmneziaWG"

func init() {
	switch runtime.GOOS {
	case "linux", "darwin":
		log.SetFlags(0)
	case "windows":
		log.SetFlags(log.Ldate | log.Ltime | log.Lmicroseconds)
	default:
		log.SetFlags(log.LstdFlags)
	}
}

func LogDebug(format string, args ...interface{}) {
	log.Printf("[DEBUG] %s: %s", tag, fmt.Sprintf(format, args...))
}

func LogWarn(format string, args ...interface{}) {
	log.Printf("[WARN] %s: %s", tag, fmt.Sprintf(format, args...))
}

func LogError(format string, args ...interface{}) {
	log.Printf("[ERROR] %s: %s", tag, fmt.Sprintf(format, args...))
}

func NewLogger(prefix string) *device.Logger {
	return &device.Logger{
		Verbosef: func(format string, args ...any) {
			LogDebug(prefix+": "+format, args...)
		},
		Errorf: func(format string, args ...any) {
			LogError(prefix+": "+format, args...)
		},
	}
}

type StatusCodeCallback C.StatusCodeCallback

var tunnelCallbacks = make(map[int32]StatusCodeCallback)

func StoreTunnelCallback(handle int32, cb StatusCodeCallback) {
	if cb != nil {
		tunnelCallbacks[handle] = cb
	}
}

func NotifyStatusCode(handle int32, status int32) {
	if cb, ok := tunnelCallbacks[handle]; ok && cb != nil {
		C.callStatusCallback(cb, C.int32_t(handle), C.int32_t(status))
	}
}

const (
	StatusHealthy = iota
	StatusHandshakeFailure
	StatusResolvingDNS
)
