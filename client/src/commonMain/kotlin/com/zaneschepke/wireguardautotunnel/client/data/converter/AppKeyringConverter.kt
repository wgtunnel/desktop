package com.zaneschepke.wireguardautotunnel.client.data.converter

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.zaneschepke.wireguardautotunnel.client.data.model.EncryptedField
import com.zaneschepke.wireguardautotunnel.core.crypto.Crypto
import javax.crypto.SecretKey
import org.koin.java.KoinJavaComponent

@ProvidedColumnTypeConverter
class AppKeyringConverter {

    private val secretKey: SecretKey by KoinJavaComponent.inject(SecretKey::class.java)

    @ColumnTypeConverter
    fun decryptQuick(encryptedQuick: String): EncryptedField {
        return EncryptedField(Crypto.decryptWithMasterKey(encryptedQuick, secretKey))
    }

    @ColumnTypeConverter
    fun encryptQuick(quick: EncryptedField): String {
        return Crypto.encryptWithMasterKey(quick.value, secretKey)
    }
}
