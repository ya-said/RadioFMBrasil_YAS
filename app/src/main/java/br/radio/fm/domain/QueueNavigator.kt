package br.radio.fm.domain

/** Escolhe a emissora anterior/próxima dentro de uma lista, voltando ao início/fim quando acaba. */
object QueueNavigator {
    fun neighbor(queue: List<Station>, currentId: String?, delta: Int): Station? {
        if (queue.size < 2 || currentId == null) return null
        val index = queue.indexOfFirst { it.id == currentId }
        if (index < 0) return null
        val n = queue.size
        return queue[((index + delta) % n + n) % n]
    }
}
