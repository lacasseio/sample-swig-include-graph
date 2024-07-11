package com.example.swig.sourceparser;

import java.io.IOException;

public interface PreprocessingReader {
    boolean readNextLine(Appendable buffer) throws IOException;
}
