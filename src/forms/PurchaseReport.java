package forms;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.print.*;
import java.sql.*;
import static ui.UIConstants.*;

public class PurchaseReport extends JInternalFrame implements Printable {

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtFrom, txtTo;
    private JLabel lblCount, lblTotal, lblVat, lblNet;

    public PurchaseReport() {
        super("Purchase Report", true, true, true, true);
        setSize(1250, 700);
        setLayout(new BorderLayout());

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(67, 20, 7), getWidth(), 0, new Color(124, 45, 18));
                g2.setPaint(gp); g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel icon = new JLabel("📉");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        JLabel title = new JLabel("Purchase Report");
        title.setFont(new Font("Trebuchet MS", Font.BOLD, 24));
        title.setForeground(TEXT_MAIN);
        JLabel sub = new JLabel("Comprehensive purchase transaction report");
        sub.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        sub.setForeground(new Color(254, 215, 170));

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
        JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        cards.setBackground(BG_DARK);
        cards.setBorder(new EmptyBorder(4, 12, 0, 12));
        lblCount = makeSummaryCard(cards, "🧾 Total Orders", "0",      new Color(56, 189, 248));
        lblTotal = makeSummaryCard(cards, "💵 Gross Total",  "₹0.00",  ACCENT);
        lblVat   = makeSummaryCard(cards, "🏛 Total VAT",    "₹0.00",  new Color(167, 139, 250));
        lblNet   = makeSummaryCard(cards, "✅ Net Cost",     "₹0.00",  WARNING);

        // ── Toolbar ─────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(51, 65, 85)));

        txtFrom = new JTextField(12); styleTextField(txtFrom); txtFrom.setText("DD-MM-YYYY");
        txtTo   = new JTextField(12); styleTextField(txtTo);   txtTo.setText("DD-MM-YYYY");

        addLabel(toolbar, "From (DD-MM-YYYY):"); toolbar.add(txtFrom);
        addLabel(toolbar, "To (DD-MM-YYYY):");   toolbar.add(txtTo);
        toolbar.add(Box.createHorizontalStrut(8));
        JButton btnLoad  = makeButton("Generate", ACCENT);
        JButton btnClear = makeButton("Clear", new Color(100, 116, 139));
        toolbar.add(btnLoad); toolbar.add(btnClear);

        // ── Table ─────────────────────────────────────────────────────────
        // Columns match actual purchase_details + JOIN on suppliers & products:
        // purchase_id | purchase_date | supplier_name | product_name | SKU |
        // Qty | Rate | Total Amt | VAT Amt | Con Amt | Net Amt | Narration
        String[] cols = {
            "Purchase ID", "Date", "Supplier Name",
            "Product Name", "SKU", "Qty", "Rate (₹)",
            "Total Amt", "VAT Amt", "Con Amt", "Net Amt", "Narration"
        };
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable(table);

