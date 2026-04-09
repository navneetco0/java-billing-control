package forms;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.print.*;
import java.sql.*;

public class EmployeeReport extends JInternalFrame implements Printable {

    // ── DB ───────────────────────────────────────────────────────────────
    private Statement stmt;
    private ResultSet rs;

    // ── Table ─────────────────────────────────────────────────────────────
    private JTable table;
    private DefaultTableModel model;

    // ── Filters ───────────────────────────────────────────────────────────
    private JComboBox<String> cmbGender, cmbSalRange;
    private JTextField        txtSearch;

    // ── Summary cards ─────────────────────────────────────────────────────
    private JLabel lblTotalEmp, lblMale, lblFemale, lblTotalSal, lblAvgSal, lblMaxSal;

    // ── Colours ───────────────────────────────────────────────────────────
    private static final Color BG_DARK    = new Color(13, 17, 28);
    private static final Color BG_CARD    = new Color(22, 30, 46);
    private static final Color BG_ROW_ALT = new Color(18, 25, 38);
    private static final Color ACCENT     = new Color(99, 179, 237);
    private static final Color ACCENT2    = new Color(72, 209, 150);
    private static final Color TEXT_MAIN  = new Color(230, 237, 248);
    private static final Color TEXT_MUTED = new Color(130, 150, 175);
    private static final Color WARNING    = new Color(250, 200, 70);
    private static final Color MALE_C     = new Color(99, 179, 237);
    private static final Color FEMALE_C   = new Color(240, 138, 175);
    private static final Color HEADER_L   = new Color(10, 60, 110);
    private static final Color HEADER_R   = new Color(20, 40, 100);

    // ── Fonts ─────────────────────────────────────────────────────────────
    private static final Font F_TITLE  = new Font("Georgia", Font.BOLD, 22);
    private static final Font F_SUB    = new Font("Georgia", Font.PLAIN, 12);
    private static final Font F_LABEL  = new Font("Verdana", Font.PLAIN, 11);
    private static final Font F_BOLD   = new Font("Verdana", Font.BOLD,  12);
    private static final Font F_CARD_V = new Font("Verdana", Font.BOLD,  20);
    private static final Font F_CARD_L = new Font("Verdana", Font.PLAIN, 10);
    private static final Font F_TBL    = new Font("Verdana", Font.PLAIN, 12);
    private static final Font F_HDR    = new Font("Verdana", Font.BOLD,  11);

