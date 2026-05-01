# FitCoach — Architektura techniczna

## Przegląd systemu

```
┌─────────────────────────────────────────────────────┐
│                  ANDROID APP                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │ Trener   │  │ Klient   │  │  Shared Features │  │
│  │ Dashboard│  │ Dashboard│  │  (auth, settings) │  │
│  └────┬─────┘  └────┬─────┘  └────────┬─────────┘  │
│       │              │                 │             │
│  ┌────▼──────────────▼─────────────────▼──────────┐ │
│  │              Domain Layer                       │ │
│  │  Use Cases: GetClients, LogWorkout, TrackHabit  │ │
│  └────────────────────┬────────────────────────────┘ │
│  ┌─────────────────────▼──────────────────────────┐  │
│  │              Data Layer                        │  │
│  │   SupabaseDataSource  │  RoomLocalDataSource   │  │
│  └─────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
             │                    │
     ┌───────▼──────┐    ┌────────▼────────┐
     │   SUPABASE   │    │  ZEWNĘTRZNE API  │
     │ PostgreSQL   │    │ Open Food Facts  │
     │ Auth         │    │ Stripe           │
     │ Realtime     │    │ FCM              │
     │ Storage      │    └─────────────────┘
     │ Edge Funcs   │
     └──────────────┘
```

## Clean Architecture — warstwy

### Presentation Layer (`features/<feature>/ui/`)
- **Compose UI** — ekrany, komponenty
- **ViewModel** — stan UI (`UiState`), obsługa eventów
- **Nawigacja** — `NavController`, `NavGraph`

```kotlin
// Przykład struktury
data class ClientsUiState(
    val clients: List<Client> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val getClientsUseCase: GetClientsUseCase
) : ViewModel() {
    val uiState: StateFlow<ClientsUiState> = ...
}
```

### Domain Layer (`core/domain/`)
- **Encje** — `Client`, `WorkoutPlan`, `Habit`, `FoodEntry`
- **Use Cases** — logika biznesowa, niezależna od Android i Supabase
- **Interfejsy repozytoriów** — kontrakt między domain a data

```kotlin
class LogWorkoutSessionUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(session: WorkoutSession): Result<Unit>
}
```

### Data Layer (`core/data/`)
- **Implementacje repozytoriów** — orkiestracja Supabase + Room
- **DTO** — modele danych Supabase (JSON)
- **Entity** — modele Room (SQLite)
- **Mapery** — DTO ↔ Domain ↔ Entity

```kotlin
class WorkoutRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val workoutDao: WorkoutDao
) : WorkoutRepository {
    override suspend fun logSession(session: WorkoutSession): Result<Unit> {
        // 1. Zapisz lokalnie w Room (offline-first)
        // 2. Spróbuj wysłać do Supabase
        // 3. Jeśli brak internetu — zakolejkuj do sync
    }
}
```

## Moduły Gradle

```
app/
├── build.gradle.kts          # Główny app module
core/
├── core-data/build.gradle.kts
├── core-domain/build.gradle.kts
└── core-ui/build.gradle.kts
features/
├── feature-auth/
├── feature-training/
├── feature-nutrition/
├── feature-habits/
└── feature-progress/
```

Na start można zacząć od jednego modułu `app` i wydzielać później.

## Zależności (Gradle Version Catalog)

