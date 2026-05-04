# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# FitCoach — Platforma dla Trenerów Personalnych

Dwustronna aplikacja Android: panel trenera + aplikacja klienta. Backend na Supabase.
Trener zarządza klientami, planami i nawykami. Klient loguje treningi, jedzenie i nawyki.

## Stos technologiczny

### Android
- **Kotlin 2.x** + Jetpack Compose (Material3)
- **Architektura:** Clean Architecture + MVVM
- **DI:** Hilt (z KSP — nie KAPT)
- **Nawigacja:** Compose Navigation 2.x
- **Sieć:** Supabase Kotlin SDK (`supabase-kt 3.x`) + Retrofit (Open Food Facts)
- **Cache:** Room (offline-first dla treningów)
- **Obrazy:** Coil 2.x
- **Skanowanie:** ML Kit Barcode Scanner
- **Push:** Firebase Cloud Messaging
- **Płatności:** Stripe Android SDK (BLIK przez Stripe)

### Backend
- **Supabase** — PostgreSQL + Auth + Realtime + Storage + Edge Functions
- **Edge Functions:** TypeScript/Deno (webhooki Stripe, logika biznesowa)
- **Płatności:** Stripe
- **Baza żywności:** Open Food Facts API (`https://world.openfoodfacts.org/`)

## Stan projektu

Faza 0 + początek Fazy 1 ukończone. Projekt jest **single-module** (`app/`). Podział na `core-data/core-domain/feature-*` to plan na przyszłość, nie aktualny stan.

### Zaimplementowane features

| Feature | Stan | Opis |
|---------|------|------|
| Auth — login | ✅ | `LoginScreen` + `LoginViewModel` + `LoginUseCase` |
| Auth — rejestracja | ✅ | `RegisterScreen` + `RegisterViewModel` + `RegisterUseCase`; wybór roli trener/klient; pole invite code (zebrane, logika TODO) |
| Dashboard trenera | ✅ | Lista klientów z Supabase, statystyka aktywnych, avatar placeholder, logout, skrót do kodów zaproszenia |
| Dashboard klienta | ✅ | Profil klienta, dzisiejszy trening z planu (3 stany: brak planu / dzień wolny / trening), nawyki z checkboxem i polem ilościowym (debounce 600ms), optimistic updates |
| Splash | ✅ | Sprawdza auth state, przekierowuje do właściwego dashboardu |
| Kody zaproszenia | ✅ | Trener generuje jednorazowe kody (8-znakowy hex) dla klientów; lista z chipami statusów; kopiowanie do schowka; anulowanie z optimistic update |
| Rejestracja z invite code | ✅ | Klient wpisuje kod podczas rejestracji; po rejestracji wywoływana Edge Function `redeem-invite-code`; błąd nie blokuje rejestracji — ostrzeżenie w UI |
| Ekran szczegółów klienta | ✅ | Profil klienta (avatar, cel, status, data dołączenia); klikalna karta nawiguje do planów |
| Plany treningowe (trener) | ✅ | Lista planów klienta (`TrainingPlanListScreen`); kreator planu z dniami i ćwiczeniami (`TrainingPlanCreatorScreen`); `ExercisePickerBottomSheet` z wyszukiwarką i filtrami kategorii; sekwencyjny zapis plan→dni→ćwiczenia przez Supabase |

### Następne do zrobienia (Faza 1)

- Aktywna sesja treningowa (ekran `ActiveWorkout`)
- Nawyki: ekran zarządzania po stronie trenera
- Offline sync (WorkManager)

## Komendy

```bash
# Android build i testy
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test                              # wszystkie unit testy
./gradlew testDebugUnitTest --tests "pl.fitcoach.features.auth.*"  # testy konkretnego pakietu
./gradlew connectedAndroidTest              # testy instrumentowane (wymaga urządzenia)
./gradlew lint

# Supabase (wymaga supabase CLI)
supabase start                    # lokalny stack
supabase db push                  # push migracji do dev
supabase functions serve          # Edge Functions lokalnie
supabase gen types typescript     # generuj typy z schematu DB

# Stripe (wymaga stripe CLI)
stripe listen --forward-to localhost:54321/functions/v1/stripe-webhook
```

## Zmienne środowiskowe

Plik `local.properties` (nie commitować):
```
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
STRIPE_PUBLISHABLE_KEY=pk_live_...
```

Zmienne są wstrzykiwane do `BuildConfig` w `build.gradle.kts` i dostępne przez `BuildConfig.SUPABASE_URL` itp. Debug build ma sufiks `applicationId = pl.fitcoach.debug`.

Plik `google-services.json` w `app/` (nie commitować).

## Struktura pakietów Android

