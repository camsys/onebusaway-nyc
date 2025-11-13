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

package org.onebusaway.nyc.transit_data_manager.job;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Quartz job to invoke Depot Assign fetching every hour.
 * Supports both HTTP and S3 sources - configured via Spring XML by commenting/uncommenting the source bean.
 *
 */
public class DepotAssignsQueryJob extends QuartzJobBean {

    private static final Logger _log = LoggerFactory.getLogger(DepotAssignsQueryJob.class);

    public static final String FILE_NAME_PREFIX = "depot_assignments_";
    public static final int DEFAULT_MINIMUM_LINES = 5000;

    private ConfigurationService _configurationService;
    private String _depotFileDir;
    private DepotAssignsSource _depotAssignsSource;

    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    public void setDepotAssignsSource(DepotAssignsSource depotAssignsSource) {
        _depotAssignsSource = depotAssignsSource;
    }

    public void setDepotFileDir(String depotFileDir) {
        _depotFileDir = depotFileDir;
    }

    @Override
    protected void executeInternal(JobExecutionContext executionContext) throws JobExecutionException {
        processDepotAssignments();
    }

    private void processDepotAssignments() {
        DepotAssignsSource source = _depotAssignsSource;

        if (source == null) {
            _log.error("No depot assigns source is configured. Please configure in data sources xml.");
            return;
        }

        String filePath = getFilePath(System.getProperty(_depotFileDir), new Date());
        if (StringUtils.isBlank(filePath)) {
            _log.warn("FilePath location not configured, exiting");
            return;
        }

        _log.info("Using {} source to fetch depot assignments", source.getSourceType());

        try {
            InputStream input = source.fetchDepotAssignments();

            DOMSource domSource = getXmlDOMSourceFromStream(input);
            File depotAssignsFile = saveDOMSourceToXmlFile(domSource, filePath);

            validateDepotAssignsFile(depotAssignsFile);

        } catch (SAXException e) {
            _log.error("Error parsing xml into document from input stream", e);
        } catch (ParserConfigurationException e) {
            _log.error("Error parsing xml into document from input stream", e);
        } catch (IOException e) {
            _log.error("Error processing xml input from {} source", source.getSourceType(), e);
        } catch (TransformerException e) {
            _log.error("Error transforming xml input to xml file {}", filePath, e);
        } catch (Exception e) {
            _log.error("Error processing xml input from {} source", source.getSourceType(), e);
            e.printStackTrace();
        }
    }

    public String getFormattedFilePathDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public String getFilePath(String filePath, Date date) {
        return filePath + File.separator + FILE_NAME_PREFIX + getFormattedFilePathDate(date) + ".xml";
    }

    public DOMSource getXmlDOMSourceFromStream(InputStream inputStream)
            throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        Document document = docBuilder.parse(inputStream);
        DOMSource source = new DOMSource(document);
        return source;
    }

    public File saveDOMSourceToXmlFile(DOMSource sourceContent, String filePath) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        File file = new File(filePath);
        StreamResult result = new StreamResult(file);
        transformer.transform(sourceContent, result);

        return file;
    }

    public boolean validateDepotAssignsFile(File file) throws IOException {
        int lines = getNumberOfLinesForFile(file);
        int minimumLines = getMinimumLines();

        if (lines < minimumLines) {
            _log.error("Insufficient lines. (Lines: {}, Minimum: {})", lines, minimumLines);
            file.delete();
            return false;
        } else {
            _log.info("{} (Lines: {}, Minimum: {})", file.getAbsoluteFile(), lines, minimumLines);
            return true;
        }
    }

    public int getNumberOfLinesForFile(File file) throws IOException {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new FileReader(file));
            lnr.skip(Long.MAX_VALUE);
            return lnr.getLineNumber();
        } catch (Exception e) {
            _log.error("Unable to read file {}", file.getAbsolutePath());
            return 0;
        } finally {
            if (lnr != null) {
                lnr.close();
            }
        }
    }

    public int getMinimumLines() {
        if (_configurationService != null) {
            try {
                return _configurationService.getConfigurationValueAsInteger("tdm.minimumLines", DEFAULT_MINIMUM_LINES);
            } catch (RemoteConnectFailureException e) {
                _log.error("Default minimum lines lookup failed:", e);
                return DEFAULT_MINIMUM_LINES;
            }
        }
        return DEFAULT_MINIMUM_LINES;
    }
}