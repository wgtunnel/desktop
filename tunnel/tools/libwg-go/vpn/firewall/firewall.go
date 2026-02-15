package firewall

import "net/netip"

// Firewall is responsible for managing the system's firewall rules, especially the kill switch. It operates independently of the router.
type Firewall interface {

	// SetPersist sets whether the kill switch should persist tunnel down
	SetPersist(enabled bool)

	// Enable activates the kill switch, blocking all outbound traffic except
	// explicitly allowed bypasses. Persist will keep the firewall active
	Enable() error

	// IsEnabled reports whether the kill switch is currently active.
	IsEnabled() bool

	// IsPersistent whether the kill switch was enabled to persist tunnel changes
	IsPersistent() bool

	// Disable deactivates the kill switch and cleans up all rules.
	Disable() error

	// AllowLocalNetworks adds bypass rules for the specified local network prefixes. Requires kill switch enabled and
	// operates independently of tunnel/router bypasses.
	AllowLocalNetworks([]netip.Prefix) error

	// RemoveLocalNetworks removes any rules set by AllowLocalNetworks
	RemoveLocalNetworks() error

	IsAllowLocalNetworksEnabled() bool
}
