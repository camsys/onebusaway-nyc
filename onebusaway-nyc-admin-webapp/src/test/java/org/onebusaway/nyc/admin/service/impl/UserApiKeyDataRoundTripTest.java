/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.admin.model.UserApiKeyData;
import org.onebusaway.nyc.admin.service.UserApiKeyDataService;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

/**
 * Characterization test: saves one fully populated {@link UserApiKeyData} through
 * {@link UserApiKeyDataServiceImpl} into in-memory HSQLDB, clears the session, reloads
 * it by id and through the service query, and checks every mapped column.
 *
 * Why: this pins how the admin webapp's only Hibernate entity round-trips through
 * Hibernate 5.2 on Java 11 today, so the later Hibernate 5.6 / Java 21 upgrade has a
 * baseline. Covered: the native (identity) id, the @Temporal(TIMESTAMP) created column
 * with millis, the fact that the entity's JPA @PrePersist callback does not fire on a
 * native Session save, non-ASCII text and a null nullable column. The entity has no BigDecimal,
 * boolean or enum fields, so those are not exercised here.
 */
public class UserApiKeyDataRoundTripTest {

    // Fixed instant with non-zero millis so a precision loss shows up.
    private static final long FIXED_MILLIS = 1793112912345L;

    private static final String NON_ASCII = "Caf\u00e9 \u00d1and\u00fa";

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
    public void userApiKeyDataRoundTrips() {
        Transaction t = getSession().beginTransaction();

        UserApiKeyData saved = new UserApiKeyData();
        saved.setName(NON_ASCII);
        saved.setEmail("caf\u00e9@example.com");
        saved.setProjectName(NON_ASCII + " Bus App");
        saved.setProjectUrl("https://bustime.mta.info/?q=caf\u00e9");
        saved.setPlatform("web, android, ios");
        saved.setApiKey("8f14e45f-ceea-467a-9575-2b6f1b8e0d11");
        saved.setCreated(new Date(FIXED_MILLIS));
        // Nullable column left null on purpose.
        saved.setComment(null);

        _service.saveOrUpdateApiKeyData(saved);
        Long id = saved.getId();
        assertNotNull(id);

        // The entity has a JPA @PrePersist (onCreate) that resets created to "now". With
        // a native Session save on Hibernate 5.2 it does NOT fire, so our fixed value
        // survives. If a newer Hibernate starts firing JPA callbacks on native saves,
        // this line fails and created will silently become the insert time in prod.
        Date createdAfterSave = saved.getCreated();
        assertEquals(FIXED_MILLIS, createdAfterSave.getTime());

        // Service does not flush or clear, so do it here to force a real reload.
        getSession().flush();
        getSession().clear();

        UserApiKeyData reloaded = (UserApiKeyData) getSession().get(UserApiKeyData.class, id);
        assertNotNull(reloaded);
        assertNotSame(saved, reloaded);
        assertFields(id, createdAfterSave, reloaded);

        // Same row through the service's own HQL query.
        getSession().clear();
        List<UserApiKeyData> all = _service.getAllUserApiKeyData();
        assertEquals(1, all.size());
        assertFields(id, createdAfterSave, all.get(0));

        t.commit();
    }

    private void assertFields(Long id, Date expectedCreated, UserApiKeyData actual) {
        assertEquals(id, actual.getId());
        assertEquals(NON_ASCII, actual.getName());
        assertEquals("caf\u00e9@example.com", actual.getEmail());
        assertEquals(NON_ASCII + " Bus App", actual.getProjectName());
        assertEquals("https://bustime.mta.info/?q=caf\u00e9", actual.getProjectUrl());
        assertEquals("web, android, ios", actual.getPlatform());
        assertEquals("8f14e45f-ceea-467a-9575-2b6f1b8e0d11", actual.getApiKey());
        assertNull(actual.getComment());
        assertNotNull(actual.getCreated());
        // @Temporal(TIMESTAMP) comes back as java.sql.Timestamp with millis intact.
        assertEquals(Timestamp.class, actual.getCreated().getClass());
        assertEquals(expectedCreated.getTime(), actual.getCreated().getTime());
    }
}
