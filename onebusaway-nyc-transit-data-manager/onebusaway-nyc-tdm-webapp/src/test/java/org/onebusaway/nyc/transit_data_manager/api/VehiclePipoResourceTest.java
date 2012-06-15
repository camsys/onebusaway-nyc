package org.onebusaway.nyc.transit_data_manager.api;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.transit_data_manager.adapters.input.MtaUtsToTcipVehicleAssignmentConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPVehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.VehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;
import org.onebusaway.nyc.transit_data_manager.api.service.VehiclePullInOutService;
import org.onebusaway.nyc.transit_data_manager.api.service.VehiclePullInOutServiceImpl;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.onebusaway.nyc.transit_data_manager.json.LowerCaseWDashesGsonJsonTool;

import com.mysql.jdbc.ConnectionPropertiesTransform;

import tcip_final_3_0_5_1.CPTTransitFacilityIden;
import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.SCHPullInOutInfo;


public class VehiclePipoResourceTest {

	private VehiclePipoResource resource;
	private VehicleAssignmentsOutputConverter converter;
	private JsonTool jsonTool;
	private MtaUtsToTcipVehicleAssignmentConverter dataConverter;
	@Mock
	private VehiclePullInOutService vehiclePullInOutService;
	
	@Before
	public void setUp() throws Exception {
		copyInputFiles();
		MockitoAnnotations.initMocks(this);
		
		System.setProperty("tdm.vehiclepipoUploadDir", System.getProperty("java.io.tmpdir"));
		System.setProperty("tdm.depotIdTranslationFile", System.getProperty("java.io.tmpdir") + "/depot_ids.csv");
		
		resource = new VehiclePipoResource();
		jsonTool = new LowerCaseWDashesGsonJsonTool(true);
		
		converter = new TCIPVehicleAssignmentsOutputConverter();
		dataConverter = new MtaUtsToTcipVehicleAssignmentConverter();
		dataConverter.setMappingTool(new UtsMappingTool());
		
		((TCIPVehicleAssignmentsOutputConverter)converter).setDataConverter(dataConverter);
		
		resource.setJsonTool(jsonTool);
		resource.setConverter(converter);
		resource.setVehiclePullInOutService(vehiclePullInOutService);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAllActivePullouts() {
		SCHPullInOutInfo pullOutInfo1 = new SCHPullInOutInfo();
		SCHPullInOutInfo pullInInfo1 = new SCHPullInOutInfo();
		
		pullOutInfo1.setTime("2012-06-15T09:23:00-04:00");
		pullOutInfo1.setPullIn(false);
		
		pullInInfo1.setTime("2012-06-15T22:57:00-04:00");
		pullInInfo1.setPullIn(true);
		
		SCHPullInOutInfo pullOutInfo2 = new SCHPullInOutInfo();
		SCHPullInOutInfo pullInInfo2 = new SCHPullInOutInfo();
		
		pullOutInfo2.setTime("2012-06-15T07:23:00-04:00");
		pullOutInfo2.setPullIn(false);
		
		pullInInfo2.setTime("2012-06-15T22:57:00-04:00");
		pullInInfo2.setPullIn(true);
		
		VehiclePullInOutInfo activePullout1 = new VehiclePullInOutInfo();
		activePullout1.setPullInInfo(pullInInfo1);
		activePullout1.setPullOutInfo(pullOutInfo1);
		
		VehiclePullInOutInfo activePullout2 = new VehiclePullInOutInfo();
		activePullout2.setPullInInfo(pullInInfo2);
		activePullout2.setPullOutInfo(pullOutInfo2);
		
		List<VehiclePullInOutInfo> activePullouts= new ArrayList<VehiclePullInOutInfo>();
		activePullouts.add(activePullout1);
		activePullouts.add(activePullout2);
		
		when(vehiclePullInOutService.getActivePullOuts(isA(List.class))).thenReturn(activePullouts);
		
		String outputJson = resource.getAllActivePullouts();
		
		//writeToFile(outputJson, "activepullouts.txt");
		
		//Analyze output better once its format is finalized
		assertTrue(outputJson.contains(" \"pull-in\": false"));
		assertTrue(outputJson.contains( "\"time\": \"2012-06-15T09:23:00-04:00\""));
		assertTrue(outputJson.contains(" \"pull-in\": true"));
		assertTrue(outputJson.contains( "\"time\": \"2012-06-15T22:57:00-04:00\""));
		assertTrue(outputJson.contains(" \"pull-in\": false"));
		assertTrue(outputJson.contains( "\"time\": \"2012-06-15T07:23:00-04:00\""));
		assertTrue(outputJson.contains(" \"pull-in\": true"));
		assertTrue(outputJson.contains( "\"time\": \"2012-06-15T22:57:00-04:00\""));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testActivePulloutsByBus() {
		SCHPullInOutInfo pullOutInfo = new SCHPullInOutInfo();
		SCHPullInOutInfo pullInInfo = new SCHPullInOutInfo();
		
		CPTVehicleIden vehicle = new CPTVehicleIden();
		vehicle.setVehicleId(1235L);
		
		pullOutInfo.setTime("2012-06-15T09:23:00-04:00");
		pullOutInfo.setPullIn(false);
		pullOutInfo.setVehicle(vehicle);
		
		pullInInfo.setTime("2012-06-15T22:57:00-04:00");
		pullInInfo.setPullIn(true);
		pullInInfo.setVehicle(vehicle);
		
		VehiclePullInOutInfo activePullout = new VehiclePullInOutInfo();
		activePullout.setPullInInfo(pullInInfo);
		activePullout.setPullOutInfo(pullOutInfo);
		
		List<VehiclePullInOutInfo> activePullouts= new ArrayList<VehiclePullInOutInfo>();
		activePullouts.add(activePullout);
		
		when(vehiclePullInOutService.getActivePullOuts(isA(List.class))).thenReturn(activePullouts);
		when(vehiclePullInOutService.getMostRecentActivePullout(isA(List.class))).thenReturn(activePullout);
		
		String outputJson = resource.getCurrentlyActivePulloutsForBus("1235");
		
		//writeToFile(outputJson, "activepulloutsbybus.txt");
		
		//Analyze output better once its format is finalized
		assertTrue(outputJson.contains(" \"pull-in\": false"));
		assertTrue(outputJson.contains( "\"time\": \"2012-06-15T09:23:00-04:00\""));
		assertTrue(outputJson.contains(" \"pull-in\": true"));
		assertTrue(outputJson.contains( "\"time\": \"2012-06-15T22:57:00-04:00\""));
		assertTrue(outputJson.contains("\"vehicle-id\": 1235"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testActivePulloutsByDepot() {
		SCHPullInOutInfo pullOutInfo = new SCHPullInOutInfo();
		SCHPullInOutInfo pullInInfo = new SCHPullInOutInfo();
		
		CPTVehicleIden vehicle = new CPTVehicleIden();
		vehicle.setVehicleId(1235L);
		
		CPTTransitFacilityIden garage = new CPTTransitFacilityIden();
		garage.setFacilityName("OS");
		
		pullOutInfo.setTime("2012-06-15T09:23:00-04:00");
		pullOutInfo.setPullIn(false);
		pullOutInfo.setVehicle(vehicle);
		pullOutInfo.setGarage(garage);
		
		pullInInfo.setTime("2012-06-15T22:57:00-04:00");
		pullInInfo.setPullIn(true);
		pullInInfo.setVehicle(vehicle);
		
		VehiclePullInOutInfo activePullout = new VehiclePullInOutInfo();
		activePullout.setPullInInfo(pullInInfo);
		activePullout.setPullOutInfo(pullOutInfo);
		
		List<VehiclePullInOutInfo> activePullouts= new ArrayList<VehiclePullInOutInfo>();
		activePullouts.add(activePullout);
		
		when(vehiclePullInOutService.getActivePullOuts(isA(List.class))).thenReturn(activePullouts);
		when(vehiclePullInOutService.getMostRecentActivePullout(isA(List.class))).thenReturn(activePullout);
		
		String outputJson = resource.getCurrentlyActivePulloutsForDepot("OS");
		
		//writeToFile(outputJson, "activepulloutsbydepot.txt");
		
		//Analyze output better once its format is finalized
		assertTrue(outputJson.contains(" \"pull-in\": false"));
		assertTrue(outputJson.contains( "\"time\": \"2012-06-15T09:23:00-04:00\""));
		assertTrue(outputJson.contains(" \"pull-in\": true"));
		assertTrue(outputJson.contains( "\"time\": \"2012-06-15T22:57:00-04:00\""));
		assertTrue(outputJson.contains("\"vehicle-id\": 1235"));
		assertTrue(outputJson.contains("\"facility-name\": \"OS\""));
	}
	
	private void copyInputFiles() {
		InputStream inputFileIn = null;
		InputStream translationFileIn = null;
		FileOutputStream inputFileOut = null;
		FileOutputStream translationFileOut = null;
		
		try {
			inputFileIn = this.getClass().getResourceAsStream("UTSPUPUFULL_20120612_1502.txt");
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
