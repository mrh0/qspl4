package com.mrh0.qspl.interpreter.evaluator;

import java.util.ArrayList;
import java.util.Iterator;

import com.mrh0.qspl.io.console.Console;
import com.mrh0.qspl.tokenizer.statement.Statement;
import com.mrh0.qspl.tokenizer.token.Token;
import com.mrh0.qspl.tokenizer.token.TokenBlock;
import com.mrh0.qspl.tokenizer.token.TokenVal;
import com.mrh0.qspl.type.TArray;
import com.mrh0.qspl.type.TContainer;
import com.mrh0.qspl.type.TString;
import com.mrh0.qspl.type.TUndefined;
import com.mrh0.qspl.type.Val;
import com.mrh0.qspl.type.func.Arguments;
import com.mrh0.qspl.type.func.TFunc;
import com.mrh0.qspl.type.func.UserFunc;
import com.mrh0.qspl.type.iterator.IIterable;
import com.mrh0.qspl.type.iterator.TFilterIterator;
import com.mrh0.qspl.type.iterator.TRangeIterator;
import com.mrh0.qspl.type.number.TNumber;
import com.mrh0.qspl.type.var.Var;
import com.mrh0.qspl.type.var.VarDef;
import com.mrh0.qspl.type.iterator.TVariableIterator;
import com.mrh0.qspl.vm.VM;

public class StatementEval {
	private Statement s;
	private ValStack vals;//ArrayDeque<Val>
	private VM vm;
	private BlockType bt;
	private boolean blockPass = false;
	private IfChainState ifChainState;
	
	public static boolean exiting = false;
	public static EvalResult exitResult = new EvalResult(TUndefined.getInstance());
	
	public static void exit(Val v) {
		exiting = true;
		exitResult = new EvalResult(v);
	}
	
	public static void exit(EvalResult r) {
		exiting = true;
		exitResult = r;
	}
	
	public static EvalResult cancelExit() {
		exiting = false;
		EvalResult r = exitResult;
		exitResult = new EvalResult(TUndefined.getInstance());
		return r;
	}
	
	public enum IfChainState {
		PASS, FAIL, BLOCKED
	};
	
	private enum BlockType {
		CODE, ARRAY, OBJECT, CONTAINER, ACCESSOR
	}
	
	public StatementEval(Statement s, VM vm, BlockType bt, ValStack vals, boolean blockPass, IfChainState ifChainState) {
		this.s = s;
		vals.clear();
		this.vals = vals;
		this.vm = vm;
		this.bt  = bt;
		this.blockPass = blockPass;
		this.ifChainState = ifChainState;
	}
	
	public StatementEval(Statement s, VM vm, ValStack vals) {
		this.s = s;
		vals.clear();
		this.vals = vals;
		this.vm = vm;
		this.bt  = BlockType.CODE;
	}
	
