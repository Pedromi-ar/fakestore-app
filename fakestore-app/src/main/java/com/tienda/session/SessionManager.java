package com.tienda.session;

import com.tienda.model.Role;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Sesión almacenada de forma LOCAL (archivo en el home del usuario).
 * El rol se lee siempre de aquí, nunca se valida contra Fake Store API.
 */
public final class SessionManager {
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".fakestore-app", "session.properties");

    private SessionManager() { }

    public static void save(Session s) {
        try {
            Files.createDirectories(FILE.getParent());
            Properties p = new Properties();
            p.setProperty("user", s.user());
            p.setProperty("role", s.role().name());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Sesion local");
            }
        } catch (IOException e) {
            System.err.println("No se pudo guardar la sesión: " + e.getMessage());
        }
    }

    public static Optional<Session> load() {
        if (!Files.exists(FILE)) return Optional.empty();
        try (InputStream in = Files.newInputStream(FILE)) {
            Properties p = new Properties();
            p.load(in);
            return Optional.of(new Session(p.getProperty("user"), Role.valueOf(p.getProperty("role"))));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void clear() {
        try { Files.deleteIfExists(FILE); } catch (IOException ignored) { }
    }
}
