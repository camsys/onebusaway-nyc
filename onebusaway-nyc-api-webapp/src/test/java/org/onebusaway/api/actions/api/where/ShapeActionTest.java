/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.actions.api.where;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Date;

import org.apache.struts2.rest.DefaultHttpHeaders;
import org.junit.Test;
import org.onebusaway.api.impl.CustomJsonLibHandler;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.TimeBean;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.utility.DateLibrary;

import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import net.sf.json.util.JSONStringer;
import net.sf.json.util.JSONUtils;

public class ShapeActionTest {
  
  private static final String encodedPoints = "{j`xF`}fbMY`AGTM^dC`Bz@j@jAt@VPbBlA|BxAt@f@dAt@~BxA|BvAv@h@dAr@|BzAdC`Bz@j@nAx@zBxAxBxAt@f@bAn@`C~A|BzAr@d@jAv@~B|Ap@b@lEtC~@l@fAr@~B|AzBzAZT`BfAxBxA~BxAlBpANJrBrA`C|AtBrAVJjAz@x@j@zBxAb@ZxA`A|BxAzBzAf@\\tA~@zBxApBpALHzBxAfCbBbAr@^Vb@T~B|At@f@fAr@|BzAr@d@jAv@~BzAfCbB|@l@hAt@|BzA~BzArBtAHD~B|A|BzAfC`BRNtBtAr@d@fAr@|BzA|BzAr@d@fAt@|BxAbAp@v@h@|BzANLf@ZbAn@|BzA|BxAfAr@r@f@`@Tz@j@^VTPdBjAfC`BdC`B|BxAzB|Ar@b@`Al@DF|BxA|BzA|BxAv@h@bAp@|BzAbC|AdBjANJ|BzAzBzA|BxAdC`BdAp@~@n@zBxAzBxAz@j@~@l@xBxAtA~@RLNH|BzAdC`B~@l@v@h@NBTFdBhAzBxAr@d@t@f@LNNRhBlAnBpAJF|BzAxBvAzBxAr@d@fAr@bC`BjAv@v@h@zBxAtBrAt@f@|@l@tBtArBrAv@f@|DjChC`BhCdB~BzAn@b@nAx@vBvArBrAvBvAxBvArBrAr@d@dAr@xBvAvBvAr@d@nAx@`C|Af@^bAH^F^L??hEdBfFtBGTtFdBDUb@}Bx@mD\\Nj@oCBMbDrAxD~Ax@\\nDxA`FrBt@ZVJf@B^AfBOdCSlBO`@CJnCDxA?DJzBDhA`@vKD`@D`@FbAD~@RrDLrCDx@b@^xB~B`@^FDlBpANJPLDBDDFBRNFFVRHDDBBBLHBBVVFHbBdCv@bAn@x@n@t@bBdBVVHLnBrDT\\hAjBl@`A\\h@hAhBvA|BdBpCLRlAnBT^DFp@jA~ArCNn@Lp@n@tDnAWxCk@l@nE";
  
  private static final String expected = "{\"entry\":{\"length\":269,\"levels\":\"\",\"points\":\"" +
		  								 encodedPoints +		  								 
		  								 "\"},\"references\":{\"agencies\":[],\"routes\":[],\"situations\":[],\"stops\":[],\"trips\":[]}}";
  @Test
  public void pointsEscapedStringTest() throws ParseException, IOException {
	  CustomJsonLibHandler jsonLib = new CustomJsonLibHandler();
	  BeanFactoryV2 factory = new BeanFactoryV2(true);
	  
	  EncodedPolylineBean shape = new EncodedPolylineBean();
	  shape.setLength(269);
	  shape.setPoints(encodedPoints);
	  
	  Writer writer = new StringWriter();
	  jsonLib.fromObject(factory.getResponse(shape), "200", writer, null);
	  writer.close();
	  
	  String jsonString = writer.toString();
	  
	  assertEquals(expected, jsonString);
  }
  
  
}
