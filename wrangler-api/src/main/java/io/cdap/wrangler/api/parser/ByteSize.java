/*
 * Copyright © 2025 Khushi Agarwal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A token that represents a byte size value (e.g., "10KB", "1.5MB").
 * This class parses byte size strings and converts them to a canonical value in bytes.
 * Supported units:
 * - B (bytes)
 * - KB (kilobytes, 1024 bytes)
 * - MB (megabytes, 1024 * 1024 bytes)
 * - GB (gigabytes, 1024 * 1024 * 1024 bytes)
 * - TB (terabytes, 1024 * 1024 * 1024 * 1024 bytes)
 */
public class ByteSize implements Token {
    private final String original;
    private final long bytes;

    /**
     * Creates a new ByteSize instance by parsing the given string.
     * The string should be in the format "number[unit]" where unit is one of B, KB, MB, GB, or TB.
     * The number can be an integer or a decimal.
     *
     * @param value the string to parse (e.g., "10KB", "1.5MB")
     * @throws IllegalArgumentException if the string cannot be parsed
     */
    public ByteSize(String value) {
        this.original = value;
        this.bytes = parseBytes(value);
    }

    private long parseBytes(String value) {
        value = value.trim().toUpperCase();
        if (value.endsWith("KB")) {
            return (long) (Double.parseDouble(value.replace("KB", "")) * 1024);
        }
        if (value.endsWith("MB")) {
            return (long) (Double.parseDouble(value.replace("MB", "")) * 1024 * 1024);
        }
        if (value.endsWith("GB")) {
            return (long) (Double.parseDouble(value.replace("GB", "")) * 1024 * 1024 * 1024);
        }
        if (value.endsWith("TB")) {
            return (long) (
                    Double.parseDouble(value.replace("TB", "")) * 1024L * 1024 * 1024 * 1024
            );
        }
        if (value.endsWith("B")) {
            return Long.parseLong(value.replace("B", ""));
        }
        throw new IllegalArgumentException("Invalid byte size unit: " + value);
    }

    /**
     * Returns the size in bytes.
     *
     * @return the size in bytes
     */
    public long getBytes() {
        return bytes;
    }

    @Override
    public Object value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("original", original);
        json.addProperty("bytes", bytes);
        return json;
    }
}
