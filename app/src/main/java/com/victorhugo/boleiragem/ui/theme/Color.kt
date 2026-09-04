package com.victorhugo.boleiragem.ui.theme

import androidx.compose.ui.graphics.Color

// Cores para o tema escuro
val GreenDark = Color(0xFF1E8E3E)
val GreenDarkVariant = Color(0xFF34A853)
val OrangeDark = Color(0xFFF29900)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)

// Cores para o tema claro
val GreenLight = Color(0xFF1E8E3E)  // Verde gramado
val GreenLightVariant = Color(0xFF34A853)
val OrangeLight = Color(0xFFF29900)  // Laranja bola
val BackgroundLight = Color(0xFFF5F5F5)  // Cinza claro em vez de branco
val SurfaceLight = Color(0xFFE8E8E8)    // Cinza mais claro para superfícies

// Tons "container" e afins (redesign, 1ª onda) — antes ausentes do ColorScheme, então telas que já
// liam MaterialTheme.colorScheme.primaryContainer/surfaceVariant/error caíam no auto-derivado do
// Material3 em vez de uma cor curada e coerente com a paleta verde/laranja do app.
val PrimaryContainerDark = Color(0xFF15391F)
val OnPrimaryContainerDark = Color(0xFFA6F1B1)
val SecondaryContainerDark = Color(0xFF1D4A2E)
val OnSecondaryContainerDark = Color(0xFFAEEFC0)
val TertiaryContainerDark = Color(0xFF5C3F00)
val OnTertiaryContainerDark = Color(0xFFFFDDA6)
val SurfaceVariantDark = Color(0xFF2A2A2A)
val OnSurfaceVariantDark = Color(0xFFC7C7C7)
val OutlineDark = Color(0xFF8A8A8A)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val PrimaryContainerLight = Color(0xFFC8F5CE)
val OnPrimaryContainerLight = Color(0xFF00210B)
val SecondaryContainerLight = Color(0xFFD3F2D8)
val OnSecondaryContainerLight = Color(0xFF042109)
val TertiaryContainerLight = Color(0xFFFFE0B3)
val OnTertiaryContainerLight = Color(0xFF2A1800)
val SurfaceVariantLight = Color(0xFFDFE4DB)
val OnSurfaceVariantLight = Color(0xFF43483F)
val OutlineLight = Color(0xFF73796E)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
