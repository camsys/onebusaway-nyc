package ext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;

import play.templates.JavaExtensions;

public class ServiceAlertExtensions extends JavaExtensions {

  // TODO This method is so full of kludges it's not funny.
  public static String formatTimeWindowValue(ServiceAlertBean serviceAlert,
      String windowName, String fieldName) {
    try {
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
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public static String formatDate(Long d) {
    if (d == 0)
      return "(none)";
    return new Date(d).toString();
  }
  
  public static String formatAffects(ServiceAlertBean serviceAlert) {
      List<String> s = new ArrayList<String>();
      if (serviceAlert == null)
        return "serviceAlert is null";
      if (serviceAlert.getAllAffects() == null)
        return "getAllAffects is null";
      List<SituationAffectsBean> affects = serviceAlert.getAllAffects();
      for (SituationAffectsBean affect: affects) {
        s.add(affect.getRouteId() + ":" + affect.getDirectionId());
      }
      return StringUtils.join(s,  "<br/>");
    }
  
  public static String formatAffect(SituationAffectsBean b) {
      return b.getRouteId() + ":" + b.getDirectionId();
    }
  
  public static String formatMultivaluedString(ServiceAlertBean serviceAlert, String fieldName) {
    String result;
    if (fieldName=="summaries") {
      return formatMultivalued(serviceAlert.getSummaries());
    } else if (fieldName=="descriptions") {
      return formatMultivalued(serviceAlert.getDescriptions());
    }
    throw new RuntimeException("Unknown field name: " + fieldName);
  }
  
  public static String formatConsequences(ServiceAlertBean serviceAlert) {
    List<SituationConsequenceBean> list = serviceAlert.getConsequences();
    if (list == null) {
      return("(none)");
    }
    List<String> effects = new ArrayList<String>();
    for (SituationConsequenceBean b: list) {
      effects.add(b.getEffect().toString());
    }
    return(StringUtils.join(effects, ", "));
  }

  private static String formatMultivalued(
      List<NaturalLanguageStringBean> values) {
    if (values == null || values.isEmpty())
      return "(none)";
    if (values.get(0) != null)
      return values.get(0).getValue();
    return "(none)";
  }
}