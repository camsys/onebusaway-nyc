package org.onebusaway.nyc.transit_data_manager.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.MtaUtsToTcipVehicleAssignmentConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPVehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.VehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.PullInOutFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.VehicleFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.PullInOut;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;
import org.onebusaway.nyc.transit_data_manager.api.VehiclePipoResource.RealtimeVehiclePipoResource;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.VehiclePipoUploadsFilePicker;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutDataProviderServiceImpl;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutService;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutServiceImpl;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.onebusaway.nyc.transit_data_manager.json.LowerCaseWDashesGsonJsonTool;

import tcip_final_3_0_5_1.CPTOperatorIden;
import tcip_final_3_0_5_1.CPTTransitFacilityIden;
import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.SCHPullInOutInfo;
import tcip_final_3_0_5_1.SCHRunIden;
import tcip_final_4_0_0.ObaSchPullOutList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

public class RealtimeVehiclePipoResourceTest {

  private VehiclePipoResource resource;
  private RealtimeVehiclePipoResource rtResource;
  private VehicleAssignmentsOutputConverter converter;
  private JsonTool jsonTool;
  private MtaUtsToTcipVehicleAssignmentConverter dataConverter;
  private ModelCounterpartConverter<VehiclePullInOutInfo, PullInOut> pulloutDataConverter;
  private VehiclePullInOutDataProviderServiceImpl vehiclePullInOutDataProviderService;
  private VehiclePipoUploadsFilePicker vehicleFilePicker;

  @Mock
  private VehiclePullInOutService vehiclePullInOutService;
  
  private static final String PIPO_JSON_FILE = "pullin-pullout-rt-tcip_20150629.json";

  public void setUp(String filename) throws Exception {
    copyInputFiles(filename);
    MockitoAnnotations.initMocks(this);

    System.setProperty("tdm.vehiclepipoUploadDir",
        System.getProperty("java.io.tmpdir"));
    System.setProperty("tdm.depotIdTranslationFile",
        System.getProperty("java.io.tmpdir") + "/depot_ids.csv");
    
    resource = new VehiclePipoResource();
    rtResource = resource.new RealtimeVehiclePipoResource();
    jsonTool = new LowerCaseWDashesGsonJsonTool(true);

    UtsMappingTool mappingTool = new UtsMappingTool();

    converter = new TCIPVehicleAssignmentsOutputConverter();
    dataConverter = new MtaUtsToTcipVehicleAssignmentConverter();
    dataConverter.setMappingTool(mappingTool);

    ((TCIPVehicleAssignmentsOutputConverter) converter)
        .setDataConverter(dataConverter);

    VehicleFromTcip vehicleConverter = new VehicleFromTcip();
    vehicleConverter.setMappingTool(mappingTool);

    pulloutDataConverter = new PullInOutFromTcip();
    ((PullInOutFromTcip) pulloutDataConverter).setVehConv(vehicleConverter);

    vehicleFilePicker = new VehiclePipoUploadsFilePicker(
        "tdm.vehiclepipoUploadDir");

    vehiclePullInOutDataProviderService = new VehiclePullInOutDataProviderServiceImpl();
    vehiclePullInOutDataProviderService.setConverter(converter);
    vehiclePullInOutDataProviderService
        .setMostRecentFilePicker(vehicleFilePicker);
    vehiclePullInOutDataProviderService
        .setPulloutDataConverter(pulloutDataConverter);

    resource.setJsonTool(jsonTool);
    resource.setVehiclePullInOutService(vehiclePullInOutService);
    resource
        .setVehiclePullInOutDataProviderService(vehiclePullInOutDataProviderService);
  }


