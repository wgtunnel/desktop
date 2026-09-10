package com.zaneschepke.wireguardautotunnel.desktop.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.NoEncryption
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.wgtunnel.backend.model.dns.DnsValidationError
import com.zaneschepke.wireguardautotunnel.client.domain.enums.BootstrapDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.enums.SeamlessRecoveryBounceDelay
import com.zaneschepke.wireguardautotunnel.client.domain.enums.SplitDnsSuffixTarget
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TransitDnsPolicy
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsMode
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelMode
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources._default
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.default_dns_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_empty
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_host
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_ip_or_host
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_port
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_scheme
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.doh
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dot
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.encrypted_dns
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.encrypted_dns_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.local_proxy
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.local_proxy_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.minutes_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.plain_dns
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.proxy
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.seconds_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.split_dns
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.split_dns_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.split_suffix_target_desc_system
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.split_suffix_target_desc_tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.split_suffix_target_system
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.split_suffix_target_tunnel
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.system
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.system_dns_only
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.system_dns_only_desc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.transit_dns_allow
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.transit_dns_block
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.transit_dns_redirect
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.vpn
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.vpn_desc
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun TunnelMode.asTitleString(): String =
    when (this) {
        TunnelMode.VPN -> stringResource(Res.string.vpn)
        TunnelMode.PROXY -> stringResource(Res.string.local_proxy)
    }

@Composable
fun TunnelMode.asDescription(): String =
    when (this) {
        TunnelMode.VPN -> stringResource(Res.string.vpn_desc)
        TunnelMode.PROXY -> stringResource(Res.string.local_proxy_desc)
    }

@Composable
fun TunnelMode.asIcon(): ImageVector =
    when (this) {
        TunnelMode.VPN -> Icons.Outlined.VpnKey
        TunnelMode.PROXY -> vectorResource(Res.drawable.proxy)
    }

fun desktopTunnelModes(): List<TunnelMode> = listOf(TunnelMode.VPN, TunnelMode.PROXY)

@Composable
fun TunnelDnsMode.asLabel(): String =
    when (this) {
        TunnelDnsMode.Off -> stringResource(Res.string._default)
        TunnelDnsMode.Encrypted -> stringResource(Res.string.encrypted_dns)
        TunnelDnsMode.Split -> stringResource(Res.string.split_dns)
        TunnelDnsMode.AllLocal -> stringResource(Res.string.system_dns_only)
    }

@Composable
fun TunnelDnsMode.asDescription(): String =
    when (this) {
        TunnelDnsMode.Off -> stringResource(Res.string.default_dns_desc)
        TunnelDnsMode.Encrypted -> stringResource(Res.string.encrypted_dns_desc)
        TunnelDnsMode.Split -> stringResource(Res.string.split_dns_desc)
        TunnelDnsMode.AllLocal -> stringResource(Res.string.system_dns_only_desc)
    }

@Composable
fun TunnelDnsMode.asIcon(): ImageVector =
    when (this) {
        TunnelDnsMode.Off -> Icons.Outlined.NoEncryption
        TunnelDnsMode.Encrypted -> Icons.Outlined.EnhancedEncryption
        TunnelDnsMode.Split -> Icons.AutoMirrored.Outlined.CallSplit
        TunnelDnsMode.AllLocal -> Icons.Outlined.Wifi
    }

@Composable
fun TunnelDnsProtocol.asLabel(): String =
    when (this) {
        TunnelDnsProtocol.Doh -> stringResource(Res.string.doh)
        TunnelDnsProtocol.Dot -> stringResource(Res.string.dot)
        TunnelDnsProtocol.Plain -> stringResource(Res.string.plain_dns)
    }

@Composable
fun BootstrapDnsProtocol.asLabel(): String =
    when (this) {
        BootstrapDnsProtocol.SYSTEM -> stringResource(Res.string.system)
        BootstrapDnsProtocol.DOH -> stringResource(Res.string.doh)
        BootstrapDnsProtocol.DOT -> stringResource(Res.string.dot)
        BootstrapDnsProtocol.UDP -> stringResource(Res.string.plain_dns)
    }

@Composable
fun SplitDnsSuffixTarget.asLabel(): String =
    when (this) {
        SplitDnsSuffixTarget.System -> stringResource(Res.string.split_suffix_target_system)
        SplitDnsSuffixTarget.Tunnel -> stringResource(Res.string.split_suffix_target_tunnel)
    }

@Composable
fun SplitDnsSuffixTarget.asDescription(): String =
    when (this) {
        SplitDnsSuffixTarget.System -> stringResource(Res.string.split_suffix_target_desc_system)
        SplitDnsSuffixTarget.Tunnel -> stringResource(Res.string.split_suffix_target_desc_tunnel)
    }

@Composable
fun TransitDnsPolicy.asLabel(): String =
    when (this) {
        TransitDnsPolicy.Redirect -> stringResource(Res.string.transit_dns_redirect)
        TransitDnsPolicy.Block -> stringResource(Res.string.transit_dns_block)
        TransitDnsPolicy.Allow -> stringResource(Res.string.transit_dns_allow)
    }

@Composable
fun DnsValidationError.asLabel(): String =
    when (this) {
        DnsValidationError.Empty -> stringResource(Res.string.dns_error_empty)
        DnsValidationError.InvalidUrl -> stringResource(Res.string.dns_error_invalid_url)
        DnsValidationError.InvalidScheme -> stringResource(Res.string.dns_error_invalid_scheme)
        DnsValidationError.InvalidHost -> stringResource(Res.string.dns_error_invalid_host)
        DnsValidationError.InvalidPort -> stringResource(Res.string.dns_error_invalid_port)
        DnsValidationError.InvalidIpOrHost ->
            stringResource(Res.string.dns_error_invalid_ip_or_host)
    }

@Composable
fun SeamlessRecoveryBounceDelay.asLabel(): String =
    if (seconds >= 60) stringResource(Res.string.minutes_template, seconds / 60)
    else stringResource(Res.string.seconds_template, seconds)
