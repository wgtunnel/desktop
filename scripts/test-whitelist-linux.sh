#!/usr/bin/env bash
# Root-only integration harness. All firewall/routing writes occur inside client.
set -euo pipefail
root_dir=$(cd "$(dirname "$0")/.." && pwd)
client="wgwl-client-$$"
isp="wgwl-isp-$$"
cleanup() {
  ip netns pids "$isp" 2>/dev/null | xargs -r kill || true
  ip netns del "$client" 2>/dev/null || true
  ip netns del "$isp" 2>/dev/null || true
}
trap cleanup EXIT
ip netns add "$client"
ip netns add "$isp"
ip link add wl-client type veth peer name wl-isp
ip link set wl-client netns "$client"
ip link set wl-isp netns "$isp"
ip -n "$client" link set lo up
ip -n "$isp" link set lo up
ip -n "$client" addr add 203.0.113.10/24 dev wl-client
ip -n "$isp" addr add 203.0.113.1/24 dev wl-isp
ip -n "$isp" addr add 203.0.113.2/24 dev wl-isp
ip -6 -n "$client" addr add 2001:db8:53::10/64 dev wl-client nodad
ip -6 -n "$isp" addr add 2001:db8:53::1/64 dev wl-isp nodad
ip -6 -n "$isp" addr add 2001:db8:53::2/64 dev wl-isp nodad
ip -n "$client" link set wl-client up
ip -n "$isp" link set wl-isp up
ip -n "$client" route add default via 203.0.113.1
ip netns exec "$client" ip tuntap add dev wgt-test mode tun
ip -n "$client" link set wgt-test up
ip -n "$client" route add default dev wgt-test table 52
ip -n "$client" rule add priority 200 table 52
ip -6 -n "$client" route add default dev wgt-test table 52
ip -6 -n "$client" rule add priority 200 table 52
ip netns exec "$isp" python3 -m http.server 8080 --bind :: >/dev/null 2>&1 &
# Wait for the remote listener before enabling the client's kill switch.
for attempt in {1..100}; do
  if ip netns exec "$isp" python3 -c 'import socket; s=socket.create_connection(("203.0.113.1",8080),.1); s.close()' 2>/dev/null; then break; fi
  sleep 0.05
done
cd "$root_dir/daemon/tools/direct-whitelist"
ip netns exec "$client" env WG_WHITELIST_INTEGRATION=1 go test . -run TestDirectWhitelistIntegration -count=1 -v
