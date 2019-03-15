package com.bigjson.parser;

import java.util.Stack;

// TODO: add possibility to check if names in an object are unique (although 
// ECMA-404 says they are not required to be unique, some people might want them to be)
public class JSONStateMachine {
	
	private static final String KEYWORD_TRUE = "true";
	private static final String KEYWORD_FALSE = "false";
	private static final String KEYWORD_NULL = "null";
	private static final boolean DEBUG = false;
	
	// TODO: should I use Deque instead?
	private Stack<State> stateStack; 
	private State state = null;
	private NumberCheckingStateMachine numberStateMachine;
	private StringReadingStateMachine stringReadingStateMachine;
	private boolean doneWithRoot = false;
	
	private int curKeywordLen = 0;
	/**
	 * we need this if the root is a number: number does not have closing symbol
	 * or predefined length, so we know it is finished only when ws, comma,
	 * bracket or eof is pushed. If the root is not a real root in the file, we
	 * allow to have, say, a comma after a number
	 * 
	 */
	private boolean expectNonSpaceBytesJustAfterRoot = false;
	private State rootState;

	public enum StateType {
		OBJECT, OBJECT_ELEMENT, NAME, ARRAY, ARRAY_ELEMENT, VALUE, NUMBER, STRING, NULL, TRUE, FALSE;
	}
	
	public JSONStateMachine(){
		stateStack = new Stack<State>(); 
		numberStateMachine = new NumberCheckingStateMachine();
		stringReadingStateMachine = new StringReadingStateMachine(StringReadingStateMachine.MODE_CHECK_ASCII);
	}
	// I do not want to allow to start from the middle of the node 
	// as it will make impossible to go outside the most inner node
//	public void reset(int currentState){
//		reset();
//		enterNewState(currentState);
//	}
	/**
	 * Reset state machine to start validate a new node.
	 * 
	 * @param expectNonSpaceBytesJustAfterRoot
	 *            should be false if going to validate a root, true otherwise
	 */
	public void reset(boolean expectNonSpaceBytesJustAfterRoot){
		if(stateStack.size() > 0){
			stateStack = new Stack<State>();
		}
		state = null;
		doneWithRoot = false;
		numberStateMachine.reset();
		stringReadingStateMachine.reset();
		this.expectNonSpaceBytesJustAfterRoot = expectNonSpaceBytesJustAfterRoot;
	}
	
	public void pushEndOfInput() throws IllegalFormatException{
		if(!doneWithRoot()){
			if(state == null && stateStack.isEmpty()){
				throw new IllegalFormatException("Looks like an input is empty");
			}
			// can be only one state left - NUMBER (because number has no special closing symbol)
			if(!getCurrentState().typeOf(StateType.NUMBER)){
				throw new IllegalFormatException("Unexpected end of input while being in state " + state);
			}
			if(exitCurrentState() != null){
				throw new IllegalFormatException(
						"Unexpected end of input while being in state " + state + " ( just exited NUMBER state)");
			}
		} else if(!stateStack.isEmpty()){
			// should not get here
			throw new RuntimeException("State stack is not empty, but doneWithRoot is true");
		}
	}
	
	
	/**
	 * If state is null, the machine is outside of the root: either before 
	 * entering the root (doneWithRoot == false) or after exiting the root 
	 * (doneWithRoot == true)
	 * 
	 * @return
	 */
	public State getCurrentState(){
		return state;
	}
	
	/**
	 * Check if the the whole top-level node has been processed.
	 *  
	 * @return true if the top level node (root) has been validated and machine
	 *         is outside the root (current state is null)
	 */
	public boolean doneWithRoot(){
		return doneWithRoot;
	}
	
	public State getRootState(){
		return rootState;
	}
	
	private void enterNewState(StateType newStateType){
		if(!stateStack.isEmpty()){
			// parent state is not empty now
			stateStack.peek().notEmpty();
		}
		state = new State(newStateType);
		stateStack.add(state);
		debug("Entered " + state + "(" + (state.isEmpty() ? "empty" : "not empty" + ", cur states: " + stateStack)
				+ ")");
	}
	
