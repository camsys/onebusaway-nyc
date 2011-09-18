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
import java.util.List;

import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.CsvEntityWriterFactory;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.csv.ListEntityHandler;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityIOException;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;

public class CompileTestTracesMain {

  public static void main(String[] args) throws CsvEntityIOException,
      IOException {

    if (args.length != 3) {
      System.err.println("usage: trace_raw.gps.csv trace_labels.gps.csv trace_output.gps.csv");
      System.exit(-1);
    }

    List<NycTestInferredLocationRecord> rawRecords = readRecords(new File(args[0]));
    List<NycTestInferredLocationRecord> labeledRecords = readRecords(new File(args[1]));

    if (rawRecords.size() != labeledRecords.size()) {
      throw new IllegalStateException("expected record counts to match: raw="
          + rawRecords.size() + " labeled=" + labeledRecords.size());
    }

    CsvEntityWriterFactory factory = new CsvEntityWriterFactory();
    FileWriter out = new FileWriter(args[2]);
    EntityHandler handler = factory.createWriter(NycTestInferredLocationRecord.class,
        out);

    for (int i = 0; i < rawRecords.size(); i++) {
      NycTestInferredLocationRecord rawRecord = rawRecords.get(i);
      NycTestInferredLocationRecord labeledRecord = labeledRecords.get(i);
      rawRecord.setActualRunId(labeledRecord.getActualRunId());
      rawRecord.setActualBlockId(labeledRecord.getActualBlockId());
      rawRecord.setActualDistanceAlongBlock(labeledRecord.getActualDistanceAlongBlock());
      rawRecord.setActualPhase(labeledRecord.getActualPhase());
      rawRecord.setActualServiceDate(labeledRecord.getActualServiceDate());

      handler.handleEntity(rawRecord);
    }

    out.close();
  }

  private static List<NycTestInferredLocationRecord> readRecords(File path)
      throws CsvEntityIOException, IOException {

    CsvEntityReader reader = new CsvEntityReader();

    ListEntityHandler<NycTestInferredLocationRecord> handler = new ListEntityHandler<NycTestInferredLocationRecord>();
    reader.addEntityHandler(handler);

    reader.readEntities(NycTestInferredLocationRecord.class, new FileReader(path));

    return handler.getValues();
  }

}
