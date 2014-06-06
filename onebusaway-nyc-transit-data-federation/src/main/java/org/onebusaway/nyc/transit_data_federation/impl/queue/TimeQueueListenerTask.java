package org.onebusaway.nyc.transit_data_federation.impl.queue;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.queue.QueueListenerTask;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public abstract class TimeQueueListenerTask extends QueueListenerTask {

  protected abstract void processResult(FeedMessage message);
  
  	private String _queueHost = null;
  	private String _queueName = "time";
  	private Integer _queuePort = 5569;
  	protected boolean _xmlOverrideUseTimePredictions = false;
  	
  	//setters for overriding queue values via spring configuration
  	public void setQueueHost(String host){
  		_queueHost = host;
  	}
  	public void setQueuePort(Integer port){
  		_queuePort = port;
  	}
  	public void setQueueName(String name){
  		_queueName = name;
  	}
  	public void setUseTimePredictions(boolean useTimePredictionsXMLOverrideOption){
  		_xmlOverrideUseTimePredictions = useTimePredictionsXMLOverrideOption;
  	}

	
	@Override
	public boolean processMessage(String address, byte[] buff) {
		try {
			if (address == null || buff == null || !address.equals(getQueueName()) || !useTimePredictionsIfAvailable()) {
				return false;
			}
			
			processResult(FeedMessage.parseFrom(buff));
			return true;
		} catch (Exception e) {
		  _log.warn("exception:", e);
			_log.warn("Received corrupted message from queue; discarding: " + e.getMessage());
			return false;
		}
	}
	
	@Override
	public String getQueueHost() {
		if(_xmlOverrideUseTimePredictions){
			return _queueHost;
		}
		return _configurationService.getConfigurationValueAsString("tds.timePredictionQueueHost", _queueHost);
	}

	@Override
	public String getQueueName() {
		if(_xmlOverrideUseTimePredictions){
			return _queueName;
		}
		return _configurationService.getConfigurationValueAsString("tds.timePredictionQueueName", _queueName);
	}

	@Override
	public Integer getQueuePort() {
		if(_xmlOverrideUseTimePredictions){
			return _queuePort;
		}
		return _configurationService.getConfigurationValueAsInteger("tds.timePredictionQueueOutputPort", _queuePort);
	}
	
  @Override
  public String getQueueDisplayName() {
    return "timePrediction";
  }
  
  private String disable = "false";
  public void setDisable(String disable) {
    this.disable = disable;
  }
  
  //Central authority method on determining if time-based-predictions are enabled
  @Refreshable(dependsOn = { "display.useTimePredictions" })
  public Boolean useTimePredictionsIfAvailable() {
	if(Boolean.TRUE.equals(this._xmlOverrideUseTimePredictions)) return true;
    if (Boolean.TRUE.equals(Boolean.parseBoolean(disable))) return false;
    return Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("display.useTimePredictions", "false"));
  }
  
	@Refreshable(dependsOn = { "tds.timePredictionQueueHost", "tds.timePredictionQueuePort", "tds.timePredictionQueueName" })
	public void startListenerThread() {
		if (_initialized == true) {
			_log.warn("Configuration service tried to reconfigure prediction input queue reader; this service is not reconfigurable once started.");
			return;
		}

		if (!useTimePredictionsIfAvailable()) {
		  _log.error("time predictions disabled -- exiting");
		  return;
		}
		
		_queueHost = getQueueHost();
		_queueName = getQueueName();
		_queuePort = getQueuePort();

		if (_queueHost == null) {
			_log.info("Prediction input queue is not attached; input hostname was not available via configuration service.");
			return;
		}

		_log.info("time prediction input queue listening on " + _queueHost + ":" + _queuePort + ", queue=" + _queueName);

		try {
			initializeQueue(_queueHost, _queueName, _queuePort);
		} catch (InterruptedException ie) {
			return;
		}

		_initialized = true;
	}

}
