package com.mrh0.qspl.type.func;

import java.util.ArrayList;

import com.mrh0.qspl.interpreter.evaluator.EvalResult;
import com.mrh0.qspl.type.TContainer;
import com.mrh0.qspl.type.Val;
import com.mrh0.qspl.vm.VM;

public class InternalFunc extends TFunc{
	
	private IFunc func;
	private TContainer defaults;
	
	public InternalFunc(IFunc func) {
		this.func = func;
		defaults = new TContainer();
	}
	
	public InternalFunc(IFunc func, TContainer defaults) {
		this.func = func;
		this.defaults = defaults;
	}

	@Override
	public EvalResult execute(VM vm, Val _this, ArrayList<Val> args) {
		return func.execute(vm, _this, args);
	}
}
