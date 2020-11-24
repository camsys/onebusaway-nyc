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
   * This method deserializes the Json read from the specified reader into an object of the specified type.
   * 
   * @param reader the reader to read from
   * @param typeOfT is the common superinterface for all types in the Java programming language. These include raw types, parameterized types, array types, type variables and primitive types.
   * @return an object of type T containing the parsed data.
   */
  <T> T readJson(Reader reader, Type typeOfT);
  
  /**
   * write json from objectToWrite to the writer.
   * @param writer
   * @param objectToWrite
   * @throws IOException
   */
  void writeJson(Writer writer, Object objectToWrite) throws IOException;
}
