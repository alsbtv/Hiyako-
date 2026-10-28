# MEMORIA — Hiyako

## Apuntes
- 2026-10-28: App Android (Kotlin/Compose, Hilt, Navigation, Coil, OkHttp+Jsoup). Pantalla de entrada `com.ejemplo.hiyako.presentation.MainActivity` (namespace/applicationId `com.ejemplo.hiyako`, compileSdk 34).
- 2026-10-28: `MainActivity` usa `NavHost` con `startDestination = "splash"` y ruta `"home"`. Flujo: SplashScreen → HomeScreen.
- 2026-10-28: `WitAnimeScraper` usa OkHttp+Jsoup con User-Agent; parsea `.anime-card-container` de witanime.cyou. Modelo `ScrapedAnime(title, url, posterUrl)` en scraper/ScraperModels.kt.
- 2026-10-28: `git push` a origin/main falla sin credenciales ("could not read Username"); conectar cuenta en Hefesto → «Git y GitHub» (o usar gh) para subir.
- 2026-10-28: `.papelera/` está sin trackear a propósito (commit previo "Remove .papelera from tracking"); no añadirla a los commits.
