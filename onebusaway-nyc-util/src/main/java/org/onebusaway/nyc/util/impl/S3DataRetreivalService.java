package org.onebusaway.nyc.util.impl;

import java.io.IOException;
import java.io.InputStream;

public class S3DataRetreivalService implements DataRetreivalService {

    private S3Utility s3Utility;

    public S3DataRetreivalService(String username, String password, String path) throws Exception {
        String bucketName = S3Utility.getBucketFromS3Path(path);;
        this.s3Utility = new S3Utility(username, password, bucketName);
    }

    @Override
    public InputStream get(String path) {
        return s3Utility.get(S3Utility.getKeyFromS3Path(path));
    }

    @Override
    public String getAsString(String path) throws IOException {
        return s3Utility.getAsString(S3Utility.getKeyFromS3Path(path));
    }
}
