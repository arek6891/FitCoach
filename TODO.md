# FitCoach — Lista zadań

Status: [ ] todo | [~] in progress | [x] done | [!] zablokowane

---

## FAZA 0 — Setup projektu (Tydzień 1–2)

### Środowisko
- [ ] Zainstalować Android Studio (najnowsze stable)
- [ ] Zainstalować Supabase CLI (`npm install -g supabase`)
- [ ] Zainstalować Stripe CLI
- [ ] Założyć konto Supabase i stworzyć projekt `fitcoach-dev`
- [ ] Założyć konto Stripe (tryb testowy)
- [ ] Założyć konto Firebase (FCM do powiadomień)

### Android projekt
- [ ] Stworzyć nowy projekt Android w Android Studio (Kotlin, Compose, minSDK 26)
- [ ] Skonfigurować `build.gradle.kts` — dodać wszystkie zależności (patrz `docs/architecture.md`)
- [ ] Skonfigurować Hilt (`@HiltAndroidApp`, `AppModule`)
- [ ] Skonfigurować Room (baza lokalna)
- [ ] Skonfigurować Supabase Kotlin SDK
- [ ] Dodać `google-services.json` (FCM)
- [ ] Skonfigurować `local.properties` z kluczami (nie commitować)
- [ ] Ustawić `proguard-rules.pro`
- [ ] Skonfigurować Material3 theme (`FitCoachTheme`, kolory, typografia)

### Supabase
- [ ] Stworzyć schemat bazy danych (patrz `docs/database-schema.md`)
- [ ] Napisać migracje SQL w `supabase/migrations/`
- [ ] Skonfigurować RLS policies dla wszystkich tabel
- [ ] Skonfigurować Storage buckety (`avatars`, `progress-photos`)
- [ ] Włączyć Realtime dla `workout_sessions`, `habit_logs`, `messages`
- [ ] Skonfigurować Auth (email, Google OAuth)

---

## FAZA 1 — MVP Core (Tydzień 3–10)

### Autentykacja (Tydzień 3–4)
- [ ] Ekran powitalny (Splash screen z logo)
- [ ] Ekran logowania (email + hasło)
- [ ] Ekran rejestracji z wyborem roli (trener / klient)
- [ ] Rejestracja trenera — formularz (imię, nazwisko, certyfikaty)
- [ ] Rejestracja klienta — kod zaproszenia od trenera
- [ ] Reset hasła (email)
- [ ] Persystencja sesji (auto-login przy powrocie)
- [ ] Nawigacja po roli: trainer → TrainerDashboard, client → ClientDashboard
- [ ] Wylogowanie

### Dashboard trenera (Tydzień 5)
- [ ] Lista klientów z avatarami i statusem dzisiejszej aktywności
- [ ] Dzisiejszy kalendarz (zaplanowane sesje)
- [ ] Szybkie statystyki (aktywni klienci, treningi w tym tyg.)
- [ ] FAB — dodaj nowego klienta

### Zarządzanie klientami — trener (Tydzień 5–6)
- [ ] Lista klientów (RecyclerView → LazyColumn)
- [ ] Profil klienta (cel, waga, wzrost, BMI, data urodzenia)
- [ ] Dodaj klienta — generuj unikalny kod zaproszenia
- [ ] Edytuj profil klienta
- [ ] Archiwizuj klienta (miękkie usunięcie)
- [ ] Ekran przeglądu klienta (dzisiejsza aktywność, nawyki, waga)

### Plany treningowe — trener (Tydzień 6–7)
- [ ] Lista planów treningowych (aktywny, archiwalne)
- [ ] Kreator planu — nazwa, opis, dni tygodnia
- [ ] Dodaj dzień treningowy (np. Poniedziałek — Nogi)
- [ ] Baza ćwiczeń (nazwa, kategoria, grupy mięśniowe, opis)
- [ ] Dodaj ćwiczenia do dnia (serie, powtórzenia, ciężar, przerwa)
- [ ] Zmień kolejność ćwiczeń (drag & drop)
- [ ] Przypisz plan do klienta
- [ ] Duplikuj plan

