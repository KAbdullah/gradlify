package com.example.backend.professor.engine;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A custom in-memory Java file manager used to compile Java source code
 * and load classes directly in memory, without touching the file system.
 *
 * Useful for running test cases against submitted Java code dynamically during runtime
 */
public class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    // Stores compiled bytecode in memory, keyed by class name
    private final Map<String, ByteArrayOutputStream> compiledClasses = new HashMap<>();

    public MemoryJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }


    //Instead of writing .class files to disk, capture them in memory using ByteArrayOutputStream
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                               JavaFileObject.Kind kind, FileObject sibling) {

        // Create a byte stream to hold the compiled class bytecode
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Store the stream so it can retrieve the compiled class later
        compiledClasses.put(className, baos);
        // Return a virtual Java file that writes a to the in-memory stream
        return new SimpleJavaFileObject(URI.create("mem:///" + className + kind.extension), kind) {
            @Override
            public OutputStream openOutputStream() {
                return baos;
            }
        };
    }

    public Map<String, byte[]> getCompiledClasses() {
        Map<String, byte[]> byteMap = new HashMap<>();
        for (Map.Entry<String, ByteArrayOutputStream> entry : compiledClasses.entrySet()) {
            byteMap.put(entry.getKey(), entry.getValue().toByteArray());
        }
        return byteMap;
    }

    /**
     * Return a custom class loader that loads classes from the in-memory bytecode map
     */
    @Override
    public ClassLoader getClassLoader(Location location) {
        return new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                // Look up the compiled bytecode for the given class name
                ByteArrayOutputStream baos = compiledClasses.get(name);
                if (baos == null) throw new ClassNotFoundException(name);

                // Convert the bytecode stream to a byte array adn define the class
                byte[] bytes = baos.toByteArray();
                return defineClass(name, bytes, 0, bytes.length);


            }
        };
    }
}
