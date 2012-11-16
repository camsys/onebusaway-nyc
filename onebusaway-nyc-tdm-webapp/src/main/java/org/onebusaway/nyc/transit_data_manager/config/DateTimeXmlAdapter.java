package org.onebusaway.nyc.transit_data_manager.config;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.joda.time.DateTime;

public class DateTimeXmlAdapter extends XmlAdapter<String, DateTime> {

  @Override
  public String marshal(DateTime dateTime) throws Exception {
    return dateTime.toString();
  }

  @Override
  public DateTime unmarshal(String dateTimeStr) throws Exception {
    return new DateTime(dateTimeStr);
  }

}
