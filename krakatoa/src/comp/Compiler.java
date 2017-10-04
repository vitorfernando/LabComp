
package comp;

import ast.*;
import lexer.*;
import java.io.*;
import java.util.*;

public class Compiler {

	private MethodDec currentMethod;
	private KraClass currentClass;

	// compile must receive an input with an character less than
	// p_input.lenght
	public Program compile(char[] input, PrintWriter outError) {

		ArrayList<CompilationError> compilationErrorList = new ArrayList<>();
		signalError = new ErrorSignaller(outError, compilationErrorList);
		symbolTable = new SymbolTable();
		lexer = new Lexer(input, signalError);
		signalError.setLexer(lexer);

		Program program = null;
		lexer.nextToken();
		program = program(compilationErrorList);
		return program;
	}

	private Program program(ArrayList<CompilationError> compilationErrorList) {
		// Program ::= KraClass { KraClass }
		ArrayList<MetaobjectCall> metaobjectCallList = new ArrayList<>();
		ArrayList<KraClass> kraClassList = new ArrayList<>();
		Program program = new Program(kraClassList, metaobjectCallList, compilationErrorList);
		try {
			while (lexer.token == Symbol.MOCall) {
				metaobjectCallList.add(metaobjectCall());
			}
			kraClassList.add(classDec());
			while (lexer.token == Symbol.CLASS)
				kraClassList.add(classDec());
			boolean progFlag = false;
			for(KraClass k: kraClassList)
			{
				if(k.getName().equals("Program"))
				{
					progFlag = true;
					break;
				}
			}
			if(!progFlag)
				signalError.showError("Source code without a class 'Program'");
			if (lexer.token != Symbol.EOF) {
				signalError.showError("End of file expected");
			}
		} catch (RuntimeException e) {
			// if there was an exception, there is a compilation signalError
		}
		return program;
	}

	/**
	 * parses a metaobject call as <code>{@literal @}ce(...)</code> in <br>
	 * <code>
	 * &#64;ce(5, "'class' expected") <br>
	 * clas Program <br>
	 *     public void run() { } <br>
	 * end <br>
	 * </code>
	 * 
	 * 
	 */
	@SuppressWarnings("incomplete-switch")
	private MetaobjectCall metaobjectCall() {
		String name = lexer.getMetaobjectName();
		lexer.nextToken();
		ArrayList<Object> metaobjectParamList = new ArrayList<>();
		if (lexer.token == Symbol.LEFTPAR) {
			// metaobject call with parameters
			lexer.nextToken();
			while (lexer.token == Symbol.LITERALINT || lexer.token == Symbol.LITERALSTRING
					|| lexer.token == Symbol.IDENT) {
				switch (lexer.token) {
				case LITERALINT:
					metaobjectParamList.add(lexer.getNumberValue());
					break;
				case LITERALSTRING:
					metaobjectParamList.add(lexer.getLiteralStringValue());
					break;
				case IDENT:
					metaobjectParamList.add(lexer.getStringValue());
				}
				lexer.nextToken();
				if (lexer.token == Symbol.COMMA)
					lexer.nextToken();
				else
					break;
			}
			if (lexer.token != Symbol.RIGHTPAR)
				signalError.showError("')' expected after metaobject call with parameters");
			else
				lexer.nextToken();
		}
		if (name.equals("nce")) {
			if (metaobjectParamList.size() != 0)
				signalError.showError("Metaobject 'nce' does not take parameters");
		} else if (name.equals("ce")) {
			if (metaobjectParamList.size() != 3 && metaobjectParamList.size() != 4)
				signalError.showError("Metaobject 'ce' take three or four parameters");
			if (!(metaobjectParamList.get(0) instanceof Integer))
				signalError.showError("The first parameter of metaobject 'ce' should be an integer number");
			if (!(metaobjectParamList.get(1) instanceof String) || !(metaobjectParamList.get(2) instanceof String))
				signalError.showError("The second and third parameters of metaobject 'ce' should be literal strings");
			if (metaobjectParamList.size() >= 4 && !(metaobjectParamList.get(3) instanceof String))
				signalError.showError("The fourth parameter of metaobject 'ce' should be a literal string");

		}

		return new MetaobjectCall(name, metaobjectParamList);
	}

	private KraClass classDec() {
		// Note que os métodos desta classe não correspondem exatamente às
		// regras
		// da gramática. Este método classDec, por exemplo, implementa
		// a produção KraClass (veja abaixo) e partes de outras produções.

		/*
		 * KraClass ::= ``class'' Id [ ``extends'' Id ] "{" MemberList "}" MemberList
		 * ::= { Qualifier Member } Member ::= InstVarDec | MethodDec InstVarDec ::=
		 * Type IdList ";" MethodDec ::= Qualifier Type Id "("[ FormalParamDec ] ")" "{"
		 * StatementList "}" Qualifier ::= [ "static" ] ( "private" | "public" )
		 */
		String superclassName = null;
		ArrayList<MethodDec> methodList = new ArrayList<>();
		ArrayList<InstanceVarDec> varList = new ArrayList<>();
		if (lexer.token != Symbol.CLASS)
			signalError.showError("'class' expected");
		lexer.nextToken();
		if (lexer.token != Symbol.IDENT)
			signalError.show(ErrorSignaller.ident_expected);
		String className = lexer.getStringValue();
		if (symbolTable.getInGlobal(className) != null)
			signalError.showError("class alreaby been delcared");
		this.currentClass = new KraClass(className);
		symbolTable.putInGlobal(className, this.currentClass);
		lexer.nextToken();
		if (lexer.token == Symbol.EXTENDS) {
			lexer.nextToken();
			if (lexer.token != Symbol.IDENT)
				signalError.show(ErrorSignaller.ident_expected);
			superclassName = lexer.getStringValue();
			if (symbolTable.getInGlobal(superclassName) == null)
				signalError.showError("superclass have not been declared");
			if(className.compareToIgnoreCase(superclassName)==0)
					signalError.showError("class" + className + "is inheriting from itself");
			this.currentClass.setSuperclass(new KraClass(superclassName));
			lexer.nextToken();
		}
		if (lexer.token != Symbol.LEFTCURBRACKET)
			signalError.showError("{ expected", true);
		lexer.nextToken();

		while (lexer.token == Symbol.PRIVATE || lexer.token == Symbol.PUBLIC) {

			Symbol qualifier;
			switch (lexer.token) {
			case PRIVATE:
				lexer.nextToken();
				qualifier = Symbol.PRIVATE;
				break;
			case PUBLIC:
				lexer.nextToken();
				qualifier = Symbol.PUBLIC;
				break;
			default:
				signalError.showError("private, or public expected");
				qualifier = Symbol.PUBLIC;
			}
			Type t = type();

			if (lexer.token != Symbol.IDENT)
				signalError.showError("Identifier expected");
			String name = lexer.getStringValue();
			lexer.nextToken();
			if (lexer.token == Symbol.LEFTPAR) {
				if(className.equals("Program") && qualifier.toString().equals("private") && name.equals("run"))
					signalError.showError("Method 'run' of class 'Program' cannot be private");
				methodDec(t, name, qualifier);
			}
			else if (qualifier != Symbol.PRIVATE)
				signalError.showError("Attempt to declare a public instance variable");
			else
				instanceVarDec(t, name);
		}
		if (lexer.token != Symbol.RIGHTCURBRACKET)
			signalError.showError("public/private or \"}\" expected");
		lexer.nextToken();
		if(this.currentClass.getName().equals("Program"))
			{
			boolean runFlag = false;
			for (MethodDec m: this.currentClass.getPublicMethodList())
			{
				if(m.getName().equals("run"))
				{
					runFlag = true;
					break;
				}
			}
			if(!runFlag)
				signalError.showError("ethod 'run' was not found in class 'Program'");
			}
		return this.currentClass;

	}

