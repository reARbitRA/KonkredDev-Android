package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiPairProgrammer
import com.example.data.database.AppDatabase
import com.example.data.database.LocalFile
import com.example.data.database.MarketplaceExtension
import com.example.data.database.SshConnection
import com.example.data.repository.CodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SidebarTab {
    object Explorer : SidebarTab
    object AI : SidebarTab
    object SSH : SidebarTab
    object Extensions : SidebarTab
    object Sync : SidebarTab
}

data class LintProblem(
    val line: Int,
    val message: String,
    val severity: String, // "Error", "Warning"
    val codeFragment: String = ""
)

class CodeEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = CodeRepository(db.fileDao(), db.sshDao(), db.extensionDao())
    private val pairProgrammer = GeminiPairProgrammer()

    // Database flows
    val localFilesState = repository.allFiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    val sshConnectionsState = repository.allSshConnections.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    val extensionsState = repository.allExtensions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Editor UI State variables
    private val _activeFile = MutableStateFlow<LocalFile?>(null)
    val activeFile: StateFlow<LocalFile?> = _activeFile.asStateFlow()

    private val _editorText = MutableStateFlow("")
    val editorText: StateFlow<String> = _editorText.asStateFlow()

    private val _activeTab = MutableStateFlow<SidebarTab>(SidebarTab.Explorer)
    val activeTab: StateFlow<SidebarTab> = _activeTab.asStateFlow()

    // Terminal State variables
    private val _terminalLogs = MutableStateFlow<List<String>>(
        listOf(
            "DevCode OS Built-in Terminal v5.0.0 (Windows-Compatible Android Daemon)",
            "System online. Offline database connected.",
            "Type 'help' to examine available actions. Use 'preview' to launch live HTML viewer."
        )
    )
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    // AI Chat dialogue history
    private val _aiDialogue = MutableStateFlow<List<Pair<String, String>>>(
        listOf(
            "Gemini Pair" to "Hello! I am your visual AI Coding Partner. Open any file in the editor, ask me questions, tap 'Fix My Code' to auto-resolve warnings, or optimize architecture!"
        )
    )
    val aiDialogue: StateFlow<List<Pair<String, String>>> = _aiDialogue.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // Real-Time Compiler/Linter warnings
    private val _lintIssues = MutableStateFlow<List<LintProblem>>(emptyList())
    val lintIssues: StateFlow<List<LintProblem>> = _lintIssues.asStateFlow()

    // Cloud Synchronizer values
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("Last synced: Just now (Offline state saved)")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    // HTML Rendering Preview layout toggle
    private val _webViewEnabled = MutableStateFlow(false)
    val webViewEnabled: StateFlow<Boolean> = _webViewEnabled.asStateFlow()

    // Terminal custom prompt buffer
    val terminalInput = MutableStateFlow("")

    val aiInputText = MutableStateFlow("")

    init {
        // Trigger automated checkout
        viewModelScope.launch {
            repository.allFiles.collect { files ->
                if (_activeFile.value == null) {
                    val readMe = files.find { it.path == "/project/readme.md" }
                        ?: files.find { !it.isDirectory }
                    readMe?.let { selectFile(it) }
                }
            }
        }
    }

    // Tab Header selection
    fun selectSidebarTab(tab: SidebarTab) {
        _activeTab.value = tab
    }

    // Toggle Preview state
    fun toggleWebViewMode(enabled: Boolean) {
        _webViewEnabled.value = enabled
        // Print action into editor terminal
        val state = if (enabled) "ENABLED" else "DISABLED"
        pushTerminalLog("> preview toggle: web display mode $state")
    }

    // Select Active Working File
    fun selectFile(file: LocalFile) {
        if (file.isDirectory) return
        viewModelScope.launch {
            // Save active file progress first
            _activeFile.value?.let { current ->
                repository.updateFile(current.copy(content = _editorText.value, lastModified = System.currentTimeMillis()))
            }
            _activeFile.value = file
            _editorText.value = file.content
            triggerLintAnalyzer(file.name, file.content)
        }
    }

    // Update active editing content buffer and scan lint warnings
    fun updateCodeText(newText: String) {
        _editorText.value = newText
        _activeFile.value?.let { current ->
            viewModelScope.launch {
                repository.updateFile(current.copy(content = newText))
            }
            triggerLintAnalyzer(current.name, newText)
        }
    }

    // Local compiler linter analyzer (Offline)
    private fun triggerLintAnalyzer(fileName: String, content: String) {
        val problems = mutableListOf<LintProblem>()
        val lines = content.lines()

        if (fileName.endsWith(".html")) {
            // HTML linter tags check
            val divOpenCount = content.split("<div").size - 1
            val divCloseCount = content.split("</div>").size - 1
            if (divOpenCount != divCloseCount) {
                problems.add(
                    LintProblem(
                        line = 1,
                        message = "Unbalanced HTML structural blocks: detected $divOpenCount <div> openings but $divCloseCount </div> closures.",
                        severity = "Warning",
                        codeFragment = "HTML Div Alignment"
                    )
                )
            }
            
            // Check script references
            if (content.contains("src=") && !content.contains("src=\"script.js\"")) {
                problems.add(
                    LintProblem(
                        line = content.lines().indexOfFirst { it.contains("src=") } + 1,
                        message = "External assets parsed should align with virtual folder files. Make sure path matches local files.",
                        severity = "Warning"
                    )
                )
            }
        } else if (fileName.endsWith(".js") || fileName.endsWith(".ts")) {
            // Check matching curly brackets
            val openCurly = content.split("{").size - 1
            val closeCurly = content.split("}").size - 1
            if (openCurly != closeCurly) {
                problems.add(
                    LintProblem(
                        line = 1,
                        message = "Syntactic scope anomaly: mismatched brackets. Count { ($openCurly) differs } ($closeCurly).",
                        severity = "Error",
                        codeFragment = "{}"
                    )
                )
            }

            // JavaScript missing semicolon guidelines (Standard advice warnings)
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.endsWith("{") && !trimmed.endsWith("}") && !trimmed.endsWith(";") && !trimmed.startsWith("//") && !trimmed.endsWith(",")) {
                    if (trimmed.contains("const ") || trimmed.contains("let ") || trimmed.contains("return") || trimmed.contains("console.log")) {
                        problems.add(
                            LintProblem(
                                line = index + 1,
                                message = "Missing terminal semicolon spacing delimiter.",
                                severity = "Warning",
                                codeFragment = trimmed
                            )
                        )
                    }
                }
            }
        } else if (fileName.endsWith(".kt")) {
            // Checking Kotlin patterns
            if (!content.contains("fun ") && !content.contains("class ")) {
                problems.add(
                    LintProblem(
                        line = 1,
                        message = "An empty compilation class detected.",
                        severity = "Warning"
                    )
                )
            }
        }

        _lintIssues.value = problems
    }

    // New File creation (Offline)
    fun createNewFile(name: String, path: String, isFolder: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val extension = name.substringAfterLast('.', "")
            val newFile = LocalFile(
                name = name,
                path = path,
                content = if (isFolder) "" else "/* Newly created file: $name */",
                isDirectory = isFolder,
                language = extension
            )
            repository.insertFile(newFile)
            withContext(Dispatchers.Main) {
                pushTerminalLog("> mkdir file: created successful asset at $path")
                if (!isFolder) {
                    selectFile(newFile)
                }
            }
        }
    }

    // Secure database cloud synchronization mock
    fun triggerCloudSync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Scanning local database nodes..."
            kotlinx.coroutines.delay(1000)
            _syncMessage.value = "Tunneling secure handshake to cloud nodes..."
            kotlinx.coroutines.delay(1200)
            _syncMessage.value = "Securing files & connection keychains... 89% cached"
            kotlinx.coroutines.delay(800)
            _isSyncing.value = false
            _syncMessage.value = "Last synced: Successfully updated. DevCode Workspace synced safely."
            pushTerminalLog("> git daemon: Sync complete with remote mirror repository.")
        }
    }

    // Create a physical remote host entry
    fun createSshHost(name: String, host: String, username: String, port: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSsh(
                SshConnection(
                    name = name,
                    host = host,
                    username = username,
                    port = port,
                    rsaKey = "MOCK_KEY_AUTO_GENERATED_${host.uppercase()}_2026"
                )
            )
            withContext(Dispatchers.Main) {
                pushTerminalLog("> ssh configuration appended for profile: $name")
            }
        }
    }

    fun deleteSsh(conn: SshConnection) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSsh(conn)
            withContext(Dispatchers.Main) {
                pushTerminalLog("> ssh configuration removed for profile: ${conn.name}")
            }
        }
    }

    // Install marketplace module
    fun toggleExtensionState(extension: MarketplaceExtension) {
        viewModelScope.launch {
            val newState = !extension.isInstalled
            repository.updateExtensionState(extension.id, newState)
            val action = if (newState) "Activated and compiled" else "Uninstalled"
            pushTerminalLog("> module registry: $action extension: '${extension.name}'")
        }
    }

    // Gemini API AI Code pair assistant call
    fun askGeminiPairProgrammer() {
        val query = aiInputText.value
        if (query.trim().isEmpty()) return

        val currentDialogue = _aiDialogue.value.toMutableList()
        currentDialogue.add("Developer" to query)
        _aiDialogue.value = currentDialogue
        aiInputText.value = ""

        _isAiThinking.value = true

        val activeFileName = _activeFile.value?.name ?: "No file open"
        val activeFileCode = _editorText.value

        val sysPrompt = """
            You are a VS Code style elite pair-programmer assistant inside the premium Android DevCode IDE applet.
            The user is coding in $activeFileName.
            Here is the active file content:
            $activeFileCode
            
            Keep your answers brief, highly technical, and formatted neatly in Markdown.
            Provide inline corrections directly in the answers. Avoid long philosophical summaries.
        """.trimIndent()

        viewModelScope.launch {
            val result = pairProgrammer.askGemini(query, sysPrompt)
            val updatedDiag = _aiDialogue.value.toMutableList()
            updatedDiag.add("Gemini AI" to result)
            _aiDialogue.value = updatedDiag
            _isAiThinking.value = false
        }
    }

    // Auto-fix optimization triggered by AI button or debugger tap
    fun executeAutoLintFix() {
        val issues = _lintIssues.value
        if (issues.isEmpty()) return

        val activeF = _activeFile.value ?: return
        _isAiThinking.value = true

        val repairPrompt = """
            The following document has compiling issues:
            File: ${activeF.name}
            Code:
            ${_editorText.value}
            
            Detected syntax anomalies:
            ${issues.joinToString("\n") { "Line ${it.line}: ${it.message}" }}
            
            Fix all anomalies. Output only the complete repaired code block, without explanation, introductory sentences, or backticks! Just output the code!
        """.trimIndent()

        viewModelScope.launch {
            val correctedCode = pairProgrammer.askGemini(repairPrompt)
            if (correctedCode.isNotEmpty() && !correctedCode.startsWith("Error") && !correctedCode.startsWith("AI Pairing")) {
                updateCodeText(correctedCode.trim())
                pushTerminalLog("> linter optimization: AI auto-resolved syntax warnings for compiler.")
                val updatedDiag = _aiDialogue.value.toMutableList()
                updatedDiag.add("Gemini AI" to "I scanned the warnings in `${activeF.name}` and applied auto-formatting and tag closures. Code buffer updated!")
                _aiDialogue.value = updatedDiag
            } else {
                // Mock recovery fixes offline fallback
                applyLocalOfflineHotfix(activeF)
            }
            _isAiThinking.value = false
        }
    }

    // Offline physical corrections in case of no internet
    private fun applyLocalOfflineHotfix(activeF: LocalFile) {
        val currentCode = _editorText.value
        var fixed = currentCode
        if (activeF.name.endsWith(".html")) {
            // Append missing closing tag if mismatched divs
            val divOpenCount = currentCode.split("<div").size - 1
            val divCloseCount = currentCode.split("</div>").size - 1
            if (divOpenCount > divCloseCount) {
                fixed = currentCode + "\n<!-- Mock Offline Compiler closure -->\n</div>".repeat(divOpenCount - divCloseCount)
            }
        } else if (activeF.name.endsWith(".js") || activeF.name.endsWith(".ts")) {
            // Append semicolons
            val lines = currentCode.lines().map { line ->
                val tr = line.trim()
                if (tr.isNotEmpty() && !tr.endsWith(";") && !tr.endsWith("{") && !tr.endsWith("}") && !tr.startsWith("//")) {
                    line + ";"
                } else {
                    line
                }
            }
            fixed = lines.joinToString("\n")
        }

        updateCodeText(fixed)
        pushTerminalLog("> local compiler: Offline local syntactic formatter applied successfully.")
    }

    // Interactive Terminal executor simulation (Highly customizable!)
    fun executeTerminalCommand() {
        val commandLine = terminalInput.value.trim()
        if (commandLine.isEmpty()) return

        val history = _terminalLogs.value.toMutableList()
        history.add("ari_eshghi@devcode-android:~$ $commandLine")

        val parts = commandLine.split(" ")
        val command = parts[0].lowercase()

        when (command) {
            "help" -> {
                history.add("Available commands inside DevCode CLI:")
                history.add("  ls                      List all project resource files in workspace")
                history.add("  cat <file_path>         Render raw file buffer details")
                history.add("  preview                 Toggle splitting side HTML preview panel on/off")
                history.add("  ssh list                Load connected virtual SSH connections")
                history.add("  ssh connect <host>      Tunnel interactive mock connection")
                history.add("  git status              Examine local project Git staging nodes")
                history.add("  lint                    Analyze current documents for bracket defects")
                history.add("  gemini <query>          Query pairprogrammer AI directly")
                history.add("  clear                   Flush the active scrollable buffer logs")
            }
            "ls" -> {
                history.add("Listing directory: /project")
                val files = localFilesState.value
                files.forEach {
                    val icon = if (it.isDirectory) "📁" else "📄"
                    history.add("  $icon ${it.name}   | path: ${it.path}")
                }
            }
            "cat" -> {
                if (parts.size < 2) {
                    history.add("Error: Please specify target file path. e.g. 'cat index.html'")
                } else {
                    val queryName = parts[1]
                    val file = localFilesState.value.find { it.name.equals(queryName, ignoreCase = true) }
                    if (file != null) {
                        history.add("--- File Content: ${file.path} ---")
                        history.addAll(file.content.lines())
                    } else {
                        history.add("Error: File '$queryName' not located in active registry.")
                    }
                }
            }
            "preview" -> {
                toggleWebViewMode(!_webViewEnabled.value)
                history.add("Webpage layout render toggled to: " + if (_webViewEnabled.value) "Active" else "Hidden")
            }
            "ssh" -> {
                if (parts.size < 2) {
                    history.add("ssh options: 'ssh list', 'ssh connect <host>'")
                } else {
                    val sub = parts[1].lowercase()
                    if (sub == "list") {
                        history.add("Configured Host Credentials:")
                        sshConnectionsState.value.forEach {
                            history.add("  - Profile: ${it.name} | ${it.username}@${it.host}:${it.port}")
                        }
                    } else if (sub == "connect") {
                        if (parts.size < 3) {
                            history.add("Provide connection target host address.")
                        } else {
                            val host = parts[2]
                            history.add("Initiating handshakes to $host...")
                            history.add("Connecting tunnels over terminal socket... Success.")
                            history.add("Ssh session established with 256-bit AES encryption.")
                        }
                    }
                }
            }
            "git" -> {
                if (parts.size > 1 && parts[1].lowercase() == "status") {
                    history.add("On branch main")
                    history.add("Your branch is up to date with 'origin/main'.")
                    history.add("Changes not staged for commit:")
                    history.add("  (use 'git add <file>...' to update workspace index)")
                    val modifiedFiles = _activeFile.value?.let { "modified:   " + it.path } ?: "No files modified currently."
                    history.add("  $modifiedFiles")
                } else {
                    history.add("git command usage: 'git status'")
                }
            }
            "lint" -> {
                val currentFile = _activeFile.value
                if (currentFile != null) {
                    triggerLintAnalyzer(currentFile.name, _editorText.value)
                    val count = _lintIssues.value.size
                    history.add("Linter: Analysis completed for ${currentFile.name}. Located $count potential complications.")
                } else {
                    history.add("No active working file to run analysis against.")
                }
            }
            "gemini" -> {
                if (parts.size < 2) {
                    history.add("Usage: gemini <query>")
                } else {
                    val query = commandLine.removePrefix("gemini").trim()
                    history.add("Consulting Pair Programmer AI on query: '$query'")
                    viewModelScope.launch {
                        val aiResponse = pairProgrammer.askGemini(query)
                        withContext(Dispatchers.Main) {
                            _terminalLogs.value = _terminalLogs.value.toMutableList().apply {
                                add("Gemini AI Feedback:")
                                addAll(aiResponse.lines())
                            }
                        }
                    }
                }
            }
            "clear" -> {
                _terminalLogs.value = emptyList()
                terminalInput.value = ""
                return
            }
            else -> {
                history.add("command not found: '$command'. Type 'help' to review guidelines.")
            }
        }

        _terminalLogs.value = history
        terminalInput.value = ""
    }

    private fun pushTerminalLog(log: String) {
        _terminalLogs.value = _terminalLogs.value.toMutableList() + log
    }

    fun deleteFile(file: LocalFile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFile(file)
            if (file.isDirectory) {
                repository.deleteDirectoryAndChildren(file.path)
            }
            withContext(Dispatchers.Main) {
                if (_activeFile.value?.id == file.id) {
                    _activeFile.value = null
                    _editorText.value = ""
                }
                pushTerminalLog("> rm file: successfully deleted file at ${file.path}")
            }
        }
    }
}
