# sqliteJni for Windows ARM64

`androidx.sqlite:sqlite-bundled` (used by Room in the `client` module) ships native
binaries for Windows x64, Linux x64/arm64 and macOS x64/arm64, but **not** for
Windows ARM64 (checked up to 2.7.0). Its `NativeLibraryLoader` already supports the
platform (`natives/windows_arm64/sqliteJni.dll`), only the binary is missing.

This directory contains our own Windows ARM64 build of that native library:

- `sqlite_bindings.cpp` — vendored verbatim from androidx (Apache-2.0),
  [`sqlite/sqlite-bundled/src/jvmAndAndroidMain/jni/sqlite_bindings.cpp`](https://github.com/androidx/androidx/blob/androidx-main/sqlite/sqlite-bundled/src/jvmAndAndroidMain/jni/sqlite_bindings.cpp)
  at commit `58fa83534ba68a3d3f2abf50001bdd9a593ef865`. Its 22 registered JNI methods
  match the `native` methods of sqlite-bundled 2.6.2 exactly (verified with `javap`).
- `build-windows-arm64.sh` — build script (llvm-mingw + SQLite 3.50.1 amalgamation,
  same version and `SQLITE_COMPILE_FLAGS` androidx pins in its `build.gradle`).
- `arm64/sqliteJni.dll` — the prebuilt result, statically linked (no runtime deps).

`conveyor.conf` adds the DLL as an input on `windows.aarch64` only. At runtime the
loader finds it through `System.loadLibrary("sqliteJni")` since the app directory is
on `java.library.path` in Conveyor packages. On all other platforms the official
binaries inside the sqlite-bundled JAR keep being used.

When bumping the `androidx-sqlite` version in `gradle/libs.versions.toml`, check
whether upstream added a `natives/windows_arm64/` binary (then this whole directory
can be deleted), and if not, re-verify the JNI bindings still match (`javap -p` on
`BundledSQLite*Kt` classes) and rebuild.

Upstream request tracker: https://issuetracker.google.com/issues/new?component=460784
