package com.ceoeg.aetherflow.core.common.security

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.ceoeg.aetherflow.core.common.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalSerializationApi::class)
class EncryptedAppSettingsSerializer(
    private val cipher: CredentialCipher,
) : Serializer<AppSettings> {

    private val protoBuf = ProtoBuf { encodeDefaults = false }

    override val defaultValue: AppSettings = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return defaultValue
        return try {
            val decrypted = cipher.decrypt(bytes)
            protoBuf.decodeFromByteArray(AppSettings.serializer(), decrypted)
        } catch (t: Throwable) {
            throw CorruptionException("Unable to decrypt or decode Proto DataStore settings", t)
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        val rawProto = protoBuf.encodeToByteArray(AppSettings.serializer(), t)
        val encrypted = cipher.encrypt(rawProto)
        output.write(encrypted)
    }
}

@Singleton
class EncryptedSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
    cipher: CredentialCipher,
) {
    private val dataStore: DataStore<AppSettings> = DataStoreFactory.create(
        serializer = EncryptedAppSettingsSerializer(cipher),
        corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler { AppSettings() },
        produceFile = { context.dataStoreFile("aetherflow_settings.pb.enc") },
    )

    val settingsFlow: Flow<AppSettings> = dataStore.data

    suspend fun current(): AppSettings = dataStore.data.first()

    suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        return dataStore.updateData(transform)
    }
}
