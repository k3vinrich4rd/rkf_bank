import type { NextConfig } from "next";
import { env } from "./lib/env";

const nextConfig: NextConfig = {
  /**
   * Proxy para o backend: o navegador chama `/api/...` na mesma origem do
   * site e o Next repassa para a API. Assim não dependemos de CORS e a URL
   * real do backend não fica exposta no bundle.
   */
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${env.apiUrl}/:path*` }];
  },
};

export default nextConfig;
