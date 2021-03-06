package com.mrh0.qspl.tokenizer;

import java.util.ArrayList;
import com.mrh0.qspl.io.console.Console;
import com.mrh0.qspl.tokenizer.statement.StatementStack;
import com.mrh0.qspl.tokenizer.token.Token;
import com.mrh0.qspl.tokenizer.token.TokenBlock;
import com.mrh0.qspl.tokenizer.token.TokenType;

public class SecondPass {
	private int line;
	private StatementStack stmts;
	private boolean newCall = false;
	private boolean funcCall = false;
	private boolean append = false;
	private int appendType = 0; // 1 = : 2 = ::
	
	public SecondPass(ArrayList<Token> tokens) {
		stmts = new StatementStack();
		stmts.push(TokenType.CODE_BLOCK);
		line = 1;
		Console.g.setCurrentLine(line);
		
		for(int i = 0; i < tokens.size(); i++) {
			Token cur = tokens.get(i);
			TokenType t = cur.getType();
			String s = cur.getToken();
			
			if(append && t != TokenType.BEGIN_BLOCK && t != TokenType.LN_BRK) {
				Console.g.err("Token ':' expects {block}, got '" + t + "'.");
				end();
			}
			
			if(t == TokenType.OP_KEYWORD) {
				switch(s) {
					case "new":
						newCall = true;
						continue;
					case "$":
						newCall = true;
						continue;
				}
			}
			else if(t == TokenType.PRE_BLOCK_KEYWORD) {
				switch(s) {
					case "func":
						stmts.peek().setFunc();
						funcCall = true;
						break;
					case "fn":
						stmts.peek().setFunc();
						funcCall = true;
						break;
				}
			}
			else if(t == TokenType.APPEND) {
				append = true;
				appendType = s.length();
				//stmts.feed(cur);
				continue;
			}
			else if(t == TokenType.SEPERATOR) {
				if(s.equals("(") || s.equals(")")) {
					stmts.feed(cur);
					
					end();
					continue;
				}
				if(s.equals(","))
					stmts.finishStatement();
				
				end();
				continue;
			}
			else if(t == TokenType.LN_BRK) {
				Console.g.setCurrentLine(line++);
				
				//end();
				continue;
			}
			else if(t == TokenType.END) {
				stmts.finishStatement();
				end();
				continue;
			}
			else if(t == TokenType.BEGIN_BLOCK) {
				
				
				//stmts.push(TokenType.CODE_BLOCK);
				if(funcCall) {
					stmts.push(s.equals("[")?TokenType.ARG_BLOCK:TokenType.CODE_BLOCK, append);
					
					append = false;
					appendType = 0;
					end();
					continue;
				}
				
				//stmts.peek().isFunc()?TokenType.CODE_BLOCK:TokenType.IF_BLOCK
				TokenType blkt = TokenType.CODE_BLOCK;//(appendType == 1)?(TokenType.CODE_BLOCK):((appendType == 2)?TokenType.WHILE_BLOCK:TokenType.CODE_BLOCK);
				stmts.push(s.equals("{")?(newCall?TokenType.OBJ_BLOCK:blkt):(newCall?TokenType.ARY_BLOCK:TokenType.ACCESSOR_BLOCK), append);
				
				append = false;
				appendType = 0;
				end();
				continue;
			}
			else if(t == TokenType.END_BLOCK) {
				
				stmts.finishStatement();
				TokenBlock tb = stmts.pop();
				end();
				continue;
			}
			
			stmts.feed(cur);
		}
		
		
	}
	
	private void end() {
		newCall = false;
		funcCall = false;
		append = false;
	}
	
	public TokenBlock get() {
		TokenBlock b = null;
		while(!stmts.isEmpty())
			b = stmts.pop();
		System.out.println("sp: "+b.toString());
		return b;
	}
}
