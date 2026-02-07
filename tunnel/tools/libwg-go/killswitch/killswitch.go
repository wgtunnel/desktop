//go:build !android

package killswitch

import "C"
import (
	"github.com/wgtunnel/desktop/tunnel/shared"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall/osfirewall/firewallmgr"
)

var logger = shared.NewLogger("KillSwitch")

//export setKillSwitch
func setKillSwitch(enabled C.int) C.int {
	fw, err := firewallmgr.Get()
	if err != nil {
		logger.Errorf("Failed to get firewall: %v", err)
		return C.int(-1)
	}

	if enabled == 1 {
		fw.SetPersist(true)
		err := fw.Enable()
		if err != nil {
			logger.Errorf("Failed to enable kill switch: %v", err)
			return C.int(-1)
		}
		logger.Verbosef("Kill switch enabled")
	} else {
		err := fw.Disable()
		if err != nil {
			logger.Errorf("Failed to disable kill switch: %v", err)
			return C.int(-1)
		}
		logger.Verbosef("Kill switch disabled")
	}
	return enabled
}

//export getKillSwitchStatus
func getKillSwitchStatus() C.int {
	fw, err := firewallmgr.Get()
	if err != nil {
		logger.Errorf("Failed to get firewall: %v", err)
		return C.int(0)
	}

	if fw.IsEnabled() && fw.IsPersistent() {
		return C.int(1)
	}
	return C.int(0)
}
