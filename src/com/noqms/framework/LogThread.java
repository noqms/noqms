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
public class LogThread extends Thread implements LogListener {
    private final ArrayDeque<LogEntry> logEntries = new ArrayDeque<>();
    private final LogListener otherLogger;
    private final String serviceName;

    public LogThread(String serviceName, LogListener otherLogger) {
        this.serviceName = serviceName;
        this.otherLogger = otherLogger;
        setDaemon(true);
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

    @Override
    public void logFatal(String text, Throwable cause) {
        addEntry(Level.FATAL, text, cause);
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
                Throwable cause = logEntry.cause;
                String causeMessage = cause == null ? "" : (": " + cause.toString());
                String text = "Noqms: " + serviceName + ": " + logEntry.text + causeMessage;
                if (otherLogger == null && logEntry.level == Level.INFO)
                    System.out.println(text);
                if (logEntry.level != Level.INFO)
                    System.err.println(text);
                if (cause != null)
                    cause.printStackTrace();
                if (otherLogger != null) {
                    try {
                        switch (logEntry.level) {
                        case INFO:
                            otherLogger.logInfo(text);
                            break;
                        case WARN:
                            otherLogger.logWarn(text);
                            break;
                        case ERROR:
                            otherLogger.logError(text, logEntry.cause);
                            break;
                        case FATAL:
                            otherLogger.logFatal(text, logEntry.cause);
                            break;
                        }
                    } catch (Exception ex) {
                        System.err.println("your logger threw an exception: " + ex.toString());
                    }
                }
            }
        }
    }

    private enum Level {
        INFO, WARN, ERROR, FATAL
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
