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

package org.onebusaway.nyc.admin.service.impl;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.onebusaway.nyc.admin.model.UserApiKeyData;
import org.onebusaway.nyc.admin.service.UserApiKeyDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component
public class UserApiKeyDataServiceImpl implements UserApiKeyDataService {

    private SessionFactory _sessionFactory;

    private static final Logger _log = LoggerFactory.getLogger(UserApiKeyDataServiceImpl.class);

    @Override
    @Autowired
    public void setSesssionFactory(SessionFactory sessionFactory) {
        _sessionFactory = sessionFactory;
    }

    private Session getSession(){
        return _sessionFactory.getCurrentSession();
    }


    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void saveOrUpdateApiKeyData(UserApiKeyData data){
        if(data.getCreated() == null){
            data.setCreated(new Date());
        }
        getSession().saveOrUpdate(data);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void deleteUserApiKeyData(UserApiKeyData data) {
        getSession().delete(data);
        getSession().flush();
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserApiKeyData> getAllUserApiKeyData() {
        Query query = getSession().createQuery("from UserApiKeyData");
        return (List<UserApiKeyData>) query.list();
    }

    @Override
    @Transactional(readOnly=true)
    public List<UserApiKeyData> getAllUserApiKeyDataPaged(Integer firstResult, Integer maxResults) {
        String hql = "from UserApiKeyData " +
                     "order by created";
        firstResult = firstResult == null ? 0 : firstResult;
        maxResults = maxResults == null ? Integer.MAX_VALUE : maxResults;
        Query query = getSession().createQuery(hql).setFirstResult(firstResult).setMaxResults(maxResults);
        _log.debug("Returning a maximum of {} keys starting from {}", maxResults, firstResult);
        return query.list();
    }
}
