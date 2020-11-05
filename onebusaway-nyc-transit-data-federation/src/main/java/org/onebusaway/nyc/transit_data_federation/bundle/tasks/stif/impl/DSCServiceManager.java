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

package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DSCServiceManager {
	private Set<String> _notInServiceDscs = new HashSet<String>();
	public void setNotInServiceDsc(String notInServiceDsc) {
		_notInServiceDscs.add(notInServiceDsc);
	}
	
	public void setNotInServiceDscs(List<String> notInServiceDscs) {
		_notInServiceDscs.addAll(notInServiceDscs);
	}

	public void setNotInServiceDscs(Set<String> notInServiceDscs) {
		_notInServiceDscs.addAll(notInServiceDscs);
	}

	private File _notInServiceDscPath;
	public void setNotInServiceDscPath(File notInServiceDscPath) {
		_notInServiceDscPath = notInServiceDscPath;
	}
	
	public void readNotInServiceDscs() {
		if (_notInServiceDscPath != null) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(_notInServiceDscPath));
				String line = null;
				while ((line = reader.readLine()) != null)
					_notInServiceDscs.add(line);
				reader.close();
			} catch (IOException ex) {
				throw new IllegalStateException("unable to read nonInServiceDscPath: " + _notInServiceDscPath);
			}
		}
	}
	
	public Set<String> getNotInServiceDSCs(){
		return _notInServiceDscs;
	}
}
