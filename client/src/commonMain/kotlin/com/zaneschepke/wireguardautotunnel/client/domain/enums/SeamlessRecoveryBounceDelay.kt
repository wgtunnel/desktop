package com.zaneschepke.wireguardautotunnel.client.domain.enums

enum class SeamlessRecoveryBounceDelay(val seconds: Int) {
    TEN(10),
    FIFTEEN(15),
    TWENTY(20),
    THIRTY(30),
    FORTY_FIVE(45),
    SIXTY(60),
    NINETY(90),
    TWO_MINUTES(120);

    companion object {
        fun fromSeconds(seconds: Int): SeamlessRecoveryBounceDelay =
            entries.find { it.seconds == seconds } ?: THIRTY
    }
}
