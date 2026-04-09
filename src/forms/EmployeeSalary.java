package forms;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import static ui.UIConstants.*;

public class EmployeeSalary extends JInternalFrame {

    // ── DB ────────────────────────────────────────────────────────────────
    private PreparedStatement pstmt;
    private Statement stmt;
    private int tot = 0;
    private boolean dirty = false;
    private boolean recalcLock = false;

    // ── Master table ──────────────────────────────────────────────────────
    private DefaultTableModel tModel;
    private JTable masterTable;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel lblCount;

    // ── Filters ───────────────────────────────────────────────────────────
    private JTextField txtSearch;
    private JTextField txtFMonth, txtFYear, txtFStatus;

    // ── Stat badges ───────────────────────────────────────────────────────
    private JLabel lblTotalSal, lblPaid, lblPending, lblAvgNet;

    // ── Form fields ───────────────────────────────────────────────────────
    private JTextField fSalId, fEmpId, fEmpName, fMonth, fYear;
    private JTextField fBSal, fHRA, fDA, fTA, fPFDed, fOthDed, fNetSal, fNarr;
    private JComboBox<String> fStatus;
    private JLabel lblDirty, lblNetBadge;

    // ── Buttons ───────────────────────────────────────────────────────────
    private JButton btnNew, btnSave, btnUpdate, btnDelete, btnMarkPaid, btnRefresh;

    // ── Toast ─────────────────────────────────────────────────────────────
    private JLabel toastLbl;
    private javax.swing.Timer toastTimer;

