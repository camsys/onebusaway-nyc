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

package org.onebusaway.nyc.report_archive.impl;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.api.HistoricalCancelledTripQuery;
import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;
import org.onebusaway.nyc.report_archive.services.CancelledTripDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class CancelledTripDaoImpl implements CancelledTripDao {
    protected static Logger _log = LoggerFactory.getLogger(CancelledTripDaoImpl.class);
    private SessionFactory _sessionFactory;

    @Autowired
    @Qualifier("sessionFactory")
    public void setSessionFactory(SessionFactory sessionFactory) {
        _sessionFactory = sessionFactory;
    }

    public Session getSession() {
        return _sessionFactory.getCurrentSession();
    }

    @Transactional(value="transactionManager", rollbackFor = Throwable.class)
    @Override
    public void saveReport(NycCancelledTripRecord cancelledTripRecord) {
        getSession().persist(cancelledTripRecord);
        getSession().flush();
        getSession().clear();
    }

    @Transactional(value="transactionManager", rollbackFor = Throwable.class)
    @Override
    public void saveReports(NycCancelledTripRecord... cancelledTripRecords) {
        for (NycCancelledTripRecord cancelledTripRecord : cancelledTripRecords) {
            getSession().persist(cancelledTripRecord);
        }
        getSession().flush();
        getSession().clear();
    }

    @Transactional
    @Override
    public List<NycCancelledTripRecord> getReports(){
        Query query = getSession().createQuery("from NycCancelledTripRecord");
        return query.list();
    }
    @Transactional
    @Override
    public List<NycCancelledTripRecord> getReports(HistoricalCancelledTripQuery hctQuery) throws java.text.ParseException {

        LocalDate serviceDate = hctQuery.getRequestedDate();
        String trip = hctQuery.getRequestedTrip();
        String block = hctQuery.getRequestedBlock();
        Integer numberOfRecords = hctQuery.getNumberOfRecords();
        LocalTime startTime = hctQuery.getStartTime();
        LocalTime endTime = hctQuery.getEndTime();

        String hql = "FROM NycCancelledTripRecord r where r.serviceDate = :sd";

        if (trip != null){
            hql += " and r.trip = :t";
        }
        if (block != null){
            hql += " and r.block = :b";
        }
        if(startTime != null){
            try {
                hql += " and r.timestamp >= :startTime";
            } catch (Exception e){
                _log.error("Unable to parse start time {}", startTime, e);
            }
        }
        if(endTime != null){
            try {
                hql += " and r.timestamp >= :endTime";
            } catch (Exception e){
                _log.error("Unable to parse end time {}", endTime, e);
            }
        }


        hql += " ORDER BY r.timestamp DESC, r.id DESC";

        Query q = getSession().createQuery(hql);

        q.setParameter("sd", serviceDate);

        if (trip != null){
            q.setParameter("t", trip);
        }
        if (block != null){
            q.setParameter("b", block);
        }
        if(startTime != null){
            q.setParameter("startTime", LocalDateTime.of(serviceDate, startTime));
        }
        if(endTime != null){
            q.setParameter("endTime", LocalDateTime.of(serviceDate, endTime));
        }

        q.setMaxResults(numberOfRecords);

        return q.list();

    }


}
