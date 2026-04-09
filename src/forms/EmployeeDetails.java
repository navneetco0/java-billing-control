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
import java.util.List;
import static ui.UIConstants.*;

public class EmployeeDetails extends JInternalFrame {
    // ── DB state ──────────────────────────────────────────────────────────
    private PreparedStatement pstmt;
    private Statement         stmt;
    private int  tot       = 0;
    /** -1 = new record not yet saved; ≥ 1 = existing Oracle-assigned ID */
    private long currentId = -1;
    private boolean dirty  = false;

    // ── Master table ──────────────────────────────────────────────────────
    private DefaultTableModel                 tModel;
    private JTable                            masterTable;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel lblCount;

    // ── Filters ───────────────────────────────────────────────────────────
    private JTextField txtSearch, txtFGender, txtFPhone;

    // ── Form fields ───────────────────────────────────────────────────────
    private JTextField        fId, fName, fDob, fDoj, fPhone, fMob, fEmail, fSal, fAddr, fNarr;
    private JComboBox<String> fGender;
    private JLabel            lblDirty, lblStatBadge;

    // ── Header badges ─────────────────────────────────────────────────────
    private JLabel lblTotal, lblMale, lblFemale, lblAvgSal;

    // ── Buttons ───────────────────────────────────────────────────────────
    private JButton btnNew, btnSave, btnUpdate, btnDelete, btnRefresh;

    // ── Toast ─────────────────────────────────────────────────────────────
    private JLabel              toastLbl;
    private javax.swing.Timer   toastTimer;

