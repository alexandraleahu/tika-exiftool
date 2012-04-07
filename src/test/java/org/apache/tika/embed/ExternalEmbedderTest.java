/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 * 
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;


import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for {@link ExternalEmbedder}s.
 *
 * @author rgauss
 *
 */
public class ExternalEmbedderTest
    extends TestCase
{
	/**
	 * Logger for this class
	 */
	private static final Log logger = LogFactory
			.getLog(ExternalEmbedderTest.class);

	protected static final String EXPECTED_METADATA_PREFIX = "TIKA-EMBEDDED: ";
	protected static final String DEFAULT_CHARSET = "UTF-8";
    private static final String COMMAND_METADATA_ARGUMENT_DESCRIPTION = "dc:description";
    private static final String EXPECTED_APPENDED_METADATA = EXPECTED_METADATA_PREFIX + "additional embedded metadata";
    private static final String TEST_TXT_PATH = "/test-documents/testTXT.txt";

	/**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ExternalEmbedderTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ExternalEmbedderTest.class );
    }

    /**
     * Gets the expected returned metadata value for the given field
     *
     * @param fieldName
     * @return a prefix added to the field name
     */
    protected String getExpectedMetadataValueString(String fieldName) {
    	return EXPECTED_METADATA_PREFIX + fieldName;
    }

    /**
     * Gets the tika <code>Metadata</code> object containing data to be embedded.
     *
     * @return the populated tika metadata object
     */
    protected Metadata getMetadataToEmbed() {
    	Metadata metadata = new Metadata();
    	metadata.add(Metadata.DESCRIPTION, EXPECTED_APPENDED_METADATA);
    	return metadata;
    }

    /**
     * Gets the <code>Embedder</code> to test.
     *
     * @return the embedder under test
     */
    protected Embedder getEmbedder() {
    	ExternalEmbedder embedder = new ExternalEmbedder();
    	Map<String, String[]> metadataCommandArguments = new HashMap<String, String[]>();
    	metadataCommandArguments.put(Metadata.DESCRIPTION, new String[] { COMMAND_METADATA_ARGUMENT_DESCRIPTION } );
    	embedder.setMetadataCommandArguments(metadataCommandArguments);
    	return embedder;
    }

    /**
     * Gets the original input stream before metadata has been embedded.
     *
     * @return a fresh input stream
     */
    protected InputStream getOriginalInputStream() {
    	return this.getClass().getResourceAsStream(TEST_TXT_PATH);
    }

    /**
     * Gets the parser to use to verify the result of the embed operation.
     *
     * @return the parser to read embedded metadata
     */
    protected Parser getParser() {
    	return new TXTParser();
    }

    /**
     * Whether or not the final result of reading the now embedded metadata is expected in the output of the external tool
     *
     * @return whether or not results are expected in command line output
     */
    protected boolean getIsMetadataExpectedInOutput() {
    	return true;
    }

    /**
     * Tests embedding metadata then reading metadata to verify the results.
     */
    public void testEmbed()
    {
    	Metadata metadataToEmbed = getMetadataToEmbed();
    	Embedder embedder = getEmbedder();

		try {
			InputStream origInputStream = getOriginalInputStream();

			InputStream embeddedInputStream = embedder.embed(origInputStream, metadataToEmbed, null);

			ParseContext context = new ParseContext();
	        Parser parser = getParser();
	        context.set(Parser.class, parser);

	        ByteArrayOutputStream result = new ByteArrayOutputStream();
	        OutputStreamWriter outputWriter = new OutputStreamWriter(result, DEFAULT_CHARSET);
	        ContentHandler handler = new BodyContentHandler(outputWriter);

	        Metadata embeddedMetadata = new Metadata();

	        parser.parse(embeddedInputStream, handler, embeddedMetadata, context);

	        String outputString = null;
	        if (getIsMetadataExpectedInOutput()) {
	        	outputString = result.toString(DEFAULT_CHARSET);
	        	logger.debug("outputString=" + outputString);
	        } else {
	        	assertTrue("no metadata found", embeddedMetadata.size() > 0);
	        }

	        for (String metadataName : metadataToEmbed.names()) {
	        	if (metadataToEmbed.get(metadataName) != null) {
	        		logger.trace("expected value=" + metadataToEmbed.get(metadataName));
		        	boolean foundExpectedValue = false;
		        	if (getIsMetadataExpectedInOutput()) {
		        		foundExpectedValue = outputString.contains(EXPECTED_METADATA_PREFIX);
		        	} else {
			        	if (embeddedMetadata.isMultiValued(metadataName)) {
			        		for (String embeddedValue : embeddedMetadata.getValues(metadataName)) {
			        			logger.debug("embedded value of '" + metadataName + "' = '" + embeddedValue + "'");
			        			if (embeddedValue != null) {
			        				foundExpectedValue = embeddedValue.contains(EXPECTED_METADATA_PREFIX);
			        			}
							}
			        	} else {
			        		String embeddedValue = embeddedMetadata.get(metadataName);
			        		logger.debug("embedded value of '" + metadataName + "' = '" + embeddedValue + "'");
			        		assertNotNull("expected metadata for " + metadataName + " not found", embeddedValue);
		        			foundExpectedValue = embeddedValue.contains(EXPECTED_METADATA_PREFIX);
			        	}
		        	}
		        	assertTrue("result did not contain expected appended metadata: " + metadataToEmbed.get(metadataName),
		 	        		foundExpectedValue);
	        	}
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			assertTrue(e.getMessage(), false);
		} catch (TikaException e) {
			logger.error(e.getMessage(), e);
			assertTrue(e.getMessage(), false);
		} catch (SAXException e) {
			logger.error(e.getMessage(), e);
			assertTrue(e.getMessage(), false);
		}

    }

}
