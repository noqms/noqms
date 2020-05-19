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
import java.util.concurrent.atomic.AtomicBoolean;

import com.noqms.MicroService;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class RequestToMeThread extends Thread {
    private final ArrayDeque<Request> requestsToMe;
    private final Framework framework;
    private final MicroService microservice;
    private final AtomicBoolean die = new AtomicBoolean();

    public static class Request {
        public final Long requestId;
        public final String serviceNameFrom;
        public final byte[] data;

        public Request(Long requestId, String serviceNameFrom, byte[] data) {
            this.requestId = requestId;
            this.serviceNameFrom = serviceNameFrom;
            this.data = data;
        }
    }

    public RequestToMeThread(String name, Framework framework, ArrayDeque<Request> requestsToMe,
            MicroService microservice) {
        this.requestsToMe = requestsToMe;
        this.framework = framework;
        this.microservice = microservice;
        setName(name);
        setDaemon(true);
    }

    public void die() {
        die.set(true);
        synchronized (requestsToMe) {
            requestsToMe.notifyAll();
        }
    }

    public void run() {
        while (!die.get()) {
            Request request = null;
            synchronized (requestsToMe) {
                request = requestsToMe.pollFirst();
                if (request == null) {
                    try {
                        requestsToMe.wait();
                    } catch (Exception ex) {
                    }
                }
            }
            if (request != null) {
                try {
                    microservice.processRequest(request.requestId, request.serviceNameFrom, request.data);
                } catch (Exception ex) {
                    framework.logError("Your microservice threw an exception in processRequest()", ex);
                }
            }
        }
    }
}
