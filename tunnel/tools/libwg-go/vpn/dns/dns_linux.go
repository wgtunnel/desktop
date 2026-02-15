//go:build linux

package dns

import (
	"context"
	"fmt"
	"net/netip"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/godbus/dbus/v5"
	"github.com/vishvananda/netlink"
	"golang.org/x/sys/unix"
)

const (
	dbusDest      = "org.freedesktop.resolve1"
	dbusInterface = "org.freedesktop.resolve1.Manager"
	dbusPath      = "/org/freedesktop/resolve1"

	resolvConfPath = "/etc/resolv.conf"
	resolvConfBak  = "/etc/resolv.conf.bak.wgt"
)

// Conn represents a systemd-resolved dbus connection.
type Conn struct {
	conn *dbus.Conn
	obj  dbus.BusObject
}

func newConn() (*Conn, error) {
	conn, err := dbus.SystemBusPrivate()
	if err != nil {
		return nil, fmt.Errorf("failed to init private conn to system bus: %w", err)
	}
	methods := []dbus.Auth{dbus.AuthExternal(strconv.Itoa(os.Getuid()))}
	err = conn.Auth(methods)
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to auth with external method: %w", err)
	}
	err = conn.Hello()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to make hello call: %w", err)
	}
	return &Conn{
		conn: conn,
		obj:  conn.Object(dbusDest, dbusPath),
	}, nil
}

// Call wraps obj.CallWithContext by using 0 as flags and formats the method with the dbus manager interface.
func (c *Conn) Call(ctx context.Context, method string, args ...interface{}) *dbus.Call {
	return c.obj.CallWithContext(ctx, fmt.Sprintf("%s.%s", dbusInterface, method), 0, args...)
}

// Close closes the current dbus connection.
func (c *Conn) Close() error {
	return c.conn.Close()
}

// SetDns configures DNS servers and search domains, using systemd-resolved if available (per-interface),
// falling back to overwriting /etc/resolv.conf otherwise.
func SetDns(iface string, dns []netip.Addr, searchDomains []string, fullTunnel bool, logger *device.Logger) error {
	index, err := getInterfaceIndex(iface)
	if isSystemdResolvedActive() {
		if err != nil {
			logger.Errorf("Failed to get interface name, falling back to resolv.conf: %w", err)
			return setDnsFile(dns, searchDomains, fullTunnel)
		}
		logger.Verbosef("Configuring systemd-resolver...")
		return setDnsSystemd(index, dns, searchDomains, fullTunnel)
	}
	logger.Verbosef("Systemd-resolver not detected, falling back to resolv.conf...")
	return setDnsFile(dns, searchDomains, fullTunnel)
}

func getInterfaceIndex(ifName string) (int, error) {
	link, err := netlink.LinkByName(ifName)
	if err != nil {
		return 0, fmt.Errorf("failed to get link for %s: %w", ifName, err)
	}
	return link.Attrs().Index, nil
}

// RevertDns reverts DNS configuration, using systemd-resolved if available, or restoring the resolv.conf backup otherwise.
func RevertDns(iface string, logger *device.Logger) error {
	index, err := getInterfaceIndex(iface)
	if isSystemdResolvedActive() {
		if err != nil {
			logger.Errorf("Failed to get interface name, attempting to revert resolv.conf from backup...")
			return revertDnsFile()
		}
		logger.Verbosef("Reverting systemd-resolver...")
		return revertDnsSystemd(index)
	}
	logger.Verbosef("Systemd-resolver not detected, attempting to revert dns from backup...")
	return revertDnsFile()
}

// isSystemdResolvedActive checks if systemd-resolved is available and responsive via DBus.
func isSystemdResolvedActive() bool {
	conn, err := newConn()
	if err != nil {
		return false
	}
	defer conn.Close()

	// Test with a simple local resolve (flags=0)
	var addresses []struct {
		IfIndex int
		Family  int
		Address []byte
	}
	var canonical string
	var outflags uint64
	call := conn.Call(context.Background(), "ResolveHostname", 0, "localhost", unix.AF_UNSPEC, uint64(0))
	if call.Err != nil {
		return false
	}
	err = call.Store(&addresses, &canonical, &outflags)
	return err == nil
}

