package forms;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.print.*;
import java.sql.*;
import static ui.UIConstants.*;

public class SaleReport extends JInternalFrame implements Printable {

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtFrom, txtTo;
    private JComboBox<String> cmbCustomer, cmbProduct;

    // Summary stat labels
    private JLabel lblCount, lblGross, lblVat, lblDiscount, lblNet;


    public SaleReport() {
        super("Sale Report", true, true, true, true);
        setSize(1200, 700);
        setLayout(new BorderLayout());

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(5, 46, 22), getWidth(), 0, new Color(6, 95, 70)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel icon = new JLabel("📈");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        JLabel title = new JLabel("Sale Report");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_MAIN);
        JLabel sub = new JLabel("Comprehensive sale transaction report with totals");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(167, 243, 208));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title); titleBox.add(sub);
        JPanel leftH = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftH.setOpaque(false); leftH.add(icon); leftH.add(titleBox);
        header.add(leftH, BorderLayout.WEST);

        JButton btnPrint = makePrintButton();
        JPanel rightH = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightH.setOpaque(false); rightH.add(btnPrint);
        header.add(rightH, BorderLayout.EAST);

        // ── Summary Cards ────────────────────────────────────────────────────
        // Cards: Total Bills | Gross Total | Total VAT | Total Discount | Net Revenue
        JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        cards.setBackground(BG_DARK);
        cards.setBorder(new EmptyBorder(4, 12, 0, 12));
        lblCount    = makeSummaryCard(cards, "🧾 Total Bills",     "0",     new Color(56, 189, 248));
        lblGross    = makeSummaryCard(cards, "💵 Gross Total",     "₹0.00", ACCENT);
        lblVat      = makeSummaryCard(cards, "🏛 Total VAT",       "₹0.00", new Color(251, 146, 60));
        lblDiscount = makeSummaryCard(cards, "🏷 Total Discount",  "₹0.00", PURPLE);
        lblNet      = makeSummaryCard(cards, "✅ Net Revenue",     "₹0.00", GOLD);

        // ── Toolbar ─────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(51, 65, 85)));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row1.setBackground(BG_CARD);
        txtFrom = new JTextField(12); styleTextField(txtFrom);
        txtTo   = new JTextField(12); styleTextField(txtTo);
        txtFrom.putClientProperty("JTextField.placeholderText", "dd-MM-yyyy");
        txtTo  .putClientProperty("JTextField.placeholderText", "dd-MM-yyyy");

        addLabel(row1, "From (dd-MM-yyyy):"); row1.add(txtFrom);
        addLabel(row1, "To:");                row1.add(txtTo);
        row1.add(Box.createHorizontalStrut(8));
        JButton btnLoad  = makeButton("Generate", ACCENT);
        JButton btnClear = makeButton("Clear",    new Color(100, 116, 139));
        row1.add(btnLoad); row1.add(btnClear);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        row2.setBackground(BG_CARD);
        cmbCustomer = filterCombo("All Customers");
        cmbProduct  = filterCombo("All Products");
        addLabel(row2, "Customer:"); row2.add(cmbCustomer);
        addLabel(row2, "Product:");  row2.add(cmbProduct);

        toolbar.add(row1, BorderLayout.NORTH);
        toolbar.add(row2, BorderLayout.SOUTH);

        // ── Table ─────────────────────────────────────────────────────────
        // Columns match sale_details JOIN customers + products
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

        int[] widths = {65, 90, 140, 150, 48, 80, 85, 55, 75, 55, 75, 95, 130};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Right-align numeric columns
        DefaultTableCellRenderer rAlign = new DefaultTableCellRenderer();
        rAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{4, 5, 6, 7, 8, 9, 10, 11})
            table.getColumnModel().getColumn(c).setCellRenderer(rAlign);

        // Net column gold + bold
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
        JPanel topCenter = new JPanel(new BorderLayout());
        topCenter.setBackground(BG_DARK);
        topCenter.add(cards,   BorderLayout.NORTH);
        topCenter.add(toolbar, BorderLayout.SOUTH);
        center.add(topCenter,  BorderLayout.NORTH);
        center.add(scroll,     BorderLayout.CENTER);
        center.add(statusBar,  BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        getContentPane().setBackground(BG_DARK);

        // Wire actions
        btnLoad .addActionListener(e -> loadReport());
        btnClear.addActionListener(e -> {
            txtFrom.setText(""); txtTo.setText("");
            cmbCustomer.setSelectedIndex(0); cmbProduct.setSelectedIndex(0);
            model.setRowCount(0); resetCards();
        });
        btnPrint    .addActionListener(e -> printReport());
        cmbCustomer .addActionListener(e -> loadReport());
        cmbProduct  .addActionListener(e -> loadReport());

        populateFilterCombos();
        loadReport();
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
    // Load report — queries sale_details JOIN customers + products
    // ─────────────────────────────────────────────────────────────────────
    private void loadReport() {
        model.setRowCount(0);

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

        if (!from.isEmpty()) {
            String f = from.replace("'", "''");
            sql.append(" AND TO_DATE(sd.sale_date,'DD-MM-YYYY') >= TO_DATE('").append(f).append("','DD-MM-YYYY')");
        }
        if (!to.isEmpty()) {
            String t = to.replace("'", "''");
            sql.append(" AND TO_DATE(sd.sale_date,'DD-MM-YYYY') <= TO_DATE('").append(t).append("','DD-MM-YYYY')");
        }
        if (custSel != null) {
            String idStr = custSel.split(" — ")[0].trim();
            sql.append(" AND sd.customer_id = ").append(idStr);
        }
        if (prodSel != null) {
            String idStr = prodSel.split(" — ")[0].trim();
            sql.append(" AND sd.product_id = ").append(idStr);
        }

        sql.append(" ORDER BY sd.sale_id DESC");

        double grossTotal = 0, vatTotal = 0, discTotal = 0, netTotal = 0;
        int count = 0;

        try (Statement st = MdiForm.sconnect.createStatement();
             ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                double tot  = rs.getDouble("total_amt");
                double vat  = rs.getDouble("vat_amt");
                double disc = rs.getDouble("discount_amt");
                double net  = rs.getDouble("net_amt");
                grossTotal += tot;
                vatTotal   += vat;
                discTotal  += disc;
                netTotal   += net;
                count++;
                model.addRow(new Object[]{
                    rs.getLong  ("sale_id"),
                    rs.getString("sale_date"),
                    rs.getString("customer_name"),
                    rs.getString("product_name"),
                    rs.getLong  ("qty"),
                    String.format("%.2f", rs.getDouble("rate")),
                    String.format("%.2f", tot),
                    String.format("%.2f", rs.getDouble("vat_pct")),
                    String.format("%.2f", vat),
                    String.format("%.2f", rs.getDouble("discount_pct")),
                    String.format("%.2f", disc),
                    String.format("%.2f", net),
                    rs.getString("narration")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }

        lblCount   .setText(String.valueOf(count));
        lblGross   .setText("₹" + String.format("%,.2f", grossTotal));
        lblVat     .setText("₹" + String.format("%,.2f", vatTotal));
        lblDiscount.setText("₹" + String.format("%,.2f", discTotal));
        lblNet     .setText("₹" + String.format("%,.2f", netTotal));
    }

    private void resetCards() {
        lblCount.setText("0"); lblGross.setText("₹0.00");
        lblVat.setText("₹0.00"); lblDiscount.setText("₹0.00"); lblNet.setText("₹0.00");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Print
    // ─────────────────────────────────────────────────────────────────────
    private void printReport() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage());
            }
        }
    }

    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex) {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        double sw = pf.getImageableWidth();
        double scale = sw / table.getWidth();
        if (scale < 1) g2.scale(scale, scale);
        table.paint(g2);
        return PAGE_EXISTS;
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

    private JLabel makeSummaryCard(JPanel parent, String label, String value, Color accent) {
        JPanel card = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accent);  g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
            }
        };
        card.setOpaque(false); card.setPreferredSize(new Dimension(185, 62));
        card.setLayout(new BorderLayout(4, 2)); card.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lbl.setForeground(TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 17)); val.setForeground(accent);
        card.add(lbl, BorderLayout.NORTH); card.add(val, BorderLayout.CENTER);
        parent.add(card); return val;
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
        b.setPreferredSize(new Dimension(100, 32)); return b;
    }

    private JButton makePrintButton() {
        JButton b = new JButton("🖨 Print") {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? WARNING.darker() : WARNING);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
            }
        };
        b.setForeground(BG_DARK); b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(110, 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
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