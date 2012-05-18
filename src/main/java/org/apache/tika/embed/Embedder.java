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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Set;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;

/**
 * Tika embedder interface
 *
 * @author rgauss
 *
 */
public interface Embedder extends Serializable {

    /**
     * Returns the set of media types supported by this embedder when used
     * with the given parse context.
     *
     * @param context parse context
     * @return immutable set of media types
     */
    Set<MediaType> getSupportedTypes(ParseContext context);

    /**
     * Embeds related document metadata from the given metadata object into the given output stream.
     * <p>
     * The given document stream is consumed but not closed by this method.
     * The responsibility to close the stream remains on the caller.
     * <p>
     * Information about the parsing context can be passed in the context
     * parameter. See the parser implementations for the kinds of context
     * information they expect.
     *
     * @param originalStream the document stream (input)
     * @param metadata document metadata (input and output)
     * @param context parse context
     * @return the document stream (input) after metadata has been embedded
     * @throws IOException if the document stream could not be read
     * @throws TikaException if the document could not be parsed
     */
    /**
     * @param originalStream
     * @param metadata
     * @param context
     * @return the metadata embedded document stream
     * @throws IOException
     * @throws TikaException
     */
    InputStream embed(
            InputStream originalStream,
            Metadata metadata, ParseContext context)
            throws IOException, TikaException;

}
