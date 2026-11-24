package br.radio.fm.player

object PlaybackContract {
    const val USER_AGENT = "RadioBrasilFM/0.2 (Android)"
    const val KEY_STATUS = "status"
    const val STATUS_OK = "ok"
    const val STATUS_RECONNECTING = "reconnecting"
    const val STATUS_FAILED = "failed"
    const val MAX_RETRIES = 6
}
