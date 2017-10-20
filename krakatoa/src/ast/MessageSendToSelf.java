package ast;


public class MessageSendToSelf extends MessageSend {
    
    public Type getType() { 
        return null;
    }
    
    public void genC( PW pw, boolean putParenthesis ) {
    }

	@Override
	public void genKra(PW pw) {
		// TODO Auto-generated method stub
		
	}

    
}