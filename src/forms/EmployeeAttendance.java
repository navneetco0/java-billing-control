package forms;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import static ui.UIConstants.*;

public class EmployeeAttendance extends JInternalFrame {
    private static final String[] STATUSES = {"Present","Absent","Half-Day","Leave"};

    // ── DB ────────────────────────────────────────────────────────────────
    private PreparedStatement pstmt;
    private Statement stmt;
    private int tot = 0;
    private boolean dirty = false;

    // ── Date state ────────────────────────────────────────────────────────
    private Calendar selectedCal = Calendar.getInstance();

    // Column indices for attendance row
    private static final int C_ATT_ID=0,C_EMP_ID=1,C_NAME=2,C_DATE=3,C_STATUS=4,C_REMARKS=5;

    // In-memory attendance rows (all for selected date)
    private final List<String[]> allRows      = new ArrayList<>();
    private final List<String[]> filteredRows = new ArrayList<>();

    // ── Master table ──────────────────────────────────────────────────────
    private DefaultTableModel tModel;
    private JTable masterTable;
    private int hoveredRow = -1;
    private JLabel lblCount;

    // ── Filters ───────────────────────────────────────────────────────────
    private JTextField txtSearch;

    // ── Stat badges ───────────────────────────────────────────────────────
    private JLabel lblPresent, lblAbsent, lblHalfDay, lblLeave, lblTotal;

    // ── Date nav ─────────────────────────────────────────────────────────
    private JLabel lblDateDisplay;

    // ── Form fields (right panel for single record editing) ───────────────
    private JTextField fAttId, fEmpId, fEmpName, fDate, fRemarks;
    private JComboBox<String> fStatus;
    private JLabel lblDirty;

    // ── Bulk action ───────────────────────────────────────────────────────
    private JComboBox<String> cmbBulkStatus;

    // ── Buttons ───────────────────────────────────────────────────────────
    private JButton btnSaveRow, btnSaveAll, btnRefresh, btnAdd;

    // ── Toast ─────────────────────────────────────────────────────────────
    private JLabel toastLbl;
    private javax.swing.Timer toastTimer;

