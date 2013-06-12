package org.onebusaway.nyc.admin.service.impl;

import org.onebusaway.nyc.admin.service.EmailService;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AWSJavaMailTransport;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;

public class EmailServiceImpl implements EmailService, ServletContextAware {

  private static Logger _log = LoggerFactory.getLogger(EmailServiceImpl.class);
  private AWSCredentials _credentials;
  AmazonSimpleEmailServiceAsyncClient _eClient;
  private String _username;
  private String _password;
  private Properties _properties;
  private Session _session;
  private Transport _transport;
  
  @Override
  public void setSmtpUser(String user) {
    _username = user;
  }
  @Override
  public void setSmtpPassword(String password) {
    _password = password;
  }
  
  @PostConstruct
  @Override
  public void setup() {
    try {
      // AWS specifics
      _credentials = new BasicAWSCredentials(_username, _password);
      _eClient = new AmazonSimpleEmailServiceAsyncClient(_credentials);
      // Java specifics
    _properties = new Properties();
    _properties.setProperty("mail.transport.protocol", "aws");
    _properties.setProperty("mail.aws.user", _credentials.getAWSAccessKeyId());
    _properties.setProperty("mail.aws.password", _credentials.getAWSSecretKey());
    _session = Session.getInstance(_properties);
    _transport = new AWSJavaMailTransport(_session, null);
    

    } catch (Exception ioe) {
      // log this heavily, but don't let it prevent context startup
      _log.error("EmailServiceImpl setup failed, likely due to missing or invalid credentials.");
      _log.error(ioe.toString());
      
    }

  }
  @Override
  public void sendAsync(String to, String from, String subject, StringBuffer messageBody) {
    List<String> toAddresses = new ArrayList<String>();
    for (String address : to.split(",")) {
      toAddresses.add(address);
    }
    Destination destination = new Destination(toAddresses);
    Body body = new Body();
    body.setText(new Content(messageBody.toString()));
    Message message = new Message(new Content(subject), body);
    SendEmailRequest sendEmailRequest = new SendEmailRequest(from, destination, message); 
    Future<SendEmailResult> result = _eClient.sendEmailAsync(sendEmailRequest);
    _log.info("sent email to " + to + " with finished=" + result.isDone());
  }

  @Override
  public void send(String to, String from, String subject, StringBuffer messageBody) {
    sendJava(to, from, subject, messageBody);
  }
  
  public void sendSES(String to, String from, String subject, StringBuffer messageBody) {
    List<String> toAddresses = new ArrayList<String>();
    for (String address : to.split(",")) {
      toAddresses.add(address);
    }
    Destination destination = new Destination(toAddresses);
    Body body = new Body();
    body.setText(new Content(messageBody.toString()));
    Message message = new Message(new Content(subject), body);
    SendEmailRequest sendEmailRequest = new SendEmailRequest(from, destination, message); 
    SendEmailResult result = _eClient.sendEmail(sendEmailRequest);
    _log.info("sent email to " + to + " with result=" + result);
  }

  public void sendJava(String to, String from, String subject, StringBuffer messageBody) {
    try {
      javax.mail.Message msg = new MimeMessage(_session);
      msg.setFrom(new InternetAddress(from));
      msg.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
      msg.setSubject(subject);
      if (messageBody != null) { 
        msg.setText(messageBody.toString());
      } 
      msg.saveChanges();
      if (!_transport.isConnected()) {
        _transport.connect();
      }
      _transport.sendMessage(msg, null);
    } catch (Exception e) {
      _log.error("sendJava failed", e);
    }
  }
  @Override
  public void setServletContext(ServletContext servletContext) {
    if (servletContext != null) {
      String user = servletContext.getInitParameter("smtp.user");
      _log.info("servlet context provided smtp.user=" + user);
      if (user != null) {
        setSmtpUser(user);
      }
      String password = servletContext.getInitParameter("smtp.password");
      if (password != null) {
        _log.info("servlet context provided smtp.password");
        setSmtpPassword(password);
      }
    }
  }

}
