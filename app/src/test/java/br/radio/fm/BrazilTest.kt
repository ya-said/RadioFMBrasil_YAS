package br.radio.fm

import br.radio.fm.domain.Brazil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrazilTest {
    @Test fun reconheceSigla() = assertEquals("SP", Brazil.ufOf("SP")?.code)
    @Test fun reconheceNomeComAcento() = assertEquals("SP", Brazil.ufOf("São Paulo")?.code)
    @Test fun reconheceNomeSemAcento() = assertEquals("SP", Brazil.ufOf("sao paulo")?.code)
    @Test fun distingueParaDeParana() {
        assertEquals("PA", Brazil.ufOf("Pará")?.code)
        assertEquals("PR", Brazil.ufOf("Paraná")?.code)
        assertEquals("PB", Brazil.ufOf("Paraíba")?.code)
    }
    @Test fun distingueMatoGrosso() {
        assertEquals("MT", Brazil.ufOf("Mato Grosso")?.code)
        assertEquals("MS", Brazil.ufOf("Mato Grosso do Sul")?.code)
    }
    @Test fun textoComPrefixo() = assertEquals("SP", Brazil.ufOf("Estado de São Paulo")?.code)
    @Test fun valorVazioOuDesconhecido() {
        assertNull(Brazil.ufOf(""))
        assertNull(Brazil.ufOf("Lisboa"))
    }
}
