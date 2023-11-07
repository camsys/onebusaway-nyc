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

package org.onebusaway.nyc.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.onebusaway.cloud.api.ExternalServices;
import org.onebusaway.cloud.api.ExternalServicesBridgeFactory;

/**
 * Utility class to test if DNS resolution of a hostname has changed.
 */
public class LeadershipElectionResolver {
	protected static Logger _log = LoggerFactory.getLogger(LeadershipElectionResolver.class);
	private ExternalServices externalServices = new ExternalServicesBridgeFactory().getExternalServices();

	boolean primaryHasChanged = false;
	boolean isPrimary;


	/**
	 * Tests if this host is at the IP corresponding to the DNS address.
	 */
	public boolean isPrimary() {
		boolean result = externalServices.isInstancePrimary();
		primaryHasChanged = (result == isPrimary);
		isPrimary = result;
		return isPrimary;
	}

	/**
	 * convenience null-safe wrapper for InetAddress.getLocalhost.
	 */
	public InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException uhe) {
			_log.error(uhe.toString());
		}
		return null;
	}

	/**
	 * null-safe toString of InetAddress.getLocalhost.
	 */
	public String getLocalHostString() {
		InetAddress local = getLocalHost();
		if (local != null) {
			return local.toString();
		}
		return "unknown";
	}

public boolean getPrimaryHasChanged(){
	return primaryHasChanged;
	}


}