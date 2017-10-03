package ast;

import java.util.ArrayList;

public class StatementList {

	private ArrayList<Statement> stmtlist;

	public ArrayList<Statement> getStmtlist() {
		return stmtlist;
	}

	public void setStmtlist(ArrayList<Statement> stmtlist) {
		this.stmtlist = stmtlist;
	}

	public StatementList(ArrayList<Statement> stmtList) {
		this.stmtlist = stmtList;
	}

	public void genKra(PW pw) {
		for(Statement s: stmtlist)
		{
			s.genKra(pw);			
		}		
	}

}
