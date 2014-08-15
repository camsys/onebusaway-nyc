package org.onebusaway.nyc.admin.service.impl;

import java.util.List;

public interface DiffTransformer{
	List<String> transform(List<String> preTransform);
}
