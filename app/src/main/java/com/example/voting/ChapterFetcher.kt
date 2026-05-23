package com.example.voting

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
}
