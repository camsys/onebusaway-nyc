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

package org.onebusaway.nyc.sms.actions.model;

import org.junit.Assert;
import org.junit.Test;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class VehicleResultTest {

    @Test
    public void VehicleResultIsSerializable()  {

        boolean exceptionThrown = false;
        try {
            VehicleOccupancyRecord vor = new VehicleOccupancyRecord();
            VehicleResult.OccupancyConfig occupancyConfig = VehicleResult.OccupancyConfig.PASSENGER_COUNT;
            VehicleResult vehicleResult = new VehicleResult("test", "test", vor, occupancyConfig,false);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(vehicleResult);
            oos.flush();
            byte [] data = bos.toByteArray();

        } catch(IOException ex) {
            exceptionThrown = true;
        }
        Assert.assertFalse(exceptionThrown);
    }
}
