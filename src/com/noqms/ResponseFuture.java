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

import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides immediate request status to check sendRequestExpectResponse() results as well as an await() method to wait
 * for the response when needed in your microservice code.
 * 
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class ResponseFuture {
    private final RequestStatus requestStatus;
    private final AtomicReference<Response> responseAtom = new AtomicReference<>();

    public ResponseFuture(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    /**
     * @return the immediate status of the request
     */
    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    /**
     * @return the response result immediately if present else null
     */
    public Response get() {
        return responseAtom.get();
    }

    /**
     * Immediately returns null if the requestStatus is not Ok - the request was not even made. Else waits for and
     * returns a non null response. The wait can be as long as the remote microservice's reported timeoutMills.
     * 
     * @return the response data from another microservice
     */
    public Response await() {
        if (requestStatus != RequestStatus.Ok)
            return null;
        synchronized (responseAtom) {
            while (responseAtom.get() == null) {
                try {
                    responseAtom.wait();
                } catch (Exception ex) {
                }
            }
            return responseAtom.get();
        }
    }

    /**
     * Encapsulates received response data. If timedOut is true, all other data is null and irrelevant.
     */
    public static class Response {
        public final boolean timedOut;

        public final String serviceNameFrom;
        public final byte[] data;
        public final Integer code;
        public final String userMessage;
        public final String nerdDetail;

        public Response(boolean timedOut, String serviceNameFrom, byte[] data, Integer code, String userMessage,
                String nerdDetail) {
            this.timedOut = timedOut;
            this.serviceNameFrom = serviceNameFrom;
            this.data = data;
            this.code = code;
            this.userMessage = userMessage;
            this.nerdDetail = nerdDetail;
        }
    }

    /**
     * The framework calls this exclusively to set and signal a received response.
     */
    public void set(Response response) {
        synchronized (responseAtom) {
            responseAtom.set(response);
            responseAtom.notifyAll();
        }
    }
}
