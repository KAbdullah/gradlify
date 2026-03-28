package com.example.backend.professor.engine;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * Represents a Java source file stored entirely in memory
 * Used to provide source code to the Java Compiler API without creating actual files
 */
public class MemoryJavaSourceFileObject extends SimpleJavaFileObject {

    // The Java source code to be compiled, stored as a string
    final String code;

    public MemoryJavaSourceFileObject(String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    // Returns the source code content when requested by compiler
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
