package forms;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;
import static ui.UIConstants.*;

public class EmployeeQuery extends JInternalFrame {

    private JTextField txtSearch;
    private JComboBox<String> cmbFilter;
    private JTable table;
    private DefaultTableModel model;
    private JLabel lblCount;

    public EmployeeQuery() {
        super("Employee Query", true, true, true, true);
        setSize(1000, 620);
        setLayout(new BorderLayout(0, 0));

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 58, 138), getWidth(), 0, new Color(88, 28, 135));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 72));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel icon = new JLabel("👥");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel title = new JLabel("Employee Query");
        title.setFont(new Font("Trebuchet MS", Font.BOLD, 22));
        title.setForeground(TEXT_MAIN);
        JLabel sub = new JLabel("Search & explore employee records");
        sub.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        sub.setForeground(new Color(196, 181, 253));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title);
        titleBox.add(sub);

        JPanel leftH = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftH.setOpaque(false);
        leftH.add(icon);
        leftH.add(titleBox);
        header.add(leftH, BorderLayout.WEST);

        lblCount = new JLabel("0 records");
        lblCount.setFont(new Font("Trebuchet MS", Font.BOLD, 13));
        lblCount.setForeground(ACCENT);
        header.add(lblCount, BorderLayout.EAST);

        // ── Toolbar ─────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(51, 65, 85)));

        JLabel lSearch = new JLabel("Search:");
        lSearch.setForeground(TEXT_MUTED);
        lSearch.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));

        txtSearch = new JTextField(22);
        styleTextField(txtSearch);

        JLabel lFilter = new JLabel("Filter by:");
        lFilter.setForeground(TEXT_MUTED);
        lFilter.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));

        cmbFilter = new JComboBox<>(new String[]{"All Fields", "Emp ID", "Name", "Gender", "Email"});
        styleCombo(cmbFilter);

        JButton btnSearch = makeButton("🔍 Search", ACCENT, BG_DARK);
        JButton btnClear  = makeButton("✕ Clear",  new Color(100, 116, 139), BG_CARD);
        JButton btnExport = makeButton("⬇ Export", SUCCESS, BG_DARK);

        toolbar.add(lSearch);
        toolbar.add(txtSearch);
        toolbar.add(lFilter);
        toolbar.add(cmbFilter);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(btnSearch);
        toolbar.add(btnClear);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(btnExport);

        // ── Table ────────────────────────────────────────────────────────────
        String[] cols = {"Emp ID", "Name", "Gender", "DOB", "DOJ", "Phone", "Mobile", "Email", "Basic Salary", "Narration"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // ── Status Bar ────────────────────────────────────────────────────────
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        statusBar.setBackground(new Color(15, 23, 42));
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(51, 65, 85)));

        // ── Assemble ─────────────────────────────────────────────────────────
        add(header, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG_DARK);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        center.add(statusBar, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        getContentPane().setBackground(BG_DARK);

        // ── Actions ──────────────────────────────────────────────────────────
        btnSearch.addActionListener(e -> loadData(txtSearch.getText().trim(), (String) cmbFilter.getSelectedItem()));
        btnClear.addActionListener(e -> { txtSearch.setText(""); loadData("", "All Fields"); });
        txtSearch.addActionListener(e -> btnSearch.doClick());

        loadData("", "All Fields");
        setVisible(true);
    }

    private void loadData(String search, String filter) {
        model.setRowCount(0);
        try {
            String sql = "SELECT emp_id, emp_name, gender, dob, doj, phone, mobile_no, email_id, bsalary, narration FROM employee_details";
            if (!search.isEmpty()) {
                switch (filter) {
                    case "Emp ID":   sql += " WHERE UPPER(emp_id)    LIKE UPPER('%" + search + "%')"; break;
                    case "Name":     sql += " WHERE UPPER(emp_name)   LIKE UPPER('%" + search + "%')"; break;
                    case "Gender":   sql += " WHERE UPPER(gender)     LIKE UPPER('%" + search + "%')"; break;
                    case "Email":    sql += " WHERE UPPER(email_id)   LIKE UPPER('%" + search + "%')"; break;
                    default:         sql += " WHERE UPPER(emp_id) LIKE UPPER('%" + search + "%')"
                                          + " OR UPPER(emp_name) LIKE UPPER('%" + search + "%')"
                                          + " OR UPPER(email_id) LIKE UPPER('%" + search + "%')";
                }
            }
            sql += " ORDER BY emp_id";
            try (Statement st = MdiForm.sconnect.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                int count = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("emp_id"), rs.getString("emp_name"), rs.getString("gender"),
                        rs.getString("dob"), rs.getString("doj"), rs.getString("phone"),
                        rs.getString("mobile_no"), rs.getString("email_id"),
                        String.format("₹%.2f", rs.getDouble("bsalary")), rs.getString("narration")
                    });
                    count++;
                }
                lblCount.setText(count + " record" + (count != 1 ? "s" : ""));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }

    // ── Styling helpers ──────────────────────────────────────────────────────
    private void styleTextField(JTextField tf) {
        tf.setBackground(BG_DARK);
        tf.setForeground(TEXT_MAIN);
        tf.setCaretColor(ACCENT);
        tf.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(BG_DARK);
        cb.setForeground(TEXT_MAIN);
        cb.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        cb.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));
    }

    private JButton makeButton(String text, Color fg, Color bg) {
        JButton b = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())      g2.setColor(fg.darker().darker());
                else if (getModel().isRollover()) g2.setColor(fg.darker());
                else                              g2.setColor(fg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(110, 32));
        return b;
    }

    private void styleTable(JTable t) {
        t.setBackground(BG_DARK);
        t.setForeground(TEXT_MAIN);
        t.setGridColor(new Color(30, 41, 59));
        t.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        t.setRowHeight(34);
        t.setSelectionBackground(new Color(56, 189, 248, 60));
        t.setSelectionForeground(TEXT_MAIN);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader th = t.getTableHeader();
        th.setBackground(BG_CARD);
        th.setForeground(ACCENT);
        th.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        th.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT));
        th.setReorderingAllowed(false);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                setForeground(TEXT_MAIN);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }
}