	/**
	 * Exits current state.
	 * 
	 * @return a state that was a parent for the one we just exited. I.e. the
	 *         returned state became a current state. If the state we just
	 *         exited was a root, return null.
	 */
	private State exitCurrentState(){
		if(state == null){
			throw new IllegalArgumentException("The machine is not in any state currently");
		}		
		State popped = stateStack.pop();
		debug("Exited " + popped + "(" + (state.isEmpty()?"empty":"not empty") + ")" + ", cur states: " + stateStack);
		if(stateStack.isEmpty()){
			doneWithRoot = true;
			state = null;
		} else {
			state = stateStack.peek();
		}
		return state;// return state just for convenience 
	}
	
	/**
	 * Push a provided byte into the machine. This can lead to a change of state
	 * if this byte is the beginning of a new state that is inside the current
	 * one or if this byte is the closing byte of the current state (closing
	 * bracket for objects and arrays; not-escaped quote for strings; comma for
	 * array and object elements; ws, comma and closing brackets for numbers;
	 * colon for a name state).<br>
	 * Whitespaces are ignored before and after the root node and between
	 * tokens.
	 */
	public void push(byte b) throws IllegalFormatException{
		debug("new byte: " + b + " (" + (char)b + ")");
		if(state == null){
			if(isWhitespace(b)){
				return; // ws outside the root influence nothing
			}
			if(doneWithRoot){
				throw new IllegalFormatException("The JSON structure contains a non-space byte outside of the root: " + b);
			}
//			enterNewState(State.VALUE);
//			pushValue(b); // start root state
			// do not make the root a value because there is no signal to exit it at the end
			enterNewState(b);
			rootState = state;
			return;
		} 
		if(state.typeOf(StateType.OBJECT)){
			pushObject(b);
		} else if(state.typeOf(StateType.OBJECT_ELEMENT)){
			pushObjectElement(b);
		} else if(state.typeOf(StateType.NAME)){
			pushName(b);
		} else if(state.typeOf(StateType.ARRAY)){
			pushArray(b);
		} else if(state.typeOf(StateType.ARRAY_ELEMENT)){
			pushArrayElement(b);
		} else if(state.typeOf(StateType.VALUE)){
			pushValue(b);
		} else if(state.typeOf(StateType.NUMBER)){
			pushNumber(b);
		} else if(state.typeOf(StateType.STRING)){
			pushString(b);
		} else if(state.typeOf(StateType.NULL)){
			pushNull(b);
		} else if(state.typeOf(StateType.TRUE)){
			pushTrue(b);
		} else if(state.typeOf(StateType.FALSE)){
			pushFalse(b);
		}
	}
	
	private void pushString(byte b) throws IllegalFormatException {
		if(b == '"' && !stringReadingStateMachine.isInEscapedSequence()){
			// end of string!
			exitCurrentState();
		}else{
			stringReadingStateMachine.pushByte(b);
			state.notEmpty();
		}
	}
	private void pushNull(byte b) throws IllegalFormatException{
		if(!state.typeOf(StateType.NULL)){
			throw new RuntimeException("The current state is not NULL: " + state);
		}
		if(b != KEYWORD_NULL.charAt(curKeywordLen)){
			throw new IllegalFormatException("Unexpected byte in 'null' at pos " + curKeywordLen + ": " + b);
		}
		curKeywordLen++;
		if(curKeywordLen == KEYWORD_NULL.length()){
			curKeywordLen = 0;
			exitCurrentState();
			if(state != null && !state.typeOf(StateType.VALUE)){
				throw new RuntimeException("'null' should always be inside of a value");
			}
		}
	}
	
