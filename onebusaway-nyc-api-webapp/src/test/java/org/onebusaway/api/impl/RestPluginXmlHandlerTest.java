/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.api.impl;

import static org.junit.Assert.assertNotNull;

import org.apache.struts2.rest.handler.JacksonXmlHandler;
import org.junit.Test;

/**
 * Guards the webapp's classpath: the Struts REST plugin builds its default
 * Jackson XML handler at startup, so the handler must be loadable.
 */
public class RestPluginXmlHandlerTest {

    /**
     * Fails with NoClassDefFoundError if jackson-dataformat-xml (or a StAX
     * provider) is missing, which is what broke every request on dev.
     */
    @Test
    public void testDefaultXmlHandlerLoads() {
        assertNotNull(new JacksonXmlHandler());
    }
}
