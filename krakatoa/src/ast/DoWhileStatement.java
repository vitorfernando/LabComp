package ast;

public class DoWhileStatement extends Statement {

	private StatementList comp;
	private Expr expr;

	public DoWhileStatement(StatementList comp2, Expr expr) {
		this.comp = comp2;
		this.expr = expr;
	}

	@Override
	public void genC(PW pw) {
		// TODO Auto-generated method stub

	}

	@Override
	public void genKra(PW pw) {
		// TODO Auto-generated method stub

	}

}
