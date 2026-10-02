package com.tienda.ui;

import com.tienda.model.Product;
import com.tienda.model.Role;
import com.tienda.service.ProductApi;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Detalle del producto con interfaz dinámica según el rol de la sesión local.
 * Los botones Editar/Eliminar SOLO se crean si el rol es ADMINISTRADOR
 * (no se instancian ocultos para los demás roles).
 */
public class ProductDetailDialog extends JDialog {
    private final ProductApi api;
    private final Runnable onChanged;
    private Product product;

    private final JLabel titleLabel = new JLabel();
    private final JLabel priceLabel = new JLabel();
    private final JLabel categoryLabel = new JLabel();
    private final JTextArea description = new JTextArea();

    public ProductDetailDialog(JFrame owner, Product product, Role role, ProductApi api, Runnable onChanged) {
        super(owner, "Detalle del producto", true);
        this.product = product;
        this.api = api;
        this.onChanged = onChanged;

        JLabel image = new JLabel("", JLabel.CENTER);
        image.setPreferredSize(new Dimension(280, 280));
        // Descarga en segundo plano; al terminar se vuelve a pedir el icono (ya en caché)
        image.setIcon(ImageLoader.get(product.image(), 260,
                () -> image.setIcon(ImageLoader.get(product.image(), 260, () -> { }))));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        priceLabel.setFont(priceLabel.getFont().deriveFont(Font.BOLD, 20f));
        priceLabel.setForeground(new Color(0x1B7F3B));
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);
        description.setBackground(getBackground());
        refreshFields();

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(titleLabel);
        info.add(Box.createVerticalStrut(8));
        info.add(priceLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(categoryLabel);
        info.add(Box.createVerticalStrut(10));
        JScrollPane sp = new JScrollPane(description);
        sp.setPreferredSize(new Dimension(320, 160));
        info.add(sp);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        body.add(image, BorderLayout.WEST);
        body.add(info, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (role == Role.ADMINISTRADOR) {           // interfaz de gestión solo para el administrador
            JButton edit = new JButton("Editar");
            JButton delete = new JButton("Eliminar");
            edit.addActionListener(e -> editProduct());
            delete.addActionListener(e -> deleteProduct());
            buttons.add(edit);
            buttons.add(delete);
        }
        JButton close = new JButton("Cerrar");
        close.addActionListener(e -> dispose());
        buttons.add(close);

        add(body, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private void refreshFields() {
        titleLabel.setText("<html><div style='width:300px'>" + esc(product.title()) + "</div></html>");
        priceLabel.setText(String.format("$%.2f", product.price()));
        categoryLabel.setText("Categoría: " + product.category());
        description.setText(product.description());
        description.setCaretPosition(0);
    }

    private void editProduct() {
        JTextField t = new JTextField(product.title(), 28);
        JTextField pr = new JTextField(String.valueOf(product.price()), 10);
        JTextArea d = new JTextArea(product.description(), 5, 28);
        d.setLineWrap(true);
        d.setWrapStyleWord(true);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Título")); form.add(t);
        form.add(new JLabel("Precio")); form.add(pr);
        form.add(new JLabel("Descripción")); form.add(new JScrollPane(d));

        if (JOptionPane.showConfirmDialog(this, form, "Editar producto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        double newPrice;
        try {
            newPrice = Double.parseDouble(pr.getText().trim());
            if (newPrice < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El precio no es válido", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Product updated = new Product(product.id(), t.getText().trim(), newPrice, d.getText().trim(),
                product.category(), product.image(), product.rating());

        run(() -> api.update(updated), () -> {
            product = updated;
            refreshFields();
            pack();
            JOptionPane.showMessageDialog(this, "Producto actualizado correctamente.");
            onChanged.run();
        }, "No se pudo actualizar el producto");
    }

    private void deleteProduct() {
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar \"" + product.title() + "\"?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                != JOptionPane.YES_OPTION) return;
        run(() -> api.delete(product.id()), () -> {
            JOptionPane.showMessageDialog(this, "Producto eliminado correctamente.");
            dispose();
            onChanged.run();
        }, "No se pudo eliminar el producto");
    }

    private interface ApiCall { void call() throws Exception; }

    private void run(ApiCall call, Runnable onOk, String errorMsg) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { call.call(); return null; }
            @Override protected void done() {
                try { get(); onOk.run(); }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(ProductDetailDialog.this, errorMsg,
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
