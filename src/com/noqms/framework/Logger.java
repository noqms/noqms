/*
 * Copyright 2019 Stanley Barzee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noqms.framework;

import java.util.ArrayDeque;

import com.noqms.LogListener;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class Logger extends Thread implements LogListener {
    private final ArrayDeque<LogEntry> logEntries = new ArrayDeque<>();
    private final LogListener externalLogger;
    private final String serviceName;

    public Logger(String serviceName, LogListener externalLogger) {
        this.serviceName = serviceName;
        this.externalLogger = externalLogger;
        setDaemon(true);
        start();
    }

    @Override
    public void logDebug(String text) {
        addEntry(Level.DEBUG, text, null);
    }

    @Override
    public void logInfo(String text) {
        addEntry(Level.INFO, text, null);
    }

    @Override
    public void logWarn(String text) {
        addEntry(Level.WARN, text, null);
    }

    @Override
    public void logError(String text, Throwable cause) {
        addEntry(Level.ERROR, text, cause);
    }

    public void die() {
        synchronized (logEntries) {
            logEntries.addLast(new LogEntry(null, null, null));
            logEntries.notify();
        }
    }

    private void addEntry(Level level, String text, Throwable cause) {
        synchronized (logEntries) {
            logEntries.addLast(new LogEntry(level, text, cause));
            logEntries.notify();
        }
    }

    public void run() {
        while (true) {
            LogEntry logEntry = null;
            synchronized (logEntries) {
                logEntry = logEntries.pollFirst();
                if (logEntry == null) {
                    try {
                        logEntries.wait();
                    } catch (Exception ex) {
                    }
                }
            }
            if (logEntry != null) {
                if (logEntry.level == null)
                    break;
                Throwable cause = logEntry.cause;
                String causeMessage = cause == null ? "" : (": " + cause.toString());
                String text = "Noqms: " + serviceName + ": " + logEntry.text;
                if (externalLogger == null) {
                    if (logEntry.level == Level.DEBUG || logEntry.level == Level.INFO)
                        System.out.println(text + causeMessage);
                    else
                        System.err.println(text + causeMessage);
                    if (cause != null)
                        cause.printStackTrace();
                } else {
                    try {
                        switch (logEntry.level) {
                        case DEBUG:
                            externalLogger.logDebug(text);
                            break;
                        case INFO:
                            externalLogger.logInfo(text);
                            break;
                        case WARN:
                            externalLogger.logWarn(text);
                            break;
                        case ERROR:
                            externalLogger.logError(text, cause);
                            break;
                        }
                    } catch (Throwable th) {
                        System.err.println("Your logger threw an exception: " + th.getMessage());
                        th.printStackTrace();
                    }
                }
            }
        }
    }

    private enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    private static class LogEntry {
        private final Level level;
        private final String text;
        private final Throwable cause;

        public LogEntry(Level level, String text, Throwable cause) {
            this.level = level;
            this.text = text;
            this.cause = cause;
        }
    }
}
