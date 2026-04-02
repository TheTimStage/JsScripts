**English:**  
[🇬🇧 English](README.md)

# JsScripts - Документация API
### Мод для написания скриптов на JavaScript для игровых событий · Fabric 1.21.8-26.1.1

JsScripts — это мощный и гибкий инструмент для вашего сервера, позволяющий создавать сюжетные (lore) события, автоматизацию и простую логику скриптов с использованием JavaScript.  
Вы можете писать код **прямо в игре**, используя встроенный редактор JavaScript.

---

## 📁 Расположение скриптов

Все скрипты хранятся в директории конфигурации сервера:

```text
config/jsscripts/
```

---

## 🔰 Основы ScriptAPI

### log(message)
Выводит сообщение в консоль сервера.

```js
log("Script started");
```

---

## ⏳ Планировщик (Scheduler)

- `setTimeout(fn, ticks)` — Выполняет функцию обратного вызова (callback) один раз через N тиков.
- `setInterval(fn, ticks)` — Повторяет выполнение каждые N тиков.
- `clearTimeout(id)` — Отменяет запланированную задачу таймаута.
- `clearInterval(id)` — Отменяет запланированную задачу интервала (является псевдонимом clearTimeout).

```js
setInterval(() => log("2 seconds passed"), 40);
```

---

## 📣 События ScriptAPI

| Событие      | Аргументы     | Описание              |
|--------------|---------------|-----------------------|
| Events.onJoin | playerName    | Срабатывает, когда игрок заходит на сервер. |

```js
Events.onJoin(p => Server.broadcast("Welcome, " + p + "!"));
```

---

## 🎮 Игрок и интерфейс (Player & HUD)

- `Player.teleport(playerName, x, y, z)` — Телепортирует игрока.
- `Player.giveItem(playerName, itemId, count)` — Выдает предмет игроку.
- `Player.getPos(playerName)` — Возвращает массив `[x, y, z]` с координатами игрока или `null`.
- `Player.sendActionBar(playerName, text)` — Отображает текст над панелью быстрого доступа (actionbar).
- `Player.sendTitle(playerName, title, subtitle, fadeIn, stay, fadeOut)` — Полная последовательность появления форматированного заголовка (title) на экране.

```js
Player.giveItem("Notch", "minecraft:diamond", 64);
```

---

## 🌍 API Мира (World API)

- `World.setBlock(x, y, z, blockId)` — Устанавливает блок в мире.
- `World.strikeLightning(x, y, z)` — Вызывает удар молнии по указанным координатам.
- `World.spawnParticle(id, x, y, z, count, dx, dy, dz, speed)` — Создает частицы.
- `World.playSound(id, x, y, z, volume, pitch)` — Воспроизводит звук по координатам в мире.

```js
World.setBlock(0, 100, 0, "minecraft:stone");
```

---

## 🧠 API Сервера и Команд

- `Server.runCommand(command)` — Выполняет команду от имени сервера.
- `Server.broadcast(message)` — Отправляет системное сообщение в чат всем игрокам на сервере.
- `Server.getPlayers()` — Возвращает список всех объектов `ServerPlayer`, находящихся онлайн.
- `Server.getPlayer(playerName)` — Возвращает объект конкретного игрока по его имени.

```js
Server.runCommand("time set day");
```

---

## 🧪 Примеры

Периодический эффект:

```js
setInterval(() => {
    Server.runCommand("weather clear");
    Server.broadcast("§bThe weather is clear again!");
}, 6000);
```

Событие при входе игрока:

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

## 🚧 Уведомление о бета-версии

JsScripts **все еще находится в стадии активной разработки** и может содержать ошибки или недостающие функции.

---

## 🎉 Спасибо

Спасибо, что используете JsScripts! Скоро появятся новые функции, улучшения стабильности и дополнительные инструменты.

