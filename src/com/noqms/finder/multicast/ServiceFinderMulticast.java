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

package com.noqms.finder.multicast;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.noqms.LogListener;
import com.noqms.ServiceFinder;
import com.noqms.ServiceInfo;
import com.noqms.framework.Util;

/**
 * @author Stanley Barzee
 * @since 1.1.0
 */
public class ServiceFinderMulticast extends ServiceFinder {
    private static final String MULTICAST_ADDRESS = Util.preferIPv6Addresses ? "ffee::100" : "224.1.10.100";
    private static final int MULTICAST_PORT_START = 1890;
    private static final int MULTICAST_PORT_SPAN = 100; // 1890 to 1989
    private static final int UDP_BUFFER_CAPACITY_MESSAGES = 100;

    private final MulticastSocket multicastSocket;
    private final byte[] receiveData;
    private final Gson gson = new Gson();
    private final Map<String, Map<String, ServiceInfoDynamic>> serviceNameToServices = new ConcurrentHashMap<>();
    private final InetAddress multicastAddress;
    private final int multicastPort;

    public ServiceFinderMulticast(String groupName, LogListener logger) throws Exception {
        super(groupName, logger);

        multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);

        // It is not critical that the port be unique among groups but it will help cut down on tossed multicast
        // messages.
        multicastPort = MULTICAST_PORT_START + (Math.abs(groupName.hashCode()) % MULTICAST_PORT_SPAN);

        multicastSocket = new MulticastSocket(multicastPort);
        multicastSocket.setSoTimeout(0);
        multicastSocket.setReceiveBufferSize(UDP_BUFFER_CAPACITY_MESSAGES * ModelMulticast.MAX_BYTES);
        multicastSocket.setSendBufferSize(UDP_BUFFER_CAPACITY_MESSAGES * ModelMulticast.MAX_BYTES);
        multicastSocket.setReuseAddress(true);
        multicastSocket.joinGroup(multicastAddress);

        receiveData = new byte[ModelMulticast.MAX_BYTES];
    }

    @Override
    public void start() {
        new ReadThread().start();
    }

    @Override
    public void die() {
        // The thread is a daemon, nothing to do here.
    }

    private class ReadThread extends Thread {
        public ReadThread() {
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                try {
                    multicastSocket.receive(packet); // blocking
                } catch (Exception ex) {
                    logger.logError("error receiving service finder multicast packet", ex);
                    continue;
                }

                ModelMulticast message = null;
                try {
                    message = gson.fromJson(new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8),
                            ModelMulticast.class);
                } catch (Exception ex) {
                    logger.logError("unable to deserialize received service finder multicast message", ex);
                    continue;
                }
                if (message.serviceName == null || message.serviceName.isBlank() || message.timeoutMillis <= 0
                        || message.groupName == null) {
                    logger.logError("bad service finder multicast message received: " + gson.toJson(message), null);
                    continue;
                }

                if (!message.groupName.equals(groupName))
                    continue;

                Map<String, ServiceInfoDynamic> services = serviceNameToServices.get(message.serviceName);
                if (services == null) {
                    services = new LinkedHashMap<>();
                    serviceNameToServices.put(message.serviceName, services);
                }
                synchronized (services) {
                    InetAddress address = message.address;
                    int port = message.port;
                    String serviceKey = formServiceKey(address, port);
                    ServiceInfoDynamic service = services.get(serviceKey);
                    if (service == null) {
                        service = new ServiceInfoDynamic(address, port);
                        services.put(serviceKey, service);
                    }
                    service.lastHeardFromTimeMillis = System.currentTimeMillis();
                    service.timeoutMillis = message.timeoutMillis;
                }
            }
        }
    }

    @Override
    public void sendMyServiceInfo(String myServiceName, InetAddress myAddress, int port, int myTimeoutMillis) {
        ModelMulticast message = new ModelMulticast();
        message.groupName = groupName;
        message.serviceName = myServiceName;
        message.address = myAddress;
        message.port = port;
        message.timeoutMillis = myTimeoutMillis;

        byte[] data = gson.toJson(message).getBytes(StandardCharsets.UTF_8);
        int dataLength = data.length;

        if (dataLength > ModelMulticast.MAX_BYTES) {
            logger.logError("send service finder multicast message length exceeds maximum: " + dataLength + " > "
                    + ModelMulticast.MAX_BYTES, null);
            return;
        }

        try {
            multicastSocket.send(new DatagramPacket(data, dataLength, multicastAddress, multicastPort));
        } catch (Exception ex) {
            logger.logError("error sending service finder multicast packet", ex);
        }
    }

    @Override
    public ServiceInfo findService(String serviceNameTo) {
        Map<String, ServiceInfoDynamic> services = serviceNameToServices.get(serviceNameTo);
        if (services == null || services.isEmpty())
            return null;
        synchronized (services) {
            ServiceInfoDynamic chosenService = null;
            for (Map.Entry<String, ServiceInfoDynamic> entry : services.entrySet()) {
                ServiceInfoDynamic service = entry.getValue();
                if (chosenService == null || service.lastHeardFromTimeMillis > chosenService.lastHeardFromTimeMillis)
                    chosenService = service;
            }
            if (chosenService == null)
                return null;
            return new ServiceInfo(chosenService.address, chosenService.port, chosenService.timeoutMillis,
                    chosenService.lastHeardFromTimeMillis);
        }
    }

    private String formServiceKey(InetAddress address, int port) {
        return address + "~" + port;
    }

    // Decrease the number of objects allocated by changing timeoutMillis and lastHeardFromTimeMillis in place.
    private class ServiceInfoDynamic {
        public final InetAddress address;
        public final int port;

        public int timeoutMillis;
        public long lastHeardFromTimeMillis;

        public ServiceInfoDynamic(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
    }
}