    // ─────────────────────────────────────────────────────────────────────
    public EmployeeSalary() {
        super("Employee Salary", true, true, true, true);
        setSize(1380, 780);
        setLocation(40, 40);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ensureTableExists();
        buildUI();
        registerShortcuts();
        loadMaster();
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // DDL
    // ═════════════════════════════════════════════════════════════════════
    private void ensureTableExists() {
        try {
            String check = "SELECT count(*) FROM user_tables WHERE table_name='SALARY_MASTER'";
            try (Statement st = MdiForm.sconnect.createStatement();
                 ResultSet r  = st.executeQuery(check)) {
                if (r.next() && r.getInt(1) == 0) {
                    String sql = "CREATE TABLE salary_master (" +
                        "sal_id VARCHAR2(20) PRIMARY KEY, emp_id VARCHAR2(20), emp_name VARCHAR2(100), " +
                        "month VARCHAR2(15), year VARCHAR2(6), bsalary NUMBER(10,2), " +
                        "hra NUMBER(10,2), da NUMBER(10,2), ta NUMBER(10,2), " +
                        "pf_deduction NUMBER(10,2), other_deduction NUMBER(10,2), " +
                        "net_salary NUMBER(10,2), status VARCHAR2(20) DEFAULT 'Pending')";
                    try (Statement st2 = MdiForm.sconnect.createStatement()) { st2.executeUpdate(sql); }
                }
            }
        } catch (SQLException ex) { showToast("DB init error: " + ex.getMessage(), DANGER); }
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI Shell
    // ═════════════════════════════════════════════════════════════════════
    private void buildUI() {
        Container c = getContentPane();
        c.setLayout(new BorderLayout()); c.setBackground(BG_MAIN);
        c.add(buildHeader(),      BorderLayout.NORTH);
        c.add(buildSplit(),       BorderLayout.CENTER);
        c.add(buildShortcutBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout()) {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(14, 38, 28), getWidth(), 0, BG_MAIN));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT2); g2.fillRect(0, getHeight()-2, getWidth(), 2);
            }
        };
        hdr.setPreferredSize(new Dimension(0, 54));
        hdr.setBorder(new EmptyBorder(0, 22, 0, 22));
        JLabel title = new JLabel("💰  Employee Salary");
        title.setFont(F_HEAD); title.setForeground(TEXT_PRI);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        lblTotalSal = styledBadge("0 Records", ACCENT,  new Color(14,38,28));
        lblPaid     = styledBadge("0 Paid",    PAID_FG,  PAID_BG);
        lblPending  = styledBadge("0 Pending", PEND_FG, PEND_BG);
        lblAvgNet   = styledBadge("₹0 Avg",   GOLD,    new Color(40,30,8));
        lblCount    = styledBadge("0 records", TEXT_MUT, BG_INPUT);
        right.add(lblTotalSal); right.add(lblPaid); right.add(lblPending);
        right.add(lblAvgNet); right.add(Box.createHorizontalStrut(8)); right.add(lblCount);
        hdr.add(title, BorderLayout.WEST); hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    private JSplitPane buildSplit() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        sp.setDividerLocation(640); sp.setDividerSize(4);
        sp.setBorder(null); sp.setBackground(BG_MAIN);
        return sp;
    }

    // ═════════════════════════════════════════════════════════════════════
    // LEFT — Master table
    // ═════════════════════════════════════════════════════════════════════
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.add(buildFilterBar(),   BorderLayout.NORTH);
        p.add(buildMasterTable(), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFilterBar() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_PANEL);
        outer.setBorder(new EmptyBorder(10, 12, 8, 12));
        txtSearch = styledField("🔍  Search by name or employee ID…");
        txtSearch.setPreferredSize(new Dimension(0, 34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));
        JPanel r1 = new JPanel(new GridLayout(1, 3, 5, 0));
        r1.setBackground(BG_PANEL); r1.setBorder(new EmptyBorder(6, 0, 4, 0));
        txtFMonth  = miniField("Month…");
        txtFYear   = miniField("Year…");
        txtFStatus = miniField("Status…");
        r1.add(txtFMonth); r1.add(txtFYear); r1.add(txtFStatus);
        for (JTextField tf : new JTextField[]{txtFMonth, txtFYear, txtFStatus})
            tf.getDocument().addDocumentListener(dl(this::applyFilter));
        outer.add(txtSearch, BorderLayout.NORTH);
        outer.add(r1,        BorderLayout.CENTER);
        return outer;
    }

    private JScrollPane buildMasterTable() {
        // COL: 0=SalID 1=EmpID 2=Name 3=Month 4=Year 5=Net 6=Status
        String[] cols = {"Sal ID","Emp ID","Name","Month","Year","Net Sal (₹)","Status"};
        tModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 5) return Double.class;
                return String.class;
            }
        };
        masterTable = new JTable(tModel) {
            int hoverRow = -1;
            {
                addMouseMotionListener(new MouseMotionAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        int r = rowAtPoint(e.getPoint());
                        if (r != hoverRow) { hoverRow = r; repaint(); }
                    }
                });
                addMouseListener(new MouseAdapter() {
                    public void mouseExited(MouseEvent e) { hoverRow = -1; repaint(); }
                });
            }
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component comp = super.prepareRenderer(tcr, row, col);
                boolean sel = isRowSelected(row);
                if (sel) { comp.setBackground(BG_ROW_SEL); comp.setForeground(Color.WHITE); }
                else if (row == hoverRow) { comp.setBackground(BG_ROW_HOVER); comp.setForeground(TEXT_PRI); }
                else { comp.setBackground(row%2==0?BG_ROW_EVEN:BG_ROW_ODD); comp.setForeground(TEXT_PRI); }
                if (!sel) {
                    int mc = masterTable.convertColumnIndexToModel(col);
                    if (mc == 0) comp.setForeground(ACCENT);
                    if (mc == 1) comp.setForeground(TEAL);
                    if (mc == 2) comp.setForeground(TEXT_PRI);
                    if (mc == 5) comp.setForeground(GOLD);
                    if (mc == 6) {
                        Object v = tModel.getValueAt(masterTable.convertRowIndexToModel(row), 6);
                        comp.setForeground("Paid".equalsIgnoreCase(v!=null?v.toString():"") ? PAID_FG : PEND_FG);
                    }
                }
                ((JComponent) comp).setBorder(new EmptyBorder(4, 8, 4, 8));
                return comp;
            }
        };
        masterTable.setFont(F_TABLE); masterTable.setRowHeight(30);
        masterTable.setShowGrid(false); masterTable.setIntercellSpacing(new Dimension(0, 0));
        masterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        masterTable.setBackground(BG_PANEL); masterTable.setForeground(TEXT_PRI);
        masterTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] ws = {75, 75, 160, 90, 60, 100, 72};
        for (int i = 0; i < ws.length; i++) masterTable.getColumnModel().getColumn(i).setPreferredWidth(ws[i]);
        JTableHeader th = masterTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBackground(BG_CARD); th.setForeground(TEXT_MUT);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        th.setReorderingAllowed(false);
        sorter = new TableRowSorter<>(tModel);
        masterTable.setRowSorter(sorter);
        masterTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });
        JScrollPane sc = new JScrollPane(masterTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(null); sc.getViewport().setBackground(BG_PANEL);
        styleScrollBar(sc); return sc;
    }

    // ═════════════════════════════════════════════════════════════════════
    // RIGHT — Detail form
    // ═════════════════════════════════════════════════════════════════════
    private JPanel buildRightPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_CARD);
        outer.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COL));
        outer.add(buildDetailHeader(), BorderLayout.NORTH);
        outer.add(buildFormScroll(),   BorderLayout.CENTER);
        outer.add(buildButtonBar(),    BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildDetailHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_CARD); p.setBorder(new EmptyBorder(12, 20, 8, 20));
        JLabel title = new JLabel("Salary Record");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14)); title.setForeground(TEXT_PRI);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        lblDirty = new JLabel("● Unsaved");
        lblDirty.setFont(F_SMALL); lblDirty.setForeground(WARNING); lblDirty.setVisible(false);
        lblNetBadge = styledBadge("", GOLD, new Color(60, 46, 10));
        badges.add(lblDirty); badges.add(lblNetBadge);
        p.add(title, BorderLayout.WEST); p.add(badges, BorderLayout.EAST);
        return p;
    }

    private JScrollPane buildFormScroll() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setBorder(new EmptyBorder(6, 18, 18, 18));

        // ── Identity ──────────────────────────────────────────────────
        form.add(sectionHeader("Salary Identity"));
        JPanel id1 = gridPanel(2);
        fSalId = roField(); fEmpId = inputField();
        id1.add(labeled("Salary ID", fSalId));
        id1.add(labeled("Employee ID *", fEmpId));
        form.add(id1);

        JPanel id2 = gridPanel(1);
        fEmpName = inputField();
        id2.add(labeled("Employee Name (auto-filled)", fEmpName));
        form.add(id2);

        // ── Period ────────────────────────────────────────────────────
        form.add(sectionHeader("Pay Period"));
        JPanel per = gridPanel(2);
        fMonth = inputField(); fYear = inputField();
        fYear.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        per.add(labeled("Month *", fMonth));
        per.add(labeled("Year *",  fYear));
        form.add(per);

        // ── Earnings ──────────────────────────────────────────────────
        form.add(sectionHeader("Earnings (₹)"));
        JPanel earn1 = gridPanel(3);
        fBSal = inputField(); fHRA = inputField(); fDA = inputField();
        earn1.add(labeled("Basic Salary *", fBSal));
        earn1.add(labeled("HRA",            fHRA));
        earn1.add(labeled("DA",             fDA));
        form.add(earn1);

        JPanel earn2 = gridPanel(1);
        fTA = inputField();
        earn2.add(labeled("TA", fTA));
        form.add(earn2);

        // ── Deductions ────────────────────────────────────────────────
        form.add(sectionHeader("Deductions (₹)"));
        JPanel ded = gridPanel(2);
        fPFDed = inputField(); fOthDed = inputField();
        ded.add(labeled("PF Deduction",    fPFDed));
        ded.add(labeled("Other Deduction", fOthDed));
        form.add(ded);

        // ── Net / Status ──────────────────────────────────────────────
        form.add(sectionHeader("Net Salary & Status"));
        JPanel net = gridPanel(2);
        fNetSal = roField(); fNetSal.setForeground(GOLD);
        fStatus = new JComboBox<>(new String[]{"Pending","Paid"});
        styleComboBox(fStatus);
        net.add(labeled("Net Salary (₹) — auto", fNetSal));
        net.add(labeled("Status", fStatus));
        form.add(net);

        // ── Narration ─────────────────────────────────────────────────
        form.add(sectionHeader("Narration"));
        JPanel narPnl = gridPanel(1);
        fNarr = inputField();
        narPnl.add(labeled("Notes", fNarr));
        form.add(narPnl);

        // Auto-fill from employee
        fEmpId.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) { autoFillEmployee(); }
        });

        // Recalculation
        DocumentListener recalcDl = dl(this::recalc);
        for (JTextField tf : new JTextField[]{fBSal, fHRA, fDA, fTA, fPFDed, fOthDed})
            tf.getDocument().addDocumentListener(recalcDl);

        // Dirty listeners
        DocumentListener dirtyDl = dl(() -> setDirty(true));
        for (JTextField tf : new JTextField[]{fEmpId, fEmpName, fMonth, fYear, fBSal, fHRA, fDA, fTA, fPFDed, fOthDed, fNarr})
            tf.getDocument().addDocumentListener(dirtyDl);
        fStatus.addActionListener(e -> setDirty(true));

        JScrollPane scroll = new JScrollPane(form,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null); scroll.getViewport().setBackground(BG_CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll); return scroll;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        bar.setBackground(new Color(12, 18, 34));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));
        btnNew      = makeBtn("＋ New",      ACCENT,  new Color(16, 62, 130));
        btnSave     = makeBtn("💾 Save",     ACCENT2, new Color(22, 108, 68));
        btnUpdate   = makeBtn("✏ Update",   GOLD,    new Color(140, 96, 10));
        btnDelete   = makeBtn("🗑 Delete",   DANGER,  new Color(140, 35, 35));
        btnMarkPaid = makeBtn("✓ Mark Paid", ACCENT2, new Color(12, 60, 36));
        btnRefresh  = makeBtn("↺ Refresh",  TEXT_MUT, BG_INPUT);
        btnNew     .addActionListener(e -> clearForm());
        btnSave    .addActionListener(e -> saveRecord());
        btnUpdate  .addActionListener(e -> updateRecord());
        btnDelete  .addActionListener(e -> deleteRecord());
        btnMarkPaid.addActionListener(e -> markAsPaid());
        btnRefresh .addActionListener(e -> { loadMaster(); setDirty(false); });
        bar.add(btnNew); bar.add(btnSave); bar.add(btnUpdate);
        bar.add(btnDelete); bar.add(btnMarkPaid); bar.add(btnRefresh);
        return bar;
    }

    private JPanel buildShortcutBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        p.setBackground(new Color(7, 10, 20));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(24, 34, 56)));
        for (String s : new String[]{"Ctrl+S  Save","Ctrl+U  Update","Ctrl+D  Delete","Ctrl+N  New","Esc  Refresh"}) {
            JLabel l = new JLabel(s); l.setFont(F_SMALL); l.setForeground(new Color(55, 78, 115)); p.add(l);
        }
        toastLbl = new JLabel(""); toastLbl.setFont(F_SMALL); toastLbl.setOpaque(true);
        toastLbl.setVisible(false); toastLbl.setBorder(new EmptyBorder(3, 12, 3, 12));
        p.add(Box.createHorizontalStrut(20)); p.add(toastLbl);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Shortcuts
    // ═════════════════════════════════════════════════════════════════════
    private void registerShortcuts() {
        JRootPane rp = getRootPane();
        rp.registerKeyboardAction(e -> saveRecord(),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> updateRecord(),
                KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> deleteRecord(),
                KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> clearForm(),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> { loadMaster(); setDirty(false); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data Operations
    // ═════════════════════════════════════════════════════════════════════
    private void loadMaster() {
        tModel.setRowCount(0); tot = 0;
        int paid = 0, pending = 0; double totalNet = 0;
        try {
            stmt = MdiForm.sconnect.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM salary_master ORDER BY year DESC, month");
            while (rs.next()) {
                String st = nvl(rs.getString("status"));
                double net = 0; try { net = rs.getDouble("net_salary"); } catch (Exception ignored) {}
                tModel.addRow(new Object[]{
                    nvl(rs.getString("sal_id")),
                    nvl(rs.getString("emp_id")),
                    nvl(rs.getString("emp_name")),
                    nvl(rs.getString("month")),
                    nvl(rs.getString("year")),
                    net,
                    st
                });
                tot++;
                if ("Paid".equalsIgnoreCase(st)) paid++;
                else pending++;
                totalNet += net;
            }
        } catch (SQLException ex) { showToast("Load error: " + ex.getMessage(), DANGER); }
        double avg = tot > 0 ? totalNet / tot : 0;
        lblTotalSal.setText(tot + " Records");
        lblPaid    .setText(paid + " Paid");
        lblPending .setText(pending + " Pending");
        lblAvgNet  .setText("₹" + String.format("%,.0f", avg) + " Avg");
        lblCount   .setText(tot + " record" + (tot == 1 ? "" : "s"));
        applyFilter();
    }

    private void onRowSelected() {
        int vr = masterTable.getSelectedRow(); if (vr < 0) return;
        Object salId = tModel.getValueAt(masterTable.convertRowIndexToModel(vr), 0);
        try {
            pstmt = MdiForm.sconnect.prepareStatement("SELECT * FROM salary_master WHERE sal_id=?");
            pstmt.setString(1, salId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { fillForm(rs); setDirty(false); }
        } catch (SQLException ex) { showToast("Load error: " + ex.getMessage(), DANGER); }
    }

    private void fillForm(ResultSet rs) throws SQLException {
        recalcLock = true;
        try {
            fSalId  .setText(nvl(rs.getString("sal_id")));
            fEmpId  .setText(nvl(rs.getString("emp_id")));
            fEmpName.setText(nvl(rs.getString("emp_name")));
            fMonth  .setText(nvl(rs.getString("month")));
            fYear   .setText(nvl(rs.getString("year")));
            fBSal   .setText(fmt(rs.getDouble("bsalary")));
            fHRA    .setText(fmt(rs.getDouble("hra")));
            fDA     .setText(fmt(rs.getDouble("da")));
            fTA     .setText(fmt(rs.getDouble("ta")));
            fPFDed  .setText(fmt(rs.getDouble("pf_deduction")));
            fOthDed .setText(fmt(rs.getDouble("other_deduction")));
            double net = rs.getDouble("net_salary");
            fNetSal.setText(fmt(net));
            String st = nvl(rs.getString("status"));
            fStatus.setSelectedItem(st);
            if (net > 0) { lblNetBadge.setText("Net ₹" + String.format("%,.2f", net)); lblNetBadge.setForeground(GOLD); }
            else lblNetBadge.setText("");
        } finally { recalcLock = false; }
    }

    private void autoFillEmployee() {
        String id = fEmpId.getText().trim(); if (id.isEmpty()) return;
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                "SELECT emp_name, bsalary FROM employee_details WHERE emp_id=?");
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                fEmpName.setText(nvl(rs.getString("emp_name")));
                if (fBSal.getText().trim().isEmpty())
                    fBSal.setText(fmt(rs.getDouble("bsalary")));
            }
        } catch (Exception ignored) {}
    }

    private void recalc() {
        if (recalcLock) return;
        recalcLock = true;
        try {
            double bsal = parseDbl(fBSal.getText()), hra = parseDbl(fHRA.getText()),
                   da   = parseDbl(fDA.getText()),   ta  = parseDbl(fTA.getText()),
                   pf   = parseDbl(fPFDed.getText()),oth = parseDbl(fOthDed.getText());
            double net = bsal + hra + da + ta - pf - oth;
            fNetSal.setText(fmt(net));
            if (net > 0) { lblNetBadge.setText("Net ₹" + String.format("%,.2f", net)); lblNetBadge.setForeground(GOLD); }
            else lblNetBadge.setText("");
        } finally { recalcLock = false; }
    }

    private void saveRecord() {
        if (fSalId.getText().trim().isEmpty()) { showToast("Salary ID is required!", WARNING); return; }
        if (fEmpId.getText().trim().isEmpty()) { showToast("Employee ID is required!", WARNING); return; }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                "INSERT INTO salary_master VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)");
            bindAll(pstmt);
            pstmt.setString(13, (String) fStatus.getSelectedItem());
            pstmt.executeUpdate();
            showToast("✔ Salary saved — ID: " + fSalId.getText().trim(), ACCENT2);
            loadMaster(); setDirty(false);
        } catch (SQLException ex) { showToast("Save error: " + ex.getMessage(), DANGER); }
    }

    private void updateRecord() {
        String salId = fSalId.getText().trim();
        if (salId.isEmpty()) { showToast("Select a salary record first!", WARNING); return; }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                "UPDATE salary_master SET emp_id=?,emp_name=?,month=?,year=?,bsalary=?,hra=?,da=?,ta=?," +
                "pf_deduction=?,other_deduction=?,net_salary=?,status=? WHERE sal_id=?");
            pstmt.setString(1,  fEmpId.getText().trim());
            pstmt.setString(2,  fEmpName.getText().trim());
            pstmt.setString(3,  fMonth.getText().trim());
            pstmt.setString(4,  fYear.getText().trim());
            pstmt.setDouble(5,  parseDbl(fBSal.getText()));
            pstmt.setDouble(6,  parseDbl(fHRA.getText()));
            pstmt.setDouble(7,  parseDbl(fDA.getText()));
            pstmt.setDouble(8,  parseDbl(fTA.getText()));
            pstmt.setDouble(9,  parseDbl(fPFDed.getText()));
            pstmt.setDouble(10, parseDbl(fOthDed.getText()));
            pstmt.setDouble(11, parseDbl(fNetSal.getText()));
            pstmt.setString(12, (String) fStatus.getSelectedItem());
            pstmt.setString(13, salId);
            int r = pstmt.executeUpdate();
            if (r > 0) { loadMaster(); setDirty(false); showToast("✔ Record updated.", ACCENT2); }
            else showToast("No record matched ID " + salId, WARNING);
        } catch (SQLException ex) { showToast("Update error: " + ex.getMessage(), DANGER); }
    }

    private void deleteRecord() {
        String salId = fSalId.getText().trim();
        if (salId.isEmpty()) { showToast("Select a record to delete!", WARNING); return; }
        int ans = JOptionPane.showConfirmDialog(this,
            "Permanently delete Salary record " + salId + "?\nThis cannot be undone.",
            "Delete Salary", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;
        try {
            pstmt = MdiForm.sconnect.prepareStatement("DELETE FROM salary_master WHERE sal_id=?");
            pstmt.setString(1, salId);
            pstmt.executeUpdate();
            clearForm(); loadMaster(); showToast("🗑 Record deleted.", DANGER);
        } catch (SQLException ex) { showToast("Delete error: " + ex.getMessage(), DANGER); }
    }

    private void markAsPaid() {
        String salId = fSalId.getText().trim();
        if (salId.isEmpty()) { showToast("Select a salary record first!", WARNING); return; }
        try {
            pstmt = MdiForm.sconnect.prepareStatement("UPDATE salary_master SET status='Paid' WHERE sal_id=?");
            pstmt.setString(1, salId);
            pstmt.executeUpdate();
            fStatus.setSelectedItem("Paid");
            loadMaster(); showToast("✔ Marked as Paid.", ACCENT2);
        } catch (SQLException ex) { showToast("Error: " + ex.getMessage(), DANGER); }
    }

    private void bindAll(PreparedStatement ps) throws SQLException {
        ps.setString(1,  fSalId.getText().trim());
        ps.setString(2,  fEmpId.getText().trim());
        ps.setString(3,  fEmpName.getText().trim());
        ps.setString(4,  fMonth.getText().trim());
        ps.setString(5,  fYear.getText().trim());
        ps.setDouble(6,  parseDbl(fBSal.getText()));
        ps.setDouble(7,  parseDbl(fHRA.getText()));
        ps.setDouble(8,  parseDbl(fDA.getText()));
        ps.setDouble(9,  parseDbl(fTA.getText()));
        ps.setDouble(10, parseDbl(fPFDed.getText()));
        ps.setDouble(11, parseDbl(fOthDed.getText()));
        ps.setDouble(12, parseDbl(fNetSal.getText()));
    }

    private void clearForm() {
        fSalId.setText(""); fEmpId.setText(""); fEmpName.setText("");
        fMonth.setText(""); fYear.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        fBSal.setText(""); fHRA.setText(""); fDA.setText(""); fTA.setText("");
        fPFDed.setText(""); fOthDed.setText(""); fNetSal.setText(""); fNarr.setText("");
        fStatus.setSelectedIndex(0); lblNetBadge.setText("");
        masterTable.clearSelection(); setDirty(false);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Filter
    // ═════════════════════════════════════════════════════════════════════
    private void applyFilter() {
        List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();
        String gs = txtSearch.getText().trim();
        if (!gs.isEmpty()) {
            try {
                filters.add(RowFilter.orFilter(Arrays.asList(
                    RowFilter.regexFilter("(?i)" + gs, 1),
                    RowFilter.regexFilter("(?i)" + gs, 2))));
            } catch (Exception ignored) {}
        }
        addColFilter(filters, txtFMonth.getText(),  3);
        addColFilter(filters, txtFYear.getText(),   4);
        addColFilter(filters, txtFStatus.getText(), 6);
        try { sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters)); }
        catch (Exception ignored) {}
        lblCount.setText(masterTable.getRowCount() + " / " + tot + " records");
    }

    private void addColFilter(List<RowFilter<DefaultTableModel, Object>> list, String val, int col) {
        if (val == null || val.trim().isEmpty()) return;
        try { list.add(RowFilter.regexFilter("(?i)" + val.trim(), col)); }
        catch (Exception ignored) {}
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI Helpers
    // ═════════════════════════════════════════════════════════════════════
    private JPanel sectionHeader(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SECTION_BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); p.setAlignmentX(0f);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        JLabel l = new JLabel(text.toUpperCase()); l.setFont(F_SEC); l.setForeground(ACCENT2);
        p.add(l, BorderLayout.WEST); return p;
    }

    private JPanel gridPanel(int cols) {
        JPanel p = new JPanel(new GridLayout(1, cols, 8, 0));
        p.setBackground(BG_CARD); p.setBorder(new EmptyBorder(6, 0, 4, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62)); p.setAlignmentX(0f); return p;
    }

    private JPanel labeled(String lbl, Component field) {
        JPanel p = new JPanel(new BorderLayout(0, 3)); p.setBackground(BG_CARD);
        JLabel l = new JLabel(lbl); l.setFont(F_LABEL); l.setForeground(TEXT_MUT);
        p.add(l, BorderLayout.NORTH); p.add(field, BorderLayout.CENTER); return p;
    }

    private JTextField inputField() {
        JTextField tf = new JTextField(); tf.setFont(F_INPUT);
        tf.setBackground(BG_INPUT); tf.setForeground(TEXT_PRI); tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    private JTextField roField() {
        JTextField tf = inputField(); tf.setEditable(false);
        tf.setBackground(new Color(14, 20, 38)); tf.setForeground(GOLD); return tf;
    }

    private JTextField styledField(String ph) {
        JTextField tf = new JTextField() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    g.setColor(TEXT_MUT); g.setFont(F_SMALL); g.drawString(ph, 10, getHeight()/2+4);
                }
            }
        };
        tf.setFont(F_INPUT); tf.setBackground(BG_INPUT); tf.setForeground(TEXT_PRI);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    private JTextField miniField(String ph) {
        JTextField tf = styledField(ph); tf.setPreferredSize(new Dimension(0, 28)); return tf;
    }

    private void styleComboBox(JComboBox<?> cb) {
        cb.setFont(F_INPUT); cb.setBackground(BG_INPUT); cb.setForeground(TEXT_PRI);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COL));
    }

    private JLabel styledBadge(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text); l.setFont(F_SMALL); l.setForeground(fg);
        l.setOpaque(true); l.setBackground(bg); l.setBorder(new EmptyBorder(3, 10, 3, 10));
        return l;
    }

    private JButton makeBtn(String text, Color fg, Color bg) {
        JButton b = new JButton(text) {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(fg.brighter()); g2.setFont(F_BTN);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                        (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setPreferredSize(new Dimension(112, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }

    private void styleScrollBar(JScrollPane sp) {
        for (JScrollBar sb : new JScrollBar[]{sp.getVerticalScrollBar(), sp.getHorizontalScrollBar()}) {
            if (sb == null) continue;
            sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                protected void configureScrollBarColors() { thumbColor=new Color(46,66,108); trackColor=BG_PANEL; }
                protected JButton createDecreaseButton(int o) { return zb(); }
                protected JButton createIncreaseButton(int o) { return zb(); }
                private JButton zb() { JButton b=new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
            });
        }
    }

    private void setDirty(boolean d) { dirty = d; SwingUtilities.invokeLater(() -> lblDirty.setVisible(d)); }

    private void showToast(String msg, Color bg) {
        if (toastLbl == null) return;
        toastLbl.setText(msg); toastLbl.setBackground(bg); toastLbl.setForeground(Color.WHITE);
        toastLbl.setVisible(true);
        if (toastTimer != null && toastTimer.isRunning()) toastTimer.stop();
        toastTimer = new javax.swing.Timer(3400, e -> toastLbl.setVisible(false));
        toastTimer.setRepeats(false); toastTimer.start();
    }

    private DocumentListener dl(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { r.run(); }
            public void removeUpdate(DocumentEvent e)  { r.run(); }
            public void changedUpdate(DocumentEvent e) {}
        };
    }

    private double parseDbl(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; } }
    private String fmt(double d)      { return d == 0 ? "" : String.format("%.2f", d); }
    private String nvl(String s)      { return s == null ? "" : s; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Test"); JDesktopPane d = new JDesktopPane();
            d.setBackground(new Color(8, 12, 20)); f.setContentPane(d);
            f.setSize(1420, 820); f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationRelativeTo(null); f.setVisible(true);
            d.add(new EmployeeSalary());
        });
    }
}