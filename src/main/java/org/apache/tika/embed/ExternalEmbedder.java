/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.embed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.io.NullOutputStream;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.external.ExternalParser;

/**
 * Embedder that uses an external program (like sed or exiftool) to embed
 * text content and metadata into a given document.
 *
 * @author rgauss
 *
 */
public class ExternalEmbedder implements Embedder {

	private static final long serialVersionUID = -2828829275642475697L;

	/**
	 * Token to be replaced with a String array of metadata assignment command arguments
	 */
	public static final String METADATA_COMMAND_ARGUMENTS_TOKEN = "${METADATA}";

	/**
	 * Token to be replaced with a String array of metadata assignment command arguments
	 */
	public static final String METADATA_COMMAND_ARGUMENTS_SERIALIZED_TOKEN = "${METADATA_SERIALIZED}";

    /**
     * Media types supported by the external program.
     */
    private Set<MediaType> supportedTypes = Collections.emptySet();

    /**
     * Mapping of Tika metadata to command line parameters.
     */
    private Map<String,String[]> metadataCommandArguments = null;

    /**
     * The external command to invoke.
     * @see Runtime#exec(String[])
     */
    private String[] command = new String[] { "sed", "-e", "$a\\\n" + METADATA_COMMAND_ARGUMENTS_SERIALIZED_TOKEN, ExternalParser.INPUT_FILE_TOKEN };

    private String commandAssignmentOperator = "=";
    private String commandAssignmentDelimeter  = ", ";
    private String commandAppendOperator = "=";

    private boolean quoteAssignmentValues = false;

