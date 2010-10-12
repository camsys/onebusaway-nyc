package org.onebusaway.nyc.sms.actions;

public class IndexAction extends AbstractNycSmsAction {

  private static final long serialVersionUID = 1L;
  
  /** text message */
  private String message;
  
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String execute() throws Exception {
    if (message == null)
      message = "";
    return SUCCESS;
  }

}
