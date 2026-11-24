package br.radio.fm

import br.radio.fm.domain.Category
import br.radio.fm.domain.IndexedStation
import br.radio.fm.domain.Station
import br.radio.fm.domain.StationFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class StationFilterTest {
    private fun st(id: String, name: String, state: String, tags: String) =
        Station(id = id, name = name, url = "https://exemplo.com/$id", state = state, tags = tags)

    private val index = listOf(
        st("1", "Rádio Jovem Pan", "São Paulo", "news,talk"),
        st("2", "Bahia FM", "Bahia", "sertanejo,pop"),
        st("3", "Gaúcha", "Rio Grande do Sul", "news"),
    ).map { IndexedStation(it) }

    private fun ids(q: String = "", uf: String? = null, c: Category? = null) =
        StationFilter.apply(index, q, uf, c).map { it.id }

    @Test fun buscaIgnoraAcentosECaixa() = assertEquals(listOf("1"), ids(q = "RADIO jovem"))
    @Test fun buscaPorEstadoNoTexto() = assertEquals(listOf("1"), ids(q = "sao paulo"))
    @Test fun filtroPorUf() = assertEquals(listOf("2"), ids(uf = "BA"))
    @Test fun filtroPorCategoria() = assertEquals(listOf("1", "3"), ids(c = Category.NOTICIAS))
    @Test fun filtrosCombinados() = assertEquals(listOf("3"), ids(uf = "RS", c = Category.NOTICIAS))
    @Test fun semResultados() = assertEquals(emptyList<String>(), ids(q = "inexistente"))
    @Test fun consultaVaziaRetornaTudo() = assertEquals(listOf("1", "2", "3"), ids())
}
