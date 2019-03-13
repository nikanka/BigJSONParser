package com.bigjson.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.junit.Test;
import static org.junit.Assert.*;

public class JSONStateMachineTest {

	private Charset utf8 = Charset.forName("UTF-8");
	private JSONStateMachine jsonValidator = new JSONStateMachine();
	
	@Test
	public void shouldValidateAllGoodJSONFiles() throws IOException, IllegalFormatException {
		for(int i = 0; i < 8; i++){
			System.out.println("Validate proper JSON #" + i);
			validate(TestUtils.getProperJSONFile(i));
		}
	}
	
	@Test
	public void shouldThrowExceptionForInvalidFormat() throws IOException, IllegalFormatException {
		expectIllegalFormatException("");
		expectIllegalFormatException(" ");
		expectIllegalFormatException("1,");
		expectIllegalFormatException("true, ");
		expectIllegalFormatException("{} , ");
		expectIllegalFormatException("[] ,");
		expectIllegalFormatException("\"\",");
		expectIllegalFormatException("bull");
		expectIllegalFormatException("nul");
		expectIllegalFormatException("nulk");
		expectIllegalFormatException("[\"root\", 0, {  },]");
		expectIllegalFormatException("{\"first\": \"root\", \"second\" : 0 ,}");
		expectIllegalFormatException("{\"first\": \"root\", \"second\" : 0,}");
		expectIllegalFormatException("{\"1\" : 1, }}");
		expectIllegalFormatException("[1, 2, ]]");
		expectIllegalFormatException("[1, 2, []");
		expectIllegalFormatException("{\"1\":1, \"2\":{}");
		expectIllegalFormatException("\"hgj\\\"");
		expectIllegalFormatException("[1, \"hgj\\\"]");
		expectIllegalFormatException("[1, \"name\": 1");
		

	}
	
	private void validate(File file) throws IOException, IllegalFormatException {
		validate(Files.readAllBytes(file.toPath()));
	}
	private void validate(byte[] bytes) throws IllegalFormatException {
		jsonValidator.reset();
		for(byte b: bytes){
			jsonValidator.push(b);
		}
		jsonValidator.pushEndOfInput();
		assertTrue(jsonValidator.doneWithRoot());
	}
	
	private void expectIllegalFormatException(String json) {
		try{
			validate(json.getBytes(utf8));
		} catch(IllegalFormatException e){
			System.err.println(e.getMessage());
			return;
		}
		throw new RuntimeException(
				"The method should have thrown an IllegalFormatException for file \"" + json + "\"");

	}
}
