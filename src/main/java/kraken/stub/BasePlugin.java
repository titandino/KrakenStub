package kraken.stub;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import com.darkan.api.LoopScript;
import com.darkan.api.Script;
import com.darkan.api.entity.MyPlayer;
import com.darkan.api.entity.VarManager;
import com.darkan.api.inter.Interfaces;
import com.darkan.api.inter.chat.Chatbox;
import com.darkan.api.inter.chat.Message;
import com.darkan.api.listeners.MessageListener;
import com.darkan.api.util.Logger;
import com.darkan.api.util.Utils;
import com.darkan.thread.DataUpdateThread;
import com.darkan.thread.DataUpdateThreadFactory;
import com.sun.tools.attach.VirtualMachine;

import kraken.plugin.api.ConVar;
import kraken.plugin.api.Debug;
import kraken.plugin.api.ImGui;
import kraken.plugin.api.Kraken;
import kraken.plugin.api.PluginContext;

public final class BasePlugin extends AbstractPlugin {

	private static ScheduledExecutorService dataUpdateExecutor = Executors.newSingleThreadScheduledExecutor(new DataUpdateThreadFactory());

	private List<String> orderedNames = new ArrayList<>();
	private Map<String, Class<? extends LoopScript>> scriptTypes = new HashMap<>();
	private Map<Class<? extends LoopScript>, LoopScript> scripts = new HashMap<>();

	private List<String> prevChats = new ArrayList<>();

	public boolean onLoaded(PluginContext pluginContext) {
		pluginContext.setName("Local Scripts");
		attachAgent();
		loadScripts();
		VarManager.linkVarbits();
		dataUpdateExecutor.scheduleAtFixedRate(new DataUpdateThread(), 0, 50, TimeUnit.MILLISECONDS);
		return true;
	}

	@SuppressWarnings("unchecked")
	private void loadScripts() {
		try {
			for (Path p : Files.list(Paths.get(Kraken.getPluginDir())).toArray(Path[]::new)) {
				if (p.getFileName().toString().contains("KrakenStub"))
					continue;
				JarLoader.addToClassPath(p.toFile());
				Debug.log("Searching for @Script annotations in " + p.getFileName().toString());
				List<Class<?>> classes = Utils.getClassesWithAnnotation(getClassLoader(p), null, Script.class);
				for (Class<?> clazz : classes) {
					Debug.log("Searching class: " + clazz.getName());
					scriptTypes.put(clazz.getAnnotationsByType(Script.class)[0].value(), (Class<? extends LoopScript>) clazz);
				}
				orderedNames.addAll(scriptTypes.keySet());
			}
			Debug.log("Parsed scripts: " + orderedNames.toString());
			Collections.sort(orderedNames);
		} catch (Exception e) {
			Debug.log("Failed to load scripts: " + e.getMessage());
			Logger.handle(e);
		}
	}
	
	public static void attachAgent() {
		try {
			String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
			String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
			VirtualMachine vm = VirtualMachine.attach(pid);
			vm.loadAgent(JarLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "");
			vm.detach();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private URLClassLoader loadJarFile(Path path) throws Exception {
		return new URLClassLoader(new URL[] { path.toFile().toURI().toURL() });
	}

	private ByteArrayClassLoader getClassLoader(Path path) throws Exception {
		 File file = path.toFile();
	        Map<String, byte[]> typeDefinitions = new HashMap<>();
	        
	        JarFile jf = new JarFile(file);
	        Enumeration<JarEntry> je = jf.entries();
	        while (je.hasMoreElements()) {
	            JarEntry entry = je.nextElement();
	            InputStream ins = jf.getInputStream(entry);
	            byte[] b = toByteArray(ins);
	            ins.close();
	            if (entry.getName().endsWith(".class")) {
	            	String className = entry.getName().replace('/', '.').replace(".class", "");
	            	Debug.log("Class loading: " + className);
	                typeDefinitions.put(className, b);
	            }
	    
	        }
	        jf.close();
	        return new ByteArrayClassLoader(Thread.currentThread().getContextClassLoader(), typeDefinitions);
	}
	

    private static byte[] toByteArray(InputStream ins) throws IOException {
        byte[] tmp = new byte[4096];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int read;
        while ((read = ins.read(tmp)) != -1) {
            bos.write(tmp, 0, read);
        }
        return bos.toByteArray();
    }

	public int onLoop() {
		try {
			for (LoopScript script : scripts.values()) {
				if (script != null)
					script.process();
			}
			List<String> currentMessages = Chatbox.getMessages();
			String prevFirst = prevChats.size() > 0 ? prevChats.get(0) : "null";
			List<Message> newMessages = new ArrayList<>();
			for (String chat : currentMessages) {
				if (chat == null)
					continue;
				if (chat.equals(prevFirst))
					break;
				Message mes = new Message(chat.replace("<br>", ""));
				newMessages.add(mes);
				System.out.println("[CHAT]: " + mes);
			}
			if (!newMessages.isEmpty()) {
				for (LoopScript script : scripts.values()) {
					if (script != null && script instanceof MessageListener) {
						for (Message chat : newMessages) {
							try {
								((MessageListener) script).onMessageReceived(chat);
							} catch (Exception e) {
								Logger.handle(e);
							}
						}
					}
				}
			}
			prevChats.clear();
			prevChats.addAll(currentMessages);
			return 36;
		} catch (Exception e) {
			Logger.handle(e);
			return 0;
		}
	}

	public void onPaint() {
		try {
			ImGui.label("Please select a script:");
			for (String scriptName : orderedNames) {
				Class<? extends LoopScript> script = scriptTypes.get(scriptName);
				boolean currRunning = scripts.get(script) != null;
				boolean running = ImGui.checkbox(scriptName, currRunning);
				if (currRunning && !running) {
					scripts.get(script).stop();
					scripts.remove(script);
				} else if (!currRunning && running) {
					try {
						scripts.put(script, script.getDeclaredConstructor().newInstance());
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
						Debug.log("Error constructing script: " + script.getSimpleName());
						e.printStackTrace();
					}
				}
			}

			for (LoopScript script : scripts.values()) {
				if (script != null)
					script.onPaint();
			}
		} catch (Exception e) {
			Logger.handle(e);
		}
	}

	public void onPaintOverlay() {
		try {
			for (LoopScript script : scripts.values()) {
				if (script != null)
					script.onPaintOverlay();
			}
		} catch (Exception e) {
			Logger.handle(e);
		}
	}

	public void onConVarChanged(ConVar conv, int oldValue, int newValue) {
		if (conv.getId() != 3513)
			System.out.println("Var changed: " + conv.getId() + " from " + oldValue + " -> " + newValue);
		MyPlayer.getVars().setVar(conv.getId(), newValue);
	}

	public void onWidgetVisibilityChanged(int id, boolean visible) {
		System.out.println("Interface visibility: " + id + " -> " + visible);
		Interfaces.setVisibility(id, visible);
	}
}
