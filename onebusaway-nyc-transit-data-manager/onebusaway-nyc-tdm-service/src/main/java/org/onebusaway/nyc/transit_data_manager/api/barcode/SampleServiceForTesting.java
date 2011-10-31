package org.onebusaway.nyc.transit_data_manager.api.barcode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.stereotype.Component;

@Path("/barcode2")
@Component
public class SampleServiceForTesting {

  public SampleServiceForTesting() {
    super();
  }
  
  @Path("/test")
  @Consumes(MediaType.TEXT_PLAIN)
  @POST
  public Response testReturnFileAtPath(String zipFileToReturnPath) {
    final File theFile = new File(zipFileToReturnPath);
    
    StreamingOutput output = new StreamingOutput() {
      
      @Override
      public void write(OutputStream out) throws IOException,
          WebApplicationException {

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(theFile));
        
        BufferedOutputStream bos = new BufferedOutputStream(out);
        
        int data;
        while ((data = bis.read()) != -1) {
          bos.write(data);
        }
        
        bis.close();
        bos.flush();
        bos.close();
      }
    };
    
    Response response = Response.ok(output, "application/zip").build();
    
    return response;
  }
}
