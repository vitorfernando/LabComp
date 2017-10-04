package ast;

public class ConstructExpr extends Expr {

	private String className;

	public ConstructExpr(String className) {
		this.className = className;
	}

	@Override
	public void genC(PW pw, boolean putParenthesis) {
		// TODO Auto-generated method stub

	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void genKra(PW pw) {
		// TODO Auto-generated method stub

	}

}
