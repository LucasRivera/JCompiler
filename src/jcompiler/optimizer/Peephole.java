package jcompiler.optimizer;

import jcompiler.utils.ILOC;
import jcompiler.utils.ILOC.command;

public class Peephole extends Optimizer{
	private Peephole(){}
	/**
	 * Peephole Optimizer looks at 3 lines of code and tries to replace them with one line of code that maintains(preserve) semantics
	 * */
	public static ILOC Optimize(ILOC input){
		for(int i=0;i<input.NumberOfInstructions();i++){
			if((i+2)>=input.NumberOfInstructions()){//Need at least 3 instructions for a window of optimization
				return input;
			}
			command c1 = input.getCommand(i);
			command c2 = input.getCommand(i+1);
			command c3 = input.getCommand(i+2);
			/*Constant Compression
			 * 3+6=9
			 * LoadI 3 => rx
			 * LoadI 6 => ry
			 * add rx, ry => rz
			 * 
			 * reduces to(figures out the math compile time)
			 * LoadI 9=>rz
			 * 
			 * also works for mult, div, sub, shift, ect
			 * */
			if(c1.getOpcode()==ILOC.opcodes.loadI && c2.getOpcode()==ILOC.opcodes.loadI && c3.getOpcode()!=ILOC.opcodes.loadI && c3.getOpcode()!=ILOC.opcodes.load && c3.getOpcode()!=ILOC.opcodes.store){
				int rx = c1.getOps()[1];
				int ry = c2.getOps()[1];
				int[] ops = c3.getOps();
				if((rx==ops[0]||rx==ops[1])&&(ry==ops[0]||ry==ops[1])){
					int ans=0;
					int rz = ops[2];
					if(c3.getOpcode()==ILOC.opcodes.add){
						ans = c1.getOps()[0]+c2.getOps()[0];
					}
					else if(c3.getOpcode()==ILOC.opcodes.sub){
						ans = c1.getOps()[0]-c2.getOps()[0];
					}
					else if(c3.getOpcode()==ILOC.opcodes.mult){
						ans = c1.getOps()[0]*c2.getOps()[0];
					}
					else if(c3.getOpcode()==ILOC.opcodes.and){
						ans = c1.getOps()[0]&c2.getOps()[0];
					}
					else if(c3.getOpcode()==ILOC.opcodes.or){
						ans = c1.getOps()[0]|c2.getOps()[0];
					}
					int[] newops = {ans,rz};
					c1.setOps(newops);
					input.remove(i+1);
					input.remove(i+1);
				}
			}
			/*Variable Folding
			 * a+0=a
			 * Load rx => ry
			 * LoadI 0 => rz
			 * add ry, rz => rw
			 * 
			 * reduces to
			 * Load rx=>rw
			 * 
			 * a*0=0
			 * Load rx => ry
			 * LoadI 0 => rz
			 * mult ry, rz => rw
			 * 
			 * reduces to
			 * LoadI 0 => ry
			 * 
			 * a*1=a
			 * Load rx => ry
			 * LoadI 1 => rz
			 * mult ry, rz => rw
			 * 
			 * reduces to
			 * Load rx => rw
			 * 
			 * */
			else if(c1.getOpcode()==ILOC.opcodes.load && c2.getOpcode()==ILOC.opcodes.loadI){
				if(c2.getOps()[0]==0 && c3.getOpcode()==ILOC.opcodes.add){
					int[] ops = c1.getOps();
					ops[1] = c3.getOps()[2];
					c1.setOps(ops);
				}
				else if(c2.getOps()[0]==1 && c3.getOpcode()==ILOC.opcodes.mult){
					int[] ops = c1.getOps();
					ops[1] = c3.getOps()[2];
					c1.setOps(ops);
				}
				else if(c2.getOps()[0]==0 && c3.getOpcode()==ILOC.opcodes.mult){
					int[] ops = c2.getOps();
					ops[1] = c3.getOps()[2];
					c2.setOps(ops);
					input.remove(i);
					input.remove(i+1);
					continue;
				}
				else{
					continue;
				}
				//delete c2, c3
				input.remove(i+1);
				input.remove(i+1);
			}
			else if(c2.getOpcode()==ILOC.opcodes.load && c1.getOpcode()==ILOC.opcodes.loadI){
				if(c1.getOps()[0]==0 && c3.getOpcode()==ILOC.opcodes.add){
					int[] ops = c2.getOps();
					ops[1] = c3.getOps()[2];
					c2.setOps(ops);
				}
				else if(c1.getOps()[0]==1 && c3.getOpcode()==ILOC.opcodes.mult){
					int[] ops = c2.getOps();
					ops[1] = c3.getOps()[2];
					c2.setOps(ops);
				}
				else if(c1.getOps()[0]==0 && c3.getOpcode()==ILOC.opcodes.mult){
					int[] ops = c1.getOps();
					ops[1] = c3.getOps()[2];
					c1.setOps(ops);
					input.remove(i);
					input.remove(i+1);
					continue;
				}
				else{
					continue;
				}
				//delete c1,c3
				input.remove(i);
				input.remove(i+1);
			}
			
		}
		return input;
	}
	public static void main(String[] args){
		//Peephole Testing Results/Example Usage
		ILOC program = new ILOC();
		
		//============= Constant Compression ==========
		
		//add
		program.add("loadI 2 => r1\n"
				+ "loadI 3 => r2\n"
				+ "add r1, r2 => r3");
		program.printProgram();
		Optimize(program).printProgram();
		program.clear();
		//sub
		program.add("loadI 10 => r1\n"
				+ "loadI 2 => r2\n"
				+ "sub r1, r2 => r3");
		program.printProgram();
		Optimize(program).printProgram();
		program.clear();
		//mult
		program.add("loadI 6 => r1\n"
				+ "loadI 2 => r2\n"
				+ "mult r1, r2 => r3");
		program.printProgram();
		Optimize(program).printProgram();
		program.clear();
		//=============================================
		
		//============= Variable Folding     ==========
		
		//var add by 0 ex a+0=a
		program.add("load r0 => r1\n"
				+ "loadI 0 => r2\n"
				+ "add r1, r2 => r3");
		program.printProgram();
		Optimize(program).printProgram();
		program.clear();
		//var mult by 1 ex a*1=a
		program.add("load r0 => r1\n"
				+ "loadI 1 => r2\n"
				+ "mult r1, r2 => r3");
		program.printProgram();
		Optimize(program).printProgram();
		program.clear();
		//var mult by 0 ex a*0=0
		program.add("load r0 => r1\n"
				+ "loadI 0 => r2\n"
				+ "mult r1, r2 => r3");
		program.printProgram();
		Optimize(program).printProgram();
		program.clear();
		//=============================================
		
	}
}
