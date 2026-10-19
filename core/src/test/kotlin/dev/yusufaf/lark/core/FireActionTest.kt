package dev.yusufaf.lark.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FireActionTest {
    private val a = 1_790_000_000_000L
    private val c = a + 60_000L

    private fun alarm(armedFor: Long?, id: Long = 1) = Alarm(
        id = id,
        hour = 10,
        minute = 0,
        days = emptySet(),
        enabled = true,
        snoozedUntilEpochMs = null,
        armedForEpochMs = armedFor,
    )

    @Test
    fun nothingRingingRings() {
        assertEquals(FireAction.Ring, fireAction(null, 1, a, alarm(a)))
    }

    @Test
    fun nothingRingingAndDeletedIsUnknown() {
        assertEquals(FireAction.Unknown, fireAction(null, 1, a, null))
    }

    @Test
    fun sameOccurrenceIsDuplicate() {
        assertEquals(FireAction.Duplicate, fireAction(Ringing(1, a), 1, a, alarm(a)))
    }

    // #25: comparing with the stored instant took this for a re-arm and settled over the save.
    @Test
    fun duplicateAfterMidRingSaveIsStillDuplicate() {
        assertEquals(FireAction.Duplicate, fireAction(Ringing(1, a), 1, a, alarm(c)))
    }

    @Test
    fun reArmAfterMidRingSaveOverlaps() {
        assertEquals(FireAction.Overlap, fireAction(Ringing(1, a), 1, c, alarm(c)))
    }

    @Test
    fun otherAlarmMidRingOverlaps() {
        assertEquals(FireAction.Overlap, fireAction(Ringing(1, a), 2, c, alarm(c, id = 2)))
    }

    @Test
    fun otherAlarmDeletedMidRingIsUnknown() {
        assertEquals(FireAction.Unknown, fireAction(Ringing(1, a), 2, c, null))
    }

    // A legacy alarm (none stored, fire armed before the upgrade) rings with no instant.
    @Test
    fun noInstantFireOfNoInstantRingIsDuplicate() {
        assertEquals(FireAction.Duplicate, fireAction(Ringing(1, null), 1, null, alarm(null)))
    }

    @Test
    fun reArmOfNoInstantRingOverlaps() {
        assertEquals(FireAction.Overlap, fireAction(Ringing(1, null), 1, c, alarm(c)))
    }

    // A fire in flight while the user saved settles against its own instant, so the save stands.
    @Test
    fun firedForPrefersTheFiresInstant() {
        assertEquals(a, firedFor(a, alarm(c)))
    }

    @Test
    fun legacyFireFallsBackToStoredInstant() {
        assertEquals(c, firedFor(null, alarm(c)))
        assertNull(firedFor(null, null))
    }
}
