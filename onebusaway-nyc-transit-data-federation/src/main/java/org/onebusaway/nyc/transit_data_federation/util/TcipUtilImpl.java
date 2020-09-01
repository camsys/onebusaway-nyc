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

package org.onebusaway.nyc.transit_data_federation.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.ObjectFactory;

import javax.annotation.PostConstruct;
import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

@Component
public class TcipUtilImpl implements TcipUtil{
    private ObjectMapper _mapper = null;
    private JAXBContext context = null;
    private static Logger _log = LoggerFactory.getLogger(TcipUtilImpl.class);

    @PostConstruct
    @Override
    public void setup(){
        setupMapper();
        setupJaxbContext();
    }

    private void setupMapper(){
        _mapper = new ObjectMapper();
        _mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private void setupJaxbContext(){
        try {
            context = JAXBContext.newInstance(
                    ObaSchPullOutList.class);
        } catch(Exception e) {
            _log.error("Failed to Serialize ObaSchPullOutList to XML", e);
        }
    }

    @Override
    public ObaSchPullOutList getFromJsonUrl(String url) throws IOException {
        ObaSchPullOutList response = _mapper.readValue(url, ObaSchPullOutList.class);
        return response;
    }

    @Override
    public String getAsJson(ObaSchPullOutList o) throws JsonProcessingException {
        return _mapper.writeValueAsString(o);
    }

    @Override
    public ObaSchPullOutList getFromXmlUrl(URL url) throws JAXBException {
        JAXBContext jaxbContext  = JAXBContext.newInstance(ObaSchPullOutList.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ObaSchPullOutList list = (ObaSchPullOutList) jaxbUnmarshaller.unmarshal(url);
        return list;
    }

    @Override
    public ObaSchPullOutList getFromXml(String xml) throws XMLStreamException, JAXBException {
        XMLInputFactory xmlInputFact = XMLInputFactory.newInstance();
        XMLStreamReader reader = xmlInputFact.createXMLStreamReader(
                new StringReader(xml));

        Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();

        JAXBElement<ObaSchPullOutList> doc = (JAXBElement<ObaSchPullOutList>) jaxbUnmarshaller.unmarshal(reader,ObaSchPullOutList.class);
        return doc.getValue();
    }

    @Override
    public String getAsXml(ObaSchPullOutList o) throws JAXBException{
        ObjectFactory f = new ObjectFactory();
        JAXBElement<ObaSchPullOutList> pullOutListElement = f.createObaSchPullOutList(o);
        Marshaller m = JAXBContext.newInstance(ObjectFactory.class).createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        StringWriter writer = new StringWriter();

        m.marshal(pullOutListElement, writer);
        return writer.toString();
    }
}
