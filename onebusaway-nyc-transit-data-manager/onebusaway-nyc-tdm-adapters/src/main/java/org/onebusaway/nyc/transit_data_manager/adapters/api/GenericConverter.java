package org.onebusaway.nyc.transit_data_manager.adapters.api;

import java.io.File;
import java.io.IOException;

import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.FileToFileConverterProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotsToJsonOutputProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotsToTcipXmlProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsCrewAssignsToJsonOutputProcess;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsCrewAssignsToTcipXmlProcess;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

public class GenericConverter {

  FileToFileConverterProcess conv = null;

  private String mtaBusDepotToAllDepotsJson = "mtabd2alldepots";
  private String mtaBusDepotToTcipXml = "mtabd2tcipxml";
  private String mtaUtsCrewToCrewJson = "utsCrew2crewjson";
  private String mtaUtsCrewToTcipXml = "utsCrew2tcipxml";

  public GenericConverter(String convType, File inputFile, File outputFile) {
    if (mtaBusDepotToAllDepotsJson.equals(convType)) {
      conv = new MtaBusDepotsToJsonOutputProcess(inputFile, outputFile);
    } else if (mtaBusDepotToTcipXml.equals(convType)) {
      conv = new MtaBusDepotsToTcipXmlProcess(inputFile, outputFile);
    } else if (mtaUtsCrewToCrewJson.equals(convType)) {
      conv = new UtsCrewAssignsToJsonOutputProcess(inputFile, outputFile);
    } else if (mtaUtsCrewToTcipXml.equals(convType)) {
      conv = new UtsCrewAssignsToTcipXmlProcess(inputFile, outputFile);
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

    try {
      GenericOptions ops = CliFactory.parseArguments(GenericOptions.class, args);

      inputFile = ops.getFiles().get(0);
      outputFile = ops.getFiles().get(1);

      ex = new GenericConverter(ops.getType(), inputFile, outputFile);

      ex.runConversion();
    } catch (ArgumentValidationException e) {
      System.out.print(e.getMessage());
    } catch (NullPointerException e) {
      System.out.println("Please add input and output files as arguments.");
    } catch (IOException e) {
      System.out.println("Had trouble writing output file to disk. Please investigate output file.");
    }

  }

}
