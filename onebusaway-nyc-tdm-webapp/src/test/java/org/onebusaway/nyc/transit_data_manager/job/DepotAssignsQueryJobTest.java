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

package org.onebusaway.nyc.transit_data_manager.job;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.quartz.JobDataMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.security.auth.login.Configuration;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;

import static org.mockito.Mockito.when;

public class DepotAssignsQueryJobTest {

    DepotAssignsQueryJob depotAssignsQueryJob;

    @Mock
    private ConfigurationService configurationService;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);

        depotAssignsQueryJob = new DepotAssignsQueryJob();

        when(configurationService.getConfigurationValueAsInteger(
                "tdm.minimumLines", depotAssignsQueryJob.DEFAULT_MINIMUM_LINES))
                .thenReturn(depotAssignsQueryJob.DEFAULT_MINIMUM_LINES);

        depotAssignsQueryJob.setConfigurationService(configurationService);
    }

    @Test
    public void testDepotAssignsJob() throws ParserConfigurationException, SAXException, IOException, TransformerException {
        InputStream resource = this.getClass().getResourceAsStream("depot_assigns_input_2021_04_26.xml");
        DOMSource source = depotAssignsQueryJob.getXmlDOMSourceFromStream(resource);
        Assert.assertNotNull(source);
        Assert.assertTrue(source.getNode().hasChildNodes());

        Date date = new Date();
        String filePathDate = depotAssignsQueryJob.getFormattedFilePathDate(date);
        String expectedFilePath = System.getProperty("java.io.tmpdir") + File.separator +
                depotAssignsQueryJob.FILE_NAME_PREFIX + filePathDate + ".xml";
        String actualFilePath = depotAssignsQueryJob.getFilePath(System.getProperty("java.io.tmpdir"), date);
        Assert.assertEquals(expectedFilePath,actualFilePath);

        File file = depotAssignsQueryJob.saveDOMSourceToXmlFile(source, actualFilePath);
        file.deleteOnExit();

        Assert.assertNotNull(file);
        Assert.assertTrue(file.exists());

        Assert.assertTrue(depotAssignsQueryJob.validateDepotAssignsFile(file));

        int actualNumberOfLines = depotAssignsQueryJob.getNumberOfLinesForFile(file);
        Assert.assertEquals(120517, actualNumberOfLines);


    }

    @Test
    public void testSmallDepotAssignsJob() throws ParserConfigurationException, SAXException, IOException, TransformerException {
        InputStream resource = this.getClass().getResourceAsStream("depot_assigns_input_small_2021_04_26.xml");
        DOMSource source = depotAssignsQueryJob.getXmlDOMSourceFromStream(resource);
        Assert.assertNotNull(source);
        Assert.assertTrue(source.getNode().hasChildNodes());

        Date date = new Date();
        String filePathDate = depotAssignsQueryJob.getFormattedFilePathDate(date);
        String expectedFilePath = System.getProperty("java.io.tmpdir") + File.separator +
                depotAssignsQueryJob.FILE_NAME_PREFIX + filePathDate + ".xml";
        String actualFilePath = depotAssignsQueryJob.getFilePath(System.getProperty("java.io.tmpdir"), date);
        Assert.assertEquals(expectedFilePath,actualFilePath);

        File file = depotAssignsQueryJob.saveDOMSourceToXmlFile(source, actualFilePath);
        Assert.assertNotNull(file);

        Assert.assertFalse(depotAssignsQueryJob.validateDepotAssignsFile(file));
        Assert.assertFalse(file.exists());
    }

}
