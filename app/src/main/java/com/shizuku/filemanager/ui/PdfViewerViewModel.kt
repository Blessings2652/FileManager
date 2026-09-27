package com.shizuku.filemanager.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shizuku.filemanager.db.DocumentAnnotation
import com.shizuku.filemanager.db.TagDatabase
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class PdfViewerViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val database = TagDatabase.getDatabase(application)
    private val annotationDao = database.annotationDao()
    private var tts: TextToSpeech? = null
    
    private val _searchResult = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResult: StateFlow<List<SearchResult>> = _searchResult

    private val _annotations = MutableStateFlow<List<DocumentAnnotation>>(emptyList())
    val annotations: StateFlow<List<DocumentAnnotation>> = _annotations

    private val _isTtsActive = MutableStateFlow(false)
    val isTtsActive: StateFlow<Boolean> = _isTtsActive

    private val _extractedText = MutableStateFlow<Map<Int, String>>(emptyMap())
    
    init {
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    fun loadAnnotations(path: String) {
        viewModelScope.launch {
            _annotations.value = annotationDao.getAnnotations(path)
        }
    }

    fun addAnnotation(annotation: DocumentAnnotation) {
        viewModelScope.launch {
            annotationDao.saveAnnotation(annotation)
            loadAnnotations(annotation.path)
        }
    }

    fun deleteAnnotation(annotation: DocumentAnnotation) {
        viewModelScope.launch {
            annotationDao.deleteAnnotation(annotation)
            loadAnnotations(annotation.path)
        }
    }

    fun search(path: String, query: String) {
        if (query.isBlank()) {
            _searchResult.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = mutableListOf<SearchResult>()
                val file = File(path)
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(getApplication())
                PDDocument.load(file).use { doc ->
                    val stripper = PDFTextStripper()
                    for (i in 0 until doc.numberOfPages) {
                        stripper.startPage = i + 1
                        stripper.endPage = i + 1
                        val text = stripper.getText(doc)
                        if (text.contains(query, ignoreCase = true)) {
                            results.add(SearchResult(i, query))
                        }
                    }
                }
                _searchResult.value = results
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun speakPage(path: String, pageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = _extractedText.value[pageIndex] ?: run {
                    val file = File(path)
                    com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(getApplication())
                    PDDocument.load(file).use { doc ->
                        val stripper = PDFTextStripper().apply {
                            startPage = pageIndex + 1
                            endPage = pageIndex + 1
                        }
                        stripper.getText(doc)
                    }
                }
                
                if (text.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        _isTtsActive.value = true
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PDF_TTS")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isTtsActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    data class SearchResult(val pageIndex: Int, val text: String)
}
