package com.zaneschepke.wireguardautotunnel.cli.provider

import picocli.CommandLine

class ManifestVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> {
        val version = ManifestVersionProvider::class.java.getPackage().implementationVersion
        return if (version != null) arrayOf(version) else arrayOf("Unknown version")
    }
}