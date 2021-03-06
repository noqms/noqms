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

package com.noqms;

import java.net.InetAddress;

/**
 * This class encapsulates the information a service finder provides on a remote microservice.
 * 
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class ServiceInfo {
    public final InetAddress address;
    public final int port;
    public final int timeoutMillis;
    public final int elapsedMillis;

    /**
     * @param address       remote service address
     * @param port          remote service port
     * @param timeoutMillis remote service timeout in millis
     * @param elapsedMillis millis since the service reported
     */
    public ServiceInfo(InetAddress address, int port, int timeoutMillis, int elapsedMillis) {
        this.address = address;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
        this.elapsedMillis = elapsedMillis;
    }
}