// setDnsSystemd configures DNS via systemd-resolved DBus (per-interface).
func setDnsSystemd(ifIndex int, dns []netip.Addr, searchDomains []string, fullTunnel bool) error {
	conn, err := newConn()
	if err != nil {
		return fmt.Errorf("dbus connect: %w", err)
	}
	defer conn.Close()

	type dnsEntry struct {
		Family  int32
		Address []byte
	}

	var linkDNS []dnsEntry
	for _, ip := range dns {
		fam := int32(unix.AF_INET)
		if ip.Is6() {
			fam = int32(unix.AF_INET6)
		}
		linkDNS = append(linkDNS, dnsEntry{
			Family:  fam,
			Address: ip.AsSlice(),
		})
	}
	call := conn.Call(context.Background(), "SetLinkDNS", ifIndex, linkDNS)
	if call.Err != nil {
		return fmt.Errorf("set link DNS: %w", call.Err)
	}

	type domainEntry struct {
		Domain  string
		Routing bool
	}

	var linkDomains []domainEntry
	for _, domain := range searchDomains {
		linkDomains = append(linkDomains, domainEntry{
			Domain:  domain,
			Routing: false,
		})
	}
	// full tunnel, add "~." as routing domain to capture all queries
	if fullTunnel && len(dns) > 0 {
		linkDomains = append(linkDomains, domainEntry{
			Domain:  "~.",
			Routing: true,
		})
	}
	call = conn.Call(context.Background(), "SetLinkDomains", ifIndex, linkDomains)
	if call.Err != nil {
		return fmt.Errorf("set link domains: %w", call.Err)
	}

	// set the link as the default DNS route for full tunnel
	if fullTunnel {
		call = conn.Call(context.Background(), "SetLinkDefaultRoute", ifIndex, true)
		if call.Err != nil {
			return fmt.Errorf("set link default route: %w", call.Err)
		}
	}

	return nil
}

// revertDnsSystemd reverts DNS configuration via systemd-resolved DBus.
func revertDnsSystemd(ifIndex int) error {
	conn, err := newConn()
	if err != nil {
		return fmt.Errorf("dbus connect: %w", err)
	}
	defer conn.Close()

	// revert default route
	call := conn.Call(context.Background(), "SetLinkDefaultRoute", ifIndex, false)
	if call.Err != nil {
		return fmt.Errorf("revert link default route: %w", call.Err)
	}

	// revert all settings for the link
	call = conn.Call(context.Background(), "RevertLink", ifIndex)
	if call.Err != nil {
		return fmt.Errorf("revert link: %w", call.Err)
	}

	return nil
}

// setDnsFile is the fallback: overwrites /etc/resolv.conf and locks if fullTunnel.
func setDnsFile(dns []netip.Addr, searchDomains []string, fullTunnel bool) error {
	if err := backupResolvConf(); err != nil {
		return err
	}

	// Write new conf
	f, err := os.Create(resolvConfPath)
	if err != nil {
		return err
	}
	defer f.Close()

	for _, d := range dns {
		fmt.Fprintf(f, "nameserver %s\n", d.String())
	}
	if len(searchDomains) > 0 {
		fmt.Fprintf(f, "search %s\n", strings.Join(searchDomains, " "))
	}

	// attempt lock if full tunnel
	if fullTunnel {
		if err := lockResolvConf(true); err != nil {
		}
	}

	return nil
}

// revertDnsFile is the fallback: restores backup and unlocks.
func revertDnsFile() error {
	lockResolvConf(false)

	if _, err := os.Stat(resolvConfBak); os.IsNotExist(err) {
		return nil
	}

	src, err := os.ReadFile(resolvConfBak)
	if err != nil {
		return err
	}
	if err := os.WriteFile(resolvConfPath, src, 0644); err != nil {
		return err
	}
	os.Remove(resolvConfBak)
	return nil
}

// backupResolvConf backs up resolv.conf if not already done.
func backupResolvConf() error {
	if _, err := os.Stat(resolvConfBak); err == nil {
		return nil
	}
	src, err := os.ReadFile(resolvConfPath)
	if err != nil {
		return err
	}
	return os.WriteFile(resolvConfBak, src, 0644)
}

// lockResolvConf locks/unlocks with chattr (immutable).
func lockResolvConf(lock bool) error {
	arg := "-i"
	if lock {
		arg = "+i"
	}
	// use filepath.Abs to handle symlinks properly
	absPath, err := filepath.Abs(resolvConfPath)
	if err != nil {
		return err
	}
	cmd := exec.Command("chattr", arg, absPath)
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("chattr %s: %w", arg, err)
	}
	return nil
}
