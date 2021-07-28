package com.veeva.vault.custom.common.utility;

import java.util.List;
import java.util.Map;

import com.veeva.vault.sdk.api.core.ErrorResult;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.queue.Message;
import com.veeva.vault.sdk.api.queue.PutMessageResponse;
import com.veeva.vault.sdk.api.queue.QueueService;

@UserDefinedClassInfo()
public class Queue {
	
	private String queueName;
	private Map<String,String>propMap;
	private List<String> idList;
	private int max;
	
	@SuppressWarnings("unchecked")
	public Queue(Map<String,String> mIn,String qName,int m) {
		queueName = qName;
		propMap = mIn;
		max = m;
		idList = VaultCollections.newList();
	}
	
	@SuppressWarnings("unchecked")
	public List<String> addId(String id) {
		List<String> ret = null;
		idList.add(id);
		if(idList.size() == max) {
			queueMessage(idList,propMap,queueName);
			ret = idList;
			idList = VaultCollections.newList();
		}
		return ret;
	}
	
	public static String truncate(String s,int size) {
		String ret = s;
		if(ret.length() > size)
			ret = ret.substring(0,size);
		return ret;
	}
	
	public List<String> close() {
		List<String>ret = null;
		if(idList.size() > 0) {
			queueMessage(idList,propMap,queueName);
			ret = idList;
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static void queueMessage(List<String> id, String obj,String queue) {
		Map<String,String>attMap = VaultCollections.newMap();
		attMap.put("object", obj);
		queueMessage(id,attMap,queue);
	}
	
	public static void queueMessage(List<String> id, Map<String,String> attributes,String queue,int split) {
		while(id.size() > split) {
			queueMessage(id.subList(0, split), attributes,queue);
			id = id.subList(split, id.size());
		}
		queueMessage(id, attributes,queue);
	}
	
	public static void queueMessage(List<String> id, Map<String,String> attributes,String queue) {
		if(id.size() > 0) {
			QueueService queueService = ServiceLocator.locate (QueueService.class);
	    	Message message = queueService.newMessage(queue)
	        	.setMessageItems(id);
	    	for(String s : attributes.keySet()) {
	    		message.setAttribute(s, attributes.get(s));
	    	}
	    	PutMessageResponse response = queueService.putMessage(message);
	    	if(response.getError() != null) {
	    		ErrorResult er = response.getError();
	    		Log.throwError("Failed to queue with err:" + er.getMessage());
	    	}else {
	    		Log.debug("Queued to " + queue + ":" + String.join(",", id));
	    	}
		}
	}
}