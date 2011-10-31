package org.onebusaway.nyc.transit_data_manager.api.barcode;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import javax.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

public class QRBatchServiceTest {

  private String batchGenUrl = "http://localhost:8080/api/barcode/batchGen";
  
  private SelectorThread selector;
  
  @Before
  public void createService() throws IOException {
    Map<String, String> initParams = new HashMap<String, String>();
    initParams.put("com.sun.jersey.config.property.packages", "org.onebusaway.nyc.transit_data_manager.api.barcode");
    
    selector = GrizzlyWebContainerFactory.create("http://localhost:8080/api/", initParams);
  }
  
  @Test
  public void test() throws ZipException, IOException {
    
    String eol = System.getProperty("line.separator");
    String requestCSV = "STOP_ID" + eol + String.valueOf(5000) + eol + String.valueOf(6000) + eol + String.valueOf(7000);
    
    Client c = Client.create();
    
    WebResource r = c.resource(batchGenUrl);
    
    InputStream response = r.accept(MediaType.WILDCARD).type("text/csv").post(InputStream.class, requestCSV);
    
    ZipInputStream zis = new ZipInputStream(response);
    
    int numEntries = 0;
    while ( zis.getNextEntry() != null ) {
      numEntries++;
    }
    
    assertEquals(3, numEntries);
  }
  
  @After
  public void destroyService() {
    selector.stopEndpoint();
    selector = null;
  }

}
