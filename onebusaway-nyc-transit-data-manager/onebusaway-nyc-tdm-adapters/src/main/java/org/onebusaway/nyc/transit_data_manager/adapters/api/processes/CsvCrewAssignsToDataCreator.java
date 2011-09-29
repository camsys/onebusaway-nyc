package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterSignCodeData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.SignCodeData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.CSVCcAnnouncementInfoConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.CcAnnouncementInfoConverter;

import tcip_final_3_0_5_1.CCDestinationSignMessage;

public class CsvCrewAssignsToDataCreator {

  private File inputFile;

  public CsvCrewAssignsToDataCreator(File inputFile) {
    this.inputFile = inputFile;
  }

  public SignCodeData generateDataObject() throws IOException {
    FileReader inputFileReader = new FileReader(inputFile);

    CcAnnouncementInfoConverter inConv = new CSVCcAnnouncementInfoConverter(
        inputFileReader);

    List<CCDestinationSignMessage> messages = inConv.getDestinationsAsList();

    inputFileReader.close();

    SignCodeData data = new ImporterSignCodeData(messages);

    return data;
  }
}
