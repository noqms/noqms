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
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Stanley Barzee
 * @since 1.1.0
 */
public class Util {
    public static final boolean preferIPv6Addresses = Boolean.valueOf(System.getProperty("java.net.preferIPv6Addresses"));

    private static final Gson gson = new Gson();
    private static final Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();

    static {
        fixTimer();
    }

    public static Properties argsToProps(String[] args) throws Exception {
        Properties props = new Properties();
        for (String arg : args) {
            int equalsPos = arg.indexOf('=');
            if (equalsPos < 1)
                throw new Exception("Failed parsing args for key value pair: " + arg);
            String key = arg.substring(0, equalsPos);
            String value = arg.substring(equalsPos + 1);
            props.put(key, value);
        }
        return props;
    }

    public static InetAddress findMyInetAddress() throws Exception {
        try {
            // If the machine is addressable from the outside world this will obtain that address else something local.
            InetAddress address = null;
            try (DatagramSocket socket = new DatagramSocket()) {
                if (preferIPv6Addresses)
                    socket.connect(Inet6Address.getByName("2607:f8b0:400f:806::200e"), 10002);
                else
                    socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                address = socket.getLocalAddress();
            }
            if (address != null && !address.isAnyLocalAddress())
                return address;
        } catch (Exception ex) {
            throw new Exception("Error obtaining my own ip address", ex);
        }
        throw new Exception("My own ip address cannot be found");
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

    public static String jsonStringFromObject(Object object) {
        return gsonPretty.toJson(object);
    }

    public static byte[] jsonBytesFromObject(Object object) {
        return gson.toJson(object).getBytes(StandardCharsets.UTF_8);
    }

    public static <T> T jsonObjectFromBytes(byte[] bytes, Class<T> clazz) {
        return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), clazz);
    }

    public static <T> T jsonObjectFromBytes(byte[] bytes, int len, Class<T> clazz) {
        return gson.fromJson(new String(bytes, 0, len, StandardCharsets.UTF_8), clazz);
    }

    public static <T> T jsonObjectFromBytes(byte[] bytes, int offset, int len, Class<T> clazz) {
        return gson.fromJson(new String(bytes, offset, len, StandardCharsets.UTF_8), clazz);
    }

    private static void fixTimer() {
        new Thread() {
            {
                setDaemon(true);
            }

            public void run() {
                sleepMillis(Integer.MAX_VALUE);
            }
        }.start();
    }
}
