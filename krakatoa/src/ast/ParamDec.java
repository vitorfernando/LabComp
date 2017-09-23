package ast;

public class ParamDec extends Variable{

	private String name;
	private Type type;
	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public ParamDec(String stringValue, Type type) {
		super(stringValue, type);
		this.name = stringValue;
		this.type = type;
	}

}
