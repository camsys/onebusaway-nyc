package org.onebusaway.nyc.admin.model;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.List;

public class BundleBuildRequest {
	private String _id;
	private String _bundleDirectory;
	private String _bundleName;
	private String _tmpDirectory;
	private String _emailAddress;
	private String _bundleStartDate;
	private String _bundleEndDate;
	private String _bundleComment;
	private boolean _archiveFlag;
	private boolean _consolidateFlag;

	public String getBundleDirectory() {
		return _bundleDirectory;
	}

	public void setBundleDirectory(String bundleDirectory) {
		_bundleDirectory = bundleDirectory;
	}

	public String getTmpDirectory() {
		return _tmpDirectory;
	}

	public void setTmpDirectory(String tmpDirectory) {
		_tmpDirectory = tmpDirectory;
	}

	// TODO this should come from config service
	public List<String> getNotInServiceDSCList() {
		ArrayList<String> dscs = new ArrayList<String>();
		dscs.add("10");
		dscs.add("11");
		dscs.add("12");
		dscs.add("13");
		dscs.add("22");
		dscs.add("6");
		return dscs;
	}

	public String getBundleName() {
		return _bundleName;
	}

	public void setBundleName(String bundleName) {
		_bundleName = bundleName;
	}

	public LocalDate getBundleStartDate() {
		DateTimeFormatter dtf = ISODateTimeFormat.date();
		return (_bundleStartDate==null?null: new LocalDate(dtf.parseLocalDate(_bundleStartDate)));
	}

	public String getBundleStartDateString() {
		return _bundleStartDate;
	}

	public void setBundleStartDate(String bundleStartDate) {
		_bundleStartDate = bundleStartDate;
	}

	public LocalDate getBundleEndDate() {
		DateTimeFormatter dtf = ISODateTimeFormat.date();
		return (_bundleEndDate==null?null: new LocalDate(dtf.parseLocalDate(_bundleEndDate)));
	}

	public void setBundleEndDate(String bundleEndDate) {
		_bundleEndDate = bundleEndDate;
	}

	public String getBundleEndDateString() {
		return _bundleEndDate;
	}

	public String getBundleComment() {
		return _bundleComment;
	}

	public void setBundleComment(String bundleComment) {
		_bundleComment = bundleComment;
	}
	
	public String getEmailAddress() {
		return _emailAddress;
	}

	public void setEmailAddress(String emailTo) {
		_emailAddress = emailTo;
	}

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		_id = id;
	}

	public boolean getArchiveFlag() {
	  return _archiveFlag;
	}
	
  public void setArchiveFlag(boolean archive) {
    _archiveFlag = archive;
  }
  
  public boolean getConsolidateFlag() {
    return _consolidateFlag;
  }
  
  public void setConsolidateFlag(boolean consolidate) {
    _consolidateFlag = consolidate;
  }

}
