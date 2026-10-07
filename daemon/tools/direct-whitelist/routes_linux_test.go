//go:build linux && !android

package main

import (
	"github.com/google/nftables"
	"github.com/google/nftables/expr"
	"github.com/vishvananda/netlink"

	"golang.org/x/sys/unix"
	"net"
	"net/netip"
	"os"
	"testing"
	"time"
)

func TestWhitelistRoutePrecedence(t *testing.T) {
	for _, value := range []string{"192.0.2.0/24", "2001:db8::/32"} {
		p := netip.MustParsePrefix(value)
		rules := directRules(p)
		if rules[1].Priority >= 100 || rules[1].Table != directTable || rules[1].Dst.String() != p.String() {
			t.Fatal(rules)
		}
		if rules[0].Type != unix.RTN_UNREACHABLE || rules[0].Priority <= rules[1].Priority || rules[0].Priority >= 100 {
			t.Fatal("missing fail-closed fallback", rules)
		}
		if rules[1].Family == netlink.FAMILY_V4 != p.Addr().Is4() {
			t.Fatal("wrong address family")
		}
	}
}

// The shell harness runs this in an isolated network namespace with a simulated
// ISP and VPN. Never modify the host firewall when running ordinary unit tests.
func TestDirectWhitelistIntegration(t *testing.T) {
	if os.Getenv("WG_WHITELIST_INTEGRATION") != "1" {
		t.Skip("requires scripts/test-whitelist-linux.sh")
	}
	if err := testFirewall(true); err != nil {
		t.Fatal(err)
	}
	defer func() { _ = applyDirectWhitelist(""); _ = testFirewall(false) }()
	check := func(ip string, allowed bool) {
		t.Helper()
		c, err := net.DialTimeout("tcp", net.JoinHostPort(ip, "8080"), 700*time.Millisecond)
		if c != nil {
			c.Close()
		}
		if (err == nil) != allowed {
			t.Fatalf("%s allowed=%v: %v", ip, allowed, err)
		}
	}
	if err := applyDirectWhitelist("203.0.113.1"); err != nil {
		t.Fatal(err)
	}
	check("203.0.113.1", true)
	check("203.0.113.2", false)
	if err := applyDirectWhitelist("*.invalid.test"); err == nil {
		t.Fatal("invalid list succeeded")
	}
	check("203.0.113.1", true)
	// Simulate VPN disconnect with the independent kill switch left active.
	r := netlink.NewRule()
	r.Family = netlink.FAMILY_V4
	r.Priority = 200
	r.Table = 52
	if err := netlink.RuleDel(r); err != nil {
		t.Fatal(err)
	}
	check("203.0.113.1", true)
	check("203.0.113.2", false)
	if err := applyDirectWhitelist("203.0.113.0/24"); err != nil {
		t.Fatal(err)
	}
	check("203.0.113.1", true)
	check("203.0.113.2", true)
	if err := applyDirectWhitelist(""); err != nil {
		t.Fatal(err)
	}
	check("203.0.113.1", false)
	if err := applyDirectWhitelist("203.0.113.1"); err != nil {
		t.Fatal(err)
	}
	if err := testFirewall(false); err != nil {
		t.Fatal(err)
	}
	if err := testFirewall(true); err != nil {
		t.Fatal(err)
	}
	if err := firewall.reconcile(); err != nil {
		t.Fatal(err)
	}
	check("203.0.113.1", true)
	check("203.0.113.2", false)
	if err := applyDirectWhitelist("203.0.113.1\n2001:db8:53::1"); err != nil {
		t.Fatal(err)
	}
	check("2001:db8:53::1", true)
	check("2001:db8:53::2", false)
}

// Mirrors core 1.8.1's private wgtunnel input/output chains and DROP policy.
func testFirewall(enabled bool) error {
	c := &nftables.Conn{}
	tables, err := c.ListTables()
	if err != nil {
		return err
	}
	for _, t := range tables {
		if t.Name == "wgtunnel" {
			c.DelTable(t)
		}
	}
	if err := c.Flush(); err != nil {
		return err
	}
	if !enabled {
		return nil
	}
	for _, family := range []nftables.TableFamily{nftables.TableFamilyIPv4, nftables.TableFamilyIPv6} {
		t := c.AddTable(&nftables.Table{Name: "wgtunnel", Family: family})
		input := c.AddChain(&nftables.Chain{Table: t, Name: "input", Type: nftables.ChainTypeFilter, Hooknum: nftables.ChainHookInput, Priority: nftables.ChainPriorityFilter})
		output := c.AddChain(&nftables.Chain{Table: t, Name: "output", Type: nftables.ChainTypeFilter, Hooknum: nftables.ChainHookOutput, Priority: nftables.ChainPriorityFilter})
		c.AddRule(&nftables.Rule{Table: t, Chain: input, Exprs: []expr.Any{&expr.Ct{Key: expr.CtKeySTATE, Register: 1}, &expr.Bitwise{SourceRegister: 1, DestRegister: 1, Len: 4, Mask: []byte{6, 0, 0, 0}, Xor: make([]byte, 4)}, &expr.Cmp{Op: expr.CmpOpNeq, Register: 1, Data: make([]byte, 4)}, &expr.Verdict{Kind: expr.VerdictAccept}}})
		for _, ch := range []*nftables.Chain{input, output} {
			c.AddRule(&nftables.Rule{Table: t, Chain: ch, Exprs: []expr.Any{&expr.Verdict{Kind: expr.VerdictDrop}}})
		}
	}
	return c.Flush()
}