  @Test
  public void testAllActiveRealtimePullouts() throws Exception {
    setUp(PIPO_JSON_FILE);

    ObjectMapper m = setupJaxb();

    String outputJson = rtResource.getAllActivePullouts(null);

    ObaSchPullOutList pulloutList = (ObaSchPullOutList) m.readValue(outputJson,
        ObaSchPullOutList.class);
    // TODO Use JsonUnit here
    assertEquals(6, pulloutList.getPullOuts().getPullOut().size());
    assertEquals("5698", pulloutList.getPullOuts().getPullOut().get(0)
        .getVehicle().getId());
    assertEquals("1264", pulloutList.getPullOuts().getPullOut().get(1)
        .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(2)
            .getVehicle().getId());
    assertEquals("3937", pulloutList.getPullOuts().getPullOut().get(3)
            .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(4)
            .getVehicle().getId());
    assertEquals("3142", pulloutList.getPullOuts().getPullOut().get(5)
            .getVehicle().getId());

    // Now test with includeAll=true
    outputJson = rtResource.getAllActivePullouts("true");

    pulloutList = (ObaSchPullOutList) m
        .readValue(outputJson, ObaSchPullOutList.class);
    assertEquals(6, pulloutList.getPullOuts().getPullOut().size());
    assertEquals("5698", pulloutList.getPullOuts().getPullOut().get(0)
        .getVehicle().getId());
    assertEquals("1264", pulloutList.getPullOuts().getPullOut().get(1)
        .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(2)
            .getVehicle().getId());
    assertEquals("3937", pulloutList.getPullOuts().getPullOut().get(3)
            .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(4)
            .getVehicle().getId());
    assertEquals("3142", pulloutList.getPullOuts().getPullOut().get(5)
            .getVehicle().getId());

    // Now test with explicit includeAll=false
    outputJson = rtResource.getAllActivePullouts("false");

    pulloutList = (ObaSchPullOutList) m
        .readValue(outputJson, ObaSchPullOutList.class);
    assertEquals(6, pulloutList.getPullOuts().getPullOut().size());
    assertEquals("5698", pulloutList.getPullOuts().getPullOut().get(0)
        .getVehicle().getId());
    assertEquals("1264", pulloutList.getPullOuts().getPullOut().get(1)
        .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(2)
            .getVehicle().getId());
    assertEquals("3937", pulloutList.getPullOuts().getPullOut().get(3)
            .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(4)
            .getVehicle().getId());
    assertEquals("3142", pulloutList.getPullOuts().getPullOut().get(5)
            .getVehicle().getId());
  }

  @Test
  public void testActiveRealtimePulloutsByBus() throws Exception {
	setUp(PIPO_JSON_FILE);

    String outputJson = rtResource.getActivePulloutsForBus("5698", null);

    ObjectMapper m = setupJaxb();

    ObaSchPullOutList pulloutList = (ObaSchPullOutList) m.readValue(outputJson,
        ObaSchPullOutList.class);
    // TODO Use JsonUnit here
    assertEquals(1, pulloutList.getPullOuts().getPullOut().size());
    assertEquals("5698", pulloutList.getPullOuts().getPullOut().get(0)
        .getVehicle().getId());

    // Now test with includeAll=true
    outputJson = rtResource.getActivePulloutsForBus("5698", "true");

    pulloutList = (ObaSchPullOutList) m
        .readValue(outputJson, ObaSchPullOutList.class);
    assertEquals(1, pulloutList.getPullOuts().getPullOut().size());
    assertEquals("5698", pulloutList.getPullOuts().getPullOut().get(0)
        .getVehicle().getId());

    // Now test with explicit includeAll=false
    outputJson = rtResource.getActivePulloutsForBus("5698", "false");

    pulloutList = (ObaSchPullOutList) m
        .readValue(outputJson, ObaSchPullOutList.class);
    assertEquals(1, pulloutList.getPullOuts().getPullOut().size());
    assertEquals("5698", pulloutList.getPullOuts().getPullOut().get(0)
        .getVehicle().getId());
    
    // Now test with null vehicle id
    /*outputJson = rtResource.getActivePulloutsForBus(null, "false");

    pulloutList = (ObaSchPullOutList) m
        .readValue(outputJson, ObaSchPullOutList.class);
    assertEquals(3, pulloutList.getPullOuts().getPullOut().size());
    assertEquals(null, pulloutList.getPullOuts().getPullOut().get(0)
        .getVehicle().getId());*/
    

    // assertTrue(outputJson.contains(" \"vehicle-id\": \"1253\""));
    // assertTrue(outputJson.contains( "\"agency-id-tcip\": \"2008\""));
    // assertTrue(outputJson.contains(" \"agency-id\": \"MTA NYCT\""));
    // assertTrue(outputJson.contains( "\"depot\": \"OS\""));
    // assertTrue(outputJson.contains(" \"service-date\": \"2012-06-15\""));
    // assertTrue(outputJson.contains(
    // "\"pullout-time\": \"2012-06-15T09:23:00-04:00\""));
    // assertTrue(outputJson.contains(" \"run\": \"SBS15-106\""));
    // assertTrue(outputJson.contains(" \"operator-id\": \"1663\""));
    // assertTrue(outputJson.contains(
    // "\"pullin-time\": \"2012-06-15T22:57:00-04:00\""));

    /*
     * { "pullouts": [ { "vehicle-id": "1253", "agency-id-tcip": "2008",
     * "agency-id": "MTA NYCT", "depot": "OS", "service-date": "2012-06-15",
     * "pullout-time": "2012-06-15T09:23:00-04:00", "run": "SBS15-106",
     * "operator-id": "1663", "pullin-time": "2012-06-15T22:57:00-04:00" } ],
     * "status": "OK" }
     */

    // verifyPulloutData(outputJson);
  }

