# SPDX-License-Identifier: Apache-2.0

DESTDIR      ?= $(CURDIR)/out
RESOURCEDIR  ?= $(CURDIR)/../../src/main/resources
HOST_OS      := $(shell uname -s | tr '[:upper:]' '[:lower:]')

ifeq ($(HOST_OS),darwin)
    PLATFORMS := darwin-amd64 darwin-arm64 linux-amd64 windows-amd64 windows-arm64
else
    PLATFORMS := linux-amd64 windows-amd64
endif

export CGO_ENABLED := 1

# Standard Go build command with -a (force rebuild)
# Added -v so you can see the progress in Gradle logs
GOBUILD := go build -a -v -buildmode=c-shared -trimpath -ldflags="-buildid="

default: all

# Macro to keep the build targets clean
# Usage: $(call go-build,GOOS,GOARCH,CC,OUTPUT)
define go-build
	@mkdir -p "$(DESTDIR)"
	GOOS=$(1) GOARCH=$(2) CC=$(3) $(GOBUILD) -o "$(4)" .
endef

$(DESTDIR)/libkeyring-linux-amd64.so: $(wildcard *.go)
	$(call go-build,linux,amd64,gcc,$@)

$(DESTDIR)/libkeyring-windows-amd64.dll: $(wildcard *.go)
	$(call go-build,windows,amd64,x86_64-w64-mingw32-gcc,$@)

$(DESTDIR)/libkeyring-windows-arm64.dll: $(wildcard *.go)
	$(call go-build,windows,arm64,aarch64-w64-mingw32-gcc,$@)

$(DESTDIR)/libkeyring-darwin-amd64.dylib: $(wildcard *.go)
	$(call go-build,darwin,amd64,clang,$@)

$(DESTDIR)/libkeyring-darwin-arm64.dylib: $(wildcard *.go)
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
	rm -rf "$(DESTDIR)"

.PHONY: default all build_all copy_to_resources clean
.DELETE_ON_ERROR: