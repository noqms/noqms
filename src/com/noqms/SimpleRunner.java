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

import com.noqms.framework.Util;

/**
 * A very simple main() to run a microservice based on passed command line parameters. For a full featured runner with
 * logging support and dynamic microservice lifecycle management based on configuration files, see the noqms-runner
 * project.
 *
 * @author Stanley Barzee
 * @since 1.0.0
 */
public class SimpleRunner {
    /**
     * Start the microservice described in the given command line parameters.
     * 
     * @param args key/value pairs - see the {@link Starter} for key/value names and descriptions
     */
    public static void main(String[] args) {
        Properties props = null;
        try {
            props = Util.argsToProps(args);
        } catch (Exception ex) {
            System.err.println("Noqms: Error parsing command line arguments: " + ex.getMessage());
            return;
        }
        try {
            Starter.start(props);
        } catch (Exception ex) {
            System.err.println("Noqms: " + ex.getMessage());
            System.exit(-1);
        }
        Util.sleepMillis(Integer.MAX_VALUE);
    }
}
