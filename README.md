**Русский язык:**  
[🇷🇺 Русский](README_ru.md)

# JsScripts - API Docs
### JavaScript Scripting Mod for Game Events · Fabric 1.21.8-26.1

JsScripts is a powerful and flexible tool for your server, allowing you to create lore events, automation, and simple script logic using JavaScript.  
You can write code **directly in the game** using the built‑in JavaScript editor.

---

## 📁 Script location

All scripts are stored in the server configuration directory:

```text
config/jsscripts/
```

---

## 🔰 ScriptAPI basics

### log(message)
Prints a message to the server console.

```js
log("Script started");
```

---

## ⏳ Scheduler

- setTimeout(fn, ticks) — Execute a callback function once after N ticks.
- setInterval(fn, ticks) — Repeat execution every N ticks.
- clearTimeout(id) — Cancels a scheduled timeout task.
- clearInterval(id) — Cancels a scheduled interval task (alias of clearTimeout).

```js
setInterval(() => log("2 seconds passed"), 40);
```

---

## 📣 ScriptAPI events

| Event        | Arguments     | Description           |
|--------------|---------------|-----------------------|
| Events.onJoin | playerName    | Triggered when a player joins the server. |

```js
Events.onJoin(p => Server.broadcast("Welcome, " + p + "!"));
```

---

## 🎮 Player & HUD

- Player.teleport(playerName, x, y, z) — Teleport the player.
- Player.giveItem(playerName, itemId, count) — Give the player an item.
- Player.getPos(playerName) — Returns an array `[x, y, z]` with the player's coordinates, or `null`.
- Player.sendActionBar(playerName, text) — Display text in the actionbar.
- Player.sendTitle(playerName, title, subtitle, fadeIn, stay, fadeOut) — Full formatted title sequence.

```js
Player.giveItem("Notch", "minecraft:diamond", 64);
```

---

## 🌍 World API

- World.setBlock(x, y, z, blockId) — Set a block in the world.
- World.strikeLightning(x, y, z) — Spawn a lightning bolt at coordinates.
- World.spawnParticle(id, x, y, z, count, dx, dy, dz, speed) — Spawn particles.
- World.playSound(id, x, y, z, volume, pitch) — Play a sound at world coordinates.

```js
World.setBlock(0, 100, 0, "minecraft:stone");
```

---

## 🧠 Server & Command API

- Server.runCommand(command) — Executes a command as the server.
- Server.broadcast(message) — Sends a system chat message to all online players.
- Server.getPlayers() — Returns a list of all online ServerPlayer objects.
- Server.getPlayer(playerName) — Returns a specific player object by name.

```js
Server.runCommand("time set day");
```

---

## 🧪 Examples

Periodic effect:

```js
setInterval(() => {
    Server.runCommand("weather clear");
    Server.broadcast("§bThe weather is clear again!");
}, 6000);
```

Event on player join:

```js
Events.onJoin(playerName => {
    Server.broadcast("§e" + playerName + " has joined the server!");
    
    setTimeout(() => {
        Player.sendTitle(playerName, "§6JsScripts", "Welcome to the game!", 10, 70, 20);
        
        let pos = Player.getPos(playerName);
        if (pos) {
            World.playSound("minecraft:entity.player.levelup", pos[0], pos[1], pos[2], 1.0, 1.0);
            World.spawnParticle("minecraft:happy_villager", pos[0], pos[1] + 2, pos[2], 20, 0.5, 0.5, 0.5, 0.1);
        }
    }, 40);
});
```

---

## 🚧 Beta notice

JsScripts is **still under active development** and may contain bugs or missing features.

---

## 🎉 Thank you

Thanks for using JsScripts! More features, stability improvements, and tools are coming soon.
