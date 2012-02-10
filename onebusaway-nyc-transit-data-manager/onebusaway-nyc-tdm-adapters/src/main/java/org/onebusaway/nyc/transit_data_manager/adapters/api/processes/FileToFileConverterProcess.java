package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

}