	private void instanceVarDec(Type type, String name) {
		// InstVarDec ::= [ "static" ] "private" Type IdList ";"

		this.currentClass.addInstanceVariable(new InstanceVariable(name, type));
		do {
			if (lexer.token != Symbol.IDENT)
				signalError.showError("Identifier expected");
			InstanceVariable var = new InstanceVariable(lexer.getStringValue(), type);
			if (symbolTable.getInLocal(var.getName()) != null)
				signalError.showError("variable has already been declared");
			symbolTable.putInLocal(var.getName(), var);
			this.currentClass.addInstanceVariable(var);
			lexer.nextToken();
		}while (lexer.token == Symbol.COMMA);
		if (lexer.token != Symbol.SEMICOLON)
			signalError.show(ErrorSignaller.semicolon_expected);
		lexer.nextToken();
	}

	private void methodDec(Type type, String name, Symbol qualifier) {
		/*
		 * MethodDec ::= Qualifier Return Id "("[ FormalParamDec ] ")" "{" StatementList
		 * "}"
		 */
		this.currentMethod = new MethodDec(type, name, qualifier);
		if (symbolTable.getInLocal(this.currentMethod.getName()) != null)
			signalError.showError("method has already been declared");
		lexer.nextToken();
		if (lexer.token != Symbol.RIGHTPAR)
			this.currentMethod.param = formalParamDec();
		if (lexer.token != Symbol.RIGHTPAR)
			signalError.showError(") expected");
		if(this.currentClass.getName().equals("Program")&& this.currentMethod.getName().equals("run")
				&& this.currentMethod.param != null)
			signalError.showError("method run can not take parameters");
		lexer.nextToken();
		if (lexer.token != Symbol.LEFTCURBRACKET)
			signalError.showError("{ expected");

		lexer.nextToken();
		this.currentMethod.stmtList = statementList();
		if (lexer.token != Symbol.RIGHTCURBRACKET)
			signalError.showError("} expected");
		if(this.currentMethod.getReturnType().getName().compareToIgnoreCase("void")!=0)
		{
			boolean retflag = false;
			for(Statement m: this.currentMethod.stmtList.getStmtlist())
			{
				if(m.getClass()==ReturnStatement.class)
				{
					retflag = true;
					break;
				}
			}		
			if(!retflag)
				signalError.showError("a non void method must have a return statement");
		}

		lexer.nextToken();
		this.currentClass.addMethod(this.currentMethod);
		this.currentMethod = null;
	}

	private LocalDec localDec() {
		// LocalDec ::= Type IdList ";"

		ArrayList<Variable> arrayVar = new ArrayList<>();
		Type type = type();
		if (lexer.token != Symbol.IDENT)
			signalError.showError("Identifier expected");
		Variable v = new Variable(lexer.getStringValue(), type);
		if (symbolTable.getInLocal(v.getName()) != null)
			signalError.showError("variable has already been declared");
		arrayVar.add(v);
		this.symbolTable.putInLocal(v.getName(), v);
		lexer.nextToken();
		while (lexer.token == Symbol.COMMA) {
			lexer.nextToken();
			if (lexer.token != Symbol.IDENT)
				signalError.showError("Identifier expected");
			v = new Variable(lexer.getStringValue(), type);
			if (symbolTable.getInLocal(v.getName()) != null)
				signalError.showError("variable has already been declared");
			this.symbolTable.putInLocal(v.getName(), v);
			arrayVar.add(v);
			lexer.nextToken();
		}
		return new LocalDec(arrayVar, type);
	}

	private FormalParamDec formalParamDec() {
		// FormalParamDec ::= ParamDec { "," ParamDec }
		ArrayList<ParamDec> params = new ArrayList<>();
		params.add(paramDec());
		while (lexer.token == Symbol.COMMA) {
			lexer.nextToken();
			params.add(paramDec());
		}
		return new FormalParamDec(params);
	}

	private ParamDec paramDec() {
		// ParamDec ::= Type Id

		Type type = type();
		if (lexer.token != Symbol.IDENT)
			signalError.showError("Identifier expected");
		ParamDec p = new ParamDec(lexer.getStringValue(), type);
		this.symbolTable.putInLocal(p.getName(), p);
		lexer.nextToken();
		return p;
	}

	private Type type() {
		// Type ::= BasicType | Id
		Type result;

		switch (lexer.token) {
		case VOID:
			result = Type.voidType;
			break;
		case INT:
			result = Type.intType;
			break;
		case BOOLEAN:
			result = Type.booleanType;
			break;
		case STRING:
			result = Type.stringType;
			break;
		case IDENT:
			// # corrija: faça uma busca na TS para buscar a classe
			// IDENT deve ser uma classe.
			KraClass classident = this.symbolTable.getInGlobal(lexer.getStringValue());
			if(classident == null)
				signalError.showError("This class does not exists");
			else
				result = classident;
		default:
			signalError.showError("Type expected");
			result = Type.undefinedType;
		}
		lexer.nextToken();
		return result;
	}

	private CompositeStatement compositeStatement() {

		lexer.nextToken();
		CompositeStatement comp = new CompositeStatement();
			comp.stmlist = statementList();
		if (lexer.token != Symbol.RIGHTCURBRACKET)
			signalError.showError("} expected");
		else
			lexer.nextToken();
		return null;
	}

	private StatementList statementList() {
		// CompStatement ::= "{" { Statement } "}"
		Symbol tk;
		ArrayList<Statement> stmtList = new ArrayList<>();
		// statements always begin with an identifier, if, read, write, ...
		while ((tk = lexer.token) != Symbol.RIGHTCURBRACKET && tk != Symbol.ELSE)
			stmtList.add(statement());
		return new StatementList(stmtList);
	}

	private Statement statement() {
		/*
		 * Statement ::= Assignment ``;'' | IfStat |WhileStat | MessageSend ``;'' |
		 * ReturnStat ``;'' | ReadStat ``;'' | WriteStat ``;'' | ``break'' ``;'' | ``;''
		 * | CompStatement | LocalDec
		 */

		switch (lexer.token) {
		case THIS:
		case IDENT:
		case SUPER:
		case INT:
		case BOOLEAN:
		case STRING:
			return assignExprLocalDec();
		case ASSERT:
			return assertStatement();
		case RETURN:
			return returnStatement();
		case READ:
			return readStatement();
		case WRITE:
			return writeStatement();
		case WRITELN:
			return writelnStatement();
		case IF:
			return ifStatement();
		case BREAK:
			return breakStatement();
		case WHILE:
			return whileStatement();
		case SEMICOLON:
			return nullStatement();
		case LEFTCURBRACKET:
			return compositeStatement();
		default:
			signalError.showError("Statement expected");
		}
		return null;
	}

	private Statement assertStatement() {
		lexer.nextToken();
		int lineNumber = lexer.getLineNumber();
		Expr e = expr();
		if (e.getType() != Type.booleanType)
			signalError.showError("boolean expression expected");
		if (lexer.token != Symbol.COMMA) {
			this.signalError.showError("',' expected after the expression of the 'assert' statement");
		}
		lexer.nextToken();
		if (lexer.token != Symbol.LITERALSTRING) {
			this.signalError.showError("A literal string expected after the ',' of the 'assert' statement");
		}
		String message = lexer.getLiteralStringValue();
		lexer.nextToken();
		if (lexer.token == Symbol.SEMICOLON)
			lexer.nextToken();
		else
			signalError.showError("; expected"); 

		return new StatementAssert(e, lineNumber, message);
	}

	/*
	 * retorne true se 'name' é uma classe declarada anteriormente. É necessário
	 * fazer uma busca na tabela de símbolos para isto.
	 */
	private boolean isType(String name) {
		return this.symbolTable.getInGlobal(name) != null;
	}

	/*
	 * AssignExprLocalDec ::= Expression [ ``$=$'' Expression ] | LocalDec
	 */
	private AssignExprLocalDec assignExprLocalDec() {

		AssignExprLocalDec a = new AssignExprLocalDec();
		Expr left = null, right = null;
		if (lexer.token == Symbol.INT || lexer.token == Symbol.BOOLEAN || lexer.token == Symbol.STRING ||
		// token é uma classe declarada textualmente antes desta
		// instrução
				(lexer.token == Symbol.IDENT && isType(lexer.getStringValue()))) {
			/*
			 * uma declaração de variável. 'lexer.token' é o tipo da variável
			 * 
			 * AssignExprLocalDec ::= Expression [ ``$=$'' Expression ] | LocalDec LocalDec
			 * ::= Type IdList ``;''
			 */
			a.localDec = localDec();
		} else {
			/*
			 * AssignExprLocalDec ::= Expression [ ``$=$'' Expression ]
			 */
			left = expr();
			if (lexer.token == Symbol.ASSIGN) {
				lexer.nextToken();
				right = expr();
				if (left.getType() != right.getType())
					signalError.showError("Expressions of diferents types");

				if (lexer.token != Symbol.SEMICOLON)
					signalError.showError("';' expected", true);
				else
					lexer.nextToken();
			}
			else
				this.signalError.showError("assingment symbol (=) expected");
		}
		a.left = left;
		a.right = right;
		return a;
	}

	private ExprList realParameters() {
		ExprList anExprList = null;

		if (lexer.token != Symbol.LEFTPAR)
			signalError.showError("( expected");
		lexer.nextToken();
		if (startExpr(lexer.token))
			anExprList = exprList();
		if (lexer.token != Symbol.RIGHTPAR)
			signalError.showError(") expected");
		lexer.nextToken();
		return anExprList;
	}

	private WhileStatement whileStatement() {

		lexer.nextToken();

		if (lexer.token != Symbol.LEFTPAR)
			signalError.showError("( expected");
		lexer.nextToken();
		Expr expr = expr();
		if (expr.getType() != Type.booleanType)
			signalError.showError("boolean expression expected");
		if (lexer.token != Symbol.RIGHTPAR)
			signalError.showError(") expected");
		lexer.nextToken();
		Statement stmt = statement();
		return new WhileStatement(expr, stmt);
	}

	private IfStatement ifStatement() {

		IfStatement ifStmt = new IfStatement();
		lexer.nextToken();
		if (lexer.token != Symbol.LEFTPAR)
			signalError.showError("( expected");
		lexer.nextToken();
		ifStmt.expr = expr();
		if (ifStmt.expr.getType() != Type.booleanType)
			signalError.showError("boolean expression expected");
		if (lexer.token != Symbol.RIGHTPAR)
			signalError.showError(") expected");
		lexer.nextToken();
		ifStmt.stmt = statement();
		if (lexer.token == Symbol.ELSE) {
			lexer.nextToken();
			ifStmt.elseStmt = statement();
		}
		return ifStmt;
	}

	private ReturnStatement returnStatement() {

		ReturnStatement retStmt = new ReturnStatement();
		lexer.nextToken();
		retStmt.expr = expr();
		if (this.currentMethod.getReturnType() == Type.voidType)
			signalError.showError("This method cannot return a value ");
		if (!retStmt.expr.getType().isCompatible(this.currentMethod.getReturnType()))
			;
		signalError.showError("Return expression type is not compatible with the method type");
		if (lexer.token != Symbol.SEMICOLON)
			signalError.show(ErrorSignaller.semicolon_expected);
		lexer.nextToken();
		return retStmt;
	}

	private ReadStatement readStatement() {
		ReadStatement readStmt = new ReadStatement();
		lexer.nextToken();
		if (lexer.token != Symbol.LEFTPAR)
			signalError.showError("( expected");
		lexer.nextToken();
		while (true) {
			if (lexer.token == Symbol.THIS) {
				lexer.nextToken();
				if (lexer.token != Symbol.DOT)
					signalError.showError(". expected");
				lexer.nextToken();
			}
			if (lexer.token != Symbol.IDENT)
				signalError.show(ErrorSignaller.ident_expected);

			readStmt.name.add(lexer.getStringValue());
			lexer.nextToken();
			if (lexer.token == Symbol.COMMA)
				lexer.nextToken();
			else
				break;
		}

		if (lexer.token != Symbol.RIGHTPAR)
			signalError.showError(") expected");
		lexer.nextToken();
		if (lexer.token != Symbol.SEMICOLON)
			signalError.show(ErrorSignaller.semicolon_expected);
		lexer.nextToken();
		return readStmt;
	}

	private WriteStatement writeStatement() {
		
		WriteStatement write = new WriteStatement();
		lexer.nextToken();
		if (lexer.token != Symbol.LEFTPAR)
			signalError.showError("( expected");
		lexer.nextToken();
		write.exprlist = exprList();
		if (lexer.token != Symbol.RIGHTPAR)
			signalError.showError(") expected");
		lexer.nextToken();
		if (lexer.token != Symbol.SEMICOLON)
			signalError.show(ErrorSignaller.semicolon_expected);
		lexer.nextToken();
		return write;
	}

	private WriteLnStatement writelnStatement() {

		WriteLnStatement writeln = new WriteLnStatement();
		lexer.nextToken();
		if (lexer.token != Symbol.LEFTPAR)
			signalError.showError("( expected");
		lexer.nextToken();
		writeln.exprlist = exprList();
		if (lexer.token != Symbol.RIGHTPAR)
			signalError.showError(") expected");
		lexer.nextToken();
		if (lexer.token != Symbol.SEMICOLON)
			signalError.show(ErrorSignaller.semicolon_expected);
		lexer.nextToken();
		return writeln;
	}

	private BreakStatement breakStatement() {
		BreakStatement breakstmt = new BreakStatement();
		lexer.nextToken();
		if (lexer.token != Symbol.SEMICOLON)
			signalError.show(ErrorSignaller.semicolon_expected);
		lexer.nextToken();
		return breakstmt;
	}

	private Statement nullStatement() {

		lexer.nextToken();
		return new NullStatement();
	}

	private ExprList exprList() {
		// ExpressionList ::= Expression { "," Expression }

		ExprList anExprList = new ExprList();
		anExprList.addElement(expr());
		while (lexer.token == Symbol.COMMA) {
			lexer.nextToken();
			anExprList.addElement(expr());
		}
		return anExprList;
	}

	private Expr expr() {

		Expr left = simpleExpr();
		Symbol op = lexer.token;
		if (op == Symbol.EQ || op == Symbol.NEQ || op == Symbol.LE || op == Symbol.LT || op == Symbol.GE
				|| op == Symbol.GT) {
			lexer.nextToken();
			Expr right = simpleExpr();
			left = new CompositeExpr(left, op, right);
		}
		return left;
	}

	private Expr simpleExpr() {
		Symbol op;

		Expr left = term();
		while ((op = lexer.token) == Symbol.MINUS || op == Symbol.PLUS || op == Symbol.OR) {
			lexer.nextToken();
			Expr right = term();
			left = new CompositeExpr(left, op, right);
		}
		return left;
	}

	private Expr term() {
		Symbol op;

		Expr left = signalFactor();
		while ((op = lexer.token) == Symbol.DIV || op == Symbol.MULT || op == Symbol.AND) {
			lexer.nextToken();
			Expr right = signalFactor();
			left = new CompositeExpr(left, op, right);
		}
		return left;
	}

	private Expr signalFactor() {
		Symbol op;
		if ((op = lexer.token) == Symbol.PLUS || op == Symbol.MINUS) {
			lexer.nextToken();
			return new SignalExpr(op, factor());
		} else
			return factor();
	}

