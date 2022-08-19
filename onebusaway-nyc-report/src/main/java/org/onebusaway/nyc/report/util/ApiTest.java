package org.onebusaway.nyc.report.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;


@Provider
@PreMatching
public class ApiTest  implements ContainerRequestFilter {


        @Override
        public void filter(ContainerRequestContext ctx) throws IOException {
            if (ctx.getLanguage() != null && "EN".equals(ctx.getLanguage()
                    .getLanguage())) {

                ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                        .entity("Cannot access")
                        .build());
            }
        }
//

}
