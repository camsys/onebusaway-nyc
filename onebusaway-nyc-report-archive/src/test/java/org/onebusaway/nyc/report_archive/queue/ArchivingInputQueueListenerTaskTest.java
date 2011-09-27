package org.onebusaway.nyc.report_archive.queue;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ArchivingInputQueueListenerTaskTest extends
    ArchivingInputQueueListenerTask {

  @Test
  public void testProcessMessage() throws IOException {
    String contents = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("sample-message.json"))).readLine(); 
    processMessage("this is the address", contents);
  }

}
