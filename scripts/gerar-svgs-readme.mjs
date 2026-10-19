// Gera os SVGs animados do README (assets/readme/): banner, botões, números, extrato, fluxo, segurança, time.
// Uso: node scripts/gerar-svgs-readme.mjs   (rode de novo quando mudar o número de testes, rotas ou migrações)
// O GitHub mostra SVG como imagem: animação CSS funciona; clique só no <a> ou no <summary> em volta; sem JS e sem fonte externa.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const RAIZ = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const SAIDA = path.join(RAIZ, 'assets', 'readme');

// Identidade visual do frontend (frontend/app/globals.css).
const FUNDO = '#0A0F1C';
const SUPERFICIE = '#111827';
const BORDA = '#1E293B';
const AZUL = '#3B82F6';
const CIANO = '#06B6D4';
const TEXTO = '#F1F5F9';
const APAGADO = '#94A3B8';
const VERDE = '#22C55E';
const VERMELHO = '#F87171';
const SANS = "'Segoe UI', Inter, Ubuntu, 'Helvetica Neue', Arial, sans-serif";
const MONO = "Consolas, 'JetBrains Mono', 'Fira Code', Menlo, monospace";

// Números exibidos no README. Atualize aqui e rode o script de novo.
const NUMEROS = {
  testes: contarTestes(),
  rotas: 9,
  migracoes: fs.readdirSync(path.join(RAIZ, 'backend/src/main/resources/db/migration')).filter((f) => /^V\d+__.*\.sql$/.test(f)).length,
  status: 9, // 400, 401, 403, 404, 409, 422, 503 + 201/204 de sucesso
  workflows: fs.readdirSync(path.join(RAIZ, '.github/workflows')).filter((f) => f.endsWith('.yml')).length,
  time: 3,
};

function contarTestes() {
  let total = 0;
  const varrer = (dir) => {
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      const f = path.join(dir, e.name);
      if (e.isDirectory()) varrer(f);
      else if (e.name.endsWith('.java')) total += (fs.readFileSync(f, 'utf8').match(/@(Test|ParameterizedTest)\b/g) ?? []).length;
    }
  };
  varrer(path.join(RAIZ, 'backend/src/test/java'));
  return total;
}

const xml = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

const cabecalho = (w, h, rotulo) =>
  `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" role="img" aria-label="${xml(rotulo)}">
  <!-- Gerado por scripts/gerar-svgs-readme.mjs. Não edite à mão. -->`;

const marca = `<linearGradient id="marca" x1="0" x2="1"><stop offset="0" stop-color="${AZUL}"/><stop offset="1" stop-color="${CIANO}"/></linearGradient>`;

const entra = `.entra { opacity: 0; animation: entra .7s cubic-bezier(.2,.8,.2,1) forwards; }
    @keyframes entra { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }`;

// ---------- 💳 cartão RKF (o mesmo desenho do components/marketing/bank-card.tsx) ----------

function cartao(x, y, w, titular = 'SEU NOME AQUI', final = '4821') {
  const h = Math.round(w / 1.586);
  const ondas = [0, 14, 28, 42, 56]
    .map((o) => `<path d="M-20 ${h * 0.79 - o} C ${w * 0.22} ${h * 0.48 - o}, ${w * 0.5} ${h * 1.03 - o}, ${w + 20} ${h * 0.44 - o}" stroke="${AZUL}" stroke-opacity="${(0.12 + o / 300).toFixed(2)}" stroke-width="1.2" fill="none"/>`)
    .join('');
  return `<g transform="translate(${x} ${y})">
      <defs>
        <linearGradient id="fundoCartao" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0f1e3d"/><stop offset=".55" stop-color="${FUNDO}"/><stop offset="1" stop-color="#0b2a4a"/></linearGradient>
        <linearGradient id="chip" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#e2c98a"/><stop offset="1" stop-color="#a88a4c"/></linearGradient>
        <clipPath id="formaCartao"><rect width="${w}" height="${h}" rx="18"/></clipPath>
        <linearGradient id="reflexo" x1="0" x2="1"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff" stop-opacity=".10"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
      </defs>
      <g clip-path="url(#formaCartao)">
        <rect width="${w}" height="${h}" fill="url(#fundoCartao)"/>
        ${ondas}
        <rect class="reflexo" x="-${w}" width="${w * 0.6}" height="${h}" fill="url(#reflexo)" transform="skewX(-20)"/>
      </g>
      <rect width="${w}" height="${h}" rx="18" fill="none" stroke="#ffffff" stroke-opacity=".12"/>
      <text x="24" y="40" style="font: 700 17px ${SANS}; letter-spacing: 5px" fill="${TEXTO}">RKF</text>
      <g transform="translate(${w - 44} 24)" stroke="${APAGADO}" stroke-width="2" fill="none" stroke-linecap="round">
        <path d="M4 4 q6 8 0 16"/><path d="M10 1 q9 11 0 22"/><path d="M16 -2 q12 14 0 28"/>
      </g>
      <rect x="24" y="${h * 0.38}" width="46" height="34" rx="6" fill="url(#chip)" opacity=".92"/>
      <text x="24" y="${h - 46}" style="font: 600 17px ${MONO}; letter-spacing: 4px" fill="${TEXTO}">•••• •••• •••• ${final}</text>
      <text x="24" y="${h - 22}" style="font: 600 11px ${SANS}; letter-spacing: 3px" fill="${APAGADO}">${xml(titular)}</text>
      <text x="${w - 24}" y="${h - 22}" text-anchor="end" style="font: 700 12px ${SANS}; letter-spacing: 3px" fill="${CIANO}">DÉBITO</text>
    </g>`;
}

// ---------- 📱 celular ----------

function celular(x, y, conteudo, { w = 250, h = 470 } = {}) {
  return `<g transform="translate(${x} ${y})">
      <rect width="${w}" height="${h}" rx="34" fill="#05080f" stroke="#2b3547" stroke-width="2"/>
      <rect x="10" y="10" width="${w - 20}" height="${h - 20}" rx="26" fill="${FUNDO}"/>
      <rect x="${w / 2 - 40}" y="18" width="80" height="20" rx="10" fill="#05080f"/>
      ${conteudo}
    </g>`;
}

// ---------- 🏦 banner ----------

function banner() {
  const tela = `
      <text x="28" y="72" style="font: 600 12px ${SANS}" fill="${APAGADO}">Olá, Fellipe 👋</text>
      <text x="28" y="96" style="font: 700 13px ${SANS}; letter-spacing: 3px" fill="${TEXTO}">RKF</text>
      <rect x="22" y="112" width="206" height="96" rx="16" fill="${SUPERFICIE}" stroke="${BORDA}"/>
      <text x="38" y="138" style="font: 400 11px ${SANS}" fill="${APAGADO}">Saldo disponível</text>
      <text x="38" y="170" class="saldo" style="font: 800 26px ${SANS}" fill="${TEXTO}">R$ 1.250,00</text>
      <text x="38" y="192" style="font: 400 10px ${MONO}" fill="${APAGADO}">ag 0001 · cc 48213579</text>
      ${[['Depositar', '↓'], ['Sacar', '↑'], ['Transferir', '⇄']].map(([t, i], k) => `
      <g transform="translate(${22 + k * 70} 222)">
        <rect width="62" height="62" rx="14" fill="${SUPERFICIE}" stroke="${BORDA}"/>
        <text x="31" y="30" text-anchor="middle" style="font: 700 18px ${SANS}" fill="${CIANO}">${i}</text>
        <text x="31" y="50" text-anchor="middle" style="font: 500 9.5px ${SANS}" fill="${TEXTO}">${t}</text>
      </g>`).join('')}
      <text x="28" y="314" style="font: 600 12px ${SANS}" fill="${TEXTO}">Extrato</text>
      ${[['Transferência recebida', '+ 50,00', VERDE], ['Saque', '− 20,00', VERMELHO], ['Depósito', '+ 150,00', VERDE]].map(([t, v, c], k) => `
      <g class="linha" style="animation-delay:${(1.2 + k * 0.35).toFixed(2)}s">
        <rect x="22" y="${326 + k * 38}" width="206" height="32" rx="9" fill="${SUPERFICIE}"/>
        <text x="34" y="${347 + k * 38}" style="font: 500 10.5px ${SANS}" fill="${TEXTO}">${t}</text>
        <text x="216" y="${347 + k * 38}" text-anchor="end" style="font: 700 10.5px ${MONO}" fill="${c}">${v}</text>
      </g>`).join('')}`;

  return `${cabecalho(1200, 440, 'RKF Banco Digital: conta digital com depósito, saque, transferência e extrato. Confiança, tecnologia e liberdade.')}
  <defs>${marca}
    <radialGradient id="luzAzul" cx=".15" cy=".2" r=".6"><stop offset="0" stop-color="${AZUL}" stop-opacity=".35"/><stop offset="1" stop-color="${AZUL}" stop-opacity="0"/></radialGradient>
    <radialGradient id="luzCiano" cx=".85" cy=".85" r=".55"><stop offset="0" stop-color="${CIANO}" stop-opacity=".25"/><stop offset="1" stop-color="${CIANO}" stop-opacity="0"/></radialGradient>
    <pattern id="grade" width="32" height="32" patternUnits="userSpaceOnUse"><path d="M32 0H0V32" fill="none" stroke="#ffffff" stroke-opacity=".035"/></pattern>
  </defs>
  <style>
    ${entra}
    .flutua { animation: flutua 5s ease-in-out infinite; }
    @keyframes flutua { 0%,100% { transform: translate(0,0) rotate(-8deg); } 50% { transform: translate(0,-10px) rotate(-6deg); } }
    .reflexo { animation: reflexo 4.5s ease-in-out infinite; }
    @keyframes reflexo { 0% { transform: translateX(0) skewX(-20deg); } 60%,100% { transform: translateX(900px) skewX(-20deg); } }
    .linha { opacity: 0; animation: entra .5s ease-out forwards; }
    .toast { opacity: 0; animation: toast 6s ease-in-out 2.4s infinite; }
    @keyframes toast { 0% { opacity: 0; transform: translateY(-12px); } 8%,40% { opacity: 1; transform: none; } 50%,100% { opacity: 0; transform: translateY(-12px); } }
    .pulso { animation: pulso 2s ease-in-out infinite; }
    @keyframes pulso { 0%,100% { opacity: .35; } 50% { opacity: 1; } }
  </style>
  <rect width="1200" height="440" rx="20" fill="${FUNDO}"/>
  <rect width="1200" height="440" rx="20" fill="url(#grade)"/>
  <rect width="1200" height="440" rx="20" fill="url(#luzAzul)"/>
  <rect width="1200" height="440" rx="20" fill="url(#luzCiano)"/>
  <rect x=".5" y=".5" width="1199" height="439" rx="20" fill="none" stroke="${BORDA}"/>

  <g transform="translate(64 0)">
    <rect x="0" y="62" width="196" height="30" rx="15" fill="${AZUL}" fill-opacity=".12" stroke="${AZUL}" stroke-opacity=".5" class="entra"/>
    <circle cx="18" cy="77" r="4" fill="${VERDE}" class="pulso"/>
    <text x="32" y="82" class="entra" style="font: 600 13px ${SANS}" fill="${TEXTO}">MVP em construção · 31/10</text>
    <text x="0" y="158" class="entra" style="font: 800 64px ${SANS}; letter-spacing: 10px; animation-delay:.15s" fill="${TEXTO}">RKF</text>
    <text x="196" y="158" class="entra" style="font: 300 40px ${SANS}; animation-delay:.25s" fill="url(#marca)">Banco Digital</text>
    <text x="2" y="200" class="entra" style="font: 600 20px ${SANS}; animation-delay:.4s" fill="${CIANO}">Confiança. Tecnologia. Liberdade.</text>
    <text x="2" y="240" class="entra" style="font: 400 16px ${SANS}; animation-delay:.55s" fill="${APAGADO}">Conta digital sem mensalidade, transferência na hora</text>
    <text x="2" y="263" class="entra" style="font: 400 16px ${SANS}; animation-delay:.6s" fill="${APAGADO}">e extrato em tempo real. Feito em Osasco, SP. 🏙️</text>
    ${[['☕ Java 25', 0], ['🍃 Spring Boot 4', 118], ['🔐 JWT', 272], ['🐬 MySQL', 352], ['⚛️ Next.js', 446]].map(([t, dx], k) => `
    <g class="entra" style="animation-delay:${(0.75 + k * 0.08).toFixed(2)}s">
      <rect x="${dx}" y="296" width="${t.length * 8.4 + 18}" height="30" rx="8" fill="${SUPERFICIE}" stroke="${BORDA}"/>
      <text x="${dx + 10}" y="316" style="font: 600 13px ${SANS}" fill="${TEXTO}">${t}</text>
    </g>`).join('')}
    <text x="2" y="372" class="entra" style="font: 400 13px ${SANS}; animation-delay:1.2s" fill="${APAGADO}">Kevin Richard · backend   ·   Renan Barros · frontend   ·   Fellipe Santana · idealizador e 1º cliente</text>
  </g>

  <g class="flutua" style="transform-origin: 800px 250px">${cartao(600, 176, 300, 'FELLIPE SANTANA', '4821')}</g>
  <g transform="translate(918 24) scale(.84)">${celular(0, 0, tela)}</g>
  <g class="toast">
    <rect x="682" y="44" width="236" height="52" rx="14" fill="${SUPERFICIE}" stroke="${VERDE}" stroke-opacity=".6"/>
    <circle cx="706" cy="70" r="12" fill="${VERDE}" fill-opacity=".18"/>
    <text x="706" y="75" text-anchor="middle" style="font: 700 13px ${SANS}" fill="${VERDE}">✓</text>
    <text x="726" y="66" style="font: 700 12px ${SANS}" fill="${TEXTO}">Transferência recebida</text>
    <text x="726" y="84" style="font: 400 11px ${SANS}" fill="${APAGADO}">+ R$ 50,00 · agora</text>
  </g>
</svg>
`;
}

// ---------- 🔘 botões ----------

function botao(texto, { cor = AZUL, destaque = false, sub = '' } = {}) {
  const h = sub ? 64 : 46;
  const largura = sub ? 360 : 22 + Math.round(texto.length * 8.4) + 40;
  return `${cabecalho(largura, h, texto + (sub ? `: ${sub}` : ''))}
  <defs>${marca}
    <linearGradient id="brilho" x1="0" x2="1"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff" stop-opacity=".16"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
    <clipPath id="forma"><rect x="1" y="1" width="${largura - 2}" height="${h - 2}" rx="${sub ? 16 : h / 2 - 1}"/></clipPath>
  </defs>
  <style>
    .brilho { animation: passa 3.4s ease-in-out infinite; }
    @keyframes passa { 0% { transform: translateX(-${largura}px); } 60%,100% { transform: translateX(${largura}px); } }
    .seta { animation: seta 1.4s ease-in-out infinite; }
    @keyframes seta { 0%,100% { transform: translateX(0); } 50% { transform: translateX(4px); } }
  </style>
  <g clip-path="url(#forma)">
    <rect width="${largura}" height="${h}" fill="${destaque ? 'url(#marca)' : FUNDO}"/>
    <rect width="${largura}" height="${h}" fill="${cor}" opacity="${destaque ? 0 : 0.1}"/>
    <rect class="brilho" width="${Math.round(largura / 2)}" height="${h}" fill="url(#brilho)"/>
  </g>
  <rect x="1" y="1" width="${largura - 2}" height="${h - 2}" rx="${sub ? 16 : h / 2 - 1}" fill="none" stroke="${destaque ? CIANO : cor}" stroke-width="1.5"/>
  <text x="22" y="${sub ? 28 : 29}" style="font: 700 15px ${SANS}" fill="${TEXTO}">${xml(texto)}</text>
  ${sub ? `<text x="22" y="48" style="font: 400 12px ${SANS}" fill="${destaque ? '#fff' : APAGADO}">${xml(sub)}</text>` : ''}
  <text x="${largura - 30}" y="${sub ? 38 : 29}" style="font: 700 16px ${SANS}" fill="${destaque ? '#fff' : cor}" class="seta">›</text>
</svg>
`;
}

const BOTOES = {
  'abrir-conta': ['🏦 Conheça o banco', { destaque: true }],
  experiencia: ['📱 Experiência', {}],
  seguranca: ['🛡️ Segurança', { cor: CIANO }],
  api: ['🔌 API', {}],
  rodar: ['🚀 Rodar local', { cor: CIANO }],
  qualidade: ['🧪 Qualidade', {}],
  roadmap: ['🗺️ Roadmap', { cor: CIANO }],
  time: ['👥 Time', {}],
  'trilha-recrutador': ['🧑‍💼 Sou recrutador(a)', { sub: 'tenho 1 minuto: o que esse projeto prova?' }],
  'trilha-dev': ['👩‍💻 Sou dev', { sub: 'quero ver código, testes e decisões' }],
  'trilha-cliente': ['💳 Quero ser cliente', { sub: 'o que o RKF Bank faz por mim?', destaque: true }],
  'trilha-time': ['🤝 Sou do time', { sub: 'como rodo, testo e entrego?' }],
};

// ---------- 🔢 números ----------

function numeros() {
  const itens = [
    [NUMEROS.testes, 'testes automatizados', VERDE],
    [NUMEROS.rotas, 'rotas REST', AZUL],
    [NUMEROS.migracoes, 'migrações Flyway', CIANO],
    [15, 'minutos de vida do JWT', AZUL],
    [NUMEROS.workflows, 'workflows de CI', CIANO],
    [NUMEROS.time, 'pessoas de Osasco', VERDE],
  ];
  const w = 1200 / itens.length;
  const cards = itens.map(([n, rotulo, cor], i) => `
  <g class="entra" style="animation-delay:${(0.1 + i * 0.12).toFixed(2)}s">
    <rect x="${i * w + 8}" y="8" width="${w - 16}" height="124" rx="16" fill="${SUPERFICIE}" stroke="${BORDA}"/>
    <rect x="${i * w + 8}" y="8" width="${w - 16}" height="4" rx="2" fill="${cor}"/>
    <text x="${i * w + w / 2}" y="78" text-anchor="middle" class="num" style="font: 800 44px ${SANS}; animation-delay:${(0.3 + i * 0.12).toFixed(2)}s" fill="${TEXTO}">${n}</text>
    <text x="${i * w + w / 2}" y="110" text-anchor="middle" style="font: 500 13px ${SANS}" fill="${APAGADO}">${rotulo}</text>
  </g>`).join('');
  return `${cabecalho(1200, 140, itens.map(([n, r]) => `${n} ${r}`).join(', '))}
  <style>
    ${entra}
    .num { animation: pop .6s cubic-bezier(.2,1.6,.4,1) both; transform-box: fill-box; transform-origin: center; }
    @keyframes pop { from { transform: scale(.4); opacity: 0; } to { transform: scale(1); opacity: 1; } }
  </style>
  ${cards}
</svg>
`;
}

// ---------- 📜 extrato animado ----------

function extrato() {
  const linhas = [
    ['12/10 · 09:14', 'Depósito', 'Depósito inicial', '+ 150,00', VERDE, '150,00'],
    ['12/10 · 12:30', 'Saque', 'Caixa 24h', '− 20,00', VERMELHO, '130,00'],
    ['13/10 · 08:02', 'Transferência enviada', 'Aluguel', '− 50,00', VERMELHO, '80,00'],
    ['14/10 · 18:45', 'Transferência recebida', 'Racha da pizza 🍕', '+ 35,00', VERDE, '115,00'],
  ];
  const corpo = linhas.map(([quando, tipo, desc, valor, cor, saldo], i) => `
    <g class="linha" style="animation-delay:${(0.5 + i * 0.6).toFixed(1)}s">
      <rect x="40" y="${96 + i * 66}" width="760" height="56" rx="12" fill="${SUPERFICIE}" stroke="${BORDA}"/>
      <circle cx="74" cy="${124 + i * 66}" r="16" fill="${cor}" fill-opacity=".15"/>
      <text x="74" y="${130 + i * 66}" text-anchor="middle" style="font: 700 15px ${SANS}" fill="${cor}">${valor.startsWith('+') ? '↓' : '↑'}</text>
      <text x="104" y="${119 + i * 66}" style="font: 600 15px ${SANS}" fill="${TEXTO}">${tipo}</text>
      <text x="104" y="${139 + i * 66}" style="font: 400 12px ${SANS}" fill="${APAGADO}">${xml(desc)} · ${quando}</text>
      <text x="780" y="${129 + i * 66}" text-anchor="end" style="font: 700 16px ${MONO}" fill="${cor}">${valor}</text>
    </g>
    <text class="s${i}" x="1010" y="232" text-anchor="middle" style="font: 800 40px ${SANS}" fill="${TEXTO}">R$ ${saldo}</text>`).join('');
  // Cada saldo aparece junto com a sua linha e some quando a próxima chega; o último fica até o ciclo de 8 s recomeçar.
  const n = linhas.length;
  const pct = (s) => ((s / 8) * 100).toFixed(1);
  const fatias = linhas.map((_, i) => {
    const inicio = 0.5 + i * 0.6;
    const fim = i === n - 1 ? 7.4 : inicio + 0.6;
    return `.s${i} { opacity: 0; animation: s${i} 8s linear infinite; }
    @keyframes s${i} { 0%, ${pct(inicio)}% { opacity: 0; } ${pct(inicio + 0.1)}%, ${pct(fim)}% { opacity: 1; } ${pct(fim + 0.05)}%, 100% { opacity: 0; } }`;
  }).join('\n    ');
  return `${cabecalho(1200, 380, 'Extrato do RKF Bank: depósito de 150, saque de 20, transferência enviada de 50 e recebida de 35; o saldo atualiza a cada lançamento e fecha em 115 reais.')}
  <defs>${marca}</defs>
  <style>
    .linha { opacity: 0; animation: linha 8s ease-out infinite; }
    @keyframes linha { 0% { opacity: 0; transform: translateX(-14px); } 6%, 92% { opacity: 1; transform: none; } 100% { opacity: 0; } }
    ${fatias}
  </style>
  <rect width="1200" height="380" rx="20" fill="${FUNDO}" stroke="${BORDA}"/>
  <text x="40" y="54" style="font: 700 22px ${SANS}" fill="${TEXTO}">📜 Extrato</text>
  <text x="170" y="54" style="font: 400 14px ${SANS}" fill="${APAGADO}">GET /contas/{contaId}/lancamentos · mais novo primeiro na API, aqui na ordem em que aconteceu</text>
  <rect x="860" y="96" width="300" height="252" rx="16" fill="${SUPERFICIE}" stroke="${BORDA}"/>
  <text x="1010" y="140" text-anchor="middle" style="font: 500 14px ${SANS}" fill="${APAGADO}">Saldo da conta</text>
  <text x="1010" y="290" text-anchor="middle" style="font: 400 12px ${SANS}" fill="${APAGADO}">BigDecimal · 2 casas · @Version</text>
  <rect x="900" y="306" width="220" height="4" rx="2" fill="url(#marca)"/>
  ${corpo}
</svg>
`;
}

// ---------- 🔁 fluxo de uma transferência ----------

function fluxo() {
  const etapas = [
    ['🌐', 'Next.js (BFF)', 'cookie httpOnly → Bearer'],
    ['🛡️', 'Spring Security', 'JWT válido? senão 401'],
    ['🎯', 'ContaController', '@Valid no JSON → 400'],
    ['⚙️', 'ContaService', 'dono? 403 · saldo? 422'],
    ['🗄️', 'MySQL', '@Transactional + @Version'],
  ];
  const w = 1200 / etapas.length;
  const caixas = etapas.map(([ic, t, s], i) => `
  <g class="entra" style="animation-delay:${(0.1 + i * 0.15).toFixed(2)}s">
    <rect x="${i * w + 14}" y="70" width="${w - 28}" height="108" rx="16" fill="${SUPERFICIE}" stroke="${BORDA}"/>
    <rect class="acende" style="animation-delay:${(i * 0.7).toFixed(1)}s" x="${i * w + 14}" y="70" width="${w - 28}" height="108" rx="16" fill="none" stroke="${CIANO}" stroke-width="2"/>
    <text x="${i * w + w / 2}" y="108" text-anchor="middle" style="font: 400 24px ${SANS}">${ic}</text>
    <text x="${i * w + w / 2}" y="138" text-anchor="middle" style="font: 700 15px ${SANS}" fill="${TEXTO}">${t}</text>
    <text x="${i * w + w / 2}" y="160" text-anchor="middle" style="font: 400 12px ${SANS}" fill="${APAGADO}">${s}</text>
  </g>`).join('');
  return `${cabecalho(1200, 250, 'Uma transferência passa pelo Next.js, pelo Spring Security, pelo controller, pelo service e pelo MySQL numa única transação, e volta com 204.')}
  <defs>${marca}
    <radialGradient id="bola"><stop offset="0" stop-color="#fff"/><stop offset=".4" stop-color="${CIANO}"/><stop offset="1" stop-color="${CIANO}" stop-opacity="0"/></radialGradient>
  </defs>
  <style>
    ${entra}
    .vai { animation: vai 3.5s cubic-bezier(.6,0,.4,1) infinite; }
    @keyframes vai { 0% { transform: translateX(0); opacity: 0; } 5% { opacity: 1; } 85% { transform: translateX(960px); opacity: 1; } 100% { transform: translateX(960px); opacity: 0; } }
    .volta { animation: volta 3.5s ease-in-out infinite; opacity: 0; }
    @keyframes volta { 0%, 85% { opacity: 0; } 92%, 100% { opacity: 1; } }
    .acende { opacity: 0; animation: acende 3.5s ease-in-out infinite; }
    @keyframes acende { 0% { opacity: 0; } 8% { opacity: 1; } 22%, 100% { opacity: 0; } }
  </style>
  <rect width="1200" height="250" rx="20" fill="${FUNDO}" stroke="${BORDA}"/>
  <text x="40" y="46" style="font: 700 20px ${SANS}" fill="${TEXTO}">🔁 POST /contas/transferencia</text>
  <text x="350" y="46" style="font: 400 14px ${SANS}" fill="${APAGADO}">débito, crédito e lançamento: tudo ou nada</text>
  <path d="M${w / 2} 124 H${1200 - w / 2}" stroke="url(#marca)" stroke-width="2" stroke-dasharray="6 8" opacity=".5"/>
  ${caixas}
  <circle class="vai" cx="${w / 2}" cy="124" r="14" fill="url(#bola)"/>
  <g class="volta">
    <rect x="452" y="200" width="296" height="34" rx="17" fill="${VERDE}" fill-opacity=".14" stroke="${VERDE}"/>
    <text x="600" y="223" text-anchor="middle" style="font: 700 14px ${SANS}" fill="${VERDE}">✓ 204 No Content · X-Request-Id no log</text>
  </g>
</svg>
`;
}

// ---------- 🛡️ camadas de segurança ----------

function seguranca() {
  const camadas = [
    ['🔑', 'Senha em BCrypt', 'hash com salt, custo 10; a senha nunca chega ao banco', AZUL],
    ['🎫', 'JWT HS256 · 15 min', 'OAuth2 Resource Server; alg none e payload adulterado → 401', CIANO],
    ['🍪', 'Cookie httpOnly', 'o JavaScript da página não alcança o token (XSS)', AZUL],
    ['👤', 'Regra de dono', 'conta de outro cliente → 403, sem vazar saldo', CIANO],
    ['🔒', 'Lock otimista', 'duas operações ao mesmo tempo → 409, saldo nunca errado', AZUL],
    ['🙈', 'Logs sem dado pessoal', 'nada de CPF, e-mail, senha ou token; X-Request-Id em tudo', CIANO],
  ];
  const linhas = camadas.map(([ic, t, s, cor], i) => `
  <g class="entra" style="animation-delay:${(0.2 + i * 0.18).toFixed(2)}s">
    <rect x="${300 + i * 10}" y="${40 + i * 52}" width="${860 - i * 20}" height="44" rx="12" fill="${SUPERFICIE}" stroke="${cor}" stroke-opacity=".45"/>
    <text x="${322 + i * 10}" y="${68 + i * 52}" style="font: 400 18px ${SANS}">${ic}</text>
    <text x="${356 + i * 10}" y="${67 + i * 52}" style="font: 700 15px ${SANS}" fill="${TEXTO}">${t}</text>
    <text x="${560 + i * 10}" y="${67 + i * 52}" style="font: 400 13px ${SANS}" fill="${APAGADO}">${xml(s)}</text>
  </g>`).join('');
  return `${cabecalho(1200, 370, 'Camadas de segurança do RKF Bank: BCrypt, JWT, cookie httpOnly, regra de dono, lock otimista e logs sem dados pessoais.')}
  <defs>${marca}
    <linearGradient id="escudo" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${AZUL}"/><stop offset="1" stop-color="${CIANO}"/></linearGradient>
  </defs>
  <style>
    ${entra}
    .escudo { animation: respira 3s ease-in-out infinite; transform-box: fill-box; transform-origin: center; }
    @keyframes respira { 0%,100% { transform: scale(1); } 50% { transform: scale(1.04); } }
    .onda { animation: onda 2.6s ease-out infinite; transform-box: fill-box; transform-origin: center; }
    @keyframes onda { 0% { transform: scale(.9); opacity: .6; } 100% { transform: scale(1.5); opacity: 0; } }
  </style>
  <rect width="1200" height="370" rx="20" fill="${FUNDO}" stroke="${BORDA}"/>
  <g transform="translate(70 70)">
    <path class="onda" d="M80 0 L160 30 V110 C160 170 120 200 80 220 C40 200 0 170 0 110 V30 Z" fill="none" stroke="${CIANO}" stroke-width="2"/>
    <path class="escudo" d="M80 0 L160 30 V110 C160 170 120 200 80 220 C40 200 0 170 0 110 V30 Z" fill="url(#escudo)" fill-opacity=".9"/>
    <path d="M48 112 L72 136 L116 86" stroke="#fff" stroke-width="10" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
    <text x="80" y="262" text-anchor="middle" style="font: 700 16px ${SANS}" fill="${TEXTO}">6 camadas</text>
  </g>
  ${linhas}
</svg>
`;
}

// ---------- 👥 time (avatares) ----------

const PELE_KEVIN = '#5a3520';
const PELE_FELLIPE = '#6b4226';
const PELE_RENAN = '#f0c8a0';

function rosto(pele, sombra) {
  return `<rect x="42" y="62" width="16" height="18" rx="5" fill="${sombra}"/>
      <circle cx="28" cy="46" r="5.5" fill="${sombra}"/>
      <circle cx="72" cy="46" r="5.5" fill="${sombra}"/>
      <ellipse cx="50" cy="44" rx="22" ry="25" fill="${pele}"/>
      <circle cx="42" cy="45" r="2.4" fill="#0d0d0d"/>
      <circle cx="58" cy="45" r="2.4" fill="#0d0d0d"/>`;
}

function corpo(roupa, detalhe) {
  return `<ellipse cx="50" cy="121" rx="30" ry="3" fill="#000" opacity=".35"/>
      <path d="M12 120 C12 90 29 78 50 78 C71 78 88 90 88 120 Z" fill="${roupa}"/>
      <path d="M50 80 L50 120" stroke="${detalhe}" stroke-width="2.5"/>
      <path d="M38 80 Q50 92 62 80" fill="none" stroke="#0a1730" stroke-width="3"/>`;
}

const AVATARES = {
  // Kevin: homem negro, cabelo raspado na máquina 2, bigode e barbicha.
  kevin: () => `${corpo('#1d4ed8', CIANO)}
      ${rosto(PELE_KEVIN, '#45281a')}
      <path d="M27.5 41 C25.5 22 36 17.5 50 17.5 C64 17.5 74.5 22 72.5 41 C70 30 61 27 50 27 C39 27 30 30 27.5 41 Z" fill="#121212" opacity=".92"/>
      <path d="M38 37 h7 M55 37 h7" stroke="#121212" stroke-width="2.4" stroke-linecap="round"/>
      <path d="M40.5 54 C43 51.5 47.5 52.5 50 54.2 C52.5 52.5 57 51.5 59.5 54 C57 56.6 53 56 50 55.4 C47 56 43 56.6 40.5 54 Z" fill="#121212"/>
      <path d="M45 63.5 C46 66 48 69.5 50 70.5 C52 69.5 54 66 55 63.5 C53 64.6 47 64.6 45 63.5 Z" fill="#121212"/>
      <path d="M45 59.4 Q50 62.6 55 59.4" stroke="#f2e6dc" stroke-width="1.8" fill="none" stroke-linecap="round"/>`,
  // Renan: homem branco, cabelo cacheado.
  renan: () => `${corpo('#0e7490', AZUL)}
      ${rosto(PELE_RENAN, '#d9a982')}
      ${[[30, 30, 8], [38, 22, 8.5], [48, 18, 9], [58, 19, 8.5], [67, 25, 8], [72, 34, 6.5], [27, 38, 6], [43, 27, 6], [54, 26, 6], [63, 30, 5.5]]
        .map(([cx, cy, r]) => `<circle cx="${cx}" cy="${cy}" r="${r}" fill="#3b2314"/>`).join('')}
      ${[[34, 24], [46, 16], [60, 18], [69, 28]].map(([cx, cy]) => `<path d="M${cx - 4} ${cy} q4 -4 8 0" stroke="#5a3a24" stroke-width="1.4" fill="none"/>`).join('')}
      <path d="M38 38 h7 M55 38 h7" stroke="#3b2314" stroke-width="2.2" stroke-linecap="round"/>
      <path d="M44 58 Q50 62.5 56 58" stroke="#9a4a3a" stroke-width="1.8" fill="none" stroke-linecap="round"/>`,
  // Fellipe: homem negro, moicano black power.
  fellipe: () => `${corpo('#0f172a', '#e2c98a')}
      ${rosto(PELE_FELLIPE, '#4f2f1b')}
      <path d="M29 40 C28 30 33 25 37 24 M71 40 C72 30 67 25 63 24" stroke="#1a1a1a" stroke-width="1.6" fill="none" opacity=".6"/>
      <ellipse cx="50" cy="12" rx="15" ry="20" fill="#141414"/>
      ${[[40, 0], [46, -6], [54, -6], [60, 0], [38, 10], [62, 10], [50, -9]].map(([cx, cy]) => `<circle cx="${cx}" cy="${cy}" r="6" fill="#141414"/>`).join('')}
      <path d="M38 38 h7 M55 38 h7" stroke="#141414" stroke-width="2.4" stroke-linecap="round"/>
      <path d="M44 58 Q50 63 56 58" stroke="#f2e6dc" stroke-width="1.8" fill="none" stroke-linecap="round"/>`,
};

function time() {
  const pessoas = [
    ['kevin', 'Kevin Richard', 'Backend', 'API, banco, segurança, testes', AZUL, '☕'],
    ['fellipe', 'Fellipe Santana', 'Idealizador · PO & QA', '1º cliente: testa antes de todo mundo', '#e2c98a', '💡'],
    ['renan', 'Renan Barros', 'Frontend', 'telas, marca e experiência', CIANO, '⚛️'],
  ];
  const cards = pessoas.map(([id, nome, papel, desc, cor, ic], i) => `
  <g class="entra" style="animation-delay:${(0.15 + i * 0.2).toFixed(2)}s">
    <rect x="${40 + i * 380}" y="60" width="360" height="300" rx="20" fill="${SUPERFICIE}" stroke="${BORDA}"/>
    <rect x="${40 + i * 380}" y="60" width="360" height="5" rx="2" fill="${cor}"/>
    <circle cx="${220 + i * 380}" cy="160" r="74" fill="${cor}" fill-opacity=".1" stroke="${cor}" stroke-opacity=".4"/>
    <g class="${i === 1 ? 'acena' : 'respira'}" style="animation-delay:${(i * 0.4).toFixed(1)}s">
      <g transform="translate(${160 + i * 380} 96) scale(1.2)">${AVATARES[id]()}</g>
    </g>
    <text x="${220 + i * 380}" y="276" text-anchor="middle" style="font: 800 20px ${SANS}" fill="${TEXTO}">${xml(nome)}</text>
    <rect x="${220 + i * 380 - (papel.length * 4.3 + 22)}" y="290" width="${papel.length * 8.6 + 44}" height="26" rx="13" fill="${cor}" fill-opacity=".14" stroke="${cor}" stroke-opacity=".6"/>
    <text x="${220 + i * 380}" y="308" text-anchor="middle" style="font: 700 12.5px ${SANS}" fill="${TEXTO}">${ic} ${xml(papel)}</text>
    <text x="${220 + i * 380}" y="340" text-anchor="middle" style="font: 400 13px ${SANS}" fill="${APAGADO}">${xml(desc)}</text>
  </g>`).join('');
  return `${cabecalho(1200, 390, 'O time do RKF Bank, todos de Osasco, SP: Kevin Richard no backend, Fellipe Santana idealizador e primeiro cliente, Renan Barros no frontend.')}
  <style>
    ${entra}
    .respira { animation: respira 2.8s ease-in-out infinite; }
    @keyframes respira { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-4px); } }
    .acena { animation: acena 2.2s ease-in-out infinite; }
    @keyframes acena { 0%,100% { transform: translateY(0); } 25% { transform: translateY(-6px); } 50% { transform: translateY(0); } 75% { transform: translateY(-3px); } }
  </style>
  <rect width="1200" height="390" rx="20" fill="${FUNDO}" stroke="${BORDA}"/>
  <text x="40" y="40" style="font: 700 20px ${SANS}" fill="${TEXTO}">👥 Quem faz o RKF Bank</text>
  <text x="1160" y="40" text-anchor="end" style="font: 500 14px ${SANS}" fill="${APAGADO}">📍 todos de Osasco, SP</text>
  ${cards}
</svg>
`;
}

// ---------- 💻 terminal ----------

