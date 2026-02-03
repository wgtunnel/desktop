package firewallmgr

import (
	"sync"

	"github.com/wgtunnel/desktop/tunnel/shared"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall/osfirewall"
)

var (
	instance firewall.Firewall
	once     sync.Once
	initErr  error
)

func Get() (firewall.Firewall, error) {
	once.Do(func() {
		instance, initErr = osfirewall.New(
			shared.NewLogger("Firewall"),
		)
	})

	return instance, initErr
}
