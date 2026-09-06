object GraalvmNativeArgs {
    fun gui(osName: String): List<String> = buildList {
        add("-H:+UnlockExperimentalVMOptions")
        add("-H:-IncludeMethodData")
        addAll(excludeForeignNatives(osName))
    }

    fun daemon(osName: String): List<String> = gui(osName) + listOf("-H:IncludeLocales=en")

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
