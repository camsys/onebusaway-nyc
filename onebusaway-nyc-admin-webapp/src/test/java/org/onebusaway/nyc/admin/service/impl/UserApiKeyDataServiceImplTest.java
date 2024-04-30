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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.admin.model.UserApiKeyData;
import org.onebusaway.nyc.admin.service.UserApiKeyDataService;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UserApiKeyDataServiceImplTest {

    private SessionFactory _sessionFactory;
    private UserApiKeyDataService _service;

    @Before
    public void setup() throws IOException {
        Configuration config = new Configuration();
        config = config.configure("org/onebusaway/nyc/admin/hibernate-configuration.xml");
        _sessionFactory = config.buildSessionFactory();

        _service = new UserApiKeyDataServiceImpl();
        _service.setSesssionFactory(_sessionFactory);
    }

    @After
    public void teardown() {
        if (_sessionFactory != null)
            _sessionFactory.close();
    }

    private Session getSession(){
        return _sessionFactory.getCurrentSession();
    }

    @Test
    public void getAllUserApiKeyDataTest(){
        Transaction t = getSession().beginTransaction();
        UserApiKeyData data = new UserApiKeyData();
        data.setName("test user");
        data.setEmail("testemail@email.com");
        data.setProjectName("test project name");
        data.setProjectUrl("http://bustime.mta.info");
        data.setPlatform("web, andriod, ios, amazon echo, other");
        data.setApiKey("API-KEY-HERE");
        _service.saveOrUpdateApiKeyData(data);

        List<UserApiKeyData> allData = _service.getAllUserApiKeyData();
        assertEquals(1, allData.size());

        UserApiKeyData dbData =  allData.get(0);
        assertEquals(new Long(1), data.getId());
        assertEquals(data.getName(), dbData.getName());
        assertEquals(data.getEmail(), dbData.getEmail());
        assertEquals(data.getProjectName(), dbData.getProjectName());
        assertEquals(data.getProjectUrl(), dbData.getProjectUrl());
        assertEquals(data.getPlatform(), dbData.getPlatform());
        assertEquals(data.getApiKey(), dbData.getApiKey());
        t.commit();

    }

    @Test
    public void getAllUserApiKeyDataPagingTest(){
        Transaction t = getSession().beginTransaction();
        for(int i=1; i < 16; i++){
            UserApiKeyData data = new UserApiKeyData();
            data.setName(String.format("test user %s", i));
            data.setEmail(String.format("testemail%s@email.com", i));
            data.setProjectName(String.format("test project name %s",1));
            data.setProjectUrl("http://bustime.mta.info");
            data.setPlatform("web, andriod, ios, amazon echo, other");
            data.setApiKey(String.format("API-KEY-HERE-%s", 1));
            _service.saveOrUpdateApiKeyData(data);
        }

        List<UserApiKeyData> allData = _service.getAllUserApiKeyData();
        assertEquals(15, allData.size());

        // test max results
        List<UserApiKeyData> pagedData = _service.getAllUserApiKeyDataPaged(0, 10);
        assertEquals(10, pagedData.size());
        assertEquals("test user 1", pagedData.get(0).getName());
        assertEquals("test user 10", pagedData.get(9).getName());

        // test first result offset by 1
        pagedData = _service.getAllUserApiKeyDataPaged(1, 10);
        assertEquals("test user 2", pagedData.get(0).getName());
        assertEquals("test user 11", pagedData.get(9).getName());

        // test first result null
        pagedData = _service.getAllUserApiKeyDataPaged(null, 5);
        assertEquals(5, pagedData.size());
        assertEquals("test user 1", pagedData.get(0).getName());

        // test first result and max results null
        pagedData = _service.getAllUserApiKeyDataPaged(null, null);
        assertEquals(15, pagedData.size());

        // test offset greater than size
        pagedData = _service.getAllUserApiKeyDataPaged(16, 10);
        assertEquals(0, pagedData.size());

        // test maxResults greater than size
        pagedData = _service.getAllUserApiKeyDataPaged(0, 16);
        assertEquals(15, pagedData.size());

        t.commit();
    }


}
