package org.onebusaway.nyc.transit_data_manager.siri;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

@Entity
@Table(name = "obanyc_servicealerts")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class ServiceAlertRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  transient static private Type listTimeRangeBeanType = new TypeToken<List<TimeRangeBean>>() {
  }.getType();
  transient static private Type listNaturalLanguageStringBeanType = new TypeToken<List<NaturalLanguageStringBean>>() {
  }.getType();
  transient static private Type listSituationAffectsBeanType = new TypeToken<List<SituationAffectsBean>>() {
  }.getType();
  transient static private Type listSituationConsequenceBeanType = new TypeToken<List<SituationConsequenceBean>>() {
  }.getType();

  @Id
  @GeneratedValue
  @AccessType("property")
  private Integer id;

  @Column(nullable = false, name = "service_alert_id", length = 64)
  private String serviceAlertId;

  @Column(nullable = false, name = "creation_time")
  private long creationTime;

  @Column(name = "active_windows_json", length = 512)
  private String activeWindows; // List<TimeRangeBean>

  @Column(name = "publication_windows_json", length = 512)
  private String publicationWindows; // List<TimeRangeBean>

  @Column(name = "reason", length = 64)
  private String reason;

  @Column(name = "summaries_json", length = 512)
  private String summaries; // List<NaturalLanguageStringBean>

  @Column(name = "descriptions_json", length = 512)
  private String descriptions; // List<NaturalLanguageStringBean>

  @Column(name = "urls_json", length = 512)
  private String urls; // List<NaturalLanguageStringBean>

  @Column(name = "all_affects_json", length = 512)
  private String allAffects; // List<SituationAffectsBean>

  @Column(name = "consequences_json", length = 512)
  private String consequences; // List<SituationConsequenceBean>

  @Column(name = "severity", length = 64)
  private String severity; // ESeverity

  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "updated_at")
  private Date updatedAt;

  @Column(name = "deleted")
  private boolean deleted;

  transient static private GsonBuilder gbuilder;

  public String getServiceAlertId() {
    return serviceAlertId;
  }

  public ServiceAlertRecord() {
    super();
    gbuilder = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES);
  }

  public ServiceAlertRecord(ServiceAlertBean bean) {
    this();
    updateFrom(bean);
  }

  public ServiceAlertRecord updateFrom(ServiceAlertBean bean) {
    if (bean == null)
      return this;
    this.serviceAlertId = bean.getId();
    this.creationTime = bean.getCreationTime();
    this.activeWindows = toJson(bean.getActiveWindows());
    this.publicationWindows = toJson(bean.getPublicationWindows());
    this.reason = bean.getReason();
    this.summaries = toJson(bean.getSummaries());
    this.descriptions = toJson(bean.getDescriptions());
    this.urls = toJson(bean.getUrls());
    this.allAffects = toJson(bean.getAllAffects());
    this.consequences = toJson(bean.getConsequences());
    this.severity = toJson(bean.getSeverity());
    return this;
  }
  

  @SuppressWarnings("unchecked")
  public static ServiceAlertBean toBean(ServiceAlertRecord o) {
    if (o == null)
      return null;

    ServiceAlertBean bean = new ServiceAlertBean();
    Gson gsonCreator = gbuilder.create();

    bean.setId(o.getServiceAlertId());
    
    bean.setCreationTime(o.getCreationTime());

    bean.setActiveWindows((List<TimeRangeBean>) gsonCreator.fromJson(
        o.getActiveWindows(), listTimeRangeBeanType));
    
    bean.setPublicationWindows((List<TimeRangeBean>) gsonCreator.fromJson(
        o.getPublicationWindows(), listTimeRangeBeanType));
    
    bean.setReason(o.getReason());
    
    bean.setSummaries((List<NaturalLanguageStringBean>) gsonCreator.fromJson(
        o.getSummaries(), listNaturalLanguageStringBeanType));
    
    bean.setDescriptions((List<NaturalLanguageStringBean>) gsonCreator.fromJson(
        o.getDescriptions(), listNaturalLanguageStringBeanType));
    
    bean.setUrls((List<NaturalLanguageStringBean>) gsonCreator.fromJson(
        o.getUrls(), listNaturalLanguageStringBeanType));

    bean.setAllAffects((List<SituationAffectsBean>) gsonCreator.fromJson(
        o.getAllAffects(), listSituationAffectsBeanType));
    
    bean.setConsequences((List<SituationConsequenceBean>) gsonCreator.fromJson(
        o.getConsequences(), listSituationConsequenceBeanType));
    
    bean.setSeverity((ESeverity) gsonCreator.fromJson(o.getSeverity(), ESeverity.class));
    return bean;
  }

  
  String toJson(Object object) {
    return gbuilder.create().toJson(object);
  }


  // These 2 annotations are NOT working at the moment.  See https://issuetracker.camsys.com/browse/OBANYC-589
  @PrePersist
  protected void onCreate() {
    createdAt = updatedAt = new Date();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = new Date();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  public String getActiveWindows() {
    return activeWindows;
  }

  public void setActiveWindows(String activeWindows) {
    this.activeWindows = activeWindows;
  }

  public String getPublicationWindows() {
    return publicationWindows;
  }

  public void setPublicationWindows(String publicationWindows) {
    this.publicationWindows = publicationWindows;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getSummaries() {
    return summaries;
  }

  public void setSummaries(String summaries) {
    this.summaries = summaries;
  }

  public String getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(String descriptions) {
    this.descriptions = descriptions;
  }

  public String getUrls() {
    return urls;
  }

  public void setUrls(String urls) {
    this.urls = urls;
  }

  public String getAllAffects() {
    return allAffects;
  }

  public void setAllAffects(String allAffects) {
    this.allAffects = allAffects;
  }

  public String getConsequences() {
    return consequences;
  }

  public void setConsequences(String consequences) {
    this.consequences = consequences;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public void setServiceAlertId(String serviceAlertId) {
    this.serviceAlertId = serviceAlertId;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

}
