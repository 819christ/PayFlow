// test_rules.mjs — Test complet des règles PayFlow
// Exécuter : node test_rules.mjs

// ══════════════════════════════════════════════════════════
// COPIE DU PARSER (depuis parser.js)
// ══════════════════════════════════════════════════════════
const SMSParser = {
    before: (text, ref) => { const i = text.indexOf(ref); return i > -1 ? text.substring(0, i).trim() : null; },
    after:  (text, ref) => { const i = text.indexOf(ref); return i > -1 ? text.substring(i + ref.length).trim() : null; },
    between: (text, s, e) => {
        const si = text.indexOf(s); if (si === -1) return null;
        const after = text.substring(si + s.length);
        const ei = after.indexOf(e);
        return ei > -1 ? after.substring(0, ei).trim() : null;
    },
    regex: (text, pattern, g = 1) => {
        try { const m = text.match(new RegExp(pattern, 'i')); return m ? (m[g] || m[0]).trim() : null; }
        catch { return null; }
    },
    extractNumber: (text) => { if (!text) return 0; const c = String(text).replace(/[^0-9]/g, ''); return c ? parseInt(c, 10) : 0; },
    cleanPhone: (text) => { if (!text) return null; return text.replace(/\s+/g, '').replace(/[^\d+]/g, '') || null; },
    parse: (body, nodes = []) => {
        const r = { senderName: null, senderPhone: null, amount: 0, txId: null };
        nodes.forEach(n => {
            let ex = null;
            if (n.type === 'before')  ex = SMSParser.before(body, n.val1);
            if (n.type === 'after')   ex = SMSParser.after(body, n.val1);
            if (n.type === 'between') ex = SMSParser.between(body, n.val1, n.val2);
            if (n.type === 'regex')   ex = SMSParser.regex(body, n.val1, Number(n.val2)||1);
            if (n.target === 'amount')      r.amount      = SMSParser.extractNumber(ex);
            if (n.target === 'senderName')  r.senderName  = ex;
            if (n.target === 'senderPhone') r.senderPhone = SMSParser.cleanPhone(ex);
            if (n.target === 'txId')        r.txId        = ex;
        });
        return r;
    },
    matches: (body, kw = '', sf = '') => {
        const b = body.toLowerCase();
        const kwOk = kw ? kw.split('.').filter(Boolean).every(k => b.includes(k.toLowerCase())) : true;
        const sfOk = sf ? b.includes(sf.toLowerCase()) : true;
        return kwOk && sfOk;
    }
};

// ══════════════════════════════════════════════════════════
// SMS DE TEST (vrais formats fournis par l'utilisateur)
// ══════════════════════════════════════════════════════════
const SMS_DEPOT = "Depot recu 2100F de ROSA VIDA GEST1 (2290161304332 - RBCCM/ABC/ 18 A 9992) le 2026-03-29 17:28:38 Solde:2105F ID:11793017938Frais:0F.";
const SMS_TRANSFERT = "Transfert 5125F de ADIOUEDE MARCEL SYLVAIN TCHIAKPE (2290197906854) 2026-04-22 07:59:36 Ref:vv Solde:5125F ID:11938552271";

// ══════════════════════════════════════════════════════════
// RÈGLES CRÉÉES (celles qu'on injecterait dans l'app)
// ══════════════════════════════════════════════════════════

const RULE_DEPOT = {
    name: "SMS Dépôt",
    keywords: "Depot.recu",
    senderFilter: "",
    enabled: true,
    nodes: [
        // Montant : ENTRE "recu " ET "F de"
        { type: "between", target: "amount",      val1: "recu ",     val2: "F de" },
        // Nom expéditeur : ENTRE "F de " ET " ("
        { type: "between", target: "senderName",  val1: "F de ",     val2: " (" },
        // Téléphone : ENTRE "(" ET " -"  (format "2290161304332 - ...")
        { type: "between", target: "senderPhone", val1: "(",         val2: " -" },
        // ID transaction : ENTRE "ID:" ET "Frais"
        { type: "between", target: "txId",        val1: "ID:",       val2: "Frais" },
    ]
};

const RULE_TRANSFERT = {
    name: "SMS Transfert",
    keywords: "Transfert",
    senderFilter: "",
    enabled: true,
    nodes: [
        // Montant : ENTRE "Transfert " ET "F de"
        { type: "between", target: "amount",      val1: "Transfert ", val2: "F de" },
        // Nom : ENTRE "F de " ET " ("
        { type: "between", target: "senderName",  val1: "F de ",      val2: " (" },
        // Téléphone : ENTRE "(" ET ")"
        { type: "between", target: "senderPhone", val1: "(",          val2: ")" },
        // ID transaction : APRÈS "ID:"
        { type: "after",   target: "txId",        val1: "ID:",        val2: "" },
    ]
};

