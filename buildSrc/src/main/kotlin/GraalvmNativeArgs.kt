object GraalvmNativeArgs {
    fun gui(osName: String, osArch: String = System.getProperty("os.arch").orEmpty()): List<String> =
        buildList {
            add("-H:+UnlockExperimentalVMOptions")
            add("-H:-IncludeMethodData")
            addAll(excludeForeignNatives(osName))
            addAll(excludeForeignArchNatives(osName, osArch))
        }

    fun daemon(osName: String, osArch: String = System.getProperty("os.arch").orEmpty()) =
        gui(osName, osArch) + listOf("-H:IncludeLocales=en")

    // Linux bundles natives for every arch, so we drop the ones not relevant to this image
    private fun excludeForeignArchNatives(osName: String, osArch: String): List<String> {
        val os = osName.lowercase()
        if (os.startsWith("windows") || os.startsWith("mac") || os.contains("darwin")) {
            return emptyList()
        }
        val isArm = osArch.lowercase().let { it == "aarch64" || it == "arm64" }
        return if (isArm) {
            listOf("-H:ExcludeResources=.*linux-(x64|x86-64|x86_64|amd64)[/.].*")
        } else {
            listOf("-H:ExcludeResources=.*linux-(aarch64|arm64)[/.].*")
        }
    }

    private fun excludeForeignNatives(osName: String): List<String> {
        val os = osName.lowercase()
        return when {
            os.startsWith("windows") ->
                listOf(
                    "-H:ExcludeResources=natives/linux.*",
                    "-H:ExcludeResources=natives/darwin.*",
                    "-H:ExcludeResources=natives/osx.*",
                    "-H:ExcludeResources=nucleus/native/linux-.*",
                    "-H:ExcludeResources=nucleus/native/darwin-.*",
                    "-H:ExcludeResources=composetray/native/linux-.*",
                    "-H:ExcludeResources=composetray/native/darwin-.*",
                    "-H:ExcludeResources=com/sun/jna/linux-.*",
                    "-H:ExcludeResources=com/sun/jna/darwin.*",
                    "-H:ExcludeResources=com/sun/jna/aix-.*",
                    "-H:ExcludeResources=.*\\.so$",
                    "-H:ExcludeResources=.*\\.dylib$",
                )
            os.startsWith("mac") || os.contains("darwin") ->
                listOf(
                    "-H:ExcludeResources=natives/linux.*",
                    "-H:ExcludeResources=natives/windows.*",
                    "-H:ExcludeResources=nucleus/native/linux-.*",
                    "-H:ExcludeResources=nucleus/native/win32-.*",
                    "-H:ExcludeResources=composetray/native/linux-.*",
                    "-H:ExcludeResources=composetray/native/win32-.*",
                    "-H:ExcludeResources=com/sun/jna/linux-.*",
                    "-H:ExcludeResources=com/sun/jna/win32-.*",
                    "-H:ExcludeResources=com/sun/jna/aix-.*",
                    "-H:ExcludeResources=.*\\.so$",
                    "-H:ExcludeResources=.*\\.dll$",
                )
            else ->
                listOf(
                    "-H:ExcludeResources=natives/windows.*",
                    "-H:ExcludeResources=natives/darwin.*",
                    "-H:ExcludeResources=natives/osx.*",
                    "-H:ExcludeResources=nucleus/native/win32-.*",
                    "-H:ExcludeResources=nucleus/native/darwin-.*",
                    "-H:ExcludeResources=composetray/native/win32-.*",
                    "-H:ExcludeResources=composetray/native/darwin-.*",
                    "-H:ExcludeResources=com/sun/jna/win32-.*",
                    "-H:ExcludeResources=com/sun/jna/darwin.*",
                    "-H:ExcludeResources=com/sun/jna/aix-.*",
                    "-H:ExcludeResources=.*\\.dll$",
                    "-H:ExcludeResources=.*\\.dylib$",
                )
        }
    }
}
