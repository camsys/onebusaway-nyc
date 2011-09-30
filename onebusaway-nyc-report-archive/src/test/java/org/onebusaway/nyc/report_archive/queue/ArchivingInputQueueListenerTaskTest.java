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

}
