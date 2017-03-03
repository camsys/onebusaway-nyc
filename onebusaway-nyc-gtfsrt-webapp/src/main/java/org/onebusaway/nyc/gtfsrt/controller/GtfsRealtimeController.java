package org.onebusaway.nyc.gtfsrt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class GtfsRealtimeController {
    @RequestMapping(value = "/hello")
    public @ResponseBody Map<String, String> getHello() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("hello", "world");
        return map;
    }

}
