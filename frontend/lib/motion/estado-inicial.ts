import type { EstadoCartao } from "./cartao-3d";

export function estadoInicialDoCartao(): EstadoCartao {
  return { cx: 0, cy: 0, len: 300, rx: 0, ry: 0, rz: 0, env: 0, shadow: 0.4, tilt: 1, light: -0.3 };
}
