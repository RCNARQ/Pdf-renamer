# PDF Renamer (Android)

App que lê PDFs de textos e renomeia os arquivos automaticamente para o padrão:

`Título - Autor.pdf`

## Como funciona

1. Você escolhe uma pasta (via seletor do sistema).
2. O app percorre todos os `.pdf` diretamente dentro dela.
3. Para cada arquivo:
   - Primeiro tenta ler os **metadados internos** do PDF (campos Title/Author, quando o PDF foi exportado com eles preenchidos — comum em artigos e e-books).
   - Se estiverem vazios, faz uma **heurística no texto da primeira página**: considera a primeira linha não vazia como título e procura, logo abaixo, uma linha curta com aparência de nome (ou que comece com "by", "por", "autor" etc.) como autor.
4. Renomeia o arquivo com `DocumentFile.renameTo()`, mantendo-o na mesma pasta.
5. Um log na tela mostra o que foi renomeado e o que não pôde ser identificado (nesses casos o arquivo é mantido com o nome original).

Não é preciso permissão de armazenamento no manifesto: o app usa o Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`), que já concede acesso de leitura/escrita à pasta escolhida.

## Como abrir e compilar

1. Instale o [Android Studio](https://developer.android.com/studio) (versão recente, Hedgehog/Iguana ou superior).
2. Abra a pasta `PDFRenamer` inteira em **File → Open**.
3. Aguarde o Gradle sincronizar (ele fará o download automático do wrapper e das dependências, incluindo a biblioteca `pdfbox-android`, que faz a leitura dos PDFs).
4. Conecte um celular Android (modo desenvolvedor + depuração USB) ou use um emulador, e clique em **Run ▶**.

Requisitos: `minSdk 24` (Android 7.0+).

## Limitações conhecidas

- A heurística de título/autor funciona melhor com PDFs "de texto" (artigos, e-books, capítulos), não com digitalizações/imagens escaneadas sem OCR.
- Só processa arquivos diretamente na pasta selecionada (não entra em subpastas). Se quiser, dá para estender `processFolder()` para percorrer recursivamente com `DocumentFile.listFiles()`.
- Como cada editora/gerador de PDF formata a capa de forma diferente, alguns arquivos podem não ter o autor identificado corretamente — nesses casos o app apenas usa o título, ou mantém o nome original se nada for encontrado.
