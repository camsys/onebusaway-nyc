/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.transit_data_manager.api;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Base class for testing resources, contains utility methods. 
 *
 */
public class ResourceTest {

  private static Logger _log = LoggerFactory.getLogger(ResourceTest.class);
  private static final int CHUNK_SIZE = 1024;

  public ResourceTest() {
    
  }
  
  @Test
  public void testNoop() {
    // pass
  }
  
  protected void copy(InputStream source, String destinationFileName) {
    byte[] buff = new byte[CHUNK_SIZE];
    DataOutputStream destination = null;
    int read = 0;
    try {
      destination = new DataOutputStream(new FileOutputStream(
          destinationFileName));
      // lazy copy -- not recommend
      while ((read = source.read(buff)) > -1) {
        destination.write(buff, 0, read);
      }
    } catch (Exception any) {
      _log.error(any.toString());
      throw new RuntimeException(any);
    } finally {
      if (source != null)
        try {
          source.close();
        } catch (Exception any) {
        }
      if (destination != null)
        try {
          destination.close();
        } catch (Exception any) {
        }
    }

  }

}
