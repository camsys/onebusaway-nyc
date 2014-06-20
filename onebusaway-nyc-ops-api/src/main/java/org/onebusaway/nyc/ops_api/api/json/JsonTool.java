package org.onebusaway.nyc.ops_api.api.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * wraps the json handler - most likely gson or jackson
 * @author sclark
 *
 */
public interface JsonTool {
  /**
   * Parse/Read json from a Reader reader to a new object of type classOfT
   * 
   * <p>
   * Note that classOfT cannot utilize a generic type, for instance List<String>.
   * </p>
   * @param reader the reader to read from
   * @param classOfT the class the input corresponds to and will be mapped to. Cannot directly pass a generic type.
   * @return an object of type classOfT containnig the parsed data.
   */
  <T> T readJson(Reader reader, Class<T> classOfT);
  
  /**
   * write json from objectToWrite to the writer.
   * @param writer
   * @param objectToWrite
   * @throws IOException
   */
  void writeJson(Writer writer, Object objectToWrite) throws IOException;
}
