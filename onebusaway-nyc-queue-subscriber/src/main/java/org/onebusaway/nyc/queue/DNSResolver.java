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

	public boolean isPrimary() {
		InetAddress newAddress = getInetAddressByName(host);
		if (newAddress == null) {
			_log.warn("Primary host did not resolve, assuming primary.  host=" + host);
			return true;
		}
		try {
			if (InetAddress.getLocalHost().equals(newAddress)) { // compares IP
				return true;
			}
		} catch (UnknownHostException uhe) {
			_log.error("misconfigured host, unable to resolve localhost");
			_log.error(uhe.toString());
		}
		return false;
	}

	public InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException uhe) {
			_log.error(uhe.toString());
		}
		return null;
	}

	public String getLocalHostString() {
		InetAddress local = getLocalHost();
		if (local != null) {
			return local.toString();
		}
		return "unknown";
	}

	public InetAddress getInetAddressByName(String host) {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(host);
		} catch (UnknownHostException uhe) {
			System.out.println("unknown host=" + host);
		}
		return address;
	}


}