# FitCoach — Schemat bazy danych (Supabase / PostgreSQL)

## Tabele

### users
Zarządzana przez Supabase Auth. Rozszerzamy przez profile.

```sql
-- Supabase Auth tworzy: auth.users (id, email, created_at, user_metadata)
-- user_metadata zawiera: { "role": "trainer" | "client" }
```

### trainer_profiles
```sql
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
```

### client_profiles
```sql
CREATE TABLE client_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    trainer_id      UUID NOT NULL REFERENCES trainer_profiles(id),
    first_name      TEXT NOT NULL,
    last_name       TEXT NOT NULL,
    goal            TEXT,                          -- np. "Redukcja wagi -10kg"
    weight_kg       NUMERIC(5,2),
    height_cm       SMALLINT,
    birth_date      DATE,
    activity_level  TEXT CHECK (activity_level IN ('sedentary','light','moderate','active','very_active')),
    avatar_url      TEXT,
    invite_code     TEXT UNIQUE,                   -- kod do dołączenia
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### exercises
```sql
CREATE TABLE exercises (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    category        TEXT NOT NULL,                 -- 'strength','cardio','flexibility','other'
    muscle_groups   TEXT[] NOT NULL DEFAULT '{}',  -- ['chest','triceps']
    description     TEXT,
    video_url       TEXT,
    is_custom       BOOLEAN NOT NULL DEFAULT false,
    trainer_id      UUID REFERENCES trainer_profiles(id), -- null = predefiniowane
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### training_plans
```sql
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
```

### training_days
```sql
CREATE TABLE training_days (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id     UUID NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,                     -- np. "Nogi i pośladki"
    day_order   SMALLINT NOT NULL,                 -- kolejność w tygodniu
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### training_day_exercises
```sql
CREATE TABLE training_day_exercises (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_id          UUID NOT NULL REFERENCES training_days(id) ON DELETE CASCADE,
    exercise_id     UUID NOT NULL REFERENCES exercises(id),
    sets            SMALLINT NOT NULL,
    reps            SMALLINT,                      -- null dla cardio (czas zamiast)
    duration_sec    SMALLINT,                      -- dla cardio/planks
    weight_kg       NUMERIC(6,2),
    rest_seconds    SMALLINT NOT NULL DEFAULT 90,
    exercise_order  SMALLINT NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### workout_sessions
```sql
CREATE TABLE workout_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES client_profiles(id),
    plan_id         UUID REFERENCES training_plans(id),
    day_id          UUID REFERENCES training_days(id),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,
    duration_sec    INT,
    client_notes    TEXT,
    trainer_comment TEXT,
    is_synced       BOOLEAN NOT NULL DEFAULT false  -- dla offline sync
);
```

### session_sets
```sql
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
```

### habits
```sql
CREATE TABLE habits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id      UUID NOT NULL REFERENCES trainer_profiles(id),
    client_id       UUID NOT NULL REFERENCES client_profiles(id),
    name            TEXT NOT NULL,
    description     TEXT,
    type            TEXT NOT NULL CHECK (type IN ('boolean','quantity')),
    target_value    NUMERIC,                       -- np. 2.5 (litry wody)
    unit            TEXT,                          -- np. 'litry', 'kroki', 'minuty'
    reminder_time   TIME,                          -- godzina przypomnienia push
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### habit_logs
```sql
CREATE TABLE habit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id    UUID NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    client_id   UUID NOT NULL REFERENCES client_profiles(id),
    date        DATE NOT NULL DEFAULT CURRENT_DATE,
    completed   BOOLEAN NOT NULL DEFAULT false,
    value       NUMERIC,                           -- dla type='quantity'
    logged_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(habit_id, date)                         -- jeden log na nawyk na dzień
);
```

### food_products
```sql
CREATE TABLE food_products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barcode             TEXT UNIQUE,
    name                TEXT NOT NULL,
    brand               TEXT,
    calories_per_100g   NUMERIC(7,2) NOT NULL,
    protein_g           NUMERIC(6,2),
    carbs_g             NUMERIC(6,2),
    fat_g               NUMERIC(6,2),
    fiber_g             NUMERIC(6,2),
    source              TEXT NOT NULL CHECK (source IN ('openfoodfacts','custom')),
    added_by            UUID REFERENCES auth.users(id),  -- null = z Open Food Facts
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### food_entries
```sql
CREATE TABLE food_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES client_profiles(id),
    product_id      UUID NOT NULL REFERENCES food_products(id),
    date            DATE NOT NULL DEFAULT CURRENT_DATE,
    meal_type       TEXT NOT NULL CHECK (meal_type IN ('breakfast','lunch','dinner','snack')),
    quantity_g      NUMERIC(7,2) NOT NULL,
    trainer_comment TEXT,
    logged_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### nutrition_goals
```sql
CREATE TABLE nutrition_goals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL UNIQUE REFERENCES client_profiles(id),
    calories        SMALLINT,
    protein_g       SMALLINT,
    carbs_g         SMALLINT,
    fat_g           SMALLINT,
    water_ml        SMALLINT DEFAULT 2500,
    set_by          UUID NOT NULL REFERENCES trainer_profiles(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### body_measurements
```sql
CREATE TABLE body_measurements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES client_profiles(id),
    date            DATE NOT NULL DEFAULT CURRENT_DATE,
    weight_kg       NUMERIC(5,2),
    body_fat_pct    NUMERIC(4,1),
    chest_cm        NUMERIC(5,1),
    waist_cm        NUMERIC(5,1),
    hips_cm         NUMERIC(5,1),
    left_arm_cm     NUMERIC(5,1),
    right_arm_cm    NUMERIC(5,1),
    left_thigh_cm   NUMERIC(5,1),
    right_thigh_cm  NUMERIC(5,1),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### progress_photos
```sql
CREATE TABLE progress_photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID NOT NULL REFERENCES client_profiles(id),
    storage_url TEXT NOT NULL,
    date        DATE NOT NULL DEFAULT CURRENT_DATE,
    pose        TEXT CHECK (pose IN ('front','back','side_left','side_right')),
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### messages
```sql
CREATE TABLE messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id   UUID NOT NULL REFERENCES auth.users(id),
    receiver_id UUID NOT NULL REFERENCES auth.users(id),
    content     TEXT NOT NULL,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation 
ON messages(LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id), created_at);
```

### subscriptions
```sql
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
```

---

## RLS Policies (Row Level Security)

```sql
-- Włącz RLS na wszystkich tabelach
ALTER TABLE trainer_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE client_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE training_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE workout_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE habits ENABLE ROW LEVEL SECURITY;
ALTER TABLE habit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE food_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE body_measurements ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

-- TRAINER PROFILES
CREATE POLICY "trainer_read_own" ON trainer_profiles
    FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "trainer_update_own" ON trainer_profiles
    FOR UPDATE USING (user_id = auth.uid());

-- CLIENT PROFILES
CREATE POLICY "client_read_own" ON client_profiles
    FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "trainer_read_own_clients" ON client_profiles
    FOR SELECT USING (
        trainer_id = (SELECT id FROM trainer_profiles WHERE user_id = auth.uid())
    );

CREATE POLICY "trainer_manage_clients" ON client_profiles
    FOR ALL USING (
        trainer_id = (SELECT id FROM trainer_profiles WHERE user_id = auth.uid())
    );

-- WORKOUT SESSIONS
CREATE POLICY "client_manage_own_sessions" ON workout_sessions
    FOR ALL USING (
        client_id = (SELECT id FROM client_profiles WHERE user_id = auth.uid())
    );

CREATE POLICY "trainer_read_client_sessions" ON workout_sessions
    FOR SELECT USING (
        client_id IN (
            SELECT id FROM client_profiles
            WHERE trainer_id = (SELECT id FROM trainer_profiles WHERE user_id = auth.uid())
        )
    );

CREATE POLICY "trainer_comment_sessions" ON workout_sessions
    FOR UPDATE USING (
        client_id IN (
            SELECT id FROM client_profiles
            WHERE trainer_id = (SELECT id FROM trainer_profiles WHERE user_id = auth.uid())
        )
    ) WITH CHECK (true);

-- HABITS
CREATE POLICY "trainer_manage_habits" ON habits
    FOR ALL USING (
        trainer_id = (SELECT id FROM trainer_profiles WHERE user_id = auth.uid())
    );

CREATE POLICY "client_read_own_habits" ON habits
    FOR SELECT USING (
        client_id = (SELECT id FROM client_profiles WHERE user_id = auth.uid())
    );

-- MESSAGES
CREATE POLICY "read_own_messages" ON messages
    FOR SELECT USING (sender_id = auth.uid() OR receiver_id = auth.uid());

CREATE POLICY "send_messages" ON messages
    FOR INSERT WITH CHECK (sender_id = auth.uid());

-- SUBSCRIPTIONS
CREATE POLICY "trainer_read_own_subscription" ON subscriptions
    FOR SELECT USING (
        trainer_id = (SELECT id FROM trainer_profiles WHERE user_id = auth.uid())
    );
```

---

## Seed data — predefiniowane ćwiczenia

```sql
INSERT INTO exercises (name, category, muscle_groups, description, is_custom) VALUES
-- Nogi
('Przysiad ze sztangą', 'strength', ARRAY['quads','glutes','hamstrings'], 'Sztanga na karku, plecy wyprostowane, zejdź do paralelu.', false),
('Martwy ciąg', 'strength', ARRAY['hamstrings','glutes','lower_back'], 'Nogi na szerokość bioder, plecy neutralne.', false),
('Wykrok', 'strength', ARRAY['quads','glutes'], 'Krok do przodu, kolano tylnej nogi lekko nad ziemią.', false),
('Leg press', 'strength', ARRAY['quads','glutes'], 'Regulacja siedziska, stopy na szerokość bioder.', false),

-- Klatka
('Wyciskanie sztangi na ławce płaskiej', 'strength', ARRAY['chest','triceps','front_deltoid'], 'Łopatki ściągnięte, łuk w odcinku lędźwiowym.', false),
('Wyciskanie hantli na ławce skośnej', 'strength', ARRAY['chest','triceps'], 'Ławka 30-45 stopni, ruch kontrolowany.', false),
('Rozpiętki', 'strength', ARRAY['chest'], 'Łokcie lekko ugięte przez cały ruch.', false),

-- Plecy
('Podciąganie na drążku', 'strength', ARRAY['lats','biceps'], 'Chwyt nachwytem, pełen zakres ruchu.', false),
('Wiosłowanie ze sztangą', 'strength', ARRAY['lats','rhomboids','biceps'], 'Tułów równoległy do podłogi, łokcie wzdłuż ciała.', false),
('Wiosłowanie hantlem', 'strength', ARRAY['lats','rhomboids'], 'Kolano i ręka oparte na ławce.', false),

-- Ramiona
('Uginanie ramion ze sztangą', 'strength', ARRAY['biceps'], 'Łokcie przy ciele, pełen zakres.', false),
('French press', 'strength', ARRAY['triceps'], 'Łokcie nieruchome, sztangielka za głowę.', false),
('Wyciskanie żołnierskie', 'strength', ARRAY['front_deltoid','triceps'], 'Brzuch napięty, lędźwie bez wygiecia.', false),

-- Cardio / Core
('Plank', 'flexibility', ARRAY['core'], 'Biodra równo, oddech miarowy.', false),
('Rower stacjonarny', 'cardio', ARRAY['quads','glutes'], 'Intensywność wg tętna docelowego.', false),
('Bieganie na bieżni', 'cardio', ARRAY['full_body'], 'Tempo i nachylenie wg planu.', false);
```

---

## Indeksy

```sql
CREATE INDEX idx_client_profiles_trainer ON client_profiles(trainer_id);
CREATE INDEX idx_training_plans_client ON training_plans(client_id);
CREATE INDEX idx_training_plans_trainer ON training_plans(trainer_id);
CREATE INDEX idx_workout_sessions_client ON workout_sessions(client_id);
CREATE INDEX idx_workout_sessions_date ON workout_sessions(client_id, started_at DESC);
CREATE INDEX idx_habit_logs_habit_date ON habit_logs(habit_id, date);
CREATE INDEX idx_habit_logs_client_date ON habit_logs(client_id, date);
CREATE INDEX idx_food_entries_client_date ON food_entries(client_id, date);
CREATE INDEX idx_body_measurements_client ON body_measurements(client_id, date DESC);
CREATE INDEX idx_food_products_barcode ON food_products(barcode) WHERE barcode IS NOT NULL;
```
