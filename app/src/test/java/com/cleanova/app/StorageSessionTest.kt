package com.cleanova.app

import com.cleanova.app.core.storage.*
import com.cleanova.app.domain.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageSessionTest {
    private val full = MediaAccess(Grant.FULL, Grant.FULL)
    private val none = MediaAccess(Grant.NONE, Grant.NONE)
    private val limited = MediaAccess(Grant.SELECTED, Grant.SELECTED)
    private fun result(access: MediaAccess) = MediaInventory(5, 2, 500, 900, access)
    private class Reader(
        val load: suspend (MediaAccess) -> MediaInventory,
        val capacityLoad: suspend () -> Capacity = { Capacity(100, 25) }
    ) : StorageReader {
        override suspend fun inventory(access: MediaAccess) = load(access)
        override suspend fun capacity() = capacityLoad()
    }

    @Test fun deniedDoesNotQueryProvider() = runTest {
        val session = StorageSession(Reader({ error("must not scan") }), { none }, this)
        session.scan()
        runCurrent()
        assertEquals(ScanStatus.Idle, session.state.value.scan)
    }

    @Test fun duplicateScanIsIgnoredAndResultIsPublished() = runTest {
        var calls = 0
        val gate = CompletableDeferred<Unit>()
        val session = StorageSession(Reader({ calls++; gate.await(); result(it) }), { full }, this)
        session.scan()
        session.scan()
        runCurrent()
        assertEquals(1, calls)
        assertTrue(session.state.value.scanning)
        gate.complete(Unit)
        runCurrent()
        assertEquals(result(full), session.state.value.inventory)
    }

    @Test fun cancellationRejectsObsoleteCompletion() = runTest {
        val gate = CompletableDeferred<Unit>()
        val session = StorageSession(Reader({ withContext(NonCancellable) { gate.await(); result(it) } }), { full }, this)
        session.scan()
        runCurrent()
        session.cancelScan()
        gate.complete(Unit)
        runCurrent()
        assertEquals(ScanStatus.Idle, session.state.value.scan)
        assertNull(session.state.value.inventory)
    }

    @Test fun grantChangeDiscardsResults() = runTest {
        var access = full
        val gate = CompletableDeferred<Unit>()
        val session = StorageSession(Reader({ gate.await(); result(it) }), { access }, this)
        session.scan()
        runCurrent()
        access = limited
        gate.complete(Unit)
        runCurrent()
        assertEquals(ScanStatus.Failed(ScanFailure.ACCESS_CHANGED), session.state.value.scan)
        assertNull(session.state.value.inventory)
    }

    @Test fun selectedSnapshotClearedOnResume() = runTest {
        val session = StorageSession(Reader({ result(it) }), { limited }, this)
        session.scan()
        runCurrent()
        assertNotNull(session.state.value.inventory)
        session.onResume()
        runCurrent()
        assertNull(session.state.value.inventory)
        assertEquals(Capacity(100, 25), session.state.value.capacity)
    }

    @Test fun providerFailureThenRetry() = runTest {
        var fail = true
        val session = StorageSession(Reader({ if (fail) error("provider") else result(it) }), { full }, this)
        session.scan()
        runCurrent()
        assertEquals(ScanStatus.Failed(ScanFailure.READ_FAILED), session.state.value.scan)
        fail = false
        session.scan()
        runCurrent()
        assertEquals(result(full), session.state.value.inventory)
    }

    @Test fun refreshKeepsLastKnownReadingOnFailure() = runTest {
        var fail = false
        val session = StorageSession(Reader({ result(it) }, { if (fail) error("capacity") else Capacity(100, 25) }), { full }, this)
        session.refresh()
        runCurrent()
        fail = true
        session.refresh()
        runCurrent()
        assertEquals(Capacity(100, 25), session.state.value.capacity)
        assertTrue(session.state.value.capacityError)
    }
}
