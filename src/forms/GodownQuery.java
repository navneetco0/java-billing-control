package forms;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;
import static ui.UIConstants.*;

public class GodownQuery extends JInternalFrame {

    private JTextField txtSearch;
    private JTable table;
    private DefaultTableModel model;
    private JLabel lblCount;

    // Filter combos
    private JComboBox<String> cmbCity, cmbState, cmbStatus;

    public GodownQuery() {
        super("Godown Query", true, true, true, true);
        setSize(1060, 600);
        setLayout(new BorderLayout());

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(8, 44, 44), getWidth(), 0, new Color(10, 14, 26)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        header.setPreferredSize(new Dimension(0, 72));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel icon = new JLabel("🏭");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel title = new JLabel("Godown Query");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_MAIN);
        JLabel sub = new JLabel("Search & explore warehouse / godown records");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(150, 230, 230));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title); titleBox.add(sub);
        JPanel leftH = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftH.setOpaque(false); leftH.add(icon); leftH.add(titleBox);
        header.add(leftH, BorderLayout.WEST);

        lblCount = new JLabel("0 records");
        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCount.setForeground(ACCENT);
        JPanel rightH = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightH.setOpaque(false); rightH.add(lblCount);
        header.add(rightH, BorderLayout.EAST);

        // ── Toolbar row 1: global search ──────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(51, 65, 85)));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row1.setBackground(BG_CARD);
        JLabel lSearch = new JLabel("🔍 Search:");
        lSearch.setForeground(TEXT_MUTED);
        lSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch = new JTextField(26); styleTextField(txtSearch);
        JButton btnSearch = makeButton("Search", ACCENT);
        JButton btnClear  = makeButton("Clear",  new Color(100, 116, 139));
        row1.add(lSearch); row1.add(txtSearch);
        row1.add(Box.createHorizontalStrut(6));
        row1.add(btnSearch); row1.add(btnClear);

        // ── Toolbar row 2: filter combos ────────────────────────────────
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        row2.setBackground(BG_CARD);

        cmbCity   = filterCombo("All Cities");
        cmbState  = filterCombo("All States");
        cmbStatus = filterCombo("All", "Active", "Archived");

        row2.add(filterLabel("City:")); row2.add(cmbCity);
        row2.add(Box.createHorizontalStrut(6));
        row2.add(filterLabel("State:")); row2.add(cmbState);
        row2.add(Box.createHorizontalStrut(6));
        row2.add(filterLabel("Status:")); row2.add(cmbStatus);

        toolbar.add(row1, BorderLayout.NORTH);
        toolbar.add(row2, BorderLayout.SOUTH);

        // ── Table ────────────────────────────────────────────────────────
        // Columns match godowns table:
        // id, godown_name, godown_code, manager_name, phone, email,
        // city, state, capacity, status
        String[] cols = {
            "ID", "Godown Name", "Code", "Manager", "Phone",
            "Email", "City", "State", "Capacity", "Status"
        };
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable(table);

        // Column widths
        int[] widths = {48, 180, 90, 130, 100, 175, 90, 100, 70, 70};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Status column colour renderer
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

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // Status bar
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
            txtSearch.setText("");
            cmbCity  .setSelectedIndex(0);
            cmbState .setSelectedIndex(0);
            cmbStatus.setSelectedIndex(0);
            loadData();
        });
        txtSearch.addActionListener(e -> btnSearch.doClick());
        cmbCity  .addActionListener(e -> loadData());
        cmbState .addActionListener(e -> loadData());
        cmbStatus.addActionListener(e -> loadData());

        populateFilterCombos();
        loadData();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Populate city / state combos from DB
    // ─────────────────────────────────────────────────────────────────────
    private void populateFilterCombos() {
        try (Statement st = MdiForm.sconnect.createStatement()) {
            ResultSet rs = st.executeQuery(
                "SELECT DISTINCT city  FROM godowns WHERE city  IS NOT NULL ORDER BY city");
            while (rs.next()) cmbCity.addItem(rs.getString(1));

            rs = st.executeQuery(
                "SELECT DISTINCT state FROM godowns WHERE state IS NOT NULL ORDER BY state");
            while (rs.next()) cmbState.addItem(rs.getString(1));
        } catch (SQLException ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // Load data with filters
    // ─────────────────────────────────────────────────────────────────────
    private void loadData() {
        model.setRowCount(0);

        String search = txtSearch.getText().trim();
        String city   = selectedOrNull(cmbCity,   "All Cities");
        String state  = selectedOrNull(cmbState,  "All States");
        String status = selectedOrNull(cmbStatus, "All");

        StringBuilder sql = new StringBuilder(
            "SELECT id, godown_name, godown_code, manager_name, phone, email, " +
            "city, state, capacity, is_active " +
            "FROM godowns WHERE 1=1");

        if (!search.isEmpty()) {
            String s = search.replace("'", "''").toUpperCase();
            sql.append(" AND (UPPER(godown_name) LIKE '%").append(s)
               .append("%' OR UPPER(godown_code) LIKE '%").append(s)
               .append("%' OR UPPER(manager_name) LIKE '%").append(s).append("%')");
        }
        if (city  != null) sql.append(" AND UPPER(city)  = UPPER('").append(city .replace("'","''")).append("')");
        if (state != null) sql.append(" AND UPPER(state) = UPPER('").append(state.replace("'","''")).append("')");
        if ("Active".equals(status))   sql.append(" AND is_active = 1");
        if ("Archived".equals(status)) sql.append(" AND is_active = 0");

        sql.append(" ORDER BY id");

        int count = 0;
        try (Statement st = MdiForm.sconnect.createStatement();
             ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getLong  ("id"),
                    rs.getString("godown_name"),
                    rs.getString("godown_code"),
                    rs.getString("manager_name"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getString("city"),
                    rs.getString("state"),
                    rs.getInt   ("capacity"),
                    rs.getInt   ("is_active") == 1 ? "Active" : "Archived"
                });
                count++;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
        lblCount.setText(count + " record" + (count != 1 ? "s" : ""));
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
        cb.setPreferredSize(new Dimension(140, 28));
        return cb;
    }

    private JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(TEXT_MUTED);
        return l;
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