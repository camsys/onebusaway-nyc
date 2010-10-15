package org.onebusaway.nyc.vehicle_tracking.impl.inference.sensor_model;

import static org.junit.Assert.assertEquals;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.lsids;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.serviceIds;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.impl.tripplanner.offline.BlockEntryImpl;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.testing.UnitTestingSupport;

public class JourneyInProgressSensorModelImplTest {

  private JourneyInProgressSensorModelImpl _model;

  @Before
  public void before() {
    _model = new JourneyInProgressSensorModelImpl();
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

    VehicleState vehicleState = new VehicleState(edgeState, new MotionState(0,
        null), JourneyState.inProgress(blockState));

    Particle p = new Particle(now);
    p.setData(vehicleState);

    NycVehicleLocationRecord record = new NycVehicleLocationRecord();
    record.setLatitude(47.0);
    record.setTime(now);

    Observation obs = new Observation(record, null);

    DeviationModel g = new DeviationModel(15 * 60);
    double expectedP = g.probability(10 * 60);
    double actualP = _model.computeScheduleDeviationProbability(p, obs);
    assertEquals(expectedP, actualP, 0.0);
  }
}
