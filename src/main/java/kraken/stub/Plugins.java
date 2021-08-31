package kraken.stub;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kraken.plugin.api.Debug;
import kraken.plugin.api.Kraken;

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

    private static void loadJar(byte[] bytes) throws IOException, ClassNotFoundException {
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry ze;

        String ep = null;
        Map<String, byte[]> typeDefinitions = new HashMap<>();
        while ((ze = zis.getNextEntry()) != null) {
            byte[] b = toByteArray(zis);

            if (ze.getName().equals("plugin.ini")) {
                ep = new String(b)
                        .replace("\n", "")
                        .replace("\r", "")
                        .trim();

            }

            if (ze.getName().endsWith(".class")) {
                typeDefinitions.put(ze.getName().replace('/', '.').replace(".class", ""), b);
            }

            zis.closeEntry();
        }

        if (ep == null) {
            Debug.log("Failed to find entry-point for jar");
            return;
        }

        Debug.log("Loading plugin at @ '" + ep + "'");
        ByteArrayClassLoader bcl = new ByteArrayClassLoader(ClassLoader.getSystemClassLoader(), typeDefinitions);
        Kraken.loadNewPlugin(bcl.loadClass(ep));
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
        loadJar(Files.readAllBytes(path));
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