  @Test
  public void testActiveRealtimePulloutsByDepot() throws Exception {
    setUp(PIPO_JSON_FILE);
    
    ObjectMapper m = setupJaxb();

    String outputJson = rtResource.getActivePulloutsForDepot("MJQT", null);

    ObaSchPullOutList pulloutList = (ObaSchPullOutList) m.readValue(outputJson,
        ObaSchPullOutList.class);
    // TODO Use JsonUnit here
    assertEquals(2, pulloutList.getPullOuts().getPullOut().size());
    assertNull(pulloutList.getPullOuts().getPullOut().get(0)
        .getVehicle().getId());
    assertEquals("3937", pulloutList.getPullOuts().getPullOut().get(1)
            .getVehicle().getId());
    
    // Now test with includeAll=true
    outputJson = rtResource.getActivePulloutsForDepot("MJQT", "true");

    pulloutList = (ObaSchPullOutList) m
        .readValue(outputJson, ObaSchPullOutList.class);
    assertEquals(2, pulloutList.getPullOuts().getPullOut().size());
    assertNull(pulloutList.getPullOuts().getPullOut().get(0)
            .getVehicle().getId());
        assertEquals("3937", pulloutList.getPullOuts().getPullOut().get(1)
                .getVehicle().getId());

    // Now test with explicit includeAll=false
    outputJson = rtResource.getActivePulloutsForDepot("MJQT", "false");

    pulloutList = (ObaSchPullOutList) m
        .readValue(outputJson, ObaSchPullOutList.class);
    assertEquals(2, pulloutList.getPullOuts().getPullOut().size());
    assertNull(pulloutList.getPullOuts().getPullOut().get(0)
            .getVehicle().getId());
        assertEquals("3937", pulloutList.getPullOuts().getPullOut().get(1)
                .getVehicle().getId());

  }

  @Test
  public void testActiveRealtimePulloutsByAgency() throws Exception {
    setUp(PIPO_JSON_FILE);
    
    ObjectMapper m = setupJaxb();
    
    String outputJson = rtResource.getActivePulloutsForAgency("MTA NYCT", null);
    ObaSchPullOutList pulloutList = (ObaSchPullOutList) m.readValue(outputJson,
        ObaSchPullOutList.class);
    
    // TODO Use JsonUnit here
    assertEquals(5, pulloutList.getPullOuts().getPullOut().size());
    assertEquals("5698", pulloutList.getPullOuts().getPullOut().get(0)
            .getVehicle().getId());
    assertEquals("1264", pulloutList.getPullOuts().getPullOut().get(1)
        .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(2)
            .getVehicle().getId());
    assertEquals("3937", pulloutList.getPullOuts().getPullOut().get(3)
            .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(4)
            .getVehicle().getId());
    
    // Now test with includeAll=true
    outputJson = rtResource.getActivePulloutsForAgency("MTA NYCT", "true");
    pulloutList = (ObaSchPullOutList) m
        .readValue(outputJson, ObaSchPullOutList.class);
    
    assertEquals(5, pulloutList.getPullOuts().getPullOut().size());
    assertEquals("5698", pulloutList.getPullOuts().getPullOut().get(0)
            .getVehicle().getId());
    assertEquals("1264", pulloutList.getPullOuts().getPullOut().get(1)
        .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(2)
            .getVehicle().getId());
    assertEquals("3937", pulloutList.getPullOuts().getPullOut().get(3)
            .getVehicle().getId());
    assertNull(pulloutList.getPullOuts().getPullOut().get(4)
            .getVehicle().getId());

    // Now test with explicit includeAll=false
    outputJson = rtResource.getActivePulloutsForAgency("MTABC", "false");

    pulloutList = (ObaSchPullOutList) m
        .readValue(outputJson, ObaSchPullOutList.class);
    assertEquals(1, pulloutList.getPullOuts().getPullOut().size());
    assertEquals("3142", pulloutList.getPullOuts().getPullOut().get(0)
        .getVehicle().getId());

  }

