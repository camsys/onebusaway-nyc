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
import java.net.HttpURLConnection;
import java.net.URL;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * 	Quartz job to invoke Depot Assign web service every hour.
 *
 */
public class DepotAssignsQueryJob extends QuartzJobBean {
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    private ConfigurationService _configurationService;
    private String _depotFileDir;
    public int DEFAULT_MINIMUM_LINES = 5000;
    private int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    public static final String FILE_NAME_PREFIX = "depot_assignments_";

    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    @Autowired
    public void setDepotFileDir(String depotFileDir) {
        _depotFileDir = depotFileDir;
    }

    private static Logger _log = LoggerFactory.getLogger(DepotAssignsSOAPQueryJob.class);


    public String getUrl(){
        return _configurationService.getConfigurationValueAsString("tdm.depotAssigns.url", null);
    }

    public int getConnectionTimeout(){
        return _configurationService.getConfigurationValueAsInteger(
                "tdm.depotAssigns.connectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
    }

    @Override
    protected void executeInternal(JobExecutionContext executionContext) throws JobExecutionException {
        processDepotAssignmentsFromURL();
    }

    public String getFormattedFilePathDate(Date date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public String getFilePath(String filePath, Date date){
        return filePath + File.separator + FILE_NAME_PREFIX + getFormattedFilePathDate(date) +".xml";
    }

    private void processDepotAssignmentsFromURL(){
        String url = getUrl();
        int connectionTimeout = getConnectionTimeout();
        String filePath = getFilePath(System.getProperty(_depotFileDir), new Date());
        if (StringUtils.isBlank(url)) {
            _log.warn("Depot assigns URL not configured, exiting");
            return;
        }
        if(StringUtils.isBlank(filePath)){
            _log.warn("FilePath location not configured, exiting");
            return;
        }
        try{
            InputStream input = getXmlInputStream(url, connectionTimeout);
            DOMSource domSource = getXmlDOMSourceFromStream(input);
            File depotAssignsFile = saveDOMSourceToXmlFile(domSource, filePath);
            validateDepotAssignsFile(depotAssignsFile);
        } catch (SAXException e) {
            _log.error("Error parsing xml into document from input stream}", url, e);
        } catch (ParserConfigurationException e) {
            _log.error("Error parsing xml into document from input stream}", url, e);
        } catch (IOException e) {
            _log.error("Error processing xml input from url {}", url, e);
        } catch (TransformerException e) {
            _log.error("Error tranforming xml input to xml file {}", filePath, e);
        } catch (Exception e) {
            _log.error("Error processing xml input from url {}", url, e);
            e.printStackTrace();
        }

    }

    public InputStream getXmlInputStream(String url, int connectionTimeout) throws IOException {
        long start = System.currentTimeMillis();
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(connectionTimeout);
        InputStream inputStream = connection.getInputStream();
        _log.debug("retrieved " + getUrl() + " in " + (System.currentTimeMillis() - start) + " ms");
        return inputStream;

    }

    public DOMSource getXmlDOMSourceFromStream(InputStream inputStream) throws IOException, SAXException, ParserConfigurationException {
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
        if (lines < minimumLines){
            _log.error("Insufficient lines. (Lines:" + lines + ", Minimum: " + minimumLines + ")");
            file.delete();
            return false;
        } else {
            _log.info(file.getAbsoluteFile()+"(Lines:" + lines + ", Minimum: " + minimumLines + ")");
            return true;
        }

    }

    public int getNumberOfLinesForFile(File file) throws IOException {
        LineNumberReader  lnr = null;
        try{
            lnr = new LineNumberReader(new FileReader(file));
            lnr.skip(Long.MAX_VALUE);
            return lnr.getLineNumber();
        } catch (Exception e) {
            _log.error("Unable to read file {}", file.getAbsolutePath());
            return 0;
        } finally{
            if(lnr != null){
                lnr.close();
            }
        }
    }

    public int getMinimumLines() {
        if (_configurationService != null) {
            try {
                return _configurationService.getConfigurationValueAsInteger("tdm.minimumLines", DEFAULT_MINIMUM_LINES);
            } catch (RemoteConnectFailureException e){
                _log.error("default minimum lines lookup failed:", e);
                return DEFAULT_MINIMUM_LINES;
            }
        }
        return DEFAULT_MINIMUM_LINES;
    }
}
