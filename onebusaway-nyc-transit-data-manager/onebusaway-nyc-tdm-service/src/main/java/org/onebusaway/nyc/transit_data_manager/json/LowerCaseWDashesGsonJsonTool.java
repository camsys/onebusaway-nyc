package org.onebusaway.nyc.transit_data_manager.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.joda.time.DateTime;
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

  @Override
  public <T> T readJson(Reader reader, Class<T> classOfT) {
    Gson gson = buildGsonObject();
    return gson.fromJson(reader, classOfT);
  }

  @Override
  public void writeJson(Writer writer, Object objectToWrite) throws IOException {
    Gson gson = buildGsonObject();
    
    String serializedObject = gson.toJson(objectToWrite);
    
    writer.write(serializedObject);
  }
  
  private Gson buildGsonObject() {
    GsonBuilder gbuilder = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES);
    
    setTypeAdapters(gbuilder);
    
    if (prettyPrintOutput)
      gbuilder.setPrettyPrinting();
    
    Gson gson = gbuilder.create();
    
    return gson;
  }
  
  private void setTypeAdapters(GsonBuilder gsonBuilder) {
    // First set Joda DateTime Adapter
    gsonBuilder.registerTypeAdapter(LocalDate.class, new JodaLocalDateAdapter());
    gsonBuilder.registerTypeAdapter(DateTime.class, new JodaDateTimeAdapter());
  }
}
