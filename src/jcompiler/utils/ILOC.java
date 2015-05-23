package jcompiler.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
/*ILOC comands not yet implemented
 * loadAI, storeAI, addI, subI, multI, div, divI, not, nor nand, and all control-flow operators, and labels
 * 
 *ILOC commands not supported
 * 
 * */
//Describes an ILOC program as a list of commands that can be run(Basically an ILOC Scanner and Parser(Linear Code))
public class ILOC {
	public enum opcodes {loadI,loadAI,loadAO,load,store,storeAI,storeAO,output,outputc,i2i,//memory operators
						add,addI,sub,subI,mult,multI,div,divI,  //Arithmetic operators
						lshift,lshiftI,rshift,rshiftI,and,andI,or,orI,not,nor,norI,nand,nandI,xor,xorI,//Logical operators
						cmp_LT,cmp_LE,cmp_EQ,cmp_GE,cmp_GT,cmp_NE,//control-flow operators
						cbr,cbr_LT,cbr_LE,cbr_EQ,cbr_GE,cbr_GT,cbr_NE,
						jump,jumpI,jal,tbl,
						nop,//other instructions
						ret,push,pop,//Stack Operations
						vecon,vecoff,//parralle instructions
						};
	private ArrayList<command> program = new ArrayList<command>();
	private HashMap<String,Integer> labels = new HashMap<String,Integer>();
	private int maxReg = 0;
	
	
	/*========================================================================
	This Constructor is used when a program starts from scratch
	and keeps adding lines(commands) one at a time
	this is the most flexible but basic means of using this framework for ILOC
	*/
	public ILOC(){}
	//========================================================================
	
	/*==================================
	This Constructor is used whenever a
	whole program is read as a file
	*/
	public ILOC(String str){
		String rd="";
		try {
			BufferedReader in = new BufferedReader(new FileReader(str));
			rd = in.readLine();
			while(rd!=null){
				add(rd);
				rd = in.readLine();
			}
			in.close();
		} catch (FileNotFoundException er) {
			er.printStackTrace();
		} catch (IOException er) {
			er.printStackTrace();
		}
		
	}//=================================
	
