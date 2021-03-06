package com.bigjson.parser;

import java.io.File;

public class TestUtils {
	
	private static File testFileDir = new File("testFiles");
	private static int fileInd = 1;
	
	public static File getTestFileDir(){
		return testFileDir;
	}
	
	public static File getProperJSONFile(int ind){
		return new File(testFileDir, "PositiveTest" + ind + ".json");
	}
	
	public static File getInvalidJSONFile(int ind){
		return new File(testFileDir, "NegativeTest" + ind + ".json");
	}
	
	public static File getGeneratedTestFile(){
		File file = getGeneratedFilePath("test" + fileInd + ".txt");
		fileInd++;
//		System.out.println("Current test file: " + file);
		return file;
	}
	
	public static File getGeneratedFilePath(String fileName){
		File generatedTestFileDir = new File(testFileDir, "autoGenerated/");
		generatedTestFileDir.mkdir();
		File file = new File(generatedTestFileDir, fileName);
		return file;
	}

}
