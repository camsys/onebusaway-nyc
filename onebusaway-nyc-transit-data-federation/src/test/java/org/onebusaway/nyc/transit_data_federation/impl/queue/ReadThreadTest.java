package org.onebusaway.nyc.transit_data_federation.impl.queue;

import static org.junit.Assert.*;

import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.realtime.api.VehicleLocationListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReadThreadTest {

  @Mock VehicleLocationListener listener;
  @Mock VehicleTrackingManagementService vtmService;
  
  @InjectMocks InferenceInputQueueListenerTask task;
  
  @Test
  public void testRun() {
//    InferenceInputQueueListenerTask task = new InferenceInputQueueListenerTask();
    Socket socket = Mockito.mock(Socket.class);
    Poller poller = Mockito.mock(Poller.class);
    when(poller.pollin(0)).thenReturn(true);
    when(socket.recv(0)).thenReturn("foobar".getBytes());
    InferenceInputQueueListenerTask.ReadThread t = task.new ReadThread(socket, poller);
    t.run();
    verify(listener).handleVehicleLocationRecord(null);
//    fail("Not yet implemented");
  }

}
