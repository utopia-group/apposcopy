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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apposcopy.model.Sample;
import com.apposcopy.util.Util;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

/*
 * Signature Generation.
 *  
 */

public class SignatureGen {

	// FIXME: make if configurable later.
	protected static final boolean RANDOM = true;

	protected static  int RANDOMNUM = 5;
	
	public static void readFile(String file, Map<String,Integer> map){
		BufferedReader br = null;

		try {

			String sCurrentLine;

			br = new BufferedReader(new FileReader(file));

			while ((sCurrentLine = br.readLine()) != null) {
				String[] parts = sCurrentLine.split(" & ");
				map.put(parts[0], Integer.parseInt(parts[1]));
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		// Analyzing all samples in a given folder.
		if (args.length <= 4) {
			Gson gson = new Gson();
			List<Sample> samples = new ArrayList<>();
			List<String> allList = new ArrayList<>();
			List<String> currList = new ArrayList<>();
			String dir = args[0];
			final File folder = new File(dir);
			assert folder.isDirectory();
			String familyName = folder.getName();

			boolean encoding_basic = false;
			if (args[1].contains("basic"))
					encoding_basic = true;
			
				
			RANDOMNUM = Integer.parseInt(args[2]);	
			
			// list all json files.
			for (final File fileEntry : folder.listFiles()) {
				String filePath = dir + "/" + fileEntry.getName();
				allList.add(filePath);
			}

			if (RANDOM) {
				assert allList.size() >= RANDOMNUM;
				Collections.shuffle(allList);
				for (int i = 0; i < RANDOMNUM; i++) {
					currList.add(allList.get(i));
				}
			} else {
				// generate signature for all samples in the folder.
				currList.addAll(allList);
			}

			/// shuffling
			for (String file : currList) {
				System.out.println("Sample " + file);
				JsonReader reader = new JsonReader(new FileReader(file));
				Sample sample = gson.fromJson(reader, Sample.class);
				samples.add(sample);
			}
			
			HashMap<String,Integer> map = new HashMap<String,Integer>();
			String filename = args[3];
			readFile(filename,map);

			// invoke synthesizer.
			InferSignature is = new InferSignature(samples, encoding_basic, map);
			
			// print the formula to a file
			is.printFormula();

			// get signature
			is.get();
			
			// Persist current signature.
			Sample signature = is.getSignature();
			if (signature == null) {
				System.out.println("Fail to generate signature.");
				return;
			}
			
			if (signature.getActivities().size()+signature.getReceivers().size()+signature.getServices().size() == 0){
				System.out.println("No signature.");
				return;
			}
			
			signature.setSamples(currList);
			String sigJson = gson.toJson(signature);
			Util.persistJson(sigJson, familyName);

		} else {
			assert false : "unknown argument.";
		}

	}

}
