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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;
import org.onebusaway.nyc.report_archive.services.CancelledTripDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
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

    //TODO: 1/31/22
    @Transactional
    @Override
    public List<NycCancelledTripRecord> getReports(){
        Query query = getSession().createQuery("FROM NycCancelledTripRecord");
        return query.list();
    }
}
