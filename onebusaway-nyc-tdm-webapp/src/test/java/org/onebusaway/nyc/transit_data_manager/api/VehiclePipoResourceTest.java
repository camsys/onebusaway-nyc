package org.onebusaway.nyc.transit_data_manager.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import org.onebusaway.nyc.transit_data_manager.api.sourceData.VehiclePipoUploadsFilePicker;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutDataProviderServiceImpl;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutService;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutServiceImpl;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.onebusaway.nyc.transit_data_manager.json.LowerCaseWDashesGsonJsonTool;

import tcip_final_4_0_0.CPTOperatorIden;
import tcip_final_4_0_0.CPTTransitFacilityIden;
import tcip_final_4_0_0.CPTVehicleIden;
import tcip_final_4_0_0.SCHPullInOutInfo;
import tcip_final_4_0_0.SCHRunIden;


public class VehiclePipoResourceTest {

  
	private VehiclePipoResource resource;
	private VehicleAssignmentsOutputConverter converter;
	private JsonTool jsonTool;
	private MtaUtsToTcipVehicleAssignmentConverter dataConverter;
	private ModelCounterpartConverter<VehiclePullInOutInfo, PullInOut> pulloutDataConverter;
	private VehiclePullInOutDataProviderServiceImpl vehiclePullInOutDataProviderService;
	private VehiclePipoUploadsFilePicker vehicleFilePicker;
	
	@Mock
	private VehiclePullInOutService vehiclePullInOutService;
	
