package org.onebusaway.api.web.schedulers;

import org.onebusaway.api.web.interceptors.BaseSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

@Component
public class DemoSocketScheduler implements Runnable{

    int n = 0;

    @Autowired
    @Qualifier("demoWebSocketHandler")
    BaseSocketHandler handler;

    @Override
    @Async
    @Scheduled(fixedRateString = "${WebSocket.FixedRate}")
    public void run(){
        for(String key : handler.getKeyList()) {
            //get info abt that key
            n = n + 1;
            handler.broadcastUpdate(new TextMessage(n + "th messsage sent! test succeeded for " + key),key);
        }
    }
}
