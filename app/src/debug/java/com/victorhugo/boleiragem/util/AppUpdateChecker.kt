package com.victorhugo.boleiragem.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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

        if (!activity.packageManager.canRequestPackageInstalls()) {
            avisarPermissaoDeInstalacaoFaltando(activity)
            return
        }

        FirebaseAppDistribution.getInstance()
            .updateIfNewReleaseAvailable()
            .addOnFailureListener { e ->
                Log.w("AppUpdateChecker", "Falha ao checar atualização via App Distribution", e)
            }
    }

    /**
     * Sem essa permissão o download da atualização conclui normalmente, mas o Android bloqueia a
     * instalação em silêncio - nenhum erro, o app simplesmente não atualiza. Leva direto para a
     * tela de configuração certa em vez de deixar o usuário sem pista do que fazer.
     */
    private fun avisarPermissaoDeInstalacaoFaltando(activity: Activity) {
        Toast.makeText(
            activity,
            "Permita instalar apps desconhecidos para o Boleiragem se atualizar",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${activity.packageName}")
        )
        runCatching { activity.startActivity(intent) }
            .onFailure { e -> Log.w("AppUpdateChecker", "Não foi possível abrir configurações de instalação", e) }
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
