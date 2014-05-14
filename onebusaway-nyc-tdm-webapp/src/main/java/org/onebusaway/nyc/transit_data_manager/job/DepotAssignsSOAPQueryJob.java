package org.onebusaway.nyc.transit_data_manager.job;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Persists TDM web service data every day at 4 a.m.
 * 
 */
public class DepotAssignsSOAPQueryJob extends QuartzJobBean {

	
	private ConfigurationService _configurationService;
	private String _depotFileDir;
	private int DEFAULT_MINIMUM_LINES = 5000;
	
	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
		_configurationService = configurationService;
	}
	
	@Autowired
	public void setDepotFileDir(String depotFileDir) {
		_depotFileDir = depotFileDir;
	}

	private static Logger _log = LoggerFactory.getLogger(DepotAssignsSOAPQueryJob.class);

	@Override
	protected void executeInternal(JobExecutionContext executionContext)
			throws JobExecutionException {
		try {
            // Create SOAP Connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // Send SOAP Message to SOAP Server
            String url = "http://dobmobile.nyct.com/prwebservices/cis.asmx?WSDL";
            SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(), url);
            
            // Process the SOAP Response
            printSOAPResponse(soapResponse);
            soapConnection.close();
        } catch (Exception e) {
            _log.error("Error occurred while sending SOAP Request to Server");
            e.printStackTrace();
        }
	}
	
	private SOAPMessage createSOAPRequest() throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        String serverURI = "http://mtlivbus/";
        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", serverURI  + "BusAssignment");
        soapMessage.saveChanges();
        return soapMessage;
    }

    /**
     * Method used to print the SOAP Response
     */
    private void printSOAPResponse(SOAPMessage soapResponse) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "text");
        Source sourceContent = soapResponse.getSOAPPart().getContent();
        String date = new SimpleDateFormat("yyyyMMdd_kkmm").format(new Date());
        String path = System.getProperty(_depotFileDir)+"/"+"depot_assignments"+date+".xml";
        File file = new File(path);
        StreamResult result = new StreamResult(file);
        transformer.transform(sourceContent, result);
        LineNumberReader  lnr = new LineNumberReader(new FileReader(file));
        lnr.skip(Long.MAX_VALUE);
        int lines = lnr.getLineNumber(); 
        lnr.close();
        int minimumLines = getMinimumLines();
        if (lines < minimumLines){
        	_log.error("Insufficient lines. (Lines:" + lines + ", Minimum: " + minimumLines + ")");
        	file.delete();
        } else {
        	_log.info(file.getAbsoluteFile()+"(Lines:" + lines + ", Minimum: " + minimumLines + ")");
        }
    }
    
    private int getMinimumLines() {
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
