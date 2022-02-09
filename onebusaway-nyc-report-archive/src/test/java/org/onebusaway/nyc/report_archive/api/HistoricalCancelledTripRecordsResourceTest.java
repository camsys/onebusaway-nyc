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
import java.util.ArrayList;
import java.util.List;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonPartEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HistoricalCancelledTripRecordsResourceTest {

    @Test
    public void testMakeJson(){
        HistoricalCancelledTripRecordsResource r = new HistoricalCancelledTripRecordsResource();
        // this is normally autowired.  See application-context.xml.
        r.setJsonTool(new LowerCaseWDashesGsonJsonTool(true));

        CancelledTripDao dao = mock(CancelledTripDao.class);
        List<NycCancelledTripRecord> results = new ArrayList<NycCancelledTripRecord>();
        NycCancelledTripRecord cancelledTripRecord = new NycCancelledTripRecord();
        cancelledTripRecord.setId((long) 1);
        cancelledTripRecord.setBlock("test block");
        results.add(cancelledTripRecord);
        System.out.println("results= "+results);
        when(dao.getReports()).thenReturn(results);

        r.setCancelledTripDao(dao);

        Response response  = r.getHistoricalCancelledTripRecords(500, null, null);
        System.out.println("response= "+response);

        Object entity = response.getEntity();
        System.out.println("entity= "+entity.toString());

        assertJsonPartEquals("test block", entity.toString(),"records[0].block");

    }
}
