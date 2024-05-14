//package org.onebusaway.api.web.actions.api;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.MediaType;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//@RestController
//public class SseDemoController {
//
//    private static Logger _log = LoggerFactory.getLogger(SseDemoController.class);
//
//    private Map<String, ConcurrentLinkedQueue<SseEmitter>> emittersById = new ConcurrentHashMap();
//
//    int n = 0;
//
//    @CrossOrigin(
//            origins = "*", // Allows requests from any origin
//            allowedHeaders = "*", // Allows all headers
//            methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS}, // Allows all common methods
//            maxAge = 3600, // Allows caching the pre-flight request for 1 hour
//            allowCredentials = "true" // Allows cookies and other credentials
//    )
//    @RequestMapping(path="/sse-demo/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public SseEmitter SseDemo(@PathVariable("id") String id,
//                              @RequestParam(name ="TimeoutMin", defaultValue = ".5",
//                                      required = false ) Long min){
//        SseEmitter emitter = new SseEmitter(1000*60*min);
//        emitter.onTimeout (()->{
//            _log.info("SSE connection timed out");
//            emitter.complete();
//        });
//        emitter.onError ((Throwable throwable) ->{
//            _log.error("Listen SSE exception", throwable);
//            emitter.complete();
//        });
//        emitter.onCompletion (() ->{
//            // this should be pretty fast so long as there's a set timeout length
//            emittersById.get(id).remove(emitter);
//        });
//        ConcurrentLinkedQueue<SseEmitter> emitters = getEmmitersOrSetDefault(id, new ConcurrentLinkedQueue());
//        emitters.add(emitter);
//        return emitter;
//    }
//
//    private synchronized ConcurrentLinkedQueue<SseEmitter> getEmmitersOrSetDefault(String id, ConcurrentLinkedQueue blankQueue){
//        ConcurrentLinkedQueue<SseEmitter> emitters = emittersById.getOrDefault(id, blankQueue);
//        if(emitters.size()==0){
//            emittersById.put(id,emitters);
//        }
//        return emitters;
//    }
//
//    @CrossOrigin(origins = "*")
//    @Async
//    @Scheduled(fixedRateString = "${Sse.FixedRate}")
//    public void emitSse() throws IOException {
//        for(String s : emittersById.keySet()){
//            //get info abt that key
//            n=n+1;
//            String out = n + "th messsage sent! test succeeded for "+s;
//            ConcurrentLinkedQueue<SseEmitter> emitters = emittersById.get(s);
//            for (SseEmitter emitter : emitters){
//                try {
//                    emitter.send(SseEmitter.event().name("message").data(out));
//                } catch (IOException e) {
//                    emitter.complete();
//                }
//
//            }
//        }
//    }
//}
