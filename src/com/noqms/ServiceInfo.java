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
