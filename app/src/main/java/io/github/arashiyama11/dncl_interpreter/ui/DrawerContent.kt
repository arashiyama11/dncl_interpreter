package io.github.arashiyama11.dncl_interpreter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_interpreter.adapter.CreatingType
import io.github.arashiyama11.dncl_interpreter.adapter.DrawerViewModel
import io.github.arashiyama11.domain.model.EntryPath
import io.github.arashiyama11.domain.model.Folder
import io.github.arashiyama11.domain.model.ProgramFile

@Composable
fun DrawerContent(drawerViewModel: DrawerViewModel) {
    val uiState by drawerViewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        drawerViewModel.onStart(focusRequester)
    }
    ModalDrawerSheet {
        var isShowFiles by remember { mutableStateOf(true) }
        NavigationDrawerItem(
            label = { Text(text = "Files") },
            selected = false,
            icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            onClick = {
                isShowFiles = !isShowFiles
                drawerViewModel.onFolderClicked(null)
            },
        )
        AnimatedVisibility(isShowFiles && uiState.rootFolder != null) {
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .wrapContentHeight()
            ) {
                for (entry in uiState.rootFolder!!.entities) {
                    when (entry) {
                        is Folder -> FolderItem(
                            entry,
                            0,
                            uiState.inputtingEntryPath, uiState.inputtingFileName,
                            focusRequester,
                            uiState.creatingType,
                            drawerViewModel::onInputtingFileNameChanged,
                            drawerViewModel::onFileSelected,
                            drawerViewModel::onFolderClicked
                        )

                        is ProgramFile -> FileItem(
                            entry,
                            0,
                            drawerViewModel::onFileSelected
                        )
                    }
                }
                if (uiState.inputtingEntryPath != null && uiState.inputtingEntryPath.toString() == uiState.rootFolder!!.path.toString()) {
                    NavigationDrawerItem(
                        label = {
                            OutlinedTextField(
                                value = uiState.inputtingFileName.orEmpty(),
                                onValueChange = {
                                    drawerViewModel.onInputtingFileNameChanged(it)
                                },
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .padding(vertical = 8.dp)
                            )
                        },
                        selected = false,
                        icon = {
                            Icon(
                                if (uiState.creatingType == CreatingType.FILE) Icons.AutoMirrored.Outlined.InsertDriveFile else Icons.Filled.Folder,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            focusRequester.requestFocus()
                        }
                    )
                }
            }
        }

        var isShowSettings by remember { mutableStateOf(false) }

        NavigationDrawerItem(label = {
            Text(text = "Settings")
        }, selected = false, icon = {
            Icon(Icons.Outlined.Settings, contentDescription = null)
        }, onClick = {
            isShowSettings = !isShowSettings
        })

        AnimatedVisibility(isShowSettings) {
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .wrapContentHeight()
            ) {
                NavigationDrawerItem(label = {
                    Text(text = "配列の最初の要素の添字を1にする")
                }, selected = false, onClick = {
                }, badge = {
                    Switch(uiState.list1IndexSwitchEnabled, onCheckedChange = {
                        drawerViewModel.onList1IndexSwitchClicked(it)
                    })
                })
            }
        }
    }
}


@Composable
fun FileItem(
    file: ProgramFile,
    depth: Int,
    onClick: (ProgramFile) -> Unit
) {
    NavigationDrawerItem(
        label = { Text(text = file.name.value) },
        selected = false,
        icon = {
            Icon(
                Icons.AutoMirrored.Outlined.InsertDriveFile,
                contentDescription = null
            )
        },
        onClick = {
            onClick(file)
        }, modifier = Modifier.padding(start = 16.dp * depth)
    )
}

@Composable
fun FolderItem(
    folder: Folder, depth: Int = 0,
    inputtingEntryPath: EntryPath?,
    inputtingFileName: String?,
    focusRequester: FocusRequester,
    creatingType: CreatingType?,
    onInputtingEntryNameChanged: (String) -> Unit,
    onFileClick: (ProgramFile) -> Unit,
    onFolderClick: (Folder) -> Unit
) {
    var isShowFiles by remember { mutableStateOf(true) }
    Column {
        Row(
            modifier = Modifier.padding(start = 16.dp * depth),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationDrawerItem(
                label = { Text(text = folder.name.value) },
                selected = false,
                icon = {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null
                    )
                },
                onClick = {
                    onFolderClick(folder)
                    isShowFiles = !isShowFiles || folder.entities.isEmpty()
                },
            )
        }

        if (isShowFiles) {
            for (entry in folder.entities) {
                when (entry) {
                    is Folder -> FolderItem(
                        entry,
                        depth + 1,
                        inputtingEntryPath,
                        inputtingFileName,
                        focusRequester,
                        creatingType,
                        onInputtingEntryNameChanged,
                        onFileClick, onFolderClick
                    )

                    is ProgramFile -> FileItem(entry, depth + 1, onFileClick)
                }
            }
            if (inputtingEntryPath != null && inputtingEntryPath.toString() == folder.path.toString()) {
                NavigationDrawerItem(
                    label = {
                        OutlinedTextField(
                            value = inputtingFileName.orEmpty(),
                            onValueChange = {
                                onInputtingEntryNameChanged(it)
                            },
                            modifier = Modifier.focusRequester(focusRequester)
                        )
                    },
                    selected = false,
                    icon = {
                        Icon(
                            if (creatingType == CreatingType.FILE) Icons.AutoMirrored.Outlined.InsertDriveFile else Icons.Filled.Folder,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        focusRequester.requestFocus()
                    }, modifier = Modifier.padding(start = 16.dp * (depth + 1))
                )
            }
        }
    }
}
