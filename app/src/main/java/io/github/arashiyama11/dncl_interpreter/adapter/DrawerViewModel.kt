package io.github.arashiyama11.dncl_interpreter.adapter

import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.domain.model.FileName
import io.github.arashiyama11.domain.usecase.IFileNameValidationUseCase
import io.github.arashiyama11.domain.usecase.IFileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class DrawerUiState(
    val selectedFileName: FileName? = null,
    val files: List<FileName> = emptyList(),
    val isFileCreating: Boolean = false,
    val inputtingFileName: String? = null,
)

@KoinViewModel
class DrawerViewModel(
    private val fileUseCase: IFileUseCase,
    private val fileNameValidationUseCase: IFileNameValidationUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DrawerUiState())
    val uiState = combine(
        _uiState, fileUseCase.selectedFileName
    ) { state, fileName ->
        state.copy(
            selectedFileName = fileName,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, DrawerUiState())

    private var focusRequester: FocusRequester? = null
    val errorChannel = Channel<String>(Channel.BUFFERED)

    fun onStart(focusRequester: FocusRequester) {
        viewModelScope.launch {
            _uiState.update { it.copy(files = fileUseCase.getAllFileNames().orEmpty()) }
        }
        this.focusRequester = focusRequester
    }

    fun onFileSelected(fileIndex: Int) {
        viewModelScope.launch {
            fileUseCase.selectFile(FileName(uiState.value.files[fileIndex].value))
        }
    }

    fun onFileAddClicked() {
        _uiState.update {
            it.copy(
                files = _uiState.value.files + FileName(""),
                isFileCreating = true,
                inputtingFileName = ""
            )
        }
        viewModelScope.launch {
            repeat(3) {
                try {
                    delay(100)
                    focusRequester?.requestFocus()
                    return@launch
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onInputtingFileNameChanged(inputtingFileName: String) {
        if (inputtingFileName.lastOrNull() == '\n') {
            if (inputtingFileName.length > 1)
                return onFileAddConfirmed(inputtingFileName.dropLast(1))
        }
        _uiState.update { it.copy(inputtingFileName = inputtingFileName) }
    }

    private fun onFileAddConfirmed(newFileName: String) {
        viewModelScope.launch {
            fileNameValidationUseCase(FileName(newFileName)).mapLeft {
                errorChannel.send(it.message)
                return@launch
            }
            try {
                fileUseCase.createFile(FileName(newFileName))
                fileUseCase.selectFile(FileName(newFileName))
            } catch (e: Exception) {
                errorChannel.send(e.message ?: "Error")
            }
            _uiState.update {
                it.copy(
                    files = fileUseCase.getAllFileNames().orEmpty(),
                    isFileCreating = false,
                    inputtingFileName = null
                )
            }
        }
    }
}
