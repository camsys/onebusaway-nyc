package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterOperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.OperatorAssignmentFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.ServiceDateOperatorAssignments;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.ListServiceDateOperatorAssignmentsMessage;
import org.onebusaway.nyc.transit_data_manager.importers.CSVCrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.CrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.CrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.TCIPCrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class UtsCrewAssignsToJsonOutputProcess extends
    FileToFileConverterProcess {

  public UtsCrewAssignsToJsonOutputProcess(File inputFile, File outputFile) {
    super(inputFile, outputFile);
  }

  @Override
  public void executeProcess() throws IOException {
    FileReader inputFileReader = new FileReader(inputFile);

    CrewAssignsInputConverter inConv = new CSVCrewAssignsInputConverter(
        inputFileReader);

    List<MtaUtsCrewAssignment> crewAssignments = inConv.getCrewAssignments();

    inputFileReader.close();

    System.out.println("ran getCrewAssignments and got "
        + crewAssignments.size() + " results");

    CrewAssignmentsOutputConverter converter = new TCIPCrewAssignmentsOutputConverter(
        crewAssignments);
    List<SCHOperatorAssignment> opAssignments = converter.convertAssignments();

    // Set up a data object to interface with the tcip data.
    OperatorAssignmentData data = new ImporterOperatorAssignmentData(
        opAssignments); // a data object to represent the data.

    ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> tcipToJsonConverter = new OperatorAssignmentFromTcip();

    // grab the list of dates present in the data.
    List<DateMidnight> sDates = data.getAllServiceDates();

    // iterate through each date with sDateIt
    Iterator<DateMidnight> sDateIt = sDates.iterator();

    DateMidnight thisDate = null; // The specific date we are converting
                                  // assignments for

    ServiceDateOperatorAssignments assignsForDateJson = null;
    List<OperatorAssignment> jsonOpAssigns = null; // the

    List<ServiceDateOperatorAssignments> dateAssignments = new ArrayList<ServiceDateOperatorAssignments>();

    while (sDateIt.hasNext()) { // for each date in the input dates in the data.
      assignsForDateJson = new ServiceDateOperatorAssignments();

      thisDate = sDateIt.next();
      DateTimeFormatter dateDTF = ISODateTimeFormat.date();
      assignsForDateJson.setServiceDate(dateDTF.print(thisDate));

      jsonOpAssigns = listConvertOpAssignTcipToJson(tcipToJsonConverter,
          data.getOperatorAssignmentsByServiceDate(thisDate)); // grab the
                                                               // assigns for
                                                               // this date and
                                                               // convert to
                                                               // json
      assignsForDateJson.setCrew(jsonOpAssigns);

      dateAssignments.add(assignsForDateJson);
    }

    // now generate the output message.
    ListServiceDateOperatorAssignmentsMessage allAssignsMessage = new ListServiceDateOperatorAssignmentsMessage();
    allAssignsMessage.setAssignments(dateAssignments);
    allAssignsMessage.setStatus("OK");

    Gson gson = new GsonBuilder().setFieldNamingPolicy(
        FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().create();
    output = gson.toJson(allAssignsMessage);

  }

  private List<OperatorAssignment> listConvertOpAssignTcipToJson(
      ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> conv,
      List<SCHOperatorAssignment> inputAssigns) {
    List<OperatorAssignment> outputAssigns = new ArrayList<OperatorAssignment>();

    Iterator<SCHOperatorAssignment> assignTcipIt = inputAssigns.iterator();

    while (assignTcipIt.hasNext()) {
      outputAssigns.add(conv.convert(assignTcipIt.next()));
    }

    return outputAssigns;
  }

}
