package org.onebusaway.nyc.transit_data_manager.api.barcode;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.api.barcode.util.ZipFileTesterUtil;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

public class ZipFileReadingTest {

  private String SAMPLE_FILE_NAME = "/two_simple_text_files.zip";
  private int NUM_ENTRIES_IN_SAMPLE = 2;

  private File twoFileContentsSampleFile;
  private InputStream twoFileContentsSampleStream;

  private ZipFileTesterUtil zipFileReader;
  
  private SelectorThread selector;

  @Before
  public void setup() throws URISyntaxException, IllegalArgumentException,
      IOException {
    twoFileContentsSampleFile = new File(getClass().getResource(
        SAMPLE_FILE_NAME).toURI());

    twoFileContentsSampleStream = getClass().getResourceAsStream(
        SAMPLE_FILE_NAME);

    // Set up the webservice.
    Map<String, String> initParams = new HashMap<String, String>();
    initParams.put("com.sun.jersey.config.property.packages",
        "org.onebusaway.nyc.transit_data_manager.api.barcode");

    selector = GrizzlyWebContainerFactory.create("http://localhost:8080/api/",
        initParams);
  }

  @Test
  public void testReadingNumEntriesWithZipFile() throws ZipException,
      IOException {
     
    int actual = new ZipFileTesterUtil().getNumEntriesInZipFile(twoFileContentsSampleFile);

    assertEquals(NUM_ENTRIES_IN_SAMPLE, actual);
  }

  @Test
  public void testReadingNumEntriesWithZipInputStream() throws IOException {
    
    ZipFileTesterUtil tester = new ZipFileTesterUtil();
    
    int actual = tester.getNumEntriesInZipInputStream(twoFileContentsSampleStream);
    
    assertEquals(NUM_ENTRIES_IN_SAMPLE, actual);
  }

  @Test
  public void testReturnZipFileService() throws IOException {
    String filePathToPass = twoFileContentsSampleFile.getAbsolutePath();

    Client c = Client.create();

    WebResource r = c.resource("http://localhost:8080/api/barcode2/test");

    InputStream response = r.accept(MediaType.WILDCARD).post(InputStream.class,
        filePathToPass);

    int numEntries = new ZipFileTesterUtil().getNumEntriesInZipInputStream(response);

    assertEquals(NUM_ENTRIES_IN_SAMPLE, numEntries);
  }

  @After
  public void shutdown() throws IOException {
    if (twoFileContentsSampleStream != null)
      twoFileContentsSampleStream.close();

    // shut down the web service
    selector.stopEndpoint();
    selector = null;
  }

}
