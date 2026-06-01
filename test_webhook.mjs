#!/usr/bin/env node
/**
 * test_webhook.mjs — Test de l'envoi du webhook PayFlow vers Supabase
 *
 * USAGE :
 *   node test_webhook.mjs <GLOBAL_KEY>
 * ou modifier directement GLOBAL_KEY et WEBHOOK_URL ci-dessous.
 *
 * FORMAT envoyé :
 *   POST <WEBHOOK_URL>
 *   Authorization: Bearer <GLOBAL_KEY>
 *   Content-Type: application/json
 *   Body: { "sms_text": "<texte brut du SMS>" }
 */

const WEBHOOK_URL = process.env.WEBHOOK_URL || "https://dawvvqrqfgrerhssagyb.supabase.co/functions/v1/webhook";
const GLOBAL_KEY  = process.argv[2] || process.env.GLOBAL_KEY || "";

const SMS_DEPOT    = "Depot recu 2100F de ROSA VIDA GEST1 (2290161304332 - RBCCM/ABC/ 18 A 9992) le 2026-03-29 17:28:38 Solde:2105F ID:11793017938Frais:0F.";
const SMS_TRANSFERT = "Transfert 5125F de ADIOUEDE MARCEL SYLVAIN TCHIAKPE (2290197906854) 2026-04-22 07:59:36 Ref:vv Solde:5125F ID:11938552271";

const STATUS_LABELS = {
  200: "✅ 200 — Succès (SMS traité)",
  409: "♻️  409 — Anti-replay (déjà traité, ignoré normalement)",
  400: "❌ 400 — Données invalides (vérifier le format)",
  401: "🔐 401 — Global Key refusée (vérifier la clé)",
};

async function sendSms(label, smsText) {
  console.log(`\n${"═".repeat(60)}`);
  console.log(`📤 TEST : ${label}`);
  console.log(`${"─".repeat(60)}`);
  console.log(`SMS brut : "${smsText.substring(0, 70)}..."`);
  console.log(`URL      : ${WEBHOOK_URL}`);
  console.log(`Clé      : ${GLOBAL_KEY ? GLOBAL_KEY.substring(0, 8) + "..." : "⚠️  AUCUNE CLÉ FOURNIE"}`);

  const payload = JSON.stringify({ sms_text: smsText });

  const headers = {
    "Content-Type": "application/json",
  };
  if (GLOBAL_KEY) {
    headers["Authorization"] = `Bearer ${GLOBAL_KEY}`;
  }

  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15000);

    const response = await fetch(WEBHOOK_URL, {
      method: "POST",
      headers,
      body: payload,
      signal: controller.signal,
    });
    clearTimeout(timeout);

    let body = {};
    const rawText = await response.text();
    try { body = JSON.parse(rawText); } catch { body = { raw: rawText }; }

    const statusLabel = STATUS_LABELS[response.status] || `⚠️  ${response.status} — Code inconnu`;
    console.log(`\nRéponse HTTP : ${statusLabel}`);
    console.log(`Réponse JSON :`, JSON.stringify(body, null, 2));

    return { status: response.status, body, ok: response.ok || response.status === 409 };
  } catch (err) {
    if (err.name === "AbortError") {
      console.log(`\n❌ TIMEOUT — Le serveur n'a pas répondu en 15 secondes`);
    } else {
      console.log(`\n❌ ERREUR RÉSEAU : ${err.message}`);
    }
    return { status: null, body: null, ok: false };
  }
}

async function main() {
  console.log("\n🚀 TEST WEBHOOK PAYFLOW");
  console.log(`${"═".repeat(60)}`);

  if (!GLOBAL_KEY) {
    console.warn("⚠️  Aucune Global Key fournie. Passez-la en argument :");
    console.warn("    node test_webhook.mjs <VOTRE_GLOBAL_KEY>\n");
  }

  const r1 = await sendSms("SMS Dépôt MTN/Moov", SMS_DEPOT);
  const r2 = await sendSms("SMS Transfert", SMS_TRANSFERT);

  console.log(`\n${"═".repeat(60)}`);
  console.log("📊 RÉSUMÉ FINAL");
  console.log(`${"─".repeat(60)}`);
  console.log(`  Dépôt     : ${r1.ok ? "✅ PASS" : "❌ FAIL"} (HTTP ${r1.status ?? "N/A"})`);
  console.log(`  Transfert : ${r2.ok ? "✅ PASS" : "❌ FAIL"} (HTTP ${r2.status ?? "N/A"})`);

  const allPass = r1.ok && r2.ok;
  console.log(`\n${allPass ? "🎉 TOUS LES TESTS PASSÉS" : "⚠️  CERTAINS TESTS ONT ÉCHOUÉ"}`);
  console.log(`${"═".repeat(60)}\n`);
}

main();
