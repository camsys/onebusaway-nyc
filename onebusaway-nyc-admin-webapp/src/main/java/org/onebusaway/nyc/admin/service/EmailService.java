package org.onebusaway.nyc.admin.service;

public interface EmailService {

  void setSmtpUser(String user);
  void setSmtpPassword(String password);
  void sendAsync(String to, String from, String subject, StringBuffer message);

  void setup();
}
