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

import com.noqms.framework.Framework;
import com.noqms.framework.FrameworkUtil;

/**
 * Provides main and methods for starting a microservice.
 * <p>
 * The typical method, using main() for standalone execution, starts the microservice at servicePath and runs forever.
 * The microservice is the program. Run using "java argname1=value argname2=value ..." format.
 * <p>
 * The start() method, starting execution from your own program/process using start(), starts the microservice at
 * servicePath and returns in 2 * emitterIntervalSeconds (giving time to become aware of other microservices). You can
 * invoke this multiple times with different properties to run multiple microservices from the same process if desired.
 * Typically, however, each microservice would run within its own resource-tailored (virtual) environment. You can also
 * run the same microservice multiple times within the same process - but, instead, run just one and simply increase the
 * threads setting. start() returns the singular instance of your microservice which you can downcast to your
 * microservice class to deal with directly if needed.
 * 
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class Runner {
    public static final String ARG_GROUP_NAME = "groupName";
    public static final String ARG_SERVICE_NAME = "serviceName";
    public static final String ARG_SERVICE_PATH = "servicePath";
    public static final String ARG_THREADS = "threads";
    public static final String ARG_TYPICAL_MILLIS = "typicalMillis";
    public static final String ARG_TIMEOUT_MILLIS = "timeoutMillis";
    public static final String ARG_MAX_MESSAGE_OUT_BYTES = "maxMessageOutBytes";
    public static final String ARG_MAX_MESSAGE_IN_BYTES = "maxMessageInBytes";
    public static final String ARG_EMITTER_INTERVAL_SECONDS = "emitterIntervalSeconds";
    public static final String ARG_SERVICE_UNAVAILABLE_SECONDS = "serviceUnavailableSeconds";
    public static final String ARG_SERVICE_FINDER_PATH = "serviceFinderPath";
    public static final String ARG_LOG_LISTENER_PATH = "logListenerPath";

    /**
     * Start the microservice at servicePath and with the specified command line parameters.
     * 
     * @param args                      command line args with the following names
     * 
     * @param groupName                 name of your group of interconnected microservices - must be the same between
     *                                  microservices intended to communicate with each other
     * 
     * @param serviceName               microservice name - must be unique among interconnected microservices
     * 
     * @param servicePath               com.x.x.x full path of your microservice - can reside anywhere on your classpath
     * 
     * @param threads                   number of threads simultaneously executing your microservice code - increase to
     *                                  fully utilize your resources (cpu/memory/disk) - consider 10s or 100s per core
     * 
     * @param typicalMillis             typical execution time of your microservice under normal circumstances - the
     *                                  back pressure threshold is roughly determined by threads * (timeoutMillis /
     *                                  typicalMillis)
     * 
     * @param timeoutMillis             time after which unanswered requests to your microservice are considered failed
     *                                  for whatever reason - the back pressure threshold is roughly determined by
     *                                  threads * (timeoutMillis / typicalMillis)
     * 
     * @param maxMessageOutBytes        max bytes for outgoing messages from your microservice, including both requests
     *                                  and responses from you
     * 
     * @param maxMessageInBytes         max bytes for incoming messages to your microservice, including both requests
     *                                  and responses to you
     * 
     * @param emitterIntervalSeconds    default=2 - interval that microservice info is broadcast - must be the same
     *                                  between interconnected microservices
     * 
     * @param serviceUnavailableSeconds default=5 - interval after which a microservice is considered dead or
     *                                  unavailable if serviceInfo has not been received for it - must be the same
     *                                  between interconnected microservices
     * 
     * @param serviceFinderPath         default="com.noqms.framework.ServiceFinderMulticast" - the full path of a
     *                                  pluggable microservice discovery mechanism - can be anywhere on your classpath
     * 
     * @param logListenerPath           the full path of an optional listener for external log message processing - can
     *                                  be anywhere on your classpath
     */
    public static void main(String[] args) {
        Properties props = null;
        try {
            props = FrameworkUtil.argsToProps(args);
        } catch (Exception ex) {
            System.err.println("Noqms: error parsing command line arguments: " + ex.getMessage());
            return;
        }
        start(props);
        FrameworkUtil.sleepMillis(Integer.MAX_VALUE);
    }

    /**
     * @param props key/value pairs - see {@link Runner#main main} for names and descriptions
     * @return an instance of the microservice at servicePath
     */
    public static MicroService start(Properties props) {
        return new Framework().start(props);
    }
}
