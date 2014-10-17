package org.onebusaway.nyc.vehicle_tracking.impl.aws;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.compress.utils.IOUtils;
import org.onebusaway.nyc.vehicle_tracking.services.aws.AwsMetadataService;
import org.springframework.stereotype.Component;

@Component
public class AwsMetadataServiceImpl implements AwsMetadataService {
	
	private static final String METADATA_URL = "http://169.254.169.254/latest/meta-data/";
	private static final String INSTANCE_ID_URL = METADATA_URL + "instance-id";
	private static final String INSTANCE_TYPE_URL = METADATA_URL + "instance-type";
	private static final String AMI_ID_URL = METADATA_URL + "ami-id";
	private static final String PUBLIC_HOSTNAME_URL = METADATA_URL + "public-hostname";
	private static final String INTERNAL_HOSTNAME_URL = METADATA_URL + "hostname";
	
	@Override
	public String getInstanceId() {
		return slurp(INSTANCE_ID_URL);
	}

	@Override
	public String getInstanceType() {
		return slurp(INSTANCE_TYPE_URL);
	}

	@Override
	public String getAmiId() {
		return slurp(AMI_ID_URL);
	}

	@Override
	public String getPublicHostname() {
		return slurp(PUBLIC_HOSTNAME_URL);
	}

	@Override
	public String getInternalHostname() {
		return slurp(INTERNAL_HOSTNAME_URL);
	}
	
	private String slurp(String urlString) {
	    URL url;
	    InputStream is = null;
	    BufferedInputStream bis = null;
	    ByteArrayOutputStream baos = null;

	    try {
	      url = new URL(urlString);
	      is = url.openStream();
	      bis = new BufferedInputStream(is);
	      baos = new ByteArrayOutputStream();
	      IOUtils.copy(bis, baos);
	    } catch (Exception any) {
	      return any.toString();
	    } finally {
	      if (bis != null)
	        try {
	          bis.close();
	        } catch (Exception e1) {
	        }
	      if (baos != null)
	        try {
	          baos.close();
	        } catch (Exception e2) {
	        }
	    }
	    return baos.toString();
	  }

}
