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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.CsvEntityWriterFactory;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public class SplitTestTracesMain {

  public static void main(String[] args) throws Exception {

    if (args.length < 2) {
      System.err.println("usage: input_trace [input_trace,...] output_dir");
      System.exit(-1);
    }

    File outputDir = new File(args[args.length - 1]);

    if (!outputDir.exists())
      outputDir.mkdirs();

    CsvEntityReader reader = new CsvEntityReader();

    OutputHandler handler = new OutputHandler(outputDir);
    reader.addEntityHandler(handler);

    for (int i = 0; i < args.length - 1; i++)
      reader.readEntities(NycTestLocationRecord.class, new FileReader(args[i]));

    handler.close();
  }

  private static class OutputHandler implements EntityHandler {

    private SimpleDateFormat _format;

    private File _outputDir;

    private FileWriter _outputWriter = null;

    private EntityHandler _entityWriter = null;

    private NycTestLocationRecord _prevRecord = null;

    public OutputHandler(File outputDir) {
      _outputDir = outputDir;

      _format = new SimpleDateFormat("yyyy-MM-dd_HH_mm");
      _format.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    }

    public void close() throws IOException {
      if (_outputWriter != null)
        _outputWriter.close();
    }

    @Override
    public void handleEntity(Object bean) {

      try {
        NycTestLocationRecord record = (NycTestLocationRecord) bean;

        if (outputNeedsRefresh(record)) {

          if (_outputWriter != null)
            _outputWriter.close();

          File outputFile = getOutputFile(record);
          _outputWriter = new FileWriter(outputFile);

          CsvEntityWriterFactory factory = new CsvEntityWriterFactory();
          _entityWriter = factory.createWriter(NycTestLocationRecord.class,
              _outputWriter);
        }

        _entityWriter.handleEntity(record);

        _prevRecord = record;

      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }

    private boolean outputNeedsRefresh(NycTestLocationRecord record) {
      return _entityWriter == null
          || _prevRecord == null
          || (record.getTimestamp() - _prevRecord.getTimestamp()) > 30 * 60 * 1000;
    }

    private File getOutputFile(NycTestLocationRecord record) {
      return new File(_outputDir, record.getVehicleId() + "-"
          + _format.format(new Date(record.getTimestamp())) + ".csv");
    }

  }
}
