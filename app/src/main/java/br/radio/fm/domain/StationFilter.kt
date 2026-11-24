package br.radio.fm.domain

/** Estação com texto de busca e metadados pré-calculados (evita recalcular a cada tecla). */
class IndexedStation(val station: Station) {
    val haystack: String = Brazil.normalize("${station.name} ${station.state} ${station.tags}")
    val uf: String? = Brazil.ufOf(station.state)?.code
    val categories: Set<Category> = Category.values().filter { it.matches(station) }.toSet()
}

object StationFilter {
    fun apply(
        index: List<IndexedStation>,
        query: String,
        uf: String?,
        category: Category?,
    ): List<Station> {
        val tokens = Brazil.normalize(query).split(' ').filter { it.isNotEmpty() }
        return index.asSequence()
            .filter { uf == null || it.uf == uf }
            .filter { category == null || category in it.categories }
            .filter { item -> tokens.all { item.haystack.contains(it) } }
            .map { it.station }
            .toList()
    }
}
