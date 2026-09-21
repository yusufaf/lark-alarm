package dev.yusufaf.lark

import android.app.Application
import dev.yusufaf.lark.data.AlarmRepository
import dev.yusufaf.lark.ring.AlarmNotifications
import dev.yusufaf.lark.schedule.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LarkApplication : Application() {
    // Process-wide scope for work that must outlive any one component, such as
    // rescheduling after boot. Never cancelled: it dies with the process.
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository by lazy { AlarmRepository(this) }
    val scheduler by lazy { AlarmScheduler(this, repository) }

    override fun onCreate() {
        super.onCreate()
        AlarmNotifications.ensureChannel(this)
    }
}
