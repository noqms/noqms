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

import java.net.InetAddress;

import com.google.gson.annotations.SerializedName;

/**
 * @author Stanley Barzee
 * @since 1.1.0
 */
public class ModelMulticast {
    // Serialized names are short because this is going to be transmitted JSON style many times over the wire.
    
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
