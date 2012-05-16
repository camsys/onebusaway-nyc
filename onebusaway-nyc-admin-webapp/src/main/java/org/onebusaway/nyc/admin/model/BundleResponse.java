package org.onebusaway.nyc.admin.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BundleResponse {
  private boolean _isComplete = false;
  private Exception _exception;
  private List<String> _statusMessages = Collections.synchronizedList(new ArrayList<String>());
  private List<String> _validationFiles = Collections.synchronizedList(new ArrayList<String>());
  
  public synchronized  boolean isComplete() {
    return _isComplete;
  }

  public synchronized  void setException(Exception any) {
    _exception = any;
  }

  public Exception getException() {
    return _exception;
  }

  public List<String> getStatusMessages() {
    return new ArrayList<String>(_statusMessages);
  }
  
  public void addStatusMessage(String msg) {
    _statusMessages.add(msg);
  }
  
  public synchronized void setComplete(boolean b) {
    _isComplete = b;
  }

  public List<String> getValidationFiles() {
    return new ArrayList<String>(_validationFiles);
  }

  public void addValidationFile(String filename) {
    _validationFiles.add(filename);
  }
}
