/**
 * Copyright (C) 2022 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.report_archive.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.report.api.json.LowerCaseWDashesGsonJsonTool;
import org.onebusaway.nyc.report.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report_archive.impl.CancelledTripDaoImpl;
import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;
import org.onebusaway.nyc.report_archive.result.HistoricalRecord;
import org.onebusaway.nyc.report_archive.services.CancelledTripDao;
import org.onebusaway.nyc.report_archive.services.HistoricalRecordsDao;

import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonPartEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HistoricalCancelledTripRecordsResourceTest {

    @Test
    public void testMakeJson() throws ParseException {
        HistoricalCancelledTripRecordsResource r = new HistoricalCancelledTripRecordsResource();

        CancelledTripDao dao = mock(CancelledTripDao.class);
        List<NycCancelledTripRecord> results = new ArrayList<NycCancelledTripRecord>();
        NycCancelledTripRecord cancelledTripRecord = new NycCancelledTripRecord();
        cancelledTripRecord.setId((long) 1);
        cancelledTripRecord.setBlock("test block");
        cancelledTripRecord.setServiceDate(LocalDate.now());
        cancelledTripRecord.setRecordTimeStamp(1647030102000l);
        results.add(cancelledTripRecord);
        System.out.println("results= "+results);
        when(dao.getReports(any(HistoricalCancelledTripQuery.class))).thenReturn(results);

        r.setCancelledTripDao(dao);

        Response response  = r.getHistoricalCancelledTripRecords(500, null,
                null, "2022-03-11", null, null);
        System.out.println("response= "+response);

        Object entity = response.getEntity();
        System.out.println("entity= "+entity.toString());

        assertJsonPartEquals("test block", entity.toString(),"records[0].block");

    }
}
