package com.github.carlkuesters.swagger.reflection;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotatedClassService {

    private String sourcePackage;
    private Reflections reflections;

    public void initialize(MavenProject mavenProject, String sourcePackage) throws MojoExecutionException {
        initializeClassLoader(mavenProject);
        createReflections(sourcePackage);
    }

    private void initializeClassLoader(MavenProject mavenProject) throws MojoExecutionException {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        try {
            Set<String> dependencies = getDependentClasspathElements(mavenProject);
            URL[] urls = new URL[dependencies.size()];
            int index = 0;
            for (String dependency : dependencies) {
                urls[index++] = Paths.get(dependency).toUri().toURL();
            }
            URLClassLoader urlClassLoader = new URLClassLoader(urls, parent);
            Thread.currentThread().setContextClassLoader(urlClassLoader);
        } catch (MalformedURLException ex) {
            throw new MojoExecutionException("Unable to create class loader with compiled classes", ex);
        } catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Dependency resolution (runtime + compile) is required");
        }
    }

    private Set<String> getDependentClasspathElements(MavenProject mavenProject) throws DependencyResolutionRequiredException {
        Set<String> dependencies = new LinkedHashSet<>();
        dependencies.add(mavenProject.getBuild().getOutputDirectory());
        List<String> compileClasspathElements = mavenProject.getCompileClasspathElements();
        if (compileClasspathElements != null) {
            dependencies.addAll(compileClasspathElements);
        }
        List<String> runtimeClasspathElements = mavenProject.getRuntimeClasspathElements();
        if (runtimeClasspathElements != null) {
            dependencies.addAll(runtimeClasspathElements);
        }
        return dependencies;
    }

    private void createReflections(String sourcePackage) {
        this.sourcePackage = sourcePackage;
        ConfigurationBuilder config = ConfigurationBuilder
                .build(sourcePackage)
                .setScanners(new ResourcesScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());
        reflections = new Reflections(config);
    }

    public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        return reflections.getTypesAnnotatedWith(annotationClass)
                .stream()
                .filter(this::filterClassByResourcePackages)
                .collect(Collectors.toSet());
    }

    private boolean filterClassByResourcePackages(Class<?> classWithAnnotation) {
        return ((sourcePackage == null) || classWithAnnotation.getPackage().getName().startsWith(sourcePackage));
    }
}
