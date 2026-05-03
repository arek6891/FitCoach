-- FitCoach — System invite codes dla klientów
-- Trener generuje jednorazowy kod → klient wpisuje go podczas rejestracji
-- Kod wygasa po 7 dniach, jest jednorazowy (status: pending → used)
-- Powiązane tabele: invite_codes → trainer_profiles, client_profiles

-- =====================
-- ENUM: status kodu
-- =====================
DO $$ BEGIN
    CREATE TYPE invite_code_status AS ENUM ('pending', 'used', 'expired');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- =====================
-- TABELA: invite_codes
-- =====================
CREATE TABLE IF NOT EXISTS invite_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            TEXT NOT NULL,
    trainer_id      UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    client_name     TEXT,                                      -- opcjonalne imię/nazwisko klienta wpisane z góry przez trenera
    status          invite_code_status NOT NULL DEFAULT 'pending',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '7 days'),
    used_at         TIMESTAMPTZ,
    client_id       UUID REFERENCES auth.users(id) ON DELETE SET NULL  -- wypełniany gdy klient użyje kodu
);

-- Unikalność kodu (case-insensitive przez upper() przy generowaniu)
CREATE UNIQUE INDEX IF NOT EXISTS idx_invite_codes_code
    ON invite_codes(code);

-- Indeks dla trenera pobierającego swoje kody
CREATE INDEX IF NOT EXISTS idx_invite_codes_trainer_id
    ON invite_codes(trainer_id, created_at DESC);

-- Indeks dla szybkiego wyszukania klienta po client_id (np. sprawdzenie czy klient ma kod)
CREATE INDEX IF NOT EXISTS idx_invite_codes_client_id
    ON invite_codes(client_id) WHERE client_id IS NOT NULL;

-- Indeks dla lazy-expiry: szybkie znajdowanie wygasłych pending kodów
CREATE INDEX IF NOT EXISTS idx_invite_codes_pending_expires
    ON invite_codes(expires_at) WHERE status = 'pending';

-- =====================
-- RLS
-- =====================
ALTER TABLE invite_codes ENABLE ROW LEVEL SECURITY;

-- Trener widzi tylko swoje kody
CREATE POLICY "trainer_read_own_invite_codes" ON invite_codes
    FOR SELECT
    USING (trainer_id = auth.uid());

-- Trener może anulować (UPDATE status → expired) tylko swoje pending kody
-- INSERT jest zablokowany dla trenerów bezpośrednio — tylko przez funkcję generate_invite_code()
CREATE POLICY "trainer_cancel_own_invite_codes" ON invite_codes
    FOR UPDATE
    USING (trainer_id = auth.uid() AND status = 'pending')
    WITH CHECK (trainer_id = auth.uid() AND status = 'expired');

-- Niezalogowany / rejestrujący się użytkownik może odczytać kod do walidacji.
-- Bezpieczeństwo: ujawniamy tylko niezbędne kolumny przez widok (patrz niżej),
-- policy daje SELECT na poziomie wiersza — kolumny ogranicza widok public_invite_code_lookup.
-- Uwaga: policy FOR SELECT bez auth.uid() check umożliwia dostęp anonimowy (anon key).
CREATE POLICY "anon_lookup_invite_code" ON invite_codes
    FOR SELECT
    USING (true);  -- filtrowanie po konkretnym kodzie odbywa się w WHERE zapytania;
                   -- widok public_invite_code_lookup dodatkowo ogranicza kolumny

-- =====================
-- WIDOK: bezpieczne wyszukiwanie kodu przy rejestracji
-- Ogranicza kolumny ujawniane niezalogowanemu użytkownikowi.
-- Klient szuka kodu → dostaje tylko to co potrzebne do walidacji.
-- =====================
CREATE OR REPLACE VIEW public_invite_code_lookup
    WITH (security_invoker = true)
AS
SELECT
    code,
    status,
    expires_at,
    trainer_id,
    client_name
FROM invite_codes
WHERE status = 'pending'
  AND expires_at > now();

