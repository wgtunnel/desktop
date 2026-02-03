# SPDX-License-Identifier: Apache-2.0
#
# Copyright © 2026 WG Tunnel.
# Adapted from WireGuard LLC.

BUILDDIR ?= $(CURDIR)/build
DESTDIR ?= $(CURDIR)/out
RESOURCEDIR ?= $(CURDIR)/../../src/main/resources

NDK_GO_ARCH_MAP_x86_64 := amd64
NDK_GO_ARCH_MAP_aarch64 := arm64

GO_VERSION := 1.25.5
GO_DIR := $(BUILDDIR)/go-$(GO_VERSION)
export GOCACHE := $(BUILDDIR)/go-cache

GO_PLATFORM := $(shell uname -s | tr '[:upper:]' '[:lower:]')-$(NDK_GO_ARCH_MAP_$(shell uname -m))

GO_TARBALL := go$(GO_VERSION).$(GO_PLATFORM).tar.gz
GO_HASH_darwin-amd64 := b69d51bce599e5381a94ce15263ae644ec84667a5ce23d58dc2e63e2c12a9f56
GO_HASH_darwin-arm64 := bed8ebe824e3d3b27e8471d1307f803fc6ab8e1d0eb7a4ae196979bd9b801dd3
GO_HASH_linux-amd64 := 9e9b755d63b36acf30c12a9a3fc379243714c1c6d3dd72861da637f336ebb35b

export GOROOT := $(GO_DIR)
export PATH := $(GO_DIR)/bin:$(PATH)
export CGO_ENABLED := 1

HOST_OS := $(shell uname -s | tr '[:upper:]' '[:lower:]')
ifeq ($(HOST_OS),darwin)
PLATFORMS := darwin-amd64 darwin-arm64 linux-amd64 windows-amd64 windows-arm64
else
PLATFORMS := linux-amd64 windows-amd64
endif

default: all

$(BUILDDIR)/$(GO_TARBALL):
	mkdir -p "$(dir $@)"
	curl -o "$@.tmp" "https://dl.google.com/go/$(GO_TARBALL)"
	echo "$(GO_HASH_$(GO_PLATFORM))  $@.tmp" | sha256sum -c
	mv "$@.tmp" "$@"

$(GO_DIR)/.prepared: $(BUILDDIR)/$(GO_TARBALL)
	mkdir -p "$(dir $@)"
	tar -C "$(dir $@)" --strip-components=1 -xzf "$^"
	cd "$(dir $@)/src/runtime" && sed -i 's/CLOCK_MONOTONIC/BOOTTIME/g' sys_linux_*.s
	cd "$(dir $@)/src/runtime" && sed -i 's/ $1/ $7/g' sys_linux_*.s
	cd "$(dir $@)/src/runtime" && sed -i '/libc_mach_absolute_time/a \//go:cgo_import_dynamic libc_mach_continuous_time mach_continuous_time "/usr/lib/libSystem.B.dylib"' sys_darwin.go
	cd "$(dir $@)/src/runtime" && sed -i 's/mach_absolute_time/mach_continuous_time/g' sys_darwin_amd64.s sys_darwin_arm64.s
	touch "$@"

$(DESTDIR)/libwg-linux-amd64.so: $(GO_DIR)/.prepared go.mod go.sum $(wildcard *.go) $(wildcard **/*.go)
	@mkdir -p "$(DESTDIR)"
	@if [ "$(HOST_OS)" = "darwin" ]; then \
		GOOS=linux GOARCH=amd64 CC="zig cc -target x86_64-linux-gnu-musl" CXX="zig c++ -target x86_64-linux-gnu-musl" go build -ldflags="-buildid=" -v -trimpath -buildvcs=false -o "$@" -buildmode=c-shared .; \
	else \
		GOOS=linux GOARCH=amd64 go build -ldflags="-buildid=" -v -trimpath -buildvcs=false -o "$@" -buildmode=c-shared .; \
	fi

$(DESTDIR)/libwg-windows-amd64.dll: $(GO_DIR)/.prepared go.mod go.sum $(wildcard *.go) $(wildcard **/*.go)
	@mkdir -p "$(DESTDIR)"
	GOOS=windows GOARCH=amd64 CC=x86_64-w64-mingw32-gcc CXX=x86_64-w64-mingw32-g++ \
	go build -ldflags="-buildid=" -v -trimpath -buildvcs=false -o "$@" -buildmode=c-shared .

