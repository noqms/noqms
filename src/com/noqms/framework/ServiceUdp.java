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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import com.noqms.LogListener;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class ServiceUdp extends Thread {
    private static final int UDP_BUFFER_CAPACITY_MESSAGES = 100;
    private static final int HEADER_LENGTH_BYTES = 10;

    private final Harness harness;
    private final LogListener logger;
    private final DatagramSocket datagramSocket;
    private final int receivePort;
    private final byte[] receiveData;
    private final AtomicBoolean die = new AtomicBoolean();

    public ServiceUdp(Harness harness) throws Exception {
        this.harness = harness;
        this.logger = harness.getLogger();

        Config config = harness.getConfig();

        datagramSocket = new DatagramSocket(config.dataPort);
        datagramSocket.setSoTimeout(0);
        datagramSocket.setReceiveBufferSize(UDP_BUFFER_CAPACITY_MESSAGES * (MessageHeader.MAX_BYTES + config.maxMessageInBytes));
        datagramSocket.setSendBufferSize(UDP_BUFFER_CAPACITY_MESSAGES * (MessageHeader.MAX_BYTES + config.maxMessageOutBytes));

        receivePort = datagramSocket.getLocalPort();
        receiveData = new byte[MessageHeader.MAX_BYTES + config.maxMessageInBytes];

        setDaemon(true);
        start();
    }

    public void die() {
        die.set(true);
        datagramSocket.close();
    }

    public int getReceivePort() {
        return receivePort;
    }

    @Override
    public void run() {
        while (!die.get()) {
            DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);

            try {
                datagramSocket.receive(packet); // blocking
            } catch (Exception ex) {
                if (!die.get())
                    logger.error("Error receiving service packet", ex);
                continue;
            }

            byte[] packetData = packet.getData();
            int packetLength = packet.getLength();
            if (packetLength < HEADER_LENGTH_BYTES) {
                logger.error("Received service message is too small: " + packetLength, null);
                continue;
            }

            byte[] headerLengthData = new byte[HEADER_LENGTH_BYTES];
            System.arraycopy(packetData, 0, headerLengthData, 0, HEADER_LENGTH_BYTES);
            String headerLengthString = new String(headerLengthData, StandardCharsets.UTF_8);
            int headerLength = 0;
            try {
                headerLength = Integer.valueOf(headerLengthString);
            } catch (Exception ex) {
                logger.error("Received service message has an invalid header length: " + headerLengthString, null);
                continue;
            }
            if (packetLength < HEADER_LENGTH_BYTES + headerLength) {
                logger.error("Received service message has insufficient bytes for header: " + packetLength + " < " + (HEADER_LENGTH_BYTES + headerLength), null);
                continue;
            }
            int serviceDataLength = packetLength - headerLength - HEADER_LENGTH_BYTES;
            if (serviceDataLength > harness.getConfig().maxMessageInBytes) {
                logger.error("Received service message length exceeds maximum: " + serviceDataLength + " > " + harness.getConfig().maxMessageInBytes, null);
                continue;
            }

            // header
            MessageHeader header = null;
            try {
                header = Util.jsonObjectFromBytes(packetData, HEADER_LENGTH_BYTES, headerLength, MessageHeader.class);
            } catch (Exception ex) {
                logger.error("Unable to deserialize received service message header: " + new String(packetData, HEADER_LENGTH_BYTES, headerLength, StandardCharsets.UTF_8), ex);
                continue;
            }
            if (header.serviceNameFrom == null || header.serviceNameFrom.isBlank() || header.serviceNameTo == null || header.serviceNameTo.isBlank() || (header.id != null && header.id <= 0)) {
                logger.error("Bad service message received: " + Util.jsonStringFromObject(header), null);
                continue;
            }
            if (!header.serviceNameTo.equals(harness.getConfig().serviceName)) {
                logger.error("Received service message was intended for a different service: " + header.serviceNameTo + " != " + harness.getConfig().serviceName, null);
                continue;
            }

            // data
            byte[] serviceData = serviceDataLength == 0 ? null : new byte[serviceDataLength];
            if (serviceDataLength > 0)
                System.arraycopy(packetData, HEADER_LENGTH_BYTES + headerLength, serviceData, 0, serviceDataLength);

            harness.getProcessor().acceptMessageToMe(header, serviceData, packet.getAddress(), packet.getPort());
        }
    }

    /**
     * @return true on success
     */
    public boolean send(MessageHeader header, byte[] data, InetAddress addressTo, int portTo) {
        int dataLength = data == null ? 0 : data.length;
        if (dataLength > harness.getConfig().maxMessageOutBytes) {
            logger.error("Sent message length exceeds maximum: " + dataLength + " > " + harness.getConfig().maxMessageOutBytes, null);
            return false;
        }

        byte[] headerBytes = Util.jsonBytesFromObject(header);
        int headerLength = headerBytes.length;
        if (headerLength > MessageHeader.MAX_BYTES) {
            logger.error("Sent header length exceeds maximum: " + headerLength + " > " + MessageHeader.MAX_BYTES, null);
            return false;
        }

        byte[] udpMessage = new byte[HEADER_LENGTH_BYTES + headerLength + dataLength];
        System.arraycopy(String.format("%0" + HEADER_LENGTH_BYTES + "d", headerLength).getBytes(StandardCharsets.UTF_8), 0, udpMessage, 0, HEADER_LENGTH_BYTES);
        System.arraycopy(headerBytes, 0, udpMessage, HEADER_LENGTH_BYTES, headerLength);
        if (data != null)
            System.arraycopy(data, 0, udpMessage, HEADER_LENGTH_BYTES + headerLength, dataLength);

        try {
            datagramSocket.send(new DatagramPacket(udpMessage, udpMessage.length, addressTo, portTo));
        } catch (Exception ex) {
            logger.error("Error sending service packet", ex);
            return false;
        }

        return true;
    }
}