	/*
	 * Factor ::= BasicValue | "(" Expression ")" | "!" Factor | "null" |
	 * ObjectCreation | PrimaryExpr
	 * 
	 * BasicValue ::= IntValue | BooleanValue | StringValue BooleanValue ::= "true"
	 * | "false" ObjectCreation ::= "new" Id "(" ")" PrimaryExpr ::= "super" "." Id
	 * "(" [ ExpressionList ] ")" | Id | Id "." Id | Id "." Id "(" [ ExpressionList
	 * ] ")" | Id "." Id "." Id "(" [ ExpressionList ] ")" | "this" | "this" "." Id
	 * | "this" "." Id "(" [ ExpressionList ] ")" | "this" "." Id "." Id "(" [
	 * ExpressionList ] ")"
	 */
	private Expr factor() {

		Expr anExpr;
		ExprList exprList;
		String messageName, id;

		switch (lexer.token) {
		// IntValue
		case LITERALINT:
			return literalInt();
		// BooleanValue
		case FALSE:
			lexer.nextToken();
			return LiteralBoolean.False;
		// BooleanValue
		case TRUE:
			lexer.nextToken();
			return LiteralBoolean.True;
		// StringValue
		case LITERALSTRING:
			String literalString = lexer.getLiteralStringValue();
			lexer.nextToken();
			return new LiteralString(literalString);
		// "(" Expression ")" |
		case LEFTPAR:
			lexer.nextToken();
			anExpr = expr();
			if (lexer.token != Symbol.RIGHTPAR)
				signalError.showError(") expected");
			lexer.nextToken();
			return new ParenthesisExpr(anExpr);

		// "null"
		case NULL:
			lexer.nextToken();
			return new NullExpr();
		// "!" Factor
		case NOT:
			lexer.nextToken();
			anExpr = expr();
			return new UnaryExpr(anExpr, Symbol.NOT);
		// ObjectCreation ::= "new" Id "(" ")"
		case NEW:
			lexer.nextToken();
			if (lexer.token != Symbol.IDENT)
				signalError.showError("Identifier expected");

			String className = lexer.getStringValue();
			KraClass aclass = this.symbolTable.getInGlobal(className);
			if (aclass == null)
				signalError.showError("class " + className + "does not exists");
			/*
			 * // encontre a classe className in symbol table KraClass aClass =
			 * symbolTable.getInGlobal(className); if ( aClass == null ) ...
			 */

			lexer.nextToken();
			if (lexer.token != Symbol.LEFTPAR)
				signalError.showError("( expected");
			lexer.nextToken();
			if (lexer.token != Symbol.RIGHTPAR)
				signalError.showError(") expected");
			lexer.nextToken();
			/*
			 * return an object representing the creation of an object
			 */
			return null;
		/*
		 * PrimaryExpr ::= "super" "." Id "(" [ ExpressionList ] ")" | Id | Id "." Id |
		 * Id "." Id "(" [ ExpressionList ] ")" | Id "." Id "." Id "(" [ ExpressionList
		 * ] ")" | "this" | "this" "." Id | "this" "." Id "(" [ ExpressionList ] ")" |
		 * "this" "." Id "." Id "(" [ ExpressionList ] ")"
		 */
		case SUPER:
			// "super" "." Id "(" [ ExpressionList ] ")"
			lexer.nextToken();
			if (lexer.token != Symbol.DOT) {
				signalError.showError("'.' expected");
			} else
				lexer.nextToken();
			if (lexer.token != Symbol.IDENT)
				signalError.showError("Identifier expected");
			messageName = lexer.getStringValue();
			/*
			 * para fazer as conferências semânticas, procure por 'messageName' na
			 * superclasse/superclasse da superclasse etc
			 */
			lexer.nextToken();
			exprList = realParameters();
			break;
		case IDENT:
			/*
			 * PrimaryExpr ::= Id | Id "." Id | Id "." Id "(" [ ExpressionList ] ")" | Id
			 * "." Id "." Id "(" [ ExpressionList ] ")" |
			 */

			String firstId = lexer.getStringValue();
			lexer.nextToken();
			if (lexer.token != Symbol.DOT) {
				// Id
				// retorne um objeto da ASA que representa um identificador
				Variable avar = this.symbolTable.getInLocal(firstId);
				if (avar == null)
					signalError.showError("class " + firstId + "does not exists");
				return null;
			} else { // Id "."
				lexer.nextToken(); // coma o "."
				if (lexer.token != Symbol.IDENT) {
					signalError.showError("Identifier expected");
				} else {
					// Id "." Id
					lexer.nextToken();
					id = lexer.getStringValue();
					if (lexer.token == Symbol.DOT) {
						// Id "." Id "." Id "(" [ ExpressionList ] ")"
						/*
						 * se o compilador permite variáveis estáticas, é possível ter esta opção, como
						 * Clock.currentDay.setDay(12); Contudo, se variáveis estáticas não estiver nas
						 * especificações, sinalize um erro neste ponto.
						 */
						lexer.nextToken();
						if (lexer.token != Symbol.IDENT)
							signalError.showError("Identifier expected");
						messageName = lexer.getStringValue();
						lexer.nextToken();
						exprList = this.realParameters();

					} else if (lexer.token == Symbol.LEFTPAR) {
						// Id "." Id "(" [ ExpressionList ] ")"
						Variable avar = this.symbolTable.getInLocal(firstId);
						if (avar == null)
							signalError.showError("Identifier " + firstId + "was not declared");
						Type typeVar = avar.getType();
						if (!(typeVar instanceof KraClass))
							signalError.showError("Attempt to call a method on a variable of a basic type");
						exprList = this.realParameters();
						KraClass classVar = (KraClass) typeVar;
						MethodDec amethod = classVar.searchPublicMethod(id);
						if (amethod == null)
							signalError.showError("Method " + id + "is not a public method of" + classVar.getCname()
									+ "which is the type of" + firstId);
						/*
						 * para fazer as conferências semânticas, procure por método 'ident' na classe
						 * de 'firstId'
						 */
					} else {
						// retorne o objeto da ASA que representa Id "." Id
					}
				}
			}
			break;
		case THIS:
			/*
			 * Este 'case THIS:' trata os seguintes casos: PrimaryExpr ::= "this" | "this"
			 * "." Id | "this" "." Id "(" [ ExpressionList ] ")" | "this" "." Id "." Id "("
			 * [ ExpressionList ] ")"
			 */
			lexer.nextToken();
			if (lexer.token != Symbol.DOT) {
				// only 'this'
				// retorne um objeto da ASA que representa 'this'
				// confira se não estamos em um método estático
				return null;
			} else {
				lexer.nextToken();
				if (lexer.token != Symbol.IDENT)
					signalError.showError("Identifier expected");
				id = lexer.getStringValue();
				lexer.nextToken();
				// já analisou "this" "." Id
				if (lexer.token == Symbol.LEFTPAR) {
					// "this" "." Id "(" [ ExpressionList ] ")"
					/*
					 * Confira se a classe corrente possui um método cujo nome é 'ident' e que pode
					 * tomar os parâmetros de ExpressionList
					 */
					exprList = this.realParameters();
				} else if (lexer.token == Symbol.DOT) {
					// "this" "." Id "." Id "(" [ ExpressionList ] ")"
					lexer.nextToken();
					if (lexer.token != Symbol.IDENT)
						signalError.showError("Identifier expected");
					lexer.nextToken();
					exprList = this.realParameters();
				} else {
					// retorne o objeto da ASA que representa "this" "." Id
					/*
					 * confira se a classe corrente realmente possui uma variável de instância
					 * 'ident'
					 */
					return null;
				}
			}
			break;
		default:
			signalError.showError("Expression expected");
		}
		return null;
	}

	private LiteralInt literalInt() {

		LiteralInt e = null;

		// the number value is stored in lexer.getToken().value as an object of
		// Integer.
		// Method intValue returns that value as an value of type int.
		int value = lexer.getNumberValue();
		lexer.nextToken();
		return new LiteralInt(value);
	}

	private static boolean startExpr(Symbol token) {

		return token == Symbol.FALSE || token == Symbol.TRUE || token == Symbol.NOT || token == Symbol.THIS
				|| token == Symbol.LITERALINT || token == Symbol.SUPER || token == Symbol.LEFTPAR
				|| token == Symbol.NULL || token == Symbol.IDENT || token == Symbol.LITERALSTRING;

	}

	private SymbolTable symbolTable;
	private Lexer lexer;
	private ErrorSignaller signalError;

}
