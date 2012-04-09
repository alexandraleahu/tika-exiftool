package org.apache.tika.parser.exiftool;

import java.util.List;
import java.util.Map;

import org.apache.tika.metadata.Property;

public interface ExiftoolTikaMapper {
	
    /**
     * Gets a map of Tika metadata names to an array of ExifTool metadata names. Most
     * useful for constructing command line arguments.
     *
     * Multiple ExifTool metadata names are provided since it is commonplace to write the
     * same general, Tika metadata value to several metadata fields.  For example,
     * a copyright notice Tika field might be written to EXIF, legacy IPTC, and
     * XMP.
     *
     * @return the map of Tika metadata names to ExifTool names
     */
	public Map<Object, List<Property>> getTikaToExiftoolMetadataMap();
	
	/**
	 * Gets a map of ExifTool metadata names to a single Tika metadata name. Most
	 * useful for parsers.
	 *
	 * @return the map of ExifTool metadata names to Tika names
	 */
	public Map<Property, List<Object>> getExiftoolToTikaMetadataMap();

}