	public void setUp(String filename) throws Exception {
		copyInputFiles(filename);
		MockitoAnnotations.initMocks(this);
		
		System.setProperty("tdm.vehiclepipoUploadDir", System.getProperty("java.io.tmpdir"));
		System.setProperty("tdm.depotIdTranslationFile", System.getProperty("java.io.tmpdir") + "/depot_ids.csv");
		
		resource = new VehiclePipoResource();
		jsonTool = new LowerCaseWDashesGsonJsonTool(true);
		
		UtsMappingTool mappingTool = new UtsMappingTool();
		
		converter = new TCIPVehicleAssignmentsOutputConverter();
		dataConverter = new MtaUtsToTcipVehicleAssignmentConverter();
		dataConverter.setMappingTool(mappingTool);
		
		((TCIPVehicleAssignmentsOutputConverter)converter).setDataConverter(dataConverter);
		
		VehicleFromTcip vehicleConverter = new VehicleFromTcip();
		vehicleConverter.setMappingTool(mappingTool);
		
		pulloutDataConverter = new PullInOutFromTcip();
		((PullInOutFromTcip)pulloutDataConverter).setVehConv(vehicleConverter);
		
		vehicleFilePicker = new VehiclePipoUploadsFilePicker("tdm.vehiclepipoUploadDir");
		
		vehiclePullInOutDataProviderService = new VehiclePullInOutDataProviderServiceImpl();
		vehiclePullInOutDataProviderService.setConverter(converter);
		vehiclePullInOutDataProviderService.setMostRecentFilePicker(vehicleFilePicker);
		vehiclePullInOutDataProviderService.setPulloutDataConverter(pulloutDataConverter);
		
		resource.setJsonTool(jsonTool);
		resource.setVehiclePullInOutService(vehiclePullInOutService);
		resource.setVehiclePullInOutDataProviderService(vehiclePullInOutDataProviderService);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAllActivePullouts() throws Exception {
	    setUp("UTSPUPUFULL_20120612_1502.txt");
		SCHPullInOutInfo pullOutInfo1 = new SCHPullInOutInfo();
		SCHPullInOutInfo pullInInfo1 = new SCHPullInOutInfo();
		
		CPTVehicleIden vehicle1 = new CPTVehicleIden();
		vehicle1.setVehicleId(1240L);
		vehicle1.setAgencyId(2008L);
		
		CPTTransitFacilityIden garage1 = new CPTTransitFacilityIden();
		garage1.setAgencydesignator("MTA NYCT");
		garage1.setFacilityName("OS");
		
		SCHRunIden run1 = new SCHRunIden();
		run1.setDesignator("SBS15-106");
		
		CPTOperatorIden operator1 = new CPTOperatorIden();
		operator1.setOperatorId(1663L);
		
		pullOutInfo1.setTime("2012-06-15T09:23:00-04:00");
		pullOutInfo1.setPullIn(false);
		pullOutInfo1.setVehicle(vehicle1);
		pullOutInfo1.setGarage(garage1);
		pullOutInfo1.setDate("2012-06-15");
		pullOutInfo1.setRun(run1);
		pullOutInfo1.setOperator(operator1);
		
		pullInInfo1.setTime("2012-06-15T22:57:00-04:00");
		pullInInfo1.setPullIn(true);
		pullInInfo1.setVehicle(vehicle1);
		pullInInfo1.setGarage(garage1);
		pullInInfo1.setDate("2012-06-15");
		pullInInfo1.setOperator(operator1);
		
		SCHPullInOutInfo pullOutInfo2 = new SCHPullInOutInfo();
		SCHPullInOutInfo pullInInfo2 = new SCHPullInOutInfo();
		
		CPTVehicleIden vehicle2 = new CPTVehicleIden();
		vehicle2.setVehicleId(1230L);
		vehicle2.setAgencyId(2008L);
		
		CPTTransitFacilityIden garage2 = new CPTTransitFacilityIden();
		garage2.setAgencydesignator("MTA NYCT");
		garage2.setFacilityName("OS");
		
		SCHRunIden run2 = new SCHRunIden();
		run2.setDesignator("SBS15-126");
		
		CPTOperatorIden operator2 = new CPTOperatorIden();
		operator2.setOperatorId(11052L);
		
		pullOutInfo2.setTime("2012-06-15T07:23:00-04:00");
		pullOutInfo2.setPullIn(false);
		pullOutInfo2.setVehicle(vehicle2);
		pullOutInfo2.setGarage(garage2);
		pullOutInfo2.setDate("2012-06-15");
		pullOutInfo2.setRun(run2);
		pullOutInfo2.setOperator(operator2);
		
		pullInInfo2.setTime("2012-06-15T22:57:00-04:00");
		pullInInfo2.setPullIn(true);
		pullInInfo2.setVehicle(vehicle2);
		pullInInfo2.setGarage(garage2);
		pullInInfo2.setDate("2012-06-15");
		pullInInfo2.setOperator(operator2);
		
		VehiclePullInOutInfo activePullout1 = new VehiclePullInOutInfo();
		activePullout1.setPullInInfo(pullInInfo1);
		activePullout1.setPullOutInfo(pullOutInfo1);
		
		VehiclePullInOutInfo activePullout2 = new VehiclePullInOutInfo();
		activePullout2.setPullInInfo(pullInInfo2);
		activePullout2.setPullOutInfo(pullOutInfo2);
		
		List<VehiclePullInOutInfo> activePullouts= new ArrayList<VehiclePullInOutInfo>();
		activePullouts.add(activePullout1);
		activePullouts.add(activePullout2);
		
		when(vehiclePullInOutService.getActivePullOuts(isA(List.class), isA(Boolean.class))).thenReturn(activePullouts);
		
		String outputJson = resource.getAllActivePullouts(null);
		
		//writeToFile(outputJson, "activepullouts.txt");
		
		//Check first pullout data
		assertTrue(outputJson.contains(" \"vehicle-id\": \"1240\""));
		assertTrue(outputJson.contains( "\"agency-id-tcip\": \"2008\""));
		assertTrue(outputJson.contains(" \"agency-id\": \"MTA NYCT\""));
		assertTrue(outputJson.contains( "\"depot\": \"OS\""));
		assertTrue(outputJson.contains(" \"service-date\": \"2012-06-15\""));
		assertTrue(outputJson.contains( "\"pullout-time\": \"2012-06-15T09:23:00-04:00\""));
		assertTrue(outputJson.contains(" \"run\": \"SBS15-106\""));
		assertTrue(outputJson.contains(" \"operator-id\": \"1663\""));
		assertTrue(outputJson.contains( "\"pullin-time\": \"2012-06-15T22:57:00-04:00\""));
		
		//Check second pullout data
		assertTrue(outputJson.contains(" \"vehicle-id\": \"1230\""));
		assertTrue(outputJson.contains( "\"agency-id-tcip\": \"2008\""));
		assertTrue(outputJson.contains(" \"agency-id\": \"MTA NYCT\""));
		assertTrue(outputJson.contains( "\"depot\": \"OS\""));
		assertTrue(outputJson.contains(" \"service-date\": \"2012-06-15\""));
		assertTrue(outputJson.contains( "\"pullout-time\": \"2012-06-15T07:23:00-04:00\""));
		assertTrue(outputJson.contains(" \"run\": \"SBS15-126\""));
		assertTrue(outputJson.contains(" \"operator-id\": \"11052\""));
		assertTrue(outputJson.contains( "\"pullin-time\": \"2012-06-15T22:57:00-04:00\""));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testActivePulloutsByBus() throws Exception {
	  setUp("UTSPUPUFULL_20120612_1502.txt");
		VehiclePullInOutInfo activePullout = setupPullOutData();
		
		List<VehiclePullInOutInfo> activePullouts= new ArrayList<VehiclePullInOutInfo>();
		activePullouts.add(activePullout);
		
		when(vehiclePullInOutService.getActivePullOuts(isA(List.class), isA(Boolean.class))).thenReturn(activePullouts);
		
		String outputJson = resource.getActivePulloutsForBus("1253", null);
		
		//writeToFile(outputJson, "activepulloutsbybus.txt");
		
		verifyPulloutData(outputJson);
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
		//Check pullout data
		assertTrue(outputJson.contains(" \"vehicle-id\": \"1253\""));
		assertTrue(outputJson.contains( "\"agency-id-tcip\": \"2008\""));
		assertTrue(outputJson.contains(" \"agency-id\": \"MTA NYCT\""));
		assertTrue(outputJson.contains( "\"depot\": \"OS\""));
		assertTrue(outputJson.contains(" \"service-date\": \"2012-06-15\""));
		assertTrue(outputJson.contains( "\"pullout-time\": \"2012-06-15T09:23:00-04:00\""));
		assertTrue(outputJson.contains(" \"run\": \"SBS15-106\""));
		assertTrue(outputJson.contains(" \"operator-id\": \"1663\""));
		assertTrue(outputJson.contains( "\"pullin-time\": \"2012-06-15T22:57:00-04:00\"")); 
	}
	
	@Test
	public void testActivePulloutsForInvalidBus() throws Exception {
	  setUp("UTSPUPUFULL_20120612_1502.txt");
		String outputJson = resource.getActivePulloutsForBus("1235", null);
		assertEquals("No pullouts found for bus : 1235", outputJson);
	}

  @Test
	public void testActivePulloutsForInvalidOperator() throws Exception {
	  setUp("UTSPUPUFULL_20120816_1202.txt");
	  
	  vehiclePullInOutService = new VehiclePullInOutServiceImpl() {
	    @Override
	    protected boolean isActive(String pullOuttimeString) {
	      // since dates aren't meaningful in a unit test -- return everything!
	      return true;
	    }
	  };
	  
	  // replace resource's mock impl with ours from above 
	  resource.setVehiclePullInOutService(vehiclePullInOutService);
	  /*
	   * known issue with 8826 - obanyc-1678 -- pass numbers with a leading non-numeric
	   * digits were setting operator-id to -1
	   */
		String outputJson = resource.getActivePulloutsForBus("8826", null);
		assertNotNull(outputJson);
		assertTrue(outputJson.contains(" \"operator-id\": \"36593\""));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testActivePulloutsByDepot() throws Exception {
	  setUp("UTSPUPUFULL_20120612_1502.txt");
		VehiclePullInOutInfo activePullout = setupPullOutData();
		
		List<VehiclePullInOutInfo> activePullouts= new ArrayList<VehiclePullInOutInfo>();
		activePullouts.add(activePullout);
		
		when(vehiclePullInOutService.getActivePullOuts(isA(List.class), isA(Boolean.class))).thenReturn(activePullouts);
		
		String outputJson = resource.getActivePulloutsForDepot("OS", null);
		
		//writeToFile(outputJson, "activepulloutsbydepot.txt");
		
		verifyPulloutData(outputJson);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testActivePulloutsByAgency() throws Exception {
	  setUp("UTSPUPUFULL_20120612_1502.txt");
		VehiclePullInOutInfo activePullout = setupPullOutData();
		
		List<VehiclePullInOutInfo> activePullouts= new ArrayList<VehiclePullInOutInfo>();
		activePullouts.add(activePullout);
		
		when(vehiclePullInOutService.getActivePullOuts(isA(List.class), isA(Boolean.class))).thenReturn(activePullouts);
		
		String outputJson = resource.getActivePulloutsForAgency("MTA NYCT", null);
		
		//writeToFile(outputJson, "activepulloutsbyagency.txt");
		
		verifyPulloutData(outputJson);
	}
	
	// obanyc-1680, run numbers should be trimmed of leading zeros
	@Test
	public void testCleanupRunNumber() throws Exception {

	  setUp("UTSPUPUFULL_20120816_1202.txt");
		
			  
	  vehiclePullInOutService = new VehiclePullInOutServiceImpl() {
	    @Override
	    protected boolean isActive(String pullOuttimeString) {
	      // since dates aren't meaningful in a unit test -- return everything!
	      return true;
	    }
	  };
	  
	  // replace resource's mock impl with ours from above 
	  resource.setVehiclePullInOutService(vehiclePullInOutService);

		//3244th row has X109,YUKT,004.  We want run=4, not 9, so run-route will be X109-4, not X109-004
		// note that BX31-X01 is correct however
		String json = resource.getActivePulloutsForDepot("YU", null);
    assertTrue(json.contains(" \"run\": \"X109-4\""));
    assertFalse(json.contains(" \"run\": \"X109-004\""));
	}
	
	private void copyInputFiles(String filename) {
		InputStream inputFileIn = null;
		InputStream translationFileIn = null;
		FileOutputStream inputFileOut = null;
		FileOutputStream translationFileOut = null;
		
		try {
			inputFileIn = this.getClass().getResourceAsStream(filename);
			translationFileIn = this.getClass().getResourceAsStream("depot_ids.csv");
			inputFileOut = new FileOutputStream(System.getProperty("java.io.tmpdir") + "/UTSPUPUFULL_20120612_1502.txt");
			translationFileOut = new FileOutputStream(System.getProperty("java.io.tmpdir") + "/depot_ids.csv");
			
			//Copy input file to temp directory
			int inputByteCount = 0;
			byte[] inputBuffer = new byte[1024];
			while ((inputByteCount = inputFileIn.read(inputBuffer)) >= 0)
				inputFileOut.write(inputBuffer, 0, inputByteCount);
			
			//Copy translation file to temp directory
			int translationByteCount = 0;
			byte[] translationBuffer = new byte[1024];
			while ((translationByteCount = translationFileIn.read(translationBuffer)) >= 0)
				translationFileOut.write(translationBuffer, 0, translationByteCount);
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(inputFileIn !=null) {
					inputFileIn.close();
				}
				if(inputFileOut !=null) {
					inputFileOut.close();
				}
				if(translationFileIn !=null) {
					translationFileIn.close();
				}
				if(translationFileOut !=null) {
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
			BufferedWriter out = new BufferedWriter(new FileWriter(System.getProperty("java.io.tmpdir") + "/" +fileName));
			out.write(outputJson);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
