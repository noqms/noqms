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

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import com.noqms.LogListener;
import com.noqms.MicroService;
import com.noqms.ServiceFinder;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class Harness {
    private Logger logger;
    private Config config;
    private Properties props;
    private ServiceInfoEmitter serviceInfoEmitter;
    private Processor processor;
    private ServiceFinder serviceFinder;
    private ServiceUdp serviceUdp;
    private final AtomicBoolean stopped = new AtomicBoolean();

    public MicroService start(Properties props, LogListener externalLogger) throws Exception {
        if (props == null)
            throw new IllegalArgumentException("Start properties must be given");
        this.props = props;

        try {
            config = Config.createFromProperties(props);
        } catch (Exception ex) {
            if (externalLogger != null)
                externalLogger.logFatal("Noqms: Failed parsing properties: " + ex.getMessage(), null);
            else
                System.err.println("Noqms: Failed parsing properties: " + ex.getMessage());
            throw ex;
        }

        logger = new Logger(config.serviceName, externalLogger);
        logger.logInfo("Starting: " + props);

        serviceInfoEmitter = new ServiceInfoEmitter(this);
        InetAddress myInetAddress = null;

        try {
            myInetAddress = Util.findMyInetAddress();
            serviceUdp = new ServiceUdp(this);
            serviceUdp.start();

            Class<?> objectClass = Class.forName(config.serviceFinderPath);
            Constructor<?> constructor = objectClass.getConstructor(String.class, LogListener.class, Properties.class);
            serviceFinder = (ServiceFinder)constructor.newInstance(config.groupName, logger, props);
            serviceFinder.start();

            processor = new Processor(this);
            processor.start();
        } catch (Throwable th) {
            logFatal("Start exception", th);
            throw new Exception("Start exception", th);
        }

        serviceInfoEmitter.start();

        // Time is given to become aware of the other microservices, important if the finder is multicast.
        Util.sleepMillis(2 * config.emitterIntervalMillis);
        logInfo("Started: address=" + myInetAddress + " port=" + serviceUdp.getReceivePort() + " group="
                + config.groupName);

        return processor.getMicroService();
    }

    public Config getConfig() {
        return config;
    }

    public Properties getProperties() {
        return props;
    }
    
    public LogListener getLogger() {
        return logger;
    }

    public ServiceInfoEmitter getServiceInfoEmitter() {
        return serviceInfoEmitter;
    }

    public Processor getProcessor() {
        return processor;
    }

    public ServiceFinder getServiceFinder() {
        return serviceFinder;
    }

    public ServiceUdp getServiceUdp() {
        return serviceUdp;
    }

    public void logInfo(String message) {
        logger.logInfo(message);
    }

    public void logWarn(String message) {
        logger.logWarn(message);
    }

    public void logError(String message, Throwable cause) {
        logger.logError(message, cause);
    }

    public void logFatal(String message, Throwable cause) {
        logger.logFatal(message, cause);
    }

    public void drain() {
        logInfo("Draining");
        if (serviceInfoEmitter != null)
            serviceInfoEmitter.die();
        Util.sleepMillis(config.emitterIntervalMillis);
        logInfo("Drained");
    }

    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            if (serviceInfoEmitter != null)
                serviceInfoEmitter.die();
            if (processor != null)
                processor.die();
            if (serviceFinder != null) {
                try {
                    serviceFinder.die();
                } catch (Throwable th) {
                }
            }
            if (serviceUdp != null)
                serviceUdp.die();
            logInfo("Stopped");
            if (logger != null)
                logger.die();
        }
    }
}
