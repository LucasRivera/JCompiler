package jcompiler.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import jcompiler.utils.ILOC.commandControl;

/*====================
load/store  5 cycles
mult/div    2 cycles
l/r shift	? cycles
add/sub     ? cycles
rest        1 cycle
=====================*/
public class Interpreter {
	//Flag for option parameter
	public static final int PRINTCYCLE = 1;//After running the program the Interpreter prints out the amount of cycles that it took to run
	public static final int READFROMFILE = 2;//Take the first argument str as a pathname to a ILOC program
	public static final int READFROMTEXT = 4;//Take the first argument str as a ILOC program *CANNOT be combined with READFROMFILE
	public static final int ALLOWSPILLMEM = 8;
	public static final int PRINTPROGRAM = 16;//Prints the program that is currently loaded into memory
	public static final int PRINTNUMBER = 32;//Prints the number of commands that were read in
	public static final int PRINTEXECNUM = 64;//Prints the number of commands that were actually executed properly
	public static final int PRINTREGS = 128;//Prints the content of all the register after the program is done executing
	public static final int PRINTREGSHEX = 256;//Same as above but in hex format
	public static final int PRINTSTACK = 512;//Prints the content of the Virtual(ILOC) stack/Memory
	public static final int FORCEVIRTUAL = 1024;//Interpreter will complain if a value is not given a unique register
	public static final int FORCEPHYSICAL = 2048;//Forces a physical register limit, limit needs to be read from the -r flag
	public static final int MEMEXPAND = 4096;//If set and the ILOC program exceeds the maxmem instead of printing error message, it will expand
	public static final int SIMSTALBRAN = 8192;//Simulates a stall that occurs with a branch
	public static final int SIMSTALMEM = 16386;//Simulates a stall that occurs with a memory interlock
	public static final int SIMSTALREG = 32772;//Simulates a stall that occurs with a register interlock
	//Alot more options can be added to this program
	private int breakpoints = -1;
	private boolean breaker = false;
	private int[] stack;
	private int[] RegSet;
	private ArrayList<Integer> expandable = new ArrayList<Integer>();
	private int cyclecount = 0;
	private int offset = 0;
	public static int memstr = 1024;
	private int option = 0;
	private int execnum =0;
	private int PC = 0;//Program Counter
	private int k = -1;
	ILOC program;
	public Interpreter(String str,int startingoffset,int sizeofstack,int option){
		String rd="";
		offset = startingoffset;
		this.option = option;
		if((option|1)%7==0){//Option flag containes both READFROMFILE and READFROM text which is not allowed (ambiguous)
			System.out.println("ERROR:Cannot read form File and from Text");
			return;
		}
		if((option&READFROMFILE)!=0){
			program = new ILOC();
			//TODO might pull this out if it exist in ILOC already (NOT READY TO COMMIT)
			try {
				BufferedReader in = new BufferedReader(new FileReader(str));
				rd = in.readLine();
				while(rd!=null){
					program.add(rd);
					rd = in.readLine();
				}
				in.close();
			} catch (FileNotFoundException er) {
				er.printStackTrace();
			} catch (IOException er) {
				er.printStackTrace();
			}
		
		}else if((option&READFROMTEXT)!=0){
			program = new ILOC(str);
		}
		RegSet = new int[program.getMaxReg()+1];
		stack = new int[sizeofstack+1];
		
	}
	public boolean storeToMem(int address,int value){
		address = address-memstr;
		if(address<0){
			System.out.println("Cannot store to BIOS");
			return false;
		}
		if(address%4==0){
			address = address/4;
			if((option&MEMEXPAND)==0){//if memexpand is off
				if(address>=stack.length){
					System.out.println("Ran out of Memory");
					return false;
				}
				execnum++;
				cyclecount+=5;
				stack[address] = value;
				return true;
			}
			else{
				execnum++;
				cyclecount+=5;
				expandable.add(value);
				return true;
			}
		}else{
			System.out.println("Deallined addressing error");
			return false;
		}
	}
	public int loadFromMem(int address){
		address = address-memstr;
		if(address<0){
			System.out.println("Cannot retreive from BIOS");
			return -1;//should throw some kind of error
		}
		if(address%4==0){
			address = address/4;
			if((option&MEMEXPAND)==0){//if memexpand is off
				if(address>=stack.length){
					System.out.println("Memory out bonds");
					return -1;
				}
				execnum++;
				cyclecount+=5;
				return stack[address];
			}
			else{
				execnum++;
				cyclecount+=5;
				return expandable.get(address);
			}
		}else{
			System.out.println("Deallined addressing error");
			return -1;
		}
	}
	private boolean output(int address){
		System.out.println(loadFromMem(address));
		return true;//should do catch
	}
	private boolean outputc(int address){
		System.out.println((char)loadFromMem(address));
		return true;
	}
	private boolean store(int op1, int op2){//op1,op2
		if(Math.max(op1, op2)<RegSet.length  && Math.min(op1, op2) >-1){
			storeToMem(RegSet[op2],RegSet[op1]);
			return true;
		}
		return false;
	}
	private boolean storeAI(int op1, int op2, int op3){//op1,op2
		if(Math.max(op1, op2)<RegSet.length  && Math.min(op1, op2) >-1){
			storeToMem(RegSet[op2],RegSet[op1]+op3);
			return true;
		}
		return false;
	}
	private boolean storeAO(int op1, int op2, int op3){//op1,op2
		if(Math.max(op1, Math.max(op2,op3))<RegSet.length  && Math.min(op1, Math.min(op2,op3)) >-1){
			storeToMem(RegSet[op2],RegSet[op1]+RegSet[op3]);
			return true;
		}
		return false;
	}
	private boolean load(int op1, int op2){//op1,op2
		if(Math.max(op1, op2)<RegSet.length  && Math.min(op1, op2) >-1){
			RegSet[op2] = loadFromMem(RegSet[op1]);
			return true;
		}
		return false;
	}
	private boolean loadI(int op1, int op2){
		if(op2<RegSet.length  && op2 >-1){
			RegSet[op2]=op1;
			execnum++;
			cyclecount+=1;
			return true;
		}
		return false;
	}
	private boolean loadAI(int op1, int op2, int op3){//op1,op2
		if(Math.max(op1, op3)<RegSet.length  && Math.min(op1, op3) >-1){
			RegSet[op3] = loadFromMem(RegSet[op1]+op2);
			return true;
		}
		return false;
	}
	private boolean loadAO(int op1, int op2, int op3){//op1,op2
		if(Math.max(op1, op3)<RegSet.length  && Math.min(op1, op3) >-1){
			RegSet[op3] = loadFromMem(RegSet[op1]+RegSet[op2]);
			return true;
		}
		return false;
	}
	private boolean i2i(int op1, int op2){//op1,op2
		if(Math.max(op1, op2)<RegSet.length  && Math.min(op1, op2) >-1){
			RegSet[op2]=RegSet[op1];
			return true;
		}
		return false;
	}
	private boolean add(int op1,int op2,int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){//shouldn't happen unless physical limit is set
			RegSet[op3] = RegSet[op1]+RegSet[op2];
			execnum++;
			cyclecount+=1;
			return true;
		}
		return false;
	}
	private boolean addI(int op1,int op2,int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){//shouldn't happen unless physical limit is set
			RegSet[op3] = RegSet[op1]+op2;
			execnum++;
			cyclecount+=1;
			return true;
		}
		return false;
	}
	private boolean sub(int op1,int op2,int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = RegSet[op1]-RegSet[op2];
			execnum++;
			cyclecount+=1;
			return true;
		}
		return false;
	}
	private boolean subI(int op1,int op2,int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){
			RegSet[op3] = RegSet[op1]-op2;
			execnum++;
			cyclecount+=1;
			return true;
		}
		return false;
	}
	private boolean mult(int op1,int op2,int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = RegSet[op1]*RegSet[op2];
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean multI(int op1,int op2,int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){
			RegSet[op3] = RegSet[op1]*op2;
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean div(int op1,int op2,int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = RegSet[op1]/RegSet[op2];
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean div(int op1,int op2,int op3,int op4){
		if(Math.max(Math.max(op1, op2),Math.min(op3,op4))<RegSet.length  && Math.min(Math.min(op1, op2),Math.min(op3,op4))>-1){
			RegSet[op3] = RegSet[op1]/RegSet[op2];
			RegSet[op4] = RegSet[op1]%RegSet[op2];
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean divI(int op1,int op2,int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){
			RegSet[op3] = RegSet[op1]/op2;
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean divI(int op1,int op2,int op3,int op4){
		if(Math.max(op1,Math.min(op3,op4))<RegSet.length  && Math.min(op1,Math.min(op3,op4))>-1){
			RegSet[op3] = RegSet[op1]/op2;
			RegSet[op4] = RegSet[op1]%op2;
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean lshift(int op1, int op2, int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = RegSet[op1]<<RegSet[op2];
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean lshiftI(int op1, int op2, int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){
			RegSet[op3] = RegSet[op1]<<op2;
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean rshift(int op1, int op2, int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = RegSet[op1]>>RegSet[op2];
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean and(int op1, int op2, int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = RegSet[op1]&RegSet[op2];
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean andI(int op1, int op2, int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){
			RegSet[op3] = RegSet[op1]&op2;
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean nand(int op1, int op2, int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = ~(RegSet[op1]&RegSet[op2]);
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean nandI(int op1, int op2, int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){
			RegSet[op3] = ~(RegSet[op1]&op2);
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean or(int op1, int op2, int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = RegSet[op1]|RegSet[op2];
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean orI(int op1, int op2, int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){
			RegSet[op3] = RegSet[op1]|op2;
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean nor(int op1, int op2, int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = ~(RegSet[op1]|RegSet[op2]);
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean norI(int op1, int op2, int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){
			RegSet[op3] = ~(RegSet[op1]|op2);
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean xor(int op1, int op2, int op3){
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			RegSet[op3] = RegSet[op1]^RegSet[op2];
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean xorI(int op1, int op2, int op3){
		if(Math.max(op1,op3)<RegSet.length  && Math.min(op1,op3)>-1){
			RegSet[op3] = RegSet[op1]^op2;
			execnum++;
			cyclecount+=2;
			return true;
		}
		return false;
	}
	private boolean not(int op1){
		if(op1<RegSet.length && op1>-1){
			RegSet[op1]=~RegSet[op1];
			return true;
		}
		return false;
	}
	private boolean cmpLT(int op1, int op2, int op3) {
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			if(RegSet[op1]<RegSet[op2]){
				RegSet[op3] = 1;//true
			}
			else{
				RegSet[op3] = 0;//false
			}
			return true;
		}
		return false;
	}
	private boolean cmpLE(int op1, int op2, int op3) {
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			if(RegSet[op1]<=RegSet[op2]){
				RegSet[op3] = 1;//true
			}
			else{
				RegSet[op3] = 0;//false
			}
			return true;
		}
		return false;
	}
	private boolean cmpEQ(int op1, int op2, int op3) {
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			if(RegSet[op1]==RegSet[op2]){
				RegSet[op3] = 1;//true
			}
			else{
				RegSet[op3] = 0;//false
			}
			return true;
		}
		return false;
	}
	private boolean cmpGE(int op1, int op2, int op3) {
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			if(RegSet[op1]>=RegSet[op2]){
				RegSet[op3] = 1;//true
			}
			else{
				RegSet[op3] = 0;//false
			}
			return true;
		}
		return false;
	}
	private boolean cmpGT(int op1, int op2, int op3) {
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			if(RegSet[op1]>RegSet[op2]){
				RegSet[op3] = 1;//true
			}
			else{
				RegSet[op3] = 0;//false
			}
			return true;
		}
		return false;
	}
	private boolean cmpNE(int op1, int op2, int op3) {
		if(Math.max(Math.max(op1, op2),op3)<RegSet.length  && Math.min(Math.min(op1, op2),op3)>-1){
			if(RegSet[op1]!=RegSet[op2]){
				RegSet[op3] = 1;//true
			}
			else{
				RegSet[op3] = 0;//false
			}
			return true;
		}
		return false;
	}
	private boolean cbr(int op1, String Label1, String Label2){
		if(op1<RegSet.length && op1>-1){
			if(RegSet[op1]==1){
				PC = program.lookUpLabel(Label1);
			}
			else if(RegSet[op1]==0){
				PC = program.lookUpLabel(Label2);
			}
		}
		return false;
	}
	public void printRegs(boolean hex){
		for(int i=0;i<RegSet.length;i++){
			if(hex){
				System.out.println("r"+i+"\t=\t"+Integer.toHexString(RegSet[i]));
			}else{
				System.out.println("r"+i+"\t=\t"+RegSet[i]);
			}
		}
	}
	public boolean run(int start, int reg, int breakpoints){//Run the Interpreter until breakpoint
		this.breakpoints = breakpoints;
		k= reg;
		breaker = true;
		return run(0);
	}
	public boolean run(int start, int breakpoints){//Run the Interpreter until breakpoint
		this.breakpoints = breakpoints;
		breaker = true;
		return run(0);
	}
	public boolean run(){
		return run(0);
	}
	public boolean run(int start){//if run returns false - signifies a program crash
		boolean check = true;
		ILOC.command toRun;
		int[] ops;
		for(PC = start;PC<program.NumberOfInstructions();PC++){//By making PC private global I have reserved the right to expand this program to support jumps
			toRun = program.getCommand(PC);
			ops = toRun.getOps();
			check = false;
			switch(toRun.getOpcode()){
				//Memory Operators(Cases)
				case output:
					check = output(ops[0]);
					break;
				case outputc:
					check = outputc(ops[0]);
					break;
				case store:
					check = store(ops[0],ops[1]);
					break;
				case storeAI:
					check = storeAI(ops[0],ops[1],ops[2]);
					break;
				case storeAO:
					check = storeAO(ops[0],ops[1],ops[2]);
					break;
				case load:
					check = load(ops[0],ops[1]);
					break;
				case loadI:
					check = loadI(ops[0],ops[1]);
					break;
				case loadAI:
					check = loadAI(ops[0],ops[1],ops[2]);
					break;
				//Arithmetic Operators(Cases)
				case add:
					check = add(ops[0],ops[1],ops[2]);
					break;
				case addI:
					check = addI(ops[0],ops[1],ops[2]);
					break;
				case sub:
					check = sub(ops[0],ops[1],ops[2]);
					break;
				case subI:
					check = subI(ops[0],ops[1],ops[2]);
					break;
				case mult:
					check = mult(ops[0],ops[1],ops[2]);
					break;
				case multI:
					check = multI(ops[0],ops[1],ops[2]);
					break;
				case div:
					if(ops.length==3){
						check = div(ops[0],ops[1],ops[2]);
					}
					else if(ops.length==4){
						check = div(ops[0],ops[1],ops[2],ops[3]);
					}
					else{
						return false;
					}
					break;
				case divI:
					if(ops.length==3){
						check = divI(ops[0],ops[1],ops[2]);
					}
					else if(ops.length==4){
						check = divI(ops[0],ops[1],ops[2],ops[3]);
					}
					else{
						return false;
					}
					break;
				//Logical Operators
				case and:
					check = and(ops[0],ops[1],ops[2]);
					break;
				case andI:
					check = andI(ops[0],ops[1],ops[2]);
					break;
				case nand:
					check = nand(ops[0],ops[1],ops[2]);
					break;
				case or:
					check = or(ops[0],ops[1],ops[2]);
					break;
				case orI:
					check = orI(ops[0],ops[1],ops[2]);
					break;
				case nor:
					check = nor(ops[0],ops[1],ops[2]);
					break;
				case xor:
					check = xor(ops[0],ops[1],ops[2]);
					break;
				case xorI:
					check = xorI(ops[0],ops[1],ops[2]);
					break;
				case not:
					check = not(ops[0]);
					break;
				case lshift:
					check = lshift(ops[0],ops[1],ops[2]);
					break;
				case lshiftI:
					check = lshiftI(ops[0],ops[1],ops[2]);
					break;
				case rshift:
					check = rshift(ops[0],ops[1],ops[2]);
					break;
				case rshiftI:
					//check = rshiftI(ops[0],ops[1],ops[2]);
					break;
				//Control-Flow Operations(cmp)
				case cmp_LT:
					check = cmpLT(ops[0],ops[1],ops[2]);
					break;
				case cmp_LE:
					check = cmpLE(ops[0],ops[1],ops[2]);
					break;
				case cmp_EQ:
					check = cmpEQ(ops[0],ops[1],ops[2]);
					break;
				case cmp_GE:
					check = cmpGE(ops[0],ops[1],ops[2]);
					break;
				case cmp_GT:
					check = cmpGT(ops[0],ops[1],ops[2]);
					break;
				case cmp_NE:
					check = cmpNE(ops[0],ops[1],ops[2]);
					break;
				//Control-Flow Operations(cbr)
				case cbr:
					commandControl ctoRun = (commandControl)toRun;
					check = cbr(ops[0],ctoRun.getTrueLabel(),ctoRun.getFalseLabel());
					continue;
				case cbr_LT:
				case cbr_LE:
				case cbr_EQ:
				case cbr_GE:
				case cbr_GT:
				case cbr_NE:
				//Control-Flow jumps
				case jump:
					PC = ops[0];
					continue;
				case jumpI:
					commandControl jtoRun = (commandControl)toRun;
					PC = program.lookUpLabel(jtoRun.getTrueLabel());
					continue;
				case jal:
				case tbl:
				//others
				case nop:
					break;
				default:
					return false;
			}
			if(!check){
				return false;
			}
		}//End of Program Execution
		//Now to deal with the ending flags
		if((option & PRINTCYCLE)!=0){
			System.out.println("Program finished excecution with:\n"+cyclecount+" cycles");
		}
		if((option & PRINTPROGRAM)!=0){
			program.printProgram();
		}
		if((option & PRINTNUMBER)!=0){
			System.out.println(program.NumberOfInstructions()+" LOC where read in by the ILOC Interpreter");
		}
		if((option & PRINTEXECNUM)!=0){
			System.out.println(execnum+" LOC where executed by the ILOC Interpreter");
		}
		if((option & PRINTREGS)!=0){
			this.printRegs(false);
		}
		if((option & PRINTREGSHEX)!=0){
			this.printRegs(true);
		}
		if((option & PRINTSTACK)!=0){
			for(int i=0; i<stack.length; i++){
				if(i%10==0){
					System.out.print("\n");
				}
				System.out.print(stack[i]+"\t");
			}
		}
		return true;
	}
	public static void main(String[] args) {
		boolean breaker = false;
		int[] iniMem=null;
		int[] breakpoints=null;
		int size = 4096;//Default stack size
		int offset = 1024;//default offset
		int flags = Interpreter.PRINTCYCLE|Interpreter.READFROMFILE;
		int k = -1;//Default is an unlimited amount of Physical Registers (or amount necessary to run the program without spilling)
		if(args.length==0){
			args = new String[1];
			args[0] = "-h";
		}
		//==========Flag Option code ==============================================
			for(int i=0;i<args.length;i++){
				if(args[i].equals("-h") || args[0].equals("-h")){//The help command
					System.out.println("Welcomb to the ILOC Interpreter\n"
							+ "Usage:$java Interpreter filename [options]\n"
							+ "Caution: ILOC.class has to be in the same folder as Interpreter.class\n"
							+ "Caution: filename should be a valid ILOC program please read the ReadMe.txt for a list of features and capabilities/Support\n"
							+ "Options:\n"
							+ "\t-i NUM ... NUM\tSets the starting memory location to the first NUM and the remaining NUMS are put into memory(input of ILOC program)\n"
							+ "\t-r NUM\t\tSets the physical register limit to NUM\n"
							+ "\t-b NUM\t\tSets a breakpoint to NUM- Interpreter stops at location NUM and prints the Registers\n"
							+ "\t-br NUM ... NUM\tSets breakpoints to all the NUMS - when Interpreter gets to a breakpoint it pauses execution prints the registers and continues running until the next breakpoint\n"
							+ "\t-m NUM\t\tSets the amount of memory to NUM\n"
							+ "\t-u NUM\t\tSets the amount of memory to unlimited CAUTION- this flag over-rides the -m flag\n"
							+ "\t-v\t\tForces the interpreter to follow virtual register standard, that each new value must have a new register\n"
							+ "\t-c\t\tPrints the amount of cycles the ILOC program would use\n"
							+ "\t-pp\t\tPrints the inputed ILOC program- Good for debugging to see if the Interpeter has the correct ILOC program in memory\n"
							+ "\t-pn\t\tPrints the amount of lines in the ILOC program\n"
							+ "\t-pe\t\tPrints the amount of lines that were executed- Good for debugging pn should equal pe\n"
							+ "\t-pr\t\tPrints the registers and their values after execution - Good for debugging\n"
							+ "\t-ph\t\tPrints the registers and their values in Hex format\n"
							+ "\t-pm\t\tPrints the memory - CAUTION may be large\n"
							+ "\t-h\t\tUssage, explains the program and runtime options\n");
					System.exit(0);
				}
				else if(args.length==1){//the user has no flags
					break;
				}
				else if(args[i].equals("-r")){//Sets the Physical register Count
					if(i+1==args.length){
						System.out.println("-r flag requires a number afterwords to signify the amount of physical registers");
						System.exit(0);
					}else{
						flags = flags | Interpreter.FORCEPHYSICAL;
						try{
							k = Integer.parseInt(args[i+1]);
						}catch(NumberFormatException er){
							System.out.println("-r flag requires a number afterwords to signify the amount of physical registers");
							System.exit(0);
						}
					}
					i++;
					continue;
				}
				else if(args[i].equals("-i")){
					int j =0;
					for(j=0;j+i+1<args.length;j++){//looks for the rest of the numbers
						if(args[j+i+1].charAt(0)=='-'){
							break;
						}
					}
					if(j==0){
						System.out.println("-i flag requires a number afterwords to signify the starting location of memory");
						System.exit(0);
					}
					if(j==1){
						try{
							offset = Integer.parseInt(args[i+1]);
						}
						catch(NumberFormatException er){
							System.out.println("-i flag requires a number afterwords to signify the starting location of memory");
							System.exit(0);
						}
					}
					if(j>1){
						iniMem = new int[j-1];
						for(int c=0;c<iniMem.length;c++){
							try{
								iniMem[c] = Integer.parseInt(args[i+2+c]);
							}
							catch(NumberFormatException er){
								System.out.println("-i flag requires a number afterwords to signify the start of memory and every additonal argument that is not a flag is treated as a number that should be put in memory");
								System.exit(0);
							}
						}
					}
					i+=j;
					continue;
					
				}
				else if(args[i].equals("-c")){//TODO not implimented yet
					try{
						size = Integer.parseInt(args[2]);
					}catch(NumberFormatException er){
					
					}
				}
				else if(args[i].equals("-m")){//sets the size of the stack(memory)
					i++;
					try{
						size = Integer.parseInt(args[i]);
					}catch(NumberFormatException er){
						System.out.println("-m flag requires a number afterwords to signify the amount of virtual memory");
						System.exit(0);
					}
				}
				else if(args[i].equals("-s")){
					flags = flags | Interpreter.ALLOWSPILLMEM;
					memstr = 0;
					
				}
				else if(args[i].equals("-u")){//sets the size of the stack(memory) to growable/unlimited
					size = 0;
					flags = flags | Interpreter.MEMEXPAND;
				}
				else if(args[i].equals("-v")){
					flags = flags | Interpreter.FORCEVIRTUAL;
				}
				else if(args[i].equals("-c")){//prints the amount of cycles that the ILOC simulator used
					flags = flags | Interpreter.PRINTCYCLE;
				}
				else if(args[i].equals("-pp")){//prints the program that was read into memory
					flags = flags | Interpreter.PRINTPROGRAM;
				}
				else if(args[i].equals("-pr")){
					flags = flags | Interpreter.PRINTREGS;
				}
				else if(args[i].equals("-ph")){
					flags = flags | Interpreter.PRINTREGSHEX;
				}
				else if(args[i].equals("-pn")){
					flags = flags | Interpreter.PRINTNUMBER;
				}
				else if(args[i].equals("-pe")){
					flags = flags | Interpreter.PRINTEXECNUM;
				}
				else if(args[i].equals("-pm")){
					flags = flags | Interpreter.PRINTSTACK;
				}
				
			}
		//=========END OF FLAG/OPTION CODE============================
		Interpreter iloc = new Interpreter(args[0],offset,size,flags);
		if(iniMem!=null){
			for(int i=0;i<iniMem.length;i++){
				iloc.storeToMem(offset + 4*i, iniMem[0]);
			}
		}
		if(k!=-1 && breaker){
			int r = 0;
			for(int i=0;i<breakpoints.length;i++){
				iloc.run(r,k,breakpoints[i]);
				r = breakpoints[i]+1;//restarts the ILOC program after the breakpoint
			}
		}
		else if(breaker){
			int r = 0;
			for(int i=0;i<breakpoints.length;i++){
				iloc.run(r,breakpoints[i]);
				r = breakpoints[i]+1;//restarts the ILOC program after the breakpoint
			}
		}
		else if(k!=-1){
			iloc.run(0,k,-1);
		}
		else{
			iloc.run();
		}
	}

}
