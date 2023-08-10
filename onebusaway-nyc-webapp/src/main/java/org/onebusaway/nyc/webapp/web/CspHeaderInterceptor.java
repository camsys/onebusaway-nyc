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

package org.onebusaway.nyc.webapp.web;

import java.security.SecureRandom;
import java.util.Base64;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import com.opensymphony.xwork2.util.ValueStack;

import javax.servlet.http.HttpServletResponse;

import static org.apache.struts2.StrutsStatics.HTTP_RESPONSE;

public class CspHeaderInterceptor implements Interceptor {

    @Override
    public void init() {}

    @Override
    public void destroy() {}

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        byte[] nonceBytes = new byte[16];
        new SecureRandom().nextBytes(nonceBytes);
        String nonce = Base64.getEncoder().encodeToString(nonceBytes);
        HttpServletResponse response = (HttpServletResponse) invocation.getInvocationContext().get(HTTP_RESPONSE);

        // Store the nonce value in the ActionContext
        ActionContext.getContext().put("cspNonce", nonce);

        String policy = "script-src 'unsafe-inline' https://*.googleapis.com https://*.google-analytics.com https://translate.google.com  https: http:; " +
                "object-src 'none';" +
                "img-src 'self' https://*.googleapis.com https://*.gstatic.com *.google.com *.googleusercontent.com *.google-analytics.com *.obanyc.com data:;" +
                "frame-src *.google.com *.youtube.com;" +
                "connect-src 'self' https://*.googleapis.com *.google.com https://*.gstatic.com; " +
                "font-src https://fonts.gstatic.com; " +
                "style-src 'unsafe-inline' 'self' https://fonts.googleapis.com https://*.gstatic.com; " +
                "worker-src;";
        policy = String.format(policy, nonce);
        response.setHeader("Content-Security-Policy", policy);
        return invocation.invoke();
    }

}