package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.LocalFile
import com.example.data.database.MarketplaceExtension
import com.example.data.database.SshConnection
import com.example.ui.code.CodeHighlighter
import com.example.ui.viewmodel.CodeEditorViewModel
import com.example.ui.viewmodel.SidebarTab
import com.example.ui.viewmodel.LintProblem
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CodeEditorScreen(
    viewModel: CodeEditorViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()
    val editorText by viewModel.editorText.collectAsState()
    val localFiles by viewModel.localFilesState.collectAsState(initial = emptyList())
    val sshConnections by viewModel.sshConnectionsState.collectAsState(initial = emptyList())
    val extensions by viewModel.extensionsState.collectAsState(initial = emptyList())
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val aiDialogue by viewModel.aiDialogue.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val lintIssues by viewModel.lintIssues.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val webViewEnabled by viewModel.webViewEnabled.collectAsState()

    val terminalInputText by viewModel.terminalInput.collectAsState()
    val aiInputText by viewModel.aiInputText.collectAsState()

    val isSidebarCollapsed by viewModel.isSidebarCollapsed.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var isShortcutsDialogOpen by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isTerminalMaximized by remember { mutableStateOf(false) }

    // Dialog fields
    var isNewFileDialogOpen by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var isFolderCreation by remember { mutableStateOf(false) }
    var selectedParentPath by remember { mutableStateOf("/project") }
    var isParentFolderDropdownExpanded by remember { mutableStateOf(false) }

    var isNewSshDialogOpen by remember { mutableStateOf(false) }
    var newSshName by remember { mutableStateOf("") }
    var newSshHost by remember { mutableStateOf("") }
    var newSshUser by remember { mutableStateOf("") }
    var newSshPort by remember { mutableStateOf("22") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // VS Code style bottom status bar
            StatusBar(
                activeFile = activeFile,
                lintCount = lintIssues.size,
                isSyncing = isSyncing,
                syncStatus = syncMessage
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(if (isDarkMode) VsBackground else Color(0xFFF5F5F5))
        ) {
            // 1. VS Code Side Activity Bar (Navigation Rail)
            ActivityBar(
                activeTab = activeTab,
                isCollapsed = isSidebarCollapsed,
                onTabSelect = { tab ->
                    if (activeTab == tab && !isSidebarCollapsed) {
                        viewModel.setSidebarCollapsed(true)
                    } else {
                        viewModel.setSidebarCollapsed(false)
                        viewModel.selectSidebarTab(tab)
                    }
                },
                viewModel = viewModel,
                isDarkMode = isDarkMode,
                onSettingsClick = { isShortcutsDialogOpen = true }
            )

            // 2. Folding Sidebar Content Panel
            AnimatedVisibility(
                visible = !isSidebarCollapsed,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .background(if (isDarkMode) VsSidebarBackground else Color(0xFFF0F0F0))
                        .border(1.dp, if (isDarkMode) Color(0xFF252526) else Color(0xFFDDDDDD))
                ) {
                    when (activeTab) {
                        SidebarTab.Explorer -> {
                            ExplorerPanel(
                                localFiles = localFiles,
                                activeFile = activeFile,
                                onFileSelect = { viewModel.selectFile(it) },
                                onFileDelete = { viewModel.deleteFile(it) },
                                onCreateFileClick = {
                                    isFolderCreation = false
                                    newFileName = ""
                                    val activeFolderPath = if (activeFile != null) {
                                        val path = activeFile!!.path
                                        if (activeFile!!.isDirectory) {
                                            path
                                        } else {
                                            val lastSlash = path.lastIndexOf('/')
                                            if (lastSlash <= 0) "/project" else path.substring(0, lastSlash)
                                        }
                                    } else {
                                        "/project"
                                    }
                                    selectedParentPath = activeFolderPath
                                    isNewFileDialogOpen = true
                                },
                                onCreateFolderClick = {
                                    isFolderCreation = true
                                    newFileName = ""
                                    val activeFolderPath = if (activeFile != null) {
                                        val path = activeFile!!.path
                                        if (activeFile!!.isDirectory) {
                                            path
                                        } else {
                                            val lastSlash = path.lastIndexOf('/')
                                            if (lastSlash <= 0) "/project" else path.substring(0, lastSlash)
                                        }
                                    } else {
                                        "/project"
                                    }
                                    selectedParentPath = activeFolderPath
                                    isNewFileDialogOpen = true
                                }
                            )
                        }
                        SidebarTab.AI -> {
                            AiAssistantPanel(
                                dialogue = aiDialogue,
                                isThinking = isAiThinking,
                                textInput = aiInputText,
                                onTextChange = { viewModel.aiInputText.value = it },
                                onSendClick = { viewModel.askGeminiPairProgrammer() },
                                lintWarningCount = lintIssues.size,
                                onAutoFixClick = { viewModel.executeAutoLintFix() }
                            )
                        }
                        SidebarTab.SSH -> {
                            SshManagerPanel(
                                connections = sshConnections,
                                onNewConnectionClick = {
                                    newSshName = ""
                                    newSshHost = ""
                                    newSshUser = ""
                                    newSshPort = "22"
                                    isNewSshDialogOpen = true
                                },
                                onDeleteClick = { viewModel.deleteSsh(it) },
                                onConnectClick = { conn ->
                                    viewModel.terminalInput.value = "ssh connect ${conn.host}"
                                    viewModel.executeTerminalCommand()
                                }
                            )
                        }
                        SidebarTab.Extensions -> {
                            ExtensionsPanel(
                                list = extensions,
                                onToggleInstall = { viewModel.toggleExtensionState(it) }
                            )
                        }
                        SidebarTab.Sync -> {
                            CloudSyncPanel(
                                isSyncing = isSyncing,
                                syncStatus = syncMessage,
                                onSyncTrigger = { viewModel.triggerCloudSync() }
                            )
                        }
                        SidebarTab.Search -> {
                            SearchAndReplacePanel(viewModel = viewModel)
                        }
                        SidebarTab.Git -> {
                            GitSourceControlPanel(viewModel = viewModel)
                        }
                    }
                }
            }

            // 3. Middle Area: Editor Pane, Web View Preview and Terminal combined
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top Tab Ribbon / Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(VsHeaderBg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active File Tabs Visual (Single or multiple file previews)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeFile?.let { current ->
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(VsBackground)
                                    .border(1.dp, Color(0xFF191919))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (current.name.endsWith(".html")) Icons.Filled.Html else Icons.Filled.Code,
                                    contentDescription = null,
                                    tint = VsAccentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = current.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } ?: Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Welcome to DevCode Editor Workspace",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Previews and compiler action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (activeFile?.name?.endsWith(".html") == true) {
                            IconButton(
                                onClick = { viewModel.toggleWebViewMode(!webViewEnabled) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (webViewEnabled) Icons.Filled.Code else Icons.Filled.Preview,
                                    contentDescription = "Toggle HTML Preview",
                                    tint = if (webViewEnabled) VsSecondaryAccent else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (lintIssues.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.executeAutoLintFix() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("ai_quick_fix_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Build,
                                    contentDescription = "AI Quick Fix",
                                    tint = Color(0xFFF1FA8C),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Main Central Workspace Row (Editor vs Live Html WebView Split Panel)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Left Split Screen - Code Text Editor Panel
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (activeFile != null) {
                            CodeWorkspaceTextEditor(
                                codeText = editorText,
                                language = activeFile?.language ?: "txt",
                                onValueChange = { viewModel.updateCodeText(it) },
                                viewModel = viewModel
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(VsBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Code,
                                        contentDescription = null,
                                        tint = VsSecondaryAccent,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Open a file from the explorer list to begin coding.",
                                        color = Color.LightGray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Right Split Screen - Live HTML layout preview pane
                    if (webViewEnabled && activeFile?.name?.endsWith(".html") == true) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(1.dp, Color(0xFF2D2D2D))
                                .background(Color.White)
                        ) {
                            HtmlPreview(htmlCode = editorText)
                        }
                    }
                }

                // 4. Bottom Collapsible/Maximizable Interactive Terminal Pane
                TerminalConsole(
                    logs = terminalLogs,
                    inputText = terminalInputText,
                    onInputChange = { viewModel.terminalInput.value = it },
                    onSendClick = { viewModel.executeTerminalCommand() },
                    isMaximized = isTerminalMaximized,
                    onToggleMaximize = { isTerminalMaximized = !isTerminalMaximized }
                )
            }
        }
    }

    // New File/Folder Appending Dialog screen
    if (isNewFileDialogOpen) {
        AlertDialog(
            onDismissRequest = { isNewFileDialogOpen = false },
            title = {
                Text(
                    text = if (isFolderCreation) "Create Virtual Folder" else "Create File Assembly",
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Specify path destination within virtual project hierarchy",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Folder Dropdown selector
                    Text(
                        text = "Parent Folder Destination",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable { isParentFolderDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = VsSecondaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedParentPath, color = Color.White, fontSize = 13.sp)
                            }
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                              )
                        }
                        
                        DropdownMenu(
                            expanded = isParentFolderDropdownExpanded,
                            onDismissRequest = { isParentFolderDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .background(VsSidebarBackground)
                                .border(1.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                        ) {
                            val directories = localFiles.filter { it.isDirectory }.map { it.path }.distinct().sorted()
                            val foldersList = if (directories.isEmpty()) listOf("/project") else directories
                            
                            // Adjust selectedParentPath validity if it was deleted
                            if (selectedParentPath !in foldersList) {
                                selectedParentPath = foldersList.first()
                            }

                            foldersList.forEach { folderPath ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Folder,
                                                contentDescription = null,
                                                tint = VsSecondaryAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(folderPath, color = Color.White, fontSize = 13.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedParentPath = folderPath
                                        isParentFolderDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (isFolderCreation) "Folder Name" else "File Name",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isFolderCreation) "e.g. assets" else "e.g. index.html") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = VsAccentColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputName = newFileName.trim()
                        if (inputName.isNotEmpty()) {
                            val cleanPath = if (inputName.startsWith("/")) {
                                inputName
                            } else {
                                val parent = selectedParentPath.removeSuffix("/")
                                "$parent/$inputName"
                            }
                            viewModel.createNewFile(inputName, cleanPath, isFolderCreation)
                            isNewFileDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VsAccentColor)
                ) {
                    Text("Assemble", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewFileDialogOpen = false }) {
                    Text("Dismiss", color = Color.White)
                }
            },
            containerColor = VsSidebarBackground
        )
    }

    // New SSH Node form connection configuration dialog
    if (isNewSshDialogOpen) {
        AlertDialog(
            onDismissRequest = { isNewSshDialogOpen = false },
            title = { Text("Create remote SSH configuration Host node", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newSshName,
                        onValueChange = { newSshName = it },
                        label = { Text("Visual Target Name") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = newSshHost,
                        onValueChange = { newSshHost = it },
                        label = { Text("SSH Destination Address") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = newSshUser,
                        onValueChange = { newSshUser = it },
                        label = { Text("Username login credential") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = newSshPort,
                        onValueChange = { newSshPort = it },
                        label = { Text("Connection Port") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSshName.trim().isNotEmpty() && newSshHost.trim().isNotEmpty()) {
                            val portInt = newSshPort.toIntOrNull() ?: 22
                            viewModel.createSshHost(newSshName, newSshHost, newSshUser, portInt)
                            isNewSshDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VsAccentColor)
                ) {
                    Text("Verify Socket", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewSshDialogOpen = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = VsSidebarBackground
        )
    }

    if (isShortcutsDialogOpen) {
        KeyboardShortcutsConfigDialog(
            viewModel = viewModel,
            onDismiss = { isShortcutsDialogOpen = false }
        )
    }
}

@Composable
fun ActivityBar(
    activeTab: SidebarTab,
    isCollapsed: Boolean,
    onTabSelect: (SidebarTab) -> Unit,
    viewModel: CodeEditorViewModel,
    isDarkMode: Boolean,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(55.dp)
            .background(if (isDarkMode) VsDarkerGrey else Color(0xFFE5E5E5))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActivityBarIcon(
                imageVector = Icons.Filled.Folder,
                selected = activeTab == SidebarTab.Explorer && !isCollapsed,
                onClick = { onTabSelect(SidebarTab.Explorer) },
                tag = "explorer_tab"
            )
            ActivityBarIcon(
                imageVector = Icons.Filled.Search,
                selected = activeTab == SidebarTab.Search && !isCollapsed,
                onClick = { onTabSelect(SidebarTab.Search) },
                tag = "search_tab"
            )
            ActivityBarIcon(
                imageVector = Icons.Filled.AccountTree,
                selected = activeTab == SidebarTab.Git && !isCollapsed,
                onClick = { onTabSelect(SidebarTab.Git) },
                tag = "git_tab"
            )
            ActivityBarIcon(
                imageVector = Icons.Filled.AutoAwesome,
                selected = activeTab == SidebarTab.AI && !isCollapsed,
                onClick = { onTabSelect(SidebarTab.AI) },
                tag = "ai_tab"
            )
            ActivityBarIcon(
                imageVector = Icons.Filled.Terminal,
                selected = activeTab == SidebarTab.SSH && !isCollapsed,
                onClick = { onTabSelect(SidebarTab.SSH) },
                tag = "ssh_tab"
            )
            ActivityBarIcon(
                imageVector = Icons.Filled.Extension,
                selected = activeTab == SidebarTab.Extensions && !isCollapsed,
                onClick = { onTabSelect(SidebarTab.Extensions) },
                tag = "extensions_tab"
            )
            ActivityBarIcon(
                imageVector = Icons.Filled.CloudSync,
                selected = activeTab == SidebarTab.Sync && !isCollapsed,
                onClick = { onTabSelect(SidebarTab.Sync) },
                tag = "sync_tab"
            )
        }

        // Bottom section containing Theme Toggle and Settings Gear
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Toggle Theme Mode",
                tint = Color.Gray,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { viewModel.toggleDarkMode() }
                    .testTag("theme_toggle_button")
            )
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Configure keyboard shortcuts",
                tint = Color.Gray,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onSettingsClick() }
                    .testTag("settings_button")
            )
        }
    }
}

@Composable
fun ActivityBarIcon(
    imageVector: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) VsLighterGrey else Color.Transparent)
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = if (selected) Color.White else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ExplorerPanel(
    localFiles: List<LocalFile>,
    activeFile: LocalFile?,
    onFileSelect: (LocalFile) -> Unit,
    onFileDelete: (LocalFile) -> Unit,
    onCreateFileClick: () -> Unit,
    onCreateFolderClick: () -> Unit
) {
    // Persistent expansion state for folders
    var expandedPaths by remember { mutableStateOf(setOf("/project")) }

    // Helper functions for hierarchical calculation
    fun getAncestors(path: String): List<String> {
        val segments = path.split("/").filter { it.isNotEmpty() }
        val ancestors = mutableListOf<String>()
        var current = ""
        for (i in 0 until segments.size - 1) {
            current += "/" + segments[i]
            ancestors.add(current)
        }
        return ancestors
    }

    fun sortHierarchical(files: List<LocalFile>): List<LocalFile> {
        val filesByParent = files.groupBy { file ->
            val path = file.path
            val lastSlash = path.lastIndexOf('/')
            if (lastSlash <= 0) "/" else path.substring(0, lastSlash)
        }

        val result = mutableListOf<LocalFile>()
        
        fun traverse(currentParent: String) {
            val children = filesByParent[currentParent] ?: return
            val sortedChildren = children.sortedWith(
                compareBy<LocalFile> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
            for (child in sortedChildren) {
                result.add(child)
                if (child.isDirectory) {
                    traverse(child.path)
                }
            }
        }

        traverse("/")

        val caughtIds = result.map { it.id }.toSet()
        val orphans = files.filter { it.id !in caughtIds }
        if (orphans.isNotEmpty()) {
            result.addAll(
                orphans.sortedWith(
                    compareBy<LocalFile> { !it.isDirectory }
                        .thenBy { it.path.lowercase() }
                )
            )
        }

        return result
    }

    // Sort files hierarchically
    val sortedFiles = remember(localFiles) { sortHierarchical(localFiles) }

    // Filter out children of collapsed folders
    val visibleFiles = remember(sortedFiles, expandedPaths) {
        sortedFiles.filter { file ->
            val ancestors = getAncestors(file.path)
            ancestors.all { it in expandedPaths }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Explorer Panel Navigation title header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPLORER: PROJECT",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row {
                IconButton(onClick = onCreateFileClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.NoteAdd,
                        contentDescription = "Add File Assembly",
                        tint = Color.LightGray,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(onClick = onCreateFolderClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.CreateNewFolder,
                        contentDescription = "Create Virtual Folder",
                        tint = Color.LightGray,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))

        // Visual File System Listing View
        if (visibleFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No files available.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visibleFiles, key = { it.id }) { file ->
                    val depth = maxOf(0, file.path.count { it == '/' } - 1)
                    val isExpanded = file.path in expandedPaths

                    FileListItem(
                        file = file,
                        depth = depth,
                        isExpanded = isExpanded,
                        isSelected = file.id == activeFile?.id,
                        onClick = {
                            if (file.isDirectory) {
                                expandedPaths = if (isExpanded) {
                                    expandedPaths - file.path
                                } else {
                                    // Expand both parent and item
                                    val ancestors = getAncestors(file.path)
                                    expandedPaths + ancestors + file.path
                                }
                            } else {
                                onFileSelect(file)
                            }
                        },
                        onDelete = { onFileDelete(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun FileListItem(
    file: LocalFile,
    depth: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) VsActiveTab else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 5.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Indentation padding
            Spacer(modifier = Modifier.width((depth * 10).dp))

            // Expand/collapse indicator for folders
            if (file.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            } else {
                // Spacer matching width of the indicator to align with folder items
                Spacer(modifier = Modifier.width(18.dp))
            }

            // File/Folder main Icon
            Icon(
                imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.DocID,
                contentDescription = null,
                tint = if (file.isDirectory) VsSecondaryAccent else VsAccentColor,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = file.name,
                color = if (isSelected) Color.White else Color.LightGray,
                fontSize = 13.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Trash icon to delete entry
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Trash entry",
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

val Icons.Filled.DocID: ImageVector get() = Icons.Filled.Description

@Composable
fun AiAssistantPanel(
    dialogue: List<Pair<String, String>>,
    isThinking: Boolean,
    textInput: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    lintWarningCount: Int,
    onAutoFixClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "GEMINI PAIR PROGRAMMER",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Warnings / Debug alert bar
        if (lintWarningCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2B2610))
                    .border(1.dp, Color(0xFFE2C226), RoundedCornerShape(8.dp))
                    .clickable { onAutoFixClick() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF1FA8C),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Syntax warnings detected ($lintWarningCount)",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Auto-Resolve brackets & semicolon errors",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = null,
                    tint = Color(0xFF50FA7B),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Dialog Scroll Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dialogue.forEach { chat ->
                val isUser = chat.first == "Developer"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = chat.first,
                        fontSize = 10.sp,
                        color = if (isUser) VsAccentColor else VsSecondaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUser) Color(0xFF313134) else VsActiveTab)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = chat.second,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            if (isThinking) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = VsSecondaryAccent
                    )
                    Text(
                        text = "  Gemini compilation analysis...",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Input send box layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask AI to format code...", fontSize = 12.sp, color = Color.Gray) },
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = VsDarkerGrey,
                    unfocusedContainerColor = VsDarkerGrey,
                    focusedBorderColor = VsAccentColor
                ),
                textStyle = TextStyle(fontSize = 12.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClick() })
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onSendClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = VsAccentColor),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SshManagerPanel(
    connections: List<SshConnection>,
    onNewConnectionClick: () -> Unit,
    onDeleteClick: (SshConnection) -> Unit,
    onConnectClick: (SshConnection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SSH CONFIG INTEGRATIONS",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNewConnectionClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Add, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (connections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No credential nodes listed.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(connections) { conn ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VsBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = conn.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(
                                        onClick = { onConnectClick(conn) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Link, null, tint = VsAccentColor, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteClick(conn) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, null, tint = Color.Red, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                            Text(
                                text = "${conn.username}@${conn.host}:${conn.port}",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionsPanel(
    list: List<MarketplaceExtension>,
    onToggleInstall: (MarketplaceExtension) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Text(
            text = "EXTENSIONS MARKETPLACE",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(list) { ext ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VsBackground)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (ext.iconName) {
                                "kotlin" -> Icons.Filled.SportsGolf
                                "html" -> Icons.Filled.Web
                                "gemini" -> Icons.Filled.AutoAwesome
                                "terminal" -> Icons.Filled.Terminal
                                "format" -> Icons.Filled.WrapText
                                else -> Icons.Filled.Extension
                            }
                            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = ext.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = ext.description, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(text = "Downloads: ${ext.downloads} | v${ext.version}", color = VsAccentColor, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Button(
                        onClick = { onToggleInstall(ext) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ext.isInstalled) Color.Transparent else VsAccentColor,
                            contentColor = if (ext.isInstalled) Color.Red else Color.White
                        ),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(
                            text = if (ext.isInstalled) "Disable" else "Install",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSyncPanel(
    isSyncing: Boolean,
    syncStatus: String,
    onSyncTrigger: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudSync,
            contentDescription = null,
            tint = if (isSyncing) VsSecondaryAccent else VsAccentColor,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "SECURE CLOUD SYNCHRONIZER",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Backs up active local code assemblies and host keychains globally behind secure encryption wrappers.",
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (isSyncing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = VsAccentColor
            )
        } else {
            Button(
                onClick = onSyncTrigger,
                colors = ButtonDefaults.buttonColors(containerColor = VsStatusBar),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Sync Sandboxed Nodes", color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = syncStatus,
            color = Color.Gray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CodeWorkspaceTextEditor(
    codeText: String,
    language: String,
    onValueChange: (String) -> Unit,
    viewModel: CodeEditorViewModel
) {
    var textFieldValueState by remember(codeText) {
        val highlighted = CodeHighlighter.highlightCode(codeText, language)
        mutableStateOf(
            TextFieldValue(
                annotatedString = highlighted
            )
        )
    }

    val coroutine = rememberCoroutineScope()
    val lines = codeText.lines()
    val lineScrollState = rememberScrollState()
    val editorScrollState = rememberScrollState()

    var editorFontSize by remember { mutableStateOf(13.sp) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val shortcuts by viewModel.customShortcuts.collectAsState()

    // Line scroll matches editor scroll layout
    LaunchedEffect(editorScrollState.value) {
        lineScrollState.scrollTo(editorScrollState.value)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) VsBackground else Color(0xFFF9F9F9))
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        val nextSize = (editorFontSize.value * zoom).coerceIn(10f, 30f)
                        editorFontSize = nextSize.sp
                    }
                }
            }
    ) {
        // Line Numbers List column
        Column(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight()
                .background(if (isDarkMode) VsSidebarBackground else Color(0xFFEBEBEB))
                .verticalScroll(lineScrollState)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (i in 1..lines.size) {
                Text(
                    text = "$i",
                    color = if (isDarkMode) Color.DarkGray else Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = editorFontSize,
                    lineHeight = (editorFontSize.value * 1.5f).sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = if (isDarkMode) Color(0xFF191919) else Color(0xFFDCDCDC)
        )

        // Actual editor content box with preview hardware key event listeners
        BasicTextField(
            value = textFieldValueState,
            onValueChange = { newValue ->
                textFieldValueState = newValue.copy(
                    annotatedString = CodeHighlighter.highlightCode(newValue.text, language)
                )
                if (newValue.text != codeText) {
                    onValueChange(newValue.text)
                }
            },
            textStyle = TextStyle(
                color = if (isDarkMode) Color.LightGray else Color(0xFF222222),
                fontFamily = FontFamily.Monospace,
                fontSize = editorFontSize,
                lineHeight = (editorFontSize.value * 1.5f).sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(if (isDarkMode) Color.White else Color.Black),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(editorScrollState)
                .onPreviewKeyEvent { event ->
                    handleKeyboardShortcut(event, shortcuts) { cmdId ->
                        viewModel.triggerShortcutCommand(cmdId)
                    }
                }
                .padding(12.dp)
                .testTag("code_editor_input")
        )
    }
}

fun handleKeyboardShortcut(
    event: KeyEvent,
    shortcuts: List<com.example.ui.viewmodel.CodeEditorViewModel.CustomShortcut>,
    onTrigger: (String) -> Unit
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    for (sc in shortcuts) {
        val composeKey = getComposeKeyFromString(sc.currentKey) ?: continue
        if (event.key == composeKey &&
            event.isCtrlPressed == sc.ctrlRequired &&
            event.isAltPressed == sc.altRequired &&
            event.isShiftPressed == sc.shiftRequired
        ) {
            onTrigger(sc.id)
            return true
        }
    }
    return false
}

fun getComposeKeyFromString(char: String): Key? {
    return when (char.uppercase()) {
        "A" -> Key.A
        "B" -> Key.B
        "C" -> Key.C
        "D" -> Key.D
        "E" -> Key.E
        "F" -> Key.F
        "G" -> Key.G
        "H" -> Key.H
        "I" -> Key.I
        "J" -> Key.J
        "K" -> Key.K
        "L" -> Key.L
        "M" -> Key.M
        "N" -> Key.N
        "O" -> Key.O
        "P" -> Key.P
        "Q" -> Key.Q
        "R" -> Key.R
        "S" -> Key.S
        "T" -> Key.T
        "U" -> Key.U
        "V" -> Key.V
        "W" -> Key.W
        "X" -> Key.X
        "Y" -> Key.Y
        "Z" -> Key.Z
        "0" -> Key.Zero
        "1" -> Key.One
        "2" -> Key.Two
        "3" -> Key.Three
        "4" -> Key.Four
        "5" -> Key.Five
        "6" -> Key.Six
        "7" -> Key.Seven
        "8" -> Key.Eight
        "9" -> Key.Nine
        else -> null
    }
}

@Composable
fun TerminalConsole(
    logs: List<String>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isMaximized: Boolean,
    onToggleMaximize: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isMaximized) 420.dp else 190.dp)
            .background(VsDarkerGrey)
            .border(1.dp, Color(0xFF252526))
    ) {
        // Terminal Panel Title header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VsSidebarBackground)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = VsSecondaryAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "COMMAND BUFFER TERMINAL",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            IconButton(onClick = onToggleMaximize, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (isMaximized) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                    contentDescription = "Expand terminal view",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Terminal Log Area displaying commands output results
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            items(logs) { log ->
                Text(
                    text = log,
                    color = getTerminalLogColor(log),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Divider(color = Color(0xFF2D2D2D))

        // Input Line row (e.g. prompt cursor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "dev@devcode-android:~$ ",
                color = VsSecondaryAccent,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSendClick() })
            )
        }
    }
}

private fun getTerminalLogColor(log: String): Color {
    return when {
        log.startsWith("dev@") -> VsAccentColor
        log.startsWith("Error:") -> Color.Red
        log.startsWith("> mkdir") || log.startsWith("> ssh") -> Color(0xFF50FA7B) // Success green
        log.startsWith("  - Profile") -> VsSecondaryAccent
        else -> Color.LightGray
    }
}

@Composable
fun StatusBar(
    activeFile: LocalFile?,
    lintCount: Int,
    isSyncing: Boolean,
    syncStatus: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(VsStatusBar)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AltRoute, null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("main", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(14.dp))
            
            // Warnings Inline Count badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$lintCount issues detected", color = Color.White, fontSize = 11.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(syncStatus, color = Color.White, fontSize = 10.sp, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(16.dp))
            Text(activeFile?.language?.uppercase() ?: "TXT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(10.dp))
            Text("UTF-8", color = Color.White, fontSize = 11.sp)
        }
    }
}
