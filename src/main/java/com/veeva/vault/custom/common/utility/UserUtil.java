package com.veeva.vault.custom.common.utility;

import java.util.List;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.group.GetGroupsResponse;
import com.veeva.vault.sdk.api.group.Group;
import com.veeva.vault.sdk.api.group.GroupService;

@UserDefinedClassInfo()
public class UserUtil {
	@SuppressWarnings("unchecked")
	public static Group getGroupByName(String name) {
		List<String>nameList = VaultCollections.newList();
		nameList.add(name);
		List<Group> groupList = getGroupsByName(nameList);
		if(groupList == null || groupList.size() != 1)
			Log.throwError("Group " + name + " not found");
		return groupList.get(0);
	}
	
	@SuppressWarnings("unchecked")
	public static List<Group> getGroupsByName(List<String> names){
		GroupService gs = ServiceLocator.locate(GroupService.class);
		List<Group> ret = VaultCollections.newList();
		GetGroupsResponse ggr = gs.getGroupsByNames(names);
		for(String name : names)
			ret.add(ggr.getGroupByName(name));
		return ret;
	}
}