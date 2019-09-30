/*
 * Copyright 2019 Stanley Barzee.
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

import java.lang.Thread.UncaughtExceptionHandler;

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
public class Framework {
    private final AtomicBoolean exiting = new AtomicBoolean();
    private LogThread logThread;
    private FrameworkConfig config;
    private ServiceInfoEmitter serviceInfoEmitter;
    private Processor processor;
    private ServiceFinder serviceFinder;
    private ServiceUdp serviceUdp;
    private InetAddress myInetAddress;
    private LogListener logListener;

    public MicroService start(Properties props) {
        try {
            config = FrameworkConfig.createFromProperties(props);
            if (!config.logListenerPath.isBlank()) {
                Class<?> objectClass = Class.forName(config.logListenerPath);
                Constructor<?> constructor = objectClass.getConstructor();
                logListener = (LogListener)constructor.newInstance();
            }
        } catch (Throwable th) {
            System.err.println("start exception: " + th.toString());
            exit();
            FrameworkUtil.sleepMillis(Integer.MAX_VALUE);
        }

        logThread = new LogThread(config.serviceName, logListener);
        logThread.start();
        logInfo("starting: " + props);
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHook());
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        try {
            myInetAddress = FrameworkUtil.findMyInetAddress();
            processor = new Processor(this);
            serviceUdp = new ServiceUdp(this);
            serviceInfoEmitter = new ServiceInfoEmitter(this);

            Class<?> objectClass = Class.forName(config.serviceFinderPath);
            if (config.serviceFinderPath.equals(FrameworkConfig.DEFAULT_SERVICE_FINDER_PATH)) {
                Constructor<?> constructor = objectClass.getConstructor(Framework.class);
                serviceFinder = (ServiceFinder)constructor.newInstance(this);
            } else {
                Constructor<?> constructor = objectClass.getConstructor(String.class);
                serviceFinder = (ServiceFinder)constructor.newInstance(config.groupName);
            }

            processor.start();
            serviceFinder.start();
            serviceUdp.start();
            serviceInfoEmitter.start();
        } catch (Throwable th) {
            logFatal("start exception", th);
            exit();
            FrameworkUtil.sleepMillis(Integer.MAX_VALUE);
        }

        // Time is given to become aware of the other microservices.
        FrameworkUtil.sleepMillis(2 * config.emitterIntervalMillis);
        logInfo("started: address=" + myInetAddress + " udpPort=" + serviceUdp.getReceivePort() + " group="
                + config.groupName);

        return processor.getMicroService();
    }

    public InetAddress getMyInetAddress() {
        return myInetAddress;
    }

    public FrameworkConfig getConfig() {
        return config;
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
        logThread.logInfo(message);
    }

    public void logWarn(String message) {
        logThread.logWarn(message);
    }

    public void logError(String message, Throwable cause) {
        logThread.logError(message, cause);
    }

    public void logFatal(String message, Throwable cause) {
        logThread.logFatal(message, cause);
    }

    public void exit() {
        if (exiting.compareAndSet(false, true)) {
            new Thread() {
                {
                    setDaemon(true);
                }

                public void run() {
                    System.exit(-1);
                }
            }.start();
        }
    }

    private class UncaughtExceptionHook implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable cause) {
            logFatal("uncaught exception", cause);
            exit();
        }
    }

    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            logInfo("ending");
            if (serviceInfoEmitter != null)
                serviceInfoEmitter.die();
            if (serviceFinder != null) {
                try {
                    serviceFinder.die();
                } catch (Throwable th) {
                }
            }
            if (processor != null)
                processor.logRunningStats();
            logInfo("ended");
            FrameworkUtil.sleepMillis(100);
        }
    }
}
