package com.tienda.ui;

import com.tienda.model.Role;
import com.tienda.session.Session;
import com.tienda.session.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Inicio de sesión simulado: guarda usuario y rol de forma local. */
public class LoginFrame extends JFrame {
    public LoginFrame() {
        super("Iniciar sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTextField user = new JTextField(16);
        JComboBox<Role> role = new JComboBox<>(Role.values());
        JButton enter = new JButton("Ingresar");

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel h = new JLabel("Tienda - Fake Store");
        h.setFont(h.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; p.add(h, c);
        c.gridwidth = 1; c.gridy = 1; p.add(new JLabel("Usuario:"), c);
        c.gridx = 1; p.add(user, c);
        c.gridx = 0; c.gridy = 2; p.add(new JLabel("Rol:"), c);
        c.gridx = 1; p.add(role, c);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; p.add(enter, c);

        Runnable login = () -> {
            String u = user.getText().trim();
            if (u.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Escribe un nombre de usuario", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Session s = new Session(u, (Role) role.getSelectedItem());
            SessionManager.save(s);
            new CatalogFrame(s).setVisible(true);
            dispose();
        };
        enter.addActionListener(e -> login.run());
        user.addActionListener(e -> login.run());

        setContentPane(p);
        getRootPane().setDefaultButton(enter);
        pack();
        setLocationRelativeTo(null);
    }
}
