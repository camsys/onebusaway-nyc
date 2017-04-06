package org.onebusaway.nyc.gtfsrt.tests;

public class SampleTripUpdateTest extends TripUpdateTest {

  //  public TripUpdateTest(String gtfsFile, String defaultAgencyId, String blockTripMapFile, String inferenceFile, String pbFile) {

  public SampleTripUpdateTest() {
    super("google_transit_manhattan.zip", "MTA", "6700_M104_2017-04-06/btmap.tsv", "6700_M104_2017-04-06/vlrb.tsv", "6700_M104_2017-04-06/tripupdate.pb");
  }
}
