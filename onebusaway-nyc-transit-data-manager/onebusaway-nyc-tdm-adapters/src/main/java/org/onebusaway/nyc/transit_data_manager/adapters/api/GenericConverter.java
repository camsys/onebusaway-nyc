package org.onebusaway.nyc.transit_data_manager.adapters.api;

import java.io.File;
import java.io.IOException;

import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.FileToFileConverterProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotsToJsonOutputProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotsToTcipXmlProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaSignCodeToTcipXmlProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsCrewAssignsToJsonOutputProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsCrewAssignsToTcipXmlProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsVehiclePulloutToTcipXmlProcess;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

public class GenericConverter {

  FileToFileConverterProcess conv = null;

  private String mtaBusDepotToAllDepotsJson = "mtabd2alldepots";
  private String mtaBusDepotToTcipXml = "mtabd2tcipxml";
  private String mtaUtsCrewToCrewJson = "utsCrew2crewjson";
  private String mtaUtsCrewToTcipXml = "utsCrew2tcipxml";
  private String mtaUtsPipoToTcipXml = "utsPipo2tcipxml";
  private String mtaDscToTcipXml = "dsccsv2tcipxml";

  public GenericConverter(String convType, File inputFile, File outputFile) {
    if (mtaBusDepotToAllDepotsJson.equals(convType)) {
      conv = new MtaBusDepotsToJsonOutputProcess(inputFile, outputFile);
    } else if (mtaBusDepotToTcipXml.equals(convType)) {
      conv = new MtaBusDepotsToTcipXmlProcess(inputFile, outputFile);
    } else if (mtaUtsCrewToCrewJson.equals(convType)) {
      conv = new UtsCrewAssignsToJsonOutputProcess(inputFile, outputFile);
    } else if (mtaUtsCrewToTcipXml.equals(convType)) {
      conv = new UtsCrewAssignsToTcipXmlProcess(inputFile, outputFile);
    } else if (mtaUtsPipoToTcipXml.equals(convType)) {
      conv = new UtsVehiclePulloutToTcipXmlProcess(inputFile, outputFile);
    } else if (mtaDscToTcipXml.equals(convType)) {
      conv = new MtaSignCodeToTcipXmlProcess(inputFile, outputFile);
    }
  }

  private void runConversion() throws IOException {
    conv.executeProcess();

    conv.writeToFile();
  }

  /**
   * Takes three arguments - a 'type' argument with one of the types above, the input file and the output file.
   * @param args
   */
  public static void main(String[] args) {
    GenericConverter ex = null;

    File inputFile = null;
    File outputFile = null;

    GenericOptions ops = null;
    
    String conversionTypeStr = null;
    try {
      ops = CliFactory.parseArguments(GenericOptions.class, args);
      
      inputFile = ops.getFiles().get(0);
      outputFile = ops.getFiles().get(1);
      
      conversionTypeStr = ops.getType();
    } catch (ArgumentValidationException e1) {
      System.out.print(e1.getMessage());
    }

    if (conversionTypeStr != null) {
      ex = new GenericConverter(conversionTypeStr, inputFile, outputFile);
      
      try {
        ex.runConversion();
      } catch (IOException e1) {
        System.out.println("Had trouble writing output file to disk. Please investigate output file.");
      }
    }

  }

}
