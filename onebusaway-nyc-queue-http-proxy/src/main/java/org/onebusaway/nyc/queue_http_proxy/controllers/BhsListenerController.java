/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.queue_http_proxy.controllers;

import org.onebusaway.nyc.queue_http_proxy.impl.PublishingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletException;
import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;

@Controller
public class BhsListenerController implements Serializable {

    private static Logger _log = LoggerFactory.getLogger(BhsListenerController.class);

    @Autowired
    private PublishingManager publisher;

    private boolean enableExceptions;

    public boolean isEnableExceptions() {
        return enableExceptions;
    }

    public void setEnableExceptions(boolean enableExceptions) {
        this.enableExceptions = enableExceptions;
    }

    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<String> doPost(@RequestBody JsonNode requestBody) throws ServletException {
        try {
            publisher.send(requestBody);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            _log.error("Failed to process request {}", e.getMessage(), e);
            // You can throw custom exceptions and handle them with @ExceptionHandler if needed
            if(enableExceptions){
                return ResponseEntity.badRequest().body("Failed to process request");
            } else {
                return ResponseEntity.ok("Failed to process request");
            }
        }
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() throws ServletException {
        return ResponseEntity.ok("Success");
    }


}
