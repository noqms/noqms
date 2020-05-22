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

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;
import com.noqms.LogListener;
import com.noqms.MicroService;
import com.noqms.RequestStatus;
import com.noqms.ResponseFuture;
import com.noqms.ServiceInfo;

/**
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class Processor extends Thread {
    private static final int ONE_MINUTE_MILLIS = (int)TimeUnit.MINUTES.toMillis(1);

    private final Harness harness;
    private final Config config;
    private final LogListener logger;
    private final MicroService microService;
    private final ArrayDeque<MessageToMe> messagesToMe = new ArrayDeque<>();
    private final ArrayDeque<MessageFromMe> messagesFromMe = new ArrayDeque<>();
    private final AtomicLong requestIdGenerator = new AtomicLong();
    private final ArrayDeque<RequestToMeThread.Request> requestsToMe = new ArrayDeque<>();
    private final List<RequestToMeThread> requestToMeThreads = new ArrayList<>();
    private final Map<Long, RequestFromMeExpectingResponse> requestsFromMeByRequestId = new ConcurrentHashMap<>();
    private final Map<Long, RequestToMeExpectingResponse> requestsToMeByInternalRequestId = new ConcurrentHashMap<>();
    private final DelayQueue<ExpiringId> expiringRequestsFromMe = new DelayQueue<>();
    private final DelayQueue<ExpiringId> expiringRequestsToMe = new DelayQueue<>();
    private final Gson gson = new Gson();
    private final PerMinuteStats perMinuteStats = new PerMinuteStats();
    private final AtomicBoolean die = new AtomicBoolean();

    public Processor(Harness harness) throws Exception {
        this.harness = harness;
        this.config = harness.getConfig();
        this.logger = harness.getLogger();

        try {
            Class<?> objectClass = Class.forName(config.servicePath);
            Constructor<?> constructor = objectClass.getConstructor();
            microService = (MicroService)constructor.newInstance();
            microService.setHarness(harness);
            microService.initialize();
        } catch (Exception ex) {
            throw new Exception("Failed loading your microservice: " + config.servicePath, ex);
        }

        for (int ix = 0; ix < config.threads; ix++)
            requestToMeThreads.add(new RequestToMeThread(harness, requestsToMe, microService, ix));
        for (RequestToMeThread requestToMeThread : requestToMeThreads)
            requestToMeThread.start();

        setDaemon(true);
    }

    public MicroService getMicroService() {
        return microService;
    }

    public void die() {
        logger.logInfo("RunningStats=" + new RunningStats().get());
        die.set(true);
        for (RequestToMeThread requestToMeThread : requestToMeThreads)
            requestToMeThread.die();
    }

    // Deal with the data and return quickly.
    // Both requests to me and response to me come through here.
    public void acceptMessageToMe(MessageHeader header, byte[] data, InetAddress serviceAddressFrom,
            int servicePortFrom) {
        synchronized (messagesToMe) {
            messagesToMe.add(new MessageToMe(header, data, serviceAddressFrom, servicePortFrom));
            messagesToMe.notify();
        }
    }

    public ResponseFuture sendRequestExpectResponse(String serviceNameTo, byte[] data) {
        ServiceInfo service = null;
        try {
            service = harness.getServiceFinder().findService(serviceNameTo);
        } catch (Throwable th) {
            perMinuteStats.failedRequests.incrementAndGet();
            logger.logError("The pluggable service finder threw an exception in findService()", th);
            return new ResponseFuture(RequestStatus.ServiceNotFound);
        }
        if (service == null) {
            perMinuteStats.failedRequests.incrementAndGet();
            logger.logWarn("The sendRequestExpectResponse() serviceNameTo service does not exist: " + serviceNameTo);
            return new ResponseFuture(RequestStatus.ServiceNotFound);
        }
        if (service.elapsedMillis > harness.getConfig().serviceUnavailableMillis) {
            perMinuteStats.failedRequests.incrementAndGet();
            logger.logWarn("The sendRequestExpectResponse() serviceNameTo service is not responsive: " + serviceNameTo);
            return new ResponseFuture(RequestStatus.ServiceNotResponsive);
        }
        MessageHeader header = new MessageHeader();
        header.serviceNameFrom = config.serviceName;
        header.serviceNameTo = serviceNameTo;
        header.id = requestIdGenerator.incrementAndGet();
        ResponseFuture responseFuture = new ResponseFuture(RequestStatus.Ok);
        synchronized (messagesFromMe) {
            messagesFromMe.add(new MessageFromMe(header, data, responseFuture, service, null));
        }
        return responseFuture;
    }

    public RequestStatus sendRequest(String serviceNameTo, byte[] data) {
        ServiceInfo service = null;
        try {
            service = harness.getServiceFinder().findService(serviceNameTo);
        } catch (Throwable th) {
            perMinuteStats.failedRequests.incrementAndGet();
            logger.logError("The pluggable service finder threw an exception in findService()", th);
            return RequestStatus.ServiceNotFound;
        }
        if (service == null) {
            perMinuteStats.failedRequests.incrementAndGet();
            logger.logWarn("The sendRequest() serviceNameTo service is not found: " + serviceNameTo);
            return RequestStatus.ServiceNotFound;
        }
        if (service.elapsedMillis > harness.getConfig().serviceUnavailableMillis) {
            perMinuteStats.failedRequests.incrementAndGet();
            logger.logWarn("The sendRequest() serviceNameTo service is not responsive: " + serviceNameTo);
            return RequestStatus.ServiceNotResponsive;
        }
        MessageHeader header = new MessageHeader();
        header.serviceNameFrom = config.serviceName;
        header.serviceNameTo = serviceNameTo;
        // header.id is not populated for requests not wanting a response
        synchronized (messagesFromMe) {
            messagesFromMe.add(new MessageFromMe(header, data, null, service, null));
        }
        return RequestStatus.Ok;
    }

    public void sendResponse(Long internalRequestId, Integer code, String userMessage, String nerdDetail, byte[] data) {
        MessageHeader header = new MessageHeader();
        header.serviceNameFrom = config.serviceName;
        // header.serviceNameTo is populated from the original header id
        // header.id is populated from the original header id
        header.responseMeta = new MessageHeader.ResponseMeta();
        header.responseMeta.code = code;
        header.responseMeta.userMessage = userMessage;
        header.responseMeta.nerdDetail = nerdDetail;
        synchronized (messagesFromMe) {
            messagesFromMe.add(new MessageFromMe(header, data, null, null, internalRequestId));
        }
    }

    @Override
    public void run() {
        long lastStatsReportTimeMillis = System.currentTimeMillis();

        while (!die.get()) {
            int requestsToMeBacklog = getRequestsToMeBacklog();
            int backPressureThreshold = 1
                    + (int)(config.threads * ((float)config.timeoutMillis / config.typicalMillis));
            if (requestsToMeBacklog > backPressureThreshold) {
                perMinuteStats.backPressureApplied = true;
                boolean wasPaused = harness.getServiceInfoEmitter().pause();
                if (!wasPaused)
                    logger.logInfo("Applying back pressure");
            } else {
                boolean wasPaused = harness.getServiceInfoEmitter().unpause();
                if (wasPaused)
                    logger.logInfo("Removing back pressure");
            }

            while (true) {
                MessageFromMe messageFromMe = null;
                synchronized (messagesFromMe) {
                    messageFromMe = messagesFromMe.poll();
                }
                if (messageFromMe == null)
                    break;
                MessageHeader header = messageFromMe.header;
                if (header.responseMeta != null) {
                    // response from me
                    RequestToMeExpectingResponse requestToMe = requestsToMeByInternalRequestId
                            .remove(messageFromMe.internalRequestId);
                    if (requestToMe == null) {
                        logger.logWarn("My response has no or expired request: response=" + gson.toJson(header));
                    } else {
                        perMinuteStats.responsesSent++;
                        header.serviceNameTo = requestToMe.header.serviceNameFrom;
                        header.id = requestToMe.header.id;
                        boolean success = harness.getServiceUdp().send(header, messageFromMe.data,
                                requestToMe.serviceAddressFrom, requestToMe.servicePortFrom);
                        if (!success)
                            perMinuteStats.failedResponses.incrementAndGet();
                    }
                } else {
                    // request from me
                    perMinuteStats.requestsSent++;
                    if (header.id != null) {
                        // request from me expecting a response
                        requestsFromMeByRequestId.put(header.id,
                                new RequestFromMeExpectingResponse(header, messageFromMe.responseFuture));
                        expiringRequestsFromMe.add(new ExpiringId(header.id, messageFromMe.serviceTo.timeoutMillis));
                    }
                    boolean success = harness.getServiceUdp().send(header, messageFromMe.data,
                            messageFromMe.serviceTo.address, messageFromMe.serviceTo.port);
                    if (!success)
                        perMinuteStats.failedRequests.incrementAndGet();
                }
            }

            while (true) {
                MessageToMe messageToMe = null;
                synchronized (messagesToMe) {
                    messageToMe = messagesToMe.poll();
                }
                if (messageToMe == null)
                    break;
                MessageHeader header = messageToMe.header;
                if (header.responseMeta != null) {
                    // response to me
                    RequestFromMeExpectingResponse requestFromMe = requestsFromMeByRequestId.remove(header.id);
                    if (requestFromMe == null) {
                        logger.logWarn("A response to me has no or expired request: response=" + gson.toJson(header));
                    } else {
                        perMinuteStats.responsesReceived++;
                        ResponseFuture.Response response = new ResponseFuture.Response(false, header.serviceNameFrom,
                                messageToMe.data, header.responseMeta.code, header.responseMeta.userMessage,
                                header.responseMeta.nerdDetail);
                        requestFromMe.responseFuture.set(response);
                    }
                } else {
                    // request to me
                    perMinuteStats.requestsReceived++;
                    Long internalRequestId = requestIdGenerator.incrementAndGet();
                    if (header.id != null) {
                        // request to me expecting a response
                        requestsToMeByInternalRequestId.put(internalRequestId, new RequestToMeExpectingResponse(header,
                                messageToMe.serviceAddressFrom, messageToMe.servicePortFrom));
                        expiringRequestsToMe.add(new ExpiringId(internalRequestId, config.timeoutMillis));
                    }
                    RequestToMeThread.Request request = new RequestToMeThread.Request(internalRequestId,
                            header.serviceNameFrom, messageToMe.data);
                    synchronized (requestsToMe) {
                        requestsToMe.addLast(request);
                        requestsToMe.notify();
                    }
                }
            }

            while (true) {
                ExpiringId expiringId = expiringRequestsFromMe.poll();
                if (expiringId == null)
                    break;
                RequestFromMeExpectingResponse request = requestsFromMeByRequestId.remove(expiringId.id);
                if (request != null) {
                    perMinuteStats.responsesDroppedByOthers++;
                    ResponseFuture.Response response = new ResponseFuture.Response(true, null, null, 0, null, null);
                    request.responseFuture.set(response);
                    logger.logWarn("A request from me was not responded to in time: timeoutMillis="
                            + expiringId.timeoutMillis + ": " + gson.toJson(request.header));
                }
            }

            while (true) {
                ExpiringId expiringId = expiringRequestsToMe.poll();
                if (expiringId == null)
                    break;
                RequestToMeExpectingResponse request = requestsToMeByInternalRequestId.remove(expiringId.id);
                if (request != null) {
                    perMinuteStats.responsesDroppedByMe++;
                    logger.logWarn("A request to me was not responded to in time: timeoutMillis="
                            + expiringId.timeoutMillis + ": " + gson.toJson(request.header));
                }
            }

            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastStatsReportTimeMillis >= ONE_MINUTE_MILLIS) {
                lastStatsReportTimeMillis = currentTimeMillis;
                logger.logInfo(
                        "PerMinuteStats=" + perMinuteStats.getAndReset() + " RunningStats=" + new RunningStats().get());
            }

            Util.sleepMillis(1); // very responsive but not burdensome during inactivity
        }
    }

    private int getRequestsToMeBacklog() {
        synchronized (requestsToMe) {
            return requestsToMe.size();
        }
    }

    private class MessageFromMe {
        private final MessageHeader header;
        private final byte[] data;
        private final ResponseFuture responseFuture;
        private final ServiceInfo serviceTo;
        private final Long internalRequestId;

        private MessageFromMe(MessageHeader header, byte[] data, ResponseFuture responseFuture, ServiceInfo serviceTo,
                Long internalRequestId) {
            this.header = header;
            this.data = data;
            this.responseFuture = responseFuture;
            this.serviceTo = serviceTo;
            this.internalRequestId = internalRequestId;
        }
    }

    private class MessageToMe {
        private final MessageHeader header;
        private final byte[] data;
        private final InetAddress serviceAddressFrom;
        private final int servicePortFrom;

        private MessageToMe(MessageHeader header, byte[] data, InetAddress serviceAddressFrom, int servicePortFrom) {
            this.header = header;
            this.data = data;
            this.serviceAddressFrom = serviceAddressFrom;
            this.servicePortFrom = servicePortFrom;
        }
    }

    private class RequestFromMeExpectingResponse {
        private final MessageHeader header;
        private final ResponseFuture responseFuture;

        private RequestFromMeExpectingResponse(MessageHeader header, ResponseFuture responseFuture) {
            this.header = header;
            this.responseFuture = responseFuture;
        }
    }

    private class RequestToMeExpectingResponse {
        private final MessageHeader header;
        private final InetAddress serviceAddressFrom;
        private final int servicePortFrom;

        private RequestToMeExpectingResponse(MessageHeader header, InetAddress serviceAddressFrom,
                int servicePortFrom) {
            this.header = header;
            this.serviceAddressFrom = serviceAddressFrom;
            this.servicePortFrom = servicePortFrom;
        }
    }

    private class ExpiringId implements Delayed {
        private final long id;
        private final long timeoutMillis;
        private final long expireTimeMillis;

        private ExpiringId(long id, long timeoutMillis) {
            this.id = id;
            this.timeoutMillis = timeoutMillis;
            this.expireTimeMillis = System.currentTimeMillis() + timeoutMillis;
        }

        @Override
        public int compareTo(Delayed o) {
            ExpiringId other = (ExpiringId)o;
            if (expireTimeMillis < other.expireTimeMillis)
                return -1;
            if (expireTimeMillis > other.expireTimeMillis)
                return 1;
            return 0;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long millisLeft = expireTimeMillis - System.currentTimeMillis();
            return unit.convert(millisLeft, TimeUnit.MILLISECONDS);
        }

    }

    @SuppressWarnings("unused")
    private class RunningStats {
        private int requestsToMeBacklog;
        private long memoryUsedMB;
        private int memoryUsedPercent;
        private int threadCount;

        private String get() {
            requestsToMeBacklog = getRequestsToMeBacklog();
            memoryUsedMB = Util.getMemoryUsedMB();
            memoryUsedPercent = Util.getMemoryUsedPercent();
            threadCount = Thread.activeCount();
            return gson.toJson(this);
        }
    }

    public void processRequestMillis(int millis) {
        perMinuteStats.processRequestMillis(millis);
    }

    @SuppressWarnings("unused")
    private class PerMinuteStats {
        private int requestsSent;
        private int requestsReceived;
        private int responsesSent;
        private int responsesReceived;
        private int responsesDroppedByMe;
        private int responsesDroppedByOthers;
        private boolean backPressureApplied;
        private int processRequestLowMillis;
        private int processRequestHighMillis;
        private final AtomicInteger failedRequests = new AtomicInteger();
        private final AtomicInteger failedResponses = new AtomicInteger();

        private void clear() {
            requestsSent = 0;
            requestsReceived = 0;
            responsesSent = 0;
            responsesReceived = 0;
            responsesDroppedByMe = 0;
            responsesDroppedByOthers = 0;
            backPressureApplied = false;
            processRequestLowMillis = 0;
            processRequestHighMillis = 0;
            failedRequests.set(0);
            failedResponses.set(0);
        }

        private synchronized String getAndReset() {
            String ret = gson.toJson(this);
            clear();
            return ret;
        }

        private synchronized void processRequestMillis(int millis) {
            if (millis == 0)
                millis = 1;
            if (processRequestLowMillis == 0 || millis < processRequestLowMillis)
                processRequestLowMillis = millis;
            if (millis > processRequestHighMillis)
                processRequestHighMillis = millis;
        }
    }
}
