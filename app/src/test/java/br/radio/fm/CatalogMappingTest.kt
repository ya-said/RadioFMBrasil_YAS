package br.radio.fm

import br.radio.fm.data.StationDto
import br.radio.fm.data.StreamValidator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMappingTest {
    private val json = """
        [
          {"stationuuid":"a1","name":"  Rádio Exemplo ","url":"http://x.com/a.pls","url_resolved":"https://x.com/live",
           "favicon":"https://x.com/logo.png","tags":"sertanejo,pop","state":"Goiás","codec":"MP3","bitrate":128,
           "votes":10,"clickcount":99,"lastcheckok":1},
          {"stationuuid":"a2","name":"Fora do ar","url_resolved":"https://x.com/off","lastcheckok":0},
          {"stationuuid":"a3","name":"URL inválida","url_resolved":"ftp://x.com/a","lastcheckok":1},
          {"stationuuid":"a4","name":"","url_resolved":"https://x.com/b","lastcheckok":1},
          {"name":"Sem id","url_resolved":"https://x.com/c","lastcheckok":1}
        ]
    """.trimIndent()

    @Test
    fun aceitaSomenteEmissorasValidas() {
        val type = object : TypeToken<List<StationDto>>() {}.type
        val dtos: List<StationDto> = Gson().fromJson(json, type)
        val stations = dtos.mapNotNull { it.toStation() }
        assertEquals(1, stations.size)
        val s = stations.first()
        assertEquals("Rádio Exemplo", s.name)
        assertEquals("https://x.com/live", s.url) // prefere url_resolved
        assertEquals("Goiás", s.state)
        assertEquals(99, s.clicks)
    }

    @Test
    fun usaUrlQuandoResolvedVazia() {
        val dto = StationDto("b", "Teste", "", "http://a.com/s", null, null, null, null, null, null, null, null, 1)
        assertEquals("http://a.com/s", dto.toStation()?.url)
    }

    @Test
    fun validadorDeUrl() {
        assertTrue(StreamValidator.isValid("https://a.com/stream"))
        assertTrue(StreamValidator.isValid("http://a.com:8000/;"))
        assertFalse(StreamValidator.isValid("ftp://a.com"))
        assertFalse(StreamValidator.isValid("javascript:alert(1)"))
        assertFalse(StreamValidator.isValid("  "))
        assertFalse(StreamValidator.isValid(null))
        assertNull(StationDto("c", "X", "nao-e-url", null, null, null, null, null, null, null, null, null, 1).toStation())
    }
}
