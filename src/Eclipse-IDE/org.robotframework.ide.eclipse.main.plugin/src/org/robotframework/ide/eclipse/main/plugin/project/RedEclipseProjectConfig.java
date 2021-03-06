/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.rf.ide.core.EnvironmentVariableReplacer;
import org.rf.ide.core.RedURI;
import org.rf.ide.core.SystemVariableAccessor;
import org.rf.ide.core.environment.EnvironmentSearchPaths;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.LibraryType;
import org.rf.ide.core.project.RobotProjectConfig.RelativeTo;
import org.rf.ide.core.project.RobotProjectConfig.RelativityPoint;
import org.rf.ide.core.project.RobotProjectConfig.SearchPath;
import org.robotframework.ide.eclipse.main.plugin.RedWorkspace.Paths;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.escape.Escaper;

public class RedEclipseProjectConfig {

    private final IProject project;

    private final RobotProjectConfig config;

    private final SystemVariableAccessor variableAccessor;

    public RedEclipseProjectConfig(final IProject project, final RobotProjectConfig config) {
        this(project, config, new SystemVariableAccessor());
    }

    @VisibleForTesting
    RedEclipseProjectConfig(final IProject project, final RobotProjectConfig config,
            final SystemVariableAccessor variableAccessor) {
        this.project = project;
        this.config = config;
        this.variableAccessor = variableAccessor;
    }

    public Optional<File> toAbsolutePath(final IPath path) {
        if (path.isAbsolute()) {
            return Optional.of(path.toFile());
        }
        final Optional<File> wsAbsolute = resolveToAbsolutePath(getRelativityLocation(config.getRelativityPoint()),
                path).map(IPath::toFile);
        return wsAbsolute.map(file -> {
            final IPath wsRelative = Paths.toWorkspaceRelativeIfPossible(new Path(file.getAbsolutePath()));
            final IResource member = project.getWorkspace().getRoot().findMember(wsRelative);
            if (member == null) {
                return file;
            } else {
                return member.getLocation().toFile();
            }
        });
    }

    @VisibleForTesting
    static Optional<IPath> resolveToAbsolutePath(final IPath base, final IPath child) {
        if (child.isAbsolute()) {
            return Optional.of(child);
        } else {
            try {
                final Escaper escaper = RedURI.URI_SPECIAL_CHARS_ESCAPER;

                final String portablePath = base.toPortableString();
                final URI filePath = new URI(escaper.escape(portablePath));
                final URI pathUri = filePath.resolve(escaper.escape(child.toString()));
                return Optional.of(new Path(RedURI.reverseUriSpecialCharsEscapes(pathUri.toString())));
            } catch (final Exception e) {
                return Optional.empty();
            }
        }
    }

    private IPath getRelativityLocation(final RelativityPoint relativityPoint) {
        final IPath result = relativityPoint.getRelativeTo() == RelativeTo.WORKSPACE
                ? project.getWorkspace().getRoot().getLocation()
                : project.getLocation();
        return result.addTrailingSeparator();
    }

    public EnvironmentSearchPaths createAdditionalEnvironmentSearchPaths() {
        return new EnvironmentSearchPaths(getResolvedPaths(config.getClassPaths()),
                getResolvedPaths(config.getPythonPaths()));
    }

    public EnvironmentSearchPaths createExecutionEnvironmentSearchPaths() {
        final List<String> classPaths = new ArrayList<>();
        classPaths.add(".");
        classPaths.addAll(getReferenceLibPaths(LibraryType.JAVA));
        classPaths.addAll(getResolvedPaths(config.getClassPaths()));
        final List<String> pythonPaths = new ArrayList<>();
        pythonPaths.addAll(getReferenceLibPaths(LibraryType.PYTHON));
        pythonPaths.addAll(getResolvedPaths(config.getPythonPaths()));
        return new EnvironmentSearchPaths(classPaths, pythonPaths);
    }

    private List<String> getResolvedPaths(final List<SearchPath> paths) {
        final EnvironmentVariableReplacer variableReplacer = new EnvironmentVariableReplacer(variableAccessor);
        return paths.stream()
                .map(SearchPath::getLocation)
                .map(variableReplacer::replaceKnownEnvironmentVariables)
                .map(Path::new)
                .map(this::toAbsolutePath)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(File::getPath)
                .collect(toList());
    }

    private List<String> getReferenceLibPaths(final LibraryType libType) {
        return config.getReferencedLibraries()
                .stream()
                .filter(lib -> lib.provideType() == libType)
                .map(lib -> Paths.toAbsoluteFromWorkspaceRelativeIfPossible(new Path(lib.getPath())).toOSString())
                .collect(toList());
    }
}