```
app/src/main/java/pl/fitcoach/
├── FitCoachApp.kt          # Application, Hilt
├── MainActivity.kt         # Single activity
├── navigation/
│   ├── NavGraph.kt         # NavHost — Splash, Login, Register, TrainerDashboard, InviteCodes, ClientDetail, TrainingPlanList, CreatePlan, ClientDashboard
│   └── Screen.kt           # Sealed class z routami
├── di/AppModule.kt         # Hilt: AppModule (provides) + RepositoryModule (binds) w jednym pliku
├── core/
│   ├── data/db/            # FitCoachDatabase (Room), aktualnie tylko UserCacheDao
│   ├── service/            # FitCoachMessagingService (FCM)
│   └── ui/theme/           # Color.kt, Theme.kt, Type.kt
└── features/
    ├── auth/               # Login + rejestracja z wyborem roli
    │   ├── data/           # AuthRepositoryImpl (Supabase), UserCacheEntity (Room)
    │   ├── domain/         # AuthRepository, LoginUseCase, RegisterUseCase, AuthState, UserRole
    │   └── ui/             # LoginScreen, LoginViewModel, RegisterScreen, RegisterViewModel
    ├── splash/             # SplashScreen + SplashViewModel — sprawdza auth, przekierowuje
    ├── clients/            # Zarządzanie klientami (trener)
    │   ├── data/           # ClientRepositoryImpl, dto/ClientDto, dto/TrainerProfileDto, dto/InviteCodeDto
    │   ├── domain/         # ClientRepository, model/{Client,TrainerProfile,InviteCode,InviteCodeStatus}
    │   │                   # usecase/{GetClients,GetClientById,GetTrainerProfile,GenerateInviteCode,
    │   │                   #          GetInviteCodes,CancelInviteCode,ValidateInviteCode}UseCase
    │   └── ui/             # InviteCodesScreen+VM, ClientDetailScreen+VM
    ├── dashboard/          # Dashboardy obu ról
    │   ├── data/           # ClientDashboardRepositoryImpl, DTO (plan + nawyki)
    │   ├── domain/         # ClientDashboardRepository, ClientProfile, GetClientDashboardUseCase, LogHabitUseCase
    │   └── ui/             # TrainerDashboardScreen+VM, ClientDashboardScreen+VM
    ├── training/           # Plany treningowe (trener)
    │   ├── data/           # TrainingRepositoryImpl, dto/TrainingDto (ExerciseDto, PlanDto, DayDto, DayExerciseDto + request DTOs)
    │   ├── domain/         # TrainingRepository, model/{Exercise,TrainingPlan,TrainingDay,TrainingDayExercise}
    │   │                   # usecase/{GetExercises,GetTrainingPlans,CreateTrainingPlan,CreateTrainingDay,AddExerciseToDay}UseCase
    │   └── ui/             # TrainingPlanListScreen+VM, TrainingPlanCreatorScreen+VM
    │       └── components/ # ExercisePickerBottomSheet
    ├── habits/             # Model domenowy: Habit, HabitType
    ├── nutrition/          # (planowane)
    ├── progress/           # (planowane)
    ├── messages/           # (planowane)
    └── settings/           # (planowane)
```

## Konwencje kodu

- Pakiety: `pl.fitcoach.features.<feature>.<layer>` (layer = data/domain/ui)
- ViewModele: `<Feature>ViewModel`, stan: `<Feature>UiState` (sealed class lub data class)
- Use Cases: `<Czasownik><Rzeczownik>UseCase` np. `GetClientsUseCase`, `LogWorkoutUseCase`
- Repozytoria: interfejs w `domain`, implementacja w `data`; binding w `RepositoryModule` w `AppModule.kt`
- Wyniki: `Result<T>` w use cases (przez `runCatching`), mapowane na UI state w ViewModelu
- Strings: polskie w `strings.xml`, angielskie w `strings-en/strings.xml`
- Kolory/typografia: tylko przez `MaterialTheme`, nie hardcodować
- Zależności: wersje w `gradle/libs.versions.toml` (Version Catalog)

## Role użytkowników

| Rola | Dostęp |
|------|--------|
| `trainer` | Widzi wszystkich swoich klientów, tworzy plany, ustawia nawyki |
| `client` | Widzi tylko swoje dane, loguje aktywność |

Rola przechowywana w `user_metadata` Supabase Auth jako `{ "role": "trainer" | "client" }`. Odczytywana w `AuthRepositoryImpl.getCurrentUser()`. Po zalogowaniu `SplashViewModel` przekierowuje do odpowiedniego dashboardu.

## Supabase

- Projekt: `fitcoach-prod` (produkcja), `fitcoach-dev` (development)
- Auth: email + hasło + Google OAuth (OAuth planowane w Fazie 3)
- Storage buckety: `avatars` (publiczny), `progress-photos` (prywatny per klient)
- Realtime włączony na: `workout_sessions`, `habit_logs`, `messages`
- RLS: główna warstwa bezpieczeństwa — trener widzi tylko swoich klientów
- Pełny schemat DB i polityki RLS: `docs/database-schema.md`
- Struktura Edge Functions: `docs/architecture.md`

### Edge Functions (supabase/functions/)

| Funkcja | Opis |
|---------|------|
| `redeem-invite-code` | POST — realizuje kod zaproszenia po rejestracji klienta; weryfikuje JWT, wywołuje `redeem_invite_code()` przez service role |

### Migracje (supabase/migrations/)

| Plik | Opis |
|------|------|
| `20260503100000_invite_codes.sql` | Tabela `invite_codes`, enum `invite_code_status`, RLS, funkcje `generate_invite_code` / `redeem_invite_code` / `expire_old_invite_codes`, widok `public_invite_code_lookup` |

## Kluczowe decyzje architektoniczne

1. **Jeden APK, dwie role** — rola po zalogowaniu determinuje widok
2. **Offline-first dla treningów** — Room jako cache, sync gdy internet wraca; `fallbackToDestructiveMigration()` włączone w trakcie dev (zmienić przed launch)
3. **RLS w Supabase** — nie sprawdzamy ról w aplikacji, baza danych egzekwuje uprawnienia
4. **BLIK przez Stripe** — nie integrujemy BLIK bezpośrednio
5. **Open Food Facts** — główna baza produktów (free), custom produkty w Supabase
6. **Paddle/Lemon Squeezy jako MoR** — rozważyć zamiast Stripe jeśli sprzedaż międzynarodowa (obsługuje VAT UE)

## Ważne zasoby

- Supabase Kotlin SDK: https://github.com/supabase-community/supabase-kt
- Material3 komponenty: https://m3.material.io/components
- Open Food Facts API: https://openfoodfacts.github.io/openfoodfacts-server/api/
- Stripe Android: https://stripe.com/docs/payments/accept-a-payment?platform=android
