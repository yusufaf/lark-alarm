package dev.yusufaf.lark.core

/** The ring in progress: which alarm, and the occurrence its fire was for (see [firedFor]). */
data class Ringing(val id: Long, val firedFor: Long?)

/** What RingService does with a fire of an alarm. */
sealed interface FireAction {
    /** Nothing is ringing: ring it. */
    data object Ring : FireAction

    /** The same alarm and occurrence as the ring in progress: ignore it. */
    data object Duplicate : FireAction

    /** Another alarm, or a re-arm of this one after a mid-ring save: settle it as rung. */
    data object Overlap : FireAction

    /** Not in the store (deleted). */
    data object Unknown : FireAction
}

/**
 * The occurrence a fire is for: the armed-for instant it carried, else (a fire armed before the
 * fire intent carried it) the stored one.
 */
fun firedFor(extraArmedFor: Long?, stored: Alarm?): Long? = extraArmedFor ?: stored?.armedForEpochMs

fun fireAction(ringing: Ringing?, id: Long, firedFor: Long?, stored: Alarm?): FireAction = when {
    ringing != null && ringing.id == id && ringing.firedFor == firedFor -> FireAction.Duplicate
    stored == null -> FireAction.Unknown
    ringing == null -> FireAction.Ring
    else -> FireAction.Overlap
}
