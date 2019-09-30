package com.noqms;

import java.net.InetAddress;

/**
 * Pluggable service finder. The method and means for microservices to find each other may vary from one platform to the
 * next. The provided multicast finder works very well; however multicast is not supported in most cloud environments.
 * There can/should be multiple choices of microservices for a given microservice name.
 * 
 * @author Stanley Barzee
 * @since 1.0.0
 * @see com.noqms.framework.ServiceFinderMulticast
 */
public class ServiceFinder {
    /**
     * Provide a constructor as shown below.
     * 
     * @param groupName the microservice group
     */
    public ServiceFinder(String groupName) {
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
     * Send the provided information identifying my microservice to other services.
     * 
     * @param myGroupName     provided group name
     * @param myServiceName   provided service name
     * @param myAddress       provided ip address
     * @param myUdpPort       provided service udp port
     * @param myTimeoutMillis provided timeoutMillis
     */
    public void sendMyServiceInfo(String myGroupName, String myServiceName, InetAddress myAddress, int myUdpPort,
            int myTimeoutMillis) {
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
