package firewall

import "net/netip"

// Firewall is responsible for managing the system's firewall rules, especially the kill switch. It operates independently of the router.
type Firewall interface {

	// Enable activates the kill switch, blocking all outbound traffic except
	// explicitly allowed bypasses.
	Enable() error

	// IsEnabled reports whether the kill switch is currently active.
	IsEnabled() bool

	// Disable deactivates the kill switch and cleans up all rules.
	Disable() error

	// AllowLocalNetworks adds bypass rules for the specified local network prefixes. Requires kill switch enabled and
	// operates independently of tunnel/router bypasses.
	AllowLocalNetworks([]netip.Prefix) error
}
