package ast;

import java.util.ArrayList;

/*
 * Krakatoa Class
 */
public class KraClass extends Type {
	
   public KraClass( String name ) {
      super(name);
   }
   
   public KraClass(String className, String superclassName, ArrayList<MethodDec> methodList,
		ArrayList<InstanceVarDec> varList) {
	// TODO Auto-generated constructor stub
}

public String getCname() {
      return getName();
   }
   
   private String name;
   private KraClass superclass;
   private ArrayList<InstanceVarDec> instanceVariableList;
   private ArrayList<MethodDec> publicMethodList, privateMethodList;
   // métodos públicos get e set para obter e iniciar as variáveis acima,
   // entre outros métodos
}
