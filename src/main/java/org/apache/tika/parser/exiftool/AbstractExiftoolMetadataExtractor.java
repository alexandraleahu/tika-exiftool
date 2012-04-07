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
package org.apache.tika.parser.exiftool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.external.ExternalParser;
import org.apache.tika.parser.xml.ElementMetadataHandler;
import org.apache.tika.parser.xml.XMLParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * A chained extractor which first calls an ExifTool {@link ExternalParser} requesting XML output
 * format then feeds that result into another {@link XMLParser} for the final metadata extraction.
 *
 * ExifTool command line is required and the path to that executable is defined by <code>exiftool.executable</code>
 * in <code>tika.exiftool.properties</code> or <code>tika.exiftool.override.properties</code>.
 *
 * @author rgauss
 * @see <a href="http://www.sno.phy.queensu.ca/~phil/exiftool/">ExifTool</a>
 *
 */
public abstract class AbstractExiftoolMetadataExtractor {
	/**
	 * Logger for this class
	 */
	private static final Log logger = LogFactory.getLog(AbstractExiftoolMetadataExtractor.class);

	private static final String DEFAULT_EXIFTOOL_EXECUTABLE = "exiftool";
	private static final String DEFAULT_CHARSET = "UTF-8";

	private static final String PROPERTIES_OVERRIDE_FILE = "tika.exiftool.override.properties";
    private static final String PROPERTIES_FILE = "tika.exiftool.properties";
    private static final String PROPERTY_EXIFTOOL_EXECUTABLE = "exiftool.executable";

    private final Metadata metadata;
    private final Set<MediaType> supportedTypes;
    private final String runtimeExiftoolExecutable;

    public AbstractExiftoolMetadataExtractor(Metadata metadata, Set<MediaType> supportedTypes) {
    	this.metadata = metadata;
    	this.supportedTypes = supportedTypes;
    	this.runtimeExiftoolExecutable = null;
    }
    
    public AbstractExiftoolMetadataExtractor(Metadata metadata, Set<MediaType> supportedTypes, String runtimeExiftoolExecutable) {
    	this.metadata = metadata;
    	this.supportedTypes = supportedTypes;
    	if (runtimeExiftoolExecutable != null && !runtimeExiftoolExecutable.equals("")) {
    		this.runtimeExiftoolExecutable = runtimeExiftoolExecutable;
    	} else {
    		this.runtimeExiftoolExecutable = null;
    	}
    }

    /**
     * Gets the command line executable path for exiftool.
     *
     * The command is fetched from a property file named "tika.exiftool.properties"
     * If a file called "tika.exiftool.override.properties" is found on classpath, this is used instead
     */
	public final String getExiftoolExecutable() {
		if (runtimeExiftoolExecutable != null) {
			return runtimeExiftoolExecutable;
		}
		String executable = DEFAULT_EXIFTOOL_EXECUTABLE;
		InputStream stream;
        stream = this.getClass().getResourceAsStream(PROPERTIES_OVERRIDE_FILE);
        if (stream == null) {
            stream = this.getClass().getResourceAsStream(PROPERTIES_FILE);
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


	/**
	 * Gets an ExifTool {@link ExternalParser} to do the work of extracting the metadata in the
	 * image into XML format.
	 *
	 * @return an ExifTool <code>ExternalParser</code>
	 */
	protected Parser getFileParser() {
		ExternalParser parser = new ExternalParser();
    	parser.setCommand(new String[] { getExiftoolExecutable(), "-X", ExternalParser.INPUT_FILE_TOKEN });
    	parser.setSupportedTypes(supportedTypes);
    	return parser;
	}

	/**
	 * Gets a parser responsible for extracting metadata from the XML output of the ExifTool external parser.
	 *
	 * @return the XML parser
	 */
	protected abstract Parser getXmlResponseParser();

	public void parse(InputStream stream) throws IOException,
			SAXException, TikaException {

		ParseContext fileContext = new ParseContext();
        Parser fileParser = getFileParser();
        fileContext.set(Parser.class, fileParser);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        OutputStreamWriter outputWriter = new OutputStreamWriter(result, DEFAULT_CHARSET);
        ContentHandler fileHandler = new BodyContentHandler(outputWriter);

        if (logger.isDebugEnabled()) {
        	logger.debug("parsing file for XML output");
        }

        try {
	        fileParser.parse(stream, fileHandler, metadata, fileContext);

	        if (logger.isTraceEnabled()) {
		    	logger.trace("XML output:\n" + result.toString(DEFAULT_CHARSET));
	        }

	        ByteArrayInputStream xmlInputStream = new ByteArrayInputStream(result.toByteArray());

	        ParseContext xmlResponseContext = new ParseContext();
	        Parser xmlResponseParser = getXmlResponseParser();
	        xmlResponseContext.set(Parser.class, xmlResponseParser);
	        ContentHandler xmlResponseHandler = new BodyContentHandler();

	        if (logger.isDebugEnabled()) {
	        	logger.debug("parsing XML output for metadata");
	        }
	        xmlResponseParser.parse(xmlInputStream, xmlResponseHandler, metadata, xmlResponseContext);
        } catch (IOException e) {
        	if (logger.isWarnEnabled()) {
        		logger.warn("exiftool may not be be available: " + e.getMessage());
        	}
        }
	}

	/**
	 * Constructs a full {@link QName} from the given <code>exiftoolProperty</code>
	 *
	 * @param exiftoolProperty
	 * @return the full QName for the property
	 */
	public static QName getQName(Property exiftoolProperty) {
		String prefix = null;
		String namespaceUrl = null;
		if (exiftoolProperty.getName().startsWith(ExifToolMetadata.PREFIX_IPTC)) {
			prefix = ExifToolMetadata.PREFIX_IPTC;
			namespaceUrl = ExifToolMetadata.NAMESPACE_URI_IPTC;
		} else if (exiftoolProperty.getName().startsWith(ExifToolMetadata.PREFIX_XMP_DC)) {
			prefix = ExifToolMetadata.PREFIX_XMP_DC;
			namespaceUrl = ExifToolMetadata.NAMESPACE_URI_XMP_DC;
		} else if (exiftoolProperty.getName().startsWith(ExifToolMetadata.PREFIX_XMP_IPTC_CORE)) {
			prefix = ExifToolMetadata.PREFIX_XMP_IPTC_CORE;
			namespaceUrl = ExifToolMetadata.NAMESPACE_URI_XMP_IPTC_CORE;
		} else if (exiftoolProperty.getName().startsWith(ExifToolMetadata.PREFIX_XMP_IPTC_EXT)) {
			prefix = ExifToolMetadata.PREFIX_XMP_IPTC_EXT;
			namespaceUrl = ExifToolMetadata.NAMESPACE_URI_XMP_IPTC_EXT;
		} else if (exiftoolProperty.getName().startsWith(ExifToolMetadata.PREFIX_XMP_PHOTOSHOP)) {
			prefix = ExifToolMetadata.PREFIX_XMP_PHOTOSHOP;
			namespaceUrl = ExifToolMetadata.NAMESPACE_URI_XMP_PHOTOSHOP;
		} else if (exiftoolProperty.getName().startsWith(ExifToolMetadata.PREFIX_XMP_PLUS)) {
			prefix = ExifToolMetadata.PREFIX_XMP_PLUS;
			namespaceUrl = ExifToolMetadata.NAMESPACE_URI_XMP_PLUS;
		} else if (exiftoolProperty.getName().startsWith(ExifToolMetadata.PREFIX_XMP_XMP_RIGHTS)) {
			prefix = ExifToolMetadata.PREFIX_XMP_XMP_RIGHTS;
			namespaceUrl = ExifToolMetadata.NAMESPACE_URI_XMP_XMP_RIGHTS;
		}
		if (prefix != null && namespaceUrl != null) {
			return new QName(namespaceUrl, exiftoolProperty.getName().replace(prefix + ExifToolMetadata.PREFIX_DELIMITER, ""), prefix);
		}
		return null;
	}

	/**
	 * Extension of <code>XMLParser</code> which provides a {@link ContentHandler} which
	 * recognizes ExifTool's namespaces and elements.
	 *
	 * @author rgauss
	 *
	 */
	public abstract class AbstractExiftoolXmlParser extends XMLParser {

		private static final long serialVersionUID = 4937561608422796636L;

		/**
		 * Gets an element handler for the given <code>Property</code> and <code>QName</code>.
		 *
		 * @param property
		 * @param metadata
		 * @param qName
		 * @param tikaMetadata
		 * @return
		 */
		protected ContentHandler getElementMetadataHandler(
				Property property, Metadata metadata, QName qName, Object tikaMetadata) {
			if (tikaMetadata != null && tikaMetadata instanceof Property) {
				return new ElementMetadataHandler(
	                    qName.getNamespaceURI(),
	                    qName.getLocalPart(),
	                    metadata,
	                    (Property) tikaMetadata);
			} else {
				return new ElementMetadataHandler(
	                    qName.getNamespaceURI(),
	                    qName.getLocalPart(),
	                    metadata,
	                    (String) tikaMetadata);
			}
		}

		/**
		 * Gets the element handlers for the given <code>property</code>
		 *
		 * @param property
		 * @param metadata
		 * @return the element handler
		 */
		protected Collection<ContentHandler> getElementMetadataHandlers(Property property, Metadata metadata) {
			ArrayList<ContentHandler> handlers = new ArrayList<ContentHandler>();
			QName qName = AbstractExiftoolMetadataExtractor.getQName(property);
			if (qName != null) {
				if (ExiftoolTikaMapper.getExiftoolToTikaMetadataMap().get(property) != null) {
					for (Object tikaMetadata : ExiftoolTikaMapper.getExiftoolToTikaMetadataMap().get(property)) {
						handlers.add(getElementMetadataHandler(property, metadata, qName, tikaMetadata));
					}
				} else {
					if (logger.isTraceEnabled()) {
			    		logger.trace("no tikaMetadata found for " + property.getName());
			    	}
				}
			}
			return handlers;
		}
		
	}

}
