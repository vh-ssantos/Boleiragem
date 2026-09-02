package com.victorhugo.boleiragem.util

import android.app.Activity

/**
 * Versão "release" - não faz nada. App Distribution é só pra testadores;
 * uma build de Play não deve nem tentar checar isso. Ver a versão em src/debug para a real.
 */
object AppUpdateChecker {
    fun verificarAtualizacao(activity: Activity) {
        // no-op
    }
}