$(DESTDIR)/libwg-windows-arm64.dll: $(GO_DIR)/.prepared go.mod go.sum $(wildcard *.go) $(wildcard **/*.go)
	@mkdir -p "$(DESTDIR)"
	GOOS=windows GOARCH=arm64 CC=aarch64-w64-mingw32-gcc CXX=aarch64-w64-mingw32-g++ \
	go build -ldflags="-buildid=" -v -trimpath -buildvcs=false -o "$@" -buildmode=c-shared .

$(DESTDIR)/libwg-darwin-amd64.dylib: $(GO_DIR)/.prepared go.mod go.sum $(wildcard *.go) $(wildcard **/*.go)
	@mkdir -p "$(DESTDIR)"
	GOOS=darwin GOARCH=amd64 go build -ldflags="-buildid=" -v -trimpath -buildvcs=false -o "$@" -buildmode=c-shared .

$(DESTDIR)/libwg-darwin-arm64.dylib: $(GO_DIR)/.prepared go.mod go.sum $(wildcard *.go) $(wildcard **/*.go)
	@mkdir -p "$(DESTDIR)"
	GOOS=darwin GOARCH=arm64 go build -ldflags="-buildid=" -v -trimpath -buildvcs=false -o "$@" -buildmode=c-shared .

go-clean-cache: $(GO_DIR)/.prepared
	@mkdir -p $(GOCACHE)
	-@go clean -cache > /dev/null 2>&1

build_all: go-clean-cache \
	$(foreach plat,$(PLATFORMS),$(DESTDIR)/libwg-$(plat).$(if $(findstring windows,$(plat)),dll,$(if $(findstring darwin,$(plat)),dylib,so)))

copy_to_resources: build_all
	@for plat in $(PLATFORMS); do \
		os=`echo "$$plat" | cut -d- -f1`; \
		arch=`echo "$$plat" | cut -d- -f2`; \
		jna_dir=; \
		if [ "$$os" = "linux" ] && [ "$$arch" = "amd64" ]; then jna_dir=linux-x86-64; fi; \
		if [ "$$os" = "windows" ] && [ "$$arch" = "amd64" ]; then jna_dir=win32-x86-64; fi; \
		if [ "$$os" = "windows" ] && [ "$$arch" = "arm64" ]; then jna_dir=win32-aarch64; fi; \
		if [ "$$os" = "darwin" ] && [ "$$arch" = "amd64" ]; then jna_dir=darwin-x86-64; fi; \
		if [ "$$os" = "darwin" ] && [ "$$arch" = "arm64" ]; then jna_dir=darwin-aarch64; fi; \
		libext=so; \
		if [ "$$os" = "windows" ]; then libext=dll; fi; \
		if [ "$$os" = "darwin" ]; then libext=dylib; fi; \
		dest_dir="$(RESOURCEDIR)/$$jna_dir"; \
		mkdir -p "$$dest_dir"; \
		cp "$(DESTDIR)/libwg-$$plat.$$libext" "$$dest_dir/libwg.$$libext"; \
		echo "Copied $$plat -> $$dest_dir/libwg.$$libext"; \
	done


all: copy_to_resources

clean:
	rm -rf $(BUILDDIR) $(DESTDIR)

.PHONY: default all build_all copy_to_resources clean
.DELETE_ON_ERROR: