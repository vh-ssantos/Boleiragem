# AGENTS.md — Boleiragem

Context file for AI coding agents working on this repo. Keep it updated as the project evolves — this is not user-facing documentation (see `README.md` for that).

## What this is

Native Android app (Kotlin + Jetpack Compose) to manage informal soccer games ("peladas"): player registry, team draws (sorteio), scoring, match history/stats, and multiple "grupos" (recurring game groups, e.g. a weekly game at a given court). Portuguese-language app and codebase — UI strings, variable names, and comments are in Portuguese; commit messages too. Not yet published; author intends to eventually ship it to the Play Store.

- Package: `com.victorhugo.boleiragem`
- minSdk 29, targetSdk/compileSdk 36, Kotlin 2.0.21, Java 17
- Architecture: MVVM — `ui/screens/<feature>/XScreen.kt` + `XViewModel.kt`, Hilt DI, Room persistence, `StateFlow` for state
- No automated tests worth relying on — `ExampleUnitTest`/`ExampleInstrumentedTest` are untouched boilerplate. Verification is: compile (`./gradlew compileDebugKotlin`), install on emulator, click through manually.

## Build / run

```
./gradlew compileDebugKotlin   # fast compile check
./gradlew installDebug         # build + install to connected device/emulator (adb devices to check)
```
Windows box, Git Bash available via the Bash tool. `adb` works from PATH.

## Structure

```
app/src/main/java/com/victorhugo/boleiragem/
  data/
    model/        entities (Jogador, GrupoPelada, HistoricoTime, ConfiguracaoSorteio, ConfiguracaoPontuacao, HistoricoPelada, ...)
    dao/           Room DAOs, one per entity
    db/            BoleiragemDatabase (Room), all migrations hand-written inline in the @Database companion object
    repository/    one repo per feature area, injected via Hilt
  domain/          SorteioUseCase — the team-draw algorithm
  di/              Hilt modules (DatabaseModule, ViewModelModule)
  navigation/      NavDestinations (routes), BoleiragemNavHost, BoleiragemBottomNavigationBar
  ui/screens/<feature>/   one folder per feature: cadastro, configuracao, sorteio, times, historico,
                          estatisticas, grupos, compartilhar, login, splash
  ui/theme/        Compose theme/colors/typography
  ui/common/       shared small composables (text fields, dimensions)
  util/            LocationPermissionManager etc.
MainActivity.kt    entry point — see "Navigation is unusual" below
```

## Navigation is unusual — read before touching MainActivity.kt

There are **two navigation systems layered on top of each other**, and this has already caused a real bug (see Known-fixed issues below):

1. `BoleiragemNavHost.kt` / `NavDestinations` — a full Jetpack Navigation Compose graph (splash → login → tabs → `DetalheJogador`/`ResultadoSorteio`/`GerenciadorPerfis`/`Estatisticas` routes) that is **dead code**: `BoleiragemNavHost(...)` is never called from anywhere in the app (verified by grep — only its own declaration references it). Don't assume editing it affects the running app.
2. `MainActivity.kt` — `BoleiragemApp()` is a **hand-rolled state machine** (`showSplashScreen`, `showLoginScreen`, `showGruposScreen`, `showResultadoSorteioRapido`, booleans checked in a `when`) that decides which top-level screen is shown. Inside `MainScreen()`, the 5 main tabs (Jogadores / Regras / Sorteio / Jogo / Estatísticas) are pages of a `HorizontalPager`, manually wired with a `when(page)` — **this is not driven by `BoleiragemNavHost` at all**, even though `NavDestinations` routes exist for these same screens and `BoleiragemNavHost` has empty composable stubs for them (`/* Conteúdo gerenciado pelo ViewPager */`).
3. Secondary/detail screens reached from inside a tab (player detail, scoring config, profile manager) are shown via a third mechanism: `isSecondaryScreen: Boolean` + `secondaryScreenContent: @Composable () -> Unit`, both state hoisted in `MainScreen()`. Not a back stack — going "back" just flips the boolean.

**Net effect:** three coexisting navigation mechanisms (NavHost, hand-rolled `when` state machine, single mutable "secondary screen" slot) for one app. When you add a new top-level tab or a new secondary/detail screen, wire it through the *same* pattern as its neighbors — don't introduce a fourth mechanism. If you have the appetite to collapse these into one real Navigation Compose graph, that is a legitimate improvement to propose to the user, but it's a deliberate refactor, not a drive-by fix.

## Room database

