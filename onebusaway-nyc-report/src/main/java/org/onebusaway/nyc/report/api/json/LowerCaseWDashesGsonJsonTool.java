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

package org.onebusaway.nyc.report.api.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import org.joda.time.LocalDate;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LowerCaseWDashesGsonJsonTool implements JsonTool{
  
  /**
   * Initialize, and specify whether we should pretty print.
   * @param prettyPrintOutput to pretty print, or not to pretty print.
   */
  public LowerCaseWDashesGsonJsonTool(boolean prettyPrintOutput) {
    super();
    
    this.prettyPrintOutput = prettyPrintOutput;
    
    buildGsonObject();
  }
  
  /**
   * Initialize using the default of no pretty printing.
   */
  public LowerCaseWDashesGsonJsonTool() {
    this(false);
  }
  
  private boolean prettyPrintOutput;
  
  public void setPrettyPrintOutput(boolean prettyPrintOutput) {
    this.prettyPrintOutput = prettyPrintOutput;
    
    buildGsonObject();
  }

  private Gson gson;
  
  @Override
  public <T> T readJson(Reader reader, Class<T> classOfT) {
    return gson.fromJson(reader, classOfT);
  }
  
  @Override
  public <T> T readJson(Reader reader, Type typeOfT) {
    return gson.fromJson(reader, typeOfT);
  }

  @Override
  public void writeJson(Writer writer, Object objectToWrite) throws IOException {

    String serializedObject = gson.toJson(objectToWrite);
    
    writer.write(serializedObject);
    
    
  }
  
  private void buildGsonObject() {
    GsonBuilder gbuilder = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES);
    
    setTypeAdapters(gbuilder);
    
    if (prettyPrintOutput)
      gbuilder.setPrettyPrinting();
    
    gson = gbuilder.create();
  }
  
  private void setTypeAdapters(GsonBuilder gsonBuilder) {
    // First set Joda DateTime Adapter
    //gsonBuilder.registerTypeAdapter(LocalDate.class, new JodaLocalDateAdapter());
  }
}
