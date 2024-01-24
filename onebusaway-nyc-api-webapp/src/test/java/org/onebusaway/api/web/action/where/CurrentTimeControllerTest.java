/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.web.action.where;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.TimeBean;
import org.onebusaway.api.web.actions.api.where.CurrentTimeController;
import org.onebusaway.utility.DateLibrary;

public class CurrentTimeControllerTest {

  @Test
  public void test() throws ParseException {

    CurrentTimeController action = new CurrentTimeController();

    long t = System.currentTimeMillis();

    ResponseBean response = action.index();
    Assert.assertEquals(200, response.getCode());
    Assert.assertEquals(1, response.getVersion());

    TimeBean time = (TimeBean) response.getData();

    Assert.assertNotNull(time);

    long delta = Math.abs(time.getTime() - t);
    Assert.assertTrue("check that time delta is within limits: delta=" + delta,
        delta < 100);

    String readableTime = DateLibrary.getTimeAsIso8601String(new Date(
        time.getTime()));
    Assert.assertEquals(readableTime, time.getReadableTime());
  }
}