        // Right-align numeric columns
        DefaultTableCellRenderer rAlign = new DefaultTableCellRenderer();
        rAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{5, 6, 7, 8, 9, 10})
            table.getColumnModel().getColumn(c).setCellRenderer(rAlign);

        // Column widths
        int[] widths = {90, 100, 170, 200, 100, 50, 90, 90, 80, 80, 100, 200};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_DARK); scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        statusBar.setBackground(BG_DARK);
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(51, 65, 85)));

        add(header, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout()); center.setBackground(BG_DARK);
        JPanel topCenter = new JPanel(new BorderLayout()); topCenter.setBackground(BG_DARK);
        topCenter.add(cards,   BorderLayout.NORTH);
        topCenter.add(toolbar, BorderLayout.SOUTH);
        center.add(topCenter, BorderLayout.NORTH);
        center.add(scroll,    BorderLayout.CENTER);
        center.add(statusBar, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        getContentPane().setBackground(BG_DARK);

        btnLoad .addActionListener(e -> loadReport());
        btnClear.addActionListener(e -> {
            txtFrom.setText("DD-MM-YYYY"); txtTo.setText("DD-MM-YYYY");
            model.setRowCount(0); resetCards();
        });
        btnPrint.addActionListener(e -> printReport());

        loadReport();
        setVisible(true);
    }

    // ── Core query ────────────────────────────────────────────────────────
    // JOIN suppliers + products to resolve names.
    // All amounts summed for the 4 summary cards.
    private void loadReport() {
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

            String from = txtFrom.getText().trim();
            String to   = txtTo.getText().trim();

            if (!from.isEmpty() && !from.equals("DD-MM-YYYY")) {
                sql.append("AND TO_DATE(pd.purchase_date,'DD-MM-YYYY') >= TO_DATE('")
                   .append(from).append("','DD-MM-YYYY') ");
            }
            if (!to.isEmpty() && !to.equals("DD-MM-YYYY")) {
                sql.append("AND TO_DATE(pd.purchase_date,'DD-MM-YYYY') <= TO_DATE('")
                   .append(to).append("','DD-MM-YYYY') ");
            }

            sql.append("ORDER BY TO_DATE(pd.purchase_date,'DD-MM-YYYY') DESC");

            double grossTotal = 0, vatTotal = 0, netTotal = 0;
            int count = 0;

            try (Statement  st = MdiForm.sconnect.createStatement();
                 ResultSet  rs = st.executeQuery(sql.toString())) {
                while (rs.next()) {
                    double total = rs.getDouble("total_amt");
                    double vat   = rs.getDouble("vat_amt");
                    double con   = rs.getDouble("con_amt");
                    double net   = rs.getDouble("net_amt");
                    grossTotal += total; vatTotal += vat; netTotal += net;
                    count++;
                    model.addRow(new Object[]{
                        rs.getString("purchase_id"),
                        rs.getString("purchase_date"),
                        rs.getString("supplier_name"),
                        rs.getString("product_name"),
                        rs.getString("sku"),
                        rs.getDouble("qty"),
                        String.format("%.2f", rs.getDouble("rate")),
                        String.format("%.2f", total),
                        String.format("%.2f", vat),
                        String.format("%.2f", con),
                        String.format("%.2f", net),
                        rs.getString("narration")
                    });
                }
            }
            lblCount.setText(String.valueOf(count));
            lblTotal.setText("₹" + String.format("%,.2f", grossTotal));
            lblVat  .setText("₹" + String.format("%,.2f", vatTotal));
            lblNet  .setText("₹" + String.format("%,.2f", netTotal));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(),
                "Report Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetCards() {
        lblCount.setText("0"); lblTotal.setText("₹0.00");
        lblVat.setText("₹0.00"); lblNet.setText("₹0.00");
    }

    // ── Print ─────────────────────────────────────────────────────────────
    private void printReport() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex) {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        double sw    = pf.getImageableWidth();
        double scale = sw / table.getWidth();
        if (scale < 1) g2.scale(scale, scale);
        table.paint(g2);
        return PAGE_EXISTS;
    }

    // ── UI helpers ────────────────────────────────────────────────────────
    private JLabel makeSummaryCard(JPanel parent, String label, String value, Color accent) {
        JPanel card = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accent); g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
            }
        };
        card.setOpaque(false); card.setPreferredSize(new Dimension(210, 60));
        card.setLayout(new BorderLayout(4, 2)); card.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Trebuchet MS", Font.PLAIN, 11)); lbl.setForeground(TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Trebuchet MS", Font.BOLD, 18)); val.setForeground(accent);
        card.add(lbl, BorderLayout.NORTH); card.add(val, BorderLayout.CENTER);
        parent.add(card); return val;
    }

    private void addLabel(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MUTED);
        l.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        p.add(l);
    }

    private void styleTextField(JTextField tf) {
        tf.setBackground(BG_DARK); tf.setForeground(TEXT_MAIN); tf.setCaretColor(ACCENT);
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
        b.setForeground(Color.WHITE); b.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100, 32));
        return b;
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
        b.setForeground(BG_DARK); b.setFont(new Font("Trebuchet MS", Font.BOLD, 13));
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(110, 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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