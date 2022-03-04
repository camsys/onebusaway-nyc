/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.report_archive.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Entity
@Table(name = "obanyc_cancelledtrip",
        indexes = {
                @Index(name = "timestamp_index", columnList = "timestamp"),
                @Index(name = "service_date_index", columnList = "serviceDate"),
                @Index(name = "trip_index", columnList = "trip"),
                @Index(name = "block_index", columnList = "block")
})
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NycCancelledTripRecord implements Serializable {

    private static final long serialVersionUID = 2L;

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="native")
    @GenericGenerator(name = "native", strategy = "native")
    @AccessType("property")
    private Long id;

    @Column(nullable = false, name = "record_timestamp")
    private long recordTimeStamp;

    @Column(name = "block")
    private String block;

    @Column(nullable = false, name = "trip")
    private String trip;

    @Column(nullable = false, name = "status")
    private String status;

    @Column(nullable = false, name = "timestamp")
    private Date timestamp;

    @Column(name = "scheduledPullOut")
    private String scheduledPullOut;

    @Column(name = "serviceDate")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    private LocalDate serviceDate;

    @Column(name = "route")
    private String route;

    @Column(name = "routeId")
    private String routeId;

    @Column(name = "firstStopId")
    private String firstStopId;

    @Column(name = "firstStopDepartureTime")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="HH:mm:ss")
    private LocalTime firstStopDepartureTime;

    @Column(name = "lastStopArrivalTime")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="HH:mm:ss")
    private LocalTime lastStopArrivalTime;

    public NycCancelledTripRecord(){}

    public NycCancelledTripRecord(CancelledTripBean nycCancelledTripBean, long recordTimeStamp){
        setBlock(nycCancelledTripBean.getBlock());
        setTrip(nycCancelledTripBean.getTrip());
        setStatus(nycCancelledTripBean.getStatus());
        setRoute(nycCancelledTripBean.getRoute());
        setRouteId(nycCancelledTripBean.getRouteId());
        setFirstStopId(nycCancelledTripBean.getFirstStopId());
        setServiceDate(LocalDate.parse(nycCancelledTripBean.getServiceDate()));
        setFirstStopDepartureTime(LocalTime.parse(nycCancelledTripBean.getFirstStopDepartureTime()));
        setLastStopArrivalTime(LocalTime.parse(nycCancelledTripBean.getLastStopArrivalTime()));
        setRecordTimeStamp(recordTimeStamp);

        if(nycCancelledTripBean.getTimestamp() > 0){
            setTimestamp(new Date(nycCancelledTripBean.getTimestamp()));
        }
        if(nycCancelledTripBean.getScheduledPullOut() != null){
            setScheduledPullOut(nycCancelledTripBean.getScheduledPullOut());
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getRecordTimeStamp() {
        return recordTimeStamp;
    }

    public void setRecordTimeStamp(long recordTimeStamp) {
        this.recordTimeStamp = recordTimeStamp;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getTrip() {
        return trip;
    }

    public void setTrip(String trip) {
        this.trip = trip;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getScheduledPullOut() {
        return scheduledPullOut;
    }

    public void setScheduledPullOut(String scheduledPullOut) {
        this.scheduledPullOut = scheduledPullOut;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getFirstStopId() {
        return firstStopId;
    }

    public void setFirstStopId(String firstStopId) {
        this.firstStopId = firstStopId;
    }

    public LocalTime getFirstStopDepartureTime() {
        return firstStopDepartureTime;
    }

    public void setFirstStopDepartureTime(LocalTime firstStopDepartureTime) {
        this.firstStopDepartureTime = firstStopDepartureTime;
    }

    public LocalTime getLastStopArrivalTime() {
        return lastStopArrivalTime;
    }

    public void setLastStopArrivalTime(LocalTime lastStopArrivalTime) {
        this.lastStopArrivalTime = lastStopArrivalTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NycCancelledTripRecord record = (NycCancelledTripRecord) o;
        return recordTimeStamp == record.recordTimeStamp &&
                Objects.equal(id, record.id) &&
                Objects.equal(block, record.block) &&
                Objects.equal(trip, record.trip) &&
                Objects.equal(status, record.status) &&
                Objects.equal(timestamp, record.timestamp) &&
                Objects.equal(scheduledPullOut, record.scheduledPullOut) &&
                Objects.equal(serviceDate, record.serviceDate) &&
                Objects.equal(route, record.route) &&
                Objects.equal(routeId, record.routeId) &&
                Objects.equal(firstStopId, record.firstStopId) &&
                Objects.equal(firstStopDepartureTime, record.firstStopDepartureTime) &&
                Objects.equal(lastStopArrivalTime, record.lastStopArrivalTime);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, recordTimeStamp, block, trip, status, timestamp, scheduledPullOut, serviceDate, route, routeId, firstStopId, firstStopDepartureTime, lastStopArrivalTime);
    }

    @Override
    public String toString() {
        return "NycCancelledTripRecord{" +
                "id=" + id +
                ", recordTimeStamp=" + recordTimeStamp +
                ", block='" + block + '\'' +
                ", trip='" + trip + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", scheduledPullOut='" + scheduledPullOut + '\'' +
                ", serviceDate=" + serviceDate +
                ", route='" + route + '\'' +
                ", routeId='" + routeId + '\'' +
                ", firstStopId='" + firstStopId + '\'' +
                ", firstStopDepartureTime=" + firstStopDepartureTime +
                ", lastStopArrivalTime=" + lastStopArrivalTime +
                '}';
    }
}
