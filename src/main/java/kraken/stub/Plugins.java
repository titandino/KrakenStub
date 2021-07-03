package kraken.stub;

import kraken.plugin.api.Client;
import kraken.plugin.api.Debug;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Provides plugin loading utilities.
 */
public class Plugins {

    private Plugins() { }

    private static byte[] toByteArray(InputStream ins) throws IOException {
        byte[] tmp = new byte[4096];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int read;
        while ((read = ins.read(tmp)) != -1) {
            bos.write(tmp, 0, read);
        }
        return bos.toByteArray();
    }

    /**
     * Loads a jar into the current classpath. We have limited control here over
     * security options, etc. but this does not matter as we will provide our
     * security at the native level.
     *
     * This function will add the jar to the classpath, and then it will resolve
     * the plugin entry-point. If this entry-point is found, then it will be
     * registered as a plugin with Kraken.
     *
     * @param path The path to the jar file to load into our classpath.
     * @throws Exception If any exceptions occur during loading.
     */
    public static void loadJar(Path path) throws Exception {
        File file = path.toFile();
        URL url = file.toURI().toURL();

        URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(classLoader, url);

        JarFile jf = new JarFile(file);
        Enumeration<JarEntry> je = jf.entries();
        while (je.hasMoreElements()) {
            JarEntry entry = je.nextElement();
            Debug.log(entry.getName());
            if (entry.getName().equals("plugin.ini")) {
                InputStream ins = jf.getInputStream(entry);
                String ep = new String(toByteArray(ins))
                        .replace("\n", "")
                        .replace("\r", "")
                        .trim();

                ins.close();

                Debug.log("Loading plugin at '" + ep + "'");
                Client.loadNewPlugin(Class.forName(ep));
            }
        }
    }

    /**
     * Loads all jars in a given directory. See Plugins#loadJar for more information.
     */
    public static void loadJars(Path dir) throws Exception {
        for (Path p : Files.list(dir).toArray(Path[]::new)) {
            Debug.log("Checking jar " + p.getFileName());
            if (!p.getFileName().toString().equals("KrakenStub-1.0-SNAPSHOT.jar")) {
                loadJar(p);
            }
        }
    }

}