// Procura vulnerabilidades conhecidas (base pública OSV.dev, a mesma que alimenta o GitHub) nas
// dependências resolvidas do backend (Maven) e do frontend (pnpm-lock.yaml).
// Uso: node scripts/varrer-cves.js [backend|frontend|tudo]   (padrão: tudo)
// Envia ao OSV.dev só nome e versão dos pacotes públicos. Sai com código 1 se encontrar algo.

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const RAIZ = path.join(__dirname, "..");
const ALVO = process.argv[2] ?? "tudo";
const pacotes = [];

if (ALVO !== "frontend") {
  const saida = path.join(RAIZ, "target", "dependencias.txt");
  fs.mkdirSync(path.dirname(saida), { recursive: true });
  // Caminho absoluto: o cmd do Windows pode não procurar executáveis na pasta atual.
  const mvnw = path.join(RAIZ, process.platform === "win32" ? "mvnw.cmd" : "mvnw");
  execSync(`"${mvnw}" -q -B -ntp dependency:list -DoutputFile="${saida}"`, { cwd: RAIZ, stdio: "inherit" });
  for (const linha of fs.readFileSync(saida, "utf8").split(/\r?\n/)) {
    const m = linha.trim().match(/^([^:\s]+):([^:]+):[^:]+:(?:[^:]+:)?([^:]+):(compile|runtime|test|provided)/);
    if (m) pacotes.push({ eco: "Maven", nome: `${m[1]}:${m[2]}`, versao: m[3], onde: `backend (${m[4]})` });
  }
}

if (ALVO !== "backend") {
  let dentro = false;
  for (const linha of fs.readFileSync(path.join(RAIZ, "frontend", "pnpm-lock.yaml"), "utf8").split(/\r?\n/)) {
    if (/^packages:/.test(linha)) { dentro = true; continue; }
    if (/^\S/.test(linha)) dentro = false;
    const m = dentro && linha.match(/^  '?(@?[^@'\s]+)@([^'():\s]+)'?:\s*$/);
    if (m) pacotes.push({ eco: "npm", nome: m[1], versao: m[2], onde: "frontend" });
  }
}

(async () => {
  const achados = [];
  for (let i = 0; i < pacotes.length; i += 500) {
    const lote = pacotes.slice(i, i + 500);
    const resposta = await fetch("https://api.osv.dev/v1/querybatch", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ queries: lote.map((p) => ({ package: { ecosystem: p.eco, name: p.nome }, version: p.versao })) }),
    });
    const json = await resposta.json();
    json.results.forEach((r, j) => r.vulns?.length && achados.push({ ...lote[j], ids: r.vulns.map((v) => v.id) }));
  }

  console.log(`\nPacotes verificados: ${pacotes.length}`);
  if (!achados.length) {
    console.log("✅ Nenhuma vulnerabilidade conhecida.");
    return;
  }
  for (const a of achados) {
    console.log(`\n## ${a.nome}@${a.versao}  [${a.onde}]`);
    for (const id of a.ids) {
      const v = await (await fetch(`https://api.osv.dev/v1/vulns/${id}`)).json();
      const severidade = v.database_specific?.severity ?? "?";
      const corrigidoEm = (v.affected ?? [])
        .filter((af) => af.package?.name === a.nome)
        .flatMap((af) => (af.ranges ?? []).flatMap((r) => r.events.filter((e) => e.fixed).map((e) => e.fixed)));
      const cve = (v.aliases ?? []).find((x) => x.startsWith("CVE")) ?? "";
      console.log(`- ${id} ${cve} [${severidade}] corrigido em: ${corrigidoEm.join(", ") || "sem correção"} :: ${v.summary ?? ""}`);
    }
  }
  process.exitCode = 1;
})();
