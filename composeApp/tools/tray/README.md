# composenativetray native for Windows ARM64

`io.github.kdroidfilter:composenativetray` ships its Windows ARM64 native inside the
JAR under `win32-arm64/tray.dll`, but JNA's resource prefix on Windows ARM64 is
`win32-aarch64`, so the classpath lookup never finds it and the tray fails with
`Unable to load library 'tray'`.

`arm64/tray.dll` is that same DLL extracted verbatim from the
`composenativetray-jvm` JAR (version pinned in `gradle/libs.versions.toml`).
`conveyor.conf` ships it as a `windows.aarch64` input so JNA finds it through
`jna.library.path` in the packaged app.

When bumping the composenativetray version: re-extract the DLL from the new JAR
(`unzip -p composenativetray-jvm-<v>.jar win32-arm64/tray.dll > arm64/tray.dll`),
and check whether upstream fixed the resource folder name (then this directory and
the conveyor input can be removed).
