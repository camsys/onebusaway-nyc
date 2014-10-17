package org.onebusaway.nyc.vehicle_tracking.services.aws;

public interface AwsMetadataService {
	public String getInstanceId();
	public String getInstanceType();
	public String getAmiId();
	public String getPublicHostname();
	public String getInternalHostname();
}
