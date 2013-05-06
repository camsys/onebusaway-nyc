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
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.csv_entities.exceptions.CsvEntityIOException;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.library.TurboButton;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

public class PausesMain {
  public static void main(String[] args) throws CsvEntityIOException,
      FileNotFoundException, IOException {

    System.out.println("start");

    final CsvEntityReader reader = new CsvEntityReader();
    reader.addEntityHandler(new Handler());
    reader.readEntities(NycTestInferredLocationRecord.class,
        new FileInputStream(args[0]));

  }

  private static class Handler implements EntityHandler {

    private CoordinatePoint lastPoint = null;

    private long lastTime = 0;

    @Override
    public void handleEntity(Object bean) {

      final NycTestInferredLocationRecord record = (NycTestInferredLocationRecord) bean;
      final CoordinatePoint p = new CoordinatePoint(record.getLat(),
          record.getLon());

      if (lastPoint == null) {
        lastPoint = p;
        lastTime = record.getTimestamp();
      }

      final double d = TurboButton.distance(lastPoint, p);
      if (d > 20) {

        if (record.getTimestamp() - lastTime > 1 * 60 * 1000) {
          System.out.println("=========");
          System.out.println(new Date(lastTime));
          System.out.println(new Date(record.getTimestamp()));
        }

        lastPoint = p;
        lastTime = record.getTimestamp();
      }
    }

  }
}
