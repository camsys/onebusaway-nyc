package org.onebusaway.nyc.util.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "obanyc_psas")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class PublicServiceAnnouncement {

  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  private Long id;
  
  @Column(nullable = false, name = "text", length=1024)
  private String text = "";
  
  public Long getId() {
    return id;
  }
  
  public void setId(Long id) {
    this.id = id;
  }
  
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