- Single `BoleiragemDatabase`, currently at **version 13**, `exportSchema = false` (so there's no schema JSON history to diff against — be extra careful with migrations).
- Every version bump has a hand-written `Migration(n, n+1)` in `BoleiragemDatabase.kt`, registered in `DatabaseModule.kt`'s `.addMigrations(...)`. **No `fallbackToDestructiveMigration()`** — good for not silently wiping user data, but it means a missing/buggy migration crashes the app on open instead of degrading gracefully. If you change any `@Entity`, you must add a corresponding migration and register it, or the app will crash for existing installs.
- There's a loose `migration_jogadores.sql` at the repo root — appears to be a manual reference/scratch script, not wired into Room's migration system. Don't assume it's applied anywhere; check before relying on it.
- `grupoId` was retrofitted onto several tables in later migrations (multi-group support was added after the fact) — most screen composables take a `grupoId: Long = -1L` parameter and call `viewModel.setGrupoId(grupoId)` in a `LaunchedEffect`. Follow this pattern for any new per-group screen/viewmodel.

## Known-fixed issues (context for "why does this code look like that")

- **2026-09-01**: User reported "app opens but the content area is blank — can't create a group, nothing loads." Root cause: `MainActivity.kt`'s `HorizontalPager` content lambda was **empty** (just a comment: `// O conteúdo das suas abas/páginas vai aqui...`) — none of the 5 tab screens were ever wired in, so every tab rendered nothing. Also `pageCount` was hardcoded to `6` while there are only 5 tabs/bottom-nav items. Fixed by wiring `CadastroJogadoresScreen` / `ConfiguracaoTimesScreen` / `SorteioTimesScreen` / `TimesAtuaisScreen` / `HistoricoScreen` into the `when(page)` block (using the existing `isSecondaryScreen`/`secondaryScreenContent` slot for drill-down screens like `DetalheJogadorScreen`, `ConfiguracaoPontuacaoScreen`, `GerenciadorPerfisScreen`), and correcting `pageCount` to `5`. This was **not** a "stale install" or migration bug — it reproduced on a fresh install too, so don't assume future blank-screen reports are the same root cause without checking.
- Removed `data/repository/SorteioRepository.kt.new` — an orphaned, out-of-date scratch copy of `SorteioRepository.kt` (missing later fields like `temPeladaAtiva`/`ehTimeReserva` handling) that wasn't referenced by any build target. If you see other `*.kt.new`/`*.bak`-style files appear, treat them the same way: diff against the real file, confirm nothing references it, then remove.

## Authentication (Firebase Auth, added 2026-09-01)

Real email/password auth was added to replace the previously fake `LoginScreen` (buttons that just flipped local booleans, no actual account).

- `data/repository/AuthRepository.kt` wraps `FirebaseAuth`: `entrarComEmailSenha`, `cadastrarComEmailSenha`, `enviarEmailRedefinicaoSenha`, `sair()`, plus `usuarioAtual` / `estadoAutenticacao` (a `Flow<FirebaseUser?>`, not yet consumed anywhere — currently only `usuarioAtual` is read synchronously).
- `di/FirebaseModule.kt` provides the `FirebaseAuth` singleton.
- `ui/screens/login/LoginViewModel.kt` (Hilt) holds `LoginUiState` (email, senha, modo LOGIN/CADASTRO, carregando, erro, mensagemInfo, autenticado) and drives `LoginScreen.kt`, which now has real email/password fields, a login↔cadastro toggle, "esqueci minha senha", and loading/error states — replacing the old visual-only buttons.
- **Wiring lives in `MainActivity.kt`'s `BoleiragemApp()`**, not in the dead `BoleiragemNavHost`: after the splash screen, it checks `FirebaseAuth.getInstance().currentUser` directly (no viewmodel — plain SDK singleton call) to skip straight to `showGruposScreen` if a session already exists; otherwise shows `LoginScreen`. `onSairClick` (in the grupos screen's app bar) now calls `FirebaseAuth.getInstance().signOut()` for real, not just a local state flip.
- "Entrar sem conta" (guest mode) was **kept** — it bypasses Firebase entirely and drops the user straight into `showGruposScreen`, same as before. Nothing currently gates any screen behind being authenticated; guest and authenticated users see the same app.
- **Firebase Console setup required, not doable from code**: the Email/Password sign-in provider must be enabled in the `boleiragem-964c9` Firebase project (Authentication → Sign-in method) or every sign-in/sign-up call fails at runtime with an auth-configuration error.
- **Google Sign-In** (added same day) uses the modern **Credential Manager API** (`androidx.credentials` + `com.google.android.libraries.identity.googleid`), not the deprecated `GoogleSignInClient`/`GoogleSignInOptions`. Flow: `LoginScreen.iniciarLoginComGoogle()` calls `CredentialManager.getCredential(...)` with a `GetGoogleIdOption` built from `context.getString(R.string.default_web_client_id)` (that string resource is auto-generated by the Google Services Gradle plugin from the `oauth_client` entry of type 3 — "web client" — in `google-services.json`; don't hardcode the client ID). The resulting `GoogleIdTokenCredential`'s `idToken` is passed to `LoginViewModel.entrarComGoogle(idToken)` → `AuthRepository.entrarComGoogle` → `GoogleAuthProvider.getCredential(idToken, null)` → `firebaseAuth.signInWithCredential(...)`.
  - Requires two things done once in the Firebase Console (already done for this project, 2026-09-01): the Google provider enabled under Authentication → Sign-in method (with a public-facing project name + support email), and the app's **debug SHA-1** fingerprint registered under Project Settings → Your apps → Android app (get it any time via `./gradlew signingReport`). Registering the SHA-1 is what makes Firebase populate the `oauth_client` (type 1, Android + type 3, Web) entries in `google-services.json` — the file must be re-downloaded and dropped into `app/google-services.json` after adding a new SHA-1, or the Web client id resource won't exist and the build/sign-in will fail.
  - A **release** build will need its own SHA-1 (the release/Play App Signing key) registered the same way before Google Sign-In works in that build variant — the debug SHA-1 only covers local debug builds on this machine.
  - Testing on an emulator requires a system image **with Google Play Store** (not just "Google APIs") *and* a Google account actually signed in on the device (Settings → Accounts, or `adb shell am start -a android.settings.ADD_ACCOUNT_SETTINGS`) — check `adb shell dumpsys account` for `Accounts: 0` as the tell. Without a signed-in account, Credential Manager throws (surfaces to the user as "GetCredentialResponse error returned from framework" in logcat / "Não foi possível entrar com Google" in the UI) even on a Play Store image. This bit the user on first test; solved by adding a Google account to the existing AVD, no new AVD needed.
- Google button styling: `LoginScreen.kt`'s Google button follows Google's official "Sign in with Google" branding (white background, `#DADCE0` border, `#1F1F1F` text, `res/drawable/ic_google_logo.xml` multi-color "G" — a plain vector, not a themed icon, so it doesn't follow the app's dark/light theme colors on purpose). If asked to reskin auth buttons, leave this one alone — it's a Google brand requirement, not an app style choice.
- **Not yet done — this is phase 1 of a larger plan**: the user's actual goal is for every `Jogador` to correspond to a real user account (currently `Jogador` is just a freestanding entity with no link to `AuthRepository`/`FirebaseUser`, and avulso/free-standing player creation is explicitly meant to keep working alongside it). That linkage (e.g. a `uid`/`usuarioId` field on `Jogador`, a Room migration to v14, a "claim this player" or auto-create-player-on-signup flow) has not been started. Don't assume it exists.

## No cloud sync yet — everything is single-device local storage (confirmed 2026-09-01)

The user's stated end goal is a shared multiplayer experience: players on different phones seeing the same grupo, jogadores, sorteios, etc. **This does not exist today**, confirmed by grepping the whole codebase for Firestore/Realtime Database/any remote API — zero hits. Concretely:

- `BoleiragemDatabase` is Room-only (local SQLite file `boleiragem_database`, one copy per device install). Firebase Auth (added today) authenticates a *user*, but nothing in `data/repository/` reads or writes that identity into any shared backend — Auth and data storage are currently two unconnected systems.
- `GrupoPelada` already has `usuarioId: String = "local"` and `compartilhado: Boolean = false` fields (see `data/model/GrupoPelada.kt`), with a code comment dating from before this session — *"Dados para controle de sessão (quando implementarmos login real)"* — i.e. someone anticipated this need but never wired it up. `usuarioId` is never set to the Firebase UID anywhere; it's always `"local"`.
- `CompartilharPeladaScreen` / `GrupoPelada.getTextoCompartilhamento()` is **not sync** — it just formats grupo details as a text blob for Android's share sheet (e.g. to paste into WhatsApp). No relation to real-time or persisted sharing between accounts.
- **Implication for any future work**: don't assume `usuarioId`, `compartilhado`, or the Auth UID currently gate or scope any data access — every `Jogador`/`GrupoPelada`/etc. query today is scoped only by local Room `grupoId`, visible to whoever has that local install.

## Cloud sync plan (Firestore) — agreed design, 2026-09-01

Aligned with the user before writing any code. Backend: **Firestore** (Firebase project already has Auth wired up). Phased rollout, each phase is a separate confirmed unit of work:

- **Fase 1 (shipped 2026-09-01, see below)**: grupo-level infrastructure — create grupo in Firestore, list "my groups" synced across devices, join-by-invite-code, membership roles.
- **Fase 2 (not started, needs its own go-ahead)**: sync the data *inside* a grupo — jogadores, sorteios, histórico, configurações. Today these stay Room-only even after Fase 1. This is why a joined grupo currently shows as a read-only stub (see below) instead of real content.
- **Fase 3 (later)**: original goal of tying `Jogador` records to real user accounts (see the auth section above).

**Roles within a grupo** (not the earlier binary owner/viewer — refined after more discussion):
- **Dono** (creator, `donoId` = Firebase UID): full control — edit grupo settings/regras, sorteio, jogadores (cadastro/convidado/avulso), and can promote other members to editor.
- **Editor** (member the dono explicitly designated): same edit rights as dono within the grupo (regras, sorteio, jogadores), *cannot* change who else is editor/remove the dono.
- **Membro** (default for anyone who joins via invite): read-only — sees jogadores, histórico, estatísticas, everything — but can't edit.
- Grupo-level toggle **`permiteConviteDeMembros`**: if true, any membro (not just dono/editor) can generate/reshare the invite; if false, only dono/editores can.

**Invite flow**: a short invite **code** (6 alphanumeric chars, excludes O/0/I/1) is always generated per grupo and is the reliable join path (type code → shown a "Fulano te convidou para Pelada X, aceitar?" confirmation screen → join as membro). A `boleiragem://convite/{codigo}` custom-scheme deep link (opens the app straight to that confirmation screen **only if the app is already installed**, no Play Store fallback) is still just a planned convenience, **not implemented** — no deep link handling exists in the manifest/nav yet, only the manual code-entry dialog. Don't build toward Firebase Dynamic Links (shut down by Google in 2025).

### Fase 1 implementation (shipped 2026-09-01)

New files: `data/model/GrupoRemoto.kt` (Firestore doc model + `PapelGrupo` enum + `papelDe`/`podeEditar`/`podeConvidar` helpers), `data/model/UsuarioPerfil.kt`, `data/repository/GrupoRemotoRepository.kt` (Firestore CRUD: `criarGrupoCompartilhado`, `observarMeusGrupos` via snapshot listener, `buscarPorCodigo`/`buscarPorId`, `entrarNoGrupo`, `sairDoGrupo`, `promoverParaEditor`/`removerEditor`, `atualizarPermiteConviteDeMembros`), `data/repository/UsuarioRepository.kt` (upserts `usuarios/{uid}` on every load of `GruposPeladaScreen` so member lists can show names instead of raw UIDs), `firestore.rules` (repo-root reference file — **not deployed**, no Firebase CLI on this machine; must be pasted manually into Firebase Console → Firestore → Regras, replacing the 30-day test-mode rules the project currently runs on).

Data model bridge: `GrupoPelada` (Room) gained a nullable `firestoreId: String?` column (Room migration `MIGRATION_13_14`, DB version 13→14). A local grupo starts with `firestoreId = null` (purely local, exactly like before this feature). The **first time** the dono taps "Convidar" on it, `GruposPeladaViewModel.abrirConvite` lazily calls `criarGrupoCompartilhado` and writes the returned Firestore doc id back onto the local Room row — "sharing" is opt-in per grupo, not automatic, and nothing about existing local grupos changes until that happens.

**Who sees what, concretely**:
- The dono's own grupos still come from Room (`_grupos` in the viewmodel, unchanged code path) — always rendered as full, editable grupos exactly as before, whether or not they've been shared.
- Grupos where the signed-in user is *membro or editor* (i.e. grupos created by someone else that they joined via code) come from a **separate** Firestore-only list, `gruposComoConvidado` (filter: `membrosIds array-contains uid && donoId != uid`). These render in a new horizontally-scrolling "Grupos que você participa" section above the main grid in `GruposPeladaScreen`, as read-only stub cards (name, owner, role chip, "sincronização completa chega em breve") — tapping does **not** navigate into the tab UI, because there is no local Room grupoId for them and Fase 2 (jogadores/sorteio/histórico sync) hasn't shipped. Don't wire that tap to `MainScreen`'s pager until Fase 2 exists — it would show an empty/broken group.
- This split (Room = "grupos I own", Firestore-only = "grupos I joined") is why there's no unified list type — it was a deliberate scope cut to avoid touching every consumer of `List<GrupoPelada>`. If Fase 2 changes this (e.g. gives joined grupos a local Room mirror), this whole section needs re-reading, not just patching.

**UI added**: `SecaoGruposConvidado` (the stub-card section above), `DialogoEntrarComCodigo` (code entry → fetch → "Fulano te convidou..." confirm → `entrarNoGrupo`), `DialogoConvite` (shows the code, a "Compartilhar convite" button reusing the existing `compartilharTexto`/Android share-sheet plumbing, the dono-only `permiteConviteDeMembros` switch, and a "Gerenciar membros" entry point), `DialogoGerenciarMembros` (dono-only: promote/demote editor, remove member — reads names via `UsuarioRepository.buscarPerfis`). A `GroupAdd` FAB ("Entrar em um grupo com código") was added to `GruposPeladaScreen`. "Convidar" was added to the existing dropdown menu on every grupo card across all three view modes (Lista/Cards/Minimalista) via a new `onConvidarClick` param threaded through `ListaVisualizacao`/`CardsVisualizacao`/`MinimalistaVisualizacao`/`GrupoItem*`.

**Verified working** (2026-09-01, one device/one account): compiled clean, Room migration 13→14 applied without crash on an existing install, tapped "Convidar" on an existing local grupo → Firestore doc created live (confirmed via a real invite code rendering, e.g. `UYAGXY`) → no crash, app process stayed alive. **Not yet verified**: the actual join-by-code flow end-to-end (needs a second account/device — one user's `EntrarComCodigo` flow finding and joining a grupo created by another), promote/demote editor, `permiteConviteDeMembros` gating. Don't assume those are confirmed working — an APK was handed to a second person specifically to test this, but the result hadn't come back as of this writing.

`firestore.rules` **is deployed** — the user pasted it into the Firebase Console (first version 2026-09-01, then again same day after a fix: the original `estaSaindoDoGrupo` function only checked that the caller's own uid was absent from the post-write `membrosIds`, which let any authenticated member remove a *different* member by writing a shorter array that merely didn't include themselves. Tightened to also require the array shrank by exactly 1, the caller was previously in it, and `editoresIds`/`donoId`/`nome`/`codigoConvite`/`permiteConviteDeMembros` are unchanged by that write). If `firestore.rules` in the repo is ever edited again, remind the user to re-paste it — there is no CI/CD deploying it automatically (no Firebase CLI on this machine, see the Fase 1 section above).

**Still open / explicitly deferred**: no auto-migration of pre-existing local grupos to Firestore (that idea from the original scoping discussion was dropped in favor of the simpler "share on demand" lazy-create above); no local Room mirror created for a joined grupo (so a membro/editor's device can't yet do anything with the grupo beyond seeing it exists — that's Fase 2); custom-scheme deep link not implemented.

## Conventions worth following

- Portuguese for all user-facing strings, entity/field names, and commit message *descriptions*.
- Screens default their Hilt viewmodel via `viewModel: XViewModel = hiltViewModel()` as the first parameter, `grupoId` and navigation callbacks after.
- Firebase (BOM + Analytics + Auth + Firestore) and Google Maps/Places SDKs are integrated (`google-services.json` present) but Maps/Places aren't deeply used yet — check current call sites before assuming they're load-bearing anywhere.

## Commit convention

As of 2026-09-01 this repo follows **Conventional Commits** (`<type>: <descrição em português>`, imperative, lowercase type, no period at the end). Earlier history predates this (used freeform prefixes like `change:`, `fix:`) — don't imitate those older commits, follow the types below going forward:

- `feat:` — new user-facing functionality (e.g. `feat: adiciona login com Google`)
- `fix:` — bug fix
- `chore:` — no production code change: deps/build config, generated files, housekeeping
- `refactor:` — code restructuring with no behavior change
- `docs:` — documentation only (README, AGENTS.md, comments)
- `style:` — formatting/whitespace only, no logic change
- `test:` — adding or fixing tests
- `perf:` — performance improvement

Scope suffix is optional (`feat(auth): ...`). Body (if any) explains *why*, same as the general project instructions. Squash-worthy WIP commits should still be avoided — commit meaningful, buildable units of work.
