package com.zaneschepke.wireguardautotunnel.desktop.ui.navigation

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.StringResource

@Keep
@Serializable
sealed class Route : NavKey {

    @Keep @Serializable data object Support : Route()

    @Keep @Serializable data object Lock : Route()

    @Keep @Serializable data object License : Route()

    @Keep @Serializable data object Logs : Route()

    @Keep @Serializable data object Appearance : Route()

    @Keep @Serializable data object Language : Route()

    @Keep @Serializable data object Display : Route()

    @Keep @Serializable data object Tunnels : Route()

    @Keep @Serializable data class Tunnel(val id: Long) : Route()

    @Keep @Serializable data class SplitTunnel(val id: Int) : Route()

    @Keep @Serializable data class ConfigGlobal(val id: Int?) : Route()

    @Keep @Serializable data class SplitTunnelGlobal(val id: Int) : Route()

    @Keep @Serializable data object Settings : Route()

    @Keep @Serializable data object TunnelMonitoring : Route()

    @Keep @Serializable data object AndroidIntegrations : Route()

    @Keep @Serializable data object Dns : Route()

    @Keep @Serializable data object ProxySettings : Route()

    @Keep @Serializable data object LockdownSettings : Route()

    @Keep @Serializable data object AutoTunnel : Route()

    @Keep @Serializable data object AdvancedAutoTunnel : Route()

    @Keep @Serializable data object WifiDetectionMethod : Route()

    @Keep @Serializable data object WifiPreferences : Route()

    @Keep @Serializable data object LocationDisclosure : Route()

    @Keep @Serializable data object Donate : Route()

    @Keep @Serializable data object Addresses : Route()

    @Keep @Serializable data class PreferredTunnel(val tunnelNetwork: TunnelNetwork) : Route()

    @Keep @Serializable data object PingTarget : Route()
}

@Serializable
enum class TunnelNetwork {
    MOBILE_DATA,
    ETHERNET,
    WIFI,
}

enum class Tab(
    val startRoute: Route,
    val titleRes: StringResource,
    val inactiveIcon: ImageVector,
    val activeIcon: ImageVector,
    val index: Int,
) {
    TUNNELS(Route.Tunnels, Res.string.tunnels, Icons.Outlined.Home, Icons.Filled.Home, 0),
    AUTOTUNNEL(Route.AutoTunnel, Res.string.auto_tunnel, Icons.Outlined.Bolt, Icons.Filled.Bolt, 1),
    SETTINGS(
        Route.Settings,
        Res.string.settings,
        Icons.Outlined.Settings,
        Icons.Filled.Settings,
        2,
    ),
    SUPPORT(
        Route.Support,
        Res.string.support,
        Icons.Outlined.QuestionMark,
        Icons.Filled.QuestionMark,
        3,
    );

    companion object {
        fun fromRoute(route: Route): Tab =
            when (route) {
                is Route.Tunnels,
                is Route.Tunnel,
                is Route.Lock,
                is Route.SplitTunnel -> TUNNELS
                is Route.AutoTunnel,
                Route.AdvancedAutoTunnel,
                Route.WifiDetectionMethod,
                Route.WifiPreferences,
                is Route.PreferredTunnel,
                Route.LocationDisclosure -> AUTOTUNNEL
                is Route.Settings,
                Route.TunnelMonitoring,
                Route.AndroidIntegrations,
                Route.Dns,
                is Route.SplitTunnelGlobal,
                Route.ProxySettings,
                Route.LockdownSettings,
                Route.Appearance,
                Route.Language,
                Route.Display,
                Route.PingTarget,
                is Route.ConfigGlobal,
                Route.Logs -> SETTINGS
                is Route.Support,
                Route.License,
                Route.Donate,
                Route.Addresses -> SUPPORT
            }
    }
}

fun routeSerializersModule(): SerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(Route.Support::class, Route.Support.serializer())
        subclass(Route.Lock::class, Route.Lock.serializer())
        subclass(Route.License::class, Route.License.serializer())
        subclass(Route.Logs::class, Route.Logs.serializer())
        subclass(Route.Appearance::class, Route.Appearance.serializer())
        subclass(Route.Language::class, Route.Language.serializer())
        subclass(Route.Display::class, Route.Display.serializer())
        subclass(Route.Tunnels::class, Route.Tunnels.serializer())
        subclass(Route.Tunnel::class, Route.Tunnel.serializer())
        subclass(Route.SplitTunnel::class, Route.SplitTunnel.serializer())
        subclass(Route.ConfigGlobal::class, Route.ConfigGlobal.serializer())
        subclass(Route.SplitTunnelGlobal::class, Route.SplitTunnelGlobal.serializer())
        subclass(Route.Settings::class, Route.Settings.serializer())
        subclass(Route.TunnelMonitoring::class, Route.TunnelMonitoring.serializer())
        subclass(Route.AndroidIntegrations::class, Route.AndroidIntegrations.serializer())
        subclass(Route.Dns::class, Route.Dns.serializer())
        subclass(Route.ProxySettings::class, Route.ProxySettings.serializer())
        subclass(Route.LockdownSettings::class, Route.LockdownSettings.serializer())
        subclass(Route.AutoTunnel::class, Route.AutoTunnel.serializer())
        subclass(Route.AdvancedAutoTunnel::class, Route.AdvancedAutoTunnel.serializer())
        subclass(Route.WifiDetectionMethod::class, Route.WifiDetectionMethod.serializer())
        subclass(Route.WifiPreferences::class, Route.WifiPreferences.serializer())
        subclass(Route.LocationDisclosure::class, Route.LocationDisclosure.serializer())
        subclass(Route.Donate::class, Route.Donate.serializer())
        subclass(Route.Addresses::class, Route.Addresses.serializer())
        subclass(Route.PreferredTunnel::class, Route.PreferredTunnel.serializer())
        subclass(Route.PingTarget::class, Route.PingTarget.serializer())
    }
}
