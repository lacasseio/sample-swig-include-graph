package com.example.swig;

import com.example.swig.sourceparser.CachingSwigSourceParser;
import com.example.swig.sourceparser.RegexBackedSwigSourceParser;
import com.example.swig.sourceparser.SwigInclude;
import com.example.swig.sourceparser.SwigSourceParser;
import org.gradle.api.Transformer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask;
import org.gradle.nativeplatform.toolchain.internal.NativeCompileSpec;
import org.gradle.work.InputChanges;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

// Note: anything exposed by AbstractNativeCompileTask should be deemed as an implementation detail.
// Note: gradle/gradle#29492 does not affect this task implementation, no need to work around it.
public abstract class SwigTask extends AbstractNativeCompileTask implements Swig {
    @Internal
    protected abstract RegularFileProperty getCppSourceFile();

    @Internal
    protected abstract Property<SwigParseResult> getSwigParseResult();

    @InputFiles
    protected abstract ConfigurableFileCollection getSwigDependencies();

    @Inject
    public SwigTask(ProviderFactory providers) {
        getObjectFileDir().fileProvider(providers.provider(getTemporaryDirFactory()::create).map(it -> new File(it, "objs"))).disallowChanges();

        // Parse all Swig files
        getSwigParseResult().set(getSources().getElements().map(toFileSet()).map(new Transformer<>() {
            private final SwigSourceParser sourceParser = new CachingSwigSourceParser(new RegexBackedSwigSourceParser());

            @Override
            public SwigParseResult transform(Set<File> files) {
                Set<File> visited = new HashSet<>();
                Deque<File> queue = new ArrayDeque<>(files);
                Set<String> headers = new LinkedHashSet<>();
                IncludeResolver resolver = new IncludeResolver(getIncludes().getFiles());
                while (!queue.isEmpty()) {
                    File file = queue.pop();
                    if (visited.add(file)) {
                        for (SwigInclude include : sourceParser.parseSource(file)) {
                            if (include.getType() == SwigInclude.IncludeType.SWIG) {
                                File candidate = resolver.resolveInclude(file, include);
                                if (candidate != null) {
                                    queue.add(candidate);
                                }
                            } else {
                                headers.add(include.getPath());
                            }
                        }
                    }
                }
                return new SwigParseResult(headers, visited);
            }
        }));
        getSwigParseResult().disallowChanges();
        getSwigParseResult().finalizeValueOnRead();

        // Generate dummy source file to use Gradle header parsing
        getCppSourceFile().fileProvider(getSwigParseResult().map(info -> {
            List<String> lines = info.getIncludePaths().stream().map(path -> "#include \"" + path + "\"").collect(Collectors.toList());
            File source = new File(getTemporaryDir(), "source.cpp");
            try {
                Files.write(source.toPath(), lines, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return source;
        }));
        getCppSourceFile().disallowChanges();
        getCppSourceFile().finalizeValueOnRead();

        getSource().from(getCppSourceFile());

        getSwigDependencies().from(getSwigParseResult().map(SwigParseResult::getSwigIncludeFiles));
    }

    private static final class IncludeResolver {
        private final Collection<File> searchPaths;

        private IncludeResolver(Collection<File> searchPaths) {
            this.searchPaths = searchPaths;
        }

        @Nullable
        public File resolveInclude(File sourceFile, SwigInclude include) {
            // TODO: use `sourceFile` if %import/%include/#include can resolve relative to source file
            for (File searchPath : searchPaths) {
                File candidate = new File(searchPath, include.getPath());
                if (candidate.exists()) {
                    return candidate;
                }
            }
            return null;
        }
    }

    public static final class SwigParseResult {
        private final Collection<String> includePaths;
        private final Collection<File> swigIncludeFiles;

        private SwigParseResult(Collection<String> includePaths, Collection<File> swigIncludeFiles) {
            this.includePaths = includePaths;
            this.swigIncludeFiles = swigIncludeFiles;
        }

        public Collection<String> getIncludePaths() {
            return includePaths;
        }

        public Collection<File> getSwigIncludeFiles() {
            return swigIncludeFiles;
        }
    }

    private Transformer<Set<File>, Set<FileSystemLocation>> toFileSet() {
        return it -> {
            Set<File> result = new TreeSet<>();
            for (FileSystemLocation location : it) {
                result.add(location.getAsFile());
            }
            return result;
        };
    }

    @Override
    protected NativeCompileSpec createCompileSpec() {
        throw new UnsupportedOperationException("do not call");
    }

    @Override
    protected void compile(InputChanges inputs) {
        throw new UnsupportedOperationException("do not call");
    }

    @TaskAction
    protected void compile() {
        for (File file : getSwigDependencies().plus(getHeaderDependencies()).getFiles()) {
            System.out.println(file);
        }
    }

    @InputFiles
    @SkipWhenEmpty
    public abstract ConfigurableFileCollection getSources();

    @Internal
    @Deprecated // Use `sources` property instead
    public ConfigurableFileCollection getSource() {
        return super.getSource();
    }
}
