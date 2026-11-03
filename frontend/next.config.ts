import type { NextConfig } from "next";

/**
 * O navegador chama `/api/...` na mesma origem do site (sem CORS, URL real do backend fora do bundle). Quem
 * repassa para a API é o BFF em `app/api/[...caminho]/route.ts`, com prazo por tentativa e aviso de "servidor
 * acordando" (a API hiberna no plano gratuito do Render). Até o preparo do deploy isso era um rewrite aqui, que não
 * deixava pôr prazo nem trocar a resposta.
 */
const nextConfig: NextConfig = {};

export default nextConfig;
