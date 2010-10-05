package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import static org.junit.Assert.assertEquals;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.*;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.impl.tripplanner.offline.BlockEntryImpl;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.testing.UnitTestingSupport;

public class SensorModelImplTest {

  private SensorModelImpl _model;

  @Before
  public void before() {
    _model = new SensorModelImpl();
  }

  @Test
  public void testComputeScheduleDeviationPropability() {

    long serviceDate = UnitTestingSupport.dateAsLong("2010-09-16 00:00");
    long now = UnitTestingSupport.dateAsLong("2010-09-16 10:00");

    EdgeState edgeState = new EdgeState(null, null, 0.0);

    BlockEntryImpl block = UnitTestingSupport.block("blockA");
    BlockConfigurationEntry blockConfig = UnitTestingSupport.blockConfiguration(
        block, serviceIds(lsids("sA"), lsids()));
    BlockInstance blockInstance = new BlockInstance(blockConfig, serviceDate);

    ScheduledBlockLocation blockLocation = new ScheduledBlockLocation();
    blockLocation.setScheduledTime(UnitTestingSupport.time(9, 50));
    BlockState blockState = new BlockState(blockInstance, blockLocation, "1234");

    VehicleState vehicleState = new VehicleState(edgeState, blockState);

    Particle p = new Particle(now);
    p.setData(vehicleState);

    NycVehicleLocationRecord record = new NycVehicleLocationRecord();
    record.setLatitude(47.0);
    record.setTime(now);

    Observation obs = new Observation(record, null);

    Gaussian g = new Gaussian(0, 15 * 60);
    double expectedP = g.getProbability(10 * 60);
    double actualP = _model.computeScheduleDeviationProbability(p, obs);
    assertEquals(expectedP, actualP, 0.0);
  }
}