	private void pushTrue(byte b) throws IllegalFormatException{
		if(!state.typeOf(StateType.TRUE)){
			throw new RuntimeException("The current state is not TRUE: " + state);
		}
		if(b != KEYWORD_TRUE.charAt(curKeywordLen)){
			throw new IllegalFormatException("Unexpected byte in 'true' at pos " + curKeywordLen + ": " + b);
		}
		curKeywordLen++;
		if(curKeywordLen == KEYWORD_TRUE.length()){
			curKeywordLen = 0;
			exitCurrentState();
			if(state != null && !state.typeOf(StateType.VALUE)){
				throw new RuntimeException("'true' should always be inside of a value");
			}
		}
	}
	
	private void pushFalse(byte b) throws IllegalFormatException{
		if(!state.typeOf(StateType.FALSE)){
			throw new RuntimeException("The current state is not FALSE: " + state);
		}
		if(b != KEYWORD_FALSE.charAt(curKeywordLen)){
			throw new IllegalFormatException("Unexpected byte in 'false' at pos " + curKeywordLen + ": " + b);
		}
		curKeywordLen++;
		if(curKeywordLen == KEYWORD_FALSE.length()){
			curKeywordLen = 0;
			exitCurrentState();
			if(state != null && !state.typeOf(StateType.VALUE)){
				throw new RuntimeException("'false' should always be inside of a value");
			}
		}
	}
	
	private void pushNumber(byte b)throws IllegalFormatException {
		if(isNumberEnd(b)){
			exitCurrentState();
			if(state == null){ // if number is a root b can only be a ws 
				if(!expectNonSpaceBytesJustAfterRoot && !isWhitespace(b)){
					throw new IllegalFormatException("Got non-space byte outside a root: " + b);
				}
			} else { 
				if(!state.typeOf(StateType.VALUE)){
					throw new RuntimeException("Number should always be inside of a value");
				}
				pushValue(b);
			}
		} else {
			numberStateMachine.push(b);
		}
	}
	
	private boolean isNumberEnd(byte b){
		return isWhitespace(b) || b == ',' || b == '}' || b == ']';
	}
	private boolean isWhitespace(byte b) {
		return b == 9|| b == 10 || b == 13 || b == 32;
	}
	
	/**
	 * Push byte to an object state (throw RuntimeException if current state is not object).<br> 
	 * Machine is in object state when the cursor is between matching curl brackets and not 
	 * in an object element.<br>
	 * If we are already in an object we can only have ws in the
	 * beginning (-> stay in the object), '}' (at the end -> exit object) or a quote ->
	 * enter a OBJECT_ELEMENT state (name-value pair) and a name state (string follower by a quote).
	 * 
	 * @param b
	 */
	private void pushObject(byte b) throws IllegalFormatException{
		if(!state.typeOf(StateType.OBJECT)) {
			throw new RuntimeException("The current state is not OBJECT: " + state);
		}
		if(isWhitespace(b)){
			return; 
		}
		if(b == '}'){
			exitCurrentState();
		} else if(b == '"') {
			enterNewState(StateType.OBJECT_ELEMENT);
			pushObjectElement(b);
		} else {
			throw new IllegalFormatException("Unexpected byte in an object outside of an object element: " + b);
		}
	}
	
	
	/**
	 * In an object element we can have:
	 * <ul>
	 * <li>ws - > just stay there</li>
	 * <li>comma - > exit this element and enter another one</li>
	 * <li>quote - > enter name (string with expected colon after it)</li>
	 * </ul>
	 * 
	 * @param b
	 */
	private void pushObjectElement(byte b) throws IllegalFormatException{
		if(!state.typeOf(StateType.OBJECT_ELEMENT)) {
			throw new RuntimeException("The current state is not OBJECT_ELEMENT: " + state);
		}
		if(isWhitespace(b)){
			return; 
		}
		if(b == ','){
			exitObjectElement();
			enterNewState(StateType.OBJECT_ELEMENT);
//		} else if(b == '}'){ // now exit object element at the time of exiting the inner value
//			exitObjectElement();
//			pushObject(b); // translate closing signal to the outer object
		} else if(b== '"'){
			enterNewState(StateType.NAME);
			enterStringState();
		} else {
			throw new IllegalFormatException("Unexpected byte in an object element outside of name and value: " + b);
		}
	}
	
