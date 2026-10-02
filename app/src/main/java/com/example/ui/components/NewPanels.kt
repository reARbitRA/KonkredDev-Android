package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.CodeEditorViewModel
import com.example.ui.theme.*

@Composable
fun SearchAndReplacePanel(viewModel: CodeEditorViewModel) {
    val searchQuery by viewModel.searchQueryText.collectAsState()
    val replaceQuery by viewModel.replaceQueryText.collectAsState()
    val isRegex by viewModel.isRegexSearch.collectAsState()
    val isCase by viewModel.isCaseSensitiveSearch.collectAsState()
    val searchResults by viewModel.globalSearchResults.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val textColor = if (isDarkMode) Color.LightGray else Color(0xFF232323)
    val inputBg = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SEARCH & REPLACE",
            style = MaterialTheme.typography.titleSmall,
            color = if (isDarkMode) VsAccentColor else Color(0xFF007ACC),
            fontWeight = FontWeight.Bold
        )

        // Search Input Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQueryText.value = it },
            label = { Text("Search patterns", color = Color.Gray, fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("search_input_field"),
            textStyle = TextStyle(color = textColor, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = VsAccentColor,
                unfocusedBorderColor = Color.Gray
            ),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQueryText.value = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        )

        // Options Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isRegex,
                    onCheckedChange = { viewModel.isRegexSearch.value = it },
                    modifier = Modifier.testTag("regex_checkbox_toggle"),
                    colors = CheckboxDefaults.colors(checkedColor = VsAccentColor)
                )
                Text("Regex", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isCase,
                    onCheckedChange = { viewModel.isCaseSensitiveSearch.value = it },
                    modifier = Modifier.testTag("case_checkbox_toggle"),
                    colors = CheckboxDefaults.colors(checkedColor = VsAccentColor)
                )
                Text("Case", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Replace Input Box
        OutlinedTextField(
            value = replaceQuery,
            onValueChange = { viewModel.replaceQueryText.value = it },
            label = { Text("Replace text", color = Color.Gray, fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("replace_input_field"),
            textStyle = TextStyle(color = textColor, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = VsAccentColor,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Replace Action Buttons
        Button(
            onClick = { viewModel.replaceAllOccurrences() },
            enabled = searchQuery.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("replace_all_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = VsAccentColor,
                contentColor = Color.Black
            )
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FormatPaint, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Replace All Globally", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Divider(color = if (isDarkMode) Color(0xFF252526) else Color(0xFFDDDDDD))

        // Live Matches Results Header
        Text(
            text = "MATCHES FOUND (${searchResults.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        // Search Matches List Scrollable
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchResults) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectFile(result.file) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF212121) else Color(0xFFEBEBEB)
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = result.file.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) VsSecondaryAccent else Color(0xFF8B2500)
                            )
                            Text(
                                text = "Line ${result.lineNumber}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = result.lineContent.trim(),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = textColor,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GitSourceControlPanel(viewModel: CodeEditorViewModel) {
    val activeBranch by viewModel.activeBranch.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val changedFiles by viewModel.gitChangedFiles.collectAsState()
    val commitLogs by viewModel.commitLogs.collectAsState()
    val messageInput by viewModel.gitCommitMessageInput.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var isBranchDropdownExpanded by remember { mutableStateOf(false) }
    var isNewBranchDialogOpen by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }

    val textColor = if (isDarkMode) Color.LightGray else Color(0xFF232323)
    val cardBg = if (isDarkMode) Color(0xFF212121) else Color(0xFFEBEBEB)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "GIT SOURCE CONTROL",
            style = MaterialTheme.typography.titleSmall,
            color = if (isDarkMode) VsAccentColor else Color(0xFF007ACC),
            fontWeight = FontWeight.Bold
        )

        // Branch Selection Dropdown Header Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isBranchDropdownExpanded = true }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountTree,
                        contentDescription = null,
                        tint = VsAccentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Branch: $activeBranch",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Expand branch options",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )

                DropdownMenu(
                    expanded = isBranchDropdownExpanded,
                    onDismissRequest = { isBranchDropdownExpanded = false }
                ) {
                    branches.forEach { br ->
                        DropdownMenuItem(
                            text = { Text(br, fontSize = 12.sp) },
                            onClick = {
                                viewModel.checkoutGitBranch(br)
                                isBranchDropdownExpanded = false
                            }
                        )
                    }
                    Divider()
                    DropdownMenuItem(
                        text = { Text("+ Create New Branch...", fontSize = 12.sp, color = VsAccentColor) },
                        onClick = {
                            isNewBranchDialogOpen = true
                            isBranchDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Divider(color = if (isDarkMode) Color(0xFF252526) else Color(0xFFDDDDDD))

        Text(
            text = "CHANGES (${changedFiles.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        if (changedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No local commits pending staging",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.height(100.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(changedFiles.toList()) { (path, changeType) ->
                    val fileName = path.substringAfterLast('/')
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = null,
                                tint = if (isDarkMode) Color.Gray else Color.DarkGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Column {
                                Text(fileName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
                                Text(path, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        
                        Badge(
                            containerColor = if (changeType == "M") Color(0xFFE5C158) else Color(0xFF43C377),
                            contentColor = Color.Black
                        ) {
                            Text(changeType, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = messageInput,
            onValueChange = { viewModel.gitCommitMessageInput.value = it },
            placeholder = { Text("Write commit message...", fontSize = 11.sp, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().height(65.dp).testTag("git_commit_message_input"),
            textStyle = TextStyle(color = textColor, fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = VsAccentColor,
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = false,
            maxLines = 2
        )

        Button(
            onClick = { viewModel.commitGitChanges() },
            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("git_commit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = VsStatusBar,
                contentColor = Color.White
            )
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Commit to Branch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Divider(color = if (isDarkMode) Color(0xFF252526) else Color(0xFFDDDDDD))

        Text(
            text = "COMMIT REVISION LOGS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(commitLogs) { commit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "r_${commit.hash}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = VsAccentColor
                            )
                            Text(
                                text = "Committed",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = commit.message,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Author: devcode", fontSize = 9.sp, color = Color.Gray)
                            Text(text = "${commit.filesCount} file(s)", fontSize = 9.sp, color = VsSecondaryAccent)
                        }
                    }
                }
            }
        }
    }

    if (isNewBranchDialogOpen) {
        AlertDialog(
            onDismissRequest = { isNewBranchDialogOpen = false },
            title = { Text("Create & Checkout Branch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Specify target branch context to checkout:")
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        placeholder = { Text("e.g. bugfix/auth-leak") },
                        modifier = Modifier.fillMaxWidth().testTag("new_branch_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createGitBranch(newBranchName)
                        isNewBranchDialogOpen = false
                        newBranchName = ""
                    },
                    modifier = Modifier.testTag("confirm_branch_creation_btn")
                ) {
                    Text("Create Branch")
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewBranchDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardShortcutsConfigDialog(
    viewModel: CodeEditorViewModel,
    onDismiss: () -> Unit
) {
    val shortcuts by viewModel.customShortcuts.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val textColor = if (isDarkMode) Color.LightGray else Color(0xFF232323)
    val cardBg = if (isDarkMode) Color(0xFF2C2C2C) else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.85f),
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_shortcuts_dialog_button")
            ) {
                Text("Close Settings")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Keyboard, contentDescription = null, tint = VsAccentColor)
                Text("Custom Key Bindings Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tablet physical keyboard users can tap modifier switches and enter keys to map rapid actions.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Divider()

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shortcuts) { shortcut ->
                        var isEditing by remember { mutableStateOf(false) }
                        var tempKey by remember { mutableStateOf(shortcut.currentKey) }
                        var ctrlPressed by remember { mutableStateOf(shortcut.ctrlRequired) }
                        var altPressed by remember { mutableStateOf(shortcut.altRequired) }
                        var shiftPressed by remember { mutableStateOf(shortcut.shiftRequired) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF222222) else Color(0xFFF0F0F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = shortcut.commandName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Text(
                                            text = "Command ID: ${shortcut.id}",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (isEditing) {
                                                viewModel.updateShortcut(
                                                    shortcut.id,
                                                    tempKey,
                                                    ctrlPressed,
                                                    altPressed,
                                                    shiftPressed
                                                )
                                                isEditing = false
                                            } else {
                                                isEditing = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isEditing) VsAccentColor else Color.Gray,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("edit_key_binding_${shortcut.id}")
                                    ) {
                                        Text(if (isEditing) "Save Key" else "Change", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isEditing) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilterChip(
                                            selected = ctrlPressed,
                                            onClick = { ctrlPressed = !ctrlPressed },
                                            label = { Text("Ctrl", fontSize = 10.sp) },
                                            modifier = Modifier.testTag("chip_ctrl_${shortcut.id}")
                                        )
                                        FilterChip(
                                            selected = altPressed,
                                            onClick = { altPressed = !altPressed },
                                            label = { Text("Alt", fontSize = 10.sp) },
                                            modifier = Modifier.testTag("chip_alt_${shortcut.id}")
                                        )
                                        FilterChip(
                                            selected = shiftPressed,
                                            onClick = { shiftPressed = !shiftPressed },
                                            label = { Text("Shift", fontSize = 10.sp) },
                                            modifier = Modifier.testTag("chip_shift_${shortcut.id}")
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        OutlinedTextField(
                                            value = tempKey,
                                            onValueChange = {
                                                if (it.length <= 1) {
                                                    tempKey = it.uppercase()
                                                }
                                            },
                                            textStyle = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                color = textColor
                                            ),
                                            modifier = Modifier
                                                .width(55.dp)
                                                .height(48.dp)
                                                .testTag("edit_key_input_${shortcut.id}"),
                                            singleLine = true,
                                            maxLines = 1
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val readableKeys = buildString {
                                        if (shortcut.ctrlRequired) append("Ctrl + ")
                                        if (shortcut.altRequired) append("Alt + ")
                                        if (shortcut.shiftRequired) append("Shift + ")
                                        append(shortcut.currentKey)
                                    }
                                    Text(
                                        text = "Active Key Binding: $readableKeys",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) VsSecondaryAccent else Color(0xFF6B3FA0)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
