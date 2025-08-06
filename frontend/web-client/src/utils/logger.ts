type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const LOG_LEVELS: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

const LOG_LEVEL = ('warn') as LogLevel;
const CURRENT_LEVEL = LOG_LEVELS[LOG_LEVEL] || LOG_LEVELS.warn;

const shouldLog = (level: LogLevel): boolean => {
  return LOG_LEVELS[level] >= CURRENT_LEVEL;
};

const createLogger = (context: string) => ({
  debug: (message: string, ...args: any[]) => {
    if (shouldLog('debug')) {
      console.debug(`[${context}] ${message}`, ...args);
    }
  },
  info: (message: string, ...args: any[]) => {
    if (shouldLog('info')) {
      console.info(`[${context}] ${message}`, ...args);
    }
  },
  warn: (message: string, ...args: any[]) => {
    if (shouldLog('warn')) {
      console.warn(`[${context}] ${message}`, ...args);
    }
  },
  error: (message: string, error?: Error, ...args: any[]) => {
    if (shouldLog('error')) {
      console.error(`[${context}] ${message}`, error, ...args);
    }
  },
});

export const logger = {
  create: createLogger,
  debug: (message: string, ...args: any[]) => createLogger('App').debug(message, ...args),
  info: (message: string, ...args: any[]) => createLogger('App').info(message, ...args),
  warn: (message: string, ...args: any[]) => createLogger('App').warn(message, ...args),
  error: (message: string, error?: Error, ...args: any[]) => 
    createLogger('App').error(message, error, ...args),
};
