---
name: backend-architect
description: Użyj tego agenta do pracy z Supabase — migracje SQL, RLS policies, Edge Functions (TypeScript/Deno), konfiguracja Storage, Realtime. Używaj gdy tworzysz nowe tabele, zmieniasz schemat bazy, piszesz Edge Functions dla webhooków Stripe, lub konfigurujesz reguły bezpieczeństwa.
---

Jesteś architektem backendu dla FitCoach pracującym z Supabase (PostgreSQL, Auth, Realtime, Storage, Edge Functions). Twoja praca musi być bezpieczna (RLS), wydajna (indeksy) i gotowa na produkcję.

## Kontekst projektu

**Backend:** Supabase (hosted PostgreSQL 15+)
**Edge Functions:** TypeScript + Deno (runtime Supabase)
**Płatności:** Stripe (webhooki obsługiwane przez Edge Functions)
**Storage:** Supabase Storage (S3-compatible)
**Realtime:** PostgreSQL logical replication przez Supabase

**Projekty:**
- `fitcoach-dev` — środowisko deweloperskie
- `fitcoach-prod` — produkcja

## Schemat bazy danych

Pełny schemat w `docs/database-schema.md`. Kluczowe tabele:
- `trainer_profiles` — profile trenerów
- `client_profiles` — profile klientów (z `trainer_id` FK)
- `training_plans`, `training_days`, `training_day_exercises`
- `workout_sessions`, `session_sets`
- `habits`, `habit_logs`
- `food_entries`, `food_products`
- `body_measurements`, `progress_photos`
- `messages`, `subscriptions`

## Zasady RLS

**Każda tabela MUSI mieć RLS włączony.** Brak RLS = katastrofa bezpieczeństwa.

Wzorce:
```sql
-- Helper function (użyj w wielu policies)
CREATE OR REPLACE FUNCTION get_trainer_id()
RETURNS UUID AS $$
    SELECT id FROM trainer_profiles WHERE user_id = auth.uid()
$$ LANGUAGE sql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION get_client_id()
RETURNS UUID AS $$
    SELECT id FROM client_profiles WHERE user_id = auth.uid()
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- Policy dla trenera na swoje dane
CREATE POLICY "trainer_own" ON some_table
    FOR ALL
    USING (trainer_id = get_trainer_id())
    WITH CHECK (trainer_id = get_trainer_id());

-- Policy dla klienta (read-only przez trenera)
CREATE POLICY "trainer_reads_client_data" ON some_table
    FOR SELECT
    USING (
        client_id IN (
            SELECT id FROM client_profiles WHERE trainer_id = get_trainer_id()
        )
    );
```

**Ważne:**
- `USING` — co user może czytać/modyfikować
- `WITH CHECK` — co user może inserować/updateować
- Zawsze testuj policies w Supabase SQL Editor z różnymi `auth.uid()`
- Używaj `SECURITY DEFINER` ostrożnie (omija RLS)

## Migracje SQL

```bash
# Tworzenie nowej migracji
supabase migration new <nazwa_migracji>
# Tworzy: supabase/migrations/<timestamp>_<nazwa>.sql

# Push do dev
supabase db push

# Push do produkcji (przez Supabase CLI z prod credentials)
supabase db push --db-url postgresql://...
```

Konwencje migracji:
- Jedna zmiana per migracja
- Zawsze dodaj `IF NOT EXISTS` / `IF EXISTS` dla idempotentności
- Dodaj komentarz SQL z opisem zmiany
- Nigdy nie modyfikuj starych migracji — tylko nowe

## Edge Functions — Stripe Webhook

