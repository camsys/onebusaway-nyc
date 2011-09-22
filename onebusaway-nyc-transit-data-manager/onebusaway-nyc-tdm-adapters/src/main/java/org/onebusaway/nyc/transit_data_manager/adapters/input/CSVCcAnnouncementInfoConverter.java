package org.onebusaway.nyc.transit_data_manager.adapters.input;

import tcip_final_3_0_5_1.*;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;

public class CSVCcAnnouncementInfoConverter implements CcAnnouncementInfoConverter {
    public static final int ROUTE = 0;
    public static final int DSC = 1;
    public static final int MESSAGE = 2;
    public static final int DIRECTION = 3;
    public static final int DEPOT = 4;
    private static final boolean DEBUG = false;
    private Reader inputReader = null;
    private CSVReader csvReader = null;

    public CSVCcAnnouncementInfoConverter (Reader csvInputReader) {
	inputReader = csvInputReader;
    }

    public CcAnnouncementInfo.Destinations getDestinations() {
	setupCsvReader();
	CcAnnouncementInfo.Destinations destinations = new CcAnnouncementInfo.Destinations();
	try {
	    // swallow header
	    csvReader.readNext();

	    String[] lastLine = null;
	    String[] line = csvReader.readNext();
	    // simple lookahead strategy to capture multi-line message text
	    while (line != null) {
		debug(line);
		// check to see if this line is complete or an artifact of merged cells
		if (StringUtils.isBlank(line[ROUTE]) && StringUtils.isBlank(line[DSC])) {
		    // we have a continuation
		    line = append(lastLine, line);
		} else {
		    // we have a complete line, that means we should flush our lookbehind
		    if (lastLine != null) {
			destinations.getDestination().add(convert(lastLine));
			lastLine = null;
		    }
		}
		lastLine = line;
		line = csvReader.readNext();
	    }
	    if (lastLine != null) {
		destinations.getDestination().add(convert(lastLine));
	    }
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
	return destinations;
    }
    
    /*
     * line format
     * 0) Route
     * 1) DSC
     * 2) Message
     * 3) Direction
     * 4) Depot
     */
    private CCDestinationSignMessage convert(String[] line) {
	CCDestinationSignMessage msg = new CCDestinationSignMessage();
	msg.setMetadata(createMetaData(line));
	msg.setRouteID(createRouteID(line));
	msg.setMessageID(createMessageID(line));
	msg.setMessageText(line[MESSAGE]);
	msg.setDirection(line[DIRECTION]);
	
	return msg;
    }

    private CCDestinationMessageIden createMessageID(String[] line) {
	CCDestinationMessageIden id = new CCDestinationMessageIden();
	try {
	    id.setMsgID(Long.parseLong(line[DSC]));
	} catch (NumberFormatException nfe) {
	    id.setName(line[DSC]);
	}
	return id;
    }

    private SCHRouteIden createRouteID(String[] line) {
	SCHRouteIden id = new SCHRouteIden();
	id.setRouteId(0);
	id.setRouteName(line[ROUTE]);
	return id;
    }
    
    private CPTRowMetaData createMetaData(String[] line) {
	CPTRowMetaData md = new CPTRowMetaData();
	DateTimeFormatter dtf = DateTimeFormat.forPattern(UtsMappingTool.UTS_TIMESTAMP_FIELD_DATEFORMAT);
	String now = dtf.print(new Date().getTime());
	md.setUpdated(now);
	return md;
    }

    // append multi-line message text from secondLine into firstLine
    private String[] append(String[] firstLine, String[] secondLine) {
	if (firstLine != null & firstLine.length > 2
	    && secondLine != null && secondLine.length > 2) {
	    firstLine[2] = firstLine[2] + " " + secondLine[2]; // add space for formatting
	}
	return firstLine;
    }

    private void setupCsvReader() {
	if (csvReader == null) {
	    csvReader = new CSVReader(inputReader);
	}
    }

    private void debug(String[] line) {
	if (DEBUG) {
	    System.out.println("line=");
	    for (String s : line) {
		System.out.print(s + ",");
	    }
	    System.out.println("");
	}
    }
}