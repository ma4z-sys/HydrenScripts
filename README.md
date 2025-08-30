# HydrenScripts (HT)

HydrenScripts lets you write **custom scripts** in a simple JavaScript-like syntax to control your server.
Create `.ht` files inside the `scripts/` folder and they will run automatically.

---

## 📥 Installation

1. Download `HydrenScripts-1.x.x.jar`.
2. Place it into your server’s `plugins/` folder.
3. Start or restart your server.
4. A folder will be created:

   ```
   plugins/HydrenScripts/scripts/
   ```
5. Put your `.ht` scripts into this folder.
6. Use `/htreload` to reload scripts without restarting.

---

## ⚙️ Configuration

`config.yml` will be generated in `plugins/HydrenScripts/`.

```yaml
# Reload all scripts on server start
reload-on-start: true

# Timeout (in seconds) for fetch() web requests
fetch-timeout-seconds: 10
```

---

## 📝 Example Script

Create a file in `plugins/HydrenScripts/scripts/example.ht`:

```javascript
define("onPlayerJoin", function(player) {
    player.sendMessage("§aWelcome, " + player.getName() + "! This is from HydrenScripts.");
});

define("onPlayerQuit", function(player) {
    console.sendMessage(player.getName() + " left the server.");
});

conditions.register("isNight", function() {
    var world = Bukkit.getWorlds()[0];
    return world.getTime() > 13000;
});

fetch("https://httpbin.org/get", function(body, status) {
    log("fetch status=" + status);
    log(body.substring(0, 80) + "...");
});
```

---

## 📖 Commands

* `/htreload` → Reload all scripts.

---

## ✅ Requirements

* Java 17+
* Spigot / Paper 1.21+