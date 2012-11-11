package org.onebusaway.nyc.transit_data_manager.logging;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "obanyc_systemlog")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class SystemLogRecord implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(generator = "increment")
	@GenericGenerator(name = "increment", strategy= "increment")
	@AccessType("property")
	private Long id;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "message_date", nullable = false)
	private Date messageDate;
	
	@Column(name = "message", nullable = false)
	private String message;
	
	@Column(name = "component", length = 32)
	private String component;
	
	@Column(name = "priority", length = 1)
	private Integer priority;

	//No-arg constructor required by Hibernate
	public SystemLogRecord() {
		
	}
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the messageDate
	 */
	public Date getMessageDate() {
		return messageDate;
	}

	/**
	 * @param messageDate the messageDate to set
	 */
	public void setMessageDate(Date messageDate) {
		this.messageDate = messageDate;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the component
	 */
	public String getComponent() {
		return component;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent(String component) {
		this.component = component;
	}

	/**
	 * @return the priority
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * @param priority the priority to set
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	
}