    // ─────────────────────────────────────────────────────────────────────
    public EmployeeDetails() {
        super("Employee Details", true, true, true, true);
        setSize(1380, 780);
        setLocation(40, 40);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        registerShortcuts();
        loadMaster();
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI Shell
    // ═════════════════════════════════════════════════════════════════════
    private void buildUI() {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.setBackground(BG_MAIN);
        c.add(buildHeader(),      BorderLayout.NORTH);
        c.add(buildSplit(),       BorderLayout.CENTER);
        c.add(buildShortcutBar(), BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout()) {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(14, 38, 60), getWidth(), 0, BG_MAIN));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        hdr.setPreferredSize(new Dimension(0, 54));
        hdr.setBorder(new EmptyBorder(0, 22, 0, 22));

        JLabel title = new JLabel("👤  Employee Details");
        title.setFont(F_HEAD); title.setForeground(TEXT_PRI);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        lblTotal  = styledBadge("0 Employees", ACCENT,   new Color(14, 38, 60));
        lblMale   = styledBadge("0 Male",       TEAL,     new Color(10, 40, 40));
        lblFemale = styledBadge("0 Female",     PINK,     new Color(50, 18, 30));
        lblAvgSal = styledBadge("₹0 Avg Sal",  GOLD,     new Color(40, 30,  8));
        lblCount  = styledBadge("0 records",   TEXT_MUT, BG_INPUT);
        right.add(lblTotal); right.add(lblMale); right.add(lblFemale);
        right.add(lblAvgSal); right.add(Box.createHorizontalStrut(8)); right.add(lblCount);

        hdr.add(title, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
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

        txtSearch = styledField("🔍  Search by name, phone, mobile or email…");
        txtSearch.setPreferredSize(new Dimension(0, 34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));

        JPanel r1 = new JPanel(new GridLayout(1, 2, 5, 0));
        r1.setBackground(BG_PANEL); r1.setBorder(new EmptyBorder(6, 0, 4, 0));
        txtFGender = miniField("Gender…");
        txtFPhone  = miniField("Phone…");
        r1.add(txtFGender); r1.add(txtFPhone);
        txtFGender.getDocument().addDocumentListener(dl(this::applyFilter));
        txtFPhone .getDocument().addDocumentListener(dl(this::applyFilter));

        outer.add(txtSearch, BorderLayout.NORTH);
        outer.add(r1,        BorderLayout.CENTER);
        return outer;
    }

    private JScrollPane buildMasterTable() {
        // COL: 0=ID  1=Name  2=Gender  3=Phone  4=Mobile  5=Email  6=Salary  7=Joined
        String[] cols = {"ID","Name","Gender","Phone","Mobile","Email","Salary (₹)","Joined"};
        tModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 0) return Long.class;
                if (c == 6) return Double.class;
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
                if      (sel)           { comp.setBackground(BG_ROW_SEL);   comp.setForeground(Color.WHITE); }
                else if (row==hoverRow) { comp.setBackground(BG_ROW_HOVER); comp.setForeground(TEXT_PRI); }
                else { comp.setBackground(row%2==0 ? BG_ROW_EVEN : BG_ROW_ODD); comp.setForeground(TEXT_PRI); }
                if (!sel) {
                    int mc = convertColumnIndexToModel(col);
                    if (mc == 0) comp.setForeground(ACCENT);
                    if (mc == 2) comp.setForeground(TEAL);
                    if (mc == 6) comp.setForeground(GOLD);
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

        int[] ws = {60, 140, 72, 100, 100, 160, 90, 90};
        for (int i = 0; i < ws.length; i++)
            masterTable.getColumnModel().getColumn(i).setPreferredWidth(ws[i]);

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
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(null); sc.getViewport().setBackground(BG_PANEL);
        styleScrollBar(sc);
        return sc;
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
        JLabel title = new JLabel("Employee Record");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14)); title.setForeground(TEXT_PRI);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        lblDirty = new JLabel("● Unsaved");
        lblDirty.setFont(F_SMALL); lblDirty.setForeground(WARNING); lblDirty.setVisible(false);
        lblStatBadge = styledBadge("", GOLD, new Color(60, 46, 10));
        badges.add(lblDirty); badges.add(lblStatBadge);
        p.add(title, BorderLayout.WEST); p.add(badges, BorderLayout.EAST);
        return p;
    }

    private JScrollPane buildFormScroll() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setBorder(new EmptyBorder(6, 18, 18, 18));

        // ── Identity section ──────────────────────────────────────────
        form.add(sectionHeader("Employee Identity"));
        JPanel id1 = gridPanel(2);

        // fId is ALWAYS read-only — Oracle generates the value.
        // Displayed in muted colour for new records, gold once assigned.
        fId = new JTextField("(auto)");
        fId.setEditable(false);
        fId.setFont(F_INPUT);
        fId.setBackground(BG_RO);
        fId.setForeground(TEXT_MUT);
        fId.setCaretColor(ACCENT);
        fId.setToolTipText("Oracle generates this value automatically (GENERATED ALWAYS AS IDENTITY)");
        fId.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4, 8, 4, 8)));

        fName = inputField();
        id1.add(labeled("Employee ID  ⟨Oracle auto-generates⟩", fId));
        id1.add(labeled("Full Name *", fName));
        form.add(id1);

        JPanel id2 = gridPanel(2);
        fGender = new JComboBox<>(new String[]{"Male","Female","Other"});
        styleComboBox(fGender);
        fDob = inputField();
        id2.add(labeled("Gender", fGender));
        id2.add(labeled("Date of Birth  (YYYY-MM-DD)", fDob));
        form.add(id2);

        // ── Contact section ───────────────────────────────────────────
        form.add(sectionHeader("Contact Information"));
        JPanel ct1 = gridPanel(2);
        fPhone = inputField(); fMob = inputField();
        ct1.add(labeled("Phone", fPhone));
        ct1.add(labeled("Mobile *", fMob));
        form.add(ct1);

        JPanel ct2 = gridPanel(1);
        fEmail = inputField();
        ct2.add(labeled("Email Address", fEmail));
        form.add(ct2);

        // ── Employment section ────────────────────────────────────────
        form.add(sectionHeader("Employment Details"));
        JPanel em1 = gridPanel(2);
        fDoj = inputField(); fSal = inputField();
        em1.add(labeled("Date of Joining  (YYYY-MM-DD)", fDoj));
        em1.add(labeled("Basic Salary (₹)", fSal));
        form.add(em1);

        // ── Address / Narration ───────────────────────────────────────
        form.add(sectionHeader("Address & Notes"));
        JPanel an1 = gridPanel(2);
        fAddr = inputField(); fNarr = inputField();
        an1.add(labeled("Address", fAddr));
        an1.add(labeled("Narration", fNarr));
        form.add(an1);

        // Dirty listeners (fId excluded — it is read-only)
        DocumentListener dirtyDl = dl(() -> setDirty(true));
        for (JTextField tf : new JTextField[]{fName, fDob, fPhone, fMob, fEmail, fDoj, fSal, fAddr, fNarr})
            tf.getDocument().addDocumentListener(dirtyDl);
        fGender.addActionListener(e -> setDirty(true));

        // Live salary badge
        fSal.getDocument().addDocumentListener(dl(() -> {
            try {
                double s = Double.parseDouble(fSal.getText().trim());
                if (s > 0) { lblStatBadge.setText("₹" + String.format("%,.0f", s)); lblStatBadge.setForeground(GOLD); }
                else        lblStatBadge.setText("");
            } catch (Exception ignored) { lblStatBadge.setText(""); }
        }));

        JScrollPane scroll = new JScrollPane(form,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null); scroll.getViewport().setBackground(BG_CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll);
        return scroll;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        bar.setBackground(new Color(12, 18, 34));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));
        btnNew     = makeBtn("＋ New",     ACCENT,   new Color(16,  62, 130));
        btnSave    = makeBtn("💾 Save",    ACCENT2,  new Color(22, 108,  68));
        btnUpdate  = makeBtn("✏ Update",  GOLD,     new Color(140,  96,  10));
        btnDelete  = makeBtn("🗑 Delete",  DANGER,   new Color(140,  35,  35));
        btnRefresh = makeBtn("↺ Refresh", TEXT_MUT, BG_INPUT);
        btnNew    .addActionListener(e -> clearForm());
        btnSave   .addActionListener(e -> saveRecord());
        btnUpdate .addActionListener(e -> updateRecord());
        btnDelete .addActionListener(e -> deleteRecord());
        btnRefresh.addActionListener(e -> { loadMaster(); clearForm(); });
        bar.add(btnNew); bar.add(btnSave); bar.add(btnUpdate);
        bar.add(btnDelete); bar.add(btnRefresh);
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
    // Keyboard shortcuts
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
        rp.registerKeyboardAction(e -> { loadMaster(); clearForm(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data — Load
    // ═════════════════════════════════════════════════════════════════════
    private void loadMaster() {
        tModel.setRowCount(0); tot = 0;
        int male = 0, female = 0; double totalSal = 0;
        try {
            checkConnection();
            stmt = MdiForm.sconnect.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT emp_id, emp_name, gender, phone, mobile_no, email_id, bsalary, doj" +
                " FROM employee_details ORDER BY emp_id");
            while (rs.next()) {
                String g   = nvl(rs.getString("gender"));
                double sal = rs.getDouble("bsalary");
                tModel.addRow(new Object[]{
                    rs.getLong  ("emp_id"),
                    nvl(rs.getString("emp_name")),
                    g,
                    nvl(rs.getString("phone")),
                    nvl(rs.getString("mobile_no")),
                    nvl(rs.getString("email_id")),
                    sal,
                    nvl(rs.getString("doj"))
                });
                tot++;
                if      ("Male".equalsIgnoreCase(g))   male++;
                else if ("Female".equalsIgnoreCase(g)) female++;
                totalSal += sal;
            }
        } catch (SQLException ex) {
            showError("Failed to load employees.\n\n" + sqlDetail(ex));
            return;
        }
        double avg = tot > 0 ? totalSal / tot : 0;
        lblTotal .setText(tot + " Employees");
        lblMale  .setText(male + " Male");
        lblFemale.setText(female + " Female");
        lblAvgSal.setText("₹" + String.format("%,.0f", avg) + " Avg");
        applyFilter();
    }

    private void onRowSelected() {
        int vr = masterTable.getSelectedRow();
        if (vr < 0) return;
        long id = (Long) tModel.getValueAt(masterTable.convertRowIndexToModel(vr), 0);
        try {
            checkConnection();
            pstmt = MdiForm.sconnect.prepareStatement(
                    "SELECT * FROM employee_details WHERE emp_id = ?");
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                fillForm(rs);
                setDirty(false);
            } else {
                showToast("⚠ Record ID " + id + " no longer exists!", WARNING);
            }
        } catch (SQLException ex) {
            showError("Failed to load employee record.\n\n" + sqlDetail(ex));
        }
    }

    private void fillForm(ResultSet rs) throws SQLException {
        currentId = rs.getLong("emp_id");

        // Show Oracle-assigned ID in gold
        fId.setText(String.valueOf(currentId));
        fId.setForeground(GOLD);

        fName .setText(nvl(rs.getString("emp_name")));
        String g = nvl(rs.getString("gender"));
        for (int i = 0; i < fGender.getItemCount(); i++)
            if (fGender.getItemAt(i).equalsIgnoreCase(g)) { fGender.setSelectedIndex(i); break; }
        fDob  .setText(nvl(rs.getString("dob")));
        fDoj  .setText(nvl(rs.getString("doj")));
        fPhone.setText(nvl(rs.getString("phone")));
        fMob  .setText(nvl(rs.getString("mobile_no")));
        fEmail.setText(nvl(rs.getString("email_id")));
        fAddr .setText(nvl(rs.getString("address")));
        fNarr .setText(nvl(rs.getString("narration")));
        double sal = rs.getDouble("bsalary");
        fSal.setText(sal == 0 ? "" : String.format("%.2f", sal));
        lblStatBadge.setText(sal > 0 ? "₹" + String.format("%,.0f", sal) : "");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data — INSERT
    //
    // CRITICAL: emp_id is NEVER included in the INSERT statement.
    // Oracle's identity column fills it from an internal sequence.
    // Passing emp_id would cause ORA-32795.
    // We recover the generated value via getGeneratedKeys().
    // ═════════════════════════════════════════════════════════════════════
    private void saveRecord() {
        if (currentId != -1) {
            showToast("⚠ An existing record is loaded. Click ＋ New to add another.", WARNING);
            return;
        }
        if (fName.getText().trim().isEmpty()) {
            showToast("⚠ Full Name is required!", WARNING); fName.requestFocus(); return;
        }
        if (fMob.getText().trim().isEmpty()) {
            showToast("⚠ Mobile number is required!", WARNING); fMob.requestFocus(); return;
        }

        try {
            checkConnection();

            // ── emp_id is deliberately excluded from this INSERT ──────
            String sql =
                "INSERT INTO employee_details" +
                " (emp_name, gender, dob, doj, phone, mobile_no, email_id, bsalary, address, narration)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // Tell JDBC which column to return so we can read the generated ID
            pstmt = MdiForm.sconnect.prepareStatement(sql, new String[]{"emp_id"});
            bindInsert(pstmt);

            int rows = pstmt.executeUpdate();
            if (rows <= 0) {
                showError("INSERT executed but 0 rows were affected.\nCheck DB constraints and retry.");
                return;
            }

            // ── Read the Oracle-generated emp_id ──────────────────────
            long generatedId = -1;
            try (ResultSet gk = pstmt.getGeneratedKeys()) {
                if (gk.next()) generatedId = gk.getLong(1);
            }

            // Fallback (some thin JDBC drivers don't support getGeneratedKeys for identity columns)
            if (generatedId == -1) {
                try (Statement s = MdiForm.sconnect.createStatement();
                     ResultSet r = s.executeQuery(
                         "SELECT MAX(emp_id) FROM employee_details")) {
                    if (r.next()) generatedId = r.getLong(1);
                }
            }

            // Update UI with the real ID
            currentId = generatedId;
            fId.setText(generatedId > 0 ? String.valueOf(generatedId) : "?");
            fId.setForeground(GOLD);

            showToast("✔ Employee saved  —  ID assigned by Oracle: " + generatedId, ACCENT2);
            loadMaster();
            setDirty(false);
            if (generatedId > 0) selectRowById(generatedId);

        } catch (SQLException ex) {
            showError("Save failed.\n\n" + sqlDetail(ex));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data — UPDATE
    // ═════════════════════════════════════════════════════════════════════
    private void updateRecord() {
        if (currentId == -1) {
            showToast("⚠ Select an employee from the list first!", WARNING); return;
        }
        if (fName.getText().trim().isEmpty()) {
            showToast("⚠ Full Name is required!", WARNING); fName.requestFocus(); return;
        }
        if (fMob.getText().trim().isEmpty()) {
            showToast("⚠ Mobile number is required!", WARNING); fMob.requestFocus(); return;
        }
        try {
            checkConnection();
            // emp_id is used in WHERE only — never SET (it's the PK identity)
            pstmt = MdiForm.sconnect.prepareStatement(
                "UPDATE employee_details SET" +
                " emp_name=?, gender=?, dob=?, doj=?, phone=?," +
                " mobile_no=?, email_id=?, bsalary=?, address=?, narration=?" +
                " WHERE emp_id=?");
            pstmt.setString(1,  fName.getText().trim());
            pstmt.setString(2,  (String) fGender.getSelectedItem());
            pstmt.setString(3,  fDob.getText().trim());
            pstmt.setString(4,  fDoj.getText().trim());
            pstmt.setString(5,  fPhone.getText().trim());
            pstmt.setString(6,  fMob.getText().trim());
            pstmt.setString(7,  fEmail.getText().trim());
            pstmt.setDouble(8,  parseDbl(fSal.getText()));
            pstmt.setString(9,  fAddr.getText().trim());
            pstmt.setString(10, fNarr.getText().trim());
            pstmt.setLong  (11, currentId);
            int r = pstmt.executeUpdate();
            if (r > 0) {
                long updatedId = currentId;
                loadMaster(); setDirty(false);
                showToast("✔ Employee #" + updatedId + " updated.", ACCENT2);
                selectRowById(updatedId);
            } else {
                showError("UPDATE matched 0 rows for emp_id=" + currentId +
                          ".\nThe record may have been deleted externally.");
            }
        } catch (SQLException ex) {
            showError("Update failed.\n\n" + sqlDetail(ex));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data — DELETE
    // ═════════════════════════════════════════════════════════════════════
    private void deleteRecord() {
        if (currentId == -1) {
            showToast("⚠ Select an employee to delete!", WARNING); return;
        }
        int ans = JOptionPane.showConfirmDialog(this,
            "Permanently delete Employee ID " + currentId + "?\nThis cannot be undone.",
            "Delete Employee", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;
        try {
            checkConnection();
            pstmt = MdiForm.sconnect.prepareStatement(
                    "DELETE FROM employee_details WHERE emp_id=?");
            pstmt.setLong(1, currentId);
            int r = pstmt.executeUpdate();
            if (r > 0) {
                showToast("🗑 Employee #" + currentId + " deleted.", DANGER);
                clearForm(); loadMaster();
            } else {
                showError("DELETE matched 0 rows for emp_id=" + currentId +
                          ".\nThe record may have already been removed.");
            }
        } catch (SQLException ex) {
            showError("Delete failed.\n\n" + sqlDetail(ex));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Bind helpers
    // ═════════════════════════════════════════════════════════════════════
    /**
     * Binds 10 editable columns for INSERT.
     * emp_id is NOT bound — Oracle's identity sequence fills it.
     * Order: emp_name, gender, dob, doj, phone, mobile_no, email_id, bsalary, address, narration
     */
    private void bindInsert(PreparedStatement ps) throws SQLException {
        ps.setString(1,  fName.getText().trim());
        ps.setString(2,  (String) fGender.getSelectedItem());
        ps.setString(3,  fDob.getText().trim());
        ps.setString(4,  fDoj.getText().trim());
        ps.setString(5,  fPhone.getText().trim());
        ps.setString(6,  fMob.getText().trim());
        ps.setString(7,  fEmail.getText().trim());
        ps.setDouble(8,  parseDbl(fSal.getText()));
        ps.setString(9,  fAddr.getText().trim());
        ps.setString(10, fNarr.getText().trim());
    }

    private void clearForm() {
        currentId = -1;
        // Reset ID field to hint state
        fId.setText("(auto)");
        fId.setForeground(TEXT_MUT);
        for (JTextField tf : new JTextField[]{fName, fDob, fDoj, fPhone, fMob, fEmail, fSal, fAddr, fNarr})
            tf.setText("");
        fGender.setSelectedIndex(0);
        lblStatBadge.setText("");
        masterTable.clearSelection();
        setDirty(false);
        SwingUtilities.invokeLater(() -> fName.requestFocus());
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
                    RowFilter.regexFilter("(?i)" + gs, 1),   // Name
                    RowFilter.regexFilter("(?i)" + gs, 3),   // Phone
                    RowFilter.regexFilter("(?i)" + gs, 4),   // Mobile
                    RowFilter.regexFilter("(?i)" + gs, 5)))); // Email
            } catch (Exception ignored) {}
        }
        addColFilter(filters, txtFGender.getText(), 2);
        addColFilter(filters, txtFPhone .getText(), 3);
        try { sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters)); }
        catch (Exception ignored) {}
        lblCount.setText(masterTable.getRowCount() + " / " + tot + " employees");
    }

    private void addColFilter(List<RowFilter<DefaultTableModel, Object>> list, String val, int col) {
        if (val == null || val.trim().isEmpty()) return;
        try { list.add(RowFilter.regexFilter("(?i)" + val.trim(), col)); }
        catch (Exception ignored) {}
    }

    /** Re-selects and scrolls to the row with the given emp_id after save/update. */
    private void selectRowById(long id) {
        for (int i = 0; i < masterTable.getRowCount(); i++) {
            Object val = tModel.getValueAt(masterTable.convertRowIndexToModel(i), 0);
            if (val instanceof Long && (Long) val == id) {
                masterTable.setRowSelectionInterval(i, i);
                masterTable.scrollRectToVisible(masterTable.getCellRect(i, 0, true));
                return;
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI Helpers
    // ═════════════════════════════════════════════════════════════════════
    private JPanel sectionHeader(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SECTION_BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); p.setAlignmentX(0f);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(4, 8, 4, 8)));
        JLabel l = new JLabel(text.toUpperCase()); l.setFont(F_SEC); l.setForeground(ACCENT);
        p.add(l, BorderLayout.WEST); return p;
    }

    private JPanel gridPanel(int cols) {
        JPanel p = new JPanel(new GridLayout(1, cols, 8, 0));
        p.setBackground(BG_CARD); p.setBorder(new EmptyBorder(6, 0, 4, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62)); p.setAlignmentX(0f);
        return p;
    }

    private JPanel labeled(String lbl, Component field) {
        JPanel p = new JPanel(new BorderLayout(0, 3)); p.setBackground(BG_CARD);
        JLabel l = new JLabel(lbl); l.setFont(F_LABEL); l.setForeground(TEXT_MUT);
        p.add(l, BorderLayout.NORTH); p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JTextField inputField() {
        JTextField tf = new JTextField(); tf.setFont(F_INPUT);
        tf.setBackground(BG_INPUT); tf.setForeground(TEXT_PRI); tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        return tf;
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
                g2.setColor(getModel().isPressed()  ? bg.darker()
                          : getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(fg.brighter()); g2.setFont(F_BTN);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setPreferredSize(new Dimension(112, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleScrollBar(JScrollPane sp) {
        for (JScrollBar sb : new JScrollBar[]{sp.getVerticalScrollBar(), sp.getHorizontalScrollBar()}) {
            if (sb == null) continue;
            sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                protected void configureScrollBarColors() { thumbColor = new Color(46,66,108); trackColor = BG_PANEL; }
                protected JButton createDecreaseButton(int o) { return zb(); }
                protected JButton createIncreaseButton(int o) { return zb(); }
                private JButton zb() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
            });
        }
    }

    private void setDirty(boolean d) {
        dirty = d; SwingUtilities.invokeLater(() -> lblDirty.setVisible(d));
    }

    /** Non-blocking bottom toast — for soft confirmations and warnings. */
    private void showToast(String msg, Color bg) {
        if (toastLbl == null) return;
        SwingUtilities.invokeLater(() -> {
            toastLbl.setText(msg); toastLbl.setBackground(bg); toastLbl.setForeground(Color.WHITE);
            toastLbl.setVisible(true);
            if (toastTimer != null && toastTimer.isRunning()) toastTimer.stop();
            toastTimer = new javax.swing.Timer(3400, e -> toastLbl.setVisible(false));
            toastTimer.setRepeats(false); toastTimer.start();
        });
    }

    /** Modal error dialog — for DB failures that must not be missed. */
    private void showError(String message) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE));
    }

    /** Throws SQLException with a clear message if the connection is absent or closed. */
    private void checkConnection() throws SQLException {
        if (MdiForm.sconnect == null || MdiForm.sconnect.isClosed())
            throw new SQLException("Database connection is closed or unavailable.");
    }

    /** Formats a SQLException into a human-readable multi-line string. */
    private String sqlDetail(SQLException ex) {
        return "Error Code : " + ex.getErrorCode() + "\n"
             + "SQL State  : " + ex.getSQLState()  + "\n"
             + "Message    : " + ex.getMessage();
    }

    private DocumentListener dl(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { r.run(); }
            public void removeUpdate(DocumentEvent e)  { r.run(); }
            public void changedUpdate(DocumentEvent e) {}
        };
    }

    private double parseDbl(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }
    private String nvl(String s) { return s == null ? "" : s; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Test"); JDesktopPane d = new JDesktopPane();
            d.setBackground(new Color(8, 12, 20)); f.setContentPane(d);
            f.setSize(1420, 820); f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationRelativeTo(null); f.setVisible(true);
            d.add(new EmployeeDetails());
        });
    }
}