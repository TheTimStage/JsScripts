package org.tts.jsscripts.systems;

import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.mozilla.javascript.*;

import java.util.*;

public final class ScriptEngine {
    private static final Map<String, Scriptable> contexts = new HashMap<>();
    private static final Map<String, Script> compiled = new HashMap<>();
    private static Scriptable baseScope;
    private static final Map<String, List<Function>> eventHandlers = new HashMap<>();
    private static final Map<String, String> scriptTypes = new HashMap<>();
    private static final Map<String, Function> loopHandlers = new HashMap<>();
    private record Task(String scriptName, int delay, Runnable callback) {}
    private static final List<Task> scheduledTasks = new ArrayList<>();
    private static MinecraftServer server;


    public static void init(MinecraftServer srv) {
        server = srv;

        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        baseScope = cx.initStandardObjects();

        registerCoreAPI();
        registerWorldAPI();
        registerPlayerAndHudAPI();
        registerCommandAPI();

        Context.exit();

        System.out.println("[JsScripts] ScriptEngine initialized with extended API.");
    }

    private static void registerCoreAPI() {

        // log(msg)
        ScriptableObject.putProperty(baseScope, "log", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length > 0) {
                    System.out.println("[JS] " + args[0]);
                }
                return null;
            }
        });

        // scriptType("once" | "loop")
        ScriptableObject.putProperty(baseScope, "scriptType", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

                if (args.length == 0) return null;

                String type = Context.toString(args[0]);

                Scriptable real = resolveRootScope(scope);
                String name = (String) ScriptableObject.getProperty(real, "__scriptName");

                scriptTypes.put(name, type);

                return null;
            }
        });

        // on(event, fn)
        ScriptableObject.putProperty(baseScope, "on", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

                if (args.length < 2 || !(args[1] instanceof Function fn)) return null;

                String event = Context.toString(args[0]);
                eventHandlers.computeIfAbsent(event, k -> new ArrayList<>()).add(fn);
                return null;
            }
        });

        // onLoop(fn)
        ScriptableObject.putProperty(baseScope, "onLoop", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

                if (args.length == 1 && args[0] instanceof Function fn) {
                    Scriptable real = resolveRootScope(scope);
                    String name = (String) ScriptableObject.getProperty(real, "__scriptName");
                    loopHandlers.put(name, fn);
                }
                return null;
            }
        });

        // wait(ticks, callback)
        BaseFunction waitFn = new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) return null;
                if (!(args[1] instanceof Function fn)) return null;

                int ticks = toInt(args[0]);
                String scriptName = getScriptName(scope);

                scheduledTasks.add(new Task(scriptName, ticks, () -> {
                    try (Context c2 = Context.enter()) {
                        fn.call(c2, fn.getParentScope(), fn.getParentScope(), new Object[]{});
                    }
                }));

                return null;
            }
        };
        ScriptableObject.putProperty(baseScope, "wait", waitFn);

        // delay(ticks, fn) → алиас wait(...)
        ScriptableObject.putProperty(baseScope, "delay", waitFn);

        // repeat(count, callback(index))
        ScriptableObject.putProperty(baseScope, "repeat", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

                if (args.length < 2) return null;
                if (!(args[1] instanceof Function fn)) return null;

                int count = toInt(args[0]);
                String scriptName = getScriptName(scope);

                for (int i = 1; i <= count; i++) {
                    final int index = i;

                    scheduledTasks.add(new Task(scriptName, i, () -> {
                        try (Context c2 = Context.enter()) {
                            fn.call(c2, fn.getParentScope(), fn.getParentScope(), new Object[]{index});
                        }
                    }));
                }

                return null;
            }
        });

        // every(ticks, fn)
        ScriptableObject.putProperty(baseScope, "every", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) return null;
                if (!(args[1] instanceof Function fn)) return null;

                int ticks = toInt(args[0]);
                String scriptName = getScriptName(scope);

                Runnable[] holder = new Runnable[1];

                Runnable task = () -> {
                    try (Context c2 = Context.enter()) {
                        fn.call(c2, fn.getParentScope(), fn.getParentScope(), new Object[]{});
                    }
                    // если скрипт не once — перепланируем
                    if (!"once".equals(scriptTypes.get(scriptName))) {
                        scheduledTasks.add(new Task(scriptName, ticks, holder[0]));
                    }
                };

                holder[0] = task;

                scheduledTasks.add(new Task(scriptName, ticks, task));
                return null;
            }
        });
    }

    private static void registerWorldAPI() {

        Context cx = Context.getCurrentContext();
        Scriptable worldObj = cx.newObject(baseScope);
        ScriptableObject.putProperty(baseScope, "World", worldObj);

        // World.overworld()
        ScriptableObject.putProperty(worldObj, "overworld", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                return server != null ? server.getOverworld() : null;
            }
        });

        // World.nether()
        ScriptableObject.putProperty(worldObj, "nether", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (server == null) return null;
                return server.getWorld(ServerWorld.NETHER);
            }
        });

        // World.end()
        ScriptableObject.putProperty(worldObj, "end", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (server == null) return null;
                return server.getWorld(ServerWorld.END);
            }
        });

        // World.setBlock(world, x,y,z, blockId)
        ScriptableObject.putProperty(worldObj, "setBlock", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 5) return null;
                if (!(args[0] instanceof ServerWorld world)) return null;

                int x = toInt(args[1]);
                int y = toInt(args[2]);
                int z = toInt(args[3]);
                String blockId = String.valueOf(args[4]);

                var block = Registries.BLOCK.get(Identifier.of(blockId));
                if (block == null) {
                    System.out.println("[JsScripts] Unknown block: " + blockId);
                    return null;
                }

                world.setBlockState(new BlockPos(x, y, z), block.getDefaultState());
                return null;
            }
        });

        // World.particle(world, id, x,y,z, dx,dy,dz, speed, count)
        ScriptableObject.putProperty(worldObj, "particle", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 10) return null;
                if (!(args[0] instanceof ServerWorld world)) return null;

                String id = String.valueOf(args[1]);

                double x = toDouble(args[2]);
                double y = toDouble(args[3]);
                double z = toDouble(args[4]);

                double dx = toDouble(args[5]);
                double dy = toDouble(args[6]);
                double dz = toDouble(args[7]);

                double speed = toDouble(args[8]);
                int count = toInt(args[9]);

                var type = Registries.PARTICLE_TYPE.get(Identifier.of(id));

                if (type instanceof net.minecraft.particle.SimpleParticleType simple) {
                    world.spawnParticles(simple, x, y, z, count, dx, dy, dz, speed);
                } else {
                    System.out.println("[JsScripts] Unsupported complex particle: " + id);
                }

                return null;
            }
        });

        // playSoundAt(world, x,y,z, id [, volume [, pitch]])
        ScriptableObject.putProperty(baseScope, "playSoundAt", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 5) return null;
                if (!(args[0] instanceof ServerWorld world)) return null;

                double x = toDouble(args[1]);
                double y = toDouble(args[2]);
                double z = toDouble(args[3]);

                String id = String.valueOf(args[4]);

                float vol = 1f;
                float pitch = 1f;

                if (args.length >= 6) vol = toFloat(args[5]);
                if (args.length >= 7) pitch = toFloat(args[6]);

                SoundEvent sound = Registries.SOUND_EVENT.get(Identifier.of(id));
                if (sound == null) {
                    System.out.println("[JsScripts] Unknown sound: " + id);
                    return null;
                }

                world.playSound(
                        null,
                        x, y, z,
                        sound,
                        SoundCategory.MASTER,
                        vol, pitch
                );
                return null;
            }
        });
    }

    private static void registerPlayerAndHudAPI() {
        // sendMessage(player, text)
        ScriptableObject.putProperty(baseScope, "sendMessage", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                Text text = Text.literal(String.valueOf(args[1]));
                player.sendMessage(text);
                return null;
            }
        });

        // msg(player, text) — удобный алиас
        ScriptableObject.putProperty(baseScope, "msg", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                Text text = Text.literal(String.valueOf(args[1]));
                player.sendMessage(text);
                return null;
            }
        });

        // actionbar(player, text)
        ScriptableObject.putProperty(baseScope, "actionbar", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                Text text = Text.literal(String.valueOf(args[1]));
                player.sendMessage(text, true); // overlay = true (actionbar)
                return null;
            }
        });

        // title(player, text) — через /title
        ScriptableObject.putProperty(baseScope, "title", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (server == null) return null;
                if (args.length < 2) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                String text = String.valueOf(args[1]).replace("\"", "'");
                String name = player.getGameProfile().getName();

                String cmd = "title " + name + " title {\"text\":\"" + text + "\"}";
                ServerCommandSource src = server.getCommandSource();
                server.getCommandManager().executeWithPrefix(src, cmd);
                return null;
            }
        });

        // subtitle(player, text)
        ScriptableObject.putProperty(baseScope, "subtitle", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (server == null) return null;
                if (args.length < 2) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                String text = String.valueOf(args[1]).replace("\"", "'");
                String name = player.getGameProfile().getName();

                String cmd = "title " + name + " subtitle {\"text\":\"" + text + "\"}";
                ServerCommandSource src = server.getCommandSource();
                server.getCommandManager().executeWithPrefix(src, cmd);
                return null;
            }
        });

        // fullTitle(player, title, subtitle, fadeIn, stay, fadeOut)
        ScriptableObject.putProperty(baseScope, "fullTitle", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (server == null) return null;
                if (args.length < 6) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                String title = String.valueOf(args[1]).replace("\"", "'");
                String subtitle = String.valueOf(args[2]).replace("\"", "'");
                int fadeIn = toInt(args[3]);
                int stay = toInt(args[4]);
                int fadeOut = toInt(args[5]);

                String name = player.getGameProfile().getName();

                ServerCommandSource src = server.getCommandSource();
                server.getCommandManager().executeWithPrefix(src,
                        "title " + name + " times " + fadeIn + " " + stay + " " + fadeOut);
                server.getCommandManager().executeWithPrefix(src,
                        "title " + name + " subtitle {\"text\":\"" + subtitle + "\"}");
                server.getCommandManager().executeWithPrefix(src,
                        "title " + name + " title {\"text\":\"" + title + "\"}");

                return null;
            }
        });

        // tp(player, x,y,z)
        ScriptableObject.putProperty(baseScope, "tp", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 4) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                ServerWorld world = player.getWorld();

                double x = toDouble(args[1]);
                double y = toDouble(args[2]);
                double z = toDouble(args[3]);


                return null;
            }
        });

        // give(player, itemId)
        ScriptableObject.putProperty(baseScope, "give", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                String id = String.valueOf(args[1]);
                var item = Registries.ITEM.get(Identifier.of(id));
                if (item == null) {
                    System.out.println("[JsScripts] Unknown item: " + id);
                    return null;
                }

                player.getInventory().insertStack(item.getDefaultStack());
                return null;
            }
        });

        // playSound(player, id [, volume [, pitch]])
        ScriptableObject.putProperty(baseScope, "playSound", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

                if (args.length < 2) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                String id = String.valueOf(args[1]);

                float vol = 1.0f;
                float pitch = 1.0f;

                if (args.length >= 3) vol = toFloat(args[2]);
                if (args.length >= 4) pitch = toFloat(args[3]);

                SoundEvent sound = Registries.SOUND_EVENT.get(Identifier.of(id));
                if (sound == null) {
                    System.out.println("[JsScripts] Unknown sound: " + id);
                    return null;
                }

                player.playSound(sound, vol, pitch);
                return null;
            }
        });

        // emitRadius(event, x, y, z, radius, ...args)
        ScriptableObject.putProperty(baseScope, "emitRadius", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (server == null) return null;
                if (args.length < 5) return null;
                String event = String.valueOf(args[0]);

                double x = toDouble(args[1]);
                double y = toDouble(args[2]);
                double z = toDouble(args[3]);
                double radius = toDouble(args[4]);

                Object[] extra = new Object[Math.max(0, args.length - 5)];
                if (args.length > 5)
                    System.arraycopy(args, 5, extra, 0, extra.length);

                double r2 = radius * radius;

                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    double dx = p.getX() - x;
                    double dy = p.getY() - y;
                    double dz = p.getZ() - z;

                    if (dx*dx + dy*dy + dz*dz <= r2) {
                        Object[] callArgs = new Object[extra.length + 1];
                        callArgs[0] = p;
                        System.arraycopy(extra, 0, callArgs, 1, extra.length);

                        ScriptEngine.emit(event, callArgs);
                    }
                }

                return null;
            }
        });
    }


    private static void registerCommandAPI() {

        // runCommand("say test")
        ScriptableObject.putProperty(baseScope, "runCommand", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (server == null) return null;
                if (args.length < 1) return null;

                String cmd = String.valueOf(args[0]);
                ServerCommandSource src = server.getCommandSource();
                server.getCommandManager().executeWithPrefix(src, cmd);
                return null;
            }
        });

        // runCommandAs(player, "say hi")
        ScriptableObject.putProperty(baseScope, "runCommandAs", new BaseFunction() {
            @Override public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (server == null) return null;
                if (args.length < 2) return null;
                if (!(args[0] instanceof ServerPlayerEntity player)) return null;

                String cmd = String.valueOf(args[1]);
                ServerCommandSource src = player.getCommandSource();
                server.getCommandManager().executeWithPrefix(src, cmd);
                return null;
            }
        });
    }

    public static void emit(String event, Object... args) {
        List<Function> handlers = eventHandlers.get(event);
        if (handlers != null) {
            try (Context cx = Context.enter()) {
                for (Function fn : handlers) {
                    fn.call(cx, fn.getParentScope(), fn.getParentScope(), args);
                }
            }
        }

        if (event.equals("serverTick")) {
            try (Context cx = Context.enter()) {
                for (var entry : loopHandlers.entrySet()) {
                    String scriptName = entry.getKey();
                    Function fn = entry.getValue();

                    if ("loop".equals(scriptTypes.get(scriptName))) {
                        fn.call(cx, fn.getParentScope(), fn.getParentScope(), new Object[]{});
                    }
                }
            }

            processScheduler();
        }
    }

    private static void processScheduler() {
        List<Task> next = new ArrayList<>();
        List<Task> run = new ArrayList<>();

        for (Task t : scheduledTasks) {
            int d = t.delay() - 1;

            if (d <= 0) {
                if (!"once".equals(scriptTypes.get(t.scriptName()))) {
                    run.add(t);
                }
            } else {
                next.add(new Task(t.scriptName(), d, t.callback()));
            }
        }

        scheduledTasks.clear();
        scheduledTasks.addAll(next);

        for (Task t : run) {
            try {
                t.callback().run();
            } catch (Throwable e) {
                System.out.println("[JsScripts] Error in scheduled task: " + e);
            }
        }
    }

    public static void run(String name, String code) {
        try (Context cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);

            Script script = compiled.computeIfAbsent(name,
                    n -> cx.compileString(code, n, 1, null));

            Scriptable scope = contexts.computeIfAbsent(name, n -> {
                Scriptable sc = cx.newObject(baseScope);
                sc.setPrototype(baseScope);
                sc.setParentScope(null);
                ScriptableObject.putProperty(sc, "__scriptName", name);
                return sc;
            });

            script.exec(cx, scope);

            if ("once".equals(scriptTypes.get(name))) {

                compiled.remove(name);
                contexts.remove(name);
                loopHandlers.remove(name);

                for (List<Function> list : eventHandlers.values()) {
                    list.removeIf(fn -> Objects.equals(getScriptName(fn.getParentScope()), name));
                }

                scheduledTasks.removeIf(t -> t.scriptName().equals(name));

                System.out.println("[JsScripts] Script '" + name + "' finished (once) and removed.");
            }

        } catch (Throwable e) {
            System.out.println("[JsScripts] JS Error: " + e);
        }
    }


    private static Scriptable resolveRootScope(Scriptable sc) {
        while (sc.getParentScope() != null)
            sc = sc.getParentScope();
        return sc;
    }

    private static String getScriptName(Scriptable sc) {
        Scriptable root = resolveRootScope(sc);
        Object o = ScriptableObject.getProperty(root, "__scriptName");
        return (o instanceof String s) ? s : null;
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private static float toFloat(Object o) {
        if (o instanceof Number n) return n.floatValue();
        if (o instanceof String s) {
            try { return Float.parseFloat(s); } catch (NumberFormatException ignored) {}
        }
        return 0f;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return 0d;
    }
}
