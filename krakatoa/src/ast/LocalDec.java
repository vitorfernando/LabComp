package ast;

import java.util.ArrayList;

public class LocalDec {

	private ArrayList<Variable> arrayVar;
	private Type type;

	public LocalDec(ArrayList<Variable> arrayVar, Type type) {
		this.arrayVar = arrayVar;
		this.type = type;
	}

	public void genkra(PW pw) {
		pw.print(this.type.getName());
		int size = this.arrayVar.size();
		for(Variable v: arrayVar)
		{
			pw.print(v.getName());
			if(--size>0)
				pw.print(", ");
		}
	}

}
