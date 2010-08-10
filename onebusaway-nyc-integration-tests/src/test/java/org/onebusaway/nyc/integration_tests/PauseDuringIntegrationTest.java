package org.onebusaway.nyc.integration_tests;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.junit.Test;

public class PauseDuringIntegrationTest {

  @Test
  public void test() throws InterruptedException {

    String value = System.getProperty("pauseDuringIntegrationTest", "false");

    if (!value.equals("true")) {
      System.err.println("===== NO PAUSE INTEGRATION TESTS =======");
      return;
    }

    System.err.println("===== PAUSING INTEGRATION TESTS =======");

    JFrame frame = new JFrame("Pause");
    JButton button = new JButton("Resume");
    frame.getContentPane().add(button);
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized (PauseDuringIntegrationTest.this) {
          PauseDuringIntegrationTest.this.notifyAll();
        }
      }
    });

    frame.pack();
    frame.setVisible(true);

    synchronized (this) {
      wait();
    }

    System.err.println("===== RESUMING INTEGRATION TESTS =======");
    frame.setVisible(false);
  }
}
