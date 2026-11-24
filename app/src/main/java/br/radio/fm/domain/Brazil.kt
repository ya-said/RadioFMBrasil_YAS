package br.radio.fm.domain

import java.text.Normalizer

object Brazil {
    data class UF(val code: String, val name: String, val region: String)

    val ufs = listOf(
        UF("AC", "Acre", "Norte"), UF("AL", "Alagoas", "Nordeste"),
        UF("AP", "Amapá", "Norte"), UF("AM", "Amazonas", "Norte"),
        UF("BA", "Bahia", "Nordeste"), UF("CE", "Ceará", "Nordeste"),
        UF("DF", "Distrito Federal", "Centro-Oeste"), UF("ES", "Espírito Santo", "Sudeste"),
        UF("GO", "Goiás", "Centro-Oeste"), UF("MA", "Maranhão", "Nordeste"),
        UF("MT", "Mato Grosso", "Centro-Oeste"), UF("MS", "Mato Grosso do Sul", "Centro-Oeste"),
        UF("MG", "Minas Gerais", "Sudeste"), UF("PA", "Pará", "Norte"),
        UF("PB", "Paraíba", "Nordeste"), UF("PR", "Paraná", "Sul"),
        UF("PE", "Pernambuco", "Nordeste"), UF("PI", "Piauí", "Nordeste"),
        UF("RJ", "Rio de Janeiro", "Sudeste"), UF("RN", "Rio Grande do Norte", "Nordeste"),
        UF("RS", "Rio Grande do Sul", "Sul"), UF("RO", "Rondônia", "Norte"),
        UF("RR", "Roraima", "Norte"), UF("SC", "Santa Catarina", "Sul"),
        UF("SP", "São Paulo", "Sudeste"), UF("SE", "Sergipe", "Nordeste"),
        UF("TO", "Tocantins", "Norte"),
    )

    private val accents = Regex("\\p{Mn}+")

    /** Minúsculas e sem acentos, para comparar textos. */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(accents, "").lowercase().trim()

    private val exact: Map<String, UF> = buildMap {
        ufs.forEach {
            put(normalize(it.name), it)
            put(it.code.lowercase(), it)
        }
    }
    private val byLongestName = ufs.sortedByDescending { it.name.length }

    fun ufOf(state: String): UF? {
        val key = normalize(state)
        if (key.isEmpty()) return null
        exact[key]?.let { return it }
        return byLongestName.firstOrNull { key.contains(normalize(it.name)) }
    }

    fun byCode(code: String?): UF? = ufs.firstOrNull { it.code == code }
}
