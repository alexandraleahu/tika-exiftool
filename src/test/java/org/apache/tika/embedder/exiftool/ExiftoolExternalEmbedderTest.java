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
package org.apache.tika.embedder.exiftool;

import java.io.InputStream;

import org.apache.tika.embedder.Embedder;
import org.apache.tika.embedder.ExternalEmbedderTest;
import org.apache.tika.embedder.exiftool.ExiftoolExternalEmbedder;
import org.apache.tika.metadata.IPTC;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.exiftool.ExiftoolImageParser;
import org.apache.tika.parser.exiftool.ExiftoolTikaIptcMapper;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit test for the ExiftoolExternalEmbedder
 * 
 * @author rgauss
 * 
 */
public class ExiftoolExternalEmbedderTest extends ExternalEmbedderTest {

    private static final String TEST_IMAGE_PATH = "/test-documents/testJPEG_IPTC_EXT.jpg";

    /**
     * Create the test case
     * 
     * @param testName
     *            name of the test case
     */
    public ExiftoolExternalEmbedderTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ExiftoolExternalEmbedderTest.class);
    }

    @Override
    protected org.apache.tika.metadata.Metadata getMetadataToEmbed() {
        Metadata metadataToEmbed = new Metadata();

        metadataToEmbed.add(IPTC.COPYRIGHT_NOTICE.getName(),
                getExpectedMetadataValueString(IPTC.COPYRIGHT_NOTICE.getName()));
        metadataToEmbed.add(IPTC.DESCRIPTION.getName(),
                getExpectedMetadataValueString(IPTC.DESCRIPTION.getName()));
        metadataToEmbed.add(IPTC.CREDIT_LINE.getName(),
                getExpectedMetadataValueString(IPTC.CREDIT_LINE.getName()));
        metadataToEmbed.add(IPTC.KEYWORDS.getName(),
                EXPECTED_METADATA_PREFIX + "keyword1");
        metadataToEmbed.add(IPTC.KEYWORDS.getName(),
                EXPECTED_METADATA_PREFIX + "keyword2");
        metadataToEmbed.add(IPTC.CONTACT_INFO_EMAIL.getName(),
                getExpectedMetadataValueString(IPTC.CONTACT_INFO_EMAIL.getName()));
        metadataToEmbed.add(IPTC.ARTWORK_OR_OBJECT_DETAIL_TITLE.getName(),
                getExpectedMetadataValueString(IPTC.ARTWORK_OR_OBJECT_DETAIL_TITLE.getName()));

        return metadataToEmbed;
    }

    @Override
    protected Embedder getEmbedder() {
        return new ExiftoolExternalEmbedder(new ExiftoolTikaIptcMapper());
    }

    @Override
    protected InputStream getOriginalInputStream() {
        return this.getClass().getResourceAsStream(TEST_IMAGE_PATH);
    }

    @Override
    protected Parser getParser() {
        return new ExiftoolImageParser();
    }

    @Override
    protected boolean getIsMetadataExpectedInOutput() {
        return false;
    }

}
