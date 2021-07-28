package com.veeva.vault.custom.common.utility;

import java.util.List;
import java.util.Map;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.job.JobParameters;
import com.veeva.vault.sdk.api.job.JobService;

@UserDefinedClassInfo()
public class Workflow {
	
	public static final String RUA = "record_user_action__v";
	
	@SuppressWarnings("unchecked")
	public static void startWF(Record record, String wf,String action,Map<String,Object> params) {
		List<Record> recordList = VaultCollections.newList();
		recordList.add(record);
		startWF(recordList,wf,action,params);
	}
	
	public static void startWF(List<Record> records,String workflow, String action,Map<String,Object>params) {
		if(records.size() > 0) {
			//Get an instance of Job for invoking user actions, such as changing state and starting workflow
	        JobService jobService = ServiceLocator.locate(JobService.class);
	        JobParameters jobParameters = jobService.newJobParameters(workflow);
	        jobParameters.setValue("user_action_name", action); 
	        jobParameters.setValue("records", records);
	        for(String s : params.keySet()) {
	        	jobParameters.setValue(s, params.get(s));
	        }
	        jobService.run(jobParameters);
		}
	}

}