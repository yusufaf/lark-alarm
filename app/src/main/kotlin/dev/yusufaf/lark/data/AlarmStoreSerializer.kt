package dev.yusufaf.lark.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import dev.yusufaf.lark.core.AlarmStore
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** Stores the whole [AlarmStore] as one JSON document; the data set is tiny. */
object AlarmStoreSerializer : Serializer<AlarmStore> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: AlarmStore = AlarmStore()

    override suspend fun readFrom(input: InputStream): AlarmStore = try {
        json.decodeFromString(AlarmStore.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("alarms.json is not a valid AlarmStore", e)
    } catch (e: IllegalArgumentException) {
        // DayOfWeek.of() rejects values outside 1..7 with this, not a SerializationException.
        throw CorruptionException("alarms.json holds an invalid value", e)
    }

    override suspend fun writeTo(t: AlarmStore, output: OutputStream) {
        output.write(json.encodeToString(AlarmStore.serializer(), t).encodeToByteArray())
    }
}