const allRules = [RULE_DEPOT, RULE_TRANSFERT];

// ══════════════════════════════════════════════════════════
// FONCTIONS DE TEST
// ══════════════════════════════════════════════════════════
function testSMS(label, smsBody, rule) {
    console.log(`\n${'═'.repeat(60)}`);
    console.log(`📱 TEST : ${label}`);
    console.log(`${'─'.repeat(60)}`);
    console.log(`SMS brut :\n  "${smsBody.substring(0, 80)}..."`);

    // Test 1: la règle match-elle ?
    const matches = SMSParser.matches(smsBody, rule.keywords, rule.senderFilter);
    console.log(`\n✅ Règle "${rule.name}" déclenchée : ${matches ? '✅ OUI' : '❌ NON'}`);
    if (!matches) { console.log('  → ÉCHEC : les mots-clés ne correspondent pas'); return false; }

    // Test 2: parsing nodal
    const parsed = SMSParser.parse(smsBody, rule.nodes);
    console.log(`\nRésultat du parsing :`);
    console.log(`  💰 Montant    : ${parsed.amount > 0 ? parsed.amount.toLocaleString('fr-FR') + ' FCFA' : '❌ non détecté'}`);
    console.log(`  👤 Nom        : ${parsed.senderName  || '❌ non détecté'}`);
    console.log(`  📞 Téléphone  : ${parsed.senderPhone || '❌ non détecté'}`);
    console.log(`  🔑 Réf. TX    : ${parsed.txId        || '❌ non détecté'}`);

    const ok = parsed.amount > 0 && parsed.senderName && parsed.senderPhone && parsed.txId;
    console.log(`\n${ok ? '✅ PARSING COMPLET' : '⚠️  PARSING PARTIEL'}`);
    return { matches, parsed, ok };
}

function testFindAll(blob, rules) {
    console.log(`\n${'═'.repeat(60)}`);
    console.log(`📦 TEST BATCH : détection dans un bloc de texte`);
    console.log(`${'─'.repeat(60)}`);
    const active = rules.filter(r => r.enabled !== false);
    const found = [];
    const chunks = blob.split(/[\n\r]{1,}/).map(s => s.trim()).filter(s => s.length > 10);
    chunks.forEach(chunk => {
        active.forEach(rule => {
            if (SMSParser.matches(chunk, rule.keywords, rule.senderFilter)) {
                found.push({ text: chunk.substring(0, 50) + '...', rule: rule.name });
            }
        });
    });
    console.log(`📊 ${found.length} SMS détecté(s) dans le bloc :`);
    found.forEach((m, i) => console.log(`  ${i+1}. [${m.rule}] "${m.text}"`));
    return found;
}

// ══════════════════════════════════════════════════════════
// EXÉCUTION DES TESTS
// ══════════════════════════════════════════════════════════
console.log('\n🚀 DÉMARRAGE DES TESTS PAYFLOW');
console.log(`${'═'.repeat(60)}`);

const r1 = testSMS('Dépôt MTN/Moov', SMS_DEPOT, RULE_DEPOT);
const r2 = testSMS('Transfert', SMS_TRANSFERT, RULE_TRANSFERT);

// Test batch : les 2 SMS dans un seul bloc
const BATCH_BLOB = `${SMS_DEPOT}\n${SMS_TRANSFERT}`;
const batchResult = testFindAll(BATCH_BLOB, allRules);

// Résumé final
console.log(`\n${'═'.repeat(60)}`);
console.log('📋 RÉSUMÉ FINAL');
console.log(`${'─'.repeat(60)}`);
console.log(`  Dépôt    : ${r1.ok ? '✅ PASS' : '❌ FAIL'}`);
console.log(`  Transfert: ${r2.ok ? '✅ PASS' : '❌ FAIL'}`);
console.log(`  Batch    : ${batchResult.length === 2 ? '✅ PASS (2/2 détectés)' : `❌ FAIL (${batchResult.length}/2 détectés)`}`);
const allPass = r1.ok && r2.ok && batchResult.length === 2;
console.log(`\n${allPass ? '🎉 TOUS LES TESTS PASSÉS' : '⚠️  CERTAINS TESTS ONT ÉCHOUÉ'}`);
console.log(`${'═'.repeat(60)}\n`);

// Export des règles prêtes à être injectées dans l'app
console.log('📤 RÈGLES À INJECTER dans PayFlow (JSON) :');
console.log(JSON.stringify(allRules, null, 2));
