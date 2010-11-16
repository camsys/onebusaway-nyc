package org.onebusaway.nyc.webapp.actions.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

@Results( {@Result(type = "stream", name = "download", params = {
    "contentType", "text/csv", "contentDisposition", "filename=\"report.csv\""})})
public class ReportingAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  private static final ExecutorService executorService = Executors.newCachedThreadPool();
  
  private static final byte[] comma = ",".getBytes();
  private static final byte[] newline = "\n".getBytes();
  
  @Autowired
  private SessionFactory sessionFactory;
  
  private String query;
  private String reportError;

  private InputStream inputStream;

  // if there is an error running the sql, this will contain an error message
  public String getReportError() {
    return reportError;
  }

  // this is the download streamed to the user
  public InputStream getInputStream() {
    return inputStream;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  @Override
  public String execute() throws Exception {
    return SUCCESS;
  }
  
  // isolate deprecation warning to this method
  @SuppressWarnings("deprecation")
  private Connection getConnectionFromSession(Session session) {
    return session.connection();
  }
  
  public String submit() throws Exception {
    Session session = null;
    Connection connection = null;
    Statement statement = null;
    ResultSet rs = null;
    try {
      session = sessionFactory.openSession();
      connection = getConnectionFromSession(session);
      connection.setReadOnly(true);
      statement = connection.createStatement();
      rs = statement.executeQuery(query);
      
    } catch (Exception e) {
      // make sure everything is closed if an exception was thrown
      try { rs.close(); } catch (Exception ex) {}
      try { statement.close(); } catch (Exception ex) {}
      try { connection.close(); } catch (Exception ex) {}
      try { session.close(); } catch (Exception ex) {}

      reportError = e.getMessage();
      // not really "success", but we'll use the same template with the error displayed
      return SUCCESS;
    }
    
    // final so the output generator thread can close it
    final Session finalSession = session;
    final Connection finalConnection = connection;
    final Statement finalStatement = statement;
    final ResultSet finalRS = rs;
    
    final PipedInputStream pipedInputStream = new PipedInputStream();
    final PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

    executorService.execute(new Runnable() {

    @Override
    public void run() {
        try {
          while (finalRS.next()) {
            ResultSetMetaData metaData = finalRS.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
              String value = finalRS.getString(i + 1);
              byte[] valueBytes = value.getBytes();
              if (i > 0)
                pipedOutputStream.write(comma);
              pipedOutputStream.write(valueBytes);
            }
            pipedOutputStream.write(newline);
          }
        } catch (Exception e) {
        } finally {
          try { pipedOutputStream.close(); } catch (IOException e) {}
          try { finalRS.close(); } catch (SQLException e) {}
          try { finalStatement.close(); } catch (SQLException e) {}
          try { finalConnection.close(); } catch (SQLException e) {}
          try { finalSession.close(); } catch (Exception e) {}
        }
      }
    });

    // the input stream will get populated by the piped output stream
    inputStream = pipedInputStream;
    return "download";
  }

}
