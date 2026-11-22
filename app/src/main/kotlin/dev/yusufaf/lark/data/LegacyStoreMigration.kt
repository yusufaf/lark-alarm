package dev.yusufaf.lark.data

import android.content.Context
import android.os.UserManager
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.dataStoreFile
import dev.yusufaf.lark.core.AlarmStore
import dev.yusufaf.lark.core.withLegacy
import dev.yusufaf.lark.schedule.AlarmScheduler
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Moves alarms.json out of credential-encrypted storage, where it lived before #43. Copies,
 * then deletes: a rename can't cross storage classes.
 */
internal class LegacyStoreMigration(private val context: Context) : DataMigration<AlarmStore> {
    // context is the normal (credential-encrypted) app context, so this is the old path.
    private val legacyFile get() = context.dataStoreFile(ALARM_STORE_FILE)
    private var read = false

    // Before the first unlock the old file can't be read; a later process start, BootReceiver
    // or MainActivity retries.
    override suspend fun shouldMigrate(currentData: AlarmStore): Boolean =
        context.getSystemService(UserManager::class.java).isUserUnlocked && legacyFile.exists()

    override suspend fun migrate(currentData: AlarmStore): AlarmStore {
        val legacy = try {
            legacyFile.inputStream().use { AlarmStoreSerializer.readFrom(it) }.also { read = true }
        } catch (e: CorruptionException) {
            read = true // same outcome as the store's own corruption handler: start over
            Log.e(AlarmScheduler.TAG, "old alarm store is corrupt; dropping it", e)
            null
        } catch (e: IOException) {
            Log.e(AlarmScheduler.TAG, "couldn't read the old alarm store; will retry", e)
            null
        }
        val result = currentData.withLegacy(legacy)
        when {
            legacy == null -> {}
            result === currentData ->
                Log.w(AlarmScheduler.TAG, "kept the device-protected alarm store; dropping a stale old one")
            else -> {
                moved.set(true)
                Log.i(AlarmScheduler.TAG, "moved alarm store to device-protected storage: ${result.alarms.size} alarms")
            }
        }
        return result
    }

    // A throw here would fail DataStore init for every reader; a file left behind is retried next time.
    override suspend fun cleanUp() {
        if (read && !legacyFile.delete()) Log.w(AlarmScheduler.TAG, "couldn't delete the old alarm store")
    }

    companion object {
        // Process-wide: the store's own migration and migrateLegacyStore() use different instances.
        private val moved = AtomicBoolean(false)

        /** True once per move: alarms were taken from the old store and nobody has re-armed them yet. */
        fun takeMoved(): Boolean = moved.getAndSet(false)
    }
}
