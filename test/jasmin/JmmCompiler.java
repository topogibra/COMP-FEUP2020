package jasmin;

import java.io.File;
import java.lang.reflect.Method;
import pt.up.fe.specs.util.SpecsIo;

public class JmmCompiler {
	
    private static String CLASS_WITH_MAIN = "main.jmm";
	private static String GENERATED_FILES_PATH = "jasminCode/";

	
	/**
	 * Compiles a .jmm file to .j code.
     *
     * @param jmm the .jmm file
     * @return a string with the .j code
	 */
	public static String compile(File jmm){

		// This method needs to be implemented before the tests can run
		// Below there is an example implementation, that assumes that the .j file is generated in the repository root
		// Adapt the code according to your implementation.
		//throw new RuntimeException("Implement JmmCompiler.compile() in order to test the execution of .jmm files");


		// Executes J-- compiler		
		try {
            // Get class with main
            Class<?> mainClass = Class.forName(CLASS_WITH_MAIN);

            // It is expected that class has a main function
            Method mainMethod = mainClass.getMethod("main", String[].class);

            // Invoke main method with file as argument
            String[] mainArgs = { jmm.getAbsolutePath() };
            Object[] invokeArgs = { mainArgs };
            mainMethod.invoke(null, invokeArgs);

        } catch (Exception e) {
			throw new RuntimeException("Error with compiling jmm", e);
		}

		var filename = GENERATED_FILES_PATH + SpecsIo.removeExtension(jmm.getName()) + ".j";
		
		var jFile = new File(filename);
				
		if(!jFile.isFile()) {
			throw new RuntimeException("Could not find file " + jFile.getAbsolutePath());
		}

		return SpecsIo.read(jFile);

	}
	
}