**English language**  
[🇬🇧 English](README.md)

# JsScripts - API Docs
### Скриптовый мод на JavaScript для игровых ивентов · Fabric 1.21.8

JsScripts - Это очень удобный инструмент для вашего сервера с большим потенциалом, позволяющий создавать лорные ивенты, автоматизацию и просто простые скрипты.
Код можно писать прямо в игре! В мод встроен удобный редактор JavaScript редактор.

---

## 📁 Где хранятся скрипты

Все скрипты хранятся серверном конфиге, в папке:

```
config/jsscripts/
```

---

# 🔰 Основы ScriptAPI

## **log(message)**
Выводит сообщение в консоль сервера.

```js
log("Скрипт запущен");
```

---

## **scriptType("once" | "loop")**
Определяет тип выполнения скрипта:

| Тип | Описание |
|-----|----------|
| **once** | Скрипт выполняется один раз и выгружается |
| **loop** | Скрипт работает каждый тик |

```js
scriptType("loop");
```

---

## scriptEnd()

Принудительно завершает выполнение текущего скрипта.
После вызова скрипт полностью выгружается из ScriptEngine: удаляются все обработчики событий, цикл onLoop, а также задачи планировщика (wait, every, repeat). Никакие дальнейшие строки скрипта также не будут выполнены.

Пример:
```js
if (player.getHealth() <= 0) {
    log("Скрипт завершён, т.к. игрок мёртв");
    scriptEnd();
}
```
---

## **on(event, handler)**
Регистрирует JS‑обработчик события.

```js
on("playerJoin", p => msg(p, "Добро пожаловать!"));
```

---

## **onLoop(handler)**
Выполняется каждый тик, если скрипт работает в режиме `loop`.

---

# ⏳ Планировщик (Scheduler)

## **wait(ticks, fn)**
Выполнить действие через N тиков.

## **delay(ticks, fn)**
Алиас `wait`.

## **repeat(count, fn(index))**
Выполнить функцию несколько раз, 1 раз в тик.

## **every(ticks, fn)**
Повторять выполнение каждые N тиков.

```js
every(40, () => log("Прошло 2 секунды"));
```

---

# 📣 События ScriptAPI

| Событие | Аргументы | Описание |
|---------|-----------|----------|
| **serverStart** | – | сервер полностью запущен |
| **serverTick** | – | вызывается каждый тик |
| **playerTick** | player | тик игрока |
| **playerJoin** | player | вход игрока |
| **playerLeave** | player | выход игрока |
| **chat** | text, player | сообщение в чате |
| **blockBreak** | player, pos, state | блок сломан |
| **blockPlace** | player, pos, state | блок установлен / кликнут |

---

# 🎮 Player & HUD

## **msg(player, text)**
Отправить игроку сообщение.

## **sendMessage(player, text)**
Алиас `msg`.

## **actionbar(player, text)**
Показать текст в actionbar.

## **title(player, text)**
Показать титул.

## **subtitle(player, text)**
Показать подзаголовок.

## **fullTitle(player, title, subtitle, fadeIn, stay, fadeOut)**
Полная титульная заставка.

## **tp(player, x, y, z)**
Телепортация игрока.

## **give(player, itemId)**
Выдать предмет:

```js
give(player, "minecraft:diamond");
```

## **playSound(player, id, volume?, pitch?)**
Проиграть звук только для конкретного игрока.

---

# 🌍 World

## **World.overworld()**
Возвращает оверворлд.

## **World.nether()**
Возвращает ад.

## **World.end()**
Возвращает край.

---

## **World.setBlock(world, x, y, z, blockId)**
Устанавливает блок.

## **World.particle(world, id, x, y, z, dx, dy, dz, speed, count)**
Создаёт частицы.

## **playSoundAt(world, x, y, z, id, volume?, pitch?)**
Проигрывает звук в мире.

---

# 🧠 Command

## **runCommand(command)**
Выполняет команду от имени сервера.

## **runCommandAs(player, command)**
Выполняет команду от имени игрока.

---

# 📡 Radius

## **emitRadius(event, x, y, z, radius, ...args)**
Вызывает событие **только для тех игроков, которые находятся в радиусе**.

```js
emitRadius("alert", 100, 70, 100, 15, "Кто-то рядом!");
```

Обработчик:

```js
on("alert", (player, msg) => msg(player, msg));
```

---

# 🧪 Примеры

## Периодический эффект
```js
scriptType("loop");

every(100, () => {
    let w = World.overworld();
    World.particle(w, "minecraft:explosion", 0, 100, 0, 0, 0, 0, 1, 10);
});
```

## Radius‑ивент при входе игрока
```js
on("playerJoin", p => {
    emitRadius("joinPing", p.getX(), p.getY(), p.getZ(), 10, "Новый игрок рядом!");
});

on("joinPing", (player, msg) => msg(player, msg));
```

---
JsScripts всё ещё на стадии разработки, и впереди множество улучшений. (По крайней мере на это надеюсь)

Жду фидбек!
