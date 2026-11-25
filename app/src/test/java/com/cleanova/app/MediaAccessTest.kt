package com.cleanova.app
import com.cleanova.app.domain.*
import org.junit.Assert.*
import org.junit.Test
class MediaAccessTest {
    @Test fun denied() { assertFalse(MediaAccess(Grant.NONE, Grant.NONE).hasAny) }
    @Test fun selectedIsNotFull() { assertEquals("Limited", MediaAccess(Grant.SELECTED, Grant.SELECTED).label) }
    @Test fun mixedGrants() { assertFalse(MediaAccess(Grant.FULL, Grant.NONE).isFull) }
    @Test fun full() { assertTrue(MediaAccess(Grant.FULL, Grant.FULL).isFull) }
}
