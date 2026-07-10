package com.example.pdfrenamer

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.example.pdfrenamer.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val log = StringBuilder()

    private val pickFolder = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri == null) {
            appendLog("Nenhuma pasta selecionada.")
            return@registerForActivityResult
        }
        // Persiste a permissão para poder ler/renomear os arquivos depois
        contentResolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        processFolder(treeUri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectFolder.setOnClickListener {
            log.clear()
            binding.tvLog.text = ""
            pickFolder.launch(null)
        }
    }

    private fun processFolder(treeUri: Uri) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSelectFolder.isEnabled = false

        lifecycleScope.launch {
            val pdfFiles = withContext(Dispatchers.IO) {
                val root = DocumentFile.fromTreeUri(this@MainActivity, treeUri)
                root?.listFiles()?.filter {
                    it.isFile && (it.name?.endsWith(".pdf", ignoreCase = true) == true)
                } ?: emptyList()
            }

            if (pdfFiles.isEmpty()) {
                appendLog("Nenhum PDF encontrado nessa pasta.")
            } else {
                appendLog("Encontrados ${pdfFiles.size} PDF(s). Processando...\n")
            }

            var renamed = 0
            for (file in pdfFiles) {
                val originalName = file.name ?: "arquivo.pdf"
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val info = PdfMetadataExtractor.extract(this@MainActivity, file.uri)
                        val newName = buildFileName(info, originalName)
                        if (newName != null && newName != originalName) {
                            val ok = file.renameTo(newName)
                            if (ok) newName else null
                        } else {
                            null
                        }
                    }.getOrNull()
                }

                if (result != null) {
                    renamed++
                    appendLog("✔ \"$originalName\" → \"$result\"")
                } else {
                    appendLog("— \"$originalName\": título/autor não identificados, mantido")
                }
            }

            appendLog("\nConcluído: $renamed de ${pdfFiles.size} arquivo(s) renomeado(s).")
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnSelectFolder.isEnabled = true
        }
    }

    /** Monta "Título - Autor.pdf" a partir dos dados extraídos, higienizando o nome. */
    private fun buildFileName(info: PdfInfo, fallbackName: String): String? {
        val title = info.title?.let(::sanitize)
        val author = info.author?.let(::sanitize)

        val base = when {
            !title.isNullOrBlank() && !author.isNullOrBlank() -> "$title - $author"
            !title.isNullOrBlank() -> title
            else -> return null
        }

        val truncated = base.take(150).trim(' ', '-')
        return if (truncated.isBlank()) null else "$truncated.pdf"
    }

    private fun sanitize(text: String): String {
        return text
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun appendLog(line: String) {
        log.append(line).append("\n")
        binding.tvLog.text = log.toString()
    }
}
