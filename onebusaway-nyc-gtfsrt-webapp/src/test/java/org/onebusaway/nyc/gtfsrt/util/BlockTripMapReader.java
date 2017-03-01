package org.onebusaway.nyc.gtfsrt.util;

import org.onebusaway.nyc.gtfsrt.model.BlockTripEntry;

import java.util.HashMap;
import java.util.Map;

public class BlockTripMapReader extends RecordReader<BlockTripEntry> {

    @Override
    public BlockTripEntry convert(Object o) {
        return (BlockTripEntry) o;
    }

    public Map<String, String> getTripToBlockMap(String filename) {
        Map<String, String> map = new HashMap<String, String>();
        for (BlockTripEntry e : getRecords(filename, BlockTripEntry.class)) {
            map.put(e.getTripId(), e.getBlockId());
        }
        return map;
    }
}
