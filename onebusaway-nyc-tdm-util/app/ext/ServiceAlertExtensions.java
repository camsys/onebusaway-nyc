package ext;

import java.util.Date;
import java.util.List;

import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;

import play.templates.JavaExtensions;

public class ServiceAlertExtensions extends JavaExtensions {

  // TODO This method is so full of kludges it's not funny.
  public static String formatTimeWindowValue(ServiceAlertBean serviceAlert,
      String windowName, String fieldName) {
    if (serviceAlert == null || serviceAlert.getPublicationWindows() == null) {
      return "(none)";
    }
    List<TimeRangeBean> windows;
    if (windowName.equalsIgnoreCase("publication"))
      windows = serviceAlert.getPublicationWindows();
    else
      windows = serviceAlert.getActiveWindows();
    TimeRangeBean bean = windows.get(0);
    long value;
    if (fieldName.equalsIgnoreCase("from"))
      value = bean.getFrom();
    else
      value = bean.getTo();
    if (value == 0)
      return "";
    return (new Date(value)).toString();
  }

  public static String formatDate(Long d) {
    if (d == 0)
      return "(none)";
    return new Date(d).toString();
  }
}