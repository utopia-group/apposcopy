package com.apposcopy.synthesis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sat4j.specs.ContradictionException;

import com.apposcopy.model.Sample;
import com.apposcopy.synthesis.sat4j.BasicEncoding;
import com.apposcopy.synthesis.sat4j.Encoding;
import com.apposcopy.synthesis.sat4j.SignatureEncoding;
import com.apposcopy.synthesis.sat4j.Solver;

public class InferSignature {
	protected Encoding encoding;
	//protected SignatureEncoding encoding;
	//protected BasicEncoding basic_encoding;
	protected Solver solver;
	protected boolean OK;
	protected boolean isRun = false;
	protected boolean res;
	protected boolean basic = false;

	protected int solver_limit = 5;
	
	public InferSignature(List<Sample> samples, boolean basic, HashMap<String,Integer> map) {
		solver = new Solver();
		this.basic = basic;
		if (basic) encoding = new BasicEncoding(samples, map);
		else encoding = new SignatureEncoding(samples);
		encoding.build();

		try {
			solver.build(encoding, basic);
			OK = true;
		} catch (ContradictionException e) {
			OK = false;
			e.printStackTrace();
		}
	}
	
	public InferSignature(Sample signature, Sample sample) {
		solver = new Solver();
		
		encoding = new SignatureEncoding(signature, sample);
		encoding.build();
		
		try {
			solver.build(encoding, basic);
			OK = true;
		} catch (ContradictionException e) {
			OK = false;
			e.printStackTrace();
//			assert false;
		}
	}

//	public SignatureEncoding getEncoding() {
//		return encoding;
//	}
//	
	public boolean isBasic() {
		return basic;
	}
	
	public Encoding getEncoding() {
		return encoding;
	}
	
	public boolean isConnected(){
		return encoding.isConnected();
	}

	public Solver getSolver() {
		return solver;
	}
	
	public void printFormula() {
		encoding.printFormula();
	}
	
	public List<String> get() {
		//if(!isRun) {
		//	isRun = true;
			if (!OK)
				return new ArrayList<String>();
	
			OK = false;
			
			String output_solver = new String();
	        try {
	        	Runtime r = Runtime.getRuntime();
	        	Process p = r.exec("./opb/open-wbo-linux ./opb/malware.opb");
	        	try {
	        		BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
		        	String line = "";

		        	while ((line = b.readLine()) != null) {
		        		if (line.startsWith("s OPTIMUM FOUND") || line.startsWith("s SATISFIABLE")){
		        			res = true;
		        		} 
		        		
		        		if (line.startsWith("s UNSATISFIABLE")){
		        			res = false;
		        		}
		        		
		        		if (line.startsWith("v ")){
		        			output_solver = line;
		        		}
		        	  //System.out.println(line);
		        	}

		        	b.close();
					p.waitFor();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
			//res = solver.solve();
			if (!res) OK = false;
			else OK = true;
		//}
		return res ? encoding.saveModel(solver, output_solver) : new ArrayList<String>();
	}
	
	public Sample getSignature() {
		// Why do we need to call get again?
		//List<String> models = get();
		//no model.
		//if(models.size() == 0)
		//	return null;
		
		if (!OK) return null;
		
		return encoding.getSignature(solver);
	}
	
	public void blockModel() {
		//if(!isConnected()) {
			solver.blockGraph(encoding.getBlockedGraph());
		//} 
		
//		solver.blockModel(encoding.getBlockVars());
	}
	
	public boolean isOK() {
		return OK;
	}
}