  @Test
  public void testActivePulloutsForInvalidBus() throws Exception {
    setUp(PIPO_JSON_FILE);
    String outputJson = rtResource.getActivePulloutsForBus("1235", null);
    assertEquals("{\"errorCode\":\"2\",\"errorDescription\":\"intentionalBlank: No pullouts found for query.\","
    		+ "\"languages\":null,\"pull-outs\":{\"pull-out\":[]},\"created\":null,\"schVersion\":null,"
    		+ "\"sourceapp\":null,\"sourceip\":null,\"sourceport\":null,\"noNameSpaceSchemaLocation\":null,"
    		+ "\"activation\":null,\"deactivation\":null}", outputJson);
  }

  /*@Test
  public void testActivePulloutsForInvalidOperator() throws Exception {
    setUp("pullin-pullout-rt-tcip.json");

    vehiclePullInOutService = new VehiclePullInOutServiceImpl() {
      @Override
      protected boolean isActive(String pullOuttimeString) {
        // since dates aren't meaningful in a unit test -- return everything!
        return true;
      }
    };

    // replace resource's mock impl with ours from above
    resource.setVehiclePullInOutService(vehiclePullInOutService);
    
     * known issue with 8826 - obanyc-1678 -- pass numbers with a leading
     * non-numeric digits were setting operator-id to -1
     
    String outputJson = rtResource.getActivePulloutsForBus("8826", null);
    assertNotNull(outputJson);
    assertTrue(outputJson.contains(" \"operator-id\": \"36593\""));
  }*/

