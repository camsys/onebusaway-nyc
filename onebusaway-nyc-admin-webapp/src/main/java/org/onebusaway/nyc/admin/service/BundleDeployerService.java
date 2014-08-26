package org.onebusaway.nyc.admin.service;

import javax.ws.rs.core.Response;

public interface BundleDeployerService {

    public Response list(String environment);

    public Response deploy(String environment);

    public Response deployStatus(String id);

    public Response getBundleList();
    
    public Response getBundleFile(String bundleId, String relativeFilename);

}
