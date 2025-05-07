package org.onebusaway.nyc.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ZeromqQueueProvider implements MessageQueueProvider {
    private static Logger _log = LoggerFactory.getLogger(ZeromqQueueProvider.class);

    private ZMQ.Context _context;
    private ZMQ.Socket _socket;
    private ZMQ.Poller _poller;
    private Thread readerThread;
    private final BlockingQueue<QueueMessage> handoff = new LinkedBlockingQueue<>(20000);

    private volatile boolean initialized = false;

    private int processedCount = 0;
    private int _countInterval = 10000;

    private String host;
    private String queueName;
    private Integer port;
    private Date markTimestamp = new Date();


    @Override
    public synchronized void initialize(String host,
                                        String queueName,
                                        Integer port) throws InterruptedException {

        this.host = host;
        this.queueName = queueName;
        this.port = port;

        if (initialized) {
            _log.info("Re-initializing ZeroMQ provider - shutting down old connection first");
            shutdown();
        }

        initializeZmq();

        readerThread = new Thread(this::readIncoming, "zmq-reader");
        readerThread.start();

        initialized = true;
    }

    private void initializeZmq() {
        if (_context == null) {
            _context = ZMQ.context(1);
        }

        _socket = _context.socket(ZMQ.SUB);
        String bind = "tcp://" + host + ":" + port;
        _log.info("binding to " + bind + " with topic=" + queueName);
        _socket.connect(bind);
        _socket.subscribe(queueName.getBytes());

        _poller = _context.poller(2);
        _poller.register(_socket, ZMQ.Poller.POLLIN);
    }

    private void readIncoming() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                _poller.poll(100);
                if (_poller.pollin(0)) {
                    String addr = _socket.recvStr();
                    byte[] data = _socket.recv();
                    handoff.put(new QueueMessage(addr, data));
                    processedCount++;
                }
                if (processedCount > _countInterval) {
                    long timeInterval = (new Date().getTime() - markTimestamp.getTime());
                    _log.info(queueName
                            + " input queue: processed " + _countInterval + " messages in "
                            + (timeInterval/1000)
                            + " seconds. (" + (1000.0 * processedCount/timeInterval)
                            + ") records/second");

                    markTimestamp = new Date();
                    processedCount = 0;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            _log.info("ZeroMQ read loop exiting");
        }
    }

    @Override
    public synchronized void shutdown() {
        if (!initialized) return;

        readerThread.interrupt();
        try {
            readerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (_socket != null) _socket.close();
        if (_context != null) _context.close();

        handoff.clear();

        initialized = false;
        _log.info("ZeroMQ provider shut down");
    }

    @Override
    public QueueMessage nextMessage() throws InterruptedException {
        return handoff.take();
    }
}

