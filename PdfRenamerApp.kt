package com.example.pdfrenamer

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfRenamerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Necessário para o PdfBox-Android carregar as fontes/recursos internos
        PDFBoxResourceLoader.init(applicationContext)
    }
}
