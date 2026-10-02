package com.tienda;

import com.tienda.session.SessionManager;
import com.tienda.ui.CatalogFrame;
import com.tienda.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            // Si ya existe una sesión local guardada, se va directo al catálogo
            SessionManager.load().ifPresentOrElse(
                    s -> new CatalogFrame(s).setVisible(true),
                    () -> new LoginFrame().setVisible(true));
        });
    }
}
