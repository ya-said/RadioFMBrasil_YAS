package br.radio.fm.domain

enum class Category(val label: String, private val keys: List<String>) {
    SERTANEJO("Sertanejo", listOf("sertanejo", "sertanejas")),
    GOSPEL("Gospel", listOf("gospel", "evangelica", "evangelico", "christian", "cristao", "crista")),
    NOTICIAS("Notícias", listOf("news", "noticias", "noticia", "jornalismo", "talk")),
    ESPORTES("Esportes", listOf("sport", "sports", "esporte", "esportes", "futebol")),
    POP("Pop", listOf("pop")),
    ROCK("Rock", listOf("rock")),
    MPB("MPB", listOf("mpb")),
    SAMBA("Samba e pagode", listOf("samba", "pagode")),
    FORRO("Forró", listOf("forro")),
    ELETRONICA("Música eletrônica", listOf("electronic", "eletronica", "dance", "techno", "house", "edm")),
    INTERNACIONAL("Música internacional", listOf("international", "internacional")),
    RELIGIOSA("Programação religiosa", listOf("religiosa", "religious", "catolica", "catholic", "igreja")),
    COMUNITARIA("Rádios comunitárias", listOf("comunitaria", "community", "comunitario")),
    UNIVERSITARIA("Rádios universitárias", listOf("universitaria", "university", "universidade")),
    PUBLICA("Rádios públicas", listOf("publica", "public", "educativa"));

    fun matches(station: Station): Boolean {
        val tokens = Brazil.normalize("${station.tags} ${station.name}")
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() }
        return keys.any { key -> tokens.any { token -> token == key } }
    }
}
