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

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.noqms.ServiceFinder;
import com.noqms.ServiceInfo;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class ServiceFinderMulticast extends ServiceFinder implements Runnable {
    private static final String MULTICAST_HB_ADDRESS = "224.1.10.100";
    private static final int MULTICAST_HB_PORT_START = 1890;
    private static final int MESSAGE_CAPACITY = 100;

    private final MulticastSocket multicastSocket;
    private final byte[] receiveData;
    private final Gson gson = new Gson();
    private final Map<String, Map<String, ServiceInfoDynamic>> serviceNameToServices = new ConcurrentHashMap<>();
    private final InetAddress multicastAddress;
    private final int multicastPort;
    private final Framework framework;

    // Serialized names are short because this is going to be transmitted JSON style many times over the wire.
    public static class MulticastMessage {
        public static int MAX_BYTES = 300;

        @SerializedName(value = "g")
        public String groupName;
        @SerializedName(value = "n")
        public String serviceName;
        @SerializedName(value = "a")
        public InetAddress address;
        @SerializedName(value = "p")
        public int port;
        @SerializedName(value = "t")
        public int timeoutMillis;
    }

    public ServiceFinderMulticast(Framework framework) throws Exception {
        super(framework.getConfig().groupName);
        this.framework = framework;

        multicastAddress = InetAddress.getByName(MULTICAST_HB_ADDRESS);

        // It is not critical that the port be unique among groups but it will help cut down on tossed multicast
        // messages.
        multicastPort = MULTICAST_HB_PORT_START + (Math.abs(framework.getConfig().groupName.hashCode()) % 30000);

        multicastSocket = new MulticastSocket(multicastPort);
        multicastSocket.setSoTimeout(0);
        multicastSocket.setReceiveBufferSize(MESSAGE_CAPACITY * MulticastMessage.MAX_BYTES);
        multicastSocket.setSendBufferSize(MESSAGE_CAPACITY * MulticastMessage.MAX_BYTES);
        multicastSocket.setReuseAddress(true);
        multicastSocket.joinGroup(multicastAddress);

        receiveData = new byte[MulticastMessage.MAX_BYTES];
    }

    @Override
    public void start() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void die() {
        // This thread is a daemon, nothing to do here.
    }

    @Override
    public void run() {
        while (true) {
            DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
            try {
                multicastSocket.receive(packet); // blocking
            } catch (Exception ex) {
                framework.logError("error receiving service finder multicast packet", ex);
                continue;
            }

            MulticastMessage message = null;
            try {
                message = gson.fromJson(new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8),
                        MulticastMessage.class);
            } catch (Exception ex) {
                framework.logError("unable to deserialize received service finder multicast message", ex);
                continue;
            }
            if (message.serviceName == null || message.serviceName.isBlank() || message.address == null
                    || message.port <= 0 || message.timeoutMillis <= 0 || message.groupName == null) {
                framework.logError("bad service finder multicast message received: " + gson.toJson(message), null);
                continue;
            }

            if (!message.groupName.equals(framework.getConfig().groupName))
                continue;

            Map<String, ServiceInfoDynamic> services = serviceNameToServices.get(message.serviceName);
            boolean addServices = false;
            if (services == null) {
                services = new LinkedHashMap<>();
                addServices = true;
            }
            synchronized (services) {
                String serviceKey = formServiceKey(message.address, message.port);
                ServiceInfoDynamic service = services.get(serviceKey);
                if (service == null) {
                    service = new ServiceInfoDynamic(message.address, message.port);
                    services.put(serviceKey, service);
                }
                service.lastHeardFromTimeMillis = System.currentTimeMillis();
                service.timeoutMillis = message.timeoutMillis;
            }
            if (addServices)
                serviceNameToServices.put(message.serviceName, services);
        }
    }

    @Override
    public void sendMyServiceInfo(String myGroupName, String myServiceName, InetAddress myAddress, int myUdpPort,
            int myTimeoutMillis) {
        MulticastMessage message = new MulticastMessage();
        message.serviceName = myServiceName;
        message.address = myAddress;
        message.port = myUdpPort;
        message.timeoutMillis = myTimeoutMillis;
        message.groupName = myGroupName;

        byte[] data = gson.toJson(message).getBytes(StandardCharsets.UTF_8);
        int dataLength = data.length;

        if (dataLength > MulticastMessage.MAX_BYTES) {
            framework.logError("send service finder multicast message length exceeds maximum: " + dataLength + " > "
                    + MulticastMessage.MAX_BYTES, null);
            return;
        }

        try {
            multicastSocket.send(new DatagramPacket(data, dataLength, multicastAddress, multicastPort));
        } catch (Exception ex) {
            framework.logError("error sending service finder multicast packet", ex);
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
            return new ServiceInfo(chosenService.address, chosenService.udpPort, chosenService.timeoutMillis,
                    chosenService.lastHeardFromTimeMillis);
        }
    }

    private String formServiceKey(InetAddress address, int port) {
        return address + "~" + port;
    }

    // Decrease the number of objects allocated by changing timeoutMillis and lastHeardFromTimeMillis in place.
    private class ServiceInfoDynamic {
        public final InetAddress address;
        public final int udpPort;

        public int timeoutMillis;
        public long lastHeardFromTimeMillis;

        public ServiceInfoDynamic(InetAddress address, int udpPort) {
            this.address = address;
            this.udpPort = udpPort;
        }
    }
}
