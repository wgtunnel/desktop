# SPDX-License-Identifier: Apache-2.0
# Build keyring as a JNI shared library (no JNA).

DESTDIR      ?= $(CURDIR)/out
RESOURCEDIR  ?= $(CURDIR)/../../src/main/resources/nucleus/native
HOST_OS      := $(shell uname -s | tr '[:upper:]' '[:lower:]')
JAVA_HOME    ?= $(shell dirname $$(dirname $$(readlink -f $$(which java))))
ifeq ($(wildcard $(JAVA_HOME)/include/jni.h),)
  override JAVA_HOME := $(shell dirname $$(dirname $$(readlink -f $$(which java))))
endif

ifeq ($(HOST_OS),darwin)
    PLATFORMS := darwin-amd64 darwin-arm64 linux-amd64 windows-amd64 windows-arm64
    JDK_JNI_CFLAGS := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin
else ifeq ($(HOST_OS),linux)
    PLATFORMS := linux-amd64 windows-amd64
    JDK_JNI_CFLAGS := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
else
    PLATFORMS := windows-amd64
    JDK_JNI_CFLAGS := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32
endif

export CGO_ENABLED := 1

GOBUILD := go build -v -buildmode=c-shared -trimpath -ldflags="-buildid="

default: all

$(DESTDIR)/libkeyring-linux-amd64.so: $(wildcard *.go)
	@mkdir -p "$(DESTDIR)"
	@test -f "$(JAVA_HOME)/include/jni.h" || (echo "jni.h not found; JAVA_HOME=$(JAVA_HOME)"; exit 1)
	CGO_CFLAGS="$(JDK_JNI_CFLAGS)" GOOS=linux GOARCH=amd64 CC=gcc $(GOBUILD) -o "$@" .

$(DESTDIR)/libkeyring-windows-amd64.dll: $(wildcard *.go)
	@mkdir -p "$(DESTDIR)"
	CGO_CFLAGS="$(JDK_JNI_CFLAGS)" GOOS=windows GOARCH=amd64 \
		CC=x86_64-w64-mingw32-gcc $(GOBUILD) -o "$@" .

$(DESTDIR)/libkeyring-windows-arm64.dll: $(wildcard *.go)
	@mkdir -p "$(DESTDIR)"
	CGO_CFLAGS="$(JDK_JNI_CFLAGS)" GOOS=windows GOARCH=arm64 \
		CC=aarch64-w64-mingw32-gcc $(GOBUILD) -o "$@" .

$(DESTDIR)/libkeyring-darwin-amd64.dylib: $(wildcard *.go)
	@mkdir -p "$(DESTDIR)"
	CGO_CFLAGS="$(JDK_JNI_CFLAGS)" GOOS=darwin GOARCH=amd64 $(GOBUILD) -o "$@" .

$(DESTDIR)/libkeyring-darwin-arm64.dylib: $(wildcard *.go)
	@mkdir -p "$(DESTDIR)"
	CGO_CFLAGS="$(JDK_JNI_CFLAGS)" GOOS=darwin GOARCH=arm64 $(GOBUILD) -o "$@" .

build_all: $(foreach plat,$(PLATFORMS),$(DESTDIR)/libkeyring-$(plat).$(if $(findstring windows,$(plat)),dll,$(if $(findstring darwin,$(plat)),dylib,so)))

copy_to_resources: build_all
	@for plat in $(PLATFORMS); do \
		os=`echo "$$plat" | cut -d- -f1`; \
		arch=`echo "$$plat" | cut -d- -f2`; \
		subdir=; \
		if [ "$$os" = "linux" ]   && [ "$$arch" = "amd64" ]; then subdir=linux-x64; fi; \
		if [ "$$os" = "linux" ]   && [ "$$arch" = "arm64" ]; then subdir=linux-aarch64; fi; \
		if [ "$$os" = "windows" ] && [ "$$arch" = "amd64" ]; then subdir=win32-x64; fi; \
		if [ "$$os" = "windows" ] && [ "$$arch" = "arm64" ]; then subdir=win32-aarch64; fi; \
		if [ "$$os" = "darwin" ]  && [ "$$arch" = "amd64" ]; then subdir=darwin-x64; fi; \
		if [ "$$os" = "darwin" ]  && [ "$$arch" = "arm64" ]; then subdir=darwin-aarch64; fi; \
		libext=so; \
		dest_name=libkeyring.so; \
		if [ "$$os" = "windows" ]; then libext=dll; dest_name=keyring.dll; fi; \
		if [ "$$os" = "darwin" ]; then libext=dylib; dest_name=libkeyring.dylib; fi; \
		dest_dir="$(RESOURCEDIR)/$$subdir"; \
		mkdir -p "$$dest_dir"; \
		cp "$(DESTDIR)/libkeyring-$$plat.$$libext" "$$dest_dir/$$dest_name"; \
		echo "Copied $$plat -> $$dest_dir/$$dest_name"; \
	done

all: copy_to_resources

clean:
	rm -rf "$(DESTDIR)"

.PHONY: default all build_all copy_to_resources clean
.DELETE_ON_ERROR:
