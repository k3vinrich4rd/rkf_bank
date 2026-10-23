import * as THREE from "three";
import { RoomEnvironment } from "three/examples/jsm/environments/RoomEnvironment.js";
import { estadoInicialDoCartao } from "./estado-inicial";

/**
 * Cartão RKF (azul, laqueado), desenhado com Three.js num `<canvas>` transparente.
 *
 * A câmera é calibrada para que 1 unidade 3D = 1 px do palco: quem anima o cartão (a timeline do scroll)
 * trabalha em pixels medidos no DOM, sem conversão. Tudo que muda a cada quadro está em `estado`.
 */
export interface EstadoCartao {
  /** Centro do cartão, em px do palco (origem no canto superior esquerdo). */
  cx: number;
  cy: number;
  /** Largura do cartão em px. */
  len: number;
  /** Rotações em radianos. `ry = π` mostra o verso. */
  rx: number;
  ry: number;
  rz: number;
  /** Giro do ambiente refletido (move os reflexos pelo metal). */
  env: number;
  /** Opacidade da sombra que cai atrás do cartão (0 a 1). */
  shadow: number;
  /** Quanto o cursor e a flutuação influenciam (0 a 1). */
  tilt: number;
  /** Posição da luz principal, de -1 (esquerda) a 1 (direita). */
  light: number;
}

export interface Cartao {
  estado: EstadoCartao;
  render(): void;
  resize(): void;
  destruir(): void;
}

interface Opcoes {
  canvas: HTMLCanvasElement;
  palco: HTMLElement;
  reduzido: boolean;
  titular: string;
  final: string;
  /** Famílias de fonte já carregadas no documento (o `next/font` gera nomes com hash). */
  fontes: Fontes;
  /** Estado animado por fora (a timeline do scroll pode existir antes do cartão). */
  estado?: EstadoCartao;
}

export interface Fontes {
  /** Plus Jakarta Sans, a mesma do `BankCard` e do resto do site. */
  texto: string;
  larga: string;
  mono: string;
}

/** Proporção do cartão de crédito (ISO/IEC 7810 ID-1: 85,60 × 53,98 mm). */
export const PROPORCAO_CARTAO = 85.6 / 53.98;
const ALTURA = 1 / PROPORCAO_CARTAO;
const ESPESSURA = 0.014;
const RAIO = 0.05;
const FOV = 28;

const TEXTURA_LARGURA = 2048;
const TEXTURA_ALTURA = Math.round(TEXTURA_LARGURA * ALTURA);

export function criarCartao({ canvas, palco, reduzido, titular, final, fontes, estado: estadoExterno }: Opcoes): Cartao {
  const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true, powerPreference: "high-performance" });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 0.92;
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFShadowMap;

  const cena = new THREE.Scene();
  const pmrem = new THREE.PMREMGenerator(renderer);
  const ambiente = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
  cena.environment = ambiente;
  cena.environmentIntensity = 0.8;

  const camera = new THREE.PerspectiveCamera(FOV, 1, 1, 10_000);

  const luz = new THREE.DirectionalLight(0xffffff, 2.2);
  luz.castShadow = true;
  luz.shadow.mapSize.set(1024, 1024);
  luz.shadow.radius = 8;
  luz.shadow.bias = -0.0005;
  cena.add(luz, luz.target);

  // Plano invisível que só recebe a sombra: no canvas transparente, ela cai "em cima" das letras do DOM.
  const materialSombra = new THREE.ShadowMaterial({ opacity: 0.4 });
  const planoSombra = new THREE.Mesh(new THREE.PlaneGeometry(1, 1), materialSombra);
  planoSombra.receiveShadow = true;
  cena.add(planoSombra);

  const texturas = criarTexturas(titular, final, fontes, renderer.capabilities.getMaxAnisotropy());
  const cartao = montarCartao(texturas);
  cena.add(cartao.grupo);

  const estado: EstadoCartao = estadoExterno ?? estadoInicialDoCartao();

  // Inclinação pelo cursor, com inércia: o alvo muda no evento, o valor real o persegue a cada quadro.
  const cursor = { alvoX: 0, alvoY: 0, x: 0, y: 0 };
  function aoMoverCursor(event: PointerEvent) {
    cursor.alvoX = (event.clientX / window.innerWidth) * 2 - 1;
    cursor.alvoY = (event.clientY / window.innerHeight) * 2 - 1;
  }
  if (!reduzido) window.addEventListener("pointermove", aoMoverCursor, { passive: true });

  let largura = 1;
  let altura = 1;
  function resize() {
    largura = Math.max(palco.clientWidth, 1);
    altura = Math.max(palco.clientHeight, 1);
    renderer.setSize(largura, altura, false);
    camera.aspect = largura / altura;
    const distancia = altura / 2 / Math.tan(THREE.MathUtils.degToRad(FOV / 2));
    camera.position.set(0, 0, distancia);
    camera.near = distancia / 20;
    camera.far = distancia * 4;
    camera.updateProjectionMatrix();
    planoSombra.scale.set(largura * 2, altura * 2, 1);
  }
  resize();

  // Só desenha quando o palco está na tela: depois do hero, o cartão para de custar GPU.
  let visivel = true;
  const observador = new IntersectionObserver(([entrada]) => {
    visivel = entrada?.isIntersecting ?? true;
  });
  observador.observe(palco);

  const relogio = new THREE.Timer();
  function render() {
    if (!visivel) return;
    relogio.update();
    const t = relogio.getElapsed();

    cursor.x += (cursor.alvoX - cursor.x) * 0.06;
    cursor.y += (cursor.alvoY - cursor.y) * 0.06;
    const influencia = reduzido ? 0 : estado.tilt;
    const flutuacao = Math.sin(t * 1.1) * estado.len * 0.018 * influencia;

    const { grupo } = cartao;
    grupo.position.set(estado.cx - largura / 2, altura / 2 - estado.cy + flutuacao, 0);
    grupo.scale.setScalar(estado.len);
    grupo.rotation.set(
      estado.rx - cursor.y * 0.22 * influencia,
      estado.ry + cursor.x * 0.32 * influencia,
      estado.rz + Math.sin(t * 0.7) * 0.025 * influencia,
    );

    // A sombra fica um pouco atrás do cartão; a luz anda com `light` e o reflexo do ambiente com `env`.
    planoSombra.position.z = -estado.len * 0.32;
    materialSombra.opacity = estado.shadow;
    luz.position.set(estado.light * altura * 0.8, altura * 0.9, camera.position.z * 0.9);
    luz.target.position.set(grupo.position.x, grupo.position.y, 0);
    ajustarCameraDaSombra(luz, estado.len);
    cena.environmentRotation.set(0.2, estado.env + cursor.x * 0.4 * influencia, 0);

    // O risco de brilho corre pelo metal conforme o cartão gira.
    const fase = grupo.rotation.y * 0.55 + grupo.rotation.x * 0.8 + estado.env * 0.35;
    cartao.brilho.uniforms.uPos.value = 0.5 + Math.sin(fase) * 0.85;

    renderer.render(cena, camera);
  }

  function destruir() {
    window.removeEventListener("pointermove", aoMoverCursor);
    observador.disconnect();
    cartao.destruir();
    texturas.destruir();
    planoSombra.geometry.dispose();
    materialSombra.dispose();
    ambiente.dispose();
    pmrem.dispose();
    renderer.dispose();
  }

  return { estado, render, resize, destruir };
}

/** A câmera ortográfica da sombra cobre só a área do cartão: sombra nítida sem mapa gigante. */
function ajustarCameraDaSombra(luz: THREE.DirectionalLight, len: number) {
  const cam = luz.shadow.camera;
  const meia = len * 1.2;
  if (cam.right === meia) return;
  cam.left = -meia;
  cam.right = meia;
  cam.top = meia;
  cam.bottom = -meia;
  cam.near = 1;
  cam.far = len * 20 + 5000;
  cam.updateProjectionMatrix();
}

// ── Geometria e materiais ───────────────────────────────────────────────

function formaArredondada(): THREE.Shape {
  const w = 1;
  const h = ALTURA;
  const x = -w / 2;
  const y = -h / 2;
  const forma = new THREE.Shape();
  forma.moveTo(x + RAIO, y);
  forma.lineTo(x + w - RAIO, y);
  forma.quadraticCurveTo(x + w, y, x + w, y + RAIO);
  forma.lineTo(x + w, y + h - RAIO);
  forma.quadraticCurveTo(x + w, y + h, x + w - RAIO, y + h);
  forma.lineTo(x + RAIO, y + h);
  forma.quadraticCurveTo(x, y + h, x, y + h - RAIO);
  forma.lineTo(x, y + RAIO);
  forma.quadraticCurveTo(x, y, x + RAIO, y);
  return forma;
}

/** `ShapeGeometry` usa as coordenadas da forma como UV; aqui elas viram 0..1 para a textura cobrir o cartão. */
function faceComUv(forma: THREE.Shape): THREE.ShapeGeometry {
  const geometria = new THREE.ShapeGeometry(forma, 16);
  const posicoes = geometria.attributes.position;
  const uv = geometria.attributes.uv;
  if (!posicoes || !uv) return geometria;
  for (let i = 0; i < posicoes.count; i++) {
    uv.setXY(i, posicoes.getX(i) + 0.5, posicoes.getY(i) / ALTURA + 0.5);
  }
  uv.needsUpdate = true;
  return geometria;
}

const SHADER_BRILHO = {
  vertex: /* glsl */ `
    varying vec2 vUv;
    void main() {
      vUv = uv;
      gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }`,
  fragment: /* glsl */ `
    uniform float uPos;
    varying vec2 vUv;
    void main() {
      float d = (vUv.x * 0.82 + vUv.y * 0.38) - uPos;
      float risco = exp(-d * d * 1800.0) * 0.28 + exp(-d * d * 40.0) * 0.05;
      gl_FragColor = vec4(vec3(0.92, 0.97, 1.0), risco);
    }`,
};

function montarCartao(texturas: Texturas) {
  const forma = formaArredondada();
  const grupo = new THREE.Group();

  const materialBorda = new THREE.MeshPhysicalMaterial({ color: 0x16336b, metalness: 0.6, roughness: 0.3, clearcoat: 1 });
  const corpo = new THREE.Mesh(
    new THREE.ExtrudeGeometry(forma, { depth: ESPESSURA, bevelEnabled: false, curveSegments: 16 }).translate(0, 0, -ESPESSURA / 2),
    materialBorda,
  );
  corpo.castShadow = true;

  const geometriaFace = faceComUv(forma);
  const materialFrente = materialDoCartao(texturas.frente, texturas.rugosidadeFrente);
  const materialVerso = materialDoCartao(texturas.verso, texturas.rugosidadeVerso);

  const frente = new THREE.Mesh(geometriaFace, materialFrente);
  frente.position.z = ESPESSURA / 2 + 0.0004;

  // O verso é a mesma face girada 180° em Y: com o cartão virado, a textura fica legível.
  const geometriaVerso = geometriaFace.clone().rotateY(Math.PI);
  const verso = new THREE.Mesh(geometriaVerso, materialVerso);
  verso.position.z = -ESPESSURA / 2 - 0.0004;

  const brilho = new THREE.ShaderMaterial({
    uniforms: { uPos: { value: 0.5 } },
    vertexShader: SHADER_BRILHO.vertex,
    fragmentShader: SHADER_BRILHO.fragment,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
  });
  const brilhoFrente = new THREE.Mesh(geometriaFace, brilho);
  brilhoFrente.position.z = ESPESSURA / 2 + 0.0009;
  const brilhoVerso = new THREE.Mesh(geometriaVerso, brilho);
  brilhoVerso.position.z = -ESPESSURA / 2 - 0.0009;

  grupo.add(corpo, frente, verso, brilhoFrente, brilhoVerso);

  function destruir() {
    corpo.geometry.dispose();
    geometriaFace.dispose();
    geometriaVerso.dispose();
    materialBorda.dispose();
    materialFrente.dispose();
    materialVerso.dispose();
    brilho.dispose();
  }

  return { grupo, brilho, destruir };
}

/** Acabamento laqueado: cor por baixo e uma camada de verniz (`clearcoat`) que pega os reflexos. */
function materialDoCartao(cor: THREE.Texture, rugosidade: THREE.Texture) {
  return new THREE.MeshPhysicalMaterial({
    map: cor,
    roughnessMap: rugosidade,
    metalness: 0.35,
    roughness: 1,
    clearcoat: 1,
    clearcoatRoughness: 0.12,
    sheen: 0.4,
    sheenColor: new THREE.Color(0x3b82f6),
    sheenRoughness: 0.6,
  });
}

// ── Texturas desenhadas em código ───────────────────────────────────────
// O mesmo desenho do `BankCard` (components/marketing/bank-card.tsx): azul-marinho, ondas da marca, chip
// dourado, aproximação, "•••• final", titular e "DÉBITO" em ciano. Aqui com mais detalhe, porque vira 3D.

interface Texturas {
  frente: THREE.CanvasTexture;
  verso: THREE.CanvasTexture;
  rugosidadeFrente: THREE.CanvasTexture;
  rugosidadeVerso: THREE.CanvasTexture;
  destruir(): void;
}

const CIANO = "#06b6d4";
const TEXTO = "rgba(241, 245, 249, 0.94)";
const TEXTO_SUAVE = "#94a3b8";

/** Rugosidade (canal cinza): escuro = brilhante (verniz), claro = fosco (chip e impressão). */
const RUGOSIDADE_BASE = 40;
const RUGOSIDADE_IMPRESSO = 110;
const RUGOSIDADE_CHIP = 80;

/** Duas camadas por face: a cor (o que se vê) e a rugosidade (onde o verniz brilha e onde a impressão fica fosca). */
function criarTexturas(titular: string, final: string, fontes: Fontes, anisotropia: number): Texturas {
  const frente = desenharFace((ctx, modo) => desenharFrente(ctx, modo, fontes, titular, final));
  const verso = desenharFace((ctx, modo) => desenharVerso(ctx, modo, fontes, titular, final));

  const lista = [frente.cor, frente.rugosidade, verso.cor, verso.rugosidade];
  for (const textura of lista) {
    textura.anisotropy = anisotropia;
    textura.needsUpdate = true;
  }
  frente.cor.colorSpace = THREE.SRGBColorSpace;
  verso.cor.colorSpace = THREE.SRGBColorSpace;

  return {
    frente: frente.cor,
    verso: verso.cor,
    rugosidadeFrente: frente.rugosidade,
    rugosidadeVerso: verso.rugosidade,
    destruir: () => lista.forEach((textura) => textura.dispose()),
  };
}

type Modo = "cor" | "rugosidade";
type Desenho = (ctx: CanvasRenderingContext2D, modo: Modo) => void;

function desenharFace(desenho: Desenho) {
  const criar = (modo: Modo) => {
    const canvas = document.createElement("canvas");
    canvas.width = TEXTURA_LARGURA;
    canvas.height = TEXTURA_ALTURA;
    const ctx = canvas.getContext("2d");
    if (ctx) {
      fundo(ctx, modo);
      desenho(ctx, modo);
    }
    return new THREE.CanvasTexture(canvas);
  };
  return { cor: criar("cor"), rugosidade: criar("rugosidade") };
}

/** Na camada de cor usa a cor; na de rugosidade, o cinza do acabamento daquela parte. */
function tinta(modo: Modo, cor: string | CanvasGradient, rugosidade: number) {
  return modo === "cor" ? cor : cinza(rugosidade);
}

/** Fundo azul-marinho do cartão de antes, com brilho azul, ondas, guilhoché e grão fino. */
function fundo(ctx: CanvasRenderingContext2D, modo: Modo) {
  const { width: w, height: h } = ctx.canvas;
  if (modo === "rugosidade") {
    ctx.fillStyle = cinza(RUGOSIDADE_BASE);
    ctx.fillRect(0, 0, w, h);
    return;
  }

  // O mesmo gradiente 135° do `BankCard`.
  const base = ctx.createLinearGradient(0, 0, w, h);
  base.addColorStop(0, "#0f1e3d");
  base.addColorStop(0.55, "#0a0f1c");
  base.addColorStop(1, "#0b2a4a");
  ctx.fillStyle = base;
  ctx.fillRect(0, 0, w, h);

  // O halo azul que o `shadow-brand-blue/20` dá ao cartão de antes, agora dentro do próprio cartão.
  brilhoRadial(ctx, w * 0.15, h * 0.1, w * 0.65, "rgba(59, 130, 246, 0.16)");

  // As ondas do cartão de antes (viewBox 400 × 252): mesmos 5 fios, mesmas opacidades.
  const sx = w / 400;
  const sy = h / 252;
  for (const deslocamento of [0, 14, 28, 42, 56]) {
    ctx.strokeStyle = `rgba(59, 130, 246, ${0.12 + deslocamento / 300})`;
    ctx.lineWidth = 1.2 * sx;
    ctx.beginPath();
    ctx.moveTo(-20 * sx, (200 - deslocamento) * sy);
    ctx.bezierCurveTo(
      90 * sx,
      (120 - deslocamento) * sy,
      200 * sx,
      (260 - deslocamento) * sy,
      420 * sx,
      (110 - deslocamento) * sy,
    );
    ctx.stroke();
  }

  // Grão fino: tira o aspecto de gradiente "liso demais".
  const aleatorio = sementeAleatoria(11);
  for (let i = 0; i < 14_000; i++) {
    ctx.fillStyle = aleatorio() > 0.5 ? "rgba(255, 255, 255, 0.035)" : "rgba(0, 0, 0, 0.06)";
    ctx.fillRect(aleatorio() * w, aleatorio() * h, 2, 2);
  }

  // Borda interna clara (o `border-white/10` do cartão de antes).
  ctx.strokeStyle = "rgba(255, 255, 255, 0.14)";
  ctx.lineWidth = 5;
  ctx.beginPath();
  ctx.roundRect(4, 4, w - 8, h - 8, w * RAIO);
  ctx.stroke();
}

function desenharFrente(ctx: CanvasRenderingContext2D, modo: Modo, fontes: Fontes, titular: string, final: string) {
  const { width: w, height: h } = ctx.canvas;
  const m = w * 0.065;

  // Mesmo arranjo do `BankCard`: RKF e aproximação em cima, chip no meio, número/titular e "DÉBITO" embaixo.
  ctx.textBaseline = "top";
  ctx.fillStyle = tinta(modo, TEXTO, RUGOSIDADE_IMPRESSO);
  ctx.font = `600 ${h * 0.07}px ${fontes.texto}`;
  ctx.letterSpacing = `${h * 0.07 * 0.3}px`;
  ctx.fillText("RKF", m, m);

  aproximacao(ctx, modo, w - m - h * 0.02, m + h * 0.04, h * 0.05);

  chip(ctx, modo, m, h * 0.42, w * 0.1);

  ctx.textBaseline = "alphabetic";
  ctx.fillStyle = tinta(modo, TEXTO, RUGOSIDADE_IMPRESSO);
  ctx.font = `500 ${h * 0.07}px ${fontes.mono}`;
  ctx.letterSpacing = `${h * 0.07 * 0.2}px`;
  ctx.fillText(`•••• ${final}`, m, h - m * 1.55);
  ctx.fillStyle = tinta(modo, TEXTO_SUAVE, RUGOSIDADE_IMPRESSO);
  ctx.font = `500 ${h * 0.042}px ${fontes.texto}`;
  ctx.letterSpacing = `${h * 0.042 * 0.2}px`;
  ctx.fillText(titular.toUpperCase(), m, h - m);

  ctx.textAlign = "right";
  ctx.fillStyle = tinta(modo, CIANO, RUGOSIDADE_IMPRESSO);
  ctx.font = `600 ${h * 0.05}px ${fontes.texto}`;
  ctx.letterSpacing = `${h * 0.05 * 0.2}px`;
  ctx.fillText("DÉBITO", w - m, h - m);
  ctx.textAlign = "left";
  ctx.letterSpacing = "0px";
}

function desenharVerso(ctx: CanvasRenderingContext2D, modo: Modo, fontes: Fontes, titular: string, final: string) {
  const { width: w, height: h } = ctx.canvas;
  const m = w * 0.065;

  // Tarja magnética.
  if (modo === "cor") {
    const tarja = ctx.createLinearGradient(0, h * 0.1, 0, h * 0.28);
    tarja.addColorStop(0, "#04070e");
    tarja.addColorStop(0.5, "#0b111d");
    tarja.addColorStop(1, "#04070e");
    ctx.fillStyle = tarja;
  } else {
    ctx.fillStyle = cinza(25);
  }
  ctx.fillRect(0, h * 0.1, w, h * 0.18);

  // Faixa de assinatura com microtexto e o CVV mascarado.
  const faixaY = h * 0.36;
  const faixaH = h * 0.12;
  const faixaW = w * 0.6;
  ctx.fillStyle = tinta(modo, "#b9c6d9", RUGOSIDADE_IMPRESSO);
  ctx.fillRect(m, faixaY, faixaW, faixaH);
  if (modo === "cor") {
    ctx.save();
    ctx.beginPath();
    ctx.rect(m, faixaY, faixaW, faixaH);
    ctx.clip();
    ctx.strokeStyle = "rgba(59, 130, 246, 0.18)";
    ctx.lineWidth = 2;
    for (let x = m - faixaH; x < m + faixaW; x += 14) {
      ctx.beginPath();
      ctx.moveTo(x, faixaY + faixaH);
      ctx.lineTo(x + faixaH, faixaY);
      ctx.stroke();
    }
    ctx.restore();
  }
  ctx.fillStyle = tinta(modo, "#0b1430", RUGOSIDADE_IMPRESSO);
  ctx.fillRect(m + faixaW + w * 0.02, faixaY, w * 0.13, faixaH);
  ctx.fillStyle = tinta(modo, TEXTO, RUGOSIDADE_IMPRESSO);
  ctx.font = `500 ${h * 0.05}px ${fontes.mono}`;
  ctx.textBaseline = "middle";
  ctx.textAlign = "center";
  ctx.fillText("•••", m + faixaW + w * 0.02 + w * 0.065, faixaY + faixaH / 2);
  ctx.textAlign = "left";

  ctx.textBaseline = "alphabetic";
  ctx.fillStyle = tinta(modo, TEXTO, RUGOSIDADE_IMPRESSO);
  ctx.font = `500 ${h * 0.062}px ${fontes.mono}`;
  ctx.letterSpacing = `${h * 0.012}px`;
  ctx.fillText(`••••  ••••  ••••  ${final}`, m, h * 0.62);

  ctx.fillStyle = tinta(modo, TEXTO_SUAVE, RUGOSIDADE_IMPRESSO);
  ctx.font = `500 ${h * 0.036}px ${fontes.mono}`;
  ctx.fillText("VALIDADE  10/31", m, h * 0.71);

  ctx.fillStyle = tinta(modo, TEXTO, RUGOSIDADE_IMPRESSO);
  ctx.font = `600 ${h * 0.056}px ${fontes.texto}`;
  ctx.letterSpacing = `${h * 0.014}px`;
  ctx.fillText(titular.toUpperCase(), m, h - m * 1.55);

  ctx.fillStyle = tinta(modo, TEXTO_SUAVE, RUGOSIDADE_IMPRESSO);
  ctx.font = `500 ${h * 0.032}px ${fontes.mono}`;
  ctx.letterSpacing = `${h * 0.008}px`;
  ctx.fillText("CONTA CORRENTE  ·  AG 0001", m, h - m * 0.9);

  ctx.textAlign = "right";
  ctx.fillStyle = tinta(modo, TEXTO, RUGOSIDADE_IMPRESSO);
  ctx.font = `700 ${h * 0.075}px ${fontes.texto}`;
  ctx.letterSpacing = `${h * 0.024}px`;
  ctx.fillText("RKF", w - m, h - m * 1.45);
  ctx.textAlign = "left";
  ctx.letterSpacing = "0px";

  aproximacao(ctx, modo, w - m - h * 0.04, h * 0.69, h * 0.04);
}

/** Chip dourado com os contatos (o `CardChip` de antes, em detalhe). */
function chip(ctx: CanvasRenderingContext2D, modo: Modo, x: number, y: number, largura: number) {
  const altura = largura * 0.74;
  const raio = largura * 0.14;
  if (modo === "cor") {
    const ouro = ctx.createLinearGradient(x, y, x + largura, y + altura);
    ouro.addColorStop(0, "#f0dca4");
    ouro.addColorStop(0.45, "#c8aa66");
    ouro.addColorStop(0.7, "#a88a4c");
    ouro.addColorStop(1, "#e2c98a");
    ctx.fillStyle = ouro;
  } else {
    ctx.fillStyle = cinza(RUGOSIDADE_CHIP);
  }
  ctx.beginPath();
  ctx.roundRect(x, y, largura, altura, raio);
  ctx.fill();

  ctx.strokeStyle = modo === "cor" ? "rgba(92, 70, 28, 0.6)" : cinza(RUGOSIDADE_IMPRESSO);
  ctx.lineWidth = 3;
  ctx.stroke();
  const linhas: [number, number, number, number][] = [
    [0, 0.33, 0.36, 0.33],
    [0, 0.66, 0.36, 0.66],
    [0.64, 0.33, 1, 0.33],
    [0.64, 0.66, 1, 0.66],
    [0.36, 0, 0.36, 1],
    [0.64, 0, 0.64, 1],
  ];
  for (const [x1, y1, x2, y2] of linhas) {
    ctx.beginPath();
    ctx.moveTo(x + largura * x1, y + altura * y1);
    ctx.lineTo(x + largura * x2, y + altura * y2);
    ctx.stroke();
  }
  ctx.beginPath();
  ctx.roundRect(x + largura * 0.36, y + altura * 0.22, largura * 0.28, altura * 0.56, raio * 0.6);
  ctx.stroke();
}

/** Símbolo de aproximação (quatro arcos, como o ícone `Nfc`). */
function aproximacao(ctx: CanvasRenderingContext2D, modo: Modo, x: number, y: number, tamanho: number) {
  ctx.strokeStyle = tinta(modo, TEXTO_SUAVE, RUGOSIDADE_IMPRESSO);
  ctx.lineWidth = tamanho * 0.14;
  ctx.lineCap = "round";
  for (let i = 0; i < 4; i++) {
    ctx.beginPath();
    ctx.arc(x - tamanho * 0.6, y, tamanho * (0.35 + i * 0.32), -Math.PI / 4, Math.PI / 4);
    ctx.stroke();
  }
}

function brilhoRadial(ctx: CanvasRenderingContext2D, x: number, y: number, raio: number, cor: string) {
  const gradiente = ctx.createRadialGradient(x, y, 0, x, y, raio);
  gradiente.addColorStop(0, cor);
  gradiente.addColorStop(1, "rgba(0, 0, 0, 0)");
  ctx.fillStyle = gradiente;
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
}

function cinza(valor: number) {
  return `rgb(${valor}, ${valor}, ${valor})`;
}

/** Gerador pseudoaleatório com semente: o grão sai igual a cada carga (sem "piscar" entre renders). */
function sementeAleatoria(semente: number) {
  let s = semente;
  return () => {
    s = (s * 16807) % 2147483647;
    return (s - 1) / 2147483646;
  };
}
