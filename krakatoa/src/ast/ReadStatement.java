package ast;

import java.util.ArrayList;

public class ReadStatement extends Statement{

	public ArrayList<String> name;

	@Override
	public void genC(PW pw) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void genKra(PW pw) {
		int size = name.size();
		pw.print("read(");
		for(String n: name)
		{
			pw.print(n);
			if(--size>0)
				pw.print(", ");
		}
		pw.println(");");
	}

}
