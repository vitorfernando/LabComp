package ast;

public class IfStatement extends Statement {

	public Expr expr;
	public Statement stmt;
	public Statement elseStmt;

	@Override
	public void genC(PW pw) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void genKra(PW pw) {
		pw.printIdent("if ( ");
		this.expr.genKra(pw);
		pw.printlnIdent(")");
		pw.add();
		this.stmt.genKra(pw);
		pw.sub();
		if(this.elseStmt!=null)
		{
			pw.printlnIdent(" else ");
			pw.add();
			this.elseStmt.genKra(pw);
			pw.sub();
		}		
	}

}
