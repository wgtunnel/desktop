package com.zaneschepke.wireguardautotunnel.daemon.tunnel

import com.wgtunnel.backend.DesktopApplicationProvider
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper

class DaemonApplicationProvider : DesktopApplicationProvider {
    override val uapiPath: String
        get() = FilePathsHelper.getDaemonRuntimeDir().toString()

    override fun refreshStatusUi() {
        // TODO
    }
}
