package org.onebusaway.nyc.transit_data_manager.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsPulloutsToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.PulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.PullInOutFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.PullInOut;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.SCHPullInOutInfo;

@Path("/pullinpullout/{serviceDate}/list")
@Component
@Scope("request")
public class VehiclePullInPullOutResource {
  @GET
  @Produces("application/json")
  public String getVehiclePullouts(@PathParam("serviceDate")
  String inputDateStr) {
    DateTimeFormatter dateDTF = ISODateTimeFormat.date();
    DateMidnight serviceDate = new DateMidnight(
        dateDTF.parseDateTime(inputDateStr));

    File inputFile = new File(System.getProperty("datapath")
        + System.getProperty("pipoFilename"));

    PulloutData data = null;

    UtsPulloutsToDataCreator process = new UtsPulloutsToDataCreator(inputFile);

    try {
      data = process.generateDataObject();
    } catch (IOException e) {
      return e.getMessage();
    }

    ModelCounterpartConverter<SCHPullInOutInfo, PullInOut> tcipToJsonConverter = new PullInOutFromTcip();

    List<PullInOut> pullIns = null;
    List<PullInOut> pullOuts = null;

    return "\"This functionality not yet implemented\"";

  }
}
