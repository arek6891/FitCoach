# FitCoach — Platforma dla Trenerów Personalnych

Dwustronna aplikacja Android: panel trenera + aplikacja klienta. Backend na Supabase.
Trener zarządza klientami, planami i nawykami. Klient loguje treningi, jedzenie i nawyki.

## Stos technologiczny

### Android
- **Kotlin 2.x** + Jetpack Compose (Material3)
- **Architektura:** Clean Architecture + MVVM
- **DI:** Hilt
- **Nawigacja:** Compose Navigation 2.x
- **Sieć:** Supabase Kotlin SDK + Retrofit (Open Food Facts)
- **Cache:** Room (offline-first dla treningów)
- **Obrazy:** Coil 2.x
- **Skanowanie:** ML Kit Barcode Scanner
- **Push:** Firebase Cloud Messaging
- **Płatności:** Stripe Android SDK (BLIK przez Stripe)

### Backend
- **Supabase** — PostgreSQL + Auth + Realtime + Storage + Edge Functions
- **Edge Functions:** TypeScript/Deno (webhooki Stripe, logika biznesowa)
- **Płatności:** Stripe
- **Baza żywności:** Open Food Facts API (bezpłatne, pokrywa PL)

## Struktura pakietów Android

```
app/src/main/java/pl/fitcoach/
├── FitCoachApp.kt          # Application, Hilt
├── MainActivity.kt         # Single activity
├── di/AppModule.kt         # Hilt moduły
├── core/
│   ├── data/               # Implementacje repozytoriów, DTO, Room, Supabase
│   ├── domain/             # Encje, use cases, interfejsy repozytoriów
│   └── ui/                 # Wspólne komponenty, temat, nawigacja
└── features/
    ├── auth/               # Logowanie, rejestracja, wybór roli
    ├── dashboard/          # Ekran główny (różny dla roli)
    ├── clients/            # Zarządzanie klientami (tylko trener)
    ├── training/           # Plany treningowe i sesje
    ├── nutrition/          # Dziennik żywienia + baza produktów
    ├── habits/             # Nawyki (ustawiane przez trenera)
    ├── progress/           # Pomiary, zdjęcia, wykresy
    ├── messages/           # Czat trener-klient
    └── settings/           # Ustawienia, subskrypcja Stripe
```

## Konwencje kodu

- Pakiety: `pl.fitcoach.features.<feature>.<layer>` (layer = data/domain/ui)
- ViewModele: `<Feature>ViewModel`, stan: `<Feature>UiState` (sealed class lub data class)
- Use Cases: `<Czasownik><Rzeczownik>UseCase` np. `GetClientsUseCase`, `LogWorkoutUseCase`
- Repozytoria: interfejs w `domain`, implementacja w `data`
- Wyniki: `Result<T>` w use cases, mapowane na UI state w ViewModelu
- Strings: polskie w `strings.xml`, angielskie w `strings-en/strings.xml`
- Kolory/typografia: tylko przez `MaterialTheme`, nie hardcodować

## Role użytkowników

| Rola | Dostęp |
|------|--------|
| `trainer` | Widzi wszystkich swoich klientów, tworzy plany, ustawia nawyki |
| `client` | Widzi tylko swoje dane, loguje aktywność |

Rola przechowywana w `user_metadata` Supabase Auth. Po zalogowaniu app przekierowuje do odpowiedniego dashboardu.

## Zmienne środowiskowe

Plik `local.properties` (nie commitować):
```
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
STRIPE_PUBLISHABLE_KEY=pk_live_...
```

Plik `google-services.json` w `app/` (nie commitować).

## Supabase

- Projekt: `fitcoach-prod` (produkcja), `fitcoach-dev` (development)
- Auth: email + hasło + Google OAuth
- Storage buckety: `avatars` (publiczny), `progress-photos` (prywatny per klient)
- Realtime włączony na: `workout_sessions`, `habit_logs`, `messages`
- RLS: główna warstwa bezpieczeństwa — trener widzi tylko swoich klientów

## Kluczowe decyzje architektoniczne

1. **Jeden APK, dwie role** — rola po zalogowaniu determinuje widok
2. **Offline-first dla treningów** — Room jako cache, sync gdy internet wraca
3. **RLS w Supabase** — nie sprawdzamy ról w aplikacji, baza danych egzekwuje uprawnienia
4. **BLIK przez Stripe** — nie integrujemy BLIK bezpośrednio
5. **Open Food Facts** — główna baza produktów (free), custom produkty w Supabase
6. **Paddle/Lemon Squeezy jako MoR** — rozważyć zamiast Stripe jeśli sprzedaż międzynarodowa (obsługuje VAT UE)

## Komendy

```bash
# Android build
./gradlew assembleDebug
./gradlew test
./gradlew lint

# Supabase (wymaga supabase CLI)
supabase start                    # lokalny stack
supabase db push                  # push migracji do dev
supabase functions serve          # Edge Functions lokalnie
supabase gen types typescript     # generuj typy z schematu DB

# Stripe (wymaga stripe CLI)
stripe listen --forward-to localhost:54321/functions/v1/stripe-webhook
```

## Ważne zasoby

- Supabase Kotlin SDK: https://github.com/supabase-community/supabase-kt
- Material3 komponenty: https://m3.material.io/components
- Open Food Facts API: https://openfoodfacts.github.io/openfoodfacts-server/api/
- Stripe Android: https://stripe.com/docs/payments/accept-a-payment?platform=android
- PolishAPI (open banking, na przyszłość): https://polishapi.org
