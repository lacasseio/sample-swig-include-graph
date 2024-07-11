package com.example.swig.sourceparser;

import org.gradle.api.GradleException;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RegexBackedSwigSourceParser implements SwigSourceParser {
    public Set<SwigInclude> parseSource(File sourceFile) {
        try (Reader fileReader = new FileReader(sourceFile)) {
            return parseSource(fileReader);
        } catch (
                Exception e) {
            throw new GradleException(String.format("Could not extract includes from source file %s.", sourceFile), e);
        }
    }

    private Set<SwigInclude> parseSource(Reader sourceReader) throws IOException {
        Set<SwigInclude> includes = new LinkedHashSet<>();
        BufferedReader reader = new BufferedReader(sourceReader);
        PreprocessingReader lineReader = new SwigPreprocessingReader(new StripCStyleCommentsPreprocessingReader(reader));
        Buffer buffer = new Buffer();
        while (true) {
            buffer.reset();
            if (!lineReader.readNextLine(buffer.value)) {
                break;
            }
            buffer.consumeWhitespace();
            if (!buffer.consume('#') && !buffer.consume('%')) {
                continue;
            }
            buffer.consumeWhitespace();
            if (buffer.consume("include") || buffer.consume("import")) {
                parseIncludeOrImportDirectiveBody(buffer, includes);
            }
        }
        return includes;
    }

    private void parseIncludeOrImportDirectiveBody(Buffer buffer, Collection<SwigInclude> includes) {
        if (buffer.hasAny()) {
            buffer.consumeWhitespace();
            if (buffer.consume('"')) {
                String path = readDelimitedExpression(buffer, '"');
                if (path != null) {
                    includes.add(new SwigInclude(path));
                }
            }
        }
    }

    @Nullable
    private static String readDelimitedExpression(Buffer buffer, char endDelim) {
        int startValue = buffer.pos;
        buffer.consumeUpTo(endDelim);
        int endValue = buffer.pos;
        return !buffer.consume(endDelim) ? null : buffer.substring(startValue, endValue);
    }

    private static int consumeWhitespace(CharSequence value, int startOffset) {
        int pos;
        for (pos = startOffset; pos < value.length(); ++pos) {
            char ch = value.charAt(pos);
            if (!Character.isWhitespace(ch) && ch != 0) {
                break;
            }
        }

        return pos;
    }

    private static class Buffer {
        final StringBuilder value = new StringBuilder();
        int pos = 0;

        @Override
        public String toString() {
            return "{buffer remaining: '" + value.substring(pos, pos + Math.min(value.length() - pos, 20)) + "'}";
        }

        void reset() {
            value.setLength(0);
            pos = 0;
        }

        /**
         * Returns text from the specified location to the end of the buffer.
         */
        String substring(int start, int end) {
            return value.substring(start, end);
        }

        /**
         * Is there another character available? Does not consume the character.
         */
        boolean hasAny() {
            return pos < value.length();
        }

        /**
         * Skip any whitespace at the current location.
         *
         * @return true if skipped, false if not.
         */
        boolean consumeWhitespace() {
            int oldPos = pos;
            pos = RegexBackedSwigSourceParser.consumeWhitespace(value, pos);
            return pos != oldPos;
        }

        /**
         * Skip the given string if present at the current location.
         *
         * @return true if skipped, false if not.
         */
        boolean consume(String token) {
            if (pos + token.length() < value.length()) {
                for (int i = 0; i < token.length(); i++) {
                    if (value.charAt(pos + i) != token.charAt(i)) {
                        return false;
                    }
                }
                pos += token.length();
                return true;
            }
            return false;
        }

        /**
         * Skip the given character if present at the current location.
         *
         * @return true if skipped, false if not.
         */
        boolean consume(char c) {
            if (pos < value.length() && value.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        /**
         * Skip characters up to the given character. Does not consume the character.
         */
        void consumeUpTo(char c) {
            while (pos < value.length() && value.charAt(pos) != c) {
                pos++;
            }
        }
    }
}
