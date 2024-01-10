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
import org.onebusaway.api.web.actions.api.where.CurrentTimeResource;
import org.onebusaway.utility.DateLibrary;

import javax.ws.rs.core.Response;

public class CurrentTimeActionTest {

  @Test
  public void test() throws ParseException {

    CurrentTimeResource action = new CurrentTimeResource();

    long t = System.currentTimeMillis();

    Response headers = action.index();
    Assert.assertEquals(200, headers.getStatus());

    ResponseBean response = action.getModel();
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
