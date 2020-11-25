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

/**
 * Implement this interface to externally process messages produced by noqms. Noqms logs a limited number of messages to
 * System.err and System.out. These messages can be sent to a LogListener instead.
 * 
 * @author Stanley Barzee
 * @since 1.0.0
 */
public interface LogListener {
    /**
     * @param text descriptive text
     */
    public void debug(String text);

    /**
     * @param text descriptive text
     */
    public void info(String text);

    /**
     * @param text descriptive text
     */
    public void warn(String text);

    /**
     * @param text  descriptive text
     * @param cause may be null
     */
    public void error(String text, Throwable cause);
}