	//General Methods used by the outerclass
	public String getLine(int linenum){
		if(linenum<1 || linenum>program.size()){
			System.out.println("Not a valid Line Number");
			return "";
		}
		return program.get(linenum+1).toString();
	}
	public command getCommand(int num){
		if(num<0 || num>=program.size()){
			System.out.println("No Command Exist");
			return null;
		}
		return program.get(num);
	}
	public int NumberOfInstructions(){
		return program.size();
	}
	public int getMaxReg(){
		return maxReg;
	}
	public void printProgram(){
		System.out.println("//Program generated by ILOC.java created by Lucas Rivera\n");
		for(int i=0;i<program.size();i++){
			System.out.println(program.get(i).toString());
		}
	}
	public boolean writeProgramToFileText(){//Saves program to file I'll do this later TODO
		return false;
	}
	public int LifeBegin(int regnum){
		int[] ops;
		for(int i=0;i<program.size();i++){
			ops = program.get(i).getOps();
			if(ops.length == 3){
				if(ops[0]==regnum||ops[1]==regnum||ops[2]==regnum){
					return i;
				}
			}else if(ops.length == 2){
				if(ops[1]==regnum){
					return i;
				}
				if(ops[0]==regnum && (getCommand(i).getOpcode()!=opcodes.loadI)){
					return i;
				}
			}
		}
		return -1;
	}
	public int LifeEnd(int regnum){//Edge of start (Not Sure what proffesor called it) TODO look it up - not that important
		int[] ops;
		for(int i=program.size()-1;i>=0;i--){
			ops = program.get(i).getOps();
			if(ops.length == 3){
				if(ops[0]==regnum||ops[1]==regnum||ops[2]==regnum){
					return i-1;
				}
			}else if(ops.length == 2){
				if(ops[1]==regnum){
					return i-1;
				}
				if(ops[0]==regnum && (getCommand(i).getOpcode()!=opcodes.loadI)){
					return i-1;
				}
			}
		}
		return -1;
	}
	public void remove(int loc){
		program.remove(loc);
	}
	/**
	 * Basically Does a removeAll/Clears the program
	 * */
	public void clear(){
		while(program.size()>0){
			program.remove(0);
		}
	}
	public int lookUpLabel(String label){
		return ((Integer)labels.get(label)).intValue();
	}
	public void add(command c, int loc){
		program.add(loc, c);
	}
	//This function receives a string that should be in the form of a command
	//If the the command is invalid the function returns false
	//Otherwise it is added to the current program
	//the add function can add-in one command or multiple commands at a time
	public boolean add(String newline){
		//String[] tokens = newline.split("");
		
		if(newline.indexOf('/')!=-1){//coment killing code
			int len = newline.length();
			int j =0;
			while(j<len){
				if(newline.charAt(j)=='/'){
					newline = newline.substring(0,j+2)+'~'+newline.substring(j+2);
					len = newline.length();
					j++;
				}
				j++;
			}
			String[] fix = newline.split("/|\n");
			newline = "";
			for(int i=0;i<fix.length;i++){
				if(fix[i].equals("")){
					continue;
				}
				else if(fix[i].indexOf('~')==-1){
					newline = newline + fix[i]+"\n";
				}
			}
		}
		StringTokenizer strTkn = new StringTokenizer(newline,' '+""+'\t'+""+'\n'+""+',');
		String token = "";
		/*
		 * loadI,loadAI,loadAO,load,store,storeAI,storeAO,output,outputc,i2i,//memory operators
						add,addI,sub,subI,mult,multI,div,divI,  //Arithmetic operators
						lshift,lshiftI,rshift,rshiftI,and,andI,or,orI,not,nor,norI,nand,nandI,xor,xorI,//Logical operators
						cmp_LT,cmp_LE,cmp_EQ,cmp_GE,cmp_GT,cmp_NE,//control-flow operators
						cbr,cbr_LT,cbr_LE,cbr_EQ,cbr_GE,cbr_GT,cbr_NE,
						jump,jumpI,jal,tbl,
						nop,//other instructions
						ret,push,pop,//Stack Operations
						vecon,vecoff,
		 * */
		while(true){
			try{
				token = strTkn.nextToken();
			}catch(NoSuchElementException er){
				return true;//end of token stream was able to add everything it could*
			}
			if(token.equals("output")){
				int op1;
				try{
					op1 = Integer.parseInt(strTkn.nextToken());//constant
					program.add(new command(ILOC.opcodes.output,op1));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse output command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("outputc")){
				int op1;
				try{
					op1 = Integer.parseInt(strTkn.nextToken());//constant
					program.add(new command(ILOC.opcodes.outputc,op1));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse outputc command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("loadI")){
				int op1,op2;
				try{
					op1 = Integer.parseInt(strTkn.nextToken());//constant
					strTkn.nextToken();//=>
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.loadI,op1,op2));
					maxReg = Math.max(maxReg, op2);
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse loadI command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("loadAI")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));// number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.loadAI,op1,op2,op3));
					maxReg = Math.max(Math.max(maxReg,op2),op3);
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse loadAI command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("loadAO")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));// number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.loadAO,op1,op2,op3));
					maxReg = Math.max(Math.max(maxReg,op2),op3);
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse loadAO command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("load")){
				int op1,op2;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.load,op1,op2));
					maxReg = Math.max(maxReg, Math.max(op1,op2));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse load command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("store")){
				int op1,op2;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.store,op1,op2));
					maxReg = Math.max(maxReg, Math.max(op1,op2));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse store command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("storeAI")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));// number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.storeAI,op1,op2,op3));
					maxReg = Math.max(Math.max(maxReg,op2),op3);
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse storeAI command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("storeAO")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.storeAO,op1,op2,op3));
					maxReg = Math.max(Math.max(maxReg,op2),op3);
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse storeAO command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("add")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.add,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(Math.max(op1,op2),op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse add command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("addI")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//constant number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.addI,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(op1,op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse addI command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("sub")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.sub,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(Math.max(op1,op2),op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse sub command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("subI")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//constant number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.subI,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(op1,op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse subI command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("mult")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.mult,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(Math.max(op1,op2),op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse mult command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("multI")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//constant number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.multI,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(op1,op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse multI command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("lshift")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.lshift,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(Math.max(op1,op2),op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse lshift command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("lshiftI")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//constant number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.lshiftI,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(op1,op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse lshiftI command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("rshift")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.rshift,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(Math.max(op1,op2),op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse rshift command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("rshiftI")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//constant number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.rshiftI,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(op1,op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse rshiftI command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("and")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.and,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(Math.max(op1,op2),op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse and command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("or")){
				int op1,op2,op3;
				try{
					op1 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					op2 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					strTkn.nextToken();//=>
					op3 = Integer.parseInt(strTkn.nextToken().substring(1));//register number
					program.add(new command(ILOC.opcodes.or,op1,op2,op3));
					maxReg = Math.max(maxReg, Math.max(Math.max(op1,op2),op3));
					continue;
				}catch (NumberFormatException er){
					System.out.println("Unable to correct parse or command @LINENUM = "+(program.size()+1));
					return false;
				}
			}
			else if(token.equals("jmpI")||token.equals("jumpI")){
				program.add(new commandControl(ILOC.opcodes.jump,strTkn.nextToken()));
			}
			else{
				if(token.contains(":")){
					String Label = token.substring(0,token.length()-1);//remove the collon :
					//need to map label
					labels.put(Label, program.size()-1);
				}
				else{
					System.out.println("Unknown ILOC command {"+token+"} @LINENUM = "+(program.size()+1));
					return false;
				}
			}
		}
		//return legal;
	}
	public static class command{
		private opcodes opcode;
		private int op1;
		private int op2;
		private int op3;
		public command(opcodes opcode, int op1){//for operations with only one opcodes like output
			this.opcode = opcode;
			this.op1 = op1;
		}
		public command(opcodes opcode, int op1, int op2){//for operations with only two opcodes like load
			this.opcode = opcode;
			this.op1 = op1;
			this.op2 = op2;
		}
		public command(opcodes opcode, int op1, int op2, int op3){//for operations with 3 opcodes like add
			this.opcode = opcode;
			this.op1 = op1;
			this.op2 = op2;
			this.op3 = op3;
		}
		public String toString(){//TODO add more operators|EXPANSIONS
			switch(opcode){
				case output:
					return "\toutput\t"+op1;
				case loadI:
					return "\tloadI\t"+op1+"\t=> r"+op2;
				case load:
					return "\tload\tr"+op1+"\t=> r"+op2;
				case loadAI:
				case loadAO:
				case store:
					return "\tstore\tr"+op1+"\t=> r"+op2;
				case storeAI:
					return "\tadd\tr"+op1+", "+op2+"\t=> r"+op3;
				case storeAO:
					return "\tadd\tr"+op1+", r"+op2+"\t=> r"+op3;
				case add:
					return "\tadd\tr"+op1+", r"+op2+"\t=> r"+op3;
				case addI:
					return "\taddI\tr"+op1+", "+op2+"\t=> r"+op3;
				case sub:
					return "\tsub\tr"+op1+", r"+op2+"\t=> r"+op3;
				case subI:
					return "\tsubI\tr"+op1+", "+op2+"\t=> r"+op3;
				case mult:
					return "\tmult\tr"+op1+", r"+op2+"\t=> r"+op3;
				case multI:
					return "\tmultI\tr"+op1+", "+op2+"\t=> r"+op3;
				case div:
				case divI:
				case lshift:
					return "\tlshift\tr"+op1+", r"+op2+"\t=> r"+op3;
				case lshiftI:
					return "\tlshiftI\tr"+op1+", r"+op2+"\t=> r"+op3;
				case rshift:
					return "\trshift\tr"+op1+", r"+op2+"\t=> r"+op3;
				case rshiftI:
					return "\trshiftI\tr"+op1+", r"+op2+"\t=> r"+op3;
				case and:
					return "\tand\tr"+op1+", r"+op2+"\t=> r"+op3;
				case andI:
					return "\tandI\tr"+op1+", r"+op2+"\t=> r"+op3;
				case or:
					return "\tor\tr"+op1+", r"+op2+"\t=> r"+op3;
				case orI:
					return "\torI\tr"+op1+", r"+op2+"\t=> r"+op3;
				case not:
					return "\tnot\t"+op1;
				case nor:
					return "\tnor\tr"+op1+", r"+op2+"\t=> r"+op3;
				case norI:
					return "\tnorI\tr"+op1+", r"+op2+"\t=> r"+op3;
				case nand:
					return "\tnand\tr"+op1+", r"+op2+"\t=> r"+op3;
				case nandI:
					return "\tnandI\tr"+op1+", r"+op2+"\t=> r"+op3;
				case xor:
					return "\txor\tr"+op1+", r"+op2+"\t=> r"+op3;
				case xorI:
					return "\txorI\tr"+op1+", r"+op2+"\t=> r"+op3;
				case cmp_LT:
				case cmp_LE:
				case cmp_EQ:
				case cmp_GE:
				case cmp_GT:
				case cmp_NE:
				case cbr:
				case cbr_LT:
				case cbr_LE:
				case cbr_EQ:
				case cbr_GE:
				case cbr_GT:
				case cbr_NE:
				case jump:
				case jumpI:
				case jal:
				case tbl:
				case nop:
					return "\tnop";
				case ret:
				case push:
				case pop:
				case vecon:
				case vecoff:
				default:
					return "Internal Command Error";
			}
		}
		public int[] getOps(){
			int[] toRet=null;
			if(opcode==opcodes.output){
				toRet = new int[1];
				toRet[0] = op1;
			}else if(opcode==opcodes.load||opcode==opcodes.loadI||opcode==opcodes.store){
				toRet = new int[2];
				toRet[0] = op1;
				toRet[1] = op2;
			}else if(opcode==opcodes.add||opcode==opcodes.sub||opcode==opcodes.mult||opcode==opcodes.lshift||opcode==opcodes.rshift||opcode==opcodes.and||opcode==opcodes.or){
				toRet = new int[3];
				toRet[0] = op1;
				toRet[1] = op2;
				toRet[2] = op3;
			}
			return toRet;
		}
		public void setOps(int[] ch){
			for(int i=0;i<ch.length;i++){
				if(i==0){
					this.op1 = ch[0];
				}
				else if(i==1){
					this.op2 = ch[1];
				}
				else if(i==2){
					this.op3 = ch[2];
				}
			}
		}
		public opcodes getOpcode(){
			return this.opcode;
		}
	}
	public static class commandext extends command{//allows for comands that are greater than the standard 3 commands
		private int op4;
		private int op5;
		public commandext(opcodes opcode, int op1){
			super(opcode,op1);
		}
	}
	public static class commandControl extends command{
		private String lable1;
		private String lable2;
		public commandControl(opcodes opcode, int op1, String lable1, String lable2){
			super(opcode,op1);
			this.lable1 = lable1;
			this.lable2 = lable2;
		}
		public commandControl(opcodes opcode, String label1){
			super(opcode,-1);//jumpI is unconditional
			this.lable1 = label1;
		}
		public commandControl(opcodes opcode, int reg){
			super(opcode,reg);//reg contains line num
		}
		public String getTrueLabel(){
			return lable1;
		}
		public String getFalseLabel(){
			return lable2;
		}
	}
}