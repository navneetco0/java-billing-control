package forms;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;
import static ui.UIConstants.*;

public class PurchaseQuery extends JInternalFrame {

    private JTextField txtSearch, txtFrom, txtTo;
    private JTable table;
    private DefaultTableModel model;
    private JLabel lblCount, lblTotal;

    public PurchaseQuery() {
        super("Purchase Query", true, true, true, true);
        setSize(1200, 640);
        setLayout(new BorderLayout());

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(120, 53, 15), getWidth(), 0, new Color(154, 52, 18));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 72));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel icon = new JLabel("📦");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel title = new JLabel("Purchase Query");
        title.setFont(new Font("Trebuchet MS", Font.BOLD, 22));
        title.setForeground(TEXT_MAIN);
        JLabel sub = new JLabel("Search & filter purchase transactions");
        sub.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        sub.setForeground(new Color(254, 215, 170));

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
        lblCount.setFont(new Font("Trebuchet MS", Font.BOLD, 13));
        lblCount.setForeground(ACCENT);
        lblTotal = new JLabel("Total: ₹0.00");
        lblTotal.setFont(new Font("Trebuchet MS", Font.BOLD, 14));
        lblTotal.setForeground(new Color(253, 224, 71));
        rightH.add(lblCount); rightH.add(Box.createHorizontalStrut(16)); rightH.add(lblTotal);
        header.add(rightH, BorderLayout.EAST);

        // ── Toolbar ─────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(51, 65, 85)));

        txtSearch = new JTextField(20); styleTextField(txtSearch);
        txtFrom   = new JTextField(12); styleTextField(txtFrom); txtFrom.setText("DD-MM-YYYY");
        txtTo     = new JTextField(12); styleTextField(txtTo);   txtTo.setText("DD-MM-YYYY");

        addLabel(toolbar, "🔍 Search:");
        toolbar.add(txtSearch);
        addLabel(toolbar, "From (DD-MM-YYYY):");
        toolbar.add(txtFrom);
        addLabel(toolbar, "To (DD-MM-YYYY):");
        toolbar.add(txtTo);
        toolbar.add(Box.createHorizontalStrut(6));
        JButton btnSearch = makeButton("Search", ACCENT);
        JButton btnClear  = makeButton("Clear",  new Color(100, 116, 139));
        toolbar.add(btnSearch);
        toolbar.add(btnClear);

        // ── Table ────────────────────────────────────────────────────────────
        // Columns now match actual purchase_details + JOIN columns:
        // purchase_id | purchase_date | supplier_name | product_name | SKU |
        // Qty | Rate | Total Amt | VAT Amt | Con Amt | Net Amt | Narration
        String[] cols = {
            "Purchase ID", "Purchase Date", "Supplier Name",
            "Product Name", "SKU", "Qty", "Rate (₹)",
            "Total Amt", "VAT Amt", "Con Amt", "Net Amt", "Narration"
        };
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable(table);

        // Right-align numeric columns: Qty, Rate, Total, VAT, Con, Net
        DefaultTableCellRenderer rAlign = new DefaultTableCellRenderer();
        rAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{5, 6, 7, 8, 9, 10})
            table.getColumnModel().getColumn(c).setCellRenderer(rAlign);

        // Column widths
        int[] widths = {90, 110, 170, 200, 100, 50, 90, 90, 80, 80, 100, 180};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_DARK); scroll.getViewport().setBackground(BG_DARK);
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

        btnSearch.addActionListener(e -> loadData(
                txtSearch.getText().trim(),
                txtFrom.getText().trim(),
                txtTo.getText().trim()));
        btnClear.addActionListener(e -> {
            txtSearch.setText(""); txtFrom.setText("DD-MM-YYYY"); txtTo.setText("DD-MM-YYYY");
            loadData("", "", "");
        });
        txtSearch.addActionListener(e -> btnSearch.doClick());

        loadData("", "", "");
        setVisible(true);
    }

    // ── Core query ────────────────────────────────────────────────────────
    // JOIN suppliers + products so we show supplier_name, product_name, sku.
    // Search works across purchase_id, supplier_name, product_name, sku.
    // Date filter uses VARCHAR comparison on 'DD-MM-YYYY' strings stored in DB.
    private void loadData(String search, String from, String to) {
        model.setRowCount(0);
        try {
            StringBuilder sql = new StringBuilder(
                "SELECT pd.purchase_id, pd.purchase_date, " +
                "  COALESCE(s.supplier_name, '—') AS supplier_name, " +
                "  COALESCE(p.name,          '—') AS product_name, " +
                "  COALESCE(p.sku,           '—') AS sku, " +
                "  pd.qty, pd.rate, pd.total_amt, pd.vat_amt, pd.con_amt, " +
                "  pd.net_amt, pd.narration " +
                "FROM purchase_details pd " +
                "LEFT JOIN suppliers s ON pd.supplier_id = s.id " +
                "LEFT JOIN products  p ON pd.product_id  = p.id " +
                "WHERE 1=1 ");

            if (!search.isEmpty()) {
                String q = search.toUpperCase().replace("'", "''"); // basic sanitise
                sql.append("AND (")
                   .append("  UPPER(TO_CHAR(pd.purchase_id)) LIKE '%").append(q).append("%' OR ")
                   .append("  UPPER(s.supplier_name)         LIKE '%").append(q).append("%' OR ")
                   .append("  UPPER(p.name)                  LIKE '%").append(q).append("%' OR ")
                   .append("  UPPER(p.sku)                   LIKE '%").append(q).append("%' ")
                   .append(") ");
            }

            // Date stored as 'DD-MM-YYYY' VARCHAR → use TO_DATE for comparison
            if (!from.isEmpty() && !from.equals("DD-MM-YYYY")) {
                sql.append("AND TO_DATE(pd.purchase_date,'DD-MM-YYYY') >= TO_DATE('")
                   .append(from).append("','DD-MM-YYYY') ");
            }
            if (!to.isEmpty() && !to.equals("DD-MM-YYYY")) {
                sql.append("AND TO_DATE(pd.purchase_date,'DD-MM-YYYY') <= TO_DATE('")
                   .append(to).append("','DD-MM-YYYY') ");
            }

            sql.append("ORDER BY TO_DATE(pd.purchase_date,'DD-MM-YYYY') DESC");

            double grandTotal = 0;
            int    count      = 0;

            try (Statement  st = MdiForm.sconnect.createStatement();
                 ResultSet  rs = st.executeQuery(sql.toString())) {
                while (rs.next()) {
                    double net = rs.getDouble("net_amt");
                    grandTotal += net;
                    model.addRow(new Object[]{
                        rs.getString("purchase_id"),
                        rs.getString("purchase_date"),
                        rs.getString("supplier_name"),
                        rs.getString("product_name"),
                        rs.getString("sku"),
                        rs.getDouble("qty"),
                        String.format("%.2f", rs.getDouble("rate")),
                        String.format("%.2f", rs.getDouble("total_amt")),
                        String.format("%.2f", rs.getDouble("vat_amt")),
                        String.format("%.2f", rs.getDouble("con_amt")),
                        String.format("%.2f", net),
                        rs.getString("narration")
                    });
                    count++;
                }
            }
            lblCount.setText(count + " record" + (count != 1 ? "s" : ""));
            lblTotal.setText("Total: ₹" + String.format("%,.2f", grandTotal));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(),
                "Query Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────
    private void addLabel(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MUTED);
        l.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        p.add(l);
    }

    private void styleTextField(JTextField tf) {
        tf.setBackground(BG_DARK); tf.setForeground(TEXT_MAIN);
        tf.setCaretColor(ACCENT);
        tf.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
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
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(90, 32));
        return b;
    }

    private void styleTable(JTable t) {
        t.setBackground(BG_DARK); t.setForeground(TEXT_MAIN);
        t.setGridColor(new Color(30, 41, 59));
        t.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        t.setRowHeight(34);
        t.setSelectionBackground(new Color(251, 146, 60, 55));
        t.setSelectionForeground(TEXT_MAIN);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader th = t.getTableHeader();
        th.setBackground(BG_CARD); th.setForeground(ACCENT);
        th.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
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