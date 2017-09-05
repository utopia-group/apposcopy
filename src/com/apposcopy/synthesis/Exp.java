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
package com.apposcopy.synthesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.apposcopy.model.Sample;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class Exp {

	public static final String jsonPath = "./data_list.txt";
	
	public static void main(String[] args) throws Exception {
		// Analyzing all samples in a given folder.
		Gson gson = new Gson();
		List<Sample> samples = new ArrayList<>();

		BufferedReader br = new BufferedReader(new FileReader(
				new File(jsonPath)));
		String line = null;
		while ((line = br.readLine()) != null) {
			if(line.startsWith("#"))
				continue;
			
			JsonReader reader = new JsonReader(new FileReader(line));
			Sample sample = gson.fromJson(reader, Sample.class);
			samples.add(sample);
		}
		System.out.println("# of samples: " + samples.size());

		br.close();
		// invoke synthesizer.
		HashMap<String,Integer> empty = new HashMap<String,Integer>();
		InferSignature is = new InferSignature(samples, false, empty);
		int max = 10;
		int i = 0;
		while (true) {
			System.out.println("model -------------------" + i);
			i++;
			List<String> models = is.get();
			for (String m : models)
				System.out.println(m);

			if (models.isEmpty())
				System.out.println("No more solutions!");

			if (models.isEmpty() || i > max || is.isConnected()) {
				break;
			}

			is.blockModel();
		}
	}

}
