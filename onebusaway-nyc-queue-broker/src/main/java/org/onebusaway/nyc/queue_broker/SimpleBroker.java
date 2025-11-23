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

package org.onebusaway.nyc.queue_broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ZeroMQ-based message broker that forwards messages from an input socket to an output socket.
 * Implements a simple pub-sub pattern with configurable ports.
 */
public class SimpleBroker {
  private static final Logger logger = LoggerFactory.getLogger(SimpleBroker.class);

  // Configuration constants
  private static final int DEFAULT_IN_PORT = 5566;
  private static final int DEFAULT_OUT_PORT = 5567;
  private static final int HWM_VALUE = 50000; // High Water Mark
  private static final int LINGER_MS = 1000; // Wait time for pending messages on close
  private static final int IO_THREADS = 1;
  private static final int BUFFER_SIZE = 1024 * 1024; // 1MB
  private static final int SHUTDOWN_TIMEOUT_MS = 5000;

  // Configuration
  private int inPort;
  private int outPort;

  // State
  private final AtomicBoolean running = new AtomicBoolean(false);

  // ZeroMQ components
  private ZMQ.Context context;
  private ZMQ.Socket subscriber;
  private ZMQ.Socket publisher;

  // Threading
  private Thread proxyThread;

  public static void main(String[] args) {
    SimpleBroker broker = new SimpleBroker();

    if (!broker.parseCommandLineArguments(args)) {
      System.exit(1);
    }

    broker.registerShutdownHook();

    try {
      broker.run();
    } catch (Exception e) {
      logger.error("Fatal error in broker", e);
      System.exit(1);
    }
  }

  public SimpleBroker() {
    logger.info("Initializing SimpleBroker");
  }

  /**
   * Parse command line arguments for port configuration.
   *
   * @param args Command line arguments [inputPort, outputPort]
   * @return true if parsing successful, false otherwise
   */
  private boolean parseCommandLineArguments(String[] args) {
    // Parse input port
    if (args.length > 0) {
      if (!setPortFromString(args[0], true)) {
        return false;
      }
    } else {
      setInPort(DEFAULT_IN_PORT);
    }

    // Parse output port
    if (args.length > 1) {
      if (!setPortFromString(args[1], false)) {
        return false;
      }
    } else {
      setOutPort(DEFAULT_OUT_PORT);
    }

    return true;
  }

  /**
   * Parse and set port from string argument.
   *
   * @param portString String representation of port number
   * @param isInputPort true if input port, false if output port
   * @return true if successful, false otherwise
   */
  private boolean setPortFromString(String portString, boolean isInputPort) {
    try {
      int port = Integer.parseInt(portString);
      if (isInputPort) {
        setInPort(port);
      } else {
        setOutPort(port);
      }
      return true;
    } catch (NumberFormatException e) {
      logger.error("Invalid {} port: {}", isInputPort ? "input" : "output", portString);
      return false;
    } catch (IllegalArgumentException e) {
      logger.error(e.getMessage());
      return false;
    }
  }

  /**
   * Register shutdown hook for graceful cleanup.
   */
  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutdown signal received, stopping broker...");
      stop();
    }, "Shutdown-Hook"));
  }

  /**
   * Main run loop - initializes and starts the broker.
   */
  public void run() {
    if (running.get()) {
      logger.warn("Broker already running");
      return;
    }

    running.set(true);
    logger.info("Starting SimpleBroker - Input: {}, Output: {}", inPort, outPort);

    try {
      initializeZeroMQ();
      startProxyThread();
      waitForCompletion();
    } catch (ZMQException e) {
      handleZMQException(e, "broker startup");
      throw new RuntimeException("Failed to start broker", e);
    } catch (Exception e) {
      logger.error("Error running broker", e);
      throw new RuntimeException("Broker failed to start", e);
    } finally {
      cleanup();
    }
  }

  /**
   * Initialize ZeroMQ context and sockets.
   */
  private void initializeZeroMQ() {
    context = ZMQ.context(IO_THREADS);
    logger.debug("ZMQ Context created with {} I/O thread(s)", IO_THREADS);

    subscriber = createAndBindSubscriber();
    publisher = createAndBindPublisher();

    logBrokerConfiguration();
  }

  /**
   * Create and bind the subscriber socket.
   */
  private ZMQ.Socket createAndBindSubscriber() {
    ZMQ.Socket socket = context.socket(ZMQ.SUB);
    configureSocket(socket, "subscriber");

    String bindAddress = "tcp://*:" + inPort;
    bindSocket(socket, bindAddress, "subscriber");

    socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
    logger.info("Subscriber successfully bound to {} and subscribed to all topics", bindAddress);

    return socket;
  }

  /**
   * Create and bind the publisher socket.
   */
  private ZMQ.Socket createAndBindPublisher() {
    ZMQ.Socket socket = context.socket(ZMQ.PUB);
    configureSocket(socket, "publisher");

    String bindAddress = "tcp://*:" + outPort;
    bindSocket(socket, bindAddress, "publisher");

    logger.info("Publisher successfully bound to {}", bindAddress);

    return socket;
  }

  /**
   * Configure socket with common options.
   */
  private void configureSocket(ZMQ.Socket socket, String socketType) {
    socket.setHWM(HWM_VALUE);
    socket.setLinger(LINGER_MS);

    if (socketType.equals("subscriber")) {
      socket.setReceiveBufferSize(BUFFER_SIZE);
    } else {
      socket.setSendBufferSize(BUFFER_SIZE);
    }
  }

  /**
   * Bind socket to address with error handling.
   */
  private void bindSocket(ZMQ.Socket socket, String bindAddress, String socketType) {
    logger.info("Binding {} to {}", socketType, bindAddress);

    int result = socket.bind(bindAddress);
    if (result == -1) {
      throw new RuntimeException(String.format(
              "Failed to bind %s to %s - port may already be in use or insufficient permissions",
              socketType, bindAddress
      ));
    }
  }

  /**
   * Log broker configuration.
   */
  private void logBrokerConfiguration() {
    logger.info("SimpleBroker is now running. Press Ctrl+C to stop.");
    logger.info("Configuration: HWM={}, Linger={}ms, IO Threads={}, Buffer={}KB",
            HWM_VALUE, LINGER_MS, IO_THREADS, BUFFER_SIZE / 1024);
  }

  /**
   * Start the ZeroMQ proxy in a separate thread.
   */
  private void startProxyThread() {
    proxyThread = new Thread(this::runProxy, "ZMQ-Proxy-Thread");
    proxyThread.start();
    logger.info("Proxy thread started");
  }

  /**
   * Run the ZeroMQ proxy loop.
   */
  private void runProxy() {
    logger.info("Proxy thread starting...");

    try {
      boolean result = ZMQ.proxy(subscriber, publisher, null);
      logger.info("Proxy returned with result: {}", result);
    } catch (ZMQException e) {
      handleProxyException(e);
    } catch (Exception e) {
      logger.error("Unexpected error in proxy thread", e);
      running.set(false);
    } finally {
      logger.info("Proxy thread exiting");
    }
  }

  /**
   * Handle exceptions from the proxy thread.
   */
  private void handleProxyException(ZMQException e) {
    if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
      logger.info("Proxy stopped - context terminated (expected during shutdown)");
    } else {
      logger.error("ZMQ proxy error: {} (code: {})", e.getMessage(), e.getErrorCode(), e);
      running.set(false);
    }
  }

  /**
   * Wait for broker completion or interruption.
   */
  private void waitForCompletion() {
    while (running.get() && proxyThread.isAlive()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.warn("Main thread interrupted");
        Thread.currentThread().interrupt();
        break;
      }
    }

    checkForUnexpectedTermination();
  }

  /**
   * Check if proxy thread died unexpectedly.
   */
  private void checkForUnexpectedTermination() {
    if (!proxyThread.isAlive() && running.get()) {
      logger.error("Proxy thread died unexpectedly while broker should be running!");
      running.set(false);
    }
  }

  /**
   * Stop the broker gracefully.
   */
  public void stop() {
    if (!running.get()) {
      logger.warn("Broker not running");
      return;
    }

    logger.info("Stopping SimpleBroker...");
    running.set(false);

    terminateContext();
    waitForProxyThreadTermination();

    logger.info("SimpleBroker stopped");
  }

  /**
   * Terminate the ZeroMQ context.
   */
  private void terminateContext() {
    if (context != null) {
      try {
        logger.info("Terminating ZMQ context...");
        context.term();
        logger.info("Context terminated");
      } catch (Exception e) {
        logger.warn("Error terminating context", e);
      }
    }
  }

  /**
   * Wait for proxy thread to terminate.
   */
  private void waitForProxyThreadTermination() {
    if (proxyThread != null && proxyThread.isAlive()) {
      try {
        logger.info("Waiting for proxy thread to finish...");
        proxyThread.join(SHUTDOWN_TIMEOUT_MS);

        if (proxyThread.isAlive()) {
          logger.warn("Proxy thread did not terminate within timeout");
          proxyThread.interrupt();
        }
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for proxy thread");
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Clean up all ZeroMQ resources.
   */
  private void cleanup() {
    logger.info("Cleaning up broker resources...");

    closeSocket(subscriber, "subscriber");
    closeSocket(publisher, "publisher");
    terminateContextIfNeeded();

    logger.info("SimpleBroker cleanup complete");
  }

  /**
   * Close a socket with error handling.
   */
  private void closeSocket(ZMQ.Socket socket, String socketName) {
    if (socket != null) {
      try {
        logger.debug("Closing {} socket...", socketName);
        socket.close();
        logger.debug("{} socket closed", capitalizeFirst(socketName));
      } catch (Exception e) {
        logger.warn("Error closing {} socket", socketName, e);
      }
    }
  }

  /**
   * Terminate context if not already terminated.
   */
  private void terminateContextIfNeeded() {
    if (context != null) {
      try {
        logger.debug("Terminating context (if not already terminated)...");
        context.term();
        logger.debug("Context terminated");
      } catch (Exception e) {
        logger.debug("Context termination: {}", e.getMessage());
      }
      context = null;
    }
  }

  /**
   * Handle ZeroMQ exceptions with appropriate logging.
   */
  private void handleZMQException(ZMQException e, String operation) {
    logger.error("ZMQ error during {}: {} (code: {})",
            operation, e.getMessage(), e.getErrorCode(), e);
  }

  /**
   * Capitalize first letter of a string.
   */
  private String capitalizeFirst(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  // Getters and Setters

  public boolean isRunning() {
    return running.get();
  }

  public int getInPort() {
    return inPort;
  }

  public int getOutPort() {
    return outPort;
  }

  public void setInPort(int inPort) {
    validatePort(inPort);
    this.inPort = inPort;
  }

  public void setOutPort(int outPort) {
    validatePort(outPort);
    this.outPort = outPort;
  }

  /**
   * Validate port number is within valid range.
   */
  private void validatePort(int port) {
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Invalid port number: " + port);
    }
  }
}