### Logowanie treningu — klient (Tydzień 7–8)
- [ ] Dashboard klienta — dzisiejszy plan + nawyki
- [ ] Widok planu treningowego (przypisany przez trenera)
- [ ] Ekran aktywnej sesji treningowej
  - [ ] Lista ćwiczeń z seriami
  - [ ] Wpisywanie powtórzeń i ciężaru
  - [ ] Timer przerwy między seriami
  - [ ] Notatka do serii / do całego treningu
- [ ] Zakończenie sesji — podsumowanie
- [ ] Historia sesji klienta
- [ ] Trener widzi ukończone sesje klienta w czasie rzeczywistym

### Nawyki (Tydzień 8–9)
- [ ] Trener: dodaj nawyk dla klienta (nazwa, opis, typ: tak/nie lub ilość)
- [ ] Klient: widok nawyków na dziś
- [ ] Klient: zaznaczanie nawyku jako wykonany
- [ ] Klient: wpisywanie wartości (np. "wypiłem 2.1L wody")
- [ ] Trener: widzi zgodność klienta z nawykami (%)
- [ ] Streak — pas dni z rzędu (motywacja)
- [ ] Powiadomienie push — przypomnienie o nawyku (konfigurowane przez trenera)

### Offline sync (Tydzień 9–10)
- [ ] Room — lokalne przechowywanie sesji treningowych
- [ ] Sync manager — wysyłaj dane gdy internet wraca
- [ ] Indicator braku połączenia w UI
- [ ] Conflict resolution (last-write-wins dla serii treningowych)

---

## FAZA 2 — Żywienie i postęp (Tydzień 11–16)

### Dziennik żywienia — klient (Tydzień 11–13)
- [ ] Ekran dziennika żywienia (4 posiłki: śniadanie, obiad, kolacja, przekąska)
- [ ] Dodaj produkt — wyszukiwarka (Open Food Facts API)
- [ ] Skaner kodów kreskowych (ML Kit)
- [ ] Dodaj porcję — gram, sztuki, ml
- [ ] Wyświetl makroskładniki posiłku i dnia
- [ ] Kółko dzienne: kalorie spożyte / cel
- [ ] Cel kaloryczny i makro — ustawiany przez trenera
- [ ] Historia dziennika żywienia
- [ ] Klient: widzi dzisiejszy plan żywieniowy od trenera
- [ ] Trener: widzi dziennik żywienia klienta

### Polska baza żywności (Tydzień 12)
- [ ] Integracja Open Food Facts API (wyszukiwanie po nazwie + barcode)
- [ ] Cache produktów w Room (offline)
- [ ] Custom produkty — trener lub klient może dodać własny
- [ ] Popularne polskie produkty jako seed data (pierogi, schabowy, kiełbasa itp.)

### Śledzenie postępu (Tydzień 13–14)
- [ ] Klient: dodaj pomiar ciała (waga, % tkanki tłuszczowej, obwody)
- [ ] Wykres wagi (linia, ostatnie 30/90 dni)
- [ ] Klient: zdjęcia postępu (upload do Supabase Storage)
- [ ] Trener: widzi pomiary i zdjęcia klienta
- [ ] Porównanie zdjęć (before/after)
- [ ] Eksport danych klienta (PDF — opcjonalne)

### Śledzenie wody (Tydzień 14)
- [ ] Dzienne spożycie wody (szklanka = 250ml)
- [ ] Cel wody ustawiany przez trenera
- [ ] Widget-like progress bar na dashboardzie

### Komentarze trenera (Tydzień 15)
- [ ] Trener zostawia komentarz do sesji treningowej
- [ ] Trener komentuje dziennik żywienia (do posiłku)
- [ ] Klient widzi komentarze z powiadomieniem push
- [ ] Prosty czat trener-klient (Supabase Realtime)

### Powiadomienia push (Tydzień 15–16)
- [ ] Integracja FCM
- [ ] Powiadomienie: przypomnienie o treningu (codziennie o X)
- [ ] Powiadomienie: przypomnienie o nawyku (customizowane)
- [ ] Powiadomienie: nowy komentarz trenera
- [ ] Powiadomienie: nowy plan treningowy od trenera
- [ ] Ustawienia powiadomień w app (co, kiedy)

