package ast;

public class ConstructExpr extends Expr {

	private String className;
	private KraClass aclass;
	
	public ConstructExpr(String className,KraClass aclass) {
		this.className = className;
		this.aclass = aclass;
	}

	@Override
	public void genC(PW pw, boolean putParenthesis) {
		// TODO Auto-generated method stub

	}

	@Override
	public Type getType(){
		return this.aclass;
	}
	
	@Override
	public void genKra(PW pw) {
		// TODO Auto-generated method stub

	}

}
