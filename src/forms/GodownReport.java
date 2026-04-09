package forms;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.print.*;
import java.sql.*;
import static ui.UIConstants.*;

public class GodownReport extends JInternalFrame implements Printable {

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;

    // Summary stat labels
    private JLabel lblTotalGodowns, lblActiveGodowns, lblArchivedGodowns, lblTotalCapacity;

    // Filter combos
    private JComboBox<String> cmbState, cmbStatus;

    public GodownReport() {
        super("Godown Report", true, true, true, true);
        setSize(1100, 680);
        setLayout(new BorderLayout());

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(8, 44, 44), getWidth(), 0, new Color(14, 30, 50)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel icon = new JLabel("🏭");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        JLabel title = new JLabel("Godown Report");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_MAIN);
        JLabel sub = new JLabel("Complete warehouse & godown directory report");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(150, 230, 230));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title); titleBox.add(sub);
        JPanel leftH = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftH.setOpaque(false); leftH.add(icon); leftH.add(titleBox);
        header.add(leftH, BorderLayout.WEST);

        JButton btnPrint = new JButton("🖨 Print") {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? WARNING.darker() : WARNING);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
            }
        };
        btnPrint.setForeground(BG_DARK); btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrint.setOpaque(false); btnPrint.setContentAreaFilled(false); btnPrint.setBorderPainted(false);
        btnPrint.setPreferredSize(new Dimension(110, 36));
        btnPrint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel rightH = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightH.setOpaque(false); rightH.add(btnPrint);
        header.add(rightH, BorderLayout.EAST);

        // ── Summary Cards ───────────────────────────────────────────────────
        JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        cards.setBackground(BG_DARK);
        cards.setBorder(new EmptyBorder(4, 12, 0, 12));
        lblTotalGodowns   = makeSummaryCard(cards, "🏭 Total Godowns",    "0",  ACCENT);
        lblActiveGodowns  = makeSummaryCard(cards, "✅ Active",            "0",  SUCCESS);
        lblArchivedGodowns= makeSummaryCard(cards, "🗄 Archived",          "0",  DANGER);
        lblTotalCapacity  = makeSummaryCard(cards, "📦 Total Capacity",    "0",  WARNING);

        // ── Toolbar ─────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(51, 65, 85)));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row1.setBackground(BG_CARD);
        JLabel lSearch = new JLabel("🔍 Filter:");
        lSearch.setForeground(TEXT_MUTED);
        lSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch = new JTextField(26); styleTextField(txtSearch);
        JButton btnLoad  = makeButton("Generate", ACCENT);
        JButton btnClear = makeButton("Clear",    new Color(100, 116, 139));
        row1.add(lSearch); row1.add(txtSearch);
        row1.add(Box.createHorizontalStrut(6));
        row1.add(btnLoad); row1.add(btnClear);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        row2.setBackground(BG_CARD);
        cmbState  = filterCombo("All States");
        cmbStatus = filterCombo("All", "Active", "Archived");
        row2.add(filterLabel("State:"));  row2.add(cmbState);
        row2.add(Box.createHorizontalStrut(8));
        row2.add(filterLabel("Status:")); row2.add(cmbStatus);

        toolbar.add(row1, BorderLayout.NORTH);
        toolbar.add(row2, BorderLayout.SOUTH);

        // ── Table ─────────────────────────────────────────────────────────
        // All columns from godowns table
        String[] cols = {
            "ID", "Godown Name", "Code", "Manager", "Phone",
            "Email", "City", "State", "Capacity", "Status"
        };
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable(table);

        int[] widths = {48, 175, 90, 130, 105, 175, 90, 100, 70, 70};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Status column colour
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                    setForeground("Active".equals(v) ? SUCCESS : DANGER);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        // Capacity column colour
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                    setForeground(ACCENT);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

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
        center.add(topCenter,  BorderLayout.NORTH);
        center.add(scroll,     BorderLayout.CENTER);
        center.add(statusBar,  BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        getContentPane().setBackground(BG_DARK);

        // Wire actions
        btnLoad .addActionListener(e -> loadReport());
        btnClear.addActionListener(e -> { txtSearch.setText(""); cmbState.setSelectedIndex(0); cmbStatus.setSelectedIndex(0); loadReport(); });
        btnPrint.addActionListener(e -> printReport());
        cmbState .addActionListener(e -> loadReport());
        cmbStatus.addActionListener(e -> loadReport());

        populateStateCombos();
        loadReport();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Populate state combo from DB
    // ─────────────────────────────────────────────────────────────────────
    private void populateStateCombos() {
        try (Statement st = MdiForm.sconnect.createStatement()) {
            ResultSet rs = st.executeQuery(
                "SELECT DISTINCT state FROM godowns WHERE state IS NOT NULL ORDER BY state");
            while (rs.next()) cmbState.addItem(rs.getString(1));
        } catch (SQLException ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // Load report data from godowns table
    // ─────────────────────────────────────────────────────────────────────
    private void loadReport() {
        model.setRowCount(0);

        String search = txtSearch.getText().trim();
        String state  = selectedOrNull(cmbState,  "All States");
        String status = selectedOrNull(cmbStatus, "All");

        StringBuilder sql = new StringBuilder(
            "SELECT id, godown_name, godown_code, manager_name, phone, email, " +
            "city, state, capacity, is_active FROM godowns WHERE 1=1");

        if (!search.isEmpty()) {
            String s = search.replace("'", "''").toUpperCase();
            sql.append(" AND (UPPER(godown_name) LIKE '%").append(s)
               .append("%' OR UPPER(godown_code) LIKE '%").append(s)
               .append("%' OR UPPER(manager_name) LIKE '%").append(s).append("%')");
        }
        if (state  != null) sql.append(" AND UPPER(state) = UPPER('").append(state.replace("'","''")).append("')");
        if ("Active".equals(status))   sql.append(" AND is_active = 1");
        if ("Archived".equals(status)) sql.append(" AND is_active = 0");

        sql.append(" ORDER BY id");

        int total = 0, active = 0, archived = 0, totalCap = 0;

        try (Statement st = MdiForm.sconnect.createStatement();
             ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                int isActive = rs.getInt("is_active");
                int cap      = rs.getInt("capacity");
                String statusStr = isActive == 1 ? "Active" : "Archived";
                model.addRow(new Object[]{
                    rs.getLong  ("id"),
                    rs.getString("godown_name"),
                    rs.getString("godown_code"),
                    rs.getString("manager_name"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getString("city"),
                    rs.getString("state"),
                    cap,
                    statusStr
                });
                total++;
                if (isActive == 1) active++; else archived++;
                totalCap += cap;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }

        lblTotalGodowns   .setText(String.valueOf(total));
        lblActiveGodowns  .setText(String.valueOf(active));
        lblArchivedGodowns.setText(String.valueOf(archived));
        lblTotalCapacity  .setText(String.format("%,d", totalCap) + " units");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Print
    // ─────────────────────────────────────────────────────────────────────
    private void printReport() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) { JOptionPane.showMessageDialog(this, ex.getMessage()); }
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
        cb.setPreferredSize(new Dimension(150, 28));
        return cb;
    }

    private JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(TEXT_MUTED);
        return l;
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
        card.setOpaque(false); card.setPreferredSize(new Dimension(210, 62));
        card.setLayout(new BorderLayout(4, 2)); card.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11)); lbl.setForeground(TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 18)); val.setForeground(accent);
        card.add(lbl, BorderLayout.NORTH); card.add(val, BorderLayout.CENTER);
        parent.add(card); return val;
    }

    private void styleTextField(JTextField tf) {
        tf.setBackground(BG_DARK); tf.setForeground(TEXT_MAIN); tf.setCaretColor(ACCENT);
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8); super.paintComponent(g);
            }
        };
        b.setForeground(BG_DARK); b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100, 32)); return b;
    }

    private void styleTable(JTable t) {
        t.setBackground(BG_DARK); t.setForeground(TEXT_MAIN);
        t.setGridColor(new Color(30, 41, 59));
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(34);
        t.setSelectionBackground(new Color(50, 200, 200, 55));
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