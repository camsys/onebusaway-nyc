package org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeEntry;

public class MotionModelLibrary {

  public static boolean isAtPotentialLayoverSpot(ScheduledBlockLocation location) {

    int time = location.getScheduledTime();

    /**
     * We could be at a layover at a stop itself
     */
    BlockStopTimeEntry closestStop = location.getClosestStop();
    StopTimeEntry closestStopTime = closestStop.getStopTime();

    if (closestStopTime.getAccumulatedSlackTime() > 60
        && closestStopTime.getArrivalTime() <= time
        && time <= closestStopTime.getDepartureTime())
      return true;

    BlockStopTimeEntry nextStop = location.getNextStop();

    // If the next stop is null, it means we're at the end of the block. Do we
    // consider this a layover spot? My sources say no.
    if (nextStop == null)
      return false;

    // Is the next stop the first stop on the block? Then we're potentially at a
    // layover before the route starts
    if (nextStop.getBlockSequence() == 0)
      return true;

    BlockTripEntry nextTrip = nextStop.getTrip();
    BlockConfigurationEntry blockConfig = nextTrip.getBlockConfiguration();
    BlockStopTimeEntry previousStop = blockConfig.getStopTimes().get(
        nextStop.getBlockSequence() - 1);
    BlockTripEntry previousTrip = previousStop.getTrip();

    return !previousTrip.equals(nextTrip);
  }

  public static boolean hasDestinationSignCodeChangedBetweenObservations(
      Observation obs) {

    String previouslyObservedDsc = null;
    NycVehicleLocationRecord previousRecord = obs.getPreviousRecord();
    if (previousRecord != null)
      previouslyObservedDsc = previousRecord.getDestinationSignCode();

    NycVehicleLocationRecord record = obs.getRecord();
    String observedDsc = record.getDestinationSignCode();

    return previouslyObservedDsc == null
        || !previouslyObservedDsc.equals(observedDsc);
  }

  public static boolean isAtEndOfBlock(BlockState blockState) {
    BlockInstance blockInstance = blockState.getBlockInstance();
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    // We give a little fudge factor
    return blockLocation.getDistanceAlongBlock() >= blockConfig.getTotalBlockDistance() - 50;
  }
}
