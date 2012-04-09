package org.onebusaway.nyc.queue_http_proxy;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onebusaway.nyc.queue.IPublisher;
import org.onebusaway.nyc.queue.Publisher;

/**
 * HTTP Proxy Servet. Listens for HTTP Posts and blindly throws the content of
 * the post onto the configured topic queue.
 * 
 * The queue is configured by 4 parameters:
 * <ul>
 * <li>queue_topic: the name of the queue topic to publish to
 * <li>queue_protocol: the protcol to send messages on (e.g., tcp)
 * <li>queue_host: the interface to bind to (e.g., * to listen on all
 * interfaces)
 * <li>queue_port: the port to bind to.
 * </ul>
 * Defaults are provided for testing of bhs_queue, tcp://*:5563
 */
public class BHSListenerServlet extends HttpServlet {

	private static final long serialVersionUID = 245140554274414196L;
	public static final String PUBLISHER_KEY = "bhs_publisher";
	public static final String DEFAULT_BHS_QUEUE = "bhs_queue";
	private static final int CHUNK_SIZE = 4096;
	private static final String DEFAULT_PROTOCOL = "tcp";
	private static final String DEFAULT_HOST = "*";
	private static final int DEFAULT_PORT = 5563;

	public synchronized void init() throws ServletException {
		IPublisher publisher = (IPublisher) getServletConfig()
				.getServletContext().getAttribute(PUBLISHER_KEY);
		// don't assume we are first invocation, according to Servlet spec
		if (publisher == null) {
			String topic = getInitParameter("queue_topic", DEFAULT_BHS_QUEUE);
			if (topic == null) {
				topic = DEFAULT_BHS_QUEUE;
			}
			String protocol = getInitParameter("queue_protocol",
					DEFAULT_PROTOCOL);
			String host = getInitParameter("queue_host", DEFAULT_HOST);
			int port = getInitParameter("queue_port", DEFAULT_PORT);

			publisher = new Publisher(topic);
			// todo pull this out to config param
			publisher.open(protocol, host, port); 
									
			// lazy instantiate and save with application context
			getServletConfig().getServletContext().setAttribute(PUBLISHER_KEY,
					publisher);
		}
	}

	/**
	 * Politely clean up.
	 */
	public synchronized void destroy() {
		IPublisher publisher = (IPublisher) getServletConfig()
				.getServletContext().getAttribute(PUBLISHER_KEY);
		if (publisher != null) {
			publisher.close();
			getServletConfig().getServletContext().removeAttribute(
					PUBLISHER_KEY);
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		service(request.getInputStream());
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// do not respond to get requests
	}

	private void service(ServletInputStream stream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(CHUNK_SIZE);
		byte[] buff = new byte[CHUNK_SIZE];
		int noData = stream.readLine(buff, 0, CHUNK_SIZE);
		/*
		 * messages are expected to be small so in-memory processing is not an
		 * issue.
		 */
		while (noData != -1) {
			baos.write(buff, 0, noData);
			noData = stream.readLine(buff, 0, CHUNK_SIZE);
		}
		enqueue(baos.toByteArray());
	}

	private void enqueue(byte[] message) {
		IPublisher publisher = (IPublisher) getServletConfig()
				.getServletContext().getAttribute(PUBLISHER_KEY);
		publisher.send(message);
	}

	/**
	 * convenience method to retrieve and init parameter or return the default
	 * value otherwise.
	 */
	private String getInitParameter(String key, String defaultValue) {
		ServletContext context = getServletConfig().getServletContext();
		String value = context.getInitParameter(key);
		if (key == null) {
			value = defaultValue;
		}
		return value;
	}

	/**
	 * convenience method to retrieve and init parameter or return the default
	 * value otherwise. This method overrides based on type.
	 */
	private int getInitParameter(String key, int defaultValue) {
		ServletContext context = getServletConfig().getServletContext();
		String value = context.getInitParameter(key);
		int valueAsInt = defaultValue;
		if (key != null) {
			try {
				valueAsInt = Integer.parseInt(value);
			} catch (NumberFormatException nfe) {
				valueAsInt = defaultValue;
			}
		}
		return valueAsInt;
	}

}