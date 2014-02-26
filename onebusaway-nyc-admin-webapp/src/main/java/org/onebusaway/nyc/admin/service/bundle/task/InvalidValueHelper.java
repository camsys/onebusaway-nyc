package org.onebusaway.nyc.admin.service.bundle.task;

import com.conveyal.gtfs.model.InvalidValue;

public class InvalidValueHelper {

   
  public static String getCsvHeader() {
    return "affectedEntity,affectedField,affectedEntityId,problemType,problemDescription,problemData";
  }
  
  public static String getCsv(InvalidValue iv) {
    StringBuffer buff = new StringBuffer();
    buff.append(iv.affectedEntity);
    buff.append(",");
    buff.append(iv.affectedField);
    buff.append(",");
    buff.append(iv.affectedEntityId);
    buff.append(",");
    buff.append(iv.problemType);
    buff.append(",");
    buff.append(iv.problemDescription);
    buff.append(",");
    buff.append(iv.problemData);
    return buff.toString();
  }
  
}
