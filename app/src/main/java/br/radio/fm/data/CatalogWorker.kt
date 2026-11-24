package br.radio.fm.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.radio.fm.RadioApp
import java.util.concurrent.TimeUnit

/** Atualiza o catálogo uma vez por dia, sem precisar publicar nova versão. */
class CatalogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as RadioApp).container
        return if (container.catalog.refresh().isSuccess) Result.success() else Result.retry()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CatalogWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "catalog_refresh", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
