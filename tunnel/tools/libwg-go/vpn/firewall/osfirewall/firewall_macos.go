// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 WG Tunnel.
// Adapted from Tailscale

//go:build darwin

package osfirewall

import (
	"errors"
	"fmt"
	"net/netip"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"unsafe"

	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall"
	"golang.org/x/net/bpf"
	"golang.org/x/sys/unix"
)

const (
	anchorName = "wgtunnel"
	pfConfPath = "/etc/pf.conf" // System PF config; we'll append our anchor
)

// macFirewall implements the firewall.Firewall interface for macOS using PF (Packet Filter).
type macFirewall struct {
	tunnelPort        uint16 // WireGuard listen port for inbound punch
	killSwitchEnabled bool   // Track if kill switch is active (not atomic, as PF is stateful)
	v6Available       bool   // Whether the host supports IPv6
	logger            *device.Logger
}

func New(logger *device.Logger) (firewall.Firewall, error) {
	v6err := CheckIPv6(logger)
	supportsV6 := v6err == nil
	logger.Verbosef("PF mode, v6 support: %v", supportsV6)

	return &macFirewall{
		v6Available: supportsV6,
		logger:      logger,
	}, nil
}

func (f *macFirewall) HasV6Available() bool {
	return f.v6Available
}

func (f *macFirewall) Active() bool {
	return f.killSwitchEnabled
}

// Enable initializes the firewall (e.g., ensures PF is enabled and our anchor is referenced).
func (f *macFirewall) Up() error {
	// Ensure PF is enabled (macOS default is off; enable if needed)
	if err := execSudoCommand("pfctl", "-e"); err != nil && !strings.Contains(err.Error(), "already enabled") {
		return fmt.Errorf("enable PF: %w", err)
	}

	// Add our anchor to /etc/pf.conf if not present (append if needed)
	conf, err := os.ReadFile(pfConfPath)
	if err != nil {
		return fmt.Errorf("read pf.conf: %w", err)
	}
	if !strings.Contains(string(conf), fmt.Sprintf(`anchor "%s"`, anchorName)) {
		if err := os.AppendFile(pfConfPath, []byte(fmt.Sprintf(`\nanchor "%s"\nload anchor "%s" from "/etc/pf.anchors/%s"\n`, anchorName, anchorName, anchorName)), 0644); err != nil {
			return fmt.Errorf("append to pf.conf: %w", err)
		}
		if err := execSudoCommand("pfctl", "-f", pfConfPath); err != nil {
			return fmt.Errorf("reload pf.conf: %w", err)
		}
	}

	f.logger.Verbosef("PF initialized")
	return nil
}

// SetTunnelPort sets the UDP port for the WireGuard tunnel and adds punch rules.
func (f *macFirewall) SetTunnelPort(port uint16) error {
	rule := fmt.Sprintf("pass in quick proto udp to any port %d keep state", port)
	if err := f.addRuleToAnchor(rule); err != nil {
		return fmt.Errorf("add port punch rule: %w", err)
	}
	f.tunnelPort = port
	f.logger.Verbosef("Added tunnel port punch for UDP port %d", port)
	return nil
}

// ToggleKillSwitch enables/disables the kill switch.
func (f *macFirewall) ToggleKillSwitch(enable bool) error {
	if enable == f.killSwitchEnabled {
		return nil
	}

	if enable {
		if err := f.addKillSwitchRules(); err != nil {
			return fmt.Errorf("add kill switch rules: %w", err)
		}
	} else {
		if err := f.delKillSwitchRules(); err != nil {
			return fmt.Errorf("del kill switch rules: %w", err)
		}
	}

	f.killSwitchEnabled = enable
	f.logger.Verbosef("Kill switch toggled: %v", enable)
	return nil
}

// addKillSwitchRules adds PF rules for kill switch (block non-tunnel outbound, with exemptions).
func (f *macFirewall) addKillSwitchRules() error {
	rules := []string{
		"block out all",                      // Default block outbound
		"pass out quick on utun0 all",        // Allow on tunnel (adjust 'utun0' dynamically if needed)
		"pass out quick to <bypass_ips> all", // Placeholder for SetBypassRoutes
		// Add loopback allowance
		"pass out quick on lo0 all",
		"pass in quick on lo0 all",
		// Established/related (PF handles keep state)
		"pass out all keep state",
		"pass in all keep state",
	}

	if err := f.writeRulesToAnchor(rules); err != nil {
		return err
	}
	f.logger.Verbosef("Kill switch rules added")
	return nil
}

// delKillSwitchRules removes kill switch rules by clearing the anchor.
func (f *macFirewall) delKillSwitchRules() error {
	if err := f.clearAnchor(); err != nil {
		return err
	}
	f.logger.Verbosef("Kill switch rules removed")
	return nil
}

// SetBypassRoutes adds exemptions for bootstrap routes.
func (f *macFirewall) SetBypassRoutes(bypassRoutes []netip.Prefix) error {
	var rules []string
	for _, route := range bypassRoutes {
		rules = append(rules, fmt.Sprintf("pass out quick to %s all", route.String()))
	}
	if err := f.addRulesToAnchor(rules); err != nil {
		return fmt.Errorf("add bypass routes: %w", err)
	}
	f.logger.Verbosef("Added bypass routes: %v", bypassRoutes)
	return nil
}

