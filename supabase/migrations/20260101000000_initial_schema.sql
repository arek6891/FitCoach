-- FitCoach — Inicjalna migracja bazy danych
-- Tworzy podstawowe tabele: profiles, plany, sesje, nawyki
-- Pełny schemat w docs/database-schema.md

-- Rozszerzenia
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- wyszukiwanie pełnotekstowe

-- Helper functions dla RLS
CREATE OR REPLACE FUNCTION get_trainer_id()
RETURNS UUID AS $$
    SELECT id FROM trainer_profiles WHERE user_id = auth.uid()
$$ LANGUAGE sql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION get_client_id()
RETURNS UUID AS $$
    SELECT id FROM client_profiles WHERE user_id = auth.uid()
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- Trigger dla updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================
-- TRAINER PROFILES
-- =====================
CREATE TABLE trainer_profiles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    first_name  TEXT NOT NULL,
    last_name   TEXT NOT NULL,
    bio         TEXT,
    phone       TEXT,
    avatar_url  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE trainer_profiles ENABLE ROW LEVEL SECURITY;

CREATE TRIGGER update_trainer_profiles_updated_at
    BEFORE UPDATE ON trainer_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE POLICY "trainer_read_own" ON trainer_profiles
    FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "trainer_update_own" ON trainer_profiles
    FOR UPDATE USING (user_id = auth.uid());

CREATE POLICY "trainer_insert_own" ON trainer_profiles
    FOR INSERT WITH CHECK (user_id = auth.uid());

-- =====================
-- CLIENT PROFILES
-- =====================
CREATE TABLE client_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    trainer_id      UUID NOT NULL REFERENCES trainer_profiles(id),
    first_name      TEXT NOT NULL,
    last_name       TEXT NOT NULL,
    goal            TEXT,
    weight_kg       NUMERIC(5,2),
    height_cm       SMALLINT,
    birth_date      DATE,
    activity_level  TEXT CHECK (activity_level IN ('sedentary','light','moderate','active','very_active')),
    avatar_url      TEXT,
    invite_code     TEXT UNIQUE,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE client_profiles ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_client_profiles_trainer ON client_profiles(trainer_id) WHERE is_active = true;

CREATE TRIGGER update_client_profiles_updated_at
    BEFORE UPDATE ON client_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE POLICY "client_read_own" ON client_profiles
    FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "trainer_read_own_clients" ON client_profiles
    FOR SELECT USING (trainer_id = get_trainer_id());

CREATE POLICY "trainer_manage_clients" ON client_profiles
    FOR ALL USING (trainer_id = get_trainer_id())
    WITH CHECK (trainer_id = get_trainer_id());

