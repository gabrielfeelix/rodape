package com.example.util.voting

sealed class ChapterFetchResult {
    data class Success(val chapters: List<Pair<Int, String>>) : ChapterFetchResult()
    object Failed : ChapterFetchResult()
}

object ChapterFetcher {

    fun extractFromText(text: String): List<Pair<Int, String>> {
        if (text.isBlank()) return emptyList()
        val lines = text.split("\n", "<br>", "<br/>", "<br />")
        val regex = Regex(
            """^\s*(?:Cap[ií]tulo|Chapter|Cap\.?)\s+(\d+)\s*[\-—:.]?\s*(.{0,120})$""",
            RegexOption.IGNORE_CASE
        )
        val results = mutableListOf<Pair<Int, String>>()
        for (line in lines) {
            val cleaned = line.trim()
            val m = regex.find(cleaned) ?: continue
            val num = m.groupValues[1].toIntOrNull() ?: continue
            val title = m.groupValues[2].trim()
            results.add(num to title)
        }
        return results
            .distinctBy { it.first }
            .sortedBy { it.first }
    }

    fun validate(chapters: List<Pair<Int, String>>): ChapterFetchResult {
        if (chapters.size < 3) return ChapterFetchResult.Failed
        return ChapterFetchResult.Success(chapters)
    }

    /**
     * Extrai capítulos do table_of_contents do Open Library. Cada entrada tem um
     * `label` (ex.: "Chapter 1", "Capítulo 3", "1") e um `title`. Pegamos só as
     * entradas cujo label indica um número de capítulo (ignora prefácio, sumário,
     * sub-seções), pra não gerar 300 "capítulos" a partir de sub-tópicos.
     * Recebe pares (label, title).
     */
    fun fromOpenLibraryToc(entries: List<Pair<String?, String?>>): ChapterFetchResult {
        val labelRegex = Regex("""^(?:chapter|cap[íi]tulo|cap\.?)?\s*(\d{1,3})\b""", RegexOption.IGNORE_CASE)
        val chapters = entries.mapNotNull { (label, title) ->
            val l = label?.trim().orEmpty()
            val num = labelRegex.find(l)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            num to (title?.trim().orEmpty())
        }.distinctBy { it.first }.sortedBy { it.first }
        return validate(chapters)
    }
}
