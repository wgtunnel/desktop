# SPDX-License-Identifier: Apache-2.0

BUILDDIR     ?= $(CURDIR)/build
DESTDIR      ?= $(CURDIR)/out
RESOURCEDIR  ?= $(CURDIR)/../../src/main/resources
HOST_OS      := $(shell uname -s | tr '[:upper:]' '[:lower:]')

ifeq ($(HOST_OS),darwin)
    PLATFORMS := darwin-amd64 darwin-arm64 linux-amd64 windows-amd64 windows-arm64
else
    PLATFORMS := linux-amd64 windows-amd64
endif

GO_VERSION := 1.25.5
GO_DIR     := $(BUILDDIR)/go-$(GO_VERSION)
export GOCACHE := $(BUILDDIR)/go-cache

GO_PLATFORM := $(shell uname -s | tr '[:upper:]' '[:lower:]')-$(shell uname -m | sed 's/x86_64/amd64/;s/aarch64/arm64/')

GO_TARBALL := go$(GO_VERSION).$(GO_PLATFORM).tar.gz
GO_HASH_darwin-amd64 := b69d51bce599e5381a94ce15263ae644ec84667a5ce23d58dc2e63e2c12a9f56
GO_HASH_darwin-arm64 := bed8ebe824e3d3b27e8471d1307f803fc6ab8e1d0eb7a4ae196979bd9b801dd3
GO_HASH_linux-amd64  := 9e9b755d63b36acf30c12a9a3fc379243714c1c6d3dd72861da637f336ebb35b

export GOROOT     := $(GO_DIR)
export PATH       := $(GO_DIR)/bin:$(PATH)
export CGO_ENABLED := 1
export MACOSX_DEPLOYMENT_TARGET := 12.0

# Standard Go build command with -a (force rebuild)
# Added -v so you can see the progress in Gradle logs
GOBUILD := go build -a -v -buildmode=c-shared -trimpath -ldflags="-buildid="

default: all

$(BUILDDIR)/$(GO_TARBALL):
	mkdir -p "$(dir $@)"
	curl -o "$@.tmp" "https://dl.google.com/go/$(GO_TARBALL)"
	echo "$(GO_HASH_$(GO_PLATFORM))  $@.tmp" | sha256sum -c
	mv "$@.tmp" "$@"

$(GO_DIR)/.prepared: $(BUILDDIR)/$(GO_TARBALL)
	mkdir -p "$(dir $@)"
	tar -C "$(dir $@)" --strip-components=1 --no-same-owner -xzf "$^"
	chmod -R u+w "$(GO_DIR)"
	touch "$@"

# Macro to keep the build targets clean
# Usage: $(call go-build,GOOS,GOARCH,CC,OUTPUT)
define go-build
	@mkdir -p "$(DESTDIR)"
	GOOS=$(1) GOARCH=$(2) CC=$(3) $(GOBUILD) -o "$(4)" .
endef

$(DESTDIR)/libkeyring-linux-amd64.so: $(GO_DIR)/.prepared $(wildcard *.go)
	$(call go-build,linux,amd64,gcc,$@)

$(DESTDIR)/libkeyring-windows-amd64.dll: $(GO_DIR)/.prepared $(wildcard *.go)
	$(call go-build,windows,amd64,x86_64-w64-mingw32-gcc,$@)

$(DESTDIR)/libkeyring-windows-arm64.dll: $(GO_DIR)/.prepared $(wildcard *.go)
	$(call go-build,windows,arm64,aarch64-w64-mingw32-gcc,$@)

$(DESTDIR)/libkeyring-darwin-amd64.dylib: $(GO_DIR)/.prepared $(wildcard *.go)
	@mkdir -p "$(DESTDIR)"
	GOOS=darwin GOARCH=amd64 CC="clang -target x86_64-apple-macos$(MACOSX_DEPLOYMENT_TARGET)" $(GOBUILD) -o "$@" .

$(DESTDIR)/libkeyring-darwin-arm64.dylib: $(GO_DIR)/.prepared $(wildcard *.go)
	$(call go-build,darwin,arm64,clang,$@)

build_all: $(foreach plat,$(PLATFORMS),$(DESTDIR)/libkeyring-$(plat).$(if $(findstring windows,$(plat)),dll,$(if $(findstring darwin,$(plat)),dylib,so)))

copy_to_resources: build_all
	@for plat in $(PLATFORMS); do \
		os=`echo "$$plat" | cut -d- -f1`; \
		arch=`echo "$$plat" | cut -d- -f2`; \
		jna_dir=; \
		if [ "$$os" = "linux" ]   && [ "$$arch" = "amd64" ]; then jna_dir=linux-x86-64; fi; \
		if [ "$$os" = "windows" ] && [ "$$arch" = "amd64" ]; then jna_dir=win32-x86-64; fi; \
		if [ "$$os" = "windows" ] && [ "$$arch" = "arm64" ]; then jna_dir=win32-aarch64; fi; \
		if [ "$$os" = "darwin" ]  && [ "$$arch" = "amd64" ]; then jna_dir=darwin-x86-64; fi; \
		if [ "$$os" = "darwin" ]  && [ "$$arch" = "arm64" ]; then jna_dir=darwin-aarch64; fi; \
		libext=so; \
		if [ "$$os" = "windows" ]; then libext=dll; fi; \
		if [ "$$os" = "darwin" ]; then libext=dylib; fi; \
		dest_dir="$(RESOURCEDIR)/$$jna_dir"; \
		mkdir -p "$$dest_dir"; \
		cp "$(DESTDIR)/libkeyring-$$plat.$$libext" "$$dest_dir/libkeyring.$$libext"; \
		echo "Copied $$plat -> $$dest_dir/libkeyring.$$libext"; \
	done

all: copy_to_resources

clean:
	rm -rf "$(DESTDIR)" "$(BUILDDIR)"

.PHONY: default all build_all copy_to_resources clean
.DELETE_ON_ERROR:
