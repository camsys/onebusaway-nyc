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

package org.onebusaway.nyc.report.model;

import org.hibernate.annotations.*;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Index;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "obanyc_invalidlocationreport")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
/**
 * Persist raw TCIP-JSON that failed marshalling/persistence along
 * with the exception generated.
 */
public class InvalidLocationRecord implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy= GenerationType.AUTO, generator="native")
  @GenericGenerator(name = "native", strategy = "native")
  @AccessType("property")
  private Integer id;

  @Column(nullable = true, name = "time_received")
  @Index(name = "time_received")
  private Date timeReceived;

  @Column(nullable = false, name = "archive_time_received")
  @Index(name = "archive_time_received")
  private Date archiveTimeReceived;

  @Column(nullable = false, name = "raw_message", length = 1400)
  private String rawMessage;

  @Column(nullable = false, name = "exception_message", length = 1400)
  private String exceptionMessage;

  public InvalidLocationRecord(String message, Throwable throwable,
      Date timeReceived) {
    setArchiveTimeReceived(new Date());
    setTimeReceived(timeReceived);
    if (message != null && message.length() > 1400) {
      message = message.substring(0, 1400);
    }
    setRawMessage(message);
    String error = null;
    if (throwable != null) {
      error = throwable.toString();
    }
    if (error != null && error.length() > 1400) {
      error = error.substring(0, 1400);
    }
    setExceptionMessage(error);
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getTimeReceived() {
    return timeReceived;
  }

  public void setTimeReceived(Date timeReceived) {
    this.timeReceived = timeReceived;
  }

  public Date getArchiveTimeReceived() {
    return archiveTimeReceived;
  }

  public void setArchiveTimeReceived(Date archiveTimeReceived) {
    this.archiveTimeReceived = archiveTimeReceived;
  }

  public String getRawMessage() {
    return rawMessage;
  }

  public void setRawMessage(String rawMessage) {
    this.rawMessage = rawMessage;
  }

  public String getExceptionMessage() {
    return exceptionMessage;
  }

  public void setExceptionMessage(String exceptionMessage) {
    this.exceptionMessage = exceptionMessage;
  }

}