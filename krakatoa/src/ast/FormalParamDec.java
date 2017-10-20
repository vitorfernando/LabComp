package ast;

import java.util.ArrayList;

public class FormalParamDec {

	private ArrayList<ParamDec> params;

	public FormalParamDec(ArrayList<ParamDec> params) {
		this.params = params;
	}
	
	public ArrayList<ParamDec> getParams() {
		return params;
	}

	public void genKra(PW pw) 
	{
		int size = params.size();
		for (ParamDec p: params)
		{
			pw.print(p.getType().getName() + " " + p.getName());
			if(--size>0)
				pw.print(", ");
		}
	}
}
