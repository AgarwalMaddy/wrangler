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
 * A token that represents a time duration value (e.g., "100ms", "2.5s").
 * This class parses time duration strings and converts them to a canonical value in milliseconds.
 * Supported units:
 * - ms (milliseconds)
 * - s (seconds, 1000 milliseconds)
 * - m (minutes, 60 * 1000 milliseconds)
 * - h (hours, 60 * 60 * 1000 milliseconds)
 */
public class TimeDuration implements Token {
    private final String original;
    private final long milliseconds;

    /**
     * Creates a new TimeDuration instance by parsing the given string.
     * The string should be in the format "number[unit]" where unit is one of ms, s, m, or h.
     * The number can be an integer or a decimal.
     *
     * @param value the string to parse (e.g., "100ms", "2.5s")
     * @throws IllegalArgumentException if the string cannot be parsed
     */
    public TimeDuration(String value) {
        this.original = value;
        this.milliseconds = parseMilliseconds(value);
    }

    private long parseMilliseconds(String value) {
        value = value.trim().toLowerCase();
        if (value.endsWith("ns")) {
            return Long.parseLong(value.replace("ns", "")) / 1_000_000;
        }
        if (value.endsWith("ms")) {
            return (long) Double.parseDouble(value.replace("ms", ""));
        }
        if (value.endsWith("s")) {
            return (long) (Double.parseDouble(value.replace("s", "")) * 1000);
        }
        if (value.endsWith("m")) {
            return (long) (Double.parseDouble(value.replace("m", "")) * 60 * 1000);
        }
        if (value.endsWith("h")) {
            return (long) (Double.parseDouble(value.replace("h", "")) * 60 * 60 * 1000);
        }
        throw new IllegalArgumentException("Invalid time duration unit: " + value);
    }

    /**
     * Returns the duration in milliseconds.
     *
     * @return the duration in milliseconds
     */
    public long getMilliseconds() {
        return milliseconds;
    }

    @Override
    public Object value() {
        return milliseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("original", original);
        json.addProperty("milliseconds", milliseconds);
        return json;
    }
}
