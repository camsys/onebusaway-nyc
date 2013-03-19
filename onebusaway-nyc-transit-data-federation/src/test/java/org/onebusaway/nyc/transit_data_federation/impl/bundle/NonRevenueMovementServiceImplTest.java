package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTripLoader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.StifTripType;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.TestStifRecordReaderTest;
import org.onebusaway.nyc.transit_data_federation.impl.nyc.NonRevenueMovementServiceImpl;
import org.onebusaway.nyc.transit_data_federation.model.nyc.NonRevenueMoveData;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleStoreService;

@RunWith(MockitoJUnitRunner.class)
public class NonRevenueMovementServiceImplTest extends NonRevenueMovementServiceImpl {
  
  @Mock
  private BundleStoreService mockBundleStoreService;

  private ServiceDate now = new ServiceDate(new Date());
  
  @Before
  public void setup() {
	    InputStream in = TestStifRecordReaderTest.class.getResourceAsStream("stif.m_0014__.210186.sun");
	    String gtfs = TestStifRecordReaderTest.class.getResource("m14.zip").getFile();

    	GtfsReader reader = new GtfsReader();
    	GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
	    try {
	    	reader.setEntityStore(dao);
	    	reader.setInputLocation(new File(gtfs));
	    	reader.run();
	    } catch(Exception e) {
	    	// discard
	    }
	    
	    StifTripLoader loader = new StifTripLoader();
	    loader.setLogger(new MultiCSVLogger());
	    loader.setGtfsDao(dao);
	    loader.run(in, new File("stif.m_0014__.210186.sun"));

	    this._nonRevenueMoveLocationsByBoxId = loader.getGeographyRecordsByBoxId();
	    this._nonRevenueMovesByServiceCode = loader.getRawStifData();
	    buildIndices();
  }
  
  @Test
  public void testPullOutNonRevMove() {
	//  2149ec00112000  2 2ec200113500        73M14AD_0072   M14AD                        A     Y N                1600 MQ   11559269     73  M14AD 00113500    0                              
	//  3149ec00112000D NT   E     0
	//  312ec200113500A NT   E  1600
	long stifTimeValueSecondsPastMidnightStart = ((112000 * 60) / 100) * 1000;
	long stifTimeValueSecondsPastMidnightEnd = ((113500 * 60) / 100) * 1000;

	NonRevenueMoveData nrmd = this.findNonRevenueMovementsForBlockAndTime(new AgencyAndId("MTA NYCT", "11559269"), 
			now.getAsDate().getTime(), now.getAsDate().getTime() + stifTimeValueSecondsPastMidnightStart);
	assertTrue(nrmd != null);
	assertTrue(nrmd.getMoveType() == StifTripType.PULLOUT);

	nrmd = this.findNonRevenueMovementsForBlockAndTime(new AgencyAndId("MTA NYCT", "11559269"), 
			now.getAsDate().getTime(), now.getAsDate().getTime() + stifTimeValueSecondsPastMidnightEnd);
	assertTrue(nrmd != null);
	assertTrue(nrmd.getMoveType() == StifTripType.PULLOUT);
  }

  @Test
  public void testPullInNonRevMove() {
	//  21551200151600W 3 49ec00153100        69M14AD_0084   M14AD                        A     N C                1600 MQ   11559269                                        69  M14AD 00149300
	//  31551200151600D NT   E     0
	//  3149ec00153100A NT   E  1600
	long stifTimeValueSecondsPastMidnightStart = ((151600 * 60) / 100) * 1000;
	long stifTimeValueSecondsPastMidnightEnd = ((153100 * 60) / 100) * 1000;
	
	NonRevenueMoveData nrmd = this.findNonRevenueMovementsForBlockAndTime(new AgencyAndId("MTA NYCT", "11559269"), 
			now.getAsDate().getTime(), now.getAsDate().getTime() + stifTimeValueSecondsPastMidnightStart);
	assertTrue(nrmd != null);
	assertTrue(nrmd.getMoveType() == StifTripType.PULLIN);

	nrmd = this.findNonRevenueMovementsForBlockAndTime(new AgencyAndId("MTA NYCT", "11559269"), 
			now.getAsDate().getTime(), now.getAsDate().getTime() + stifTimeValueSecondsPastMidnightEnd);
	assertTrue(nrmd != null);
	assertTrue(nrmd.getMoveType() == StifTripType.PULLIN);
  }

  @Test
  public void testRevMove() {
	//  212ec200113500E 1 3c7100117400        73M14AD_0001   M14AD                        A0140 N N                3640 MQ   11559269     73  M14AD 00118700   13  M14AD     73  M14AD 00112000
	//  312ec200113500D ST   E     0
	//  3145ae00113763T SN   E   262
	//  312ea900113900T ST   E   400
	//  3129f100114100T SN   E   570
	//  31294700114300T ST   E   740
	//  3129f200114623T SN   E   910
	//  3129f300114983T SN   E  1100
	//  312e9f00115191T SN   E  1210
	//  3137e100115400T ST   E  1320
	//  31290300115568T SN   E  1480
	//  31290400115705T SN   E  1610
	//  31290500115863T SN   E  1760
	//  31290600116000T ST   E  1890
	//  31290700116185T SN   E  2030
	//  3129f400116368T SN   E  2170
	//  317e8c00116500T ST   E  2270
	//  3129f600116686T SN   E  2520
	//  3129f900116761T SN   E  2620
	//  3129fa00116836T SN   E  2720
	//  3129fb00116910T SN   E  2820
	//  312d9900117000T ST   E  2940
	//  3129fc00117051T SN   E  3030
	//  3129fd00117108T SN   E  3130
	//  3133f100117160T SN   E  3220
	//  312d2800117275T SN   E  3420
	//  314e9200117355T SN   E  3560
	//  313c7100117400A ST   E  3640
	long stifTimeValueSecondsPastMidnightT1 = ((115705 * 60) / 100) * 1000;
	long stifTimeValueSecondsPastMidnightT2 = ((116836 * 60) / 100) * 1000;
	
	NonRevenueMoveData nrmd = this.findNonRevenueMovementsForBlockAndTime(new AgencyAndId("MTA NYCT", "11559269"), 
			now.getAsDate().getTime(), now.getAsDate().getTime() + stifTimeValueSecondsPastMidnightT1);
	assertTrue(nrmd == null);

	nrmd = this.findNonRevenueMovementsForBlockAndTime(new AgencyAndId("MTA NYCT", "11559269"), 
			now.getAsDate().getTime(), now.getAsDate().getTime() + stifTimeValueSecondsPastMidnightT2);
	assertTrue(nrmd == null);
  }

}
