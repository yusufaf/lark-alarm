package dev.yusufaf.lark.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.core.AlarmStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// A corrupt file must not take the alarm clock down with it: start over empty instead
// of throwing to every reader (including RingService, before it can startForeground()).
private val Context.alarmDataStore: DataStore<AlarmStore> by dataStore(
    fileName = "alarms.json",
    serializer = AlarmStoreSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { AlarmStore() },
)

class AlarmRepository(context: Context) {
    private val store = context.applicationContext.alarmDataStore

    val alarms: Flow<List<Alarm>> = store.data.map { it.alarms }

    suspend fun snapshot(): List<Alarm> = store.data.first().alarms

    suspend fun get(id: Long): Alarm? = snapshot().firstOrNull { it.id == id }

    /** Persists [alarm] under a fresh id (the passed id is ignored) and returns it. */
    suspend fun add(alarm: Alarm): Alarm {
        var saved = alarm
        store.updateData { current ->
            saved = alarm.copy(id = current.nextId)
            current.copy(alarms = current.alarms + saved, nextId = current.nextId + 1)
        }
        return saved
    }

    suspend fun update(alarm: Alarm) {
        store.updateData { current ->
            current.copy(alarms = current.alarms.map { if (it.id == alarm.id) alarm else it })
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        store.updateData { current ->
            current.copy(
                alarms = current.alarms.map {
                    if (it.id == id) it.copy(enabled = enabled, snoozedUntilEpochMs = null) else it
                },
            )
        }
    }
}
