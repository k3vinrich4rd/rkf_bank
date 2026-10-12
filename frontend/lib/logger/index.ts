/**
 * Ponto único de importação dos logs: `import { … } from "@/lib/logger"`.
 * Guia de uso em `lib/logger/README.md`.
 */
export { createLogger, type Logger, type LogContext, type LogLevel } from "./logger";
export { logClientes, logContas, logHttp } from "./loggers";
export { descreverErro } from "./erro";
