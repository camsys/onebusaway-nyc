package org.onebusaway.nyc.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility class to test if DNS resolution of a hostname has changed.
 */
public class DNSResolver {
  protected static Logger _log = LoggerFactory.getLogger(DNSResolver.class);
	private InetAddress currentAddress = null;
	private String host = null;

	public DNSResolver(String host) {
		this.host = host;
		currentAddress = getInetAddressByName(host);
	}

	public boolean hasAddressChanged() {
		InetAddress newAddress = getInetAddressByName(host);
		// test if not previously resolved
		if (currentAddress == null) {
			if (newAddress != null) {
				_log.warn("Previous unresolvable address resolved to " + newAddress);
				currentAddress = newAddress;
				return true;
			}
		} else if (!currentAddress.equals(newAddress)) {
			_log.warn("Resolver changed from " + currentAddress + " to " + newAddress);
			currentAddress = newAddress;
			return true;
		}
		return false;
	}

	public static InetAddress getInetAddressByName(String host) {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(host);
		} catch (UnknownHostException uhe) {
			System.out.println("unknown host=" + host);
		}
		return address;
	}


}