package com.veeva.vault.custom.common.utility;

import com.veeva.vault.sdk.api.core.RequestContextValue;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.job.JobCompletionContext;

@UserDefinedClassInfo()
public class JobUtil  implements RequestContextValue{
	public static void logCon(JobCompletionContext con) {
		int num = con.getJobResult().getNumberFailedTasks();
		con.getJobLogger().log(con.getJobId() + ":" + con.getJobResult().getNumberCompletedTasks() + ":" + num);
		if(num > 0) {
			String mess = "Job Ran With Failed Tasks:" + num;
			Log.createErrorRecord(con.getJobId(), mess, null);
		}
		con.getTasks().forEach(t -> {
			con.getJobLogger().log("State:"+t.getTaskOutput().getState().toString());
		});
		
	}
	
}