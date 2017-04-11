/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.gtfsrt.util;

import org.onebusaway.nyc.gtfsrt.model.test.BlockTripEntry;

import java.util.HashMap;
import java.util.Map;

/**
 * Read a TSV of the format "block_id   trip_id".
 */
public class BlockTripMapReader extends RecordReader<BlockTripEntry> {

    @Override
    public BlockTripEntry convert(Object o) {
        return (BlockTripEntry) o;
    }

    /**
     * Get a map from trip to block from a TSV.
     *
     * @param filename file to read from
     * @return map from trip ID to block ID.
     */
    public Map<String, String> getTripToBlockMap(String filename) {
        Map<String, String> map = new HashMap<String, String>();
        for (BlockTripEntry e : getRecords(filename, BlockTripEntry.class)) {
            map.put(e.getTripId(), e.getBlockId());
        }
        return map;
    }
}
