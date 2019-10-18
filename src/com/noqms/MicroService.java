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

import com.noqms.framework.Framework;

/**
 * Extend this class and implement the request/response details of your microservice in processRequest(). Call
 * sendResponse() from that code to respond to requests requiring a response. Call sendRequest() or
 * sendRequestExpectResponse() to make requests of other microservices.
 * 
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class MicroService {
    public static final int MAX_STRING_LENGTH = 100;
    public static final int MAX_DATA_LENGTH = 64000;

    private Framework framework;

    /*
     * Utilized by the framework alone to set the handler for the methods.
     */
    @SuppressWarnings("exports")
    public void setFramework(Framework framework) {
        this.framework = framework;
    }

    /**
     * Override this to implement your microservice request processing.
     * 
     * @param requestId       if non null, a response from you is required
     * @param serviceNameFrom name of the microservice which sent this message
     * @param data            application and microservice specific message data
     */
    public void processRequest(Long requestId, String serviceNameFrom, byte[] data) {
        framework.logError("a request was received to an unimplemented processRequest()", null);
    }

    /**
     * Call this from your processRequest() to respond to a microservice message.
     * 
     * @param requestId   non null requestId value passed to you by processRequest()
     * @param code        application defined message status code
     * @param userMessage application defined user presentable message
     * @param nerdDetail  application defined technical details
     * @param data        application and microservice specific message data
     */
    public void sendResponse(Long requestId, Integer code, String userMessage, String nerdDetail, byte[] data) {
        if (requestId == null || requestId <= 0)
            throw new IllegalArgumentException("responseId must be positive");
        if (userMessage != null && userMessage.length() > MAX_STRING_LENGTH)
            throw new IllegalArgumentException("userMessage length must be no greater than " + MAX_STRING_LENGTH);
        if (nerdDetail != null && nerdDetail.length() > MAX_STRING_LENGTH)
            throw new IllegalArgumentException("nerdDetail length must be no greater than " + MAX_STRING_LENGTH);
        if (data != null && data.length > MAX_DATA_LENGTH)
            throw new IllegalArgumentException("data length must be no greater than " + MAX_DATA_LENGTH);
        framework.getProcessor().sendResponse(requestId, code, userMessage, nerdDetail, data);
    }

    /**
     * Make a request of a microservice and do not require a response. Returns immediately.
     * 
     * @param serviceNameTo name of the destination microservice
     * @param data          application and microservice specific message data
     * @return immediate request status
     */
    public RequestStatus sendRequest(String serviceNameTo, byte[] data) {
        if (serviceNameTo == null || serviceNameTo.isBlank())
            throw new IllegalArgumentException("serviceNameTo is required");
        if (serviceNameTo.length() > MAX_STRING_LENGTH)
            throw new IllegalArgumentException("serviceNameTo length must be no greater than " + MAX_STRING_LENGTH);
        if (data != null && data.length > MAX_DATA_LENGTH)
            throw new IllegalArgumentException("data length must be no greater than " + MAX_DATA_LENGTH);
        return framework.getProcessor().sendRequest(serviceNameTo, data);
    }

    /**
     * Make a request of a microservice and require a response. Returns immediately. Immediately check the request
     * status of the call with ResponseStatus.getRequestStatus(). Deal with the ResponseFuture in your code later using
     * ResponseStatus.await() when resolution of the response is desired.
     * 
     * @param serviceNameTo name of the destination microservice
     * @param data          application and microservice specific message data
     * @return the immediate status of the request and an await() method to process the data when ready
     */
    public ResponseFuture sendRequestExpectResponse(String serviceNameTo, byte[] data) {
        if (serviceNameTo == null || serviceNameTo.isBlank())
            throw new IllegalArgumentException("serviceNameTo is required");
        if (serviceNameTo.length() > MAX_STRING_LENGTH)
            throw new IllegalArgumentException("serviceNameTo length must be no greater than " + MAX_STRING_LENGTH);
        if (data != null && data.length > MAX_DATA_LENGTH)
            throw new IllegalArgumentException("data length must be no greater than " + MAX_DATA_LENGTH);
        return framework.getProcessor().sendRequestExpectResponse(serviceNameTo, data);
    }
}
