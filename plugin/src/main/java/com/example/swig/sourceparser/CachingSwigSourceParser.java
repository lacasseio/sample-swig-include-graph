package com.example.swig.sourceparser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CachingSwigSourceParser implements SwigSourceParser {
    private final Map<File, Set<SwigInclude>> cache = new HashMap<>();
    private final SwigSourceParser delegate;

    public CachingSwigSourceParser(SwigSourceParser delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<SwigInclude> parseSource(File sourceFile) {
        return cache.computeIfAbsent(sourceFile, delegate::parseSource);
    }
}
