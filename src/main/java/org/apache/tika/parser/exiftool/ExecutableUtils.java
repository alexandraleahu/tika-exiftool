package org.apache.tika.parser.exiftool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ExecutableUtils {
	/**
	 * Logger for this class
	 */
	private static final Log logger = LogFactory.getLog(ExecutableUtils.class);
	
	private static final String DEFAULT_EXIFTOOL_EXECUTABLE = "exiftool";

	private static final String PROPERTIES_OVERRIDE_FILE = "tika.exiftool.override.properties";
    private static final String PROPERTIES_FILE = "tika.exiftool.properties";
    private static final String PROPERTY_EXIFTOOL_EXECUTABLE = "exiftool.executable";
	
    /**
     * Gets the command line executable path for exiftool.
     *
     * The command is fetched from a property file named "tika.exiftool.properties"
     * If a file called "tika.exiftool.override.properties" is found on classpath, this is used instead
     */
	public static final String getExiftoolExecutable(String runtimeExiftoolExecutable) {
		if (runtimeExiftoolExecutable != null) {
			return runtimeExiftoolExecutable;
		}
		String executable = DEFAULT_EXIFTOOL_EXECUTABLE;
		InputStream stream;
        stream = ExecutableUtils.class.getResourceAsStream(PROPERTIES_OVERRIDE_FILE);
        if (stream == null) {
            stream = ExecutableUtils.class.getResourceAsStream(PROPERTIES_FILE);
        }
        if(stream != null){
            try {
            	Properties props = new Properties();
                props.load(stream);
                executable = (String) props.get(PROPERTY_EXIFTOOL_EXECUTABLE);
            } catch (IOException e) {
            	logger.warn("IOException while trying to load property file. Message: " + e.getMessage());
            }
        }
        return executable;
	}

}