    // ─────────────────────────────────────────────────────────────────────
    public EmployeeAttendance() {
        super("Employee Attendance", true, true, true, true);
        setSize(1380, 780);
        setLocation(40, 40);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        createTableIfNotExists();
        buildUI();
        registerShortcuts();
        loadAttendanceForDate();
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // DDL
    // ═════════════════════════════════════════════════════════════════════
    private void createTableIfNotExists() {
        String sql = "CREATE TABLE employee_attendance (" +
            "att_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
            "emp_id   VARCHAR2(20) NOT NULL," +
            "att_date DATE NOT NULL," +
            "status   VARCHAR2(20) DEFAULT 'Absent' NOT NULL," +
            "remarks  VARCHAR2(255) DEFAULT ''," +
            "CONSTRAINT uq_emp_date UNIQUE (emp_id, att_date)," +
            "CONSTRAINT fk_att_emp FOREIGN KEY (emp_id) REFERENCES employee_details(emp_id) ON DELETE CASCADE)";
        try {
            stmt = MdiForm.sconnect.createStatement(); stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            if (ex.getErrorCode() != 955)
                showToast("DB setup: " + ex.getMessage(), WARNING);
        }
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
                g2.setPaint(new GradientPaint(0, 0, new Color(14, 38, 60), getWidth(), 0, BG_MAIN));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT); g2.fillRect(0, getHeight()-2, getWidth(), 2);
            }
        };
        hdr.setPreferredSize(new Dimension(0, 54));
        hdr.setBorder(new EmptyBorder(0, 22, 0, 22));

        JLabel title = new JLabel("📋  Employee Attendance");
        title.setFont(F_HEAD); title.setForeground(TEXT_PRI);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        // Date navigation
        JButton btnPrev = navBtn("◀"); btnPrev.addActionListener(e -> shiftDay(-1));
        JButton btnNext = navBtn("▶"); btnNext.addActionListener(e -> shiftDay(+1));
        JButton btnToday = pillBtn("Today"); btnToday.addActionListener(e -> goToday());
        lblDateDisplay = new JLabel(formatDateFull(selectedCal));
        lblDateDisplay.setFont(new Font("Segoe UI", Font.BOLD, 12)); lblDateDisplay.setForeground(TEXT_PRI);

        // Stat badges
        lblTotal   = styledBadge("0 Total",    TEXT_MUT, BG_INPUT);
        lblPresent = styledBadge("0 Present",  PRESENT_FG, PRESENT_BG);
        lblAbsent  = styledBadge("0 Absent",   ABSENT_FG,  ABSENT_BG);
        lblHalfDay = styledBadge("0 Half-Day", HALF_FG,    HALF_BG);
        lblLeave   = styledBadge("0 Leave",    LEAVE_FG,   LEAVE_BG);
        lblCount   = styledBadge("0 records",  TEXT_MUT, BG_INPUT);

        right.add(btnToday); right.add(Box.createHorizontalStrut(6));
        right.add(btnPrev); right.add(lblDateDisplay); right.add(btnNext);
        right.add(Box.createHorizontalStrut(10));
        right.add(lblTotal); right.add(lblPresent); right.add(lblAbsent);
        right.add(lblHalfDay); right.add(lblLeave);
        right.add(Box.createHorizontalStrut(6)); right.add(lblCount);

        hdr.add(title, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    private JSplitPane buildSplit() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        sp.setDividerLocation(720); sp.setDividerSize(4);
        sp.setBorder(null); sp.setBackground(BG_MAIN);
        return sp;
    }

    // ═════════════════════════════════════════════════════════════════════
    // LEFT — Attendance table
    // ═════════════════════════════════════════════════════════════════════
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.add(buildFilterBar(),   BorderLayout.NORTH);
        p.add(buildMasterTable(), BorderLayout.CENTER);
        p.add(buildBulkBar(),     BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildFilterBar() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_PANEL);
        outer.setBorder(new EmptyBorder(10, 12, 8, 12));
        txtSearch = styledField("🔍  Search by employee name or ID…");
        txtSearch.setPreferredSize(new Dimension(0, 34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));
        outer.add(txtSearch, BorderLayout.NORTH);
        return outer;
    }

    private JScrollPane buildMasterTable() {
        // COL: 0=# 1=EmpID 2=Name 3=Date 4=Status 5=Remarks
        String[] cols = {"#","Emp ID","Employee Name","Date","Status","Remarks"};
        tModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 4 || c == 5; }
        };

        masterTable = new JTable(tModel) {
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component comp = super.prepareRenderer(tcr, row, col);
                boolean sel = isRowSelected(row);
                if (sel) { comp.setBackground(BG_ROW_SEL); comp.setForeground(Color.WHITE); }
                else if (row == hoveredRow) { comp.setBackground(BG_ROW_HOVER); comp.setForeground(TEXT_PRI); }
                else { comp.setBackground(row%2==0?BG_ROW_EVEN:BG_ROW_ODD); comp.setForeground(TEXT_PRI); }
                if (!sel) {
                    int mc = masterTable.convertColumnIndexToModel(col);
                    if (mc == 0) comp.setForeground(TEXT_MUT);
                    if (mc == 1) comp.setForeground(TEAL);
                    if (mc == 2) comp.setForeground(TEXT_PRI);
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

        int[] ws = {40, 80, 0, 90, 90, 0};
        for (int i = 0; i < ws.length; i++)
            if (ws[i] > 0) {
                masterTable.getColumnModel().getColumn(i).setMaxWidth(ws[i]);
                masterTable.getColumnModel().getColumn(i).setMinWidth(ws[i]);
            }

        // Status badge renderer + combo editor
        masterTable.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
        masterTable.getColumnModel().getColumn(4).setCellEditor(new StatusComboEditor());

        JTableHeader th = masterTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBackground(BG_CARD); th.setForeground(TEXT_MUT);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        th.setReorderingAllowed(false);

        masterTable.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int r = masterTable.rowAtPoint(e.getPoint());
                if (r != hoveredRow) { hoveredRow = r; masterTable.repaint(); }
            }
        });
        masterTable.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) { hoveredRow = -1; masterTable.repaint(); }
        });
        masterTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        JScrollPane sc = new JScrollPane(masterTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(null); sc.getViewport().setBackground(BG_PANEL);
        styleScrollBar(sc); return sc;
    }

    private JPanel buildBulkBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(new Color(12, 18, 34));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));
        JLabel lbl = new JLabel("Bulk mark all:");
        lbl.setFont(F_SMALL); lbl.setForeground(TEXT_MUT);
        cmbBulkStatus = new JComboBox<>(STATUSES); styleComboBox(cmbBulkStatus);
        cmbBulkStatus.setPreferredSize(new Dimension(100, 28));
        JButton btnBulk = makeBtn("Apply", ACCENT, new Color(16, 50, 100));
        btnBulk.setPreferredSize(new Dimension(80, 30));
        btnSaveAll = makeBtn("💾 Save All", ACCENT2, new Color(22, 108, 68));
        btnSaveAll.setPreferredSize(new Dimension(110, 30));
        btnAdd = makeBtn("＋ Add Record", PURPLE, new Color(40, 18, 70));
        btnAdd.setPreferredSize(new Dimension(120, 30));
        btnRefresh = makeBtn("↺ Refresh", TEXT_MUT, BG_INPUT);
        btnRefresh.setPreferredSize(new Dimension(90, 30));
        btnBulk   .addActionListener(e -> bulkMarkAll());
        btnSaveAll.addActionListener(e -> saveAllRows());
        btnAdd    .addActionListener(e -> openAddDialog());
        btnRefresh.addActionListener(e -> loadAttendanceForDate());
        bar.add(lbl); bar.add(cmbBulkStatus); bar.add(btnBulk);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(btnSaveAll); bar.add(btnAdd); bar.add(btnRefresh);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════
    // RIGHT — Detail / Edit form for selected row
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
        JLabel title = new JLabel("Attendance Record");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14)); title.setForeground(TEXT_PRI);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        lblDirty = new JLabel("● Unsaved");
        lblDirty.setFont(F_SMALL); lblDirty.setForeground(WARNING); lblDirty.setVisible(false);
        badges.add(lblDirty);
        p.add(title, BorderLayout.WEST); p.add(badges, BorderLayout.EAST);
        return p;
    }

    private JScrollPane buildFormScroll() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setBorder(new EmptyBorder(6, 18, 18, 18));

        // ── Identity ──────────────────────────────────────────────────
        form.add(sectionHeader("Record Identity"));
        JPanel id1 = gridPanel(2);
        fAttId = roField(); fDate = inputField();
        fDate.setText(dbDate(selectedCal));
        id1.add(labeled("Attendance ID", fAttId));
        id1.add(labeled("Date (YYYY-MM-DD) *", fDate));
        form.add(id1);

        // ── Employee ──────────────────────────────────────────────────
        form.add(sectionHeader("Employee"));
        JPanel emp = gridPanel(2);
        fEmpId = inputField(); fEmpName = roField();
        emp.add(labeled("Employee ID *", fEmpId));
        emp.add(labeled("Employee Name (auto)", fEmpName));
        form.add(emp);

        // Auto-fill name
        fEmpId.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) { autoFillEmployeeName(); }
        });

        // ── Status & Remarks ──────────────────────────────────────────
        form.add(sectionHeader("Attendance Status"));
        JPanel st1 = gridPanel(2);
        fStatus = new JComboBox<>(STATUSES); styleComboBox(fStatus);
        fRemarks = inputField();
        st1.add(labeled("Status *", fStatus));
        st1.add(labeled("Remarks",  fRemarks));
        form.add(st1);

        // Dirty
        DocumentListener dirtyDl = dl(() -> setDirty(true));
        for (JTextField tf : new JTextField[]{fEmpId, fDate, fRemarks})
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
        btnSaveRow = makeBtn("💾 Save Row", ACCENT2, new Color(22, 108, 68));
        btnSaveRow.addActionListener(e -> saveSingleRowFromForm());
        bar.add(btnSaveRow);
        JLabel note = new JLabel("  Select a row to edit status/remarks");
        note.setFont(F_SMALL); note.setForeground(TEXT_MUT);
        bar.add(note);
        return bar;
    }

    private JPanel buildShortcutBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        p.setBackground(new Color(7, 10, 20));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(24, 34, 56)));
        for (String s : new String[]{"◀/▶ Navigate days","Ctrl+S  Save All","Esc  Refresh"}) {
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
        rp.registerKeyboardAction(e -> saveAllRows(),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> loadAttendanceForDate(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data Operations
    // ═════════════════════════════════════════════════════════════════════
    private void loadAttendanceForDate() {
        allRows.clear();
        String dateStr = dbDate(selectedCal);
        try {
            String sql = "SELECT a.att_id, a.emp_id, e.emp_name, " +
                "TO_CHAR(a.att_date,'YYYY-MM-DD') AS att_date, a.status, a.remarks " +
                "FROM employee_attendance a " +
                "JOIN employee_details e ON a.emp_id = e.emp_id " +
                "WHERE TRUNC(a.att_date) = TO_DATE(?, 'YYYY-MM-DD') ORDER BY e.emp_name";
            pstmt = MdiForm.sconnect.prepareStatement(sql);
            pstmt.setString(1, dateStr);
            ResultSet rs = pstmt.executeQuery();
            Set<String> marked = new HashSet<>();
            while (rs.next()) {
                allRows.add(new String[]{
                    rs.getString("att_id"),
                    rs.getString("emp_id"),
                    nvl(rs.getString("emp_name")),
                    nvl(rs.getString("att_date")),
                    nvl(rs.getString("status")),
                    nvl(rs.getString("remarks"))
                });
                marked.add(rs.getString("emp_id"));
            }
            // Unmarked employees default to Absent
            try {
                stmt = MdiForm.sconnect.createStatement();
                ResultSet rsEmp = stmt.executeQuery("SELECT emp_id, emp_name FROM employee_details ORDER BY emp_name");
                while (rsEmp.next()) {
                    String eid = rsEmp.getString("emp_id");
                    if (!marked.contains(eid)) {
                        allRows.add(new String[]{"NEW", eid, nvl(rsEmp.getString("emp_name")),
                            dateStr, "Absent", ""});
                    }
                }
            } catch (Exception ignored) {}
        } catch (SQLException ex) { showToast("DB error: " + ex.getMessage(), DANGER); }
        applyFilter(); refreshStats();
    }

    private void onRowSelected() {
        int vr = masterTable.getSelectedRow(); if (vr < 0) return;
        if (vr >= filteredRows.size()) return;
        String[] r = filteredRows.get(vr);
        fAttId  .setText(r[C_ATT_ID]);
        fEmpId  .setText(r[C_EMP_ID]);
        fEmpName.setText(r[C_NAME]);
        fDate   .setText(r[C_DATE]);
        for (int i = 0; i < fStatus.getItemCount(); i++)
            if (fStatus.getItemAt(i).equalsIgnoreCase(r[C_STATUS])) { fStatus.setSelectedIndex(i); break; }
        fRemarks.setText(r[C_REMARKS]);
        setDirty(false);
    }

    private void autoFillEmployeeName() {
        String id = fEmpId.getText().trim(); if (id.isEmpty()) return;
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                "SELECT emp_name FROM employee_details WHERE emp_id=?");
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) fEmpName.setText(nvl(rs.getString("emp_name")));
        } catch (Exception ignored) {}
    }

    private void saveSingleRowFromForm() {
        String empId  = fEmpId.getText().trim();
        String date   = fDate.getText().trim();
        String status = (String) fStatus.getSelectedItem();
        String rem    = fRemarks.getText().trim();
        String attId  = fAttId.getText().trim();
        if (empId.isEmpty() || date.isEmpty()) { showToast("Employee ID and Date are required!", WARNING); return; }
        try {
            if ("NEW".equals(attId) || attId.isEmpty()) {
                pstmt = MdiForm.sconnect.prepareStatement(
                    "INSERT INTO employee_attendance(emp_id,att_date,status,remarks) VALUES(?,TO_DATE(?,'YYYY-MM-DD'),?,?)");
                pstmt.setString(1, empId); pstmt.setString(2, date);
                pstmt.setString(3, status); pstmt.setString(4, rem);
                pstmt.executeUpdate();
            } else {
                pstmt = MdiForm.sconnect.prepareStatement(
                    "UPDATE employee_attendance SET status=?,remarks=? WHERE att_id=?");
                pstmt.setString(1, status); pstmt.setString(2, rem);
                pstmt.setInt(3, Integer.parseInt(attId));
                pstmt.executeUpdate();
            }
            showToast("✔ Record saved.", ACCENT2);
            loadAttendanceForDate(); setDirty(false);
        } catch (SQLException ex) { showToast("Error: " + ex.getMessage(), DANGER); }
    }

    private void saveAllRows() {
        try {
            MdiForm.sconnect.setAutoCommit(false);
            for (String[] r : allRows) {
                if ("NEW".equals(r[C_ATT_ID])) {
                    pstmt = MdiForm.sconnect.prepareStatement(
                        "INSERT INTO employee_attendance(emp_id,att_date,status,remarks) VALUES(?,TO_DATE(?,'YYYY-MM-DD'),?,?)");
                    pstmt.setString(1, r[C_EMP_ID]); pstmt.setString(2, r[C_DATE]);
                    pstmt.setString(3, r[C_STATUS]);  pstmt.setString(4, r[C_REMARKS]);
                    pstmt.executeUpdate();
                } else {
                    pstmt = MdiForm.sconnect.prepareStatement(
                        "UPDATE employee_attendance SET status=?,remarks=? WHERE att_id=?");
                    pstmt.setString(1, r[C_STATUS]); pstmt.setString(2, r[C_REMARKS]);
                    pstmt.setInt(3, Integer.parseInt(r[C_ATT_ID]));
                    pstmt.executeUpdate();
                }
            }
            MdiForm.sconnect.commit(); MdiForm.sconnect.setAutoCommit(true);
            showToast("✔ All records saved.", ACCENT2);
            loadAttendanceForDate();
        } catch (SQLException ex) {
            try { MdiForm.sconnect.rollback(); MdiForm.sconnect.setAutoCommit(true); } catch (Exception ignored) {}
            showToast("Error: " + ex.getMessage(), DANGER);
        }
    }

    private void bulkMarkAll() {
        String status = (String) cmbBulkStatus.getSelectedItem();
        for (String[] r : allRows) r[C_STATUS] = status;
        applyFilter(); refreshStats();
        showToast("Bulk marked as " + status, ACCENT);
    }

    private void openAddDialog() {
        JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Add Attendance Record", true);
        dlg.setLayout(new BorderLayout()); dlg.setSize(420, 280); dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_MAIN);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBackground(BG_CARD); form.setBorder(new EmptyBorder(16, 16, 12, 16));

        JTextField nEmpId   = inputField(), nDate   = inputField(), nRemarks = inputField();
        JComboBox<String> nStatus = new JComboBox<>(STATUSES); styleComboBox(nStatus);
        nDate.setText(dbDate(selectedCal));

        form.add(labeled("Employee ID *", nEmpId));
        form.add(labeled("Date (YYYY-MM-DD) *", nDate));
        form.add(labeled("Status *", nStatus));
        form.add(labeled("Remarks", nRemarks));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        btns.setBackground(new Color(12, 18, 34));
        btns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));
        JButton save = makeBtn("💾 Save", ACCENT2, new Color(22,108,68));
        JButton cancel = makeBtn("✕ Cancel", TEXT_MUT, BG_INPUT);

        save.addActionListener(e -> {
            if (nEmpId.getText().trim().isEmpty() || nDate.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Employee ID and Date are required!"); return;
            }
            try {
                pstmt = MdiForm.sconnect.prepareStatement(
                    "INSERT INTO employee_attendance(emp_id,att_date,status,remarks) VALUES(?,TO_DATE(?,'YYYY-MM-DD'),?,?)");
                pstmt.setString(1, nEmpId.getText().trim()); pstmt.setString(2, nDate.getText().trim());
                pstmt.setString(3, (String) nStatus.getSelectedItem()); pstmt.setString(4, nRemarks.getText().trim());
                pstmt.executeUpdate();
                dlg.dispose(); loadAttendanceForDate(); showToast("✔ Record added.", ACCENT2);
            } catch (SQLException ex) { JOptionPane.showMessageDialog(dlg, "Error: " + ex.getMessage()); }
        });
        cancel.addActionListener(e -> dlg.dispose());
        btns.add(cancel); btns.add(save);

        dlg.add(new JScrollPane(form) {{ setBorder(null); getViewport().setBackground(BG_CARD); }}, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Filter + Table
    // ═════════════════════════════════════════════════════════════════════
    private void applyFilter() {
        String q = txtSearch != null ? txtSearch.getText().toLowerCase().trim() : "";
        filteredRows.clear();
        for (String[] r : allRows) {
            if (q.isEmpty() || r[C_NAME].toLowerCase().contains(q) || r[C_EMP_ID].toLowerCase().contains(q))
                filteredRows.add(r);
        }
        refreshTable();
        lblCount.setText(filteredRows.size() + " / " + allRows.size() + " records");
    }

    private void refreshTable() {
        tModel.setRowCount(0);
        for (int i = 0; i < filteredRows.size(); i++) {
            String[] r = filteredRows.get(i);
            tModel.addRow(new Object[]{ i+1, r[C_EMP_ID], r[C_NAME], r[C_DATE], r[C_STATUS], r[C_REMARKS] });
        }
    }

    private void refreshStats() {
        int total=allRows.size(), present=0, absent=0, half=0, leave=0;
        for (String[] r : allRows) {
            switch (r[C_STATUS]) {
                case "Present":  present++; break;
                case "Absent":   absent++;  break;
                case "Half-Day": half++;    break;
                case "Leave":    leave++;   break;
            }
        }
        lblTotal  .setText(total   + " Total");
        lblPresent.setText(present + " Present");
        lblAbsent .setText(absent  + " Absent");
        lblHalfDay.setText(half    + " Half-Day");
        lblLeave  .setText(leave   + " Leave");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Date Navigation
    // ═════════════════════════════════════════════════════════════════════
    private void shiftDay(int delta) {
        selectedCal.add(Calendar.DAY_OF_MONTH, delta);
        lblDateDisplay.setText(formatDateFull(selectedCal));
        if (fDate != null) fDate.setText(dbDate(selectedCal));
        loadAttendanceForDate();
    }

    private void goToday() {
        selectedCal = Calendar.getInstance();
        lblDateDisplay.setText(formatDateFull(selectedCal));
        if (fDate != null) fDate.setText(dbDate(selectedCal));
        loadAttendanceForDate();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Renderers
    // ═════════════════════════════════════════════════════════════════════
    private class StatusBadgeRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            String status = v != null ? v.toString() : "Absent";
            JLabel badge = new JLabel(status, SwingConstants.CENTER);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            badge.setOpaque(true);
            applyStatusColor(badge, status);
            badge.setBorder(new EmptyBorder(3, 8, 3, 8));
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            boolean isSel = masterTable.isRowSelected(row);
            p.setBackground(isSel ? BG_ROW_SEL : (row==hoveredRow?BG_ROW_HOVER:(row%2==0?BG_ROW_EVEN:BG_ROW_ODD)));
            p.add(badge); return p;
        }
    }

    private class StatusComboEditor extends DefaultCellEditor {
        private final JComboBox<String> combo;
        private StatusComboEditor(JComboBox<String> c) {
            super(c); combo = c; combo.setFont(F_INPUT);
            combo.setBackground(BG_INPUT); combo.setForeground(TEXT_PRI);
            combo.addActionListener(e -> {
                int vr = masterTable.getSelectedRow();
                if (vr >= 0 && vr < filteredRows.size()) {
                    filteredRows.get(vr)[C_STATUS] = (String) combo.getSelectedItem();
                    refreshStats();
                }
            });
        }
        StatusComboEditor() { this(new JComboBox<>(STATUSES)); }
        public Object getCellEditorValue() { return combo.getSelectedItem(); }
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI Helpers
    // ═════════════════════════════════════════════════════════════════════
    private static void applyStatusColor(JLabel l, String status) {
        switch (status) {
            case "Present":  l.setBackground(PRESENT_BG); l.setForeground(PRESENT_FG); break;
            case "Absent":   l.setBackground(ABSENT_BG);  l.setForeground(ABSENT_FG);  break;
            case "Half-Day": l.setBackground(HALF_BG);    l.setForeground(HALF_FG);    break;
            case "Leave":    l.setBackground(LEAVE_BG);   l.setForeground(LEAVE_FG);   break;
            default:         l.setBackground(BG_INPUT);   l.setForeground(TEXT_MUT);
        }
    }

    private JPanel sectionHeader(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SECTION_BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); p.setAlignmentX(0f);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        JLabel l = new JLabel(text.toUpperCase()); l.setFont(F_SEC); l.setForeground(ACCENT);
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

    private JButton navBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(TEXT_PRI); b.setOpaque(false);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }

    private JButton pillBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(F_SMALL); b.setForeground(TEXT_PRI); b.setOpaque(true);
        b.setBackground(BG_INPUT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1, true), new EmptyBorder(4, 10, 4, 10)));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
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

    private static String dbDate(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }
    private static String formatDateFull(Calendar cal) {
        return new SimpleDateFormat("EEE, dd MMM yyyy").format(cal.getTime());
    }
    private String nvl(String s) { return s == null ? "" : s; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Test"); JDesktopPane d = new JDesktopPane();
            d.setBackground(new Color(8, 12, 20)); f.setContentPane(d);
            f.setSize(1420, 820); f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationRelativeTo(null); f.setVisible(true);
            d.add(new EmployeeAttendance());
        });
    }
}