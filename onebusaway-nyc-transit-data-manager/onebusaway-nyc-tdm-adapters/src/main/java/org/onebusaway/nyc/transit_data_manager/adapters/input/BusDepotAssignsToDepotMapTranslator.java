package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;

public class BusDepotAssignsToDepotMapTranslator
    implements
    GroupByPropInListObjectTranslator<List<MtaBusDepotAssignment>, Map<String, List<MtaBusDepotAssignment>>> {

  public Map<String, List<MtaBusDepotAssignment>> restructure(
      List<MtaBusDepotAssignment> listInput) {

    Map<String, List<MtaBusDepotAssignment>> resultMap = new HashMap<String, List<MtaBusDepotAssignment>>();

    // Will do this with a loop.
    // loop through all of the elements in listInput.
    // For each element, check the depot field of the element against all the
    // existing keys in the map.
    // Can probably check for preexisting depots in a couple ways, including
    // using resultMap.get(depotVal)
    // If the key is not found, add a new row with the depot as the value.

    // Use an iterator to loop through all elements in listInput.
    Iterator<MtaBusDepotAssignment> bdIt = listInput.iterator();
    MtaBusDepotAssignment bdAssign = null;
    List<MtaBusDepotAssignment> depotBusAssigns = null;

    while (bdIt.hasNext()) {
      // for each element
      bdAssign = bdIt.next();

      // check the depot field of the element against all the existing keys in
      // the map.
      depotBusAssigns = resultMap.get(bdAssign.getDepot()); // Grab the value
                                                            // for the depot key
                                                            // from the
                                                            // resultMap.
      if (depotBusAssigns == null) { // The depot key does not yet exist in the
                                     // resultMap.
        depotBusAssigns = new ArrayList<MtaBusDepotAssignment>(); // create a
                                                                  // new list
                                                                  // for this
                                                                  // particular
                                                                  // depot.
        depotBusAssigns.add(bdAssign); // add the bus assignment to it.
        resultMap.put(bdAssign.getDepot(), depotBusAssigns); // and stick it
                                                             // into the
                                                             // resultMap.
      } else { // The depot key already exists in the resultMap.
        depotBusAssigns.add(bdAssign); // just add this busassignment to the
                                       // depotBusAssigns, which already lives
                                       // in the resultMap.
      }
    }

    return resultMap;
  }
}
