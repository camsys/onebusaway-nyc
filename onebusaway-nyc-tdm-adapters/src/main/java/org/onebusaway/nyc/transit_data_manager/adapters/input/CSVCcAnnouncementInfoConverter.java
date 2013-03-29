package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import tcip_final_3_0_5_1.CCDestinationMessageIden;
import tcip_final_3_0_5_1.CCDestinationSignMessage;
import tcip_final_3_0_5_1.CPTRowMetaData;
import tcip_final_3_0_5_1.CcAnnouncementInfo;
import tcip_final_3_0_5_1.SCHRouteIden;

public class CSVCcAnnouncementInfoConverter implements
    CcAnnouncementInfoConverter {
  
  private static Logger _log = LoggerFactory.getLogger(CSVCcAnnouncementInfoConverter.class);
  
  public static final int AGENCY = 0;
  public static final int ROUTE = 1;
  public static final int DSC = 2;
  public static final int MESSAGE = 3;
  private static final boolean DEBUG = true;
  
  private File csvFile;
  
  public CSVCcAnnouncementInfoConverter(File inputCsvFile) {
    csvFile = inputCsvFile;
  }

  /*
   * Refactored by Scott C to allow me to directly grab a List of messages from
   * the service code. (non-Javadoc)
   * 
   * @see org.onebusaway.nyc.transit_data_manager.adapters.input.
   * CcAnnouncementInfoConverter#getDestinations()
   */
  public CcAnnouncementInfo.Destinations getDestinations() {
    CcAnnouncementInfo.Destinations destinations = new CcAnnouncementInfo.Destinations();

    destinations.getDestination().addAll(getDestinationsAsList());

    return destinations;
  }

  public List<CCDestinationSignMessage> getDestinationsAsList() {
    
    List<CCDestinationSignMessage> messages = new ArrayList<CCDestinationSignMessage>();

    CSVReader csvReader = null;
    
    try {
      csvReader = new CSVReader(new FileReader(csvFile));
      
      // swallow header
      debug(csvReader.readNext());

      String[] lastLine = null;
      String[] line = csvReader.readNext();
      // simple lookahead strategy to capture multi-line message text
      while (line != null) {
        debug(line);

        // check to see if this line is complete or an artifact of merged cells
        if (StringUtils.isBlank(line[ROUTE]) && StringUtils.isBlank(line[DSC])) {
          line = append(lastLine, line);
        } else {
          // we have a complete line, that means we should flush our lookbehind
          if (lastLine != null) {
            messages.add(convert(lastLine));
            lastLine = null;
          }
        }
        lastLine = line;
        line = csvReader.readNext();
      }
      if (lastLine != null) {
        messages.add(convert(lastLine));
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      if (csvReader != null)
        try {
          csvReader.close();
        } catch (IOException e) {}
    }

    return messages;
  }

  /*
   * line format 0) agency 1) Route 2) DSC 3) Message X) ignored if present
   */
  private CCDestinationSignMessage convert(String[] line) {
    CCDestinationSignMessage msg = new CCDestinationSignMessage();
    msg.setMetadata(createMetaData(line));
    msg.setRouteID(createRouteID(line));
    msg.setMessageID(createMessageID(line));
    msg.setMessageText(line[MESSAGE]);

    return msg;
  }

  private CCDestinationMessageIden createMessageID(String[] line) {
    CCDestinationMessageIden id = new CCDestinationMessageIden();
    try {
      id.setMsgID(Long.parseLong(line[DSC]));
    } catch (NumberFormatException nfe) {
      _log.error("invalid dsc id=" + line[DSC]);
      id.setName(line[DSC]);
    }
    return id;
  }

  private SCHRouteIden createRouteID(String[] line) {
    SCHRouteIden id = new SCHRouteIden();
    id.setRouteId(0);
    id.setRouteName(line[ROUTE]);
    id.setAgencydesignator(line[AGENCY]);
    return id;
  }

  private CPTRowMetaData createMetaData(String[] line) {
    CPTRowMetaData md = new CPTRowMetaData();
    DateTimeFormatter dtf = TcipMappingTool.TCIP_DATETIME_FORMATTER;
    String now = dtf.print(new Date().getTime());
    md.setUpdated(now);
    return md;
  }

  // append multi-line message text from secondLine into firstLine
  private String[] append(String[] firstLine, String[] secondLine) {
    if (firstLine != null & firstLine.length > MESSAGE && secondLine != null
        && secondLine.length > MESSAGE) {
      firstLine[MESSAGE] = firstLine[MESSAGE] + " " + secondLine[MESSAGE]; // add space for
                                                         // formatting
    }
    return firstLine;
  }

  private void debug(String[] line) {
    if (DEBUG) {
      System.out.print("line=");
      for (String s : line) {
        System.out.print(s + ",");
      }
      System.out.println("");
    }
  }
}