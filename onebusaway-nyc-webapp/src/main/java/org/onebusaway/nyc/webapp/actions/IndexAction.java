/**
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.onebusaway.nyc.webapp.actions;

import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Action for home page
 * 
 */
public class IndexAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  public String getRevision() throws IOException {
    URL url = getClass().getResource("revision.txt");
    assert (url != null);
    String filename = url.getFile();
    FileInputStream fis = new FileInputStream(new File(filename));
    StringBuffer out = new StringBuffer();
    byte[] b = new byte[4096];
    while (true) {
      int read = fis.read(b);
        if (read == -1) {
            break;
        }
      out.append(new String(b, 0, read));
    }
    return out.toString();
  }

}
