package ast;

public class AssignExprLocalDec extends Statement{

	public LocalDec localDec;
	public Expr left;
	public Expr right;

	@Override
	public void genC(PW pw) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void genKra(PW pw) {
		if(this.localDec!=null)
			this.localDec.genkra(pw);
		else
		{
			this.left.genKra(pw);
			pw.print("=");
			this.right.genKra(pw);
		}
		
	}

}
