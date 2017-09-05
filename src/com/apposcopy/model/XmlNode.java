package com.apposcopy.model;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

public class XmlNode {

	String name;
	String permission;
	boolean isMain;
	String type;
	String intentFilter;
	List<String> actionList = new ArrayList<String>();
	// should be a set of integers.
	Set<String> filterPrilist = new HashSet<String>();

	public String getName() {
		return name;
	}

	public void setName(String s) {
		name = s;
	}

	public void setPermission(String s) {
		permission = s;
	}

	public String getPermission() {
		return permission;
	}

	public void setType(String s) {
		type = s;
	}

	public String getType() {
		return type;
	}

	public boolean getMain() {
		return isMain;
	}

	public void setMain(boolean s) {
		isMain = s;
	}

	public void setIntentFilter(String s) {
		intentFilter = s;
	}

	public String getIntentFilter() {
		return intentFilter;
	}

	public void addAction(String s) {
		actionList.add(s);
	}

	public List<String> getActionList() {
		return actionList;
	}

	public void addFilter(String s) {
		filterPrilist.add(s);
	}

	public Set<String> getFilterList() {
		return filterPrilist;
	}

	public String toString() {
		return "\nNode name: " + name + " type:" + type + " actionList: "
				+ actionList + " priorityList:" + filterPrilist;
	}

}
