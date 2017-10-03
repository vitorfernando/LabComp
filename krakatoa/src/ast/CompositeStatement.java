package ast;

public class CompositeStatement extends Statement {

	public StatementList stmlist;

	@Override
	public void genC(PW pw) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void genKra(PW pw) {
		pw.printlnIdent("{");
		pw.add();
		this.stmlist.genKra(pw);
		pw.sub();
		pw.println("}");
	}

}