	public EvalResult eval() {
		if(s.length() > 0)
			Console.g.setCurrentLine(s.getToken(0).getLine());
		for(int i = 0; i < s.length(); i++) {
			Token t = s.getToken(i);
			//Console.g.currentLine = t.line;
			Val hl;
			Val vl;
			if(exiting)
				return new EvalResult();
			
			switch(t.getType()) {
				case OPERATOR:
					switch(t.getToken()) {
						case "+":
							hl = vals.pop();
							vals.push(vals.pop().add(hl));
							break;
						case "-":
							hl = vals.pop();
							vals.push(vals.pop().sub(hl));
							break;
						case "*":
							hl = vals.pop();
							vals.push(vals.pop().multi(hl));
							break;
						case "/":
							hl = vals.pop();
							vals.push(vals.pop().div(hl));
							break;
						case "%":
							hl = vals.pop();
							vals.push(vals.pop().mod(hl));
							break;
							
						case "++":
							vals.push(vals.pop().increment(TNumber.create(1)));
							break;
						case "--":
							vals.push(vals.pop().decrement(TNumber.create(1)));
							break;
						case "**":
							hl = vals.pop();
							vals.push(vals.pop().pow(hl));
							break;
							
						case "==":
							hl = vals.pop();
							vals.push(TNumber.create(vals.pop().equals(hl)));
							break;
						case "!=":
							hl = vals.pop();
							vals.push(TNumber.create(!vals.pop().equals(hl)));
							break;
							
						case "is":
							hl = vals.pop();
							vals.push(vals.pop().is(hl));
							break;
						case "as":
							hl = vals.pop();
							vals.push(vals.pop().as(hl));
							break;
							
						case "<":
							hl = vals.pop();
							vals.push(TNumber.create(vals.pop().compare(hl)==-1));
							break;
						case ">":
							hl = vals.pop();
							vals.push(TNumber.create(vals.pop().compare(hl)==1));
							break;
						case "<=":
							hl = vals.pop();
							vals.push(TNumber.create(vals.pop().compare(hl)!=1));
							break;
						case ">=":
							hl = vals.pop();
							vals.push(TNumber.create(vals.pop().compare(hl)!=-1));
							break;
							
						case "<<":
							hl = vals.pop();
							vals.push(vals.pop().shiftLeft(hl));
							break;
						case ">>":
							hl = vals.pop();
							vals.push(vals.pop().shiftRight(hl));
							break;
						case "<<<":
							hl = vals.pop();
							vals.push(vals.pop().rotateLeft(hl));
							break;
						case ">>>":
							hl = vals.pop();
							vals.push(vals.pop().rotateRight(hl));
							break;
							
						case "->":
							hl = vals.pop();
							vals.push(vals.pop().pull(hl));
							break;
						case "<-":
							hl = vals.pop();
							vals.push(vals.pop().push(hl));
							break;
							
						case "&&":
							hl = vals.pop();
							vals.push(vals.pop().logicalAnd(hl));
							break;
						case "||":
							hl = vals.pop();
							vals.push(vals.pop().logicalOr(hl));
							break;
						case "!":
							vals.push(vals.pop().logicalNot());
							break;
						case "~":
							vals.push(vals.pop().approx());
							break;
						case "...":
							hl = vals.pop();
							vals.push(new TRangeIterator(TNumber.from(vals.pop()).integerValue(), TNumber.from(hl).integerValue()));
							break;
							
						case "=":
							hl = vals.pop();
							vl = bt == BlockType.CODE?vals.pop():vals.pop().duplicate();
							vals.push(vl.assign(hl));
							break;
						case "+=":
							hl = vals.pop();
							if(bt != BlockType.CODE)
								Console.g.err("Invalid use of operator '+='.");
							vl = vals.pop();
							vals.push(vl.assign(vl.add(hl)));
							break;
						case "-=":
							hl = vals.pop();
							if(bt != BlockType.CODE)
								Console.g.err("Invalid use of operator '-='.");
							vl = vals.pop();
							vals.push(vl.assign(vl.sub(hl)));
							break;
						case "*=":
							hl = vals.pop();
							if(bt != BlockType.CODE)
								Console.g.err("Invalid use of operator '*='.");
							vl = vals.pop();
							vals.push(vl.assign(vl.multi(hl)));
							break;
						case "/=":
							hl = vals.pop();
							if(bt != BlockType.CODE)
								Console.g.err("Invalid use of operator '/='.");
							vl = vals.pop();
							vals.push(vl.assign(vl.div(hl)));
							break;
						case "%=":
							hl = vals.pop();
							if(bt != BlockType.CODE)
								Console.g.err("Invalid use of operator '%='.");
							vl = vals.pop();
							vals.push(vl.assign(vl.mod(hl)));
							break;
						case "#":
							hl = vals.pop();
							if(bt != BlockType.CODE)
								Console.g.err("Invalid use of operator '#'.");
							vl = vals.pop();
							vals.push(vl.accessor(new TString(Var.from(hl).getName())));
							break;
						default:
							System.err.println("Unknown op: " + t.getToken());
							break;
					}
					break;
					
				//Value
				case VAL:
					vals.push(((TokenVal)t).getValue());
					break;
				case IDENTIFIER:
					Var var = vm.getVariable(t.getToken());
					vals.push(var);
					break;
					
				case OP_KEYWORD:
					switch(t.getToken()) {
						case "in":
							hl = vals.pop();
							vals.push(new TVariableIterator(Var.from(vals.pop()), IIterable.from(hl)));
							break;
						case "let":
							if(!vals.isEmpty() && bt == BlockType.CODE) {
								Val v = vals.peek();
								if(v.isVariable()) {
									vals.pop();
									vals.push(vm.setVariable((Var)v));
								}
							}
							else {
								Console.g.err("Keyword 'let' used in illegal context.");
							}
							break;
						case "where":
							hl = vals.pop();
							vals.push(new TFilterIterator(TFunc.from(hl), IIterable.from(vals.pop()), vm));
							break;
						}
					break;
				
				//Value keyword
				case VAL_KEYWORD:
					switch(t.getToken()) {
						case "true":
							vals.push(TNumber.create(true));
							break;
						case "false":
							vals.push(TNumber.create(false));
							break;
						case "undefined":
							vals.push(TUndefined.getInstance());
							break;
						/*case "else":
							vals.push(TNumber.create(!blockPass));
							break;*/
						case "prev":
							vals.push(vm.getPreviousResult());
							break;
					}
					
				//Tail Keyword
				case TAIL_KEYWORD:
					switch(t.getToken()) {
						case "out":
							Console.g.log(vals.isEmpty()?TUndefined.getInstance():vals.peek());
							break;
						case "assert":
							if(vals.isEmpty() || !vals.peek().booleanValue())
								Console.g.err("Assertion '"+s+"' was made false.");
							break;
						case "error":
							Console.g.err(vals.isEmpty()?TUndefined.getInstance():vals.peek());
							break;
						case "val":
							if(vals.peek().isVariable())
								vals.push(((Var)vals.pop()).get());
							break;
						case "delete":
							if(vals.peek().isVariable())
								((Var)vals.pop()).delete(vm);
							else
								Console.g.err("Cannot preform delete on type " + vals.peek().getTypeName());
							break;
						case "exit":
							if(vals.isEmpty()) {
								exit(TUndefined.getInstance());
								return new EvalResult(TUndefined.getInstance());
							}
							else {
								exit(vals.peek());
								return new EvalResult(vals.pop());
							}
					}
					break;
					
				
				//Pre Block Keyword
				case PRE_BLOCK_KEYWORD:
					Token nt = s.getToken(i-1);
					switch(t.getToken()) {
						case "else":
							if((vals.isEmpty()|| vals.pop().booleanValue()) && ifChainState == IfChainState.FAIL) {
								vals.push(evalBlock((TokenBlock)s.getToken(i-1), vm).getResult());
								ifChainState = IfChainState.BLOCKED;
							}
							break;
						case "if":
							ifChainState = IfChainState.FAIL;
							if(vals.pop().booleanValue()) {
								vals.push(evalBlock((TokenBlock)s.getToken(i-1), vm).getResult());
								ifChainState = IfChainState.BLOCKED;
							}
							break;
						case "func":
							hl = vals.pop();
							vl = vals.pop();
							if(hl.isFunction())
								Console.g.err("Function has already been defined.");
							if(!hl.isContainer() || !vl.isVariable() || !(nt instanceof TokenBlock))
								Console.g.err("Malformed function definition." + nt);
							
							TokenBlock nblock = (TokenBlock)nt;
							vl.assign(new UserFunc(nblock, TContainer.from(hl)));
							vals.push(new VarDef((Var)vl));
							break;
						case "loop":
							ifChainState = IfChainState.FAIL;
							if(vals.peek().isIterable()) {
								Iterator<Val> it = IIterable.from(vals.peek()).iterator();
								while(it.hasNext()) {
									ifChainState = IfChainState.BLOCKED;
									it.next();
									evalBlock((TokenBlock)nt, vm);
								}
								continue;
							}
							else if(vals.peek().booleanValue()) {
								evalBlock((TokenBlock)nt, vm);
								ifChainState = IfChainState.BLOCKED;
								//try {Thread.sleep(500);
								//} catch (InterruptedException e) {e.printStackTrace();}
								i = -1;
								this.vals.clear();
								continue;
							}
							break;
					}
					break;
					
				//Blocks:
				case CODE_BLOCK:
					if(!s.getToken(i+1).isPreBlockKeyword())
						Console.g.err("Unsolicited block: '"+t+"'.");
					//vals.push(evalBlock((TokenBlock)t, vm).getResult());
					break;
				case OBJ_BLOCK:
					vals.push(evalContainerBlock((TokenBlock)t, vm).getResult());
					break;
				case ARY_BLOCK:
					vals.push(evalArrayBlock((TokenBlock)t, vm).getResult());
					break;
				case ACCESSOR_BLOCK:
					vals.push(evalAccessorBlock((TokenBlock)t, vm, vals.pop()).getResult());
					break;
				case ARG_BLOCK:
					vals.push(evalArgumentBlock((TokenBlock)t, vm).getResult());
					break;
				/*case IF_BLOCK:
					blockPass = false;
					if(vals.peek().booleanValue()) {
						EvalResult res = evalBlock((TokenBlock)t, vm);
						vals.push(res.getResult());
						blockPass = true;
					}
					break;*/
				/*case WHILE_BLOCK:
					//Val r = TUndefined.getInstance();
					blockPass = false;
					if(vals.peek().isIterable()) {
						Iterator<Val> it = IIterable.from(vals.peek()).iterator();
						while(it.hasNext()) {
							blockPass = true;
							it.next();
							evalBlock((TokenBlock)t, vm);
						}
						continue;
					}
					else if(vals.peek().booleanValue()) {
						evalBlock((TokenBlock)t, vm);//.getResult();
						blockPass = true;
						//try {Thread.sleep(500);
						//} catch (InterruptedException e) {e.printStackTrace();}
						i = -1;
						this.vals.clear();
						continue;
					}
					
					break;*/
			default:
				break;
				
			}
			
		}
		// With statement result:
		if(bt == BlockType.CODE && !vals.isEmpty()) {
			Val v = vals.peek();
			if(v.isDefinition()) {
				vm.setVariable((Var)v);
			}
		}
		return new EvalResult(vals.isEmpty()?TUndefined.getInstance():vals.pop(), ifChainState);
	}
	
	public static EvalResult evalBlock(TokenBlock b, VM vm) {
		return evalCodeBlock(b, vm);
	}
	
	public static EvalResult evalCodeBlock(TokenBlock b, VM vm) {
		Statement[] l = b.getBlock().getStatements();
		EvalResult r = new EvalResult(TUndefined.getInstance());
		ValStack bvals = new ValStack();
		boolean blockPass = true;
		IfChainState chain = IfChainState.BLOCKED;
		for(int i = 0; i < l.length; i++) {
			r = new StatementEval(l[i], vm, BlockType.CODE, bvals, blockPass, chain).eval();
			blockPass = r.didPass()==IfChainState.PASS;
			chain = r.didPass();
			vm.setPreviousResult(r.getResult());
		}
		return new EvalResult(r.getResult());
	}
	
	public EvalResult evalArrayBlock(TokenBlock b, VM vm) {
		Statement[] l = b.getBlock().getStatements();
		TArray a;
		ValStack bvals = new ValStack();
		if(l.length == 1) {
			Val r = new StatementEval(l[0], vm, BlockType.ARRAY, bvals, blockPass, ifChainState).eval().getResult();
			if(r.isIterable()) {
				a = new TArray(IIterable.from(r));
				return new EvalResult(a);
			}
			a = new TArray();
			a.add(r);
			return new EvalResult(a);
		}
		
		a = new TArray();
		for(int i = 0; i < l.length; i++) {
			a.add(new StatementEval(l[i], vm, BlockType.ARRAY, bvals, blockPass, ifChainState).eval().getResult());
		}
		return new EvalResult(a);
	}
	
	public EvalResult evalObjectBlock(TokenBlock b, VM vm) {
		Statement[] l = b.getBlock().getStatements();
		for(int i = 0; i < l.length; i++) {
			
		}
		return new EvalResult(TUndefined.getInstance());
	}
	
	public EvalResult evalContainerBlock(TokenBlock b, VM vm) {
		Statement[] l = b.getBlock().getStatements();
		TContainer c = new TContainer();
		ValStack bvals = new ValStack();
		for(int i = 0; i < l.length; i++) {
			c.add(new StatementEval(l[i], vm, BlockType.CONTAINER, bvals, blockPass, ifChainState).eval().getResult());
		}
		return new EvalResult(c);
	}
	
	public EvalResult evalArgumentBlock(TokenBlock b, VM vm) {
		Statement[] l = b.getBlock().getStatements();
		TContainer c = new TContainer();
		ValStack bvals = new ValStack();
		for(int i = 0; i < l.length; i++) {
			Val r = new StatementEval(l[i], vm, BlockType.CONTAINER, bvals, blockPass, ifChainState).eval().getResult();
			if(r.isDefinition())
				c.add(r);
			else if(r.isVariable())
				c.add(new VarDef(Var.from(r).getName(), TUndefined.getInstance()));
			else
				Console.g.err("Illegal function definition parameter: " + r);
		}
		return new EvalResult(c);
	}
	
	public EvalResult evalAccessorBlock(TokenBlock b, VM vm, Val toAccess) {
		Statement[] l = b.getBlock().getStatements();
		ArrayList<Val> stmtResult = new ArrayList<Val>();
		
		ValStack bvals = new ValStack();
		for(int i = 0; i < l.length; i++) {
			stmtResult.add(new StatementEval(l[i], vm, BlockType.CONTAINER, bvals, blockPass, ifChainState).eval().getResult());
		}
		if(toAccess.isFunction()) {
			ArrayList<Val> args = new ArrayList<Val>();
			boolean isNameMode = false;
			int assigned = 0;
			TFunc f = TFunc.from(toAccess);
			for(int i = 0; i < stmtResult.size(); i++) {
				Val v = stmtResult.get(i);
				if(v.isDefinition()) {
					Var d = (Var)v;
					isNameMode = true;
					boolean found = false;
					for(int j = assigned; j < f.getNamedArgs(); j++) {
						if(f.getArgName(j).equals(d.getName())) {
							found = true;
							for(int k = args.size(); k < j; k++) {
								args.add(f.getDefault(k));
							}
							args.add(v);
							break;
						}
					}
					if(!found)
						Console.g.err("Argument " + d.toString() + " is not used in function " + toAccess.toString());
					assigned++;
					continue;
				}
				else if(isNameMode) {
					Console.g.err("Late use of unnamed parameter in function call.");
					return new EvalResult(TUndefined.getInstance());
				}
				args.add(v);
				assigned++;
			}
			return f.execute(vm, TUndefined.getInstance(), new Arguments(args));
		}
		return new EvalResult(toAccess.accessor(stmtResult));
	}
}
