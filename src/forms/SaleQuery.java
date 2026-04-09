package forms;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import static ui.UIConstants.*;
import java.sql.*;

public class SaleQuery extends JInternalFrame {

    private JTextField txtSearch, txtFrom, txtTo;
    private JComboBox<String> cmbCustomer, cmbProduct;
    private JTable table;
    private DefaultTableModel model;
    private JLabel lblCount, lblTotal;

    public SaleQuery() {
        super("Sale Query", true, true, true, true);
        setSize(1150, 640);
        setLayout(new BorderLayout());

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(6, 78, 59), getWidth(), 0, new Color(4, 120, 87)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        header.setPreferredSize(new Dimension(0, 72));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel icon = new JLabel("🛒");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel title = new JLabel("Sale Query");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_MAIN);
        JLabel sub = new JLabel("Search & filter sale transactions");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(167, 243, 208));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title); titleBox.add(sub);
        JPanel leftH = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftH.setOpaque(false); leftH.add(icon); leftH.add(titleBox);
        header.add(leftH, BorderLayout.WEST);

        JPanel rightH = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        rightH.setOpaque(false);
        lblCount = new JLabel("0 records");
        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCount.setForeground(ACCENT);
        lblTotal = new JLabel("Net Total: ₹0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotal.setForeground(GOLD);
        rightH.add(lblCount);
        rightH.add(Box.createHorizontalStrut(16));
        rightH.add(lblTotal);
        header.add(rightH, BorderLayout.EAST);

        // ── Toolbar row 1 ───────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(51, 65, 85)));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row1.setBackground(BG_CARD);
        txtSearch = new JTextField(18); styleTextField(txtSearch);
        txtFrom   = new JTextField(12); styleTextField(txtFrom);
        txtTo     = new JTextField(12); styleTextField(txtTo);
        txtFrom.putClientProperty("JTextField.placeholderText", "dd-MM-yyyy");
        txtTo  .putClientProperty("JTextField.placeholderText", "dd-MM-yyyy");

        addLabel(row1, "🔍 Search:");  row1.add(txtSearch);
        addLabel(row1, "From (dd-MM-yyyy):"); row1.add(txtFrom);
        addLabel(row1, "To:");          row1.add(txtTo);

        JButton btnSearch = makeButton("Search", ACCENT);
        JButton btnClear  = makeButton("Clear",  new Color(100, 116, 139));
        row1.add(Box.createHorizontalStrut(6));
        row1.add(btnSearch); row1.add(btnClear);

        // ── Toolbar row 2: customer / product filter combos ─────────────────
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        row2.setBackground(BG_CARD);
        cmbCustomer = filterCombo("All Customers");
        cmbProduct  = filterCombo("All Products");
        addLabel(row2, "Customer:"); row2.add(cmbCustomer);
        addLabel(row2, "Product:");  row2.add(cmbProduct);

        toolbar.add(row1, BorderLayout.NORTH);
        toolbar.add(row2, BorderLayout.SOUTH);

        // ── Table ─────────────────────────────────────────────────────────
        // Columns match sale_details JOIN customers + products:
        // sale_id, sale_date, customer_name, product_name,
        // qty, rate, total_amt, vat_pct, vat_amt, discount_pct, discount_amt, net_amt, narration
        String[] cols = {
            "Sale ID", "Sale Date", "Customer", "Product",
            "Qty", "Rate (₹)", "Total (₹)", "VAT %", "VAT (₹)",
            "Disc %", "Disc (₹)", "Net (₹)", "Narration"
        };
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable(table);

        // Column widths
        int[] widths = {65, 90, 140, 150, 48, 80, 85, 55, 75, 55, 75, 90, 130};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Right-align numeric columns
        DefaultTableCellRenderer rAlign = new DefaultTableCellRenderer();
        rAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{4, 5, 6, 7, 8, 9, 10, 11})
            table.getColumnModel().getColumn(c).setCellRenderer(rAlign);

        // Net Amount column gold highlight
        table.getColumnModel().getColumn(11).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.RIGHT); }
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                    setForeground(GOLD);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        // Customer column teal
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                    setForeground(TEAL);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        // Product column purple
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                    setForeground(PURPLE);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        statusBar.setBackground(BG_DARK);
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(51, 65, 85)));

        add(header, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG_DARK);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(scroll,  BorderLayout.CENTER);
        center.add(statusBar, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        getContentPane().setBackground(BG_DARK);

        // Wire actions
        btnSearch.addActionListener(e -> loadData());
        btnClear .addActionListener(e -> {
            txtSearch.setText(""); txtFrom.setText(""); txtTo.setText("");
            cmbCustomer.setSelectedIndex(0); cmbProduct.setSelectedIndex(0);
            loadData();
        });
        txtSearch  .addActionListener(e -> btnSearch.doClick());
        cmbCustomer.addActionListener(e -> loadData());
        cmbProduct .addActionListener(e -> loadData());

        populateFilterCombos();
        loadData();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Populate customer / product combos from DB
    // ─────────────────────────────────────────────────────────────────────
    private void populateFilterCombos() {
        try (Statement st = MdiForm.sconnect.createStatement()) {
            ResultSet rs = st.executeQuery(
                "SELECT id, customer_name FROM customers WHERE is_active = 1 ORDER BY customer_name");
            while (rs.next())
                cmbCustomer.addItem(rs.getLong(1) + " — " + rs.getString(2));

            rs = st.executeQuery(
                "SELECT id, name FROM products WHERE is_active = 1 ORDER BY name");
            while (rs.next())
                cmbProduct.addItem(rs.getLong(1) + " — " + rs.getString(2));
        } catch (SQLException ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // Load data — queries sale_details JOIN customers + products
    // ─────────────────────────────────────────────────────────────────────
    private void loadData() {
        model.setRowCount(0);

        String search  = txtSearch.getText().trim();
        String from    = txtFrom.getText().trim();
        String to      = txtTo.getText().trim();
        String custSel = selectedOrNull(cmbCustomer, "All Customers");
        String prodSel = selectedOrNull(cmbProduct,  "All Products");

        StringBuilder sql = new StringBuilder(
            "SELECT sd.sale_id, sd.sale_date, " +
            "  COALESCE(c.customer_name,'—') AS customer_name, " +
            "  COALESCE(p.name,'—')          AS product_name, " +
            "  sd.qty, sd.rate, sd.total_amt, sd.vat_pct, sd.vat_amt, " +
            "  sd.discount_pct, sd.discount_amt, sd.net_amt, sd.narration " +
            "FROM sale_details sd " +
            "LEFT JOIN customers c ON sd.customer_id = c.id " +
            "LEFT JOIN products  p ON sd.product_id  = p.id " +
            "WHERE 1=1");

        // Global text search across customer name and product name
        if (!search.isEmpty()) {
            String s = search.replace("'", "''").toUpperCase();
            sql.append(" AND (UPPER(c.customer_name) LIKE '%").append(s)
               .append("%' OR UPPER(p.name) LIKE '%").append(s)
               .append("%' OR UPPER(sd.narration) LIKE '%").append(s).append("%')");
        }

        // Date range — sale_date stored as VARCHAR2 in dd-MM-yyyy format
        // Use TO_DATE for proper comparison
        if (!from.isEmpty()) {
            String f = from.replace("'", "''");
            sql.append(" AND TO_DATE(sd.sale_date,'DD-MM-YYYY') >= TO_DATE('").append(f).append("','DD-MM-YYYY')");
        }
        if (!to.isEmpty()) {
            String t = to.replace("'", "''");
            sql.append(" AND TO_DATE(sd.sale_date,'DD-MM-YYYY') <= TO_DATE('").append(t).append("','DD-MM-YYYY')");
        }

        // Customer filter
        if (custSel != null) {
            String idStr = custSel.split(" — ")[0].trim();
            sql.append(" AND sd.customer_id = ").append(idStr);
        }

        // Product filter
        if (prodSel != null) {
            String idStr = prodSel.split(" — ")[0].trim();
            sql.append(" AND sd.product_id = ").append(idStr);
        }

        sql.append(" ORDER BY sd.sale_id DESC");

        double grandNet = 0;
        int count = 0;
        try (Statement st = MdiForm.sconnect.createStatement();
             ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                double net = rs.getDouble("net_amt");
                grandNet += net;
                model.addRow(new Object[]{
                    rs.getLong  ("sale_id"),
                    rs.getString("sale_date"),
                    rs.getString("customer_name"),
                    rs.getString("product_name"),
                    rs.getLong  ("qty"),
                    String.format("%.2f", rs.getDouble("rate")),
                    String.format("%.2f", rs.getDouble("total_amt")),
                    String.format("%.2f", rs.getDouble("vat_pct")),
                    String.format("%.2f", rs.getDouble("vat_amt")),
                    String.format("%.2f", rs.getDouble("discount_pct")),
                    String.format("%.2f", rs.getDouble("discount_amt")),
                    String.format("%.2f", net),
                    rs.getString("narration")
                });
                count++;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }

        lblCount.setText(count + " record" + (count != 1 ? "s" : ""));
        lblTotal.setText("Net Total: ₹" + String.format("%,.2f", grandNet));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────
    private String selectedOrNull(JComboBox<String> cmb, String allLabel) {
        String s = (String) cmb.getSelectedItem();
        return (s == null || s.equals(allLabel)) ? null : s;
    }

    private JComboBox<String> filterCombo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setBackground(new Color(28, 40, 68));
        cb.setForeground(TEXT_MAIN);
        cb.setPreferredSize(new Dimension(200, 28));
        return cb;
    }

    private void addLabel(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MUTED);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(l);
    }

    private void styleTextField(JTextField tf) {
        tf.setBackground(BG_DARK); tf.setForeground(TEXT_MAIN);
        tf.setCaretColor(ACCENT);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
    }

    private JButton makeButton(String text, Color fg) {
        JButton b = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? fg.darker() : fg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };
        b.setForeground(BG_DARK); b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(90, 32));
        return b;
    }

    private void styleTable(JTable t) {
        t.setBackground(BG_DARK); t.setForeground(TEXT_MAIN);
        t.setGridColor(new Color(30, 41, 59));
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(34);
        t.setSelectionBackground(new Color(52, 211, 153, 55));
        t.setSelectionForeground(TEXT_MAIN);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader th = t.getTableHeader();
        th.setBackground(BG_CARD); th.setForeground(ACCENT);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT));

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                setForeground(TEXT_MAIN);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }
}