package com.apposcopy.synthesis.sat4j;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.OptToPBSATAdapter;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

public class Solver {

	/**
	 * Members of Encoding class
	 */
	// protected IPBSolver solver;
	protected IPBSolver solver;
	protected ArrayList<Boolean> model;
	protected ArrayList<Integer> assumptions;
	//protected Constraint objective_function;
	protected IConstr objective_blocking;
	protected int assumption_var;
	
	private static int MAX_ITERATIONS = 5;

	private static final Logger LOG = Logger.getLogger(Solver.class.getName());

	/**
	 * Constructors
	 */
	public Solver() {
		solver = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));
		model = new ArrayList<Boolean>();
		assumptions = new ArrayList<Integer>();
		//objective_function = null;
		assumption_var = 0;
		objective_blocking = null;
	}
	
	public IPBSolver getSolver() {
		return solver;
	}

	/**
	 * Public methods
	 */
	public ArrayList<Boolean> getModel() {
		return model;
	}

	public void update(Constraint c) throws ContradictionException {
		addConstraint(c);
	}
	
	public int getMaxIterations(){
		return MAX_ITERATIONS;
	}
	
	public void setMaxIterations(int itn){
		MAX_ITERATIONS = itn;
	}
	
	public void build(Encoding encoding, boolean basic) throws ContradictionException{
		if (basic) buildBasic((BasicEncoding)encoding);
		else buildEmbedding((SignatureEncoding)encoding);
	}
	
	public void buildBasic(BasicEncoding encoding) throws ContradictionException {

		// rebuild the solver
		solver = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));
		solver.setTimeout(3600);

		// create variables in the solver
		solver.newVar(encoding.nVars());
		
		// add constraints
		for (int i = 0; i < encoding.nConstraints(); i++) {
			Constraint c = encoding.getConstraints().get(i);
			addConstraint(c);
		}
		
		// add objective
		VecInt constraint = new VecInt();
		Vec<BigInteger> coefficients = new Vec<BigInteger>();
		
		// lexicographical optimization
		List<Integer> weightFunctions = new ArrayList<Integer>();
		for (int i = 0; i < encoding.getObjectiveFunctions().size(); i++)
			weightFunctions.add(0);
		
		int total = 0;
		for (int i = encoding.getObjectiveFunctions().size()-1; i >= 1; i--){
			int weight = total;
			for (int j = 0; j < encoding.getObjectiveFunctions().get(i).getCoefficients().size(); j++){
				weight += encoding.getObjectiveFunctions().get(i).getCoefficients().get(j);
				total += encoding.getObjectiveFunctions().get(i).getCoefficients().get(j);
			}
			total++;
			weightFunctions.set(i-1, total);
		}
		
		
		for (int i = 0; i < encoding.getObjectiveFunctions().size(); i++){
//			int weight = 1;
//			for (int j = i+1; j < encoding.getObjectiveFunctions().size(); j++){
//				weight += encoding.getObjectiveFunctions().get(j).getSize();
//			}
//			System.out.println("Weight: " + weight);
//			
			for (int j = 0; j < encoding.getObjectiveFunctions().get(i).getCoefficients().size(); j++){
				constraint.insertFirst(encoding.getObjectiveFunctions().get(i).getLiterals().get(j).getSolverId()+1);
				//coefficients.insertFirst(BigInteger.valueOf(-weight));
				int weight = -(weightFunctions.get(i)+encoding.getObjectiveFunctions().get(i).getCoefficients().get(j));
				coefficients.insertFirst(BigInteger.valueOf(weight));
			}
		}
		
		
//		for (int i = 0; i < encoding.getObjectiveFunctions().size(); i++){
//			for (int j = 0; j < encoding.getObjectiveFunctions().get(i).getCoefficients().size(); j++){
//				constraint.insertFirst(encoding.getObjectiveFunctions().get(i).getLiterals().get(j).getSolverId()+1);
//				coefficients.insertFirst(BigInteger.valueOf(-encoding.getObjectiveFunctions().get(i).getCoefficients().get(j)));
//			}
//		}
		
		solver.setObjectiveFunction(new ObjectiveFunction(constraint, coefficients));


	}
	
	public void buildMatch(MatchingEncoding encoding) throws ContradictionException {

		// rebuild the solver
		solver = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));
		solver.setTimeout(3600);

		// create variables in the solver
		solver.newVar(encoding.nVars());
		
		// add constraints
		for (int i = 0; i < encoding.nConstraints(); i++) {
			Constraint c = encoding.getConstraints().get(i);
//			System.out.println("------------------------");
//			c.print();
			addConstraint(c);
		}

		// add objective
		VecInt constraint = new VecInt();
		Vec<BigInteger> coefficients = new Vec<BigInteger>();
		for (int i = 0; i < encoding.getObjectiveFunctions().size(); i++){
			int weight = 1;
			for (int j = i+1; j < encoding.getObjectiveFunctions().size(); j++){
				weight += encoding.getObjectiveFunctions().get(j).getSize();
			}
			//System.out.println("Weight: " + weight);
			
			for (int j = 0; j < encoding.getObjectiveFunctions().get(i).getCoefficients().size(); j++){
				constraint.insertFirst(encoding.getObjectiveFunctions().get(i).getLiterals().get(j).getSolverId()+1);
				coefficients.insertFirst(BigInteger.valueOf(-weight));
			}
		}
		
		solver.setObjectiveFunction(new ObjectiveFunction(constraint, coefficients));
