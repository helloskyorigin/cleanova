package com.cleanova.app
import com.cleanova.app.data.Capacity
import org.junit.Assert.assertEquals
import org.junit.Test
class CapacityTest {
    @Test fun normalCapacity() {
        val c = Capacity(100, 25)
        assertEquals(75L, c.used)
        assertEquals(.75f, c.fraction, .001f)
    }
    @Test fun zeroCapacityDoesNotDivideByZero() {
        assertEquals(0f, Capacity(0, 0).fraction, .001f)
    }
    @Test fun negativeUsedIsClamped() {
        assertEquals(0L, Capacity(100, 110).used)
    }
}
