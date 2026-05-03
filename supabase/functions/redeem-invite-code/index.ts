// supabase/functions/redeem-invite-code/index.ts
//
// Realizuje kod zaproszenia po rejestracji klienta.
//
// POST /functions/v1/redeem-invite-code
// Authorization: Bearer <user-jwt>
// Content-Type: application/json
// Body: { "code": "A3F2B901" }
//
// Odpowiedzi:
//   200 { trainer_profile_id, trainer_user_id, client_name }
//   400 brak lub puste pole "code"
//   401 brak lub nieważny JWT
//   404 kod nie istnieje          (SQLSTATE P0002)
//   409 kod już użyty/nieaktywny  (SQLSTATE P0004)
//   410 kod wygasł                (SQLSTATE P0003)
//   500 nieoczekiwany błąd serwera

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// ── CORS ────────────────────────────────────────────────────────────────────
// Android nie wysyła preflight OPTIONS, ale dodajemy nagłówki dla kompletności
// i ewentualnych przyszłych wywołań z web-clienta / Postmana.

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
};

function corsResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      ...CORS_HEADERS,
    },
  });
}

// ── Mapowanie SQLSTATE → HTTP status ────────────────────────────────────────

const PG_ERROR_HTTP_MAP: Record<string, { status: number; message: string }> = {
  P0002: { status: 404, message: "Kod zaproszenia nie istnieje." },
  P0003: { status: 410, message: "Kod zaproszenia wygasł." },
  P0004: { status: 409, message: "Kod zaproszenia jest nieaktywny (już użyty lub anulowany)." },
};

// ── Handler ──────────────────────────────────────────────────────────────────

Deno.serve(async (req: Request): Promise<Response> => {
  // Obsługa preflight OPTIONS
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  if (req.method !== "POST") {
    return corsResponse(405, { error: "Method not allowed." });
  }

  // ── 1. Weryfikacja JWT ─────────────────────────────────────────────────────
  // Używamy klienta z anon key — auth.getUser() weryfikuje podpis JWT.

  const authHeader = req.headers.get("Authorization") ?? "";
  const jwt = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : "";

  if (!jwt) {
    return corsResponse(401, { error: "Brak tokenu autoryzacyjnego." });
  }

  const supabaseAuth = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { auth: { persistSession: false } },
  );

  const { data: { user }, error: authError } = await supabaseAuth.auth.getUser(jwt);

  if (authError || !user) {
    return corsResponse(401, { error: "Nieważny lub wygasły token." });
  }

  // ── 2. Parsowanie body ─────────────────────────────────────────────────────

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return corsResponse(400, { error: "Nieprawidłowy format body (oczekiwano JSON)." });
  }

  const code = typeof body.code === "string" ? body.code.trim() : "";
  if (!code) {
    return corsResponse(400, { error: "Pole 'code' jest wymagane i nie może być puste." });
  }

  // ── 3. Wywołanie redeem_invite_code przez service role ────────────────────
  // Funkcja PostgreSQL jest dostępna TYLKO dla service_role (REVOKE dla authenticated/anon).

  const supabaseAdmin = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    { auth: { persistSession: false } },
  );

  const { data, error: rpcError } = await supabaseAdmin.rpc("redeem_invite_code", {
    p_code: code,
    p_client_user_id: user.id,
  });

  // ── 4. Obsługa błędów PostgreSQL ──────────────────────────────────────────

  if (rpcError) {
    // Supabase zwraca kod błędu PostgreSQL w polu `code` obiektu błędu.
    const pgCode = (rpcError as { code?: string }).code ?? "";
    const mapped = PG_ERROR_HTTP_MAP[pgCode];

    if (mapped) {
      return corsResponse(mapped.status, { error: mapped.message });
    }

    // Nieznany błąd — loguj po stronie serwera, klientowi zwróć ogólny komunikat.
    console.error("[redeem-invite-code] Unexpected RPC error:", rpcError);
    return corsResponse(500, { error: "Wewnętrzny błąd serwera. Spróbuj ponownie później." });
  }

  // ── 5. Sukces ──────────────────────────────────────────────────────────────
  // data to JSONB zwrócone przez redeem_invite_code:
  //   { trainer_profile_id: UUID, trainer_user_id: UUID, client_name: string | null }

  return corsResponse(200, data);
});
