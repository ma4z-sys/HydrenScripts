package com.hydren.ht;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import javax.script.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ScriptManager {
    private final Plugin plugin;
    private final Path scriptsDir;
    private final ScriptEngine engine;
    private final Bindings bindings;
    private final HttpClient httpClient;

    public ScriptManager(Plugin plugin, Path scriptsDir) {
        this.plugin = plugin;
        this.scriptsDir = scriptsDir;
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("javascript");
        this.bindings = engine != null ? engine.createBindings() : null;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        try { Files.createDirectories(scriptsDir); } catch (IOException ignored) {}
        if (engine != null) prepareBindings();
    }

    private void prepareBindings() {
        bindings.put("Bukkit", Bukkit.getServer());
        bindings.put("server", Bukkit.getServer());
        bindings.put("plugin", plugin);
        bindings.put("console", Bukkit.getConsoleSender());
        bindings.put("log", (ScriptLogger) args -> {
            plugin.getLogger().info(args != null && args.length > 0 ? String.valueOf(args[0]) : "");
            return null;
        });

        bindings.put("define", (DefineFunction) (name, fn) -> {
            getDefinitions().put(name, fn);
            return null;
        });

        bindings.put("conditions", new ConditionsProxy());

        bindings.put("fetch", (FetchFunction) (url, callback) -> {
            fetchAsync(url, callback);
            return null;
        });

        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    }

    private Map<String, Object> getDefinitions() {
        Object defs = bindings.get("__definitions");
        if (defs == null) {
            defs = new HashMap<String, Object>();
            bindings.put("__definitions", defs);
        }
        return (Map<String, Object>) defs;
    }

    private void fetchAsync(String url, Object callback) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            CompletableFuture<HttpResponse<String>> cf =
                    httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            cf.thenAccept(res -> {
                if (engine instanceof Invocable && callback != null) {
                    try {
                        ((Invocable) engine).invokeMethod(callback, "call", null, res.body(), res.statusCode());
                    } catch (Throwable t) {
                        plugin.getLogger().warning("fetch callback error: " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("fetch error: " + t.getMessage());
        }
    }

    public void loadAll() {
        if (engine == null) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(scriptsDir, "*.ht")) {
            for (Path p : stream) {
                try {
                    String content = Files.readString(p);
                    engine.eval(content);
                    plugin.getLogger().info("Loaded script: " + p.getFileName());
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in script " + p + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Script dir error: " + e.getMessage());
        }
    }

    public void reloadAll() {
        loadAll();
    }

    public void callHandler(String name, Object... args) {
        Object fn = getDefinitions().get(name);
        if (fn == null || !(engine instanceof Invocable)) return;
        try {
            ((Invocable) engine).invokeMethod(fn, "call", null, args);
        } catch (Throwable t) {
            plugin.getLogger().warning("Error calling handler " + name + ": " + t.getMessage());
        }
    }

    public void shutdown() { }

    public interface ScriptLogger { Object call(Object[] args); }
    public interface DefineFunction { Object define(String name, Object fn); }
    public interface FetchFunction { Object fetch(String url, Object callback); }

    public class ConditionsProxy {
        private final Map<String, Object> conditions = new HashMap<>();

        public void register(String name, Object fn) {
            conditions.put(name, fn);
        }

        public boolean check(String name, Object... args) {
            Object fn = conditions.get(name);
            if (fn == null || !(engine instanceof Invocable)) return false;
            try {
                Object res = ((Invocable) engine).invokeMethod(fn, "call", null, args);
                return res instanceof Boolean ? (Boolean) res : false;
            } catch (Throwable t) {
                plugin.getLogger().warning("Condition check error: " + t.getMessage());
                return false;
            }
        }
    }
}
