package com.safecallkids.app.monetization

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class TrialReminderTest {
    @Test
    fun remindersAreDueAtTrialEndAndThreeAndSevenDaysLater() {
        val trialStartedAt = TimeUnit.DAYS.toMillis(100)

        assertEquals(trialStartedAt + TimeUnit.DAYS.toMillis(7), TrialReminder.EXPIRED.dueAtMillis(trialStartedAt))
        assertEquals(trialStartedAt + TimeUnit.DAYS.toMillis(10), TrialReminder.AFTER_THREE_DAYS.dueAtMillis(trialStartedAt))
        assertEquals(trialStartedAt + TimeUnit.DAYS.toMillis(14), TrialReminder.AFTER_SEVEN_DAYS.dueAtMillis(trialStartedAt))
    }

    @Test
    fun workScheduledByThePreviousVersionIsTheExpiryReminder() {
        assertEquals(TrialReminder.EXPIRED, TrialReminder.fromWorkInput(null))
    }
}
