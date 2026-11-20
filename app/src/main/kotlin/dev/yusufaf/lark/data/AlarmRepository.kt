package dev.yusufaf.lark.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.deviceProtectedDataStore
import dev.yusufaf.lark.core.Alarm
import dev.yusufaf.lark.core.AlarmStore
import dev.yusufaf.lark.core.without
import dev.yusufaf.lark.schedule.AlarmScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal const val ALARM_STORE_FILE = "alarms.json"

// Device-protected, so alarms re-arm and ring before the first unlock after a reboot (#43).
// A corrupt file must not take the alarm clock down with it: start over empty instead
// of throwing to every reader (including RingService, before it can startForeground()).
private val Context.alarmDataStore: DataStore<AlarmStore> by deviceProtectedDataStore(
    fileName = ALARM_STORE_FILE,
    serializer = AlarmStoreSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { AlarmStore() },
    produceMigrations = { context -> listOf(LegacyStoreMigration(context)) },
)

class AlarmRepository(context: Context) {
    private val appContext = context.applicationContext
    private val store = appContext.alarmDataStore

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

    // armedForEpochMs belongs to the scheduler; a copy read earlier must not roll it back.
    suspend fun update(alarm: Alarm) {
        store.updateData { current ->
            current.copy(
                alarms = current.alarms.map {
                    if (it.id == alarm.id) alarm.copy(armedForEpochMs = it.armedForEpochMs) else it
                },
            )
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        store.updateData { current ->
            current.copy(
                alarms = current.alarms.map {
                    if (it.id == id) {
                        it.copy(enabled = enabled, snoozedUntilEpochMs = null, armedForEpochMs = null)
                    } else {
                        it
                    }
                },
            )
        }
    }

    suspend fun setArmedFor(id: Long, epochMs: Long?) {
        store.updateData { current ->
            current.copy(
                alarms = current.alarms.map {
                    if (it.id == id) it.copy(armedForEpochMs = epochMs) else it
                },
            )
        }
    }

    suspend fun setSnoozedUntil(id: Long, epochMs: Long?) {
        store.updateData { current ->
            current.copy(
                alarms = current.alarms.map {
                    if (it.id == id) it.copy(snoozedUntilEpochMs = epochMs) else it
                },
            )
        }
    }

    suspend fun delete(id: Long) {
        store.updateData { it.without(id) }
    }

    /**
     * Picks up the old credential-encrypted store when this process started before the first unlock,
     * so the DataStore migration was skipped (it runs once per process). Cheap when there is nothing
     * to move. Returns true when this process moved alarms in (here or in the store's own migration)
     * that no caller has re-armed yet.
     */
    suspend fun migrateLegacyStore(): Boolean {
        val migration = LegacyStoreMigration(appContext)
        try {
            var ran = false
            store.updateData { current ->
                if (!migration.shouldMigrate(current)) return@updateData current
                ran = true
                migration.migrate(current)
            }
            if (ran) migration.cleanUp()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AlarmScheduler.TAG, "failed to move the old alarm store", e)
        }
        return LegacyStoreMigration.takeMoved()
    }
}
