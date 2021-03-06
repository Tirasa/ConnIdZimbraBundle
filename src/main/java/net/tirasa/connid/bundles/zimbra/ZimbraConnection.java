/**
 * Copyright (C) 2017 ConnId (connid-dev@googlegroups.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tirasa.connid.bundles.zimbra;

/**
 * Class to represent a Zimbra Connection.
 */
public class ZimbraConnection {

    private final ZimbraConfiguration configuration;

    /**
     * Constructor of ZimbraConnection class.
     *
     * @param configuration the actual {@link ZimbraConfiguration}
     */
    public ZimbraConnection(ZimbraConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Release internal resources.
     */
    public void dispose() {
        //implementation
    }

    /**
     * If internal connection is not usable, throw IllegalStateException.
     */
    public void test() {
        //implementation
    }

}