	private void exitObjectElement(){
		if(!exitCurrentState().typeOf(StateType.OBJECT)){ // should exit into outer object
			throw new RuntimeException("Object element should always be inside of an object");
		}
	}
	
	/**
	 * Push a byte if the machine is already in the name state (i.e. it already
	 * exited the inner string state, because when the opening quote is pushed
	 * the machine enters name state and string state at once, without waiting
	 * for another input).<br>
	 * Basically, the name state contains only an opening quote (which it shares
	 * with the string state), ws (if any) after the inner string and a colon,
	 * which serves as a signal to exit the name state and enter a value state.
	 */
	private void pushName(byte b) throws IllegalFormatException{
		if(!state.typeOf(StateType.NAME)) {
			throw new RuntimeException("The current state is not NAME: " + state);
		}
		if(isWhitespace(b)){
			return; 
		}
		if(b == ':'){
			if(!exitCurrentState().typeOf(StateType.OBJECT_ELEMENT)){
				throw new RuntimeException("Name should always be inside of an objct element");
			}
			enterNewState(StateType.VALUE);
		} else {
			throw new IllegalFormatException("Unexpected byte in a name outside of a string");
		}
	}
		
	/**
	 * Push a byte into the array state (throw RuntimeException if the current
	 * state is not an array).<br>
	 * If we are already in an array we can only have ws in the beginning (->
	 * stay in the array or the beginning of an
	 * array element (-> enter an array element state and push the byte into it)
	 * 
	 * @param b
	 */
	private void pushArray(byte b)throws IllegalFormatException{
		if(!state.typeOf(StateType.ARRAY)) {
			throw new RuntimeException("The current state is not ARRAY: " + state);
		}
		if(isWhitespace(b)){
			return; 
		}
		if(b == ']'){
			exitCurrentState();
		} else {
			enterNewState(StateType.ARRAY_ELEMENT);
			pushArrayElement(b);
		}	
	}
	
	/**
	 * In an array element we can have:
	 * <ul>
	 * <li>ws - > just stay there</li>
	 * <li>comma - > exit this element and enter another one</li>
	 * <li>']' -> exit the element and push it to outer array (array elements should
	 * always be in an array)</li>
	 * <li>anything else - > enter value state and push the byte into it</li>
	 * </ul>
	 * 
	 * @param b
	 */
	private void pushArrayElement(byte b)throws IllegalFormatException{
		if(!state.typeOf(StateType.ARRAY_ELEMENT)) {
			throw new RuntimeException("The current state is not ARRAY_ELEMENT: " + state);
		}
		if(isWhitespace(b)){
			return; // spaces in the beginning of an object influence nothing
		}
		if(b == ','){
			exitArrayElement();
			enterNewState(StateType.ARRAY_ELEMENT);
//		} else if(b == ']'){ // now exit array element at the time of exiting the inner value
//			exitArrayElement();
//			pushArray(b); // translate closing signal to the outer array
		} else {
			enterNewState(StateType.VALUE);
			pushValue(b);
		}
	}
	
	private void exitArrayElement(){
		if(!exitCurrentState().typeOf(StateType.ARRAY)){ // should exit into outer array
			throw new RuntimeException("Array element should always be inside of an array");
		}
	}

	/**
	 * Push a byte into a VALUE state, which basically contains only ws around
	 * an actual value (child state) and a closing symbol, which it shares with
	 * the parent. <br>
	 * Whitespaces are ignored. <br>
	 * The value state is a container for an object, array, string keywords or
	 * number state, allowing to have ws around corresponding tokens.
	 * 
	 * @param b
	 * @throws IllegalFormatException
	 */
	private void pushValue(byte b) throws IllegalFormatException{
		if(!state.typeOf(StateType.VALUE)) {
			throw new RuntimeException("The current state is not VALUE: " + state);
		}
		if(isWhitespace(b)){
			return; 
		}
		if(state.isEmpty()){
			enterNewState(b);
			return;
		}
		// VALUE is not empty -> it has a child, which is now closed -> it can be exited 
		// after exiting the value state machine can be in array or object element state 
		if(b == ','){
			exitCurrentState(); // exit to an array/object element
			if (!state.typeOf(StateType.ARRAY_ELEMENT) && !state.typeOf(StateType.OBJECT_ELEMENT)) {
				throw new IllegalFormatException("Value should be inside of an array or an object element");
			}
			push(b); // translate the signal to the parent
		} else if(b == '}') {
			if(!exitCurrentState().typeOf(StateType.OBJECT_ELEMENT)){
				throw new IllegalFormatException("Unexpected '}' in VALUE that is inside " + state);
			}
			exitObjectElement();
			pushObject(b); // translate closing signal to the outer object			 
		} else if(b == ']'){
			if(!exitCurrentState().typeOf(StateType.ARRAY_ELEMENT)){
				throw new IllegalFormatException("Unexpected ']' in VALUE that is inside " + state);
			}
			exitArrayElement();
			pushArray(b); // translate closing signal to the outer array
		} else {
			throw new IllegalFormatException("Unexpected byte in VALUE state: " + b + "(" + (char)b + ")");
		}
	}
	
	/**
	 * The enter method for a value of any type: decide (based on the provided
	 * byte) what type of value this is and proceed correspondingly - either
	 * just enters the appropriate state or also push provided byte into this
	 * state (for keywords and numbers).
	 * 
	 * @param b
	 *            the first non-space byte of a new value
	 * @throws IllegalFormatException
	 */
	private void enterNewState(byte b) throws IllegalFormatException {
		if(b == '{'){
			enterNewState(StateType.OBJECT);
		} else if(b == '['){
			enterNewState(StateType.ARRAY);
		} else if(b == '"'){
			enterStringState();
		} else if(b == 't'){
			enterNewState(StateType.TRUE);
			state.notEmpty();
			pushTrue(b);
		} else if(b == 'f'){
			enterNewState(StateType.FALSE);
			state.notEmpty();
			pushFalse(b);
		} else if(b == 'n'){
			enterNewState(StateType.NULL);
			state.notEmpty();
			pushNull(b);
		} else { // should be a number, otherwise - throw an exception
			if(isNumberEnd(b)){
				throw new IllegalFormatException("Empty number: got byte " + b + " as a first symbol of a number");
			}
			enterNewState(StateType.NUMBER);
			numberStateMachine.reset(); // prepare for a new number
			state.notEmpty();
			pushNumber(b);
		}
	}
	
	private void enterStringState(){
		enterNewState(StateType.STRING);
		stringReadingStateMachine.reset();
	}
	
	private static void debug(String str){
		if(DEBUG){
			System.out.println(str);
		}
	}

	public static class State {
		
		private StateType type;

		private boolean isEmpty = true;
		
		private State(StateType type){
			this.type = type;
		}
		
		private State(StateType type, boolean isEmpty){
			this(type);
			this.isEmpty = isEmpty; 
		}

		public void notEmpty() {
			isEmpty = false;
		}

		public boolean isEmpty() {
			return isEmpty;
		}
		
		public boolean typeOf(StateType type){
			return this.type == type;
		}
		
		/**
		 * Number state is special because number has no end of state symbol (like a quote for 
		 * a string, a bracket for an object and an array and the known length for keywords).
		 * We know that the number is over only when we meet ws, comma, a bracket or eof
		 * @return
		 */
		public boolean isNumberState(){
			return this.type == StateType.NUMBER;
		}

		@Override
		public String toString() {
			return type.toString();
		}
	}
}
