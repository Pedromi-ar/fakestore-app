package com.tienda.ui;

import com.tienda.model.Product;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 * Renderer reutilizable (equivalente a RecyclerView/FlatList): la misma instancia
 * se reutiliza para pintar cada celda, solo se renderizan las visibles.
 * Cada tarjeta muestra imagen, título y precio.
 */
public class ProductCellRenderer extends JPanel implements ListCellRenderer<Product> {
    static final int CELL_W = 200, CELL_H = 250, IMG = 140;

    private final JLabel image = new JLabel();
    private final JLabel title = new JLabel();
    private final JLabel price = new JLabel();

    public ProductCellRenderer() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDDDDD)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        image.setAlignmentX(CENTER_ALIGNMENT);
        title.setAlignmentX(CENTER_ALIGNMENT);
        price.setAlignmentX(CENTER_ALIGNMENT);
        price.setFont(price.getFont().deriveFont(Font.BOLD, 16f));
        price.setForeground(new Color(0x1B7F3B));
        add(image);
        add(javax.swing.Box.createVerticalStrut(8));
        add(title);
        add(javax.swing.Box.createVerticalStrut(6));
        add(price);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Product> list, Product p, int index,
                                                  boolean selected, boolean focus) {
        image.setIcon(ImageLoader.get(p.image(), IMG, list::repaint));
        String t = p.title().length() > 42 ? p.title().substring(0, 40) + "…" : p.title();
        title.setText("<html><div style='width:150px;text-align:center'>" + escape(t) + "</div></html>");
        price.setText(String.format("$%.2f", p.price()));
        setBackground(selected ? new Color(0xE8F0FE) : Color.WHITE);
        return this;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