-- =====================
-- EXERCISES
-- =====================
CREATE TABLE exercises (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    category        TEXT NOT NULL CHECK (category IN ('strength','cardio','flexibility','other')),
    muscle_groups   TEXT[] NOT NULL DEFAULT '{}',
    description     TEXT,
    video_url       TEXT,
    is_custom       BOOLEAN NOT NULL DEFAULT false,
    trainer_id      UUID REFERENCES trainer_profiles(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE exercises ENABLE ROW LEVEL SECURITY;

CREATE POLICY "read_public_exercises" ON exercises
    FOR SELECT USING (is_custom = false OR trainer_id = get_trainer_id());

CREATE POLICY "trainer_manage_custom_exercises" ON exercises
    FOR ALL USING (trainer_id = get_trainer_id())
    WITH CHECK (trainer_id = get_trainer_id() AND is_custom = true);

-- =====================
-- TRAINING PLANS
-- =====================
CREATE TABLE training_plans (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id  UUID NOT NULL REFERENCES trainer_profiles(id),
    client_id   UUID NOT NULL REFERENCES client_profiles(id),
    name        TEXT NOT NULL,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE training_plans ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_training_plans_client ON training_plans(client_id);
CREATE INDEX idx_training_plans_trainer ON training_plans(trainer_id);

CREATE POLICY "trainer_manage_plans" ON training_plans
    FOR ALL USING (trainer_id = get_trainer_id())
    WITH CHECK (trainer_id = get_trainer_id());

CREATE POLICY "client_read_own_plans" ON training_plans
    FOR SELECT USING (client_id = get_client_id());

-- =====================
-- TRAINING DAYS
-- =====================
CREATE TABLE training_days (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id     UUID NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    day_order   SMALLINT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE training_days ENABLE ROW LEVEL SECURITY;

CREATE POLICY "trainer_manage_days" ON training_days
    FOR ALL USING (
        plan_id IN (SELECT id FROM training_plans WHERE trainer_id = get_trainer_id())
    );

CREATE POLICY "client_read_own_days" ON training_days
    FOR SELECT USING (
        plan_id IN (SELECT id FROM training_plans WHERE client_id = get_client_id())
    );

-- =====================
-- TRAINING DAY EXERCISES
-- =====================
CREATE TABLE training_day_exercises (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_id          UUID NOT NULL REFERENCES training_days(id) ON DELETE CASCADE,
    exercise_id     UUID NOT NULL REFERENCES exercises(id),
    sets            SMALLINT NOT NULL,
    reps            SMALLINT,
    duration_sec    SMALLINT,
    weight_kg       NUMERIC(6,2),
    rest_seconds    SMALLINT NOT NULL DEFAULT 90,
    exercise_order  SMALLINT NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE training_day_exercises ENABLE ROW LEVEL SECURITY;

CREATE POLICY "trainer_manage_day_exercises" ON training_day_exercises
    FOR ALL USING (
        day_id IN (
            SELECT td.id FROM training_days td
            JOIN training_plans tp ON tp.id = td.plan_id
            WHERE tp.trainer_id = get_trainer_id()
        )
    );

CREATE POLICY "client_read_own_day_exercises" ON training_day_exercises
    FOR SELECT USING (
        day_id IN (
            SELECT td.id FROM training_days td
            JOIN training_plans tp ON tp.id = td.plan_id
            WHERE tp.client_id = get_client_id()
        )
    );

-- =====================
-- WORKOUT SESSIONS
-- =====================
CREATE TABLE workout_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES client_profiles(id),
    plan_id         UUID REFERENCES training_plans(id),
    day_id          UUID REFERENCES training_days(id),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,
    duration_sec    INT,
    client_notes    TEXT,
    trainer_comment TEXT
);

ALTER TABLE workout_sessions ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_workout_sessions_client ON workout_sessions(client_id);
CREATE INDEX idx_workout_sessions_client_date ON workout_sessions(client_id, started_at DESC);

ALTER PUBLICATION supabase_realtime ADD TABLE workout_sessions;

CREATE POLICY "client_manage_own_sessions" ON workout_sessions
    FOR ALL USING (client_id = get_client_id())
    WITH CHECK (client_id = get_client_id());

CREATE POLICY "trainer_read_client_sessions" ON workout_sessions
    FOR SELECT USING (
        client_id IN (
            SELECT id FROM client_profiles WHERE trainer_id = get_trainer_id()
        )
    );

CREATE POLICY "trainer_comment_sessions" ON workout_sessions
    FOR UPDATE USING (
        client_id IN (
            SELECT id FROM client_profiles WHERE trainer_id = get_trainer_id()
        )
    );

-- =====================
-- SESSION SETS
-- =====================
CREATE TABLE session_sets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id     UUID NOT NULL REFERENCES exercises(id),
    set_number      SMALLINT NOT NULL,
    reps            SMALLINT,
    weight_kg       NUMERIC(6,2),
    duration_sec    SMALLINT,
    completed_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE session_sets ENABLE ROW LEVEL SECURITY;

CREATE POLICY "client_manage_own_sets" ON session_sets
    FOR ALL USING (
        session_id IN (SELECT id FROM workout_sessions WHERE client_id = get_client_id())
    );

CREATE POLICY "trainer_read_client_sets" ON session_sets
    FOR SELECT USING (
        session_id IN (
            SELECT ws.id FROM workout_sessions ws
            JOIN client_profiles cp ON cp.id = ws.client_id
            WHERE cp.trainer_id = get_trainer_id()
        )
    );

-- =====================
-- HABITS
-- =====================
CREATE TABLE habits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id      UUID NOT NULL REFERENCES trainer_profiles(id),
    client_id       UUID NOT NULL REFERENCES client_profiles(id),
    name            TEXT NOT NULL,
    description     TEXT,
    type            TEXT NOT NULL CHECK (type IN ('boolean','quantity')),
    target_value    NUMERIC,
    unit            TEXT,
    reminder_time   TIME,
    current_streak  INT NOT NULL DEFAULT 0,
    longest_streak  INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE habits ENABLE ROW LEVEL SECURITY;

CREATE POLICY "trainer_manage_habits" ON habits
    FOR ALL USING (trainer_id = get_trainer_id())
    WITH CHECK (trainer_id = get_trainer_id());

CREATE POLICY "client_read_own_habits" ON habits
    FOR SELECT USING (client_id = get_client_id());

-- =====================
-- HABIT LOGS
-- =====================
CREATE TABLE habit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id    UUID NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    client_id   UUID NOT NULL REFERENCES client_profiles(id),
    date        DATE NOT NULL DEFAULT CURRENT_DATE,
    completed   BOOLEAN NOT NULL DEFAULT false,
    value       NUMERIC,
    logged_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(habit_id, date)
);

ALTER TABLE habit_logs ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_habit_logs_client_date ON habit_logs(client_id, date DESC);
CREATE INDEX idx_habit_logs_habit_date ON habit_logs(habit_id, date);

ALTER PUBLICATION supabase_realtime ADD TABLE habit_logs;

CREATE POLICY "client_manage_own_logs" ON habit_logs
    FOR ALL USING (client_id = get_client_id())
    WITH CHECK (client_id = get_client_id());

CREATE POLICY "trainer_read_client_logs" ON habit_logs
    FOR SELECT USING (
        client_id IN (
            SELECT id FROM client_profiles WHERE trainer_id = get_trainer_id()
        )
    );

-- =====================
-- SUBSCRIPTIONS
-- =====================
CREATE TABLE subscriptions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id              UUID NOT NULL UNIQUE REFERENCES trainer_profiles(id),
    stripe_customer_id      TEXT UNIQUE,
    stripe_subscription_id  TEXT UNIQUE,
    plan                    TEXT CHECK (plan IN ('trial','basic','pro')),
    status                  TEXT CHECK (status IN ('active','canceled','past_due','trialing')),
    trial_ends_at           TIMESTAMPTZ,
    current_period_end      TIMESTAMPTZ,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "trainer_read_own_subscription" ON subscriptions
    FOR SELECT USING (trainer_id = get_trainer_id());

-- =====================
-- SEED: Predefiniowane ćwiczenia
-- =====================
INSERT INTO exercises (name, category, muscle_groups, description, is_custom) VALUES
('Przysiad ze sztangą', 'strength', ARRAY['quads','glutes','hamstrings'], 'Sztanga na karku, plecy wyprostowane, zejdź do paralelu', false),
('Martwy ciąg', 'strength', ARRAY['hamstrings','glutes','lower_back'], 'Nogi na szerokość bioder, plecy neutralne', false),
('Wykrok', 'strength', ARRAY['quads','glutes'], 'Krok do przodu, kolano tylnej nogi nad ziemią', false),
('Leg press', 'strength', ARRAY['quads','glutes'], 'Stopy na szerokość bioder', false),
('Wyciskanie sztangi na ławce płaskiej', 'strength', ARRAY['chest','triceps','front_deltoid'], 'Łopatki ściągnięte', false),
('Wyciskanie hantli skośna', 'strength', ARRAY['chest','triceps'], 'Ławka 30-45 stopni', false),
('Rozpiętki', 'strength', ARRAY['chest'], 'Łokcie lekko ugięte przez cały ruch', false),
('Podciąganie na drążku', 'strength', ARRAY['lats','biceps'], 'Chwyt nachwytem, pełen zakres ruchu', false),
('Wiosłowanie ze sztangą', 'strength', ARRAY['lats','rhomboids','biceps'], 'Tułów równoległy do podłogi', false),
('Wiosłowanie hantlem', 'strength', ARRAY['lats','rhomboids'], 'Kolano i ręka oparte na ławce', false),
('Uginanie ramion ze sztangą', 'strength', ARRAY['biceps'], 'Łokcie przy ciele, pełen zakres', false),
('French press', 'strength', ARRAY['triceps'], 'Łokcie nieruchome', false),
('Wyciskanie żołnierskie', 'strength', ARRAY['front_deltoid','triceps'], 'Brzuch napięty', false),
('Plank', 'flexibility', ARRAY['core'], 'Biodra równo, oddech miarowy', false),
('Rower stacjonarny', 'cardio', ARRAY['quads','glutes'], 'Wg tętna docelowego', false),
('Bieganie na bieżni', 'cardio', ARRAY['full_body'], 'Tempo i nachylenie wg planu', false);
