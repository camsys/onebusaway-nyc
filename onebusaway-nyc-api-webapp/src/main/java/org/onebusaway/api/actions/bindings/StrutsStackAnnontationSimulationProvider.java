package org.onebusaway.api.actions.bindings;


import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

//@Provider
public class StrutsStackAnnontationSimulationProvider implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        return;
    }
}