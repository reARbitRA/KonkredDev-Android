package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.ui.viewmodel.CodeEditorViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Journey-level tests for [CodeEditorViewModel] executed on the JVM via Robolectric.
 * Covers: J1 (seed + auto-open), J2 (edit persistence), J3 (create-file persistence),
 * J6 (global replace), J7 (simulated terminal).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CodeEditorViewModelTest {

    private lateinit var vm: CodeEditorViewModel
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        // Reset the Room singleton so each test starts from a fresh database.
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)

        val app = ApplicationProvider.getApplicationContext<Application>()
        app.deleteDatabase("devcode_editor_db")

        vm = CodeEditorViewModel(app)
        db = AppDatabase.getDatabase(app, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined))
    }

    private fun idleMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Polls a condition while idling the main looper and letting IO threads run. */
    private fun waitFor(timeoutMs: Long = 20_000, cond: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            idleMain()
            if (cond()) return true
            Thread.sleep(25)
        }
        idleMain()
        return cond()
    }

    @Test
    fun j1_seedsSampleProjectAndAutoOpensReadme() {
        assertTrue("sample project files were not seeded", waitFor { vm.localFilesState.value.isNotEmpty() })
        val paths = vm.localFilesState.value.map { it.path }
        assertTrue(paths.contains("/project/readme.md"))
        assertTrue(paths.contains("/project/index.html"))
        assertTrue("readme.md was not auto-opened", waitFor { vm.activeFile.value?.path == "/project/readme.md" })
    }

    @Test
    fun j2_editingActiveFilePersistsToDatabase() {
        assertTrue(waitFor { vm.activeFile.value != null })
        val path = vm.activeFile.value!!.path
        vm.updateCodeText("hello persistence")
        val persisted = waitFor {
            runBlocking { db.fileDao().getFileByPath(path)?.content == "hello persistence" }
        }
        assertTrue("edit to '$path' was not persisted to Room", persisted)
    }

    @Test
    fun j3_newlyCreatedFileHasRealIdAndPersistsEdits() {
        assertTrue(waitFor { vm.localFilesState.value.isNotEmpty() })
        vm.createNewFile("notes.txt", "/project/notes.txt", false)
        assertTrue("new file was not auto-selected", waitFor { vm.activeFile.value?.path == "/project/notes.txt" })
        assertNotEquals("new file still carries the unsaved id=0 (F-DATA-001 regression)", 0L, vm.activeFile.value!!.id)
        vm.updateCodeText("typed content")
        val persisted = waitFor {
            runBlocking { db.fileDao().getFileByPath("/project/notes.txt")?.content == "typed content" }
        }
        assertTrue("edits to the newly created file were not persisted (F-DATA-001)", persisted)
    }

    @Test
    fun j6_globalSearchReplacePersistsAcrossFiles() {
        assertTrue(waitFor { vm.activeFile.value != null })
        val path = vm.activeFile.value!!.path
        vm.updateCodeText("alpha beta alpha")
        assertTrue(waitFor { runBlocking { db.fileDao().getFileByPath(path)?.content == "alpha beta alpha" } })

        vm.searchQueryText.value = "alpha"
        vm.replaceQueryText.value = "gamma"
        vm.replaceAllOccurrences()

        val replaced = waitFor {
            runBlocking { db.fileDao().getFileByPath(path)?.content == "gamma beta gamma" }
        }
        assertTrue("replace-all did not persist", replaced)
        assertEquals("", vm.searchQueryText.value)
    }

    @Test
    fun j7_terminalHelpAndLsAreSimulatedLocally() {
        assertTrue(waitFor { vm.localFilesState.value.isNotEmpty() })

        vm.terminalInput.value = "help"
        vm.executeTerminalCommand()
        idleMain()
        assertTrue(vm.terminalLogs.value.any { it.contains("Available commands") })

        vm.terminalInput.value = "ls"
        vm.executeTerminalCommand()
        idleMain()
        assertTrue(vm.terminalLogs.value.any { it.contains("index.html") })
        assertEquals("", vm.terminalInput.value)
    }
}
