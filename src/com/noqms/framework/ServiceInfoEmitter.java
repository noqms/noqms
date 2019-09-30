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

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class ServiceInfoEmitter extends Thread {
    private final Framework framework;
    private final AtomicBoolean die = new AtomicBoolean();
    private final AtomicBoolean pause = new AtomicBoolean();

    public ServiceInfoEmitter(Framework framework) throws Exception {
        this.framework = framework;
        setDaemon(true);
    }

    public void die() {
        die.set(true);
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
        FrameworkConfig config = framework.getConfig();
        String myGroupName = config.groupName;
        String myServiceName = config.serviceName;
        InetAddress myAddress = framework.getMyInetAddress();
        int myUdpPort = framework.getServiceUdp().getReceivePort();
        int myTimeoutMillis = config.timeoutMillis;

        while (!die.get()) {
            synchronized (pause) {
                while (pause.get()) {
                    try {
                        pause.wait();
                    } catch (Exception ex) {
                    }
                }
            }
            try {
                framework.getServiceFinder().sendMyServiceInfo(myGroupName, myServiceName, myAddress, myUdpPort,
                        myTimeoutMillis);
            } catch (Exception ex) {
                framework.logError("pluggable service finder threw an exception in sendMyServiceInfo()", ex);
            }
            FrameworkUtil.sleepMillis(framework.getConfig().emitterIntervalMillis);
        }
    }
}
