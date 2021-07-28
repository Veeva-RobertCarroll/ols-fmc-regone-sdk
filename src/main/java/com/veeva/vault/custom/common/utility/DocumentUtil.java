package com.veeva.vault.custom.common.utility;

import java.util.List;

import com.veeva.vault.sdk.api.core.BatchOperationError;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.document.DocumentService;
import com.veeva.vault.sdk.api.document.DocumentVersion;
import com.veeva.vault.sdk.api.role.DocumentRoleService;
import com.veeva.vault.sdk.api.role.DocumentRoleUpdate;

@UserDefinedClassInfo
public class DocumentUtil {
	@SuppressWarnings("unchecked")
	public static List<String> saveDocuments(List<DocumentVersion> recordListIn) {
		List<String> ret = VaultCollections.newList();
		if(recordListIn.size() > 0) {
			Log.debug("Writing documents:" + recordListIn.size());
			List<DocumentVersion> recordList = VaultCollections.newList();
			DocumentService ds = ServiceLocator.locate(DocumentService.class);
			for(int i = 0;i<recordListIn.size();i++) {
				recordList.add(recordListIn.get(i));
				if(i % 500 == 0 || i == recordListIn.size() - 1) {
					
					ds.saveDocumentVersions(recordList).getSuccesses().forEach(pdv -> {
						ret.add(pdv.getDocumentVersionId());
					});
					recordList = VaultCollections.newList();
				}
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public static List<String> saveDocumentRoles(List<DocumentRoleUpdate> recordListIn) {
		List<String> ret = VaultCollections.newList();
		if(recordListIn.size() > 0) {
			Log.debug("Updating Document Roles:" + recordListIn.size());
			List<DocumentRoleUpdate> recordList = VaultCollections.newList();
			DocumentRoleService drs = ServiceLocator.locate(DocumentRoleService.class);
			for(int i = 0;i<recordListIn.size();i++) {
				recordList.add(recordListIn.get(i));
				if(i % 500 == 0 || i == recordListIn.size() - 1) {
					drs.batchUpdateDocumentRoles(recordList).onErrors(err -> {
						String errStr = "";
						for(BatchOperationError e : err) {
							errStr += e.getError().toString();
						}
						Log.throwError("Update error:" + errStr);
					}).execute();
				}
			}
		}
		return ret;
	}
}