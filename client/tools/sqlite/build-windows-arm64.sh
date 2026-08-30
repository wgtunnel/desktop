#!/bin/sh
# Builds sqliteJni.dll for Windows ARM64, see README.md
#
# Requires llvm-mingw on PATH and JAVA_HOME set for jni.h

set -eu

cd "$(dirname "$0")"

SQLITE_YEAR=2025
SQLITE_VERSION=3500100
SQLITE_SHA256=41716b44ac8777188c4c3f1f370f01c9cb9e3b6428eb5c981d086c35de2d9d3f

CC="${CC:-aarch64-w64-mingw32-clang}"
CXX="${CXX:-aarch64-w64-mingw32-clang++}"

if [ -z "${JAVA_HOME:-}" ]; then
    echo "JAVA_HOME must be set (needed for jni.h)" >&2
    exit 1
fi

BUILD_DIR=build
AMALGAMATION="sqlite-amalgamation-$SQLITE_VERSION"
mkdir -p "$BUILD_DIR"

if [ ! -d "$BUILD_DIR/$AMALGAMATION" ]; then
    curl -L -o "$BUILD_DIR/$AMALGAMATION.zip" "https://www.sqlite.org/$SQLITE_YEAR/$AMALGAMATION.zip"
    echo "$SQLITE_SHA256  $BUILD_DIR/$AMALGAMATION.zip" | sha256sum -c
    unzip -q -o "$BUILD_DIR/$AMALGAMATION.zip" -d "$BUILD_DIR"
fi

# Keep in sync with SQLITE_COMPILE_FLAGS in androidx sqlite/sqlite-bundled/build.gradle
SQLITE_FLAGS="-DHAVE_USLEEP=1 \
    -DSQLITE_DEFAULT_AUTOVACUUM=1 \
    -DSQLITE_DEFAULT_MEMSTATUS=0 \
    -DSQLITE_DEFAULT_WAL_SYNCHRONOUS=1 \
    -DSQLITE_ENABLE_COLUMN_METADATA \
    -DSQLITE_ENABLE_FTS3 \
    -DSQLITE_ENABLE_FTS3_PARENTHESIS \
    -DSQLITE_ENABLE_FTS4 \
    -DSQLITE_ENABLE_FTS5 \
    -DSQLITE_ENABLE_JSON1 \
    -DSQLITE_ENABLE_MATH_FUNCTIONS \
    -DSQLITE_ENABLE_NORMALIZE \
    -DSQLITE_ENABLE_RTREE \
    -DSQLITE_ENABLE_STAT4 \
    -DSQLITE_HAVE_ISNAN \
    -DSQLITE_OMIT_BUILTIN_TEST \
    -DSQLITE_OMIT_DEPRECATED \
    -DSQLITE_OMIT_PROGRESS_CALLBACK \
    -DSQLITE_OMIT_SHARED_CACHE \
    -DSQLITE_SECURE_DELETE \
    -DSQLITE_TEMP_STORE=3 \
    -DSQLITE_THREADSAFE=2"

"$CC" -O3 -DNDEBUG $SQLITE_FLAGS \
    -c "$BUILD_DIR/$AMALGAMATION/sqlite3.c" -o "$BUILD_DIR/sqlite3.o"

# _JNI_IMPLEMENTATION_ makes jni.h declare JNI_OnLoad as dllexport.
"$CXX" -O3 -DNDEBUG $SQLITE_FLAGS -D_JNI_IMPLEMENTATION_ \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/win32" -I"$BUILD_DIR/$AMALGAMATION" \
    -c sqlite_bindings.cpp -o "$BUILD_DIR/bindings.o"

mkdir -p arm64
"$CXX" -shared -static -o arm64/sqliteJni.dll "$BUILD_DIR/sqlite3.o" "$BUILD_DIR/bindings.o"

echo "Built arm64/sqliteJni.dll"
