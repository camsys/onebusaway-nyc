package org.onebusaway.nyc.webapp.actions;

public class LoginAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  private boolean _failure;

  public void setFailure(boolean failure) {
    _failure = failure;
  }

  public boolean isFailure() {
    return _failure;
  }

}
