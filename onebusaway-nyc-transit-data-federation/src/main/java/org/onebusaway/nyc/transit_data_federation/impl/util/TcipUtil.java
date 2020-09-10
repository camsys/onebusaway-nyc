/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.transit_data_federation.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import tcip_final_4_0_0.ObaSchPullOutList;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URL;

public interface TcipUtil {

    @PostConstruct
    void setup();

    ObaSchPullOutList getFromJsonUrl(String url) throws IOException;

    String getAsJson(ObaSchPullOutList o) throws JsonProcessingException;

    ObaSchPullOutList getFromXmlUrl(URL url) throws JAXBException, IOException, XMLStreamException;

    ObaSchPullOutList getFromXml(String xml) throws XMLStreamException, JAXBException;

    String getAsXml(ObaSchPullOutList o) throws JAXBException;
}
