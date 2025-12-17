**Русский язык:**  
[🇷🇺 Русский](README_ru.md)

# JsScripts - API Docs
### JavaScript Scripting Mod for Game Events · Fabric 1.21.8

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

### scriptType("once" | "loop")
Defines when and how the script executes.

| Type    | Description              |
|---------|--------------------------|
| once    | Executes once and unloads |
| loop    | Runs every server tick   |

```js
scriptType("loop");
```

### scriptEnd()
Forcibly terminates the current script. After calling this function, the script is fully unloaded from the ScriptEngine: all event handlers, onLoop handlers, and scheduled tasks (wait, every, repeat) are removed. No further code lines will execute.

Example:

```js
if (player.getHealth() <= 0) {
    log("Script terminated because player is dead");
    scriptEnd();
}
```

### on(event, handler)
Registers a JavaScript event handler.

```js
on("playerJoin", p => msg(p, "Welcome!"));
```

### onLoop(handler)
Runs every tick if the script type is `loop`.

---

## ⏳ Scheduler

- wait(ticks, fn) — Execute a callback after N ticks.
- delay(ticks, fn) — Alias of `wait`.
- repeat(count, fn(index)) — Execute several times, once per tick.
- every(ticks, fn) — Repeat execution every N ticks.

```js
every(40, () => log("2 seconds passed"));
```

---

## 📣 ScriptAPI events

| Event        | Arguments     | Description           |
|--------------|---------------|-----------------------|
| serverStart  | –             | server fully started  |
| serverTick   | –             | runs every tick       |
| playerTick   | player        | tick for each player  |
| playerJoin   | player        | player joined         |
| playerLeave  | player        | player left           |
| chat         | text, player  | chat message          |
| blockBreak   | player, pos, state | block broken    |
| blockPlace   | player, pos, state | block placed / interacted |

---

## 🎮 Player & HUD

- msg(player, text) — Send a message to a player.
- sendMessage(player, text) — Alias of `msg`.
- actionbar(player, text) — Display text in the actionbar.
- title(player, text) — Show a title.
- subtitle(player, text) — Show a subtitle.
- fullTitle(player, title, subtitle, fadeIn, stay, fadeOut) — Full formatted title sequence.
- tp(player, x, y, z) — Teleport the player.
- give(player, itemId) — Give the player an item.

```js
give(player, "minecraft:diamond");
```

- playSound(player, id, volume?, pitch?) — Play a sound only for the specified player.

---

## 🌍 World API

- World.overworld() — Returns the overworld.
- World.nether() — Returns the nether.
- World.end() — Returns the end dimension.

- World.setBlock(world, x, y, z, blockId) — Set a block in the world.
- World.particle(world, id, x, y, z, dx, dy, dz, speed, count) — Spawn particles.
- playSoundAt(world, x, y, z, id, volume?, pitch?) — Play a sound at world coordinates.

---

## 🧠 Command API

- runCommand(command) — Executes a command as the server.
- runCommandAs(player, command) — Executes a command as the player.

---

## 📡 Radius API

emitRadius(event, x, y, z, radius, ...args) — Triggers an event **only for players inside a given radius**.

```js
emitRadius("alert", 100, 70, 100, 15, "Someone is near!");
```

Handler:

```js
on("alert", (player, msg) => msg(player, msg));
```

---

## 🧪 Examples

Periodic effect:

```js
scriptType("loop");

every(100, () => {
    let w = World.overworld();
    World.particle(w, "minecraft:explosion", 0, 100, 0, 0, 0, 0, 1, 10);
});
```

Radius event on player join:

```js
on("playerJoin", p => {
    emitRadius("joinPing", p.getX(), p.getY(), p.getZ(), 10, "A new player is nearby!");
});

on("joinPing", (player, msg) => msg(player, msg));
```

---

## 🚧 Beta notice

JsScripts is **still under active development** and may contain bugs or missing features.  
Feedback, suggestions, and reports are highly appreciated.

