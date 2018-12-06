package org.onebusaway.nyc.queue.apc;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import org.apache.commons.lang.StringUtils;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.QueueListenerTask;
import org.onebusaway.nyc.transit_data.model.NycVehicleLoadBean;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;

import java.util.Date;

/*
 * Push presence of APC data to Cloudwatch for inspection.
 *
 * Run via:
 * nohup java \
 *  -jar onebusaway-nyc-queue-apc-jar-with-dependencies.jar \
 *  -Dapc.host=queue.qa.obanyc.com \
 *  -Dapc.env=Obanyc:qa \
 *  -Dapc.key=my_cloudwatch_key \
 *  -Dapc.value=my_cloudwatch_secret \
 *  >apc.log 2>&1 &
 */
public class ApcMonitor extends QueueListenerTask {

    public static final String DEFAULT_ENV = "Obanyc:qa";
    public static final String DEFAULT_KEY = "my_cw_key";
    public static final String DEFAULT_VALUE = "secret";
    public static final String DEFAULT_HOST = "queue.qa.obanyc.com";
    public static final String DEFAULT_NAME = "apc";
    public static final String DEFAULT_DISPLAY_NAME = DEFAULT_NAME;
    public static final int DEFAULT_PORT = 5576;

    private AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(new BasicAWSCredentials(getKey(), getValue()));

    @Override
    public boolean processMessage(String contents, byte[] buff) throws Exception {
        if(StringUtils.isBlank(contents)){
            log("rejected message, message is empty");
            return false;
        }

        try {
            NycVehicleLoadBean bean = _mapper.readValue(buff, NycVehicleLoadBean.class);
            processResult(bean, contents);
        } catch (Exception any) {
            log("received corrupted APC message from queue; discarding: " + any.getMessage(), any);
            log("Contents=" + contents);
            return false;
        }
        return true;
    }

    @Override
    public void startListenerThread() {
        String host = getQueueHost();
        String queueName = getQueueName();
        Integer port = getQueuePort();

        System.out.println("connecting to " + queueName + " queue at " + host  + ":" + port);
        try {
            initializeQueue(host, queueName, port);
        } catch (InterruptedException ie) {
            return;
        }
        _initialized = true;
    }

    @Override
    public String getQueueHost() {
        String override =  System.getProperty("apc.host");
        if (StringUtils.isNotEmpty(override)) {
            return override;
        }
        return DEFAULT_HOST;

    }

    @Override
    public String getQueueName() {
        String override =  System.getProperty("apc.name");
        if (StringUtils.isNotEmpty(override)) {
            return override;
        }
        return DEFAULT_NAME;

    }

    @Override
    public String getQueueDisplayName() {
        String override =  System.getProperty("apc.displayName");
        if (StringUtils.isNotEmpty(override)) {
            return override;
        }
        return DEFAULT_DISPLAY_NAME;

    }

    @Override
    public Integer getQueuePort() {
        String override =  System.getProperty("apc.displayName");
        if (StringUtils.isNotEmpty(override)) {
            return Integer.parseInt(override);
        }
        return DEFAULT_PORT;
    }


    private void processResult(NycVehicleLoadBean message, String contents) {
        VehicleOccupancyRecord vor = toVehicleOccupancyRecord(message);
        publishMetric(vor);


    }


    public static void main(String[] args) {
        System.out.println("starting up....");
        ApcMonitor monitor = new ApcMonitor();
        monitor.setup();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("caught SIGINT, exiting");
                return;
            }
        }
        System.out.println("main thread exiting!");
    }

    private VehicleOccupancyRecord toVehicleOccupancyRecord(NycVehicleLoadBean message) {
        VehicleOccupancyRecord vor = new VehicleOccupancyRecord();
        vor.setOccupancyStatus(message.getLoad());
        vor.setTimestamp(new Date(message.getRecordTimestamp()));
        vor.setVehicleId(AgencyAndId.convertFromString(message.getVehicleId()));
        vor.setRouteId(message.getRoute());
        vor.setDirectionId(message.getDirection());
        return vor;
    }

    private void publishMetric(VehicleOccupancyRecord vor) {
        if (cloudWatch == null) {
            log("cloudwatch init failed");
            return;
        }

        long now = System.currentTimeMillis();
        long received = vor.getTimestamp().getTime() * 1000;

        PutMetricDataRequest request = buildMetricData("QueueListenerOccupancy", "Count", 1., buildDimensions("vehicleId=" + vor.getVehicleId()));
        try {
            if (vor.getOccupancyStatus().valueOf() != -1) {
                logCSV(vor, now);
                cloudWatch.putMetricData(request);
            } else {
                logCSV(vor, now);
            }

        } catch (Exception any) {
            log("push to cloudwatch failed:", any);
        }
    }

    private PutMetricDataRequest buildMetricData(String metricName, String unit, Double value, Dimension dimensions) {
        MetricDatum datum;
        if (dimensions != null)
            datum = new MetricDatum().withMetricName(metricName)
                    .withUnit(unit)
                    .withValue(value)
                    .withDimensions(dimensions);
        else
            datum = new MetricDatum().withMetricName(metricName)
                    .withUnit(unit)
                    .withValue(value);
        return new PutMetricDataRequest().withNamespace(getEnvironmentName()).withMetricData(datum);
    }

    private Dimension buildDimensions(String s) {
        if (s == null) return null;
        if (s.indexOf('=') == -1) return null;
        String[] dimArray = s.split("=");
        Dimension dimension = new Dimension().withName(dimArray[0]).withValue(dimArray[1]);
        return dimension;
    }

    private String getEnvironmentName() {
        String override =  System.getProperty("apc.env");
        if (StringUtils.isNotEmpty(override)) {
            return override;
        }
        return DEFAULT_ENV;
    }

    private String getKey() {
        String override = System.getProperty("apc.key");
        if (StringUtils.isNotEmpty(override)) {
            return override;
        }
        return DEFAULT_KEY;
    }

    private String getValue() {
        String override = System.getProperty("apc.value");
        if (StringUtils.isNotEmpty(override)) {
            return override;
        }
        return DEFAULT_VALUE;
    }

    private void log(String s) {
        System.out.println(new Date() + "[INFO]: " + s);
    }

    private void log(String s, Exception any) {
        log(s);
        any.printStackTrace();
    }
    private void logCSV(VehicleOccupancyRecord vor, long now) {
        StringBuffer record = new StringBuffer();
        record.append(now)
                .append(",")
                .append(vor.getVehicleId())
                .append(",")
                .append(vor.getRouteId())
                .append(",")
                .append(vor.getDirectionId())
                .append(",")
                .append(vor.getOccupancyStatus().toString())
                .append(",")
                .append(vor.getTimestamp().getTime()*1000)
                .append(",")
                .append( (now - vor.getTimestamp().getTime()*1000));
        System.out.println(record.toString());
    }


}