package com.bigjson.parser;

import org.junit.Test;

public class NumberCheckingStateMachineTest {

	private NumberCheckingStateMachine numberChecking = new NumberCheckingStateMachine();
	
	@Test
	public void shouldReadInt() throws IllegalFormatException{
		String[] nums = new String[]{
				"1",
				"0",
				"342",
				"11982883746538290903938434",
				"-1",
				"-0",
				"-293864"
		};
		for(String str: nums){
			checkNumber(str);
		}
	}
	
	@Test
	public void shouldReadExp() throws IllegalFormatException{
		String[] nums = new String[]{
				"1E-1",
				"0E287",
				"342e0",
				"11982883746538290903938434E-28735",
				"-1e-0",
				"-0E-037",
				"-293864e1"
		};
		for(String str: nums){
			checkNumber(str);
		}
	}
	
	@Test
	public void shouldReadFrac() throws IllegalFormatException{
		String[] nums = new String[]{
				"1.23",
				"0.0",
				"342.12",
				"11982883746538290903938434.09",
				"-1.37278",
				"-0.7829312309210302983923",
				"-293864.0"
		};
		for(String str: nums){
			checkNumber(str);
		}
	}

	@Test
	public void shouldReadFracExp() throws IllegalFormatException{
		String[] nums = new String[]{
				"1.23e-1",
				"0.0E0",
				"342.12e+92367156",
				"11982883746538290903938434.09E+0",
				"-1.37278e-9087",
				"-0.7829312309210302983923E-0",
				"-293864.0E+9"
		};
		for(String str: nums){
			checkNumber(str);
		}
	}

	@Test
	public void shouldThrowExceptionForUnfinishedFrac() throws IllegalFormatException{
		expectIllegalFormatException("0.");
		expectIllegalFormatException("-0.");
		expectIllegalFormatException("238172.e3");
		expectIllegalFormatException("-238172.E-023");
	}
	
	@Test
	public void shouldThrowExceptionForUnfinishedInt()throws IllegalFormatException{
		expectIllegalFormatException(".0");
		expectIllegalFormatException(".");
		expectIllegalFormatException("-e3");
		expectIllegalFormatException("-.E-023");
		expectIllegalFormatException("E-023");
	}
	
	@Test
	public void shouldThrowExceptionForUnfinishedExp()throws IllegalFormatException{
		expectIllegalFormatException("1e");
		expectIllegalFormatException("-0.7E");
		expectIllegalFormatException("-0e");
		expectIllegalFormatException("-7E-");
		expectIllegalFormatException("6.7E+");
	}
	
	@Test
	public void shouldThrowExceptionForUnexpectedSymbol()throws IllegalFormatException{
		expectIllegalFormatException("+1");
		expectIllegalFormatException("--0.7E5");
		expectIllegalFormatException("0.-5");
		expectIllegalFormatException("-34E-+6");
		expectIllegalFormatException("-34E-6+");
		expectIllegalFormatException("0.43-");
		expectIllegalFormatException("0e.-5");
		expectIllegalFormatException("-34F6");
		expectIllegalFormatException("0.43a");
		expectIllegalFormatException("=0.43a");
		expectIllegalFormatException(" 0.43");
		expectIllegalFormatException("0.43 ");
		expectIllegalFormatException(" ");
		expectIllegalFormatException("");
	}
	
	private void expectIllegalFormatException(String numStr)throws IllegalFormatException{
		try{
			checkNumber(numStr);
		}catch(IllegalFormatException e){
			System.err.println(e.getMessage());
			return;
		}
		throw new RuntimeException(
				"The method should have thrown an IllegalFormatException for number string '" + numStr + "'");
	}
	
	/**
	 * Parse the number. 
	 * @param numStr a string supposedly representing a number in JSON format
	 * @throws IllegalFormatException if there is an error in the number format
	 */
	private void checkNumber(String numStr) throws IllegalFormatException{
		numberChecking.reset();
		for(byte b: numStr.getBytes()){
			numberChecking.push(b);
		}
		if(!numberChecking.numberIsFinished()){
			throw new IllegalFormatException("Number is not finished: '" + numStr + "'");
		}
	}
}
