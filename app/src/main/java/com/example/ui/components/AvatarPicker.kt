package com.example.ui.components

/**
 * Escolha automatica de avatar preset baseada no nome do usuario.
 *
 * Estrategia:
 *  1. Detecta genero pelo primeiro nome (heuristica pt-BR — nao perfeita, mas
 *     funciona pra maioria dos nomes comuns).
 *  2. Sorteia um preset dentro do grupo apropriado, com seed estavel baseada
 *     no nome — mesma pessoa sempre pega o mesmo avatar (nao muda a cada login).
 *  3. Se nao deu pra detectar, escolhe entre os neutros.
 *
 * Presets disponiveis (vide Avatar.kt):
 *  Femininos: leitora, alice, petalas, joana_darc, emilia
 *  Masculinos: leitor, don_quixote, indigena, detetive, mago, fantasma
 *  Universal:  pequeno_principe (crianca, ambiguo)
 */
object AvatarPicker {

    private val femininos = listOf(
        "preset:leitora",
        "preset:alice",
        "preset:petalas",
        "preset:joana_darc",
        "preset:emilia",
    )

    private val masculinos = listOf(
        "preset:leitor",
        "preset:don_quixote",
        "preset:indigena",
        "preset:detetive",
        "preset:mago",
        "preset:fantasma",
    )

    private val universais = listOf("preset:pequeno_principe")

    // Excecoes que terminam em "a" mas sao masculinos
    private val masculinosExcecao = setOf(
        "joshua", "joaquia", "isaias", "tobias", "matias", "elias",
        "lucas", "thomas", "andre", "noah", "joaquim", "jeronimo",
        "tiago", "vicente", "felipe", "lucca",
    )

    // Excecoes que terminam diferente mas sao femininos
    private val femininosExcecao = setOf(
        "beatriz", "carmen", "ines", "agnes", "miriam", "ester", "raquel",
        "elizabeth", "isabel", "isabelly", "thalys",
    )

    /**
     * Retorna avatar_key apropriado pro nome dado. Determinista: mesma string
     * sempre retorna mesmo preset, entao o avatar e estavel.
     */
    fun pickFor(fullName: String?): String {
        val first = fullName?.trim()?.split(" ")?.firstOrNull()?.lowercase() ?: return universais.first()
        if (first.isBlank()) return universais.first()

        val pool = when {
            first in masculinosExcecao -> masculinos
            first in femininosExcecao -> femininos
            // Heuristica: termina em "a" ou "ah" ou "ia" geralmente feminino em pt-BR
            first.endsWith("a") || first.endsWith("ah") -> femininos
            // Termina em "o", "el", "il", "or", "on", "an", "ar" geralmente masculino
            first.endsWith("o") || first.endsWith("el") || first.endsWith("il") ||
                first.endsWith("or") || first.endsWith("on") || first.endsWith("an") -> masculinos
            // Fallback: neutro
            else -> universais + masculinos.take(2) + femininos.take(2)
        }

        // Seed estavel a partir do nome -> mesmo nome sempre pega mesmo preset
        val idx = (first.hashCode().toUInt() % pool.size.toUInt()).toInt()
        return pool[idx]
    }
}