---

## FAZA 3 — Monetyzacja i onboarding (Tydzień 17–20)

### Stripe + BLIK (Tydzień 17–18)
- [ ] Konfiguracja Stripe w trybie produkcyjnym
- [ ] Edge Function: tworzenie Stripe Customer przy rejestracji trenera
- [ ] Edge Function: webhook Stripe (aktywacja subskrypcji, anulowanie)
- [ ] Ekran subskrypcji w app (plan Basic 79 zł/mies, Pro 149 zł/mies)
- [ ] Integracja Stripe Android SDK (Payment Sheet)
- [ ] Obsługa BLIK przez Stripe
- [ ] Ekran "Twoja subskrypcja" (plan, data odnowienia, anuluj)
- [ ] Obsługa wygaśnięcia subskrypcji (graceful degradation)
- [ ] Trial 14 dni dla nowych trenerów

### Limity planów (Tydzień 18)
- [ ] Basic (79 zł): do 10 aktywnych klientów
- [ ] Pro (149 zł): nieograniczona liczba klientów
- [ ] Blokada po przekroczeniu limitu z CTA do upgrade

### Onboarding (Tydzień 19–20)
- [ ] Onboarding trenera (3 kroki: profil → dodaj klienta → stwórz plan)
- [ ] Onboarding klienta (2 kroki: profil → zacznij pierwszy trening)
- [ ] Puste stany (empty states) z akcją (brak klientów, brak planu itp.)
- [ ] Tooltips / coach marks dla nowych użytkowników
- [ ] Ekran "Co nowego" po aktualizacji

---

## FAZA 4 — Szlifowanie i launch (Tydzień 21–26)

### Jakość i testy
- [ ] Unit testy dla use cases (JUnit5 + MockK)
- [ ] Testy UI dla krytycznych flow (Compose Test)
- [ ] Testy integracyjne dla Room DAO
- [ ] Testy Edge Functions (Supabase)
- [ ] Performance: Baseline Profiles dla Compose
- [ ] Crashlytics (Firebase) — monitoring błędów produkcyjnych

### UX i design
- [ ] Dark mode
- [ ] Animacje przejść (Compose AnimatedNavigation)
- [ ] Haptic feedback na kluczowych akcjach
- [ ] Accessibility (TalkBack, rozmiary fontów)
- [ ] Ekran ładowania (skeleton placeholders)
- [ ] Pull-to-refresh tam gdzie ma sens

### Google Play
- [ ] Konto Google Play Developer (25 USD jednorazowo)
- [ ] Grafika sklepu: ikona, feature graphic, 5+ screenshotów
- [ ] Opis aplikacji po polsku (ASO — słowa kluczowe)
- [ ] Polityka Prywatności (RODO) — strona www
- [ ] Regulamin korzystania
- [ ] Data Safety form w Play Console
- [ ] Closed testing (beta) — 20 trenerów
- [ ] Produkcyjny launch

### Marketing (równolegle)
- [ ] Landing page (Vercel + Next.js lub Carrd.co na start)
- [ ] Grupy FB dla trenerów personalnych — darmowe beta testy
- [ ] Instagram profil FitCoach
- [ ] Outreach do 50 trenerów personalnych (DM + email)
- [ ] Artykuł gościnny na portalu dla trenerów

---

## BACKLOG (po launch, w zależności od feedbacku)

- [ ] Wersja iOS (Kotlin Multiplatform lub React Native)
- [ ] Panel web dla trenera (Next.js + Supabase)
- [ ] Szablony planów treningowych (biblioteka)
- [ ] Video ćwiczeń (upload przez trenera)
- [ ] Integracja z Garmin / Fitbit / Samsung Health
- [ ] Kalendarz z rezerwacją sesji (jak Calendly)
- [ ] Faktura PDF do pobrania
- [ ] Integracja PolishAPI (open banking — śledzenie wydatków na suplementy)
- [ ] Wielojęzyczność (angielski — wejście na rynek UK/DE dla Polonii)
- [ ] Marketplace trenerów (klient szuka trenera)