//		System.out.println("Real variables: " + solver.nVars());
//		System.out.println("Real constraints: " + solver.nConstraints());
		

//		LOG.info("Objective Function #" + encoding.getObjectiveId() + " Variables #" + solver.nVars() + " Constraints #"
//				+ solver.nConstraints());

	}

	public void buildEmbedding(SignatureEncoding encoding) throws ContradictionException {

		// rebuild the solver
		solver = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));
		solver.setTimeout(3600);

		// create variables in the solver
		solver.newVar(encoding.nVars());
		
		// add constraints
		for (int i = 0; i < encoding.nConstraints(); i++) {
			Constraint c = encoding.getConstraints().get(i);
//			System.out.println("------------------------");
//			c.print();
			addConstraint(c);
		}
		
		ArrayList<Integer> obj_values = new ArrayList<Integer>();
		for (int i = 0; i < encoding.getObjectiveFunctions().size(); i++)
			obj_values.add(1);
		
		int sum = 0;
		for (int i = encoding.getObjectiveFunctions().size()-2; i >= 0; i--){
			obj_values.set(i, obj_values.get(i+1)*encoding.getObjectiveFunctions().get(i+1).getSize()+1+sum);
			sum += obj_values.get(i+1)*encoding.getObjectiveFunctions().get(i+1).getSize();
		}

		// add objective
		VecInt constraint = new VecInt();
		Vec<BigInteger> coefficients = new Vec<BigInteger>();
		for (int i = 0; i < encoding.getObjectiveFunctions().size(); i++){
			int weight = obj_values.get(i);
			for (int j = 0; j < encoding.getObjectiveFunctions().get(i).getCoefficients().size(); j++){
				constraint.insertFirst(encoding.getObjectiveFunctions().get(i).getLiterals().get(j).getSolverId()+1);
				coefficients.insertFirst(BigInteger.valueOf(-weight));
			}
		}
		
		solver.setObjectiveFunction(new ObjectiveFunction(constraint, coefficients));
//		System.out.println("Real variables: " + solver.nVars());
//		System.out.println("Real constraints: " + solver.nConstraints());
		

//		LOG.info("Objective Function #" + encoding.getObjectiveId() + " Variables #" + solver.nVars() + " Constraints #"
//				+ solver.nConstraints());

	}
	
	public boolean solve() {
		try {
			boolean res = solver.isSatisfiable();

			//System.out.println("res: " + res);
			if(res) {
				// saves the model
				int len = solver.model().length;
				Boolean[] modelArray = new Boolean[len];
				for (int i = 0; i < len; ++i) {
					modelArray[i] = solver.model(i + 1);
				}
				model = new ArrayList<Boolean>(Arrays.asList(modelArray));
			}
			return res;
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
//		int itn = 0;
//		model.clear();
//		
//		if(assumption_var != 0){
//			try {
//				solver.addClause(new VecInt(new int[] {assumption_var}));
//			} catch (ContradictionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			assumption_var = 0;
//		}
//		
//		try {
//
//			while (itn < MAX_ITERATIONS) {
//
//				boolean res = false;
//
//				solver.setTimeout(3600); // Timeout for the SAT solver
//				
//				if (assumption_var == 0) {
//					 res = solver.isSatisfiable();
//				}
//				else {
//						res = solver.isSatisfiable(new VecInt(new int[] {-assumption_var}));
//				}
//
//				if (res) {
//					
//					//System.out.println("model at itn: " + itn);
//					// saves the model
//					int len = solver.model().length;
//					Boolean[] modelArray = new Boolean[len];
//					for (int i = 0; i < len; ++i) {
//						modelArray[i] = solver.model(i + 1);
//					}
//					model = new ArrayList<Boolean>(Arrays.asList(modelArray));
//
//					// updates the objective function
//					if (objective_function.getSize() > 0) {
//
//						int k = getOptimum(objective_function);
//						//System.out.println("Objective value: " + k);
//						
//						if (assumption_var != 0){
//							try {
//								solver.addClause(new VecInt(new int[] {assumption_var}));
//							} catch (ContradictionException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
//							
//
//						int sum = 0;
//						for (Integer l : objective_function.getCoefficients()) {
//							if (l > 0)
//								sum += l;
//						}
//
//						ArrayList<Variable> obj = new ArrayList<Variable>();
//						obj.addAll(objective_function.getLiterals());
//
//						ArrayList<Integer> coeff = new ArrayList<Integer>();
//						coeff.addAll(objective_function.getCoefficients());
//
//						Constraint c = new Constraint(obj, coeff, ConstraintType.LEQ, k - 1);
//						
//						 try {
//								 objective_blocking = addConstraint(c);
//						 } catch (ContradictionException e) {
//
//							 return true;
//						 }
//						
//					} else {
//						break;
//					}
//
//				} else {
//						
//						if (!model.isEmpty()) {
//							return true;
//						} else
//							return false;
//					//System.out.println("UNSAT at itn: " + itn);
//					
//				}
//				itn++;
//			}
//
//		} catch (TimeoutException e) {
//			LOG.warning("Timeout in the SAT solver.");
//			return false;
//		}

//		return true;
	}

	public IConstr addConstraint(Constraint c) throws ContradictionException {

		int size = c.getSize();
		int[] constraint = new int[size];
		int[] coefficients = new int[size];
		for (int j = 0; j < size; j++) {
			assert c.getLiterals() != null;
			assert c.getLiterals().get(j) != null : c.getLiterals().size() + " v.s." + j;
			constraint[j] = c.getLiterals().get(j).getSolverId() + 1;
			coefficients[j] = c.getCoefficients().get(j);
			assert(constraint[j] > 0 && constraint[j] <= solver.nVars());
		}
		
		switch (c.getType()) {
		case EQ:
			return solver.addExactly(new VecInt(constraint), new VecInt(coefficients), c.getRhs());
		case GEQ:
			return solver.addAtLeast(new VecInt(constraint), new VecInt(coefficients), c.getRhs());
		case LEQ:{
//			c.print();
			return solver.addAtMost(new VecInt(constraint), new VecInt(coefficients), c.getRhs());
		}
		default:
			assert(false);
		}
		return null;
	}

	/**
	 * Protected methods
	 */
//	protected void addObjectiveConstraints(SignatureEncoding encoding) throws ContradictionException {
//
//		boolean encode = false;
//
//		Constraint c = encoding.getObjectiveFunctions();
//		int size = 0;
//		for (Integer coeff : c.getCoefficients()) {
//			if (coeff != 0) {
//				encode = true;
//				size++;
//			}
//		}
//
//		if (encode) {
//			solver.setObjectiveFunction(convertObjectiveFunction(c, size));
//		}
//
//	}

	protected ObjectiveFunction convertObjectiveFunction(Constraint c, int size) {

		int[] objective = new int[size];
		BigInteger[] coefficients = new BigInteger[size];

		assert(c.getCoefficients().size() == c.getLiterals().size());

		int pos = 0;

		for (int i = 0; i < c.getSize(); i++) {
			if (c.getCoefficients().get(i) == 0)
				continue;
			objective[pos] = c.getLiterals().get(i).getSolverId() + 1;
			assert(objective[pos] > 0 && objective[pos] <= solver.nVars());
			coefficients[pos] = BigInteger.valueOf(c.getCoefficients().get(i));
			assert(c.getCoefficients().get(i) > 0);
			pos++;
		}

		return new ObjectiveFunction(new VecInt(objective), new Vec<BigInteger>(coefficients));
	}

	protected int getOptimum(Constraint c) {

		assert(solver.model().length > 0);

		int value = 0;
		for (int i = 0; i < c.getLiterals().size(); i++) {
			// Variables in SAT4J start with index 1
			int sat4j_variable = c.getLiterals().get(i).getSolverId();
			if (model.get(sat4j_variable)) {
				value += c.getCoefficients().get(i);
			}
		}
		return value;
	}
	
	public boolean blockGraph(List<Variable> blockVars) {
		Vector<Integer> clause = new Vector<Integer>();
		for (Variable v : blockVars) {
			clause.add(-(v.getSolverId()+1));
		}
		try {
			solver.addClause(fromVectortoIVec(clause));
		} catch (ContradictionException e) {
			//assert false;
			return false;
		}

		return true;
	}
	
	public boolean blockModel(List<Variable> blockVars) {
		assert(solver.model().length > 0);
		Vector<Integer> clause = new Vector<Integer>();
		for (Variable v : blockVars) {
			assert (v.solverId < solver.model().length);
			clause.add(-solver.model()[v.solverId]);
		}
		try {
			solver.addClause(fromVectortoIVec(clause));
		} catch (ContradictionException e) {
			return false;
		}

		return true;
	}
	
	// conversion to sat4j vector type
	protected IVecInt fromVectortoIVec(Vector<Integer> vec) {
		int[] constraint = new int[vec.size()];
		for (int i = 0; i < vec.size(); i++) {
			constraint[i] = vec.get(i);
		}
		return new VecInt(constraint);
	}

}
