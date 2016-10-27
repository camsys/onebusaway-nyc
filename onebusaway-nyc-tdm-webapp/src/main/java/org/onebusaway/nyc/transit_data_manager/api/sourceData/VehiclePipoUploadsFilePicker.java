package org.onebusaway.nyc.transit_data_manager.api.sourceData;

import java.io.IOException;

public class VehiclePipoUploadsFilePicker extends DateUbarTimeTimestampFilePicker {

	private static String FILE_PREFIX = "UTSPUPUFULL_";
	private static String FILE_SUFFIX = ".txt";
	  
  private String filePrefix;
  private String fileSuffix;

  public VehiclePipoUploadsFilePicker(String timestampedUploadsDirProperty)
      throws IOException {
    super(System.getProperty(timestampedUploadsDirProperty));
    filePrefix = FILE_PREFIX;
    fileSuffix = FILE_SUFFIX;
  }

  public VehiclePipoUploadsFilePicker(String timestampedUploadsDirProperty, String prefix, String suffix)
      throws IOException {
    super(System.getProperty(timestampedUploadsDirProperty));
    filePrefix = prefix;
    fileSuffix = suffix;
  }

	@Override
	protected String getFilePrefix() {
		return filePrefix;
	}

	@Override
	protected String getFileSuffix() {
		return fileSuffix;
	}

	void setFilePrefix(String filePrefix) {
		this.filePrefix = filePrefix;
	}

	void setFileSuffix(String fileSuffix) {
		this.fileSuffix = fileSuffix;
	}

}
