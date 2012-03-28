package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

public abstract class FileToFileConverterProcess {

  protected File inputFile = null;
  protected File outputFile = null;
  
  protected DepotIdTranslator depotIdTranslator = null;

  protected FileWriter outputFileWriter = null;

  protected String output = "";

  public FileToFileConverterProcess(File inputFile, File outputFile) {
    this.inputFile = inputFile;
    this.outputFile = outputFile;
  }

  abstract public void executeProcess() throws IOException;

  public void writeToFile() throws IOException {
    outputFileWriter = new FileWriter(outputFile);

    outputFileWriter.write(this.output);

    outputFileWriter.close();
  }
  
  public void setupDepotIdTranslator(File configFile) throws IOException {
    depotIdTranslator = new DepotIdTranslator(configFile);
  }
  
  protected XMLGregorianCalendar getDefaultRequiredTcipAttrCreated() throws DatatypeConfigurationException {
    // Simply construct a new xmlgregoriancalendar with the current time.
    return dateTimeToXmlGregCal(new DateTime());
  }
  
  protected String getDefaultRequiredTcipAttrSchVersion() {
    return "0";
  }

  protected String getDefaultRequiredTcipAttrNoNameSpaceSchemaLocation() {
    return "0";
  }

  protected BigInteger getDefaultRequiredTcipAttrSourceport() {
    return new BigInteger("0");
  }

  protected String getDefaultRequiredTcipAttrSourceip() {
    return "0.0.0.0";
  }

  protected String getDefaultRequiredTcipAttrSourceapp() {
    return "";
  }
  
  private static XMLGregorianCalendar dateTimeToXmlGregCal(DateTime inputDateTime) throws DatatypeConfigurationException {
    XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar(inputDateTime.toGregorianCalendar());
    return cal;
  }

}
