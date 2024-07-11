package com.example.swig;

import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;

public interface Swig extends Task {
    ConfigurableFileCollection getSources();
    ConfigurableFileCollection getIncludes();
}
