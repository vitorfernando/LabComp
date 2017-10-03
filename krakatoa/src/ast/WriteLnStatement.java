package ast;

public class WriteLnStatement extends Statement {

	public ExprList exprlist;

	@Override
	public void genC(PW pw) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void genKra(PW pw) {
		pw.print("write(");
		this.exprlist.genKra(pw);
		pw.println(");");
		
	}

}
