package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support.donate.crypto

import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.avalanche
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.avalanche_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.bitcoin
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.bitcoin_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.bitcoin_cash
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.bitcoin_cash_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.btc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ecash
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ecash_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.eth
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ethereum
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ethereum_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.litecoin
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.litecoin_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ltc
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.monero
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.monero_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.polygon
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.polygon_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.solana
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.solana_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.stellar
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.stellar_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tron
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.tron_address
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.xmr
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.zcash
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.zcash_address
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class Address(
    val name: StringResource,
    val address: StringResource,
    val icon: DrawableResource,
) {
    companion object {
        val allAddresses =
            listOf(
                Address(
                    name = Res.string.bitcoin,
                    address = Res.string.bitcoin_address,
                    icon = Res.drawable.btc,
                ),
                Address(
                    name = Res.string.monero,
                    address = Res.string.monero_address,
                    icon = Res.drawable.xmr,
                ),
                Address(
                    name = Res.string.ethereum,
                    address = Res.string.ethereum_address,
                    icon = Res.drawable.eth,
                ),
                Address(
                    name = Res.string.zcash,
                    address = Res.string.zcash_address,
                    icon = Res.drawable.zcash,
                ),
                Address(
                    name = Res.string.litecoin,
                    address = Res.string.litecoin_address,
                    icon = Res.drawable.ltc,
                ),
                Address(
                    name = Res.string.ecash,
                    address = Res.string.ecash_address,
                    icon = Res.drawable.ecash,
                ),
                Address(
                    name = Res.string.polygon,
                    address = Res.string.polygon_address,
                    icon = Res.drawable.polygon,
                ),
                Address(
                    name = Res.string.avalanche,
                    address = Res.string.avalanche_address,
                    icon = Res.drawable.avalanche,
                ),
                Address(
                    name = Res.string.solana,
                    address = Res.string.solana_address,
                    icon = Res.drawable.solana,
                ),
                Address(
                    name = Res.string.stellar,
                    address = Res.string.stellar_address,
                    icon = Res.drawable.stellar,
                ),
                Address(
                    name = Res.string.tron,
                    address = Res.string.tron_address,
                    icon = Res.drawable.tron,
                ),
                Address(
                    name = Res.string.bitcoin_cash,
                    address = Res.string.bitcoin_cash_address,
                    icon = Res.drawable.bitcoin_cash,
                ),
            )
    }
}
