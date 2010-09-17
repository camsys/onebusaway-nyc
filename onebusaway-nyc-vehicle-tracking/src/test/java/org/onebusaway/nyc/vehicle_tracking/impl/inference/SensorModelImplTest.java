package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.junit.Before;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.impl.tripplanner.offline.BlockEntryImpl;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.testing.DateSupport;
import org.onebusaway.transit_data_federation.testing.MockEntryFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SensorModelImplTest {

  private SensorModelImpl _model;

  @Before
  public void before() {
    _model = new SensorModelImpl();
  }

  @Autowired
  public void test() {
    long serviceDate = DateSupport.time("2010-09-16 00:00");
    long t = DateSupport.time("2010-09-16 10:00");

    
    EdgeState edgeState = new EdgeState(null);

    BlockEntryImpl blockEntry = MockEntryFactory.block("blockA");
    BlockInstance blockInstance = new BlockInstance(blockEntry, serviceDate);
    ScheduledBlockLocation blockLocation = new ScheduledBlockLocation();

    BlockState blockState = new BlockState(blockInstance, blockLocation, "1234");

    VehicleState vehicleState = new VehicleState(edgeState, blockState);

    Particle p = new Particle(t);
    p.setData(vehicleState);
    
    NycVehicleLocationRecord record = new NycVehicleLocationRecord();
    record.setLatitude(47.0);
    
    Observation obs = new Observation(record, null);

    double v = _model.computeScheduleDeviationProbability(p, obs);
  }
}