function terminal() {
  const linhas = [
    ['$ scripts/validar.sh backend', TEXTO],
    ['>>> Backend: ./mvnw clean verify (perfil test)', APAGADO],
    ['[INFO] Flyway: Successfully applied 3 migrations (V1, V2, V3)', CIANO],
    ['[INFO] SegurancaIntegracaoTest .... alg none → 401 ✔  conta de outro → 403 ✔', APAGADO],
    ['[INFO] ContaRepositoryTest ........ @Version → OptimisticLock ✔', APAGADO],
    [`    Tests run: ${NUMEROS.testes}, Failures: 0, Errors: 0, Skipped: 0`, TEXTO],
    ['    ✅ BUILD SUCCESS (relatório de cobertura: target/site/jacoco/index.html)', VERDE],
  ];
  const corpo = linhas.map(([l, c], i) => `<text x="28" y="${74 + i * 28}" class="ln" style="font: 400 15px ${MONO}; animation-delay:${(0.3 + i * 0.45).toFixed(2)}s" fill="${c}" xml:space="preserve">${xml(l)}</text>`).join('\n  ');
  return `${cabecalho(1200, 290, `Terminal rodando os testes do RKF Bank: ${NUMEROS.testes} testes, 0 falhas, BUILD SUCCESS.`)}
  <style>
    .ln { opacity: 0; animation: mostra .3s ease-out forwards; }
    @keyframes mostra { from { opacity: 0; transform: translateX(-6px); } to { opacity: 1; transform: none; } }
    .cursor { animation: pisca 1s step-end infinite; }
    @keyframes pisca { 50% { opacity: 0; } }
  </style>
  <rect width="1200" height="290" rx="14" fill="#05080f" stroke="${BORDA}"/>
  <rect width="1200" height="38" rx="14" fill="${SUPERFICIE}"/><rect y="26" width="1200" height="12" fill="${SUPERFICIE}"/>
  <circle cx="22" cy="19" r="6" fill="#ff5f56"/><circle cx="42" cy="19" r="6" fill="#ffbd2e"/><circle cx="62" cy="19" r="6" fill="#27c93f"/>
  <text x="600" y="24" text-anchor="middle" style="font: 600 13px ${SANS}" fill="${APAGADO}">rkf_bank — bash</text>
  ${corpo}
  <rect x="28" y="${74 + linhas.length * 28 - 10}" width="10" height="16" fill="${CIANO}" class="cursor"/>
</svg>
`;
}

// ---------- ➖ divisor e 📣 chamada ----------

function divisor() {
  return `${cabecalho(1200, 14, 'divisor')}
  <defs>${marca}
    <radialGradient id="luz"><stop offset="0" stop-color="#fff" stop-opacity=".95"/><stop offset=".35" stop-color="${CIANO}" stop-opacity=".8"/><stop offset="1" stop-color="${CIANO}" stop-opacity="0"/></radialGradient>
  </defs>
  <style>
    .luz { animation: corre 4.5s ease-in-out infinite; }
    @keyframes corre { 0% { transform: translateX(-60px); } 100% { transform: translateX(1260px); } }
  </style>
  <rect y="6" width="1200" height="2" rx="1" fill="url(#marca)" opacity=".85"/>
  <ellipse class="luz" cx="0" cy="7" rx="60" ry="6" fill="url(#luz)"/>
</svg>
`;
}

function chamada() {
  return `${cabecalho(1200, 200, 'Mais que um banco, um parceiro para o seu amanhã. Acompanhe o RKF Bank até o lançamento do MVP em 31/10.')}
  <defs>${marca}
    <radialGradient id="g1" cx=".15" cy=".3" r=".7"><stop offset="0" stop-color="${AZUL}" stop-opacity=".45"/><stop offset="1" stop-color="${AZUL}" stop-opacity="0"/></radialGradient>
    <radialGradient id="g2" cx=".9" cy=".8" r=".6"><stop offset="0" stop-color="${CIANO}" stop-opacity=".3"/><stop offset="1" stop-color="${CIANO}" stop-opacity="0"/></radialGradient>
  </defs>
  <style>
    ${entra}
    .onda { animation: onda 2.4s ease-out infinite; transform-box: fill-box; transform-origin: center; }
    @keyframes onda { 0% { transform: scale(1); opacity: .7; } 100% { transform: scale(1.3); opacity: 0; } }
    .reflexo { animation: reflexo 4.5s ease-in-out infinite; }
    @keyframes reflexo { 0% { transform: translateX(0) skewX(-20deg); } 60%,100% { transform: translateX(600px) skewX(-20deg); } }
  </style>
  <rect width="1200" height="200" rx="18" fill="${FUNDO}" stroke="${BORDA}"/>
  <rect width="1200" height="200" rx="18" fill="url(#g1)"/><rect width="1200" height="200" rx="18" fill="url(#g2)"/>
  <g transform="translate(46 38) scale(.62)">${cartao(0, 0, 300, 'VOCÊ AQUI', '0001')}</g>
  <text x="280" y="86" class="entra" style="font: 800 32px ${SANS}; animation-delay:.15s" fill="#fff">Mais que um banco, um parceiro para o seu amanhã.</text>
  <text x="282" y="122" class="entra" style="font: 400 16px ${SANS}; animation-delay:.35s" fill="${APAGADO}">Acompanhe o RKF Bank até o lançamento do MVP em 31/10/2026 ⭐</text>
  <g transform="translate(900 136)">
    <rect width="250" height="44" rx="22" fill="none" stroke="${CIANO}" stroke-width="2" class="onda"/>
    <rect width="250" height="44" rx="22" fill="url(#marca)"/>
    <text x="125" y="28" text-anchor="middle" style="font: 700 15px ${SANS}" fill="#fff">Ver o roadmap  ›</text>
  </g>
</svg>
`;
}

// ---------- gravação ----------

const arquivos = {
  'banner.svg': banner(),
  'numeros.svg': numeros(),
  'extrato.svg': extrato(),
  'fluxo-transferencia.svg': fluxo(),
  'seguranca.svg': seguranca(),
  'time.svg': time(),
  'terminal.svg': terminal(),
  'divisor.svg': divisor(),
  'chamada.svg': chamada(),
  ...Object.fromEntries(Object.entries(BOTOES).map(([nome, [texto, opcoes]]) => [`botoes/${nome}.svg`, botao(texto, opcoes)])),
};

for (const [nome, conteudo] of Object.entries(arquivos)) {
  const destino = path.join(SAIDA, nome);
  fs.mkdirSync(path.dirname(destino), { recursive: true });
  fs.writeFileSync(destino, conteudo);
}
console.log(`${Object.keys(arquivos).length} SVGs em assets/readme/ (testes=${NUMEROS.testes}, migracoes=${NUMEROS.migracoes}, workflows=${NUMEROS.workflows})`);
