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

import java.util.Properties;

import com.noqms.Runner;

/**
 * @author Stanley Barzee
 * @since 1.1.0
 */
public class Config {
    private static final String DEFAULT_SERVICE_FINDER_PATH = "com.noqms.finder.multicast.ServiceFinderMulticast";
    private static final int MAX_STRING_LENGTH = 100;
    private static final int DEFAULT_EMITTER_INTERVAL_SECONDS = 2;
    private static final int DEFAULT_SERVICE_UNAVAILABLE_SECONDS = 1 + 2 * DEFAULT_EMITTER_INTERVAL_SECONDS;

    public final String groupName;
    public final int threads;
    public final int timeoutMillis;
    public final String serviceName;
    public final String servicePath;
    public final int maxMessageOutBytes;
    public final int maxMessageInBytes;
    public final int typicalMillis;
    public final int emitterIntervalMillis;
    public final int serviceUnavailableMillis;
    public final String serviceFinderPath;
    public final String logListenerPath;
    public final int appDataPort;

    public static Config createFromProperties(Properties props) throws Exception {
        int threads = loadInt(props, Runner.ARG_THREADS, null);
        int timeoutMillis = loadInt(props, Runner.ARG_TIMEOUT_MILLIS, null);
        String serviceName = loadString(props, Runner.ARG_SERVICE_NAME, null);
        String servicePath = loadString(props, Runner.ARG_SERVICE_PATH, null);
        int maxMessageOutBytes = loadInt(props, Runner.ARG_MAX_MESSAGE_OUT_BYTES, null);
        int maxMessageInBytes = loadInt(props, Runner.ARG_MAX_MESSAGE_IN_BYTES, null);
        int typicalMillis = loadInt(props, Runner.ARG_TYPICAL_MILLIS, null);
        String groupName = loadString(props, Runner.ARG_GROUP_NAME, null);
        int emitterIntervalSeconds = loadInt(props, Runner.ARG_EMITTER_INTERVAL_SECONDS,
                DEFAULT_EMITTER_INTERVAL_SECONDS);
        int serviceUnavailableSeconds = loadInt(props, Runner.ARG_SERVICE_UNAVAILABLE_SECONDS,
                DEFAULT_SERVICE_UNAVAILABLE_SECONDS);
        String serviceFinderPath = loadString(props, Runner.ARG_SERVICE_FINDER_PATH, DEFAULT_SERVICE_FINDER_PATH);
        String logListenerPath = loadString(props, Runner.ARG_LOG_LISTENER_PATH, "");
        int appDataPort = loadInt(props, Runner.ARG_APP_DATA_PORT, 0);

        if (threads <= 0)
            throw new Exception("config threads must positive: " + threads);
        if (timeoutMillis <= 0)
            throw new Exception("config timeoutMillis must be positive: " + timeoutMillis);
        if (serviceName.length() > MAX_STRING_LENGTH)
            throw new Exception(
                    "config serviceName length must not be greater than " + MAX_STRING_LENGTH + ": " + serviceName);
        if (maxMessageOutBytes < 0)
            throw new Exception("config maxMessageOutBytes must be zero or more: " + maxMessageOutBytes);
        if (maxMessageInBytes < 0)
            throw new Exception("config maxMessageInBytes must be zero or more: " + maxMessageInBytes);
        if (typicalMillis <= 0)
            throw new Exception("config typicalMillis must be positive: " + typicalMillis);
        if (timeoutMillis < typicalMillis)
            throw new Exception(
                    "config timeoutMillis must be greater than typicalMillis: " + timeoutMillis + ", " + typicalMillis);
        if (groupName.length() > MAX_STRING_LENGTH)
            throw new Exception(
                    "config groupName length must not be greater than " + MAX_STRING_LENGTH + ": " + groupName);
        if (emitterIntervalSeconds <= 0)
            throw new Exception("config emitterIntervalSeconds must be positive: " + maxMessageOutBytes);
        if (serviceUnavailableSeconds <= 0)
            throw new Exception("config serviceUnavailableSeconds must be positive: " + maxMessageOutBytes);
        if (serviceUnavailableSeconds < emitterIntervalSeconds)
            throw new Exception("config serviceUnavailableSeconds must be greater than emitterIntervalSeconds: "
                    + serviceUnavailableSeconds + ", " + emitterIntervalSeconds);
        if (appDataPort < 0 || appDataPort > 65535)
            throw new Exception("config appDataPort must be positive and less than 65536: " + appDataPort);

        return new Config(threads, timeoutMillis, serviceName, servicePath, maxMessageOutBytes,
                maxMessageInBytes, typicalMillis, groupName, emitterIntervalSeconds, serviceUnavailableSeconds,
                serviceFinderPath, logListenerPath, appDataPort);
    }

    private Config(int threads, int timeoutMillis, String serviceName, String servicePath,
            int maxMessageOutBytes, int maxMessageInBytes, int typicalMillis, String groupName,
            int emitterIntervalSeconds, int serviceUnavailableSeconds, String serviceFinderPath, String logListenerPath,
            Integer appDataPort) {
        this.threads = threads;
        this.timeoutMillis = timeoutMillis;
        this.serviceName = serviceName;
        this.servicePath = servicePath;
        this.maxMessageOutBytes = maxMessageOutBytes;
        this.maxMessageInBytes = maxMessageInBytes;
        this.typicalMillis = typicalMillis;
        this.groupName = groupName;
        this.emitterIntervalMillis = 1000 * emitterIntervalSeconds;
        this.serviceUnavailableMillis = 1000 * serviceUnavailableSeconds;
        this.serviceFinderPath = serviceFinderPath;
        this.logListenerPath = logListenerPath;
        this.appDataPort = appDataPort;
    }

    private static int loadInt(Properties props, String name, Integer defaultValue) throws Exception {
        String strValue = props.getProperty(name);
        if (strValue == null) {
            if (defaultValue == null)
                throw new Exception("config value required: " + name);
            else
                return defaultValue;
        }
        return Integer.valueOf(strValue.trim());
    }

    private static String loadString(Properties props, String name, String defaultValue) throws Exception {
        String strValue = props.getProperty(name);
        if (strValue == null || strValue.isBlank()) {
            if (defaultValue == null)
                throw new Exception("config value required: " + name);
            else
                return defaultValue;
        }
        return strValue.trim();
    }
}