    private TemporaryResources tmp = new TemporaryResources();


    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return getSupportedTypes();
    }

    public Set<MediaType> getSupportedTypes() {
        return supportedTypes;
    }

    public void setSupportedTypes(Set<MediaType> supportedTypes) {
        this.supportedTypes =
            Collections.unmodifiableSet(new HashSet<MediaType>(supportedTypes));
    }

    public String[] getCommand() {
        return command;
    }

    /**
     * Sets the command to be run. This can include either of
     *  {@link #INPUT_FILE_TOKEN} or {@link #OUTPUT_FILE_TOKEN}
     *  if the command needs filenames.
     * @see Runtime#exec(String[])
     */
    public void setCommand(String... command) {
        this.command = command;
    }


    /**
     * Gets the assignment operator for the command line tool, i.e. "=".
     *
     * @return the assignment operator
     */
    public String getCommandAssignmentOperator() {
		return commandAssignmentOperator;
	}

	/**
	 * Sets the assignment operator for the command line tool, i.e. "=".
	 *
	 * @param commandAssignmentOperator
	 */
	public void setCommandAssignmentOperator(String commandAssignmentOperator) {
		this.commandAssignmentOperator = commandAssignmentOperator;
	}

	/**
	 * Gets the delimiter for multiple assignments for the command line tool, i.e. ", ".
	 *
	 * @return the assignment delimiter
	 */
	public String getCommandAssignmentDelimeter() {
		return commandAssignmentDelimeter;
	}

	/**
	 * Sets the delimiter for multiple assignments for the command line tool, i.e. ", ".
	 *
	 * @param commandAssignmentDelimeter
	 */
	public void setCommandAssignmentDelimeter(String commandAssignmentDelimeter) {
		this.commandAssignmentDelimeter = commandAssignmentDelimeter;
	}

	/**
	 * Gets the operator to append rather than replace a value for the command line tool, i.e. "+=".
	 *
	 * @return the append operator
	 */
	public String getCommandAppendOperator() {
		return commandAppendOperator;
	}

	/**
	 * Sets the operator to append rather than replace a value for the command line tool, i.e. "+=".
	 *
	 * @param commandAppendOperator
	 */
	public void setCommandAppendOperator(String commandAppendOperator) {
		this.commandAppendOperator = commandAppendOperator;
	}

	/**
	 * Gets whether or not to quote assignment values, i.e. tag='value'.
	 * The default is false.
	 *
	 * @return whether or not to quote assignment values
	 */
	public boolean isQuoteAssignmentValues() {
		return quoteAssignmentValues;
	}

	/**
	 * Sets whether or not to quote assignment values, i.e. tag='value'.
	 *
	 * @param quoteAssignmentValues
	 */
	public void setQuoteAssignmentValues(boolean quoteAssignmentValues) {
		this.quoteAssignmentValues = quoteAssignmentValues;
	}

	/**
	 * Gets the map of Metadata keys to command line parameters.
     *
	 * @return
	 */
	public Map<String,String[]> getMetadataCommandArguments() {
       return metadataCommandArguments;
    }

    /**
     * Sets the map of Metadata keys to command line parameters.
     * Set this to null to disable Metadata embedding.
     */
    public void setMetadataCommandArguments(Map<String,String[]> arguments) {
       this.metadataCommandArguments = arguments;
    }

    /**
     * Constructs a collection of command line arguments responsible for setting individual metadata fields
     * based on the given <code>metadata</code>.
     *
     * @param metadata
     * @return the metadata-related command line arguments
     */
    protected List<String> getCommandMetadataSegments(Metadata metadata) {
    	List<String> commandMetadataSegments = new ArrayList<String>();
    	if (metadata == null || metadata.names() == null) {
    		return commandMetadataSegments;
    	}

    	for (String metadataName : metadata.names()) {
			if (metadataCommandArguments.containsKey(metadataName)) {
				String[] metadataCommandArguments = getMetadataCommandArguments().get(metadataName);
    			if (metadataCommandArguments != null) {
    				for (String metadataCommandArgument : metadataCommandArguments) {
						if (metadata.isMultiValued(metadataName)) {
		            		for (String metadataValue : metadata.getValues(metadataName)) {
		            			String assignmentValue = metadataValue;
		            			if (quoteAssignmentValues) {
		            				assignmentValue = "'" + assignmentValue + "'";
		            			}
		            			commandMetadataSegments.add(
		            					metadataCommandArgument + commandAppendOperator + assignmentValue);
							}
		            	} else {
		            		String assignmentValue = metadata.get(metadataName);
		        			if (quoteAssignmentValues) {
		        				assignmentValue = "'" + assignmentValue + "'";
		        			}
		        			commandMetadataSegments.add(
		            				metadataCommandArgument + commandAssignmentOperator + assignmentValue);
		                }
    				}
    			}
			}
		}
    	return commandMetadataSegments;
    }

    /**
     * Serializes a collection of metadata command line arguments into a single string.
     *
     * @param metadataCommandArguments
     * @return the serialized metadata arguments string
     */
    protected static String serializeMetadata(List<String> metadataCommandArguments) {
    	if (metadataCommandArguments != null) {
    		return Arrays.toString(metadataCommandArguments.toArray());
//    		return ArrayUtils.toString(metadataCommandArguments);
    	}
    	return "";
    }

    /**
     * Executes the configured external command and passes the given document
     *  stream as a simple XHTML document to the given SAX content handler.
     * Metadata is only extracted if {@link #setMetadataExtractionPatterns(Map)}
     *  has been called to set patterns.
     */
    public InputStream embed(
            final InputStream stream,
            Metadata metadata, ParseContext context)
            throws IOException, TikaException {

        boolean inputToStdIn = true;
        boolean outputFromStdOut = true;
        boolean hasMetadataCommandArguments = (metadataCommandArguments != null && !metadataCommandArguments.isEmpty());
        boolean serializeMetadataCommandArgumentsToken = false;
        boolean replacedMetadataCommandArgumentsToken = false;

        TikaInputStream tikaStream = TikaInputStream.get(stream);
        File output = null;

        List<String> commandMetadataSegments = null;
        if (hasMetadataCommandArguments) {
        	commandMetadataSegments = getCommandMetadataSegments(metadata);
        }

        // Build our command
        List<String> origCmd = Arrays.asList(command);
        List<String> cmd = new ArrayList<String>();
        for (String commandSegment : origCmd) {
           if(commandSegment.indexOf(ExternalParser.INPUT_FILE_TOKEN) != -1) {
        	  commandSegment = commandSegment.replace(ExternalParser.INPUT_FILE_TOKEN, tikaStream.getFile().toString());
              inputToStdIn = false;
           }
           if(commandSegment.indexOf(ExternalParser.OUTPUT_FILE_TOKEN) != -1) {
        	  if (output == null) {
        		  output = tmp.createTemporaryFile();
        		  outputFromStdOut = false;
        	  }
        	  commandSegment = commandSegment.replace(ExternalParser.OUTPUT_FILE_TOKEN, output.toString());
           }
           if(commandSegment.indexOf(METADATA_COMMAND_ARGUMENTS_SERIALIZED_TOKEN) != -1) {
        	   serializeMetadataCommandArgumentsToken = true;
           }
           if(commandSegment.indexOf(METADATA_COMMAND_ARGUMENTS_TOKEN) != -1) {
        	   if (hasMetadataCommandArguments) {
        		   for (String commandMetadataSegment : commandMetadataSegments) {
        			   cmd.add(commandMetadataSegment);
        		   }
        	   }
        	   replacedMetadataCommandArgumentsToken = true;
	       } else {
	    	   cmd.add(commandSegment);
	       }
        }
        if (hasMetadataCommandArguments) {
        	if (serializeMetadataCommandArgumentsToken) {
        		// Find all metadata tokens and replace with encapsulated metadata
        		int i=0;
	        	for (String commandSegment : cmd) {
	        		if(commandSegment.indexOf(METADATA_COMMAND_ARGUMENTS_SERIALIZED_TOKEN) != -1) {
	        			commandSegment = commandSegment.replace(METADATA_COMMAND_ARGUMENTS_SERIALIZED_TOKEN,
	        					serializeMetadata(commandMetadataSegments));
	        			cmd.set(i, commandSegment);
	        		}
	        		i++;
				}
        	} else if (!replacedMetadataCommandArgumentsToken && !serializeMetadataCommandArgumentsToken) {
        		// Tack metadata onto the end of the cmd as arguments
        		cmd.addAll(commandMetadataSegments);
        	}
        }

        // Execute
        Process process;
        if(cmd.toArray().length == 1) {
           process = Runtime.getRuntime().exec( cmd.toArray(new String[] {})[0] );
        } else {
           process = Runtime.getRuntime().exec( cmd.toArray(new String[] {}) );
        }

        try {
            if(inputToStdIn) {
               sendInput(process, stream);
            } else {
               process.getOutputStream().close();
            }

            InputStream out = process.getInputStream();
            InputStream err = process.getErrorStream();
            ignoreStream(err);

            if(outputFromStdOut) {
            	return out;
            } else {
            	tmp.dispose();
            	try {
            		process.waitFor();
		      	} catch (InterruptedException ignore) {
		        }
            	FileInputStream outFile = new FileInputStream(output);
	            return outFile;
            }
        } finally {
        	if (outputFromStdOut) {
	            try {
	                process.waitFor();
	            } catch (InterruptedException ignore) {
	            }
        	} else {
        		try {
        			// Clean up temp output files
        			output.delete();
        		} catch (Exception e) {
        		}
        	}
            if (!inputToStdIn) {
            	// Clean up temp input files
            	tikaStream.getFile().delete();
            }
        }

    }

    /**
     * Starts a thread that sends the contents of the given input stream
     * to the standard input stream of the given process. Potential
     * exceptions are ignored, and the standard input stream is closed
     * once fully processed. Note that the given input stream is <em>not</em>
     * closed by this method.
     *
     * @param process process
     * @param stream input stream
     */
    private void sendInput(final Process process, final InputStream stream) {
        new Thread() {
            public void run() {
                OutputStream stdin = process.getOutputStream();
                try {
                    IOUtils.copy(stream, stdin);
                } catch (IOException e) {
                } finally {
                    IOUtils.closeQuietly(stdin);
                }
            }
        }.start();
    }

    /**
     * Starts a thread that reads and discards the contents of the
     * standard stream of the given process. Potential exceptions
     * are ignored, and the stream is closed once fully processed.
     *
     * @param process process
     */
    private void ignoreStream(final InputStream stream) {
        new Thread() {
            public void run() {
                try {
                    IOUtils.copy(stream, new NullOutputStream());
                } catch (IOException e) {
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        }.start();
    }

    /**
     * Checks to see if the command can be run. Typically used with
     *  something like "myapp --version" to check to see if "myapp"
     *  is installed and on the path.
     *
     * @param checkCmd The check command to run
     * @param errorValue What is considered an error value?
     */
    public static boolean check(String checkCmd, int... errorValue) {
       return check(new String[] {checkCmd}, errorValue);
    }
    public static boolean check(String[] checkCmd, int... errorValue) {
       if(errorValue.length == 0) {
          errorValue = new int[] { 127 };
       }

       try {
          Process process;
          if(checkCmd.length == 1) {
             process = Runtime.getRuntime().exec(checkCmd[0]);
          } else {
             process = Runtime.getRuntime().exec(checkCmd);
          }
          int result = process.waitFor();

          for(int err : errorValue) {
             if(result == err) return false;
          }
          return true;
       } catch(IOException e) {
          // Some problem, command is there or is broken
          return false;
       } catch (InterruptedException ie) {
          // Some problem, command is there or is broken
          return false;
      }
    }
}
