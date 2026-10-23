import { Archivo, IBM_Plex_Mono } from "next/font/google";

/** Archivo com o eixo de largura (`wdth`): as palavras gigantes usam a versão expandida. */
export const fonteLarga = Archivo({
  variable: "--font-larga",
  subsets: ["latin"],
  axes: ["wdth"],
  display: "swap",
});

/** Números, horários e specs: mono alinha os dígitos em coluna. */
export const fonteMono = IBM_Plex_Mono({
  variable: "--font-mono-landing",
  subsets: ["latin"],
  weight: ["400", "500"],
  display: "swap",
});
