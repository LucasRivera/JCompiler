package jcompiler.frontend.scanner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Reads in a text form a file or console in Backus–Naur form and stores important information in easily obtainable object
 * Example:
 * S ::= A | B
 * A ::= a S b | ~
 * B ::= bb
 * 
 * */
public class BNF {
	ArrayList<String> nonterminals = new ArrayList<String>();
	ArrayList<String> terminals    = new ArrayList<String>();//Set of terminals
	ArrayList<RewriteRule> productions = new ArrayList<RewriteRule>();
	public BNF(String filename){
		String rd="";
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
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
	}
	private void add(String rd) {
		
	}
	public static class RewriteRule{
		public RewriteRule(){}
	}
	public static void main(String[] args){
		
		
	}
}
