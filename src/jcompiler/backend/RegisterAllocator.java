package jcompiler.backend;
import java.util.ArrayList;

import jcompiler.utils.ILOC;

//Assumes feasible registers come after physical allocatable registers (A) (why not)
//Maybe add a program copy (maybe) depends on implementation
//r0=>1020
//gen loadI r0 => 1020
//In TopDownSimp
//Maybe reorder the spill set that way the loadi's are sequential

//Well define life ranges
public class RegisterAllocator {
	static int kR;
	static int A;
	//static int F;
	static int memstr = 1020;//Start of Memory Reserved for spill code TODO need to gen going down
	public static void main(String[] args) {
		//To make testing easier ill add this at the end
		int algo = -1;
		ILOC program = null;
		String ussage =
				"ERROR:  Incorrect Ussuage\n"
						+ "\tThe correct amount of arguments is 3 and their uses are described below:\n"
						+ "\tExample:\n"
						+ "\t$java RegisterAllocator k algo inputfile\n"
						+ "\t\targ1 = k\t\tThe amount of physical registers for the targeted machine\n"
						+ "\t\targ2 = algo\t\tThe algorithm that should be used:\n"
						+ "\t\t\ts\t\tFor simple top down\n"
						+ "\t\t\tt\t\tFor Full top down (With MAX_LIVE consideration)\n"
						+ "\t\t\tb\t\tFor Bottom up allocator\n"
						+ "\t\targ3 = inputfile\tThe ILOC code file that should be used for allocation\n"
						+ "\n\tThe output will be sent to stdout\n";
		if(args.length==3){
			try{
				kR = Integer.parseInt(args[0]);
			}
			catch(NumberFormatException er){
				System.out.println(ussage);
				System.exit(0);
			}
			String type = args[1];
			if(type.equalsIgnoreCase("s")){
				algo = 0;
			}
			else if(type.equalsIgnoreCase("t")){
				algo = 1;
			}
			else if(type.equalsIgnoreCase("b")){
				algo = 2;
			}
			else{
				System.out.println(ussage);
				System.exit(0);
			}
			program = new ILOC(args[2]);
		}
		else{
			System.out.println(ussage);
			System.exit(0);
		}
		switch(algo){
			case 0:
				topDownSimp(program).printProgram();
				System.exit(0);
			case 1:
				topDownFull(program).printProgram();
				System.exit(0);
			case 2:
				bottomUp(program).printProgram();
				System.exit(0);
		}
	}
	public static ArrayList<RegB> fillOccurence(ILOC program, ArrayList<RegB> OccurencePriority){
		ILOC.command cmd;
		RegB temp;
		for(int i=0;i<program.NumberOfInstructions();i++){
			cmd = program.getCommand(i);
			int[] toS = cmd.getOps();
			for(int j=0;j<toS.length;j++){
				if(!(j==0 && (cmd.getOpcode()==ILOC.opcodes.loadI || cmd.getOpcode()==ILOC.opcodes.output))){
					if(OccurencePriority.contains(new Reg(toS[j]))){
						temp = OccurencePriority.get(OccurencePriority.indexOf(new Reg(toS[j])));
						temp.incFreg();
						OccurencePriority.remove(temp);
						int k=0;
						for(k=0;k<OccurencePriority.size();k++){
							if(OccurencePriority.get(k).frequency < temp.frequency){
								OccurencePriority.add(k, temp);
								break;
							}
						}
						if(k==OccurencePriority.size()){
							if(OccurencePriority.size()!=0){
								OccurencePriority.add(OccurencePriority.size()-1, temp);
							}
							else{
								OccurencePriority.add(0, temp);
							}
						}
					}
					else{
						OccurencePriority.add(new RegB(toS[j]));
					}
				}
			}
		}
		return OccurencePriority;
	}
	public static ILOC AssignMap(ILOC program,map2D map, ArrayList<Reg> phys, ArrayList<RegB> spill){
		ILOC.command cmd;
		ILOC.command cmd1=null;
		ILOC.command cmd2=null;
		ILOC.command cmd3=null;
		ILOC.command cmd4=null;
		for(int i=0;i<program.NumberOfInstructions();i++){
			cmd = program.getCommand(i);
			ILOC.opcodes code = cmd.getOpcode();
			int[] ops = cmd.getOps();
			int j = -1;
			boolean sop1 = false;
			boolean sop2 = false;
			int d = 0;
			for(int k=0;k<ops.length;k++){//physical mappings
				if(!spill.contains((new Reg(ops[k]))) && !(code==ILOC.opcodes.output) && !(k==0 && code==ILOC.opcodes.loadI) ){
					ops[k] = -map.f(ops[k]);
				}
			}
			if(ops.length==3){
				if(spill.contains((new Reg(ops[0])))){
					sop1 = true;
				}
				if(spill.contains((new Reg(ops[1])))){
					sop2 = true;
				}
				if(sop1 && sop2){//both reads need to be pulled from spilled
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, memstr-4*spill.indexOf((new Reg(ops[0]))), phys.size()+1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, phys.size()+1, phys.size()+1);
					cmd3 = new ILOC.command(ILOC.opcodes.loadI, memstr-4*spill.indexOf((new Reg(ops[1]))), phys.size()+2);
					cmd4 = new ILOC.command(ILOC.opcodes.load, phys.size()+2, phys.size()+2);
					ops[0] = phys.size()+1;
					ops[1] = phys.size()+2;
					program.add(cmd4,i);
					program.add(cmd3,i);
					program.add(cmd2,i);
					program.add(cmd1,i);
					i = i + 4;
				}
				else if(sop1 || sop2){//its one of the reads
					if(sop1){//op1 to spill
						j = 0;
					}
					else{//op2 to spill
						j = 1;
					}
					//general spill code for both of them
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, memstr-4*spill.indexOf(new Reg(ops[j])), phys.size()+1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, phys.size()+1, phys.size()+1);
					ops[j] = phys.size()+1;
					program.add(cmd2,i);
					program.add(cmd1,i);
					i = i + 2;
				}
				if(spill.contains((new Reg(ops[2])))){
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, memstr-4*spill.indexOf(new Reg(ops[2])), phys.size()+2);
					cmd2 = new ILOC.command(ILOC.opcodes.store, phys.size()+1, phys.size()+2);
					ops[2] = phys.size()+1;
					program.add(cmd1,i+1);
					program.add(cmd2,i+2);
					i = i + 2;
				}
				sop1 = false;
				sop2 = false;
			}
			else if(ops.length == 2){
				if(spill.contains((new Reg(ops[1]))) && (code!=ILOC.opcodes.store)){//its a write
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, memstr-4*spill.indexOf((new Reg(ops[1]))), phys.size()+2);
					cmd2 = new ILOC.command(ILOC.opcodes.store, phys.size()+1, phys.size()+2);
					program.add(cmd1,i+1);
					program.add(cmd2,i+2);
					ops[1] = phys.size()+1;
					d = d + 2;
				}
				if((spill.contains((new Reg(ops[0])))) && (spill.contains((new Reg(ops[1])))) && (code==ILOC.opcodes.store)){//double read
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, memstr-4*spill.indexOf((new Reg(ops[0]))), phys.size()+1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, phys.size()+1, phys.size()+1);
					cmd3 = new ILOC.command(ILOC.opcodes.loadI, memstr-4*spill.indexOf((new Reg(ops[1]))), phys.size()+2);
					cmd4 = new ILOC.command(ILOC.opcodes.load, phys.size()+2, phys.size()+2);
					ops[0] = phys.size()+1;
					ops[1] = phys.size()+2;
					program.add(cmd4,i);
					program.add(cmd3,i);
					program.add(cmd2,i);
					program.add(cmd1,i);
					d = d + 4;
				}
				else if((spill.contains((new Reg(ops[0])))) && !(code==ILOC.opcodes.loadI)){//its a read
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, memstr-4*spill.indexOf((new Reg(ops[0]))), phys.size()+1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, phys.size()+1, phys.size()+1);
					ops[0] = phys.size()+1;
					program.add(cmd2,i);
					program.add(cmd1,i);
					d = d + 2;
				}
				else if(spill.contains((new Reg(ops[1]))) && (code==ILOC.opcodes.store)){//its a read
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, memstr-4*spill.indexOf((new Reg(ops[1]))), phys.size()+1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, phys.size()+1, phys.size()+1);
					ops[1] = phys.size()+1;
					program.add(cmd2,i);
					program.add(cmd1,i);
					d = d + 2;
				}
				i = i + d;
			}
			for(int k=0;k<ops.length;k++){//physical mappings
				if(!(code==ILOC.opcodes.output) && !(k==0 && code==ILOC.opcodes.loadI) ){
					ops[k] = Math.abs(ops[k]);
				}
			}
			cmd.setOps(ops);
			ops = cmd.getOps();
			/*DEBUGGING PURPOSES
			if(ops.length == 2){
				System.out.println("AFTER:\t"+"op1="+ops[0]+" op2="+ops[1]);
			}
			else if(ops.length == 3){
				System.out.println("AFTER:\t"+"op1="+ops[0]+" op2="+ops[1]+" op3="+ops[2]);
			}*/
		}
		return program;
	}
	public static ArrayList<RegX> RegBtoX(ILOC program, ArrayList<RegB> Occ){
		ArrayList<RegX> tor = new ArrayList<RegX>();
		int regnum;
		for(int i=0;i<Occ.size();i++){
			regnum = Occ.get(i).regnum;
			tor.add(new RegX(regnum,Occ.get(i).frequency, program.LifeBegin(regnum), program.LifeEnd(regnum)));
		}
		return tor;
	}
	public static ArrayList<RegU> RegUtoX(ILOC program, ArrayList<RegB> Occ){
		ArrayList<RegU> tor = new ArrayList<RegU>();
		int regnum;
		for(int i=0;i<Occ.size();i++){
			regnum = Occ.get(i).regnum;
			tor.add(new RegU(regnum,Occ.get(i).frequency,program.LifeBegin(regnum),program.LifeEnd(regnum)));
		}
		return tor;
	}
	public static ArrayList<RegX> liveAt(ArrayList<RegX> full, int place){
		ArrayList<RegX> live = new ArrayList<RegX>();
		RegX itr;
		for(int i=0;i<full.size();i++){
			itr = full.get(i);
			if(!(place>itr.end || place<itr.begin)){
				live.add(itr);
			}
		}
		return live;
	}
	public static ArrayList<RegU> liveAtU(ArrayList<RegU> full, int place){
		ArrayList<RegU> live = new ArrayList<RegU>();
		RegU itr;
		for(int i=0;i<full.size();i++){
			itr = full.get(i);
			if(!(place>itr.end || place<itr.begin)){
				live.add(itr);
			}
		}
		return live;
	}
	//This methods looks to find out if any register life range eneded
	//that way if they have ended that physical register can
	//be reused for another virtual register
	public static void update(ArrayList<RegX> inUse, ArrayList<RegX> list, int place, ArrayList<Reg> phys, map2D map){
		for(int i=0;i<inUse.size();i++){
			if(inUse.get(i).end < place){
				phys.add(new Reg(map.f(list.remove(list.indexOf(inUse.remove(i))).regnum)));
				//i--;
				i = -1;
			}
		}
	}
	public static void update(int place, ArrayList<RegU> active, ArrayList<Reg> free, map2D map){
		RegU temp = null;
		for(int i=0;i<active.size();i++){
			if(active.get(i).end < place){
				temp = active.remove(i);
				free.add(new Reg(map.f(temp.regnum)));
				map.remove(temp.regnum);
				i = -1;
				//physC--;
			}
		}
		//return physC;
	}
	public static void findNextHits(ILOC program, int startpoint, ArrayList<RegU> active){
		int[] ops;
		for(int k=0;k<active.size();k++){
			for(int i=startpoint+1;i<program.NumberOfInstructions();i++){
				ops = program.getCommand(i).getOps();
				if(ops.length==3){
					if(active.get(k).regnum==ops[0] ||active.get(k).regnum==ops[1] ||active.get(k).regnum==ops[2]){
						active.get(k).nextHit = i;
						break;
					}
				}
				if(ops.length==2){
					if(program.getCommand(i).getOpcode()==ILOC.opcodes.loadI){
						if(active.get(k).regnum==ops[1]){
							active.get(k).nextHit = i;
							break;
						}
					}
					else{
						if(active.get(k).regnum==ops[0] ||active.get(k).regnum==ops[1]){
							active.get(k).nextHit = i;
							break;
						}
					}
				}
			}
		}
	}
	public static void close(){
		System.out.println("Not enough physical registers where given to run all posibale ILOC instructions");
		System.exit(0);
	}
	public static int[] getMaxLiveAt(ILOC program,ArrayList<RegX> list){
		int[] ret = new int[2];
		ArrayList<RegX> live;
		int max = -1;
		int maxAt = -1;
		for(int i=0;i<program.NumberOfInstructions();i++){
			live = liveAt(list,i);
			if(live.size()>max){
				max = live.size();
				maxAt = i;
			}
		}
		ret[0] = maxAt;
		ret[1] = max;
		return ret;
	}
	//Top down allocator that assumes all life ranges are the same(i.e through out the entire basic block)
	public static ILOC topDownSimp(ILOC program){
		A = kR-3;//damn r0 crap
		if(A<0){
			close();
		}
		ArrayList<RegB> OccurencePriority = new ArrayList<RegB>();
		ArrayList<RegB> spill = new ArrayList<RegB>();
		ArrayList<RegB> active = new ArrayList<RegB>();
		ArrayList<Reg> phys = new ArrayList<Reg>();
		if(program==null){//GIGO garbage in => garbage out
			return null;
		}
		OccurencePriority = fillOccurence(program,OccurencePriority);////figures out the occurrences and orders the virtual registers in order of occurrences/frequency
		//================  Mapping/Allocation Code Logic is here   ======================================
		map2D map = new map2D();
		if(A<OccurencePriority.size()){
			int toSpill = OccurencePriority.size() - A;
			for(int i=0;i<A;i++){
				active.add(OccurencePriority.get(i));//might not need an active set due to the map feature TODO
				phys.add(new Reg(i+1));
				map.add(active.get(i).regnum, (i+1));
			}
			for(int i=OccurencePriority.size()-1;i>=OccurencePriority.size()-toSpill;i--){
				spill.add(OccurencePriority.get(i));
			}
		}
		else{
			/*  This is the easy/nice scenario where the amount of 
			    physical registers is greater then virtual regs(values)
			    each virtual register fits into a physical register */
			for(int i=0;i<OccurencePriority.size();i++){
				map.add(OccurencePriority.get(i).regnum, i+1);
				phys.add(new Reg(i+1));
			}
		}
		/*DEBUGGING PURPOSES
		System.out.println(map.toString());
		System.out.println("Spilled Registers");
		for(int i=0;i<spill.size();i++){
			System.out.println("r="+spill.get(i).regnum);
		}
		System.out.println("Physical Registers");
		for(int i=0;i<phys.size();i++){
			System.out.println("r="+phys.get(i).regnum);
		}*/
		program = AssignMap(program,map,phys,spill);
		program.add(new ILOC.command(ILOC.opcodes.loadI,1020,0),0);
		return program;
	}
	//Top down allocator that figures out the MAX_LIVE and the life ranges
	public static ILOC topDownFull(ILOC program){
		A = kR-3;//damn r0
		//F = kR-2;
		if(A<0){
			close();
		}
		ArrayList<RegB> OP = new ArrayList<RegB>();
		ArrayList<RegX> list = new ArrayList<RegX>();
		OP = fillOccurence(program,OP);
		list = RegBtoX(program,OP);
		/*DEBUGGING PURPOSES
		for(int i=0;i<list.size();i++){
			RegX temp = list.get(i);
			System.out.println("r"+temp.regnum+"\tOCC:"+temp.frequency+"\tLifeB:"+(temp.begin+1)+"\tLifeE:"+(temp.end+2)+"\tLifeL:"+temp.life);
		}*/
		/*
		Now we have a list of virtual registers, that are ordered by Occurrence
		and contain their life ranges
		===========Allocation/Map Stage =================*/
		map2D map = new map2D();
		ArrayList<RegX> live;
		ArrayList<RegX> inUse = new ArrayList<RegX>();
		ArrayList<Reg> phys = new ArrayList<Reg>();
		ArrayList<RegB> spill = new ArrayList<RegB>();
		for(int i=1;i<=A;i++){
			phys.add(new Reg(i));
		}
		ArrayList<RegX> mins = new ArrayList<RegX>();
		int maxLiveAt=-2;
		int maxLive = -2;
		int minOcR = Integer.MAX_VALUE;
		int maxLife = -3;
		int maxLifeAt = -3;
		int[] ret = new int[2];
		do{
			ret = getMaxLiveAt(program,list);
			maxLiveAt = ret[0];
			maxLive = ret[1];
			if(A>=maxLive){//this should work if A==0 then maxlive needs to be 0 which would happend if everything got spilled
				break;
			}
			live = liveAt(list,maxLiveAt);
			for(int i=0;i<live.size();i++){
				RegX temp = live.get(i);
			}
			//gets the min of occurence (since there can be a n-way tie need another for loop)
			for(int i=0;i<live.size();i++){
				if(live.get(i).frequency<minOcR){
					minOcR = live.get(i).frequency;
				}
			}
			for(int i=0;i<live.size();i++){
				RegX temp = live.get(i);
				if(temp.frequency == minOcR){
					mins.add(temp);
				}
			}
			if(mins.size()>1){//theres a n-way tie find longest life range to spill
				for(int j=0;j<mins.size();j++){
					if(maxLife<=mins.get(j).life){
						maxLife = mins.get(j).life;
						maxLifeAt = j;
					}
				}
				spill.add(mins.get(maxLifeAt));
				list.remove(mins.get(maxLifeAt));
			}
			else{//there is no tie just spill the leaste occurence
				spill.add(mins.get(0));
				list.remove(mins.get(0));
			}
			mins = new ArrayList<RegX>();//reseting
			minOcR=Integer.MAX_VALUE;
			maxLife = -3;maxLifeAt = -3;
		}while(true);
		//======End of Spill Regs ===================================================//
		/*
		System.out.println("Spilled Registers");
		for(int i=0;i<spill.size();i++){
			System.out.println("r="+spill.get(i).regnum);
		}
		System.out.println("Allocable Registers");
		for(int i=0;i<list.size();i++){
			RegX temp = list.get(i);
			System.out.println("r="+temp.regnum +"\tLifeB:"+temp.begin+"\tLifeE:"+temp.end);
		}*/
		//System.out.println("Mapping . . . ");
		//LIST NOW CONATINS ONLY REGISTERS THAT ARE READY FOR MAPPING!
		for(int i=0;i<program.NumberOfInstructions();i++){
			ILOC.command cmd=program.getCommand(i);
			int[] ops = cmd.getOps();
			if(cmd.getOpcode()==ILOC.opcodes.output){
				continue;//No Reg to add
			}else if(cmd.getOpcode()==ILOC.opcodes.loadI){
				if(!(inUse.contains(new Reg(ops[1]))) && (list.contains(new Reg(ops[1])))){
					map.add(ops[1], phys.remove(0).regnum);
					//physAt++;
					inUse.add(list.get(list.indexOf(new Reg(ops[1]))));
				}
			}else{
				if(ops.length == 2){
					if(!(inUse.contains(new Reg(ops[0]))) && (list.contains(new Reg(ops[0])))){
						map.add(ops[0], phys.remove(0).regnum);
						//physAt++;
						inUse.add(list.get(list.indexOf(new Reg(ops[0]))));
					}
					else if(!(inUse.contains(new Reg(ops[1]))) && (list.contains(new Reg(ops[1])))){
						map.add(ops[1], phys.remove(0).regnum);
						//physAt++;
						inUse.add(list.get(list.indexOf(new Reg(ops[1]))));
					}
				}
				else{//size is 3
					if(!(inUse.contains(new Reg(ops[0]))) && (list.contains(new Reg(ops[0])))){
						map.add(ops[0], phys.remove(0).regnum);
						//physAt++;
						inUse.add(list.get(list.indexOf(new Reg(ops[0]))));
					}
					else if(!(inUse.contains(new Reg(ops[1]))) && (list.contains(new Reg(ops[1])))){
						map.add(ops[1], phys.remove(0).regnum);
						//physAt++;
						inUse.add(list.get(list.indexOf(new Reg(ops[1]))));
					}
					else if(!(inUse.contains(new Reg(ops[2]))) && (list.contains(new Reg(ops[2])))){
						map.add(ops[2], phys.remove(0).regnum);
						//physAt++;
						inUse.add(list.get(list.indexOf(new Reg(ops[2]))));
					}
				}
			}
			update(inUse,list,i+1,phys,map);
		}
		program = AssignMap(program,map,phys,spill);
		program.add(new ILOC.command(ILOC.opcodes.loadI,1020,0),0);
		return program;
	}
	public static ILOC bottomUp(ILOC program){
		A = kR - 2;//one less feasable since you use the reg before you spill (always keeping one reg free)
		//I feel like the min(A) = 1, therefore min(kR)=3
		//op add f1,f2=>b1 spill b1
		if(A<1){
			close();
		}
		ArrayList<RegB> OP = new ArrayList<RegB>();
		ArrayList<RegU> list = new ArrayList<RegU>();
		ArrayList<Reg> free = new ArrayList<Reg>();
		ArrayList<RegU> active = new ArrayList<RegU>();
		ArrayList<RegU> spill = new ArrayList<RegU>();
		ArrayList<RegU> ntd = new ArrayList<RegU>();
		OP = fillOccurence(program,OP);
		list = RegUtoX(program,OP);
		for(int i=1;i<=A;i++){
			free.add(new Reg(i));
		}
		int F1 = A;
		int F2 = A + 1;
		//int physAt = 0;
		int spillnum = 0;
		int farH = -1;
		int farAt = -1;
		map2D map = new map2D();
		map2D smap= new map2D();
		ILOC.command cmd1;
		ILOC.command cmd2;
		ILOC.command cmd3;
		ILOC.command cmd4;
		RegU temp1=null;
		for(int i=0;i<program.NumberOfInstructions();i++){
			ILOC.command cmd=program.getCommand(i);
			ILOC.opcodes code = cmd.getOpcode();
			if(cmd.getOpcode()==ILOC.opcodes.output){
				continue;//No Reg to add
			}
			int[] ops = cmd.getOps();
			int[] nps = cmd.getOps();
			for(int j=0;j<ops.length;j++){
				if(!(j==0 && code==ILOC.opcodes.loadI)){
					if(!active.contains((new Reg(ops[j])))){
						if(spill.contains((new Reg(ops[j])))){
							smap.remove(spill.remove(spill.indexOf((new Reg(ops[j])))).regnum);
						}
						ntd.add(list.get(list.indexOf((new Reg(ops[j])))));
					}
				}
			}
			while(ntd.size()>0){
				while(free.size()>0 && ntd.size()>0){
					temp1 = ntd.remove(0);
					active.add(temp1);
					map.add(temp1.regnum,free.remove(0).regnum);
				}
				if(free.size()==0){//bag is full need to set one free
					findNextHits(program,i,active);
					farH = -1;farAt=-1;//reseting
					for(int k=0;k<active.size();k++){
						if(farH<active.get(k).nextHit){
							farAt = k;
							farH = active.get(k).nextHit;
						}
					}//now we know who has the farset hit
					if(farAt>-1){
						temp1 = active.remove(farAt);
						free.add(new Reg(map.f(temp1.regnum)));//TODO this was an addtion to the origanal code
						map.remove(temp1.regnum);
						spill.add(temp1);
						smap.add(temp1.regnum,memstr-4*spillnum);
						spillnum++;
					}else{
						System.out.println("Opcode = "+code+" @line "+i);
						System.out.println("size of bag is = " + active.size());
						System.out.println("Size of free is = "+free.size());
						update(i,active,free,map);
						System.out.println("size of bag is = " + active.size());
						System.out.println("Size of free is = "+free.size());
						System.exit(0);
					}
				}
			}
			if(ops.length == 3){//to phys
				if(active.contains(new Reg(ops[0]))){
					nps[0] = map.f(ops[0]);
				}
				else{//in spill
					nps[0] = smap.f(ops[0]);
				}
				if(active.contains(new Reg(ops[1]))){
					nps[1] = map.f(ops[1]);
				}
				else{//in spill
					nps[1] = smap.f(ops[1]);
				}
				if(active.contains(new Reg(ops[2]))){
					nps[2] = map.f(ops[2]);
				}
				else{//in spill
					nps[2] = smap.f(ops[2]);
				}
				cmd = new ILOC.command(cmd.getOpcode(),nps[0],nps[1],nps[2]);
			}
			else if(ops.length == 2){
				if(cmd.getOpcode()==ILOC.opcodes.loadI){
					nps[0] = ops[0];
					if(active.contains(new Reg(ops[1]))){
						nps[1] = map.f(ops[1]);
					}
					else{//in spill
						nps[1] = smap.f(ops[1]);
					}
				}
				else{
					if(active.contains(new Reg(ops[0]))){
						nps[0] = map.f(ops[0]);
					}
					else{//in spill
						nps[0] = smap.f(ops[0]);
					}
					if(active.contains(new Reg(ops[1]))){
						nps[1] = map.f(ops[1]);
					}
					else{//in spill
						nps[1] = smap.f(ops[1]);
					}
				}
				cmd = new ILOC.command(cmd.getOpcode(),nps[0],nps[1]);
			}
			//gen code
			boolean sop1 = false;
			boolean sop2 = false;
			int d = 0,j=0;
			if(ops.length==3){
				if(spill.contains((new Reg(ops[0])))){
					sop1 = true;
				}
				if(spill.contains((new Reg(ops[1])))){
					sop2 = true;
				}
				if(sop1 && sop2){//both reads need to be pulled from spilled
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, smap.f(ops[0]), F1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, F1, F1);
					cmd3 = new ILOC.command(ILOC.opcodes.loadI, smap.f(ops[1]), F2);
					cmd4 = new ILOC.command(ILOC.opcodes.load, F2, F2);
					nps[0] = F1;
					nps[1] = F2;
					program.add(cmd4,i);
					program.add(cmd3,i);
					program.add(cmd2,i);
					program.add(cmd1,i);
					i = i + 4;
				}
				else if(sop1 || sop2){//its one of the reads
					if(sop1){//op1 to spill
						j = 0;
					}
					else{//op2 to spill
						j = 1;
					}
					//general spill code for both of them
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, smap.f(ops[j]), F1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, F1, F1);
					ops[j] = F1;
					program.add(cmd2,i);
					program.add(cmd1,i);
					i = i + 2;
				}
				if(spill.contains((new Reg(ops[2])))){//its a write
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, smap.f(ops[2]), F2);
					cmd2 = new ILOC.command(ILOC.opcodes.store, F1, F2);
					ops[2] = F1;
					program.add(cmd1,i+1);
					program.add(cmd2,i+2);
					i = i + 2;
				}
				sop1 = false;
				sop2 = false;
			}
			else if(ops.length == 2){
				if(spill.contains((new Reg(ops[1]))) && (code!=ILOC.opcodes.store)){//its a write
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, smap.f(ops[1]), F2);
					cmd2 = new ILOC.command(ILOC.opcodes.store, F1, F2);
					ops[1] = F1;
					program.add(cmd1,i+1);
					program.add(cmd2,i+2);
					d = d + 2;
				}
				if((spill.contains((new Reg(ops[0])))) && (spill.contains((new Reg(ops[1])))) && (code==ILOC.opcodes.store)){//double read
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, smap.f(ops[0]), F1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, F1, F1);
					cmd3 = new ILOC.command(ILOC.opcodes.loadI, smap.f(ops[1]), F2);
					cmd4 = new ILOC.command(ILOC.opcodes.load, F2, F2);
					ops[0] = F1;
					ops[1] = F2;
					program.add(cmd4,i);
					program.add(cmd3,i);
					program.add(cmd2,i);
					program.add(cmd1,i);
					d = d + 4;
				}
				else if((spill.contains((new Reg(ops[0])))) && !(code==ILOC.opcodes.loadI)){//its a read
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, smap.f(ops[0]), F1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, F1, F1);
					ops[0] = F1;
					program.add(cmd2,i);
					program.add(cmd1,i);
					d = d + 2;
				}
				else if(spill.contains((new Reg(ops[1]))) && (code==ILOC.opcodes.store)){//its a read
					cmd1 = new ILOC.command(ILOC.opcodes.loadI, smap.f(ops[1]), F1);
					cmd2 = new ILOC.command(ILOC.opcodes.load, F1, F1);
					ops[1] = F1;
					program.add(cmd2,i);
					program.add(cmd1,i);
					d = d + 2;
				}
				i = i + d;
			}
			update(i+1,active,free,map);
			//need to change the virt->phys
			
		}
		program.add(new ILOC.command(ILOC.opcodes.loadI,1020,0),0);
		return program;
	}

	public static class Reg{
		int regnum;
		public Reg(int regnum){
			this.regnum = regnum;
		}
		public boolean equals(Object obj){
			RegB comp = (RegB)obj;
			if(regnum==comp.regnum){
				return true;
			}
			else{
				return false;
			}
			
		}	
	}
	public static class RegB extends Reg{
		//private int regnum;
		protected int frequency=1;
		public RegB(int regnum){
			//this.regnum = regnum;
			super(regnum);
		}
		public RegB(int regnum, int freq){
			super(regnum);
			this.frequency = freq;
		}
		public void incFreg(){
			this.frequency++;
		}
		
		@Override
		public boolean equals(Object obj){
			Reg comp = (Reg)obj;
			if(regnum==comp.regnum){
				return true;
			}
			else{
				return false;
			}
		}	
	}
	public static class RegX extends RegB{
		protected int life;//Life width
		protected int begin;//Beginning of Life Range
		protected int end;//End of Life Range
		//private int[] hits;//LOC where the register was used
		public RegX(int regnum,int begin, int end){
			super(regnum);
			this.begin = begin;
			this.end = end;
			this.life = this.end - this.begin;
		}
		public RegX(int regnum,int frequency, int begin, int end){
			super(regnum,frequency);
			this.begin = begin;
			this.end = end;
			this.life = this.end - this.begin;
		}
	}
	public static class RegU extends RegX{
		int nextHit=0;
		public RegU(int regnum,int frequency, int begin, int end){
			super(regnum,frequency,begin,end);
		}
		public RegU(int regnum,int begin, int end){
			super(regnum,begin,end);
		}
	}
	public static class map2D{
		public class point2D{
			private int x;
			private int y;
			public point2D(int x,int y){
				this.x = x;
				this.y = y;
			}
			public String toString(){
				return "f("+x+")="+y+"\n";
			}
		}
		private ArrayList<point2D> set = new ArrayList<point2D>();
		public void add(int x, int y){
			add(new point2D(x,y));
		}
		public void add(point2D p){
			set.add(p);
		}
		public boolean remove(int x){
			return remove(x,this.f(x));
		}
		public boolean remove(int x, int y){
			return remove(new point2D(x,y));
		}
		public boolean remove(point2D p){
			return set.remove(p);
		}
		public int f(int x){
			for(int i=0;i<set.size();i++){
				if(x==set.get(i).x){
					return set.get(i).y;
				}
			}
			return x;//error occurred could not find a mapping, so the input is re-given has the output
		}
		public String toString(){
			String tor = "";
			for(int i=0;i<set.size();i++){
				tor+=set.get(i).toString();
			}
			return tor;
		}
	}
}
