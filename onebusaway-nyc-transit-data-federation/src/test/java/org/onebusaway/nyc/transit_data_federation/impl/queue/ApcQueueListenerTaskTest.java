package org.onebusaway.nyc.transit_data_federation.impl.queue;

import org.junit.Test;
import org.onebusaway.nyc.transit_data.model.NycVehicleLoadBean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * test different combinations of serialization
 * as we change support for crowding data
 */
public class ApcQueueListenerTaskTest {

    @Test
    public void testProcessResult() throws Exception {
        ApcQueueListenerTask task = getTask();
        String badMessage = "foo";
        byte[] buff = badMessage.getBytes();
        assertFalse(task.processMessage(badMessage, buff));

        // original style message
        String validMessage1 = "{\"load\": \"UNKNOWN\", \"direction\": \"1\", \"vehicleId\": \"MTABC_650\", \"route\": \"MTABC_Q66\", \"recordTimestamp\": 1602853767.0, \"estimatedCount\": 8}";
        assertTrue(task.processMessage(validMessage1, validMessage1.getBytes()));

        // test introduction of loadDesc
        String validMessage2 = "{\"load\": \"UNKNOWN\", \"loadDesc\": \"H\", \"direction\": \"1\", \"vehicleId\": \"MTABC_650\", \"route\": \"MTABC_Q66\", \"recordTimestamp\": 1602853767.0, \"estimatedCount\": 8}";
        assertTrue(task.processMessage(validMessage2, validMessage2.getBytes()));

        // test removal of load with loadDesc remaining
        String validMessage3 = "{\"loadDesc\": \"H\", \"direction\": \"1\", \"vehicleId\": \"MTABC_650\", \"route\": \"MTABC_Q66\", \"recordTimestamp\": 1602853767.0, \"estimatedCount\": 8}";
        assertTrue(task.processMessage(validMessage3, validMessage3.getBytes()));

    }

    private ApcQueueListenerTask getTask() {
        return new ApcQueueListenerTask() {
            @Override
            public String getQueueName() {
                return "apc";
            }

            @Override
            public Boolean useApcIfAvailable() {
                return Boolean.TRUE;
            }

            @Override
            protected void processResult(NycVehicleLoadBean message, String contents) {
                // do nothing for the test, the json deserialization is what we are testing
            }
        };
    }

}