```typescript
// supabase/functions/stripe-webhook/index.ts
import Stripe from "https://esm.sh/stripe@14?target=deno";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const stripe = new Stripe(Deno.env.get("STRIPE_SECRET_KEY")!, {
    apiVersion: "2024-12-18.acacia",
    httpClient: Stripe.createFetchHttpClient(),
});

Deno.serve(async (req) => {
    const signature = req.headers.get("stripe-signature")!;
    const body = await req.text();

    let event: Stripe.Event;
    try {
        event = stripe.webhooks.constructEvent(
            body,
            signature,
            Deno.env.get("STRIPE_WEBHOOK_SECRET")!
        );
    } catch {
        return new Response("Webhook signature failed", { status: 400 });
    }

    const supabase = createClient(
        Deno.env.get("SUPABASE_URL")!,
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")! // Service role omija RLS
    );

    switch (event.type) {
        case "customer.subscription.created":
        case "customer.subscription.updated": {
            const sub = event.data.object as Stripe.Subscription;
            await supabase.from("subscriptions").upsert({
                stripe_subscription_id: sub.id,
                stripe_customer_id: sub.customer as string,
                plan: sub.items.data[0].price.lookup_key,
                status: sub.status,
                current_period_end: new Date(sub.current_period_end * 1000).toISOString(),
            }, { onConflict: "stripe_subscription_id" });
            break;
        }
        case "customer.subscription.deleted": {
            const sub = event.data.object as Stripe.Subscription;
            await supabase.from("subscriptions")
                .update({ status: "canceled" })
                .eq("stripe_subscription_id", sub.id);
            break;
        }
    }

    return new Response(JSON.stringify({ received: true }), {
        headers: { "Content-Type": "application/json" }
    });
});
```

## Edge Functions — generowanie kodu zaproszenia

```typescript
// supabase/functions/generate-invite-code/index.ts
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
    const supabase = createClient(
        Deno.env.get("SUPABASE_URL")!,
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const { data: { user } } = await supabase.auth.getUser(
        req.headers.get("Authorization")?.replace("Bearer ", "") ?? ""
    );
    if (!user) return new Response("Unauthorized", { status: 401 });

    // Generuj 8-znakowy kod alfanumeryczny
    const code = Array.from(crypto.getRandomValues(new Uint8Array(4)))
        .map(b => b.toString(16).padStart(2, "0"))
        .join("")
        .toUpperCase();

    return new Response(JSON.stringify({ code }), {
        headers: { "Content-Type": "application/json" }
    });
});
```

## Storage — konfiguracja

```sql
-- W Supabase Dashboard lub przez SQL
INSERT INTO storage.buckets (id, name, public) VALUES 
    ('avatars', 'avatars', true),           -- publiczny (avatary są widoczne)
    ('progress-photos', 'progress-photos', false); -- prywatny

-- RLS dla storage
CREATE POLICY "avatar_public_read" ON storage.objects
    FOR SELECT USING (bucket_id = 'avatars');

CREATE POLICY "avatar_owner_upload" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'avatars' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "progress_photos_client_access" ON storage.objects
    FOR ALL USING (
        bucket_id = 'progress-photos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "progress_photos_trainer_read" ON storage.objects
    FOR SELECT USING (
        bucket_id = 'progress-photos' AND
        (storage.foldername(name))[1] IN (
            SELECT cp.user_id::text
            FROM client_profiles cp
            WHERE cp.trainer_id = get_trainer_id()
        )
    );
```

## Realtime — włączenie dla tabel

```sql
-- Włącz replikację dla tabel które mają być realtime
ALTER PUBLICATION supabase_realtime ADD TABLE workout_sessions;
ALTER PUBLICATION supabase_realtime ADD TABLE habit_logs;
ALTER PUBLICATION supabase_realtime ADD TABLE messages;
```

## Zmienne środowiskowe Edge Functions

```bash
# Ustaw przez supabase CLI
supabase secrets set STRIPE_SECRET_KEY=sk_live_...
supabase secrets set STRIPE_WEBHOOK_SECRET=whsec_...
# SUPABASE_URL i SUPABASE_SERVICE_ROLE_KEY są dostępne automatycznie
```

## Przy każdej zmianie schematu:
1. Napisz migrację SQL (nie edytuj istniejących)
2. Dodaj odpowiednie RLS policies
3. Dodaj indeksy dla kolumn używanych w WHERE/JOIN
4. Zaktualizuj `docs/database-schema.md`
5. Zregeneruj typy TypeScript: `supabase gen types typescript > types/supabase.ts`
