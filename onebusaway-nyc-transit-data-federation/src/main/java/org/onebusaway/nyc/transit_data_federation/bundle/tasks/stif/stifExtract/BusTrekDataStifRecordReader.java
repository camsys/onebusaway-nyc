/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.*;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.reader.StifRecordReader;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.MidTripExtendedTripRecordFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class BusTrekDataStifRecordReader extends StifRecordReader {
    //todo: refactor by merging MidTripExtendedTripRecord into TripRecord and deleating this, its parent and extendedTripRecord

    private static final int BUFFER_SIZE = 8192;
    private InputStream inputStream;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int start = 0;
    private int bufferEnd = 0;

    static Map<Integer, StifRecordFactory<?>> recordFactories;

    static {
        recordFactories = new HashMap<Integer, StifRecordFactory<?>>();
        recordFactories.put(11, new TimetableRecordFactory());
        recordFactories.put(15, new GeographyRecordFactory());
        recordFactories.put(21, new MidTripExtendedTripRecordFactory());
        recordFactories.put(31, new EventRecordFactory());
        recordFactories.put(35, new SignCodeRecordFactory());
    }

    public BusTrekDataStifRecordReader(InputStream stream) {
        super(stream);
        inputStream = stream;
    }


    public StifRecord read() throws IOException {
        int recordStart;
        int end = -1;
        StifRecordFactory<?> factory;
        while (true) {
            end = -1;
            while (end == -1) {
                for (int i = start; i < bufferEnd; ++i) {
                    if (buffer[i] == '\r') {
                        end = i;
                        break;
                    }
                    if (buffer[i] == '\n') {
                        end = i -1;
                        break;
                    }
                }
                if (end != -1) {
                    break;
                }
                // we need to move the stuff at the end of the buffer to the
                // beginning and refill
                ByteBuffer bbuffer = ByteBuffer.wrap(buffer);
                if (start < BUFFER_SIZE) {
                    bbuffer.put(buffer, start, BUFFER_SIZE - start);
                    bufferEnd = bufferEnd - start;
                } else {
                    // skip extra bytes (newlines) at the beginning of a block
                    bufferEnd = 0;
                    inputStream.read(buffer, bufferEnd, start - BUFFER_SIZE);
                }
                if (BUFFER_SIZE == bufferEnd) {
                    // buffer is full and yet we want to read more. Throw an
                    // exception
                    throw new IOException("Too-long line trying to read STIF");
                }
                int bytesRead = inputStream.read(buffer, bufferEnd, BUFFER_SIZE
                        - bufferEnd);
                if (bytesRead == -1) {
                    // eof
                    if (start >= bufferEnd) {
                        return null;
                    } else {
                        throw new RuntimeException(
                                "Parse error: stif requires trailing newline");
                    }
                }
                bufferEnd += bytesRead;
                start = 0;
            }
            // at this point we definitely have the record between start and
            // end.
            recordStart = start;
            start = end + 2; // snip off cr/nl
            int recordType = Integer
                    .parseInt(new String(buffer, recordStart, 2));
            factory = recordFactories.get(recordType);
            if (factory != null) {
                break;
            }
        }
        return factory.createRecord(buffer, recordStart, end);
    }
}
