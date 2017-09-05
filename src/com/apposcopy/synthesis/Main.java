package com.apposcopy.synthesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.apposcopy.model.Sample;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class Main {

	public static void main(String[] args) throws FileNotFoundException {
		// Analyzing all samples in a given folder.
		if (args.length == 1) {
			Gson gson = new Gson();
			List<Sample> samples = new ArrayList<>();

			String dir = args[0];
			final File folder = new File(dir);
			// list all json files.
			for (final File fileEntry : folder.listFiles()) {
				String filePath = dir + "/" + fileEntry.getName();
				JsonReader reader = new JsonReader(new FileReader(filePath));
				Sample sample = gson.fromJson(reader, Sample.class);
				samples.add(sample);
			}

			// invoke synthesizer.
			HashMap<String,Integer> empty = new HashMap<String,Integer>();
			InferSignature is = new InferSignature(samples, false, empty);
			int max = 10;
			int i = 0;
			while (true) {
				System.out.println("model -------------------" + i);
				i++;
				List<String> models = is.get();
				for(String m : models) 
					System.out.println(m);
				
				if (models.isEmpty())
					System.out.println("No more solutions!");
				
				if(models.isEmpty() || i > max || is.isConnected()){
					break;
				}
					

				is.blockModel();
			}
			
			// try the signature on the first sample
			InferSignature isMatch = new InferSignature(is.getSignature(), samples.get(0));
			System.out.println("match model --------------");
			List<String> models = isMatch.get();
			for(String m : models) 
				System.out.println(m);
		} else {
			assert false : "unknown argument.";
		}

	}

}
