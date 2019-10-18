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
 * Pluggable service finder. The method and means for microservices to find each other may vary from one platform to the
 * next. The provided multicast finder works very well; however multicast is not supported in most cloud environments.
 * 
 * @author Stanley Barzee
 * @since 1.0.0
 * @see com.noqms.finder.multicast.ServiceFinderMulticast
 */
public class ServiceFinder {
    protected final String groupName;
    protected final LogListener logger;

    /**
     * Provide a constructor as shown below.
     * 
     * @param groupName the microservice group
     * @param logger    the logger
     */
    public ServiceFinder(String groupName, LogListener logger) {
        this.groupName = groupName;
        this.logger = logger;
    }

    /**
     * Start the process, if any, involved with service finding.
     */
    public void start() {
    }

    /**
     * Kill the process, if any, involved with service finding. The program is terminating.
     */
    public void die() {
    }

    /**
     * Send the provided information identifying my microservice to other services or to the mechanism implementing
     * microservice discovery.
     * 
     * @param myServiceName   provided service name
     * @param myAddress       provided ip address
     * @param myPort          provided app data port
     * @param myTimeoutMillis provided timeoutMillis
     */
    public void sendMyServiceInfo(String myServiceName, InetAddress myAddress, int myPort, int myTimeoutMillis) {
    }

    /**
     * Find and return the best service option taking into consideration the last time service info was received from
     * the service. This should be done very quickly - this is called on the microservice thread when a request is sent.
     * 
     * @param serviceNameTo desination microservice
     * @return servive information of the chosen microservices
     */
    public ServiceInfo findService(String serviceNameTo) {
        return null;
    }
}
