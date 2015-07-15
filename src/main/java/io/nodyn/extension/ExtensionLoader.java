package io.nodyn.extension;

import io.nodyn.runtime.NodynClassLoader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author Bob McWhirter
 */
public class ExtensionLoader {

    private final NodynClassLoader classLoader;

    public ExtensionLoader(NodynClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Object load(String filename) throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        String extensionName = null;

        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException(filename);
        }

        if (file.isDirectory()) {
            File extensionFile = new File(file, "node.extension");
            if (extensionFile.exists()) {
                FileInputStream in = new FileInputStream(extensionFile);
                try {
                    extensionName = readNodeExtension(in);
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // swallow
                    }
                }
            }
        } else {
            if (filename.endsWith(".jar")) {
                JarFile jarFile = new JarFile(file);
                ZipEntry entry = jarFile.getEntry("node.extension");
                if (entry != null) {
                    InputStream in = jarFile.getInputStream(entry);
                    try {
                        extensionName = readNodeExtension(in);
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // swallow

                        }
                    }
                }
            }
        }

        this.classLoader.append(filename);

        return loadExtension(extensionName);
    }

    private Object loadExtension(String extensionName) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        if (extensionName == null) {
            return null;
        }

        Class<?> extClass = this.classLoader.loadClass(extensionName);

        Method[] methods = extClass.getMethods();

        for (Method method : methods) {
            if (method.getName().equals("require")) {
                if (Modifier.isStatic(method.getModifiers())) {
                    if (method.getParameterTypes().length == 0) {
                        return loadExtension(method);
                    }
                }
            }
        }

        return null;
    }

    private Object loadExtension(Method method) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(null);
    }

    private String readNodeExtension(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("//")) {
                continue;
            }
            if (line.equals("")) {
                continue;
            }
            return line;
        }

        return null;
    }
}