-- =====================
-- FUNKCJA: generate_invite_code(p_trainer_id UUID, p_client_name TEXT)
-- Generuje unikalny 8-znakowy kod alfanumeryczny uppercase i wstawia rekord.
-- Wykonywana z SECURITY DEFINER — omija RLS dla INSERT.
-- Wywołuje trener (musi być auth.uid() = p_trainer_id).
-- =====================
CREATE OR REPLACE FUNCTION generate_invite_code(
    p_trainer_id UUID,
    p_client_name TEXT DEFAULT NULL
)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_code        TEXT;
    v_attempts    INT := 0;
    v_max_attempts INT := 10;
BEGIN
    -- Tylko zalogowany trener może generować kod dla siebie
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Brak autoryzacji' USING ERRCODE = '42501';
    END IF;

    IF auth.uid() <> p_trainer_id THEN
        RAISE EXCEPTION 'Nie możesz generować kodu dla innego trenera' USING ERRCODE = '42501';
    END IF;

    -- Sprawdź czy trainer_id faktycznie istnieje w trainer_profiles
    IF NOT EXISTS (SELECT 1 FROM trainer_profiles WHERE user_id = p_trainer_id) THEN
        RAISE EXCEPTION 'Profil trenera nie istnieje' USING ERRCODE = '23503';
    END IF;

    -- Generuj unikalny kod, ponawiaj w razie kolizji (niezwykle rzadkie przy 8 znakach)
    LOOP
        -- 4 bajty losowe → 8 znaków hex uppercase, np. "A3F2B901"
        v_code := upper(encode(gen_random_bytes(4), 'hex'));

        BEGIN
            INSERT INTO invite_codes (code, trainer_id, client_name)
            VALUES (v_code, p_trainer_id, p_client_name);

            RETURN v_code;
        EXCEPTION
            WHEN unique_violation THEN
                v_attempts := v_attempts + 1;
                IF v_attempts >= v_max_attempts THEN
                    RAISE EXCEPTION 'Nie udało się wygenerować unikalnego kodu po % próbach', v_max_attempts
                        USING ERRCODE = 'P0001';
                END IF;
                -- Kontynuuj pętlę i spróbuj nowego kodu
        END;
    END LOOP;
END;
$$;