  @SuppressWarnings("unchecked")
  @Test
  public void testActivePulloutsByDepot() throws Exception {
    setUp(PIPO_JSON_FILE);
    VehiclePullInOutInfo activePullout = setupPullOutData();

    List<VehiclePullInOutInfo> activePullouts = new ArrayList<VehiclePullInOutInfo>();
    activePullouts.add(activePullout);

    when(
        vehiclePullInOutService.getActivePullOuts(isA(List.class),
            isA(Boolean.class))).thenReturn(activePullouts);

    String outputJson = resource.getActivePulloutsForDepot("OS", null);

    // writeToFile(outputJson, "activepulloutsbydepot.txt");

    verifyPulloutData(outputJson);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testActivePulloutsByAgency() throws Exception {
    setUp(PIPO_JSON_FILE);
    VehiclePullInOutInfo activePullout = setupPullOutData();

    List<VehiclePullInOutInfo> activePullouts = new ArrayList<VehiclePullInOutInfo>();
    activePullouts.add(activePullout);

    when(
        vehiclePullInOutService.getActivePullOuts(isA(List.class),
            isA(Boolean.class))).thenReturn(activePullouts);

    String outputJson = resource.getActivePulloutsForAgency("MTA NYCT", null);

    // writeToFile(outputJson, "activepulloutsbyagency.txt");

    verifyPulloutData(outputJson);
  }

  // obanyc-1680, run numbers should be trimmed of leading zeros
  @Test
  public void testCleanupRunNumber() throws Exception {

    setUp(PIPO_JSON_FILE);

    vehiclePullInOutService = new VehiclePullInOutServiceImpl() {
      @Override
      protected boolean isActive(String pullOuttimeString) {
        // since dates aren't meaningful in a unit test -- return everything!
        return true;
      }
    };

    // replace resource's mock impl with ours from above
    resource.setVehiclePullInOutService(vehiclePullInOutService);

    // 3244th row has X109,YUKT,004. We want run=4, not 9, so run-route will be
    // X109-4, not X109-004
    // note that BX31-X01 is correct however
    String json = resource.getActivePulloutsForDepot("YU", null);
    assertTrue(json.contains(" \"run\": \"X109-4\""));
    assertFalse(json.contains(" \"run\": \"X109-004\""));
  }

  /*
   * Private support methods.
   * 
   */
  
  private ObjectMapper setupJaxb() {
    JaxbAnnotationModule module = new JaxbAnnotationModule();
    ObjectMapper m = new ObjectMapper();
    m.registerModule(module);
    return m;
  }

  private VehiclePullInOutInfo setupPullOutData() {
    SCHPullInOutInfo pullOutInfo = new SCHPullInOutInfo();
    SCHPullInOutInfo pullInInfo = new SCHPullInOutInfo();

    CPTVehicleIden vehicle = new CPTVehicleIden();
    vehicle.setVehicleId(1253L);
    vehicle.setAgencyId(2008L);

    CPTTransitFacilityIden garage = new CPTTransitFacilityIden();
    garage.setAgencydesignator("MTA NYCT");
    garage.setFacilityName("OS");

    SCHRunIden run = new SCHRunIden();
    run.setDesignator("SBS15-106");

    CPTOperatorIden operator = new CPTOperatorIden();
    operator.setOperatorId(1663L);

    pullOutInfo.setTime("2012-06-15T09:23:00-04:00");
    pullOutInfo.setPullIn(false);
    pullOutInfo.setVehicle(vehicle);
    pullOutInfo.setGarage(garage);
    pullOutInfo.setDate("2012-06-15");
    pullOutInfo.setRun(run);
    pullOutInfo.setOperator(operator);

    pullInInfo.setTime("2012-06-15T22:57:00-04:00");
    pullInInfo.setPullIn(true);
    pullInInfo.setVehicle(vehicle);
    pullInInfo.setGarage(garage);
    pullInInfo.setDate("2012-06-15");
    pullInInfo.setOperator(operator);

    VehiclePullInOutInfo activePullout = new VehiclePullInOutInfo();
    activePullout.setPullInInfo(pullInInfo);
    activePullout.setPullOutInfo(pullOutInfo);
    return activePullout;
  }

  private void verifyPulloutData(String outputJson) {
    // Check pullout data
    assertTrue(outputJson.contains(" \"vehicle-id\": \"1253\""));
    assertTrue(outputJson.contains("\"agency-id-tcip\": \"2008\""));
    assertTrue(outputJson.contains(" \"agency-id\": \"MTA NYCT\""));
    assertTrue(outputJson.contains("\"depot\": \"OS\""));
    assertTrue(outputJson.contains(" \"service-date\": \"2012-06-15\""));
    assertTrue(outputJson
        .contains("\"pullout-time\": \"2012-06-15T09:23:00-04:00\""));
    assertTrue(outputJson.contains(" \"run\": \"SBS15-106\""));
    assertTrue(outputJson.contains(" \"operator-id\": \"1663\""));
    assertTrue(outputJson
        .contains("\"pullin-time\": \"2012-06-15T22:57:00-04:00\""));
  }

  private void copyInputFiles(String filename) {
    InputStream translationFileIn = null;
    FileOutputStream translationFileOut = null;
    InputStream rtFileIn = null;
    FileOutputStream rtFileOut = null;

    String rtPipoFilename = System.getProperty("java.io.tmpdir")
        + "/RtSchPullinPulloutList_20120612_1502.json";
    System.setProperty("tdm.vehiclepipoUploadDir",
        System.getProperty("java.io.tmpdir"));
    System.setProperty("tdm.rtPipoFilename", rtPipoFilename);

    try {
      rtFileIn = this.getClass().getResourceAsStream(filename);
      translationFileIn = this.getClass().getResourceAsStream("depot_ids.csv");
      rtFileOut = new FileOutputStream(rtPipoFilename);
      translationFileOut = new FileOutputStream(
          System.getProperty("java.io.tmpdir") + "/depot_ids.csv");

      // Copy RT input file to temp directory
      int inputByteCount = 0;
      byte[] inputBuffer = new byte[1024];
      while ((inputByteCount = rtFileIn.read(inputBuffer)) >= 0)
        rtFileOut.write(inputBuffer, 0, inputByteCount);

      // Copy translation file to temp directory
      int translationByteCount = 0;
      byte[] translationBuffer = new byte[1024];
      while ((translationByteCount = translationFileIn.read(translationBuffer)) >= 0)
        translationFileOut.write(translationBuffer, 0, translationByteCount);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (rtFileIn != null) {
          rtFileIn.close();
        }
        if (rtFileOut != null) {
          rtFileOut.close();
        }
        if (translationFileIn != null) {
          translationFileIn.close();
        }
        if (translationFileOut != null) {
          translationFileOut.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @SuppressWarnings("unused")
  private void writeToFile(String outputJson, String fileName) {
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(
          System.getProperty("java.io.tmpdir") + "/" + fileName));
      out.write(outputJson);
      out.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
