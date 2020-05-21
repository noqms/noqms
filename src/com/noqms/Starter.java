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

import java.util.Properties;

import com.noqms.framework.Harness;
import com.noqms.framework.Util;

/**
 * Methods for starting a microservice from the given properties.
 * 
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class Starter {
    public static final String ARG_GROUP_NAME = "noqms.groupName";
    public static final String ARG_SERVICE_NAME = "noqms.serviceName";
    public static final String ARG_SERVICE_PATH = "noqms.servicePath";
    public static final String ARG_THREADS = "noqms.threads";
    public static final String ARG_TYPICAL_MILLIS = "noqms.typicalMillis";
    public static final String ARG_TIMEOUT_MILLIS = "noqms.timeoutMillis";
    public static final String ARG_MAX_MESSAGE_OUT_BYTES = "noqms.maxMessageOutBytes";
    public static final String ARG_MAX_MESSAGE_IN_BYTES = "noqms.maxMessageInBytes";
    public static final String ARG_EMITTER_INTERVAL_SECONDS = "noqms.emitterIntervalSeconds";
    public static final String ARG_SERVICE_UNAVAILABLE_SECONDS = "noqms.serviceUnavailableSeconds";
    public static final String ARG_SERVICE_FINDER_PATH = "noqms.serviceFinderPath";
    public static final String ARG_DATA_PORT = "noqms.dataPort";

    /**
     * Start the microservice at noqms.servicePath and with the following specified property key/value pairs.
     * 
     * @param noqms.groupName                 name of your group of interconnected microservices - must be the same
     *                                        between microservices intended to communicate with each other
     * 
     * @param noqms.serviceName               microservice name - must be unique among interconnected microservice types
     *                                        - instances of the same microservice have the same microservice name
     * 
     * @param noqms.servicePath               com.x.x.x full path of your microservice - can reside anywhere on your
     *                                        classpath
     * 
     * @param noqms.threads                   number of threads simultaneously executing your microservice code -
     *                                        increase to fully utilize your resources (cpu/memory/disk) - consider 10s
     *                                        or 100s per core
     * 
     * @param noqms.typicalMillis             typical execution time of your microservice under normal circumstances -
     *                                        the back pressure threshold is roughly determined by threads *
     *                                        (timeoutMillis / typicalMillis)
     * 
     * @param noqms.timeoutMillis             time after which unanswered requests to your microservice are considered
     *                                        failed for whatever reason - the back pressure threshold is roughly
     *                                        determined by threads * (timeoutMillis / typicalMillis)
     * 
     * @param noqms.maxMessageOutBytes        max bytes for outgoing messages from your microservice, including both
     *                                        requests and responses from you
     * 
     * @param noqms.maxMessageInBytes         max bytes for incoming messages to your microservice, including both
     *                                        requests and responses to you
     * 
     * @param noqms.emitterIntervalSeconds    default=2 - interval that microservice info is broadcast - must be the
     *                                        same between interconnected microservices
     * 
     * @param noqms.serviceUnavailableSeconds default=5 - interval after which a microservice is considered dead or
     *                                        unavailable if serviceInfo has not been received for it - must be the same
     *                                        between interconnected microservices
     * 
     * @param noqms.serviceFinderPath         default="com.noqms.finder.multicast.ServiceFinderMulticast" - the full
     *                                        path of a pluggable microservice discovery mechanism - can be anywhere on
     *                                        your classpath
     * 
     * @param noqms.dataPort                  default=any available - UDP port this service reads for incoming
     *                                        microservice application data
     */

    /**
     * Start a microservice.
     * 
     * @param props key/value pairs - see above (Starter.java source) for names and descriptions
     * @return an instance of the microservice at noqms.servicePath
     */
    public static MicroService start(Properties props) throws Exception {
        return new Harness().start(props, null);
    }

    /**
     * Start a microservice with an optional external logger.
     * 
     * @param props          key/value pairs - see above (Starter.java source) for names and descriptions
     * @param externalLogger an optional real logger; otherwise it is just stdout and stderr
     * @return an instance of the microservice at noqms.servicePath
     */
    public static MicroService start(Properties props, LogListener externalLogger) throws Exception {
        return new Harness().start(props, externalLogger);
    }
}
