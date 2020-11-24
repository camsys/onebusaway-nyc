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

package org.onebusaway.nyc.transit_data_federation.impl.bundle;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleStoreUtil {
	
	private static Logger _log = LoggerFactory.getLogger(BundleStoreUtil.class);
	
	public void deleteBundle(String bundleRootPath, String bundleId) throws Exception {
		// ensure bundle path exists locally
		final File bundleRoot = new File(bundleRootPath, bundleId);

		if (!bundleRoot.exists()) {
			throw new Exception("Deletion of bundle " + bundleId + " at "
					+ bundleRootPath + " failed, bundle does not exist.");
		} else {
			_log.warn("Deleting Bundle: " + bundleRoot.getAbsolutePath());
			if(!deleteDirectory(bundleRoot)){
				_log.error("Unable to delete bundle: " + bundleRoot.getAbsolutePath());
			}
		}
	}

	public static boolean deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] children = dir.listFiles();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDirectory(children[i]);
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}
}
