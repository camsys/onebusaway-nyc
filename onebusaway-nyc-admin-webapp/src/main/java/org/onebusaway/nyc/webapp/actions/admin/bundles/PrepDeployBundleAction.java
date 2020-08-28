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

package org.onebusaway.nyc.webapp.actions.admin.bundles;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Action class used by Transit Data Bundle Utility to prepare S3 files for deployment.
 *
 */
@Namespace(value="/admin/bundles")
@Results({
        @Result(name="bundleModifiedDate", type="json",
                params={"root", "bundleDate"}),
        @Result(name="prepDeploymentCompletionMessage", type="json",
                params={"root", "deploymentPrepBundleDetailsMessages"})
})
public class PrepDeployBundleAction extends OneBusAwayNYCAdminActionSupport {
    private static Logger _log = LoggerFactory.getLogger(PrepDeployBundleAction.class);
    private int dataset_build_id;
    private String datasetName;
    private String buildName;
    private FileService fileService;
    private String bundleDate;
    private String s3Path;
    private List<String> deploymentPrepBundleDetailsMessages = new ArrayList<>();



    @Autowired
    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public void setS3Path(String s3Path){this.s3Path = s3Path;}

    public String getBundleName(){
        return buildName + ".tar.gz";
    }

    public String getBundleDate() {
        return bundleDate;
    }

    public String getBundlePath() {
        return datasetName +"/"+fileService.getBuildPath() + "/" + buildName + "/" + getBundleName();
    }

    public List<String> getDeploymentPrepBundleDetailsMessages(){
        return deploymentPrepBundleDetailsMessages;
    }

    public String requestBundleModifiedDate(){
        _log.info("existingBuildList called for path=" + fileService.getBucketName()+"/"+ getBundlePath());
        List<Map<String,String>> existingFiles = fileService.listObjectsTabular( getBundlePath(), 150);
        int numberOfFiles = existingFiles.size();
        if(numberOfFiles!=1){
            throw new RuntimeException("PrepDeployBundleAction looked for " + getBundlePath() + " and found " +
                    existingFiles.size() + " results. PrepDeployBundleAction requires 1 result");
        }
        Map<String,String> deploymentPrepBundle= getDeploymentPrepBundleDetails();
        bundleDate = deploymentPrepBundle.get("lastModified");

        return "bundleModifiedDate";
    }

    public String copyBundleToDeployLocation(){
        Map<String,String> deploymentPrepBundle= getDeploymentPrepBundleDetails();
        String bundleLocation = deploymentPrepBundle.get("key");
        s3Path = (s3Path.substring(0,5).equals("s3://")) ? s3Path.substring(5) : s3Path;
        String[] s3PathParts = s3Path.split("/");
        String s3Bucket = s3PathParts[0];
        String s3Dir = s3Path.substring(s3Bucket.length()+1);

        List<Map<String,String>> objectsInActive = fileService.listObjectsTabular( s3Dir, 150);
        Map<String,String> mostRecentObject = objectsInActive.iterator().next();
        for (Map<String,String> objectInActive : objectsInActive){
            Date mostRecentObjectDate = new Date(mostRecentObject.get("lastModified"));
            Date objectInActiveDate = new Date(objectInActive.get("lastModified"));
            if(objectInActiveDate.after(mostRecentObjectDate)){
                deploymentPrepBundleDetailsMessages.add("Deleating " + mostRecentObject.get("key"));
                fileService.deleteObject(mostRecentObject.get("key"));
                mostRecentObject = objectInActive;
            } else if(objectInActiveDate.before(mostRecentObjectDate)){
                deploymentPrepBundleDetailsMessages.add("Deleating " + objectInActive.get("key"));
                fileService.deleteObject(objectInActive.get("key"));
            }
        }


        fileService.copyS3Object(bundleLocation, s3Dir+getBundleName());
        deploymentPrepBundleDetailsMessages.add("Moved " + bundleLocation + " to " + s3Dir+getBundleName());

        return "prepDeploymentCompletionMessage";
    }



    private Map<String,String> getDeploymentPrepBundleDetails(){
        _log.info("existingBuildList called for path=" + fileService.getBucketName()+"/"+ getBundlePath());
        List<Map<String,String>> existingFiles = fileService.listObjectsTabular( getBundlePath(), 150);
        int numberOfFiles = existingFiles.size();
        if(numberOfFiles!=1){
            throw new RuntimeException("PrepDeployBundleAction looked for " + getBundlePath() + " and found " +
                    existingFiles.size() + " results. PrepDeployBundleAction requires 1 result");
        }
        return existingFiles.iterator().next();
    }

}
