package com.tienda.ui;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Descarga imágenes en hilos de fondo (no congela la UI) y las guarda en caché. */
public final class ImageLoader {
    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "image-loader");
        t.setDaemon(true);
        return t;
    });

    private ImageLoader() { }

    /** Devuelve el icono si ya está en caché; si no, un placeholder y lanza la descarga. */
    public static Icon get(String url, int size, Runnable onLoaded) {
        String key = url + "@" + size;
        Icon cached = CACHE.get(key);
        if (cached != null) return cached;
        if (PENDING.add(key)) {
            POOL.submit(() -> {
                try {
                    var conn = URI.create(url).toURL().openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 FakeStoreApp");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    BufferedImage img;
                    try (var in = conn.getInputStream()) { img = ImageIO.read(in); }
                    CACHE.put(key, img == null ? placeholder(size) : new ImageIcon(scale(img, size)));
                } catch (Exception e) {
                    CACHE.put(key, placeholder(size));
                } finally {
                    PENDING.remove(key);
                    SwingUtilities.invokeLater(onLoaded);
                }
            });
        }
        return placeholder(size);
    }

    private static BufferedImage scale(BufferedImage src, int size) {
        double f = Math.min((double) size / src.getWidth(), (double) size / src.getHeight());
        int w = Math.max(1, (int) (src.getWidth() * f)), h = Math.max(1, (int) (src.getHeight() * f));
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, (size - w) / 2, (size - h) / 2, w, h, null);
        g.dispose();
        return out;
    }

    private static Icon placeholder(int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new Color(0xEEEEEE));
        g.fillRoundRect(0, 0, size, size, 12, 12);
        g.dispose();
        return new ImageIcon(out);
    }
}
