package org.onebusaway.nyc.queue;

public class PrintingSubscriber extends Subscriber {

  public static void main(String[] args) {
    new PrintingSubscriber().run();
  }

  @Override
  void process(String address, String contents) {
    System.out.println(address + " : " + contents);
  }

}
