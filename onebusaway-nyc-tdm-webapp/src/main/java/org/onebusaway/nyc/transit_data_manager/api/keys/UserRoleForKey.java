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

package org.onebusaway.nyc.transit_data_manager.api.keys;

import org.onebusaway.users.model.UserRole;
import org.onebusaway.users.services.StandardAuthoritiesService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum UserRoleForKey {
    OPS("OPS") {
        public String getRoleName(){
            return StandardAuthoritiesService.OPS_API;
        }
        public UserRole getRole(StandardAuthoritiesService _authServ){
            return _authServ.getOpsApiRole();
        }
    };

    private static final Map<String, UserRoleForKey> BY_LABEL = new HashMap<>();

    private String[] labels;

    static {
        for (UserRoleForKey role: values()) {
            for(String label : role.labels){
                BY_LABEL.put(label, role);
            }
        }
    }

    private UserRoleForKey(String... labels) {
        this.labels = labels;
    }

    public static UserRoleForKey valueOfLabel(String label) {
        UserRoleForKey userRoleForKey = BY_LABEL.get(label.toUpperCase());
        if(userRoleForKey == null){
            throw new IllegalArgumentException("No enum constant with label " + label);
        }
        return userRoleForKey;
    }

    public abstract String getRoleName();

    public abstract UserRole getRole(StandardAuthoritiesService _authServ);

    @Override
    public String toString() {
        return Arrays.toString(this.labels);
    }
}
