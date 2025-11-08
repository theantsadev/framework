package servlet.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClassDetector {

    public static List<Class<?>> getAllClassesFromClasspath() throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL root = classLoader.getResource("");

        if (root == null) {
            throw new IOException("Impossible de trouver le répertoire racine des classes");
        }

        String decodedPath = URLDecoder.decode(root.getPath(), StandardCharsets.UTF_8);
        File baseDir = new File(decodedPath);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new IOException("Le répertoire des classes est invalide : " + baseDir.getAbsolutePath());
        }

        scanDirectory(baseDir, baseDir, classes);
        return classes;
    }

    private static void scanDirectory(File rootDir, File currentDir, List<Class<?>> classes)
            throws ClassNotFoundException {
        File[] files = currentDir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(rootDir, file, classes);
            } else if (file.getName().endsWith(".class")) {
                String path = file.getAbsolutePath()
                        .substring(rootDir.getAbsolutePath().length() + 1)
                        .replace(File.separatorChar, '.')
                        .replaceAll("\\.class$", "");
                try {
                    Class<?> clazz = Class.forName(path);
                    classes.add(clazz);
                } catch (Throwable e) {

                }
            }
        }
    }
}
