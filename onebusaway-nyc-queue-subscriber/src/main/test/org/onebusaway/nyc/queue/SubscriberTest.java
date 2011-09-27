package org.onebusaway.nyc.queue;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.zeromq.ZMQ.*;

import org.junit.Test;
import org.mockito.Mockito;

public class SubscriberTest {

  @Test
  public void testMain() {
    Socket socket = Mockito.mock(Socket.class);
    Context context = Mockito.mock(Context.class);
    
    when(socket.recv(0)).thenReturn("foobar".getBytes())
    .thenReturn("barfoo".getBytes());

    Subscriber subscriber = new Subscriber() {
      @Override
      void process(String address, String contents) {
        assertEquals("foobar", address);
        assertEquals("barfoo", contents);
      }
    };
    // TODO Yes, should probably do these with injectMocks
    subscriber.setSocket(socket);
    subscriber.setContext(context);
    subscriber.readAndProcess();
  }

}
