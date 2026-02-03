// Package dnsresolver provides modular DNS resolution with backoff retries using AdguardTeam/dnsproxy.
// It supports various protocols (plain DNS, DoT, DoH, DoQ, DNSCrypt) via upstream URLs.
// Example upstream formats:
// - Plain UDP: "udp://1.1.1.1:53"
// - Plain TCP: "tcp://1.1.1.1:53"
// - DoT: "tls://1.1.1.1:853"
// - DoH: "https://cloudflare-dns.com/dns-query"
// - DoQ: "quic://dns.adguard-dns.com:853"
// - DNSCrypt: "sdns://AQIAAAAAAAAAFDEuZTAuMC4xOjg0NDMg04wIk9UdC5pYol3Wg92WwgQzOKk8J6SxvE-rO4jDW56HAgBgML0pB4"

package dns

import (
	"context"
	"errors"
	"fmt"
	"net"
	"net/netip"
	"sync"
	"time"

	"github.com/AdguardTeam/dnsproxy/upstream"
	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/cenkalti/backoff/v5"
	"github.com/miekg/dns"
)

// ResolverOptions configures the DNS resolver.
type ResolverOptions struct {
	UpstreamURL string
	Timeout     time.Duration
}

// DefaultOptions returns default resolver options with 1.1.1.1 over UDP.
func DefaultOptions() ResolverOptions {
	return ResolverOptions{
		UpstreamURL: "udp://1.1.1.1:53",
		Timeout:     5 * time.Second,
	}
}

type Resolved struct {
	V4 []netip.Addr
	V6 []netip.Addr
}

func resolveInner(host string, ipType uint16, u upstream.Upstream, dialer *net.Dialer, wg *sync.WaitGroup) ([]netip.Addr, error) {
	var addr []netip.Addr
	defer wg.Done()

	req := &dns.Msg{}
	req.Id = dns.Id()
	req.RecursionDesired = true
	req.SetQuestion(dns.Fqdn(host), ipType)

	req.SetEdns0(4096, true)

	// Since upstream.Options doesn't take a dialer, we use the miekg/dns client
	// directly with our custom dialer to ensure the SO_MARK/Binding is applied.
	client := &dns.Client{
		Net:     "udp",
		Dialer:  dialer,
		Timeout: 5 * time.Second,
		UDPSize: 4096,
	}

	// We use the Address from the upstream (e.g., "1.1.1.1:53")
	res, _, err := client.Exchange(req, u.Address())
	if err != nil {
		return nil, err
	}

	if res.Rcode != dns.RcodeSuccess {
		return nil, fmt.Errorf("DNS query failed with Rcode: %d", res.Rcode)
	}

	for _, ans := range res.Answer {
		switch ipType {
		case dns.TypeA:
			if a, ok := ans.(*dns.A); ok {
				if ip, err := netip.ParseAddr(a.A.String()); err == nil {
					addr = append(addr, ip)
				}
			}
		case dns.TypeAAAA:
			if aaaa, ok := ans.(*dns.AAAA); ok {
				if ip, err := netip.ParseAddr(aaaa.AAAA.String()); err == nil {
					addr = append(addr, ip)
				}
			}
		}
	}
	return addr, nil
}

func Resolve(host string, opts ResolverOptions, preferIpv6 bool) ([]netip.Addr, []netip.Addr, error) {
	dialer, err := GetBypassDialer(preferIpv6)
	if err != nil {
		return nil, nil, fmt.Errorf("bypass dialer failed: %w", err)
	}

	// 2. Setup the library just to handle URL parsing and certificates
	// We pass the CustomResolver (which uses our bypass dialer) for bootstrapping
	u, err := upstream.AddressToUpstream(opts.UpstreamURL, &upstream.Options{
		Bootstrap:  CustomResolver(preferIpv6),
		Timeout:    opts.Timeout,
		PreferIPv6: preferIpv6,
	})
	if err != nil {
		return nil, nil, err
	}
	defer u.Close()

	var wg sync.WaitGroup
	var v4, v6 []netip.Addr
	var v4Err, v6Err error

	wg.Add(2)
	// 3. We use the 'dialer' directly in resolveInner
	go func() { v4, v4Err = resolveInner(host, dns.TypeA, u, dialer, &wg) }()
	go func() { v6, v6Err = resolveInner(host, dns.TypeAAAA, u, dialer, &wg) }()
	wg.Wait()

	if v4Err != nil && v6Err != nil {
		return nil, nil, errors.Join(v4Err, v6Err)
	}

	if len(v4) == 0 && len(v6) == 0 {
		if v4Err != nil {
			return nil, nil, v4Err
		}
		if v6Err != nil {
			return nil, nil, v6Err
		}
		return nil, nil, errors.New("no IP addresses found")
	}

	return v4, v6, nil
}

// ResolveWithBackoff retries resolution with exponential backoff until success
func ResolveWithBackoff(ctx context.Context, host string, opts ResolverOptions, preferIpv6 bool, logger *device.Logger) (Resolved, error) {
	logger.Verbosef("Starting DNS resolution...")
	operation := func() (Resolved, error) {
		if err := ctx.Err(); err != nil {
			return Resolved{}, backoff.Permanent(err)
		}
		v4, v6, err := Resolve(host, opts, preferIpv6)
		if err != nil {
			logger.Errorf("Error resolving host %s: %v, retrying...", host, err)
			return Resolved{}, err
		}
		if len(v4) == 0 && len(v6) == 0 {
			logger.Errorf("No IPs resolved for host %s, retrying...", host)
			return Resolved{}, errors.New("no IPs resolved")
		}
		logger.Verbosef("Host successfully resolved.")
		return Resolved{V4: v4, V6: v6}, nil
	}

	return backoff.Retry(ctx, operation,
		backoff.WithBackOff(backoff.NewExponentialBackOff()),
		backoff.WithMaxElapsedTime(0), // retry forever
	)
}
