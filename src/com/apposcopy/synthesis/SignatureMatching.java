package com.apposcopy.synthesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apposcopy.model.Sample;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

/*
 * Signature matching.
 */
public class SignatureMatching {

	public static void main(String[] args) throws IOException {
		// Analyzing all samples in a given folder.
		if (args.length == 2) {
			// arg0: signature in json
			// arg1: sample(s) in testing set: json file or directory that
			// contains json files.
			String sigPath = args[0];
			String testDir = args[1];
			Gson gson = new Gson();
			int succNum = 0;
			int failNum = 0;
			List<Sample> samples = new ArrayList<>();
			List<Sample> testSet = new ArrayList<>();
			Map<Sample, String> mapId = new HashMap<>();
			
			//read signature...
			JsonReader readerSig = new JsonReader(new FileReader(sigPath));
			Sample signature = gson.fromJson(readerSig, Sample.class);
			//first sample is the signature.
			samples.add(signature);

			final File folder = new File(testDir);
			// list all json files.
			if (folder.isDirectory()) {
				for (final File fileEntry : folder.listFiles()) {
					System.out.println("Adding test data----------:" + fileEntry);
					JsonReader reader = new JsonReader(new FileReader(fileEntry));
					Sample sample = gson.fromJson(reader, Sample.class);
					testSet.add(sample);
					mapId.put(sample, fileEntry.getName());
					reader.close();
				}
			} else {
				//only one sample.
				JsonReader reader = new JsonReader(new FileReader(folder));
				Sample sample = gson.fromJson(reader, Sample.class);
				System.out.println("Adding test data----------:" + folder);
				testSet.add(sample);
				mapId.put(sample, folder.getName());
				reader.close();
			}

			// invoke synthesizer.
			try {
				for (Sample test : testSet) {
					System.out.println("Matching----------" + mapId.get(test));
					MatchSignature is = new MatchSignature(signature, test);
					List<String> models = is.get();

					if (models.isEmpty()) {
						System.out.println("Fail.");
						failNum++;
					} else {
						System.out.println("Success.");
						succNum++;
					}
					for (String m : models)
						System.out.println(m);
				}
			} catch (Exception ex) {

			}
			
			System.out.println("Matching report==========================");
			System.out.println("Total: " + testSet.size());
			System.out.println("Success: " + succNum);
			System.out.println("Fail: " + failNum);
			System.out.println("Detection rate: " + (100.00 * succNum)/testSet.size() + "%");
			System.out.println("==========================================");

		} else {
			assert false : "unknown argument.";
		}

	}

}
