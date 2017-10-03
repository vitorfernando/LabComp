package ast;

public class ReturnStatement extends Statement{

	public Expr expr;

	@Override
	public void genC(PW pw) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void genKra(PW pw) {
		pw.printIdent("return ");
		this.expr.genKra(pw);
		pw.println(";");		
	}

}
