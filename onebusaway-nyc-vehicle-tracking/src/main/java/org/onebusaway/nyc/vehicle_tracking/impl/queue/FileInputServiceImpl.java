package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("fileInputService")
public class FileInputServiceImpl extends InputServiceImpl implements
		InputService {

	@Override
	public String replaceMessageContents(String contents) {
		if(StringUtils.isNotBlank(contents)){
			contents = addSurroundingBrackets(contents);
			contents = replaceFirstStringOccurances(contents);
			contents = replaceAllStringOccurances(contents);
		}
		return contents;	
	}
	
	private String addSurroundingBrackets(String contents){
		if(contents.charAt(0) != '{')
			 contents = "{" + contents + "}";	
		return contents;
	}
	
	private String replaceFirstStringOccurances(String contents){
		return contents.replaceFirst("UUID.*UUID", "UUID");
	}
	
	private String replaceAllStringOccurances(String contents){
		String[] searchList = new String[] { "vehiclepowerstate" };
		String[] replacementList = new String[] { "vehiclePowerState" };
		return StringUtils.replaceEach(contents, searchList, replacementList);
	}
	
	@Override
	@PostConstruct
	public void setup() {
		super.setup();
	}

}
