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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class FrameworkUtil {
    static {
        fixTimer();
    }

    public static Properties argsToProps(String[] args) throws Exception {
        Properties props = new Properties();
        for (String arg : args) {
            int equalsPos = arg.indexOf('=');
            if (equalsPos < 1)
                throw new Exception("failed parsing arg for key value pair: " + arg);
            String key = arg.substring(0, equalsPos);
            String value = arg.substring(equalsPos + 1);
            props.put(key, value);
        }
        return props;
    }

    public static InetAddress findMyInetAddress() throws Exception {
        try {
            InetAddress address = null;
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces
                    .hasMoreElements();) {
                NetworkInterface iface = ifaces.nextElement();
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress())
                            return inetAddr;
                        else if (address == null)
                            address = inetAddr;
                    }
                }
            }
            if (address != null)
                return address;

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                address = socket.getLocalAddress();
            }
            if (address != null)
                return address;

            throw new Exception("my own ip address cannot be found");
        } catch (Exception ex) {
            throw new Exception("error obtaining my own ip address", ex);
        }
    }

    public static void sleepMillis(long millis) {
        long sleptMillis = 0;
        while (sleptMillis < millis) {
            long millisStart = System.currentTimeMillis();
            try {
                Thread.sleep(millis - sleptMillis);
            } catch (Exception ex) {
            }
            sleptMillis += System.currentTimeMillis() - millisStart;
        }
    }

    public static void fixTimer() {
        new Thread() {
            {
                setDaemon(true);
            }

            public void run() {
                sleepMillis(Integer.MAX_VALUE);
            }
        }.start();
    }

    public static long getMemoryUsedMB() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        return (totalMemory - freeMemory) / (1024 * 1024);
    }

    public static int getMemoryUsedPercent() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        return 100 - (int)(100.0F * freeMemory / totalMemory);
    }
}
