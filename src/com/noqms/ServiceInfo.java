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

import java.io.Serializable;
import java.net.InetAddress;

/**
 * This class encapsulates the information a service finder provides on a remote microservice.
 * 
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class ServiceInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public final InetAddress address;
    public final int udpPort;
    public final int timeoutMillis;
    public final long lastHeardFromTimeMillis;

    public ServiceInfo(InetAddress address, int udpPort, int timeoutMillis, long lastHeardFromTimeMillis) {
        this.address = address;
        this.udpPort = udpPort;
        this.timeoutMillis = timeoutMillis;
        this.lastHeardFromTimeMillis = lastHeardFromTimeMillis;
    }
}
