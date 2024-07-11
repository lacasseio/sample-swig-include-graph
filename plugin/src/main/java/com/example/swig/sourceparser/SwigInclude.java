package com.example.swig.sourceparser;

public final class SwigInclude {
    private final String path;

    public enum IncludeType {
        SWIG, C
    }

    public SwigInclude(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public IncludeType getType() {
        return path.endsWith(".i") ? IncludeType.SWIG : IncludeType.C;
    }

    @Override
    public String toString() {
        return "include/import \"" + path + "\"";
    }
}
