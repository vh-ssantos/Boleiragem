package com.victorhugo.boleiragem.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.appdistribution.FirebaseAppDistribution

/**
 * Checagem de atualização in-app via Firebase App Distribution.
 * Só existe no source set "debug" - a versão em "release" é um no-op, porque App Distribution
 * é exclusivo pra builds de teste (falha silenciosamente em builds de Play de qualquer forma).
 */
object AppUpdateChecker {
    fun verificarAtualizacao(activity: Activity) {
        solicitarPermissaoNotificacaoSeNecessario(activity)

        FirebaseAppDistribution.getInstance()
            .updateIfNewReleaseAvailable()
            .addOnFailureListener { e ->
                Log.w("AppUpdateChecker", "Falha ao checar atualização via App Distribution", e)
            }
    }

    private fun solicitarPermissaoNotificacaoSeNecessario(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val jaConcedida = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!jaConcedida) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }
}
