package whitelist

import (
	"strings"
	"testing"
)

func TestParseCanonicalDestinations(t *testing.T) {
	entries, err := Parse(" 192.0.2.17/24\n192.0.2.0/24\n2001:db8::1\n::ffff:192.0.2.5\nEXAMPLE.com.\nexample.com\nпример.рф\n")
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 5 {
		t.Fatalf("deduplication: %#v", entries)
	}
	if got := entries[0].Prefix.String(); got != "192.0.2.0/24" {
		t.Fatal(got)
	}
	if got := entries[2].Prefix.String(); got != "192.0.2.5/32" {
		t.Fatal(got)
	}
	if entries[3].Host != "example.com" || entries[4].Host != "xn--e1afmkfd.xn--p1ai" {
		t.Fatal(entries)
	}
}

func TestRejectUnsafeAndMalformedEntries(t *testing.T) {
	for _, value := range []string{"0.0.0.0/0", "::/0", "192.0.2.1/99", "999.1.2.3", "https://example.com", "*.example.com", "a..com", "-a.com", "a-.com", "example.com:443", "localhost", "fe80::1%eth0", "::ffff:192.0.2.1/120", "example.com\rmalicious.com"} {
		t.Run(value, func(t *testing.T) {
			if _, err := Parse(value); err == nil {
				t.Fatalf("accepted %q", value)
			}
		})
	}
}

func TestLimitAndEmptyList(t *testing.T) {
	if e, err := Parse(" \n\r\n"); err != nil || len(e) != 0 {
		t.Fatal(e, err)
	}
	var b strings.Builder
	for i := 0; i <= MaxEntries; i++ {
		b.WriteString(strings.Repeat("a", i/60+1))
		b.WriteString(string(rune('a' + i%26)))
		b.WriteString(string(rune('a' + i/26%26)))
		b.WriteString(".test\n")
	}
	if _, err := Parse(b.String()); err == nil {
		t.Fatal("entry limit not enforced")
	}
}
