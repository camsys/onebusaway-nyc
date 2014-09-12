package org.onebusaway.nyc.admin.service;

import java.util.List;
import org.onebusaway.nyc.admin.service.impl.DiffTransformer;

public interface DiffService {
	List<String> diff(String filename1, String filename2);
	void setDiffTransformer(DiffTransformer diffTransformer);
}
