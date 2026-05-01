# FitCoach — Roadmapa techniczna

## Oś czasu (26 tygodni do launch)

```
Tyg 1–2   ████ FAZA 0: Setup
Tyg 3–10  ████████████████ FAZA 1: MVP Core
Tyg 11–16 ████████████ FAZA 2: Żywienie + Postęp
Tyg 17–20 ████████ FAZA 3: Monetyzacja
Tyg 21–26 ████████████ FAZA 4: Launch
```

---

## Faza 0 — Setup (Tydzień 1–2)

**Cel:** Działające środowisko dev, pusta aplikacja na urządzeniu, baza danych gotowa.

| Zadanie | Narzędzie | Uwagi |
|---------|-----------|-------|
| Android projekt | Android Studio | minSDK 26 (Android 8.0, pokrywa 97%+ urządzeń) |
| Zależności | Gradle version catalogs | Hilt, Compose, Room, Retrofit, Supabase SDK |
| Schemat DB | Supabase + SQL migrations | RLS od początku, nie jako afterthought |
| Auth | Supabase Auth | email + hasło na start, Google OAuth w Fazie 3 |
| CI/CD | GitHub Actions | build + test na każdym PR |

**Deliverable:** App otwiera się, można się zalogować jako trener lub klient.

---

## Faza 1 — MVP Core (Tydzień 3–10)

**Cel:** Trener może zarządzać klientami i planami. Klient loguje treningi i nawyki. Trener widzi dane klienta.

### Tydzień 3–4: Autentykacja
- Logowanie / rejestracja z wyborem roli
- Kod zaproszenia (trener → klient)
- Persystencja sesji

### Tydzień 5: Dashboard trenera
- Lista klientów + status aktywności
- Dzisiejszy kalendarz

### Tydzień 5–6: Zarządzanie klientami
- CRUD klientów
- Profil klienta (cel, dane fizyczne)
- Widok aktywności klienta (read-only dla trenera)

### Tydzień 6–7: Plany treningowe
- Kreator planu (dni → ćwiczenia → serie/powt./ciężar)
- Baza ćwiczeń (predefiniowane + custom)
- Przypisanie planu do klienta

### Tydzień 7–8: Logowanie treningu (klient)
- Aktywna sesja z timerem przerwy
- Wpisywanie wyników
- Historia sesji

### Tydzień 8–9: Nawyki
- Trener ustawia nawyki per klient
- Klient odhacza nawyki
- Trener widzi zgodność (%)

### Tydzień 9–10: Offline sync
- Room cache dla sesji
- Sync manager

**Deliverable:** Działający MVP gotowy do beta testów z prawdziwymi trenerami.

**Milestone: Pokaż 5 trenerom. Zbierz feedback. Iteruj przed Fazą 2.**

---

## Faza 2 — Żywienie i postęp (Tydzień 11–16)

**Cel:** Trener ma pełny obraz klienta — trening + jedzenie + pomiary.

### Tydzień 11–13: Dziennik żywienia
- 4 posiłki dziennie
- Wyszukiwarka Open Food Facts
- Skaner kodów kreskowych
- Makroskładniki: kalorie, białko, węgle, tłuszcz
- Cel ustawiany przez trenera

### Tydzień 13–14: Postęp ciała
- Pomiary (waga, obwody)
- Wykresy (linia trendu wagi)
- Zdjęcia postępu (Supabase Storage)

### Tydzień 15: Komunikacja
- Komentarze trenera do sesji i posiłków
- Prosty czat (Supabase Realtime)

### Tydzień 15–16: Powiadomienia push
- FCM integracja
- Przypomnienia o treningu i nawykach
- Powiadomienia o komentarzach trenera

**Deliverable:** Kompletna aplikacja gotowa na płatny model.

---

## Faza 3 — Monetyzacja (Tydzień 17–20)

**Cel:** Trener płaci, aplikacja działa produkcyjnie.

### Tydzień 17–18: Stripe + BLIK
```
Przepływ płatności:
1. Trener rejestruje się → 14-dniowy trial
2. Trial kończy się → ekran wyboru planu
3. Stripe Payment Sheet (BLIK / karta)
4. Webhook → Supabase → aktualizacja statusu subskrypcji
5. Przy anulowaniu → dostęp do końca okresu
```

Plany:
- **Basic** — 79 zł/mies lub 699 zł/rok (= 2 mies gratis)
- **Pro** — 149 zł/mies lub 1299 zł/rok

### Tydzień 19–20: Onboarding
- Guided setup dla nowego trenera (3 kroki)
- Empty states z actionable CTA
- Tooltips dla nowych użytkowników

**Deliverable:** Pierwsze płacące konta.

---

## Faza 4 — Launch (Tydzień 21–26)

**Cel:** Aplikacja w Google Play, 30+ płacących trenerów.

### Tydzień 21–22: Testy i jakość
- Unit testy use cases
- UI testy krytycznych flow
- Performance profiling
- Crashlytics setup

### Tydzień 23–24: UX polish
- Dark mode
- Animacje
- Accessibility
- Skeleton loading

### Tydzień 25–26: Google Play launch
- Closed testing (20 trenerów)
- Grafika sklepu
- ASO (opis, słowa kluczowe)
- Polityka prywatności
- Produkcyjny release

---

## Metryki sukcesu

| Metryka | Cel 3 mies po launch | Cel 6 mies |
|---------|---------------------|------------|
| Zarejestrowani trenerzy | 50 | 150 |
| Płacący trenerzy | 20 | 80 |
| Aktywni klienci (przez trenerów) | 100 | 500 |
| MRR | 1 580 zł | 6 320 zł |
| Churn miesięczny | < 10% | < 7% |
| Ocena w Play Store | > 4.2 | > 4.5 |

---

## Ryzyka i mitygacje

| Ryzyko | Prawdopodobieństwo | Mitygacja |
|--------|-------------------|-----------|
| Niska adopcja (trenerzy nie chcą płacić) | Średnie | Darmowe beta, zbierz 5 płacących przed Fazą 3 |
| Supabase limity free tier | Niskie | Free: 500MB DB, 1GB storage — wystarczy na 6 mies |
| Stripe odrzuca konto | Niskie | Alternatywa: Paddle (Merchant of Record) |
| Zbyt złożone dla solo dev | Wysokie | Okroić scope Fazy 1 — tylko trening + 1 nawyk |
| Konkurencja (Trainerize wchodzi do PL) | Niskie | Polska lokalizacja + BLIK to realny moat |

---

## Techniczny dług do monitorowania

- [ ] Testy integracyjne Supabase RLS (uruchomić przed launch)
- [ ] Rate limiting na Edge Functions (spam protection)
- [ ] Rotacja kluczy API (SUPABASE_ANON_KEY nie jest wrażliwy, ale SERVICE_ROLE_KEY tak)
- [ ] Backup strategia dla Supabase (automatyczny w Pro planie)
- [ ] RODO: formularz "usuń moje konto" (prawo do bycia zapomnianym)
