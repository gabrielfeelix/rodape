package com.example.voting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterFetcherTest {

    @Test
    fun `extrai capitulos do padrao Capitulo N - Titulo`() {
        val text = """
            Capítulo 1 - A culpa é minha
            Capítulo 2 - Deixa eu pelo menos pensar
            Capítulo 3 - Ela não sabia nada
        """.trimIndent()
        val result = ChapterFetcher.extractFromText(text)
        assertEquals(3, result.size)
        assertEquals(1 to "A culpa é minha", result[0])
        assertEquals(3 to "Ela não sabia nada", result[2])
    }

    @Test
    fun `extrai capitulos do padrao Chapter N`() {
        val text = """
            Chapter 1 The Beginning
            Chapter 2 The Middle
            Chapter 3 The End
        """.trimIndent()
        val result = ChapterFetcher.extractFromText(text)
        assertEquals(3, result.size)
        assertEquals(1, result[0].first)
        assertEquals("The Beginning", result[0].second)
    }

    @Test
    fun `texto sem padrao retorna lista vazia`() {
        val text = "Some random book description without chapters listed."
        val result = ChapterFetcher.extractFromText(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `validate falha com menos de 3`() {
        val result = ChapterFetcher.validate(listOf(1 to "a", 2 to "b"))
        assertEquals(ChapterFetchResult.Failed, result)
    }

    @Test
    fun `validate aceita 3 ou mais`() {
        val list = listOf(1 to "a", 2 to "b", 3 to "c")
        val result = ChapterFetcher.validate(list)
        assertTrue(result is ChapterFetchResult.Success)
        assertEquals(list, (result as ChapterFetchResult.Success).chapters)
    }

    @Test
    fun `dedup por numero`() {
        val text = """
            Capítulo 1 - A
            Capítulo 1 - duplicado
            Capítulo 2 - B
            Capítulo 3 - C
        """.trimIndent()
        val result = ChapterFetcher.extractFromText(text)
        assertEquals(3, result.size)
        assertEquals("A", result.first { it.first == 1 }.second)
    }
}
