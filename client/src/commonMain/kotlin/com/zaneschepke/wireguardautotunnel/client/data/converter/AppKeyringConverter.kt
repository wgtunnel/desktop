package com.zaneschepke.wireguardautotunnel.client.data.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.zaneschepke.wireguardautotunnel.client.data.model.EncryptedField
import com.zaneschepke.wireguardautotunnel.core.crypto.Crypto
import org.koin.java.KoinJavaComponent
import javax.crypto.SecretKey

@ProvidedTypeConverter
class AppKeyringConverter {

    private val secretKey: SecretKey by KoinJavaComponent.inject(SecretKey::class.java)

    @TypeConverter
    fun decryptQuick(encryptedQuick: String): EncryptedField {
        return EncryptedField(Crypto.decryptWithMasterKey(encryptedQuick, secretKey))
    }

    @TypeConverter
    fun encryptQuick(quick: EncryptedField): String {
        return Crypto.encryptWithMasterKey(quick.value, secretKey)
    }
}