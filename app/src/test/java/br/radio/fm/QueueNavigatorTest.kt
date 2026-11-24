package br.radio.fm

import br.radio.fm.domain.QueueNavigator
import br.radio.fm.domain.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueNavigatorTest {
    private fun st(id: String) = Station(id = id, name = id, url = "https://x/$id")
    private val queue = listOf(st("a"), st("b"), st("c"))

    @Test fun proxima() = assertEquals("b", QueueNavigator.neighbor(queue, "a", 1)?.id)
    @Test fun anterior() = assertEquals("a", QueueNavigator.neighbor(queue, "b", -1)?.id)
    @Test fun voltaAoInicioNoFim() = assertEquals("a", QueueNavigator.neighbor(queue, "c", 1)?.id)
    @Test fun voltaAoFimNoInicio() = assertEquals("c", QueueNavigator.neighbor(queue, "a", -1)?.id)
    @Test fun listaComUmItemNaoNavega() = assertNull(QueueNavigator.neighbor(listOf(st("a")), "a", 1))
    @Test fun emissoraForaDaLista() = assertNull(QueueNavigator.neighbor(queue, "z", 1))
    @Test fun semEmissoraAtual() = assertNull(QueueNavigator.neighbor(queue, null, 1))
}
