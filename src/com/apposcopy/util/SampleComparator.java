package com.apposcopy.util;

import java.util.Comparator;

import com.apposcopy.model.Sample;

public class SampleComparator implements Comparator<Sample> {

	@Override
	public int compare(Sample o1, Sample o2) {
		
		int o1_size = o1.getActivities().size()+o1.getDangerAPIs().size()+o1.getIntentFilters().size()+o1.getTaintFlows().size();
		int o2_size = o2.getActivities().size()+o2.getDangerAPIs().size()+o2.getIntentFilters().size()+o2.getTaintFlows().size();

		if (o1_size < o2_size){
			return -1;
		} else if (o1_size > o2_size)
			return 1;
		else 
			return 0;
	}
	
}
