package org.onebusaway.nyc.queue_http_proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.DataInputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.zeromq.ZMQ;

import org.junit.Test;
import org.onebusaway.nyc.queue_broker.ZMQForwarder;

import static org.junit.Assert.*;

/**
 * integration testing of http proxy.
 */
public class ListenerIT {
    
    static Logger logger = LoggerFactory.getLogger(ListenerIT.class);
    private ExecutorService _executorService = null;

    @Test
    public void testPing() throws Exception {
        String url = "http://localhost:8181/test/";
        
        setupBroker();
        sleep(1); // allow broker to startup

        final int MAX = 10;
        Listener listener = new Listener(MAX);
        new Thread(listener).start();
        new Thread(new Worker(url, "throwaway")).start();  // first message wakes up port; it will be lost!

        // now messages will be received
        for (int i = 0; i<MAX; i++) {
            // here we tack on an id to the end of the stream for later verification
            new Thread(new Worker(url, getContent(i))).start();
        }
        sleep(1); // allow subscriber(s) to finish
        listener.end();
        // last message ends while look, it will be discarded
        new Thread(new Worker(url, "throwaway")).start();  // break out of loop
    }

    /**
     * The proxy no longer provides its own socket, it connects to an existing socket.
     */
    private void setupBroker() {
    ZMQ.Context context = ZMQ.context(1); // 1 = number of threads

    ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
    int inPort = 5563;
    String inBind = "tcp://*:" + inPort;
    logger.info("subscribing to queue at " + inBind);
    
    subscriber.bind(inBind);
    
    // subscribe to everything
    subscriber.subscribe(new byte[0]); // was inTopic.getBytes()

    ZMQ.Socket publisher = context.socket(ZMQ.PUB);
    int outPort = 5564;
    String outBind = "tcp://*:" + outPort;
    logger.info("publishing to queue at " + outBind);

    publisher.bind(outBind);
    _executorService = Executors.newFixedThreadPool(1);
    //background the ZMQForwarder (broker appliance)
    ZMQForwarder broker = new ZMQForwarder(context, subscriber, publisher);
    _executorService.execute(broker);
    
    }
    
    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    /**
     * Post to the servlet.
     */
    public static class Worker implements Runnable {
        private String urlString;
        private String content;
        public Worker(String urlString, String content) {
            this.urlString = urlString;
            this.content = content;
        }

        public void run() {
            try {
                logger.debug("Posting");
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                connection.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(content);
                wr.flush();
                wr.close();

                DataInputStream input = new DataInputStream(connection.getInputStream());
                String response;
                while (null != ((response = input.readLine()))) {
                    System.err.println(response);
                }
                input.close();
            } catch (Exception e) {
                // bury
            }
        }
    }    

    /**
     * Subscriber to Queue.  Counts messages received, and verifies none
     * missing (except the first and the last which act to sync the listening)
     */
    public static class Listener implements Runnable {
        private ZMQ.Context context;
        private ZMQ.Socket subscriber;
        private int size = 0;
        private boolean keepRunning = true;
        private Map<Integer, Boolean> responses = Collections.synchronizedMap(new HashMap<Integer, Boolean>());

        public Listener(int size) {
            this.size = size;
            // Prepare our context and subscriber
            context = ZMQ.context(1);
            subscriber = context.socket(ZMQ.SUB);

            subscriber.connect("tcp://localhost:5564");

            subscriber.subscribe("bhs_queue".getBytes()); // subscribe to one topic only
            
            for (int i = 0; i<size; i++) {
                responses.put(i, false);
            }
        }

        public void run() {

            while (keepRunning) {
                // Read envelope with address
                String address = new String(subscriber.recv(0));

                // ensure the queue is only the expected topic
                assert("bhs_queue".equals(address));
                // Read message contents
                String message = new String(subscriber.recv(0));
                process(message);
            }

            subscriber.close();
        }

        public void end() {
            keepRunning = false;
            for (int i = 0; i<size; i++) {
                if (Boolean.FALSE.equals(responses.get(i))) {
                    System.err.println("missing id=" + i);
                }
                // assert we received each posted vehicle
                assert(Boolean.TRUE.equals(responses.get(i)));
            }
            logger.info("ListenerIT completed successfully!");
        }

        private void process(String contents) {
          String match = "vehicle-id\": ";
          int pos = contents.indexOf(match);
            if (pos != -1) {
                int end = contents.indexOf(",", pos+1);
                String idStr = contents.substring(pos + match.length(), end);
                try {
                  logger.debug("idStr=|" + idStr + "|");
                  Integer id = new Integer(idStr);
                  
                  // mark the response as received
                  responses.put(id, true);
                } catch (NumberFormatException nfe) {
                  logger.info("nfe");
                }
            } else {
              logger.debug("unknown contents=" + contents);
            }
        }
    }
    
    private String getContent(int vehicleId) {
              String content = 
"{" +
"    \"CcLocationReport\": {" +
"        \"request-id\" : 1205," +
"        \"vehicle\": {" +
"            \"vehicle-id\": " + vehicleId + "," +
"            \"agency-id\": 2008," +
"            \"agencydesignator\": \"MTA NYCT\"" +
"        }," +
"        \"status-info\": 0," +
"        \"time-reported\": \"2011-06-22T10:58:10.0-00:00\"," +
"        \"latitude\": 40640760," +
"        \"longitude\": -74018234," +
"        \"direction\": {" +
"            \"deg\": 128.77" +
"        }," +
"        \"speed\": 36," +
"        \"data-quality\": 4," +
"        \"manufacturer-data\": \"VFTP123456789\"," +
"        \"operatorID\": {" +
"            \"operator-id\": 0," +
"            \"designator\": \"123456\"" +
"        }," +
"        \"runID\": {" +
"            \"run-id\": 0," +
"            \"designator\": \"1\"" +
"        }," +
"        \"destSignCode\": 4631," +
"        \"emergencyCodes\": {" +
"            \"emergencyCode\": [" +
"                \"1\"" +
"            ]" +
"        }," +
"        \"routeID\": {" +
"            \"route-id\": 0," +
"            \"route-designator\": \"63\"" +
"        }," +
"        \"localCcLocationReport\": {" +
"            \"NMEA\": {" +
"                \"sentence\": [" +
"                    \"$GPRMC,105850.00,A,4038.445646,N,07401.094043,W,002.642,128.77,220611,,,A*7C\"," +
"" +
"                    \"$GPGGA,105850.000,4038.44565,N,07401.09404,W,1,09,01.7,+00042.0,M,,M,,*49\"" +
"                ]" +
"            }" +
"        }" +
"    }" +
"}";
              return content;
    }

}