package com.apposcopy.synthesis.sat4j;

import java.util.ArrayList;
import java.util.List;

public class Constraint {
	
	public static enum ConstraintType {
		LEQ, GEQ, EQ
	}
	
	public static enum EncodingType {
		INIT, GOAL, EMBED, FIRE_TRANSACTION, FRAME_AXIOMS
	}
	
	/**
	 * Members of Encoding class
	 */
	protected List<Variable> literals;
	protected List<Integer> coefficients;
	protected ConstraintType type;
	protected Integer rhs;
	protected EncodingType enctype;
	
	/**
	 * Constructors
	 */
	public Constraint(List<Variable> literals, List<Integer> coefficients, ConstraintType type, Integer rhs){
		for(Variable v : literals) {
			assert v != null : literals;
		}
		this.literals = literals;
		this.coefficients = coefficients;
		this.type = type;
		this.rhs = rhs;
	}
	
	public Constraint(List<Variable> literals, List<Integer> coefficients, ConstraintType type, Integer rhs, EncodingType enctype){
		for(Variable v : literals) {
			assert v != null;
		}
		this.literals = literals;
		this.coefficients = coefficients;
		this.type = type;
		this.rhs = rhs;
		this.enctype = enctype;
	}

	
	public Constraint(List<Variable> literals, ConstraintType type, Integer rhs){
		for(Variable v : literals) {
			assert v != null;
		}
		this.literals = literals;
		this.type = type;
		
		coefficients = new ArrayList<Integer>();
		for (int i = 0; i < literals.size(); i++)
			coefficients.add(1);
		
		this.rhs = rhs;
	}
	
	public Constraint(List<Variable> literals, ConstraintType type, Integer rhs, EncodingType enctype){
		for(Variable v : literals) {
			assert v != null;
		}
		this.literals = literals;
		this.type = type;
		this.enctype = enctype;
		
		coefficients = new ArrayList<Integer>();
		for (int i = 0; i < literals.size(); i++)
			coefficients.add(1);
		
		this.rhs = rhs;
	}
	
	public Constraint(){
		this.literals = new ArrayList<Variable>();
		this.coefficients = new ArrayList<Integer>();
		this.type = ConstraintType.GEQ;
		this.rhs = 0;
	}
	
	/**
	 * Public methods
	 */
	public void addLiteral(Variable v, int coeff){
		literals.add(v);
		coefficients.add(coeff);
	}
	
	public List<Variable> getLiterals(){
		return literals;
	}
	
	public List<Integer> getCoefficients(){
		return coefficients;
	}
	
	public ConstraintType getType(){
		return type;
	}
	
	public int getRhs(){
		return rhs;
	}
	
	public void setRhs(int rhs){
		this.rhs = rhs;
	}
	
	public void setType(ConstraintType type){
		this.type = type;
	}
	
	public int getSize(){
		return literals.size();
	}
	
	public EncodingType getEncodingType(){
		return enctype;
	}
	
	public String printOPB(){
		String s = "";
		assert (literals.size() == coefficients.size());
		int pos = 0;
		for (Variable v : literals){
			int coeff = coefficients.get(pos);
			if (coeff > 0) s = s.concat("+" + coefficients.get(pos) + " x" + (v.getSolverId()+1) + " ");
			else s = s.concat(coefficients.get(pos) + " x" + (v.getSolverId()+1) + " ");
			pos++;
		}
		if(type == ConstraintType.EQ)
			s = s.concat("=");
		else if (type == ConstraintType.GEQ)
			s = s.concat(">=");
		else if (type == ConstraintType.LEQ)
			s = s.concat("<=");
		
		s = s.concat(" " + rhs + " ;");
		return s;
	}
	
	public void print(){
		assert (literals.size() == coefficients.size());
		int pos = 0;
		for (Variable v : literals){
			System.out.print(coefficients.get(pos) + " x" + (v.getSolverId()+1) + " [" + v.toString() + "] ");
			pos++;
		}
		if(type == ConstraintType.EQ)
			System.out.print("=");
		else if (type == ConstraintType.GEQ)
			System.out.print(">=");
		else if (type == ConstraintType.LEQ)
			System.out.print("<=");
		
		System.out.println(" " + rhs);
	}
}