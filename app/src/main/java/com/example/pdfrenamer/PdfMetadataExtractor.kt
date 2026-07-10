package com.example.pdfrenamer

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

data class PdfInfo(val title: String?, val author: String?)

object PdfMetadataExtractor {

    // Palavras que costumam indicar uma linha de autoria no início do texto
    private val authorHints = listOf(
        "by ", "por ", "autor", "author", "autores", "de ", "written by"
    )

    /**
     * Tenta extrair título e autor de um PDF:
     * 1) Primeiro olha os metadados internos do documento (Title/Author).
     * 2) Se estiverem vazios, tenta inferir a partir do texto da primeira página,
     *    assumindo que a primeira linha não vazia é o título e procurando, logo
     *    abaixo, uma linha curta que pareça um nome de autor.
     */
    fun extract(context: Context, uri: Uri): PdfInfo {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val document = PDDocument.load(input)
            try {
                val info = document.documentInformation
                var title = info?.title?.trim()?.takeIf { it.isNotBlank() }
                var author = info?.author?.trim()?.takeIf { it.isNotBlank() }

                if (title == null || author == null) {
                    val stripper = PDFTextStripper()
                    stripper.startPage = 1
                    stripper.endPage = minOf(1, document.numberOfPages)
                    val text = stripper.getText(document)
                    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

                    if (title == null && lines.isNotEmpty()) {
                        title = lines.first().take(120)
                    }

                    if (author == null) {
                        author = guessAuthor(lines)
                    }
                }

                return PdfInfo(title, author)
            } finally {
                document.close()
            }
        }
        return PdfInfo(null, null)
    }

    private fun guessAuthor(lines: List<String>): String? {
        // Examina as próximas linhas após o título (até 6 linhas)
        val candidates = lines.drop(1).take(6)

        // 1) Linha que começa explicitamente com uma dica de autoria
        candidates.forEach { line ->
            val lower = line.lowercase()
            authorHints.forEach { hint ->
                if (lower.startsWith(hint)) {
                    return line.substring(hint.length).trim().take(80).ifBlank { null }
                }
            }
        }

        // 2) Linha curta, sem pontuação de frase, com poucas palavras
        //    capitalizadas — padrão comum de "Nome Sobrenome"
        candidates.forEach { line ->
            val words = line.split(" ").filter { it.isNotBlank() }
            val looksLikeName = line.length in 4..60 &&
                words.size in 2..5 &&
                !line.contains(".") &&
                words.all { it.first().isUpperCase() || !it.first().isLetter() }
            if (looksLikeName) return line.take(80)
        }

        return null
    }
}
