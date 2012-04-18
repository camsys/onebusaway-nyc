/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.utility;

import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.csv_entities.CsvEntityWriterFactory;
import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.csv_entities.exceptions.CsvEntityIOException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.csv.TabTokenizerStrategy;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SplitNycVehicleLocationReportIntoTracesMain {
  public static void main(String[] args) throws CsvEntityIOException,
      IOException {

    if (args.length < 1) {
      System.err.println("usage: trace.txt [trace.txt ...] outputDirectory");
      System.exit(-1);
    }

    final File outputDirectory = new File(args[args.length - 1]);

    if (!outputDirectory.exists())
      outputDirectory.mkdirs();

    final CsvEntityReader csvReader = new CsvEntityReader();
    csvReader.setTokenizerStrategy(new TabTokenizerStrategy());

    final EntityHandlerImpl handler = new EntityHandlerImpl(outputDirectory);
    csvReader.addEntityHandler(handler);

    for (int i = 0; i < args.length - 1; i++) {
      final FileReader reader = new FileReader(args[0]);
      csvReader.readEntities(NycRawLocationRecord.class, reader);
      reader.close();
    }

    csvReader.close();
    handler.close();
  }

  private static class EntityHandlerImpl implements EntityHandler {

    private final Map<AgencyAndId, NycRawLocationRecord> _lastRecordsByVehicleId = new HashMap<AgencyAndId, NycRawLocationRecord>();

    private final Map<AgencyAndId, EntityHandler> _entityHandlersByVehicleId = new HashMap<AgencyAndId, EntityHandler>();

    private final Map<AgencyAndId, Writer> _writersByVehicleId = new HashMap<AgencyAndId, Writer>();

    private final CsvEntityWriterFactory _factory = new CsvEntityWriterFactory();

    private final long _maxOffset = 60 * 60 * 1000;

    private final SimpleDateFormat _format = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH-mm-ss");

    private final File _outputDirectory;

    public EntityHandlerImpl(File outputDirectory) {
      _outputDirectory = outputDirectory;
      _factory.setTokenizerStrategy(new TabTokenizerStrategy());
    }

    @Override
    public void handleEntity(Object bean) {

      try {
        final NycRawLocationRecord record = (NycRawLocationRecord) bean;
        final AgencyAndId vehicleId = record.getVehicleId();

        final NycRawLocationRecord prev = _lastRecordsByVehicleId.put(
            vehicleId, record);

        EntityHandler handler = _entityHandlersByVehicleId.get(vehicleId);

        final long time = getTimeForRecord(record);

        if (prev == null || getTimeForRecord(prev) + _maxOffset < time
            || handler == null) {

          Writer writer = _writersByVehicleId.get(vehicleId);
          if (writer != null)
            writer.close();

          final String timeString = _format.format(new Date(time));
          final String fileName = vehicleId.getId() + "-" + timeString + ".txt";
          final File outputFile = new File(_outputDirectory, fileName);
          writer = new FileWriter(outputFile);

          handler = _factory.createWriter(NycRawLocationRecord.class, writer);
          _entityHandlersByVehicleId.put(vehicleId, handler);
          _writersByVehicleId.put(vehicleId, writer);
        }

        handler.handleEntity(record);
      } catch (final IOException ex) {
        throw new IllegalStateException(ex);
      }
    }

    public void close() throws IOException {
      for (final Writer writer : _writersByVehicleId.values())
        writer.close();
    }

    private long getTimeForRecord(NycRawLocationRecord record) {
      // This is more reliable, for grouping at least, than the actual device
      // time
      return record.getTimeReceived();
    }

  }
}
