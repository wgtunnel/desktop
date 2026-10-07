//go:build linux

package main

import (
	"bufio"
	"encoding/json"
	"log"
	"os"
	"os/signal"
	"syscall"
)

type request struct {
	Entries string `json:"entries"`
}
type response struct {
	Error string `json:"error"`
}

func main() {
	// Logs never share stdout with the response protocol.
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGTERM, syscall.SIGINT)
	go func() { <-signals; cleanup(); os.Exit(0) }()
	defer cleanup()
	scan := bufio.NewScanner(os.Stdin)
	scan.Buffer(make([]byte, 4096), 256*1024)
	enc := json.NewEncoder(os.Stdout)
	for scan.Scan() {
		var r request
		err := json.Unmarshal(scan.Bytes(), &r)
		if err == nil {
			err = applyDirectWhitelist(r.Entries)
		}
		reply := response{}
		if err != nil {
			reply.Error = err.Error()
		}
		if enc.Encode(reply) != nil {
			return
		}
	}
	if err := scan.Err(); err != nil {
		log.Print(err)
	}
}
func cleanup() {
	direct.Lock()
	defer direct.Unlock()
	_ = firewall.SetDirectWhitelist(nil, false)
	_ = cleanupDirectRules()
}
