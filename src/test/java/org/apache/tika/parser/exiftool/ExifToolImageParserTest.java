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

import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.tika.metadata.IPTC;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TIFF;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.helpers.DefaultHandler;

public class ExifToolImageParserTest extends TestCase {
	/**
	 * Logger for this class
	 */
	private static final Log logger = LogFactory
			.getLog(ExifToolImageParserTest.class);

    private final Parser parser = new ExiftoolImageParser();

    public void testJPEGIPTC() throws Exception {
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "image/jpeg");
        InputStream stream =
            getClass().getResourceAsStream("/test-documents/testJPEG_IPTC_EXT.jpg");
        parser.parse(stream, new DefaultHandler(), metadata, new ParseContext());

        assertEquals("http://alfresco.com", metadata.get(IPTC.CONTACT_INFO_WEB_URL));
        assertTrue(metadata.get(IPTC.COPYRIGHT_NOTICE).contains("Ray Gauss II"));
        assertEquals("iptc subject code here", metadata.get(IPTC.SUBJECT_CODE));
        assertEquals("Rock Creek Park", metadata.get(IPTC.SUBLOCATION));
        assertEquals("job identifier", metadata.get(IPTC.JOB_ID));
        assertEquals("Downstream", metadata.get(IPTC.TITLE));
        assertEquals("100-ABC-ABC-555", metadata.get(IPTC.REGISTRY_ENTRY_CREATED_ITEM_ID));
        assertEquals("RGAUSS", metadata.get(IPTC.IMAGE_SUPPLIER_ID));
        assertEquals("A stream bank in Rock Creek Park Washington DC during a photo bike tour with ASPP DC/South chapter.", metadata.get(IPTC.DESCRIPTION));
        List<String> iptcKeywords = Arrays.asList(metadata.getValues(IPTC.KEYWORDS.getName()));
        assertTrue(iptcKeywords.contains("stream"));
        assertTrue(iptcKeywords.contains("park"));
        assertTrue(iptcKeywords.contains("bank"));

        assertEquals("Downstream", metadata.get(Metadata.TITLE));
        assertEquals("A stream bank in Rock Creek Park Washington DC during a photo bike tour with ASPP DC/South chapter.", metadata.get(Metadata.DESCRIPTION));
        List<String> tikaKeywords = Arrays.asList(metadata.getValues(Metadata.KEYWORDS));
        assertTrue(Arrays.toString(tikaKeywords.toArray()).contains("stream"));
        assertTrue(Arrays.toString(tikaKeywords.toArray()).contains("park"));
        assertTrue(Arrays.toString(tikaKeywords.toArray()).contains("bank"));

        assertTrue(metadata.isMultiValued(IPTC.IMAGE_CREATOR_NAME.getName()));
        List<String> imageCreatorNames = Arrays.asList(metadata.getValues(IPTC.IMAGE_CREATOR_NAME.getName()));
        assertEquals(imageCreatorNames.size(), 2);
        assertTrue(imageCreatorNames.contains("GG"));

        List<String> copyrightOwnerIds = Arrays.asList(metadata.getValues(IPTC.COPYRIGHT_OWNER_ID.getName()));
        assertTrue(copyrightOwnerIds.contains("RGAUSS"));

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2011);
        calendar.set(Calendar.MONTH, 7);
        calendar.set(Calendar.DATE, 31);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals(calendar.getTime(), metadata.getDate(IPTC.DATE_CREATED));
     }
    
    public void testJPEG() throws Exception {
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "image/jpeg");
        InputStream stream =
            getClass().getResourceAsStream("/test-documents/testJPEG.jpg");
        parser.parse(stream, new DefaultHandler(), metadata, new ParseContext());
        
        assertEquals("100", metadata.get(TIFF.IMAGE_WIDTH));
        for (String name : metadata.names()) {
			logger.trace("JPEG-- " + name + "=" + metadata.get(name));
		}
    }
    
    public void testPNGIPTC() throws Exception {
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "image/png");
        InputStream stream =
            getClass().getResourceAsStream("/test-documents/testPNG_IPTC.png");
        parser.parse(stream, new DefaultHandler(), metadata, new ParseContext());

        for (String name : metadata.names()) {
        	logger.trace("PNG-- " + name + "=" + metadata.get(name));
		}
        assertEquals("100", metadata.get(TIFF.IMAGE_WIDTH));
        assertEquals("Cat in a garden", metadata.get(IPTC.HEADLINE));
    }
    
    public void testTIFFIPTC() throws Exception {
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, "image/tiff");
        InputStream stream =
            getClass().getResourceAsStream("/test-documents/testTIFF_IPTC.tif");
        parser.parse(stream, new DefaultHandler(), metadata, new ParseContext());

        for (String name : metadata.names()) {
        	logger.trace("TIFF-- " + name + "=" + metadata.get(name));
		}
        List<String> iptcKeywords = Arrays.asList(metadata.getValues(IPTC.KEYWORDS));
        assertTrue(iptcKeywords.contains("garden"));
        assertTrue(iptcKeywords.contains("cat"));
        assertEquals("Cat in a garden", metadata.get(IPTC.HEADLINE));
    }
}