```toml
# gradle/libs.versions.toml

[versions]
kotlin = "2.0.21"
compose-bom = "2024.12.01"
hilt = "2.52"
room = "2.6.1"
supabase = "3.0.2"
retrofit = "2.11.0"
coil = "2.7.0"
stripe = "21.0.0"
mlkit-barcode = "17.3.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-navigation = { group = "androidx.navigation", name = "navigation-compose", version = "2.8.5" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Supabase
supabase-postgrest = { group = "io.github.jan-tennert.supabase", name = "postgrest-kt", version.ref = "supabase" }
supabase-auth = { group = "io.github.jan-tennert.supabase", name = "auth-kt", version.ref = "supabase" }
supabase-realtime = { group = "io.github.jan-tennert.supabase", name = "realtime-kt", version.ref = "supabase" }
supabase-storage = { group = "io.github.jan-tennert.supabase", name = "storage-kt", version.ref = "supabase" }

# Sieć
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }

# Obrazy
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# Stripe
stripe-android = { group = "com.stripe", name = "stripe-android", version.ref = "stripe" }

# ML Kit
mlkit-barcode = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "mlkit-barcode" }

# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version = "33.7.0" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }

# Testy
junit5 = { group = "org.junit.jupiter", name = "junit-jupiter", version = "5.11.3" }
mockk = { group = "io.mockk", name = "mockk", version = "1.13.13" }
compose-test = { group = "androidx.compose.ui", name = "ui-test-junit4" }
```

## Supabase — schemat bazy danych

Pełny schemat w `docs/database-schema.md`. Skrócona mapa relacji:

```
users (Supabase Auth)
  └── trainer_profiles (1:1)
  └── client_profiles (1:1)
        └── trainer_id → trainer_profiles

training_plans
  ├── trainer_id → trainer_profiles
  ├── client_id → client_profiles
  └── training_days
        └── training_day_exercises
              └── exercise_id → exercises

workout_sessions
  ├── client_id → client_profiles
  ├── plan_id → training_plans
  └── session_sets

habits
  ├── trainer_id → trainer_profiles
  └── client_id → client_profiles
        └── habit_logs

food_entries
  ├── client_id → client_profiles
  └── food_product_id → food_products

body_measurements → client_id
progress_photos → client_id
messages → sender_id, receiver_id
subscriptions → trainer_id
```

## Nawigacja

```kotlin
// NavGraph — dwie niezależne gałęzie po roli
sealed class Screen(val route: String) {
    // Auth
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Trainer
    data object TrainerDashboard : Screen("trainer/dashboard")
    data object ClientDetail : Screen("trainer/client/{clientId}")
    data object CreatePlan : Screen("trainer/plan/create")

    // Client
    data object ClientDashboard : Screen("client/dashboard")
    data object ActiveWorkout : Screen("client/workout/{sessionId}")
    data object FoodLog : Screen("client/food")
    data object Progress : Screen("client/progress")
}
```

## Offline-first flow

```
[Klient loguje serię]
       │
       ▼
[Room.insertSet()] ← natychmiastowy zapis lokalny
       │
       ▼
[SyncManager próbuje Supabase]
       │
    ┌──┴──┐
    │     │
    ▼     ▼
  OK    Brak internetu
    │     │
    │     └→ [Zakolejkuj w WorkManager]
    │              │
    └──────────────┘
           │
           ▼
[Supabase.upsert() gdy internet wraca]
```

## Realtime — aktualizacje trenera

```kotlin
// Trener subskrybuje zmiany klienta
supabaseClient.realtime.channel("client-${clientId}")
    .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
        table = "workout_sessions"
        filter = "client_id=eq.${clientId}"
    }
    .onEach { change -> 
        // Aktualizuj UI trenera na żywo
    }
    .launchIn(viewModelScope)
```

## Bezpieczeństwo — RLS przykłady

```sql
-- Trener widzi tylko swoich klientów
CREATE POLICY "trainer_sees_own_clients"
ON client_profiles FOR SELECT
USING (
    trainer_id = (
        SELECT id FROM trainer_profiles 
        WHERE user_id = auth.uid()
    )
);

-- Klient widzi tylko swoje sesje
CREATE POLICY "client_sees_own_sessions"
ON workout_sessions FOR ALL
USING (client_id = (
    SELECT id FROM client_profiles WHERE user_id = auth.uid()
));
```

## Struktura Edge Functions

```
supabase/functions/
├── stripe-webhook/          # Obsługa eventów Stripe
│   └── index.ts
├── create-subscription/     # Tworzenie Stripe Subscription
│   └── index.ts
├── send-notification/       # FCM push przez service account
│   └── index.ts
└── generate-invite-code/    # Unikalny kod zaproszenia
    └── index.ts
```
