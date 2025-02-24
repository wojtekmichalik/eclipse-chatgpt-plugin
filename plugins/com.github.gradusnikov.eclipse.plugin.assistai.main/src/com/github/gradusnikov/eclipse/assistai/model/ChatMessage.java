package com.github.gradusnikov.eclipse.assistai.model;


/**
 * Represents a chat message with an ID, role, number of tokens, and the message content.
 */
public class ChatMessage {
	
	public final String id;
	public final String role;
	public final String name;
	public StringBuffer content;
    public FunctionCall functionCall;
	
	/**
	 * Constructs a ChatMessage with the given ID and role.
	 * @param id The unique identifier for the chat message
	 * @param role The role associated with the chat message (e.g., "user", "assistant")
	 */
	public ChatMessage( String id, String role )
	{
	    this( id, null, role);
	}
   public ChatMessage( String id, String name, String role )
    {
        this.id = id;
        this.role = role;
        this.name = name;
        this.content = new StringBuffer();
    }
	/**
	 * Appends the given message to the existing message.
	 * @param msg The message to be appended
	 */
	public void append( String msg )
	{
		this.content.append( msg );
	}

	/**
	 * Retrieves the message content.
	 * @return The message content
	 */
	public String getContent() {
		return content.toString();
	}
	public FunctionCall getFunctionCall()
	{
	    return functionCall;
	}
    public void setFunctionCall( FunctionCall functionCall )
    {
        this.functionCall = functionCall;
    }
	/**
	 * Sets the message content.
	 * @param message The new message content
	 */
	public void setContent(String message) {
		this.content.setLength( 0 );
		this.content.append( message );
	}

	/**
	 * Retrieves the unique identifier.
	 * @return The ID of the chat message
	 */
	public String getId() {
		return id;
	}

	/**
	 * Retrieves the role associated with the chat message.
	 * @return The role of the chat message
	 */
	public String getRole() {
		return role;
	}
    public String getName()
    {
        return name;
    }
	public void setMessage(String string) {
		content.append(string);//again
		
	}
	
	


}