    // ══════════════════════════════════════════════════════════════════════
    public EmployeeReport() {
        super("Employee Report", true, true, true, true);
        setSize(1150, 700);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BG_DARK);

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);

        ensureTableExists();
        loadReport();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Ensure employee_salary table exists  (creates it if missing)
    // ─────────────────────────────────────────────────────────────────────
    private void ensureTableExists() {
        try {
            // Check if salary table exists — if not, create a minimal one
            // so the report still opens without crashing.
            DatabaseMetaData meta = MdiForm.sconnect.getMetaData();
            ResultSet tables = meta.getTables(null, null, "EMPLOYEE_SALARY", null);
            if (!tables.next()) {
                String ddl =
                    "CREATE TABLE employee_salary (" +
                    "  sal_id       VARCHAR2(20) PRIMARY KEY, " +
                    "  emp_id       VARCHAR2(20), " +
                    "  month        VARCHAR2(20), " +
                    "  year         VARCHAR2(10), " +
                    "  bsalary      NUMBER(10,2) DEFAULT 0, " +
                    "  hra          NUMBER(10,2) DEFAULT 0, " +
                    "  da           NUMBER(10,2) DEFAULT 0, " +
                    "  ta           NUMBER(10,2) DEFAULT 0, " +
                    "  pf_deduction NUMBER(10,2) DEFAULT 0, " +
                    "  other_ded    NUMBER(10,2) DEFAULT 0, " +
                    "  net_salary   NUMBER(10,2) DEFAULT 0, " +
                    "  status       VARCHAR2(20) DEFAULT 'Pending'" +
                    ")";
                MdiForm.sconnect.createStatement().executeUpdate(ddl);
                System.out.println("employee_salary table created.");
            }
        } catch (SQLException ex) {
            // table might already exist under a different check — ignore
            System.out.println("ensureTableExists: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Header
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, HEADER_L, getWidth(), 0, HEADER_R);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        hdr.setPreferredSize(new Dimension(0, 72));
        hdr.setBorder(new EmptyBorder(0, 20, 0, 20));

        // Left: icon + title
        JLabel icon  = new JLabel("📋");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel title = new JLabel("Employee Report");
        title.setFont(F_TITLE);
        title.setForeground(TEXT_MAIN);
        JLabel sub   = new JLabel("Live data from employee_details & employee_salary");
        sub.setFont(F_SUB);
        sub.setForeground(new Color(160, 200, 240));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title);
        titleBox.add(sub);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(icon);
        left.add(titleBox);
        hdr.add(left, BorderLayout.WEST);

        // Right: Print button
        JButton btnPrint = makeButton("🖨  Print", WARNING, BG_DARK);
        btnPrint.addActionListener(e -> printReport());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(btnPrint);
        hdr.add(right, BorderLayout.EAST);

        return hdr;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Center  (summary + toolbar + table)
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(BG_DARK);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(BG_DARK);
        top.setBorder(new EmptyBorder(10, 14, 0, 14));
        top.add(buildSummaryRow());
        top.add(Box.createVerticalStrut(10));
        top.add(buildToolbar());
        top.add(Box.createVerticalStrut(6));

        center.add(top,           BorderLayout.NORTH);
        center.add(buildTable(),  BorderLayout.CENTER);
        center.add(buildFooter(), BorderLayout.SOUTH);

        return center;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Summary cards row  — pulls from BOTH tables
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 6, 8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        lblTotalEmp = addCard(row, "👤 Total Employees", "0",     new Color(99,  179, 237));
        lblMale     = addCard(row, "♂ Male",             "0",     MALE_C);
        lblFemale   = addCard(row, "♀ Female",           "0",     FEMALE_C);
        lblTotalSal = addCard(row, "💰 Total Salary",    "₹0",    ACCENT2);
        lblAvgSal   = addCard(row, "📊 Avg Salary",      "₹0",    WARNING);
        lblMaxSal   = addCard(row, "⬆ Max Salary",       "₹0",    new Color(200, 140, 255));
        return row;
    }

    private JLabel addCard(JPanel parent, String label, String val, Color accent) {
        JLabel valueLabel = new JLabel(val);
        valueLabel.setFont(F_CARD_V);
        valueLabel.setForeground(accent);

        JLabel lbl = new JLabel(label);
        lbl.setFont(F_CARD_L);
        lbl.setForeground(TEXT_MUTED);

        JPanel card = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(2, 2));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.add(lbl,        BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        parent.add(card);
        return valueLabel;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Toolbar
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(BG_CARD);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 1, 0, new Color(40, 55, 80)),
            new EmptyBorder(4, 8, 4, 8)));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        // Search
        addTBLabel(bar, "Search:");
        txtSearch = new JTextField(18);
        txtSearch.setFont(F_LABEL);
        txtSearch.setBackground(BG_DARK);
        txtSearch.setForeground(TEXT_MAIN);
        txtSearch.setCaretColor(ACCENT);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 70, 100)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        bar.add(txtSearch);

        // Gender filter
        addTBLabel(bar, "Gender:");
        cmbGender = styledCombo(new String[]{"All","Male","Female"});
        bar.add(cmbGender);

        // Salary range filter
        addTBLabel(bar, "Salary:");
        cmbSalRange = styledCombo(new String[]{"All","0–20k","20k–50k","50k+"});
        bar.add(cmbSalRange);

        bar.add(Box.createHorizontalStrut(6));
        JButton btnLoad  = makeButton("Load / Refresh", ACCENT,  Color.WHITE);
        JButton btnClear = makeButton("Clear Filters",  new Color(60,75,100), TEXT_MUTED);
        bar.add(btnLoad);
        bar.add(btnClear);

        btnLoad .addActionListener(e -> loadReport());
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbGender.setSelectedIndex(0);
            cmbSalRange.setSelectedIndex(0);
            loadReport();
        });

        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Table
    // ─────────────────────────────────────────────────────────────────────
    private JScrollPane buildTable() {
        // Columns come purely from employee_details
        // (no salary sub-table needed — basic salary is on the employee record)
        String[] cols = {
            "#", "Emp ID", "Name", "Gender", "Date of Birth",
            "Date of Joining", "Phone", "Mobile", "Email",
            "Basic Salary (₹)", "Address", "Narration"
        };

        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setFont(F_TBL);
        table.setBackground(BG_DARK);
        table.setForeground(TEXT_MAIN);
        table.setGridColor(new Color(28, 40, 60));
        table.setRowHeight(30);
        table.setSelectionBackground(new Color(40, 80, 140, 120));
        table.setSelectionForeground(TEXT_MAIN);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoCreateRowSorter(true);  // ← clickable sort on any column

        // Header
        JTableHeader th = table.getTableHeader();
        th.setFont(F_HDR);
        th.setBackground(BG_CARD);
        th.setForeground(ACCENT);
        th.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT));
        th.setReorderingAllowed(false);

        // Alternating row renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                setForeground(TEXT_MAIN);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        // Gender column badge renderer (col index 3)
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                String v = val == null ? "" : val.toString();
                JLabel badge = new JLabel(v, SwingConstants.CENTER);
                badge.setFont(new Font("Verdana", Font.BOLD, 10));
                badge.setOpaque(true);
                boolean female = "Female".equalsIgnoreCase(v);
                badge.setBackground(female ? new Color(80, 30, 50) : new Color(20, 50, 90));
                badge.setForeground(female ? FEMALE_C : MALE_C);
                badge.setBorder(new EmptyBorder(3, 10, 3, 10));
                JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
                p.setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                p.add(badge);
                return p;
            }
        });

        // Salary column right-align (col index 9)
        DefaultTableCellRenderer rAlign = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (!sel) setBackground(row % 2 == 0 ? BG_DARK : BG_ROW_ALT);
                setForeground(ACCENT2);
                setBorder(new EmptyBorder(0, 8, 0, 12));
                return this;
            }
        };
        table.getColumnModel().getColumn(9).setCellRenderer(rAlign);

        // Serial number column narrow
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(0).setMinWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(9).setPreferredWidth(120);

        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(BG_DARK);
        sp.getViewport().setBackground(BG_DARK);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setBackground(BG_CARD);

        return sp;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Footer status bar
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 5));
        foot.setBackground(BG_CARD);
        foot.setBorder(new MatteBorder(1, 0, 0, 0, new Color(40, 55, 80)));
        JLabel info = new JLabel("Data source: employee_details table  |  Click any column header to sort");
        info.setFont(F_LABEL);
        info.setForeground(TEXT_MUTED);
        foot.add(info);
        return foot;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Load data — queries employee_details directly
    // Summary stats computed in-memory from loaded rows
    // ─────────────────────────────────────────────────────────────────────
    private void loadReport() {
        model.setRowCount(0);

        // Build WHERE clause from filters
        String searchText = txtSearch  != null ? txtSearch.getText().trim()            : "";
        String gender     = cmbGender  != null ? (String) cmbGender.getSelectedItem()  : "All";
        String salRange   = cmbSalRange != null ? (String) cmbSalRange.getSelectedItem(): "All";

        StringBuilder sql = new StringBuilder(
            "SELECT emp_id, emp_name, gender, dob, doj, phone, mobile_no, " +
            "       email_id, bsalary, address, narration " +
            "FROM employee_details WHERE 1=1");

        if (!searchText.isEmpty()) {
            sql.append(" AND (UPPER(emp_name) LIKE UPPER('%").append(searchText).append("%')")
               .append("   OR UPPER(emp_id)   LIKE UPPER('%").append(searchText).append("%')")
               .append("   OR phone LIKE '%").append(searchText).append("%')");
        }
        if (!"All".equals(gender)) {
            sql.append(" AND UPPER(gender) = UPPER('").append(gender).append("')");
        }
        switch (salRange) {
            case "0–20k"   -> sql.append(" AND bsalary < 20000");
            case "20k–50k" -> sql.append(" AND bsalary BETWEEN 20000 AND 50000");
            case "50k+"    -> sql.append(" AND bsalary > 50000");
        }
        sql.append(" ORDER BY emp_id");

        // Summary accumulators
        int    totalEmp  = 0, maleCount = 0, femaleCount = 0;
        double totalSal  = 0, maxSal    = 0;

        try {
            stmt = MdiForm.sconnect.createStatement();
            rs   = stmt.executeQuery(sql.toString());
            int serial = 1;
            while (rs.next()) {
                totalEmp++;
                String gVal  = rs.getString("gender");
                double bsal  = rs.getDouble("bsalary");

                if ("Male".equalsIgnoreCase(gVal))   maleCount++;
                else if ("Female".equalsIgnoreCase(gVal)) femaleCount++;

                totalSal += bsal;
                if (bsal > maxSal) maxSal = bsal;

                model.addRow(new Object[]{
                    serial++,
                    rs.getString("emp_id"),
                    rs.getString("emp_name"),
                    gVal,
                    rs.getString("dob"),
                    rs.getString("doj"),
                    rs.getString("phone"),
                    rs.getString("mobile_no"),
                    rs.getString("email_id"),
                    String.format("%,.2f", bsal),
                    rs.getString("address"),
                    rs.getString("narration")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Database error while loading employee report:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Update summary cards
        double avgSal = totalEmp > 0 ? totalSal / totalEmp : 0;
        lblTotalEmp .setText(String.valueOf(totalEmp));
        lblMale     .setText(String.valueOf(maleCount));
        lblFemale   .setText(String.valueOf(femaleCount));
        lblTotalSal .setText("₹" + compactNum(totalSal));
        lblAvgSal   .setText("₹" + compactNum(avgSal));
        lblMaxSal   .setText("₹" + compactNum(maxSal));
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
        double sw  = pf.getImageableWidth();
        double sh  = pf.getImageableHeight();
        double scl = Math.min(sw / table.getWidth(), sh / table.getHeight());
        if (scl < 1) g2.scale(scl, scl);
        table.paint(g2);
        return PAGE_EXISTS;
    }

    // ─────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────
    private void addTBLabel(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL);
        l.setForeground(TEXT_MUTED);
        p.add(l);
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(F_LABEL);
        cb.setBackground(BG_DARK);
        cb.setForeground(TEXT_MAIN);
        cb.setBorder(BorderFactory.createLineBorder(new Color(50, 70, 100)));
        return cb;
    }

    private JButton makeButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };
        b.setFont(F_BOLD);
        b.setForeground(fg);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(130, 30));
        return b;
    }

    /** Format large numbers compactly: 125000 → "1,25,000" (Indian style) */
    private String compactNum(double v) {
        if (v >= 10_00_000) return String.format("%.1fL", v / 1_00_000.0);
        if (v >= 1000)      return String.format("%,.0f", v);
        return String.format("%.0f", v);
    }
}