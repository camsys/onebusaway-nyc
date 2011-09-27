#!/bin/bash
java -Dmq.host='*' -Dmq.port=5563 -cp "jzmq-053c2d7.jar:classes" org.onebusaway.nyc.queue.Subscriber
