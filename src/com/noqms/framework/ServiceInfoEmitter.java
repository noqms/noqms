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

import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class ServiceInfoEmitter extends Thread {
    private final Harness harness;
    private final AtomicBoolean die = new AtomicBoolean();
    private final AtomicBoolean pause = new AtomicBoolean();

    public ServiceInfoEmitter(Harness harness) {
        this.harness = harness;
        setDaemon(true);
    }

    public void die() {
        die.set(true);
        unpause();
    }

    public boolean pause() {
        return pause.getAndSet(true);
    }

    public boolean unpause() {
        synchronized (pause) {
            boolean oldValue = pause.get();
            pause.set(false);
            pause.notify();
            return oldValue;
        }
    }

    @Override
    public void run() {
        Config config = harness.getConfig();
        String myServiceName = config.serviceName;
        int myPort = harness.getServiceUdp().getReceivePort();
        int myTimeoutMillis = config.timeoutMillis;
        int intervalMillis = harness.getConfig().emitterIntervalMillis;
        int intervalHalfWindowMillis = intervalMillis / 5;
        Random random = new Random();

        while (!die.get()) {
            synchronized (pause) {
                while (pause.get()) {
                    try {
                        pause.wait();
                    } catch (Exception ex) {
                    }
                }
            }
            if (die.get())
                break;
            try {
                InetAddress myAddress = Util.findMyInetAddress();
                harness.getServiceFinder().sendMyServiceInfo(myServiceName, myAddress, myPort, myTimeoutMillis);
            } catch (Throwable th) {
                harness.logError("Pluggable service finder threw an exception in sendMyServiceInfo()", th);
            }
            // Introduce jitter for better distribution when a low number of a given unique microservice exists
            int sleepMillis = intervalMillis - intervalHalfWindowMillis + random.nextInt(2 * intervalHalfWindowMillis);
            Util.sleepMillis(sleepMillis);
        }
    }
}
