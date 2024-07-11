package com.example.swig.sourceparser;

import java.io.IOException;

public final class SwigPreprocessingReader implements PreprocessingReader {
    private final PreprocessingReader delegate;

    public SwigPreprocessingReader(PreprocessingReader delegate) {
        this.delegate = delegate;
    }

    // Ignores raw blocks, e.g. `%{ ... %}`
    public boolean readNextLine(Appendable buffer) throws IOException {
        StringBuilder line = new StringBuilder();
        boolean inRaw = false;
        while (delegate.readNextLine(line)) {
            if (!inRaw && line.substring(0, 2).equals("%{")) {
                inRaw = true;
                line.setLength(0);
            } else if (inRaw && line.substring(0, 2).equals("%}")) {
                inRaw = false;
                line.setLength(0);
            } else if (inRaw) {
                line.setLength(0);
            } else {
                buffer.append(line);
                return true;
            }
        }
        return false;
    }
}
