package org.tts.jsscripts.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class JsSStorage {

    private static final Path ROOT = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("jsscripts")
            .normalize()
            .toAbsolutePath();

    public static void init() {
        try {
            Files.createDirectories(ROOT);
            System.out.println("[JsSStorage] ROOT = " + ROOT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public record Entry(String path, boolean directory) {}

    // ----------------------------------------------------------------------
    // LIST DIRECTORY
    // ----------------------------------------------------------------------
    public static List<Entry> list(String directory) {
        List<Entry> out = new ArrayList<>();

        Path base = (directory == null || directory.isEmpty())
                ? ROOT
                : ROOT.resolve(directory).normalize().toAbsolutePath();

        if (!base.startsWith(ROOT)) {
            System.out.println("[JsSStorage] LIST BLOCKED: " + base);
            return List.of();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path p : stream) {
                boolean dir = Files.isDirectory(p);
                String rel = ROOT.relativize(p).toString().replace("\\", "/");
                out.add(new Entry(rel, dir));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }

    // ----------------------------------------------------------------------
    // LOAD
    // ----------------------------------------------------------------------
    public static String loadScript(String path) {
        if (path == null || path.isEmpty()) return "";
        Path file = ROOT.resolve(path).normalize().toAbsolutePath();

        if (!file.startsWith(ROOT)) return "";

        try {
            if (Files.exists(file)) {
                return Files.readString(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    // ----------------------------------------------------------------------
    // SAVE
    // ----------------------------------------------------------------------
    public static void saveScript(String path, String code) {
        if (path == null || path.isEmpty()) return;

        Path file = ROOT.resolve(path).normalize().toAbsolutePath();

        if (!file.startsWith(ROOT)) {
            System.out.println("[JsSStorage] SAVE BLOCKED: " + file);
            return;
        }

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(
                    file,
                    code,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------------
    // DELETE
    // ----------------------------------------------------------------------
    public static void deleteEntry(String path, boolean directory) {
        if (path == null || path.isEmpty()) return;

        Path file = ROOT.resolve(path).normalize().toAbsolutePath();

        if (!file.startsWith(ROOT)) return;

        try {
            if (directory) {
                Files.walk(file)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
            } else {
                Files.deleteIfExists(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
