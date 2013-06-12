package org.onebusaway.nyc.report_archive.queue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;

@RunWith(MockitoJUnitRunner.class)
public class ArchivingInputQueueListenerTaskTest {

  @Mock
  CcLocationReportDao dao;
  
  @InjectMocks
  ArchivingInputQueueListenerTask t = new ArchivingInputQueueListenerTask();

  @Test
  public void testProcessMessage() throws IOException {
    String contents = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("sample-message.json"))).readLine();
    // This is a no-op test for now.
    t.processMessage("this is the address", contents.getBytes());
  }

}