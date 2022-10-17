/**
 * Copyright (C) 2022 Cambridge Systematics
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

package org.onebusaway.nyc.api.lib.impl;



import org.onebusaway.nyc.api.lib.services.ApiKeyWithRolesPermissionService;
import org.onebusaway.users.client.model.UserBean;
import org.onebusaway.users.model.User;
import org.onebusaway.users.model.UserIndex;
import org.onebusaway.users.model.UserIndexKey;
import org.onebusaway.users.services.ApiKeyPermissionService;
import org.onebusaway.users.services.StandardAuthoritiesService;
import org.onebusaway.users.services.UserIndexTypes;
import org.onebusaway.users.services.UserService;
import org.onebusaway.util.SystemTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;


public class ApiKeyWithRolesPermissionServiceImpl implements ApiKeyWithRolesPermissionService {

    private HashMap<String, Long> _lastVisitForUser;
    private UserService _userService;
    private StandardAuthoritiesService _authoritiesService;

    @Autowired
    public void setUserService(UserService userService) {
        _userService = userService;
    }

    @Autowired
    public void setAuthoritiesService(
            StandardAuthoritiesService authoritiesService) {
        _authoritiesService = authoritiesService;
    }

    public ApiKeyWithRolesPermissionServiceImpl() {
        _lastVisitForUser = new HashMap<String, Long>();

    }

    @Override
    public Status getPermission(String key, String service) {

        Long minRequestInterval = _userService.getMinApiRequestIntervalForKey(key,false);
        if (minRequestInterval == null) {
            return Status.UNAUTHORIZED;
        }

        long now = SystemTime.currentTimeMillis();
        Long lastVisit = _lastVisitForUser.get(key);

        Status ok = Status.RATE_EXCEEDED;

        if (lastVisit == null || lastVisit + minRequestInterval <= now) {
            ok = Status.AUTHORIZED;
        }

        _lastVisitForUser.put(key, now);
        return ok;
    }

    public Status getRoleOnlyPermission(String key, String service, String roleToMatch) {
        Status ok = getPermission(key, service);
        if (ok == Status.AUTHORIZED) {
            ok = Status.UNAUTHORIZED;

            UserIndexKey indexKey = new UserIndexKey(UserIndexTypes.API_KEY, key);
            UserIndex userIndex = _userService.getUserIndexForId(indexKey);

            if (userIndex != null) {
                User user = userIndex.getUser();
                if (user.getRoles().stream().anyMatch(
                        role -> role.getName().equals(roleToMatch))) {
                    ok = Status.AUTHORIZED;
                }
            }
        }
        return ok;
    }

    @Override
    public Status getAdminOnlyPermission(String key, String service) {
        return getRoleOnlyPermission(key,service,_authoritiesService.getAdministratorRole().getName());
    }

    @Override
    public Status getUserOnlyPermission(String key, String service) {
        return getRoleOnlyPermission(key,service,_authoritiesService.getUserRole().getName());
    }

    @Override
    public Status getOperatorOnlyPermission(String key, String service) {
        return getRoleOnlyPermission(key,service,_authoritiesService.getReportingRole().getName());
    }

    @Override
    public Status getOpsApiOnlyPermission(String key, String service) {
        return getRoleOnlyPermission(key,service,_authoritiesService.getOpsApiRole().getName());
    }


}
