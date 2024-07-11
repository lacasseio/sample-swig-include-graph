package com.example.swig.sourceparser;

import java.io.File;
import java.util.Set;

public interface SwigSourceParser {
    Set<SwigInclude> parseSource(File sourceFile);
}
