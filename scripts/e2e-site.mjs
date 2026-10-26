// E2E da aplicação inteira: o site (Next) conversando com a API de verdade, como o navegador faria.
// Precisa da API no perfil visitante (Ana e Bruno) e do site rodando com API_URL apontando para ela.
//
//   node scripts/e2e-site.mjs                         → site em http://localhost:3000
//   node scripts/e2e-site.mjs http://localhost:3001   → outra porta
//
// Confere: as páginas públicas abrem; o login pelo site (rota /api/auth/login do Next) grava a sessão em cookie
// httpOnly; com esse cookie, o proxy do Next leva "minhas contas" até a API; sem cookie, a área logada manda
// para o login. Sai com código 1 se algo falhar.

const SITE = (process.argv[2] ?? "http://localhost:3000").replace(/\/$/, "");
let falhas = 0;

function confere(nome, ok, detalhe = "") {
  console.log(`  ${ok ? "✅" : "❌"} ${nome}${ok || !detalhe ? "" : ` · ${detalhe}`}`);
  if (!ok) falhas += 1;
}

async function pagina(caminho, cookie) {
  return fetch(SITE + caminho, { redirect: "manual", headers: cookie ? { Cookie: cookie } : {} });
}

console.log(`\n🌐 Site em ${SITE}`);
for (const caminho of ["/", "/login", "/cadastro", "/termos", "/privacidade"]) {
  const r = await pagina(caminho);
  confere(`GET ${caminho} → 200`, r.status === 200, `veio ${r.status}`);
}

const semSessao = await pagina("/conta");
confere("área logada sem sessão manda para o login", [302, 303, 307, 308].includes(semSessao.status)
  && (semSessao.headers.get("location") ?? "").includes("/login"), `veio ${semSessao.status} ${semSessao.headers.get("location")}`);

console.log("\n🔐 Login pelo site (Ana Visitante)");
const login = await fetch(`${SITE}/api/auth/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ cpf: "24681357928", senha: "Visitante@2026" }),
});
const cookies = login.headers.getSetCookie().map((c) => c.split(";")[0]);
confere("POST /api/auth/login → 200", login.status === 200, `veio ${login.status}`);
confere("sessão gravada em cookie (rkf_token)", cookies.some((c) => c.startsWith("rkf_token=")), cookies.join(" "));
const cookie = cookies.join("; ");

console.log("\n💰 Pelo proxy do Next até a API");
const contas = await fetch(`${SITE}/api/contas`, { headers: { Cookie: cookie } });
const corpo = contas.status === 200 ? await contas.json() : null;
confere("GET /api/contas → 200 com a conta da Ana", Array.isArray(corpo) && corpo[0]?.numeroConta === "900001",
  `veio ${contas.status}`);
const conta = await pagina("/conta", cookie);
confere("página /conta abre com a sessão", conta.status === 200, `veio ${conta.status}`);

console.log(falhas ? `\n❌ ${falhas} verificação(ões) falharam` : "\n✅ site e API conversando de ponta a ponta");
process.exit(falhas ? 1 : 0);
