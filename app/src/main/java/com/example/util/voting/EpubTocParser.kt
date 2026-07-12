package com.example.util.voting

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Extrai o índice (table of contents) de um arquivo EPUB.
 *
 * EPUB é um zip. O índice fica em:
 *  - EPUB3: um documento de navegação (nav.xhtml) — `<nav epub:type="toc">` com `<a>`.
 *  - EPUB2: um NCX (toc.ncx) — `<navPoint><navLabel><text>...</text></navLabel>`.
 *
 * Best-effort e TOLERANTE: qualquer falha (zip inválido, sem TOC) devolve lista
 * vazia. Usa regex tolerante em vez de parser XML estrito (EPUBs no mundo real
 * têm namespaces/variações que quebram parser rígido).
 */
object EpubTocParser {

    fun parse(epubBytes: ByteArray): List<Pair<Int, String>> =
        try {
            parseInner(epubBytes)
        } catch (e: Exception) {
            emptyList()
        }

    private fun parseInner(epubBytes: ByteArray): List<Pair<Int, String>> {
        val files = readRelevantFiles(epubBytes)
        val container = files["META-INF/container.xml"] ?: return emptyList()
        val opfPath = Regex("""full-path="([^"]+)"""").find(container)?.groupValues?.get(1) ?: return emptyList()
        val opf = files[opfPath] ?: return emptyList()
        val baseDir = opfPath.substringBeforeLast('/', "")
        fun resolve(href: String): String {
            val clean = href.substringBefore('#')
            return if (baseDir.isEmpty()) clean else "$baseDir/$clean"
        }

        // EPUB3: item com properties="nav"
        val navHref = Regex("""<item\b[^>]*\bproperties="[^"]*\bnav\b[^"]*"[^>]*\bhref="([^"]+)"""").find(opf)?.groupValues?.get(1)
            ?: Regex("""<item\b[^>]*\bhref="([^"]+)"[^>]*\bproperties="[^"]*\bnav\b[^"]*"""").find(opf)?.groupValues?.get(1)
        if (navHref != null) {
            files[resolve(navHref)]?.let { extractNavTitles(it) }?.let { titles ->
                if (titles.size >= 3) return numbered(titles)
            }
        }

        // EPUB2: item media-type application/x-dtbncx+xml
        val ncxHref = Regex("""<item\b[^>]*\bmedia-type="application/x-dtbncx\+xml"[^>]*\bhref="([^"]+)"""").find(opf)?.groupValues?.get(1)
            ?: Regex("""<item\b[^>]*\bhref="([^"]+)"[^>]*\bmedia-type="application/x-dtbncx\+xml"""").find(opf)?.groupValues?.get(1)
        if (ncxHref != null) {
            files[resolve(ncxHref)]?.let { extractNcxTitles(it) }?.let { titles ->
                if (titles.size >= 3) return numbered(titles)
            }
        }
        return emptyList()
    }

    // Lê só os arquivos de metadados/TOC do zip (não capítulos inteiros), com teto.
    private fun readRelevantFiles(bytes: ByteArray): Map<String, String> {
        val out = HashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                val name = e.name
                if (!e.isDirectory && (
                        name.endsWith(".opf") || name.endsWith(".ncx") ||
                        name.endsWith("container.xml") ||
                        name.endsWith("nav.xhtml") || name.endsWith("toc.xhtml") ||
                        name.endsWith("toc.ncx")
                    )) {
                    val data = zis.readBytes()
                    if (data.size <= 2_000_000) out[name] = data.toString(Charsets.UTF_8)
                }
                zis.closeEntry()
                e = zis.nextEntry
            }
        }
        // fallback: se não achou nav/ncx por nome, lê qualquer .xhtml/.html pequeno
        if (out.keys.none { it.endsWith(".ncx") || it.endsWith("nav.xhtml") || it.endsWith("toc.xhtml") }) {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    val name = e.name
                    if (!e.isDirectory && (name.endsWith(".xhtml") || name.endsWith(".html")) && !out.containsKey(name)) {
                        val data = zis.readBytes()
                        if (data.size <= 500_000 && data.toString(Charsets.UTF_8).contains("epub:type=\"toc\"", ignoreCase = true)) {
                            out[name] = data.toString(Charsets.UTF_8)
                        }
                    }
                    zis.closeEntry()
                    e = zis.nextEntry
                }
            }
        }
        return out
    }

    private fun extractNavTitles(nav: String): List<String> {
        val toc = Regex("""<nav\b[^>]*epub:type="[^"]*\btoc\b[^"]*".*?</nav>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(nav)?.value ?: nav
        return Regex("""<a\b[^>]*>(.*?)</a>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .findAll(toc).map { cleanText(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()
    }

    private fun extractNcxTitles(ncx: String): List<String> =
        Regex("""<navPoint\b.*?<text>(.*?)</text>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .findAll(ncx).map { cleanText(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()

    private fun cleanText(s: String): String =
        s.replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&#39;", "'").replace("&apos;", "'").replace("&quot;", "\"").replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ").trim()

    private fun numbered(titles: List<String>): List<Pair<Int, String>> =
        titles.take(400).mapIndexed { i, t -> (i + 1) to t }
}
