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

import java.net.InetAddress;

import com.google.gson.annotations.SerializedName;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class MessageHeader {
    // Object forms are used for any optional fields which, if null, will then intentionally not get serialized.
    // Serialized names are short because this is going to be transmitted JSON style many times over the wire.
    
    public static final int MAX_BYTES = 500;
    
    public static class ResponseMeta {     
        @SerializedName(value = "c") public Integer code;               // application-defined status code (200,400,409,500 for example), if any
        @SerializedName(value = "u") public String userMessage;         // user message, if any
        @SerializedName(value = "n") public String nerdDetail;          // stack trace, critical details, etc, if any
    }

    @SerializedName(value = "n") public String serviceNameFrom;         // microservice name from  
    @SerializedName(value = "a") public InetAddress serviceAddressFrom; // microservice address from  
    @SerializedName(value = "p") public int servicePortFrom;            // microservice port from  
    @SerializedName(value = "t") public String serviceNameTo;           // microservice name to  
    @SerializedName(value = "i") public Long id;                        // requestId/responseId: present if this is a request and a response is expected or if this is a response (echo the request id value) 
    @SerializedName(value = "m") public ResponseMeta responseMeta;      // present if this is a response
}
