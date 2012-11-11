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
import org.onebusaway.nyc.transit_data_manager.adapters.input.CrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPCrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVCrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.OperatorAssignmentFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.ServiceDateOperatorAssignments;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.ListServiceDateOperatorAssignmentsMessage;

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
    
    UtsCrewAssignsToDataCreator dataCreator = new UtsCrewAssignsToDataCreator(inputFile);
    
    OperatorAssignmentData data;
    
    data = dataCreator.generateDataObject();

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

      jsonOpAssigns = new UTSUtil().listConvertOpAssignTcipToJson(tcipToJsonConverter,
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

}