// TemporaryBypassSocket uses BPF to attach a filter to the socket for bypass.
func (f *macFirewall) TemporaryBypassSocket(fd int) (func() error, error) {
	// Compile a simple BPF program to allow specific traffic (e.g., UDP to VPN ports)
	// Example: Allow UDP dport == your tunnel port (adjust as needed)
	// This is a basic UDP check; extend for port/IP as needed
	instructions := []bpf.Instruction{
		bpf.LoadAbsolute{Off: 9, Size: 1},                      // Load IP protocol (offset 9 in IP header)
		bpf.JumpIf{Cond: bpf.JumpEqual, Val: 17, SkipFalse: 2}, // Check if UDP (17), jump to reject if not
		// Add port check here if needed, e.g.:
		// bpf.LoadAbsolute{Off: 22, Size: 2}, // Load UDP dport (network byte order, offset 20 src +2 dst in UDP)
		// bpf.JumpIf{Cond: bpf.JumpEqual, Val: uint32(f.tunnelPort), SkipFalse: 1},
		bpf.RetConstant{Val: 65535}, // Accept (return max packet length)
		bpf.RetConstant{Val: 0},     // Reject
	}

	prog, err := bpf.Assemble(instructions) // Compile to machine code
	if err != nil {
		return nil, fmt.Errorf("assemble BPF: %w", err)
	}

	// Prepare SockFprog struct
	sockFprog := unix.SockFprog{
		Len:    uint16(len(prog)),
		Filter: (*unix.Sockfilter)(unsafe.Pointer(&prog[0])),
	}

	// Attach to socket using exported SetsockoptSockFprog
	if err := unix.SetsockoptSockFprog(fd, unix.SOL_SOCKET, unix.SO_ATTACH_FILTER, &sockFprog); err != nil {
		return nil, fmt.Errorf("attach BPF to fd %d: %w", fd, err)
	}
	f.logger.Verbosef("BPF bypass attached to fd %d", fd)

	return func() error {
		// Detach BPF
		if err := unix.SetsockoptInt(fd, unix.SOL_SOCKET, unix.SO_DETACH_FILTER, 0); err != nil {
			f.logger.Errorf("Failed to detach BPF on fd %d: %v", fd, err)
			return err
		}
		f.logger.Verbosef("BPF detached from fd %d", fd)
		return nil
	}, nil
}

// Helper: writeRulesToAnchor writes rules to anchor file and reloads.
func (f *macFirewall) writeRulesToAnchor(rules []string) error {
	anchorPath := filepath.Join("/etc/pf.anchors", anchorName)
	content := strings.Join(rules, "\n")
	if err := os.WriteFile(anchorPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("write anchor file: %w", err)
	}
	return f.reloadAnchor()
}

// addRuleToAnchor appends a single rule and reloads.
func (f *macFirewall) addRuleToAnchor(rule string) error {
	anchorPath := filepath.Join("/etc/pf.anchors", anchorName)
	file, err := os.OpenFile(anchorPath, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0644)
	if err != nil {
		return fmt.Errorf("open anchor file: %w", err)
	}
	defer file.Close()
	if _, err := file.WriteString(rule + "\n"); err != nil {
		return fmt.Errorf("append rule: %w", err)
	}
	return f.reloadAnchor()
}

// addRulesToAnchor appends multiple rules and reloads.
func (f *macFirewall) addRulesToAnchor(rules []string) error {
	anchorPath := filepath.Join("/etc/pf.anchors", anchorName)
	file, err := os.OpenFile(anchorPath, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0644)
	if err != nil {
		return fmt.Errorf("open anchor file: %w", err)
	}
	defer file.Close()
	for _, rule := range rules {
		if _, err := file.WriteString(rule + "\n"); err != nil {
			return fmt.Errorf("append rule: %w", err)
		}
	}
	return f.reloadAnchor()
}

// clearAnchor clears the anchor file and reloads.
func (f *macFirewall) clearAnchor() error {
	anchorPath := filepath.Join("/etc/pf.anchors", anchorName)
	if err := os.WriteFile(anchorPath, []byte{}, 0644); err != nil {
		return fmt.Errorf("clear anchor file: %w", err)
	}
	return f.reloadAnchor()
}

// reloadAnchor reloads the PF anchor.
func (f *macFirewall) reloadAnchor() error {
	if err := execSudoCommand("pfctl", "-a", anchorName, "-F", "all"); err != nil {
		return fmt.Errorf("flush anchor: %w", err)
	}
	if err := execSudoCommand("pfctl", "-a", anchorName, "-f", filepath.Join("/etc/pf.anchors", anchorName)); err != nil {
		return fmt.Errorf("load anchor: %w", err)
	}
	return nil
}

// execSudoCommand runs a command with sudo (assumes sudo is available; handle prompts in app if needed).
func execSudoCommand(name string, args ...string) error {
	cmd := exec.Command("sudo", append([]string{name}, args...)...)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("%s: %w\nOutput: %s", name, err, output)
	}
	return nil
}

func CheckIPv6(logger *device.Logger) error {
	// Similar to Linux: Check sysctl or interfaces for IPv6
	interfaces, err := net.Interfaces()
	if err != nil {
		return err
	}
	for _, iface := range interfaces {
		addrs, err := iface.Addrs()
		if err != nil {
			continue
		}
		for _, addr := range addrs {
			ip, _, err := net.ParseCIDR(addr.String())
			if err == nil && ip.To16() != nil && ip.To4() == nil {
				logger.Verbosef("IPv6 detected on interface %s", iface.Name)
				return nil
			}
		}
	}
	return errors.New("no IPv6 interfaces found")
}
