# Development

Run the daemon from a packaged-style layout (not as root Gradle):

```shell
mise install
./gradlew :daemon:run
```

`JAVA_HOME` is provided by the project toolchain (mise + Gradle Temurin 25). Do not pass a global JDK 27 `JAVA_HOME` into the build.
