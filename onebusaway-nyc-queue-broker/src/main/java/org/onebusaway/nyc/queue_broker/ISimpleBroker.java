package org.onebusaway.nyc.queue_broker;

public interface ISimpleBroker {
    void main(String[] args);
    void run();
    void setInPort(int inPort);
    void setOutPort(int outPort);
}
