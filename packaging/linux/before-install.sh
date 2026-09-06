#!/bin/bash
# Stop the daemon before the payload is unpacked so binaries can be replaced.
systemctl stop '${sanitizedProductName}-daemon.service' 2>/dev/null || true
