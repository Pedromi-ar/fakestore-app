package com.tienda.ui;

import com.tienda.model.Product;
import com.tienda.service.ProductApi;
import com.tienda.session.Session;
import com.tienda.session.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** Pantalla principal: US03 (catálogo), US04 (filtro por categoría), acceso a US05 (detalle). */
public class CatalogFrame extends JFrame {
    private static final String LOADING = "loading", LIST = "list", ERROR = "error";

    private final Session session;
    private final ProductApi api = new ProductApi();

    private final DefaultListModel<Product> model = new DefaultListModel<>();
    private final JList<Product> list = new JList<>(model);
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final JLabel errorLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final ButtonGroup chipGroup = new ButtonGroup();

    private String currentCategory = null;   // null = "Ver todos"
    private int requestSeq = 0;              // descarta respuestas obsoletas

    public CatalogFrame(Session session) {
        super("Catálogo de productos");
        this.session = session;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 700);
        setLocationRelativeTo(null);

        // --- Encabezado ---
        JLabel title = new JLabel("Catálogo de productos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel who = new JLabel(session.user() + " (" + session.role() + ")");
        JButton logout = new JButton("Cerrar sesión");
        logout.addActionListener(e -> {
            SessionManager.clear();
            dispose();
            new LoginFrame().setVisible(true);
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(who);
        right.add(logout);
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));
        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        // --- Chips de categorías (no bloquean la vista de productos) ---
        JScrollPane chipScroll = new JScrollPane(chips,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chipScroll.setBorder(BorderFactory.createEmptyBorder());
        chipScroll.setPreferredSize(new Dimension(100, 52));
        addChip("Ver todos", null, true);

        JPanel north = new JPanel(new BorderLayout());
        north.add(header, BorderLayout.NORTH);
        north.add(chipScroll, BorderLayout.SOUTH);

        // --- Lista tipo cuadrícula con celdas reciclables ---
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(0);
        list.setFixedCellWidth(ProductCellRenderer.CELL_W + 12);
        list.setFixedCellHeight(ProductCellRenderer.CELL_H + 12);
        list.setCellRenderer(new ProductCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = list.locationToIndex(e.getPoint());
                if (i >= 0 && list.getCellBounds(i, i).contains(e.getPoint())) {
                    openDetail(model.get(i));
                    list.clearSelection();
                }
            }
        });

        // --- Estado de carga (spinner centrado) ---
        JProgressBar spinner = new JProgressBar();
        spinner.setIndeterminate(true);
        spinner.setPreferredSize(new Dimension(220, 14));
        JLabel loadingText = new JLabel("Cargando productos…");
        JPanel loading = new JPanel(new java.awt.GridBagLayout());
        JPanel box = new JPanel(new BorderLayout(0, 10));
        box.add(loadingText, BorderLayout.NORTH);
        box.add(spinner, BorderLayout.CENTER);
        loading.add(box);

        // --- Estado de error con "Reintentar" ---
        JButton retry = new JButton("Reintentar");
        retry.addActionListener(e -> loadProducts(currentCategory));
        JPanel error = new JPanel(new java.awt.GridBagLayout());
        JPanel eBox = new JPanel(new BorderLayout(0, 12));
        errorLabel.setFont(errorLabel.getFont().deriveFont(15f));
        eBox.add(errorLabel, BorderLayout.CENTER);
        JPanel btnWrap = new JPanel();
        btnWrap.add(retry);
        eBox.add(btnWrap, BorderLayout.SOUTH);
        error.add(eBox);

        center.add(loading, LOADING);
        center.add(new JScrollPane(list), LIST);
        center.add(error, ERROR);

        add(north, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        loadCategories();
        loadProducts(null);
    }

    // ---------- US04 · Escenario 1: categorías ----------
    private void loadCategories() {
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() throws Exception { return api.getCategories(); }
            @Override protected void done() {
                try {
                    for (String c : get()) addChip(c, c, false);
                    chips.revalidate();
                    chips.repaint();
                } catch (Exception ignored) {
                    // Sin categorías: el catálogo general sigue funcionando
                }
            }
        }.execute();
    }

    private void addChip(String text, String category, boolean selected) {
        JToggleButton b = new JToggleButton(text, selected);
        b.setFocusPainted(false);
        b.addActionListener(e -> {
            if (java.util.Objects.equals(currentCategory, category)) return;
            currentCategory = category;
            loadProducts(category);
        });
        chipGroup.add(b);
        chips.add(b);
    }

    // ---------- US03 y US04: carga (general o filtrada) ----------
    private void loadProducts(String category) {
        final int myRequest = ++requestSeq;
        model.clear();                       // evita mostrar datos viejos mientras carga
        cards.show(center, LOADING);

        new SwingWorker<List<Product>, Void>() {
            @Override protected List<Product> doInBackground() throws Exception {
                return category == null ? api.getProducts() : api.getByCategory(category);
            }
            @Override protected void done() {
                if (myRequest != requestSeq) return;   // llegó tarde, ya hay otra petición
                try {
                    model.addAll(get());
                    cards.show(center, LIST);
                } catch (Exception e) {
                    errorLabel.setText("<html><div style='text-align:center'>No pudimos cargar los productos.<br>"
                            + "Revisa tu conexión e inténtalo de nuevo.</div></html>");
                    cards.show(center, ERROR);
                }
            }
        }.execute();
    }

    // ---------- US05: abrir detalle ----------
    private void openDetail(Product p) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Product, Void>() {
            @Override protected Product doInBackground() throws Exception { return api.getById(p.id()); }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    new ProductDetailDialog(CatalogFrame.this, get(), session.role(), api,
                            () -> loadProducts(currentCategory)).setVisible(true);
                } catch (Exception e) {
                    // Escenario 3: alerta y permanecemos en el catálogo general
                    JOptionPane.showMessageDialog(CatalogFrame.this, "Producto no disponible",
                            "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }
}