-- Odbierz uprawnienia PUBLIC do wywołania — tylko przez RPC z poprawnym JWT
REVOKE ALL ON FUNCTION generate_invite_code(UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION generate_invite_code(UUID, TEXT) TO authenticated;

-- =====================
-- FUNKCJA: redeem_invite_code(p_code TEXT, p_client_user_id UUID)
-- Wywołana po rejestracji klienta (przez service role z Edge Function).
-- 1. Weryfikuje kod (pending + nie wygasł)
-- 2. Ustawia status=used, client_id, used_at
-- 3. Ustawia client_profiles.trainer_id dla nowego klienta
-- Zwraca trainer_id (auth.users.id) — potrzebny do dalszego onboardingu.
-- SECURITY DEFINER — potrzebne do UPDATE przez service role / anon przy rejestracji.
-- W praktyce powinna być wywoływana WYŁĄCZNIE przez Edge Function z service_role key.
-- =====================
CREATE OR REPLACE FUNCTION redeem_invite_code(
    p_code          TEXT,
    p_client_user_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_invite        invite_codes%ROWTYPE;
    v_trainer_profile_id UUID;
    v_result        JSONB;
BEGIN
    -- Pobierz kod z blokadą wiersza — zapobiega race condition przy równoczesnym użyciu
    SELECT *
    INTO v_invite
    FROM invite_codes
    WHERE code = upper(trim(p_code))
    FOR UPDATE;

    -- Kod nie istnieje
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Kod zaproszenia nie istnieje' USING ERRCODE = 'P0002';
    END IF;

    -- Lazy expiry: oznacz jako wygasły jeśli minął termin
    IF v_invite.expires_at <= now() AND v_invite.status = 'pending' THEN
        UPDATE invite_codes
        SET status = 'expired'
        WHERE id = v_invite.id;

        RAISE EXCEPTION 'Kod zaproszenia wygasł' USING ERRCODE = 'P0003';
    END IF;

    -- Kod już użyty lub ręcznie wygaszony
    IF v_invite.status <> 'pending' THEN
        RAISE EXCEPTION 'Kod zaproszenia jest nieaktywny (status: %)', v_invite.status
            USING ERRCODE = 'P0004';
    END IF;

    -- Pobierz id profilu trenera (potrzebny jako FK w client_profiles.trainer_id)
    SELECT id
    INTO v_trainer_profile_id
    FROM trainer_profiles
    WHERE user_id = v_invite.trainer_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profil trenera nie istnieje' USING ERRCODE = '23503';
    END IF;

    -- Oznacz kod jako użyty
    UPDATE invite_codes
    SET
        status    = 'used',
        client_id = p_client_user_id,
        used_at   = now()
    WHERE id = v_invite.id;

    -- Ustaw trainer_id w profilu klienta (client_profiles.trainer_id → trainer_profiles.id)
    -- Zakładamy że client_profiles już istnieje (utworzony w tym samym flow rejestracji)
    -- lub jest tworzony przez trigger after insert on auth.users —
    -- jeśli profil jeszcze nie istnieje, UPDATE zwróci 0 wierszy bez błędu;
    -- Edge Function powinna zadbać o właściwą kolejność operacji.
    UPDATE client_profiles
    SET
        trainer_id  = v_trainer_profile_id,
        invite_code = upper(trim(p_code)),
        updated_at  = now()
    WHERE user_id = p_client_user_id;

    v_result := jsonb_build_object(
        'trainer_profile_id', v_trainer_profile_id,
        'trainer_user_id',    v_invite.trainer_id,
        'client_name',        v_invite.client_name
    );

    RETURN v_result;
END;
$$;

-- redeem_invite_code NIE powinna być dostępna z poziomu klienta —
-- wywołuje ją tylko Edge Function używając service_role key (który omija RLS i granty).
-- Odbieramy wykonanie dla authenticated i anon — tylko service_role może wywołać.
REVOKE ALL ON FUNCTION redeem_invite_code(TEXT, UUID) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION redeem_invite_code(TEXT, UUID) FROM authenticated;
REVOKE EXECUTE ON FUNCTION redeem_invite_code(TEXT, UUID) FROM anon;

-- =====================
-- FUNKCJA: expire_old_invite_codes()
-- Lazy batch cleanup — oznacza wszystkie przeterminowane pending kody jako expired.
-- Może być wywoływana przez pg_cron (jeśli włączony) lub ręcznie.
-- Nie jest wymagana do poprawnego działania systemu (redeem_invite_code robi lazy expiry
-- per-kod), ale przydatna dla czystości danych i czytelnych list po stronie trenera.
-- =====================
CREATE OR REPLACE FUNCTION expire_old_invite_codes()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_count INT;
BEGIN
    UPDATE invite_codes
    SET status = 'expired'
    WHERE status = 'pending'
      AND expires_at <= now();

    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END;
$$;

REVOKE ALL ON FUNCTION expire_old_invite_codes() FROM PUBLIC;
-- Tylko service_role (np. z pg_cron lub Edge Function maintenance job) może wywołać.

-- =====================
-- KOMENTARZE
-- =====================
COMMENT ON TABLE invite_codes IS
    'Jednorazowe kody zaproszenia generowane przez trenerów dla klientów. Kod wygasa po 7 dniach.';

COMMENT ON COLUMN invite_codes.code IS
    '8-znakowy kod hex uppercase (np. A3F2B901). Generowany przez generate_invite_code().';

COMMENT ON COLUMN invite_codes.trainer_id IS
    'auth.users.id trenera który wygenerował kod.';

COMMENT ON COLUMN invite_codes.client_name IS
    'Opcjonalne imię/nazwisko klienta wpisane z góry przez trenera — ułatwia identyfikację.';

COMMENT ON COLUMN invite_codes.client_id IS
    'auth.users.id klienta który użył kodu. NULL dopóki kod nie zostanie wykorzystany.';

COMMENT ON FUNCTION generate_invite_code IS
    'Generuje unikalny 8-znakowy kod i wstawia rekord do invite_codes. Wywołać przez RPC z JWT trenera.';

COMMENT ON FUNCTION redeem_invite_code IS
    'Realizuje kod zaproszenia: ustawia status=used i trainer_id w client_profiles. Wywoływać TYLKO przez Edge Function z service_role key.';

COMMENT ON FUNCTION expire_old_invite_codes IS
    'Batch cleanup wygasłych kodów. Opcjonalnie uruchamiać przez pg_cron raz dziennie.';
