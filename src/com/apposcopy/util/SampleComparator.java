/*
 * Copyright (C) 2017 The Apposcopy and Astroid Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
