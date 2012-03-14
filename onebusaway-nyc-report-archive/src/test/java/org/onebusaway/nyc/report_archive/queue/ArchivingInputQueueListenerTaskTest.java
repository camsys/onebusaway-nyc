package org.onebusaway.nyc.report_archive.queue;

import static org.junit.Assert.*;

import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.staticmock.MockStaticEntityMethods;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.TimeZone;

public class ArchivingInputQueueListenerTaskTest {

  @Mock
  CcLocationReportDao dao;
  
  @InjectMocks
  ArchivingInputQueueListenerTask t = new ArchivingInputQueueListenerTask();
  
  @Test
  public void testProcessMessage() throws IOException {
    String contents = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("sample-message.json"))).readLine();
    // TODO This is a no-op test for now.
//    t.processMessage("this is the address", contents);
  }

  @Test
  public void testGetZoneOffset() {
    Calendar c = Calendar.getInstance();
    c.set(2012, 2, 8, 0, 0, 0); // 1 week before DST
    assertEquals(-18000000, TimeZone.getTimeZone("America/New_York").getOffset(c.getTime().getTime()));
    assertEquals(14400000, TimeZone.getTimeZone("Europe/Moscow").getOffset(c.getTime().getTime()));

    // NYC timezone offset standard time
    assertEquals(-18000000, TimeZone.getDefault().getOffset(c.getTime().getTime()));

    String offset = t.getZoneOffset(c.getTime(), "America/New_York");
    assertEquals("-05:00", offset);

    c.set(2012, 2, 16, 0, 0, 0); // 1 week after DST
    assertEquals(-14400000, TimeZone.getTimeZone("America/New_York").getOffset(c.getTime().getTime()));
    assertEquals(14400000, TimeZone.getTimeZone("Europe/Moscow").getOffset(c.getTime().getTime()));

    // reset internal state
    t = new ArchivingInputQueueListenerTask();

    String offsetDST = t.getZoneOffset(c.getTime(), "America/New_York");
    assertEquals("-04:00", offsetDST);
  }
}
