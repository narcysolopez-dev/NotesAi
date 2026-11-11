package com.narcy.notesai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.narcy.notesai.ui.NotesApp
import com.narcy.notesai.ui.theme.NotesAiTheme

class MainActivity : ComponentActivity() {
    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NotesAiTheme {
                NotesApp(viewModel)
            }
        }
    }
}
