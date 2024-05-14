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
package org.onebusaway.api.web.actions.api.where;

import java.util.*;

public class FieldErrorSupport {
  public static final String MISSING_REQUIRED_FIELD = "missingRequiredField";
  
  public static final String INVALID_FIELD_VALUE = "invalidFieldValue";

  LinkedHashMap<String, List<String>> errorOutput;
  int numDefault;

  public FieldErrorSupport() {
    errorOutput = new LinkedHashMap<>();
    //tie this to the appropriate setter
    numDefault = -1;
  }

  public FieldErrorSupport invalidValue(String s){
    errorOutput.put(s,arrayListOf(FieldErrorSupport.INVALID_FIELD_VALUE));
    return this;
  }

  public FieldErrorSupport addError(String s, String e){
    errorOutput.put(s,arrayListOf(e));
    return this;
  }

  public FieldErrorSupport hasFieldError(Object o, String s){
    if (o==null) errorOutput.put(s,arrayListOf(FieldErrorSupport.MISSING_REQUIRED_FIELD));
    return this;
  }
  public FieldErrorSupport hasFieldError(int i, String s) {
    if (i == numDefault) errorOutput.put(s, arrayListOf(FieldErrorSupport.MISSING_REQUIRED_FIELD));
    return this;
  }
  public FieldErrorSupport hasFieldError(long i, String s){
    if (i==numDefault) errorOutput.put(s,arrayListOf(FieldErrorSupport.MISSING_REQUIRED_FIELD));
    return this;
  }

  public Map<String, List<String>> getErrors(){
    return errorOutput;
  }

  public boolean hasErrors(){
    return errorOutput.size()!=0;
  }

  private List arrayListOf(String s){
    List out = new ArrayList<>();
    out.add(s);
    return out;
  }
}
