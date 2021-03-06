package com.mrh0.qspl.type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import com.mrh0.qspl.io.console.Console;
import com.mrh0.qspl.type.iterator.IIterable;
import com.mrh0.qspl.type.iterator.IKeyIterable;
import com.mrh0.qspl.type.iterator.TRangeIterator;
import com.mrh0.qspl.type.number.TNumber;
import com.mrh0.qspl.type.var.Var;

public class TArray implements Val, IIterable, IKeyIterable{
	
	protected final ArrayList<Val> values;
	
	public TArray() {
		this.values = new ArrayList<Val>();
	}
	
	public TArray(Iterable<Val> it) {
		this.values = new ArrayList<Val>();
		for(Val v : it)
			this.add(v);
	}
	
	public TArray(ArrayList<Val> vals) {
		this.values = new ArrayList<Val>();
		for(int i = 0; i < vals.size(); i++) {
			this.values.add(vals.get(i));
		}
	}
	
	public TArray(String...strs) {
		this.values = new ArrayList<Val>();
		for(int i = 0; i < strs.length; i++) {
			this.add(new TString(strs[i]));
		}
	}
	
	@Override
	public TAtom getTypeAtom() {
		return TAtom.get("array");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(int i = 0; i < values.size(); i++) {
			sb.append(values.get(i));
			if(i+1 < values.size())
				sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public int getType() {
		return 0;
	}

	@Override
	public Val duplicate() {
		return new TArray(values);
	}

	@Override
	public boolean booleanValue() {
		return values.size() > 0;
	}

	@Override
	public String getTypeName() {
		return "array";
	}

	@Override
	public Object getValue() {
		return values;
	}
	
	public Val get(int i) {
		return values.get(i < 0?size()+i:i);
	}
	
	public int size() {
		return values.size();
	}
	
	public void set(int index, Val v) {
		values.set(index, new Var(index+"", v));
	}
	
	public void set(int index, Var v) {
		values.set(index, v);
	}
	
	@Override
	public boolean isArray() {
		return true;
	}
	
	@Override
	public Val push(Val v) {
		values.add(new Var(size()+"", v));
		return this;
	}
	
	public Val add(TArray v) {
		if(v.isArray()) {
			TArray r = new TArray(values);
			r.merge(TArray.from(v));
			return r;
		}
		return Val.super.add(v);
	}
	
	@Override
	public Val add(Val v) {
		return push(v);
	}
	
	@Override
	public Val multi(Val v) {
		if(v.isString()) {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < values.size(); i++) {
				sb.append(get(i));
				if(i+1 < values.size())
					sb.append(TString.from(v).get());
			}
			return new TString(sb.toString());
		}
		return Val.super.multi(v);
	}
	
	@Override
	public Val accessor(List<Val> args) {
		/*if(args.size() == 0) 
			return new TNumber(values.size());
		else if(args.size() == 1) {
			if(args.get(0).isNumber()) {
				return get(TNumber.from(args.get(0)).integerValue());
			}
			else if(args.get(0).isIterable()) {
				TArray a = new TArray();
				IIterable iter = IIterable.from(args.get(0));
				for(Val v : iter)
					a.add(this.get(TNumber.from(v).integerValue()));
				return a;
			}
		}
		else {
			TArray a = new TArray();
			for(int i = 0; i < args.size(); i++) {
				if(args.get(i).isNumber()) {
					a.add(get(TNumber.from(args.get(i)).integerValue()));
				}
				else if(args.get(i).isIterable()) {
					
					IIterable iter = IIterable.from(args.get(i));
					for(Val v : iter)
						a.add(this.get(TNumber.from(v).integerValue()));
				}
			}
			return a;
		}*/
		if(args.size() == 0) 
			return TNumber.create(values.size());
		else if(args.size() == 1) {
			if(args.get(0).isNumber()) {
				return get(TNumber.from(args.get(0)).integerValue());
			}
			else if(args.get(0).isIterable()) {
				TArray a = new TArray();
				IIterable iter = IIterable.from(args.get(0));
				for(Val v : iter)
					a.add(this.get(TNumber.from(v).integerValue()));
				return a;
			}
		}
		else if(args.size() == 2) {
			if(args.get(0).isNumber() && args.get(1).isNumber()) {
				TArray a = new TArray();
				int as = TNumber.from(args.get(0)).integerValue();
				int ae = TNumber.from(args.get(1)).integerValue();
				
				if(as == ae) {
					a.add(get(as));
				}
				else if(as < ae && as < 0 && ae >= 0) {
					for(int i = size()+as; i > ae-1; i--) {
						a.add(get(i));
					}
				}
				else if(as > ae && as >= 0 && ae < 0){
					for(int i = as; i < size()+ae+1; i++) {
						a.add(get(i));
					}
				}
				else if(as < ae) {
					for(int i = as; i < ae+1; i++) {
						a.add(get(i));
					}
				}
				else if(as > ae){
					for(int i = as; i > ae-1; i--) {
						a.add(get(i));
					}
				}
				return a;
			}
		}
		return TUndefined.getInstance();
	}
	
	public class TArrayIterator implements Iterator<Val> {
		private Iterator<Val> a;
		public TArrayIterator(TArray a) {
			this.a = a.values.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return a.hasNext();
		}

		@Override
		public Val next() {
			return a.next();
		}
	}

	@Override
	public Iterator<Val> iterator() {
		return new TArrayIterator(this);
	}
	
	@Override
	public Iterator<Val> keyIterator() {
		return new TRangeIterator(0, size()-1);
	}
	
	public void remove(int i) {
		values.remove(i);
	}
	
	public static TArray from(Val v) {
		if(v instanceof TArray)
			return (TArray)v;
		if(v instanceof Var && v.isArray())
			return from(((Var)v).get());
		Console.g.err("Cannot convert " + v.getTypeName() + " to array.");
		return null;
	}
	
	@Override
	public Val rotateLeft(Val v) {
		int rot = TNumber.from(v).integerValue();
		for(int i = 0; i < rot; i++)
			values.add(values.remove(0));
		return this;
	}
	
	@Override
	public Val rotateRight(Val v) {
		int rot = TNumber.from(v).integerValue();
		for(int i = 0; i < rot; i++)
			values.add(0, values.remove(values.size()-1));
		return this;
	}
	
	@Override
	public Val shiftLeft(Val v) {
		TArray a = new TArray();
		int shift = TNumber.from(v).integerValue();
		for(int i = 0; i < shift; i++)
			a.add(values.remove(0));
		return a;
	}
	
	@Override
	public Val shiftRight(Val v) {
		TArray a = new TArray();
		int shift = TNumber.from(v).integerValue();
		for(int i = 0; i < shift; i++)
			a.add(values.remove(values.size()-1));
		return a;
	}
	
	public Val is(Val v) {
		return new TNumber(TArray.class.isInstance(v));
	}
	
	public void merge(TArray a) {
		for(Val v : a) {
			add(v);
		}
	}
	
	public JSONArray toJSON() {
		JSONArray o = new JSONArray();
		for(int i = 0; i < values.size(); i++) {
			Val vt = values.get(i);
			if(vt.isContainer())
				o.put(i, TContainer.from(vt).toJSON());
			else if(vt.isArray())
				o.put(i, TArray.from(vt).toJSON());
			else if(vt.isNumber())
				o.put(i, TNumber.from(vt).get());
			else if(vt.isString())
				o.put(i, TString.from(vt).get());
			else
				o.put(i, vt.toString());
		}
		return o;
	}
	
	public static TArray fromJSON(JSONArray j) {
		TArray o = new TArray();
		for(Object i : j.toList()) {
			if(i instanceof JSONObject)
				o.push(new TContainer().fromJSON((JSONObject)i));
			else if(i instanceof JSONArray)
				o.push(new TArray().fromJSON((JSONArray)i));
			else if(i instanceof Double)
				o.push(new TNumber((double)i));
			else if(i instanceof Integer)
				o.push(new TNumber((int)i));
			else
				o.push(new TString(i.toString()));
		}
		return o;
	}
}
