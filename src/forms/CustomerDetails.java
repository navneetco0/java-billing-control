package forms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import static ui.UIConstants.*;

/**
 * CustomerDetails — CRUA (Create, Read, Update, Archive) UI
 *
 * Table: customers
 * id, customer_name, mobile, email, address, city, state,
 * gst_no, opening_balance, balance_type, is_active, created_at
 */
public class CustomerDetails extends JInternalFrame {

    // ── DB ─────────────────────────────────────────────────────────────────
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot = 0;
    private long currentId = -1;
    private boolean dirty = false;

    // ── Master table ───────────────────────────────────────────────────────
    private DefaultTableModel tModel;
    private JTable masterTable;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel lblCount;

    // ── Filter bar ─────────────────────────────────────────────────────────
    private JTextField txtSearch;
    private JTextField txtFName, txtFCity, txtFState, txtFGst, txtFMobile;
    private JComboBox<String> cmbFActive, cmbFBalType;

    // ── Form fields ────────────────────────────────────────────────────────
    private JTextField fId, fName, fMobile, fEmail, fAddress;
    private JTextField fCity, fState, fGst, fOpenBal, fCreatedAt;
    private JComboBox<String> fBalType;
    private JCheckBox fIsActive;
    private JLabel lblDirty, lblArchiveBadge, lblBalBadge;

    // ── Buttons ────────────────────────────────────────────────────────────
    private JButton btnNew, btnSave, btnUpdate, btnArchive, btnRefresh;

    // ── Toast ──────────────────────────────────────────────────────────────
    private JLabel toastLbl;
    private javax.swing.Timer toastTimer;

    // ──────────────────────────────────────────────────────────────────────
    public CustomerDetails() {
        super("Customer Details", true, true, true, true);
        setSize(1300, 760);
        setLocation(30, 30);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ensureTable();
        buildUI();
        registerShortcuts();
        loadMaster();
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // DDL
    // ═════════════════════════════════════════════════════════════════════
    private void ensureTable() {
        try {
            stmt = MdiForm.sconnect.createStatement();
            try {
                stmt.executeUpdate(
                        "CREATE TABLE customers (" +
                                "  id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                                "  customer_name    VARCHAR2(150) NOT NULL," +
                                "  mobile           VARCHAR2(15)," +
                                "  email            VARCHAR2(150)," +
                                "  address          VARCHAR2(300)," +
                                "  city             VARCHAR2(100)," +
                                "  state            VARCHAR2(100)," +
                                "  gst_no           VARCHAR2(20)," +
                                "  opening_balance  NUMBER(12,2) DEFAULT 0," +
                                "  balance_type     VARCHAR2(10)," +
                                "  is_active        NUMBER(1) DEFAULT 1," +
                                "  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "  CONSTRAINT chk_cust_active   CHECK (is_active IN (0,1))," +
                                "  CONSTRAINT chk_cust_baltype  CHECK (balance_type IN ('DR','CR'))" +
                                ")");
            } catch (SQLException ignored) {
                /* already exists */ }
        } catch (SQLException ex) {
            showToast("DB Init Error: " + ex.getMessage(), DANGER);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI Shell
    // ═════════════════════════════════════════════════════════════════════
    private void buildUI() {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.setBackground(BG_MAIN);
        c.add(buildHeader(), BorderLayout.NORTH);
        c.add(buildSplit(), BorderLayout.CENTER);
        c.add(buildShortcutBar(), BorderLayout.SOUTH);
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout()) {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(18, 38, 78), getWidth(), 0, new Color(10, 14, 26)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT2);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        hdr.setPreferredSize(new Dimension(0, 54));
        hdr.setBorder(new EmptyBorder(0, 22, 0, 22));

        JLabel title = new JLabel("👤  Customer Management");
        title.setFont(F_HEAD);
        title.setForeground(TEXT_PRI);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        lblCount = styledBadge("0 customers", TEXT_MUT, BG_INPUT);
        right.add(lblCount);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    // ── Split ──────────────────────────────────────────────────────────────
    private JSplitPane buildSplit() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        sp.setDividerLocation(580);
        sp.setDividerSize(4);
        sp.setBorder(null);
        sp.setBackground(BG_MAIN);
        return sp;
    }

    // ═════════════════════════════════════════════════════════════════════
    // LEFT — Master table + filters
    // ═════════════════════════════════════════════════════════════════════
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.add(buildFilterBar(), BorderLayout.NORTH);
        p.add(buildMasterTable(), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFilterBar() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_PANEL);
        outer.setBorder(new EmptyBorder(10, 12, 8, 12));

        txtSearch = styledField("🔍  Search by Name, Mobile or Email…");
        txtSearch.setPreferredSize(new Dimension(0, 34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));

        JPanel r1 = new JPanel(new GridLayout(1, 5, 5, 0));
        r1.setBackground(BG_PANEL);
        r1.setBorder(new EmptyBorder(6, 0, 4, 0));
        txtFName = miniField("Name…");
        txtFMobile = miniField("Mobile…");
        txtFCity = miniField("City…");
        txtFState = miniField("State…");
        txtFGst = miniField("GST No…");
        r1.add(txtFName);
        r1.add(txtFMobile);
        r1.add(txtFCity);
        r1.add(txtFState);
        r1.add(txtFGst);

        JPanel r2 = new JPanel(new GridLayout(1, 2, 6, 0));
        r2.setBackground(BG_PANEL);
        cmbFActive = miniCombo("Status", "All", "Active", "Archived");
        cmbFBalType = miniCombo("Balance Type", "All", "DR", "CR");
        r2.add(cmbFActive);
        r2.add(cmbFBalType);

        for (JTextField tf : new JTextField[] { txtFName, txtFMobile, txtFCity, txtFState, txtFGst })
            tf.getDocument().addDocumentListener(dl(this::applyFilter));
        for (JComboBox<?> cb : new JComboBox[] { cmbFActive, cmbFBalType })
            cb.addActionListener(e -> applyFilter());

        outer.add(txtSearch, BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(2, 1, 0, 4));
        rows.setBackground(BG_PANEL);
        rows.add(r1);
        rows.add(r2);
        outer.add(rows, BorderLayout.CENTER);
        return outer;
    }

    private JScrollPane buildMasterTable() {
        // COL: 0=ID 1=Name 2=Mobile 3=Email 4=City 5=State 6=GST
        // 7=Opening Bal 8=Bal Type 9=Active
        String[] cols = { "ID", "Name", "Mobile", "Email", "City", "State", "GST No",
                "Opening Bal", "Bal Type", "Status" };
        tModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            public Class<?> getColumnClass(int c) {
                return c == 0 ? Long.class : c == 7 ? Double.class : String.class;
            }
        };

        masterTable = new JTable(tModel) {
            int hoverRow = -1;
            {
                addMouseMotionListener(new MouseMotionAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        int r = rowAtPoint(e.getPoint());
                        if (r != hoverRow) {
                            hoverRow = r;
                            repaint();
                        }
                    }
                });
                addMouseListener(new MouseAdapter() {
                    public void mouseExited(MouseEvent e) {
                        hoverRow = -1;
                        repaint();
                    }
                });
            }

            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component comp = super.prepareRenderer(tcr, row, col);
                boolean sel = isRowSelected(row);
                String activeVal = "";
                try {
                    activeVal = tModel.getValueAt(
                            masterTable.convertRowIndexToModel(row), 9).toString();
                } catch (Exception ignored) {
                }
                boolean archived = "Archived".equals(activeVal);

                if (sel) {
                    comp.setBackground(BG_ROW_SEL);
                    comp.setForeground(Color.WHITE);
                } else if (row == hoverRow) {
                    comp.setBackground(BG_ROW_HOVER);
                    comp.setForeground(TEXT_PRI);
                } else {
                    comp.setBackground(row % 2 == 0 ? BG_ROW_EVEN : BG_ROW_ODD);
                    comp.setForeground(archived ? ARCHIVED_COL : TEXT_PRI);
                }
                if (!sel) {
                    int mc = masterTable.convertColumnIndexToModel(col);
                    if (mc == 0)
                        comp.setForeground(ACCENT);
                    if (mc == 8)
                        comp.setForeground("DR".equals(getValueAt(row, col)) ? DANGER : ACCENT2);
                    if (mc == 9)
                        comp.setForeground(archived ? DANGER : ACCENT2);
                }
                ((JComponent) comp).setBorder(new EmptyBorder(4, 8, 4, 8));
                return comp;
            }
        };
        masterTable.setFont(F_TABLE);
        masterTable.setRowHeight(30);
        masterTable.setShowGrid(false);
        masterTable.setIntercellSpacing(new Dimension(0, 0));
        masterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        masterTable.setBackground(BG_PANEL);
        masterTable.setForeground(TEXT_PRI);
        masterTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] widths = { 50, 140, 100, 150, 90, 80, 110, 90, 70, 68 };
        for (int i = 0; i < widths.length; i++)
            masterTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JTableHeader th = masterTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBackground(BG_CARD);
        th.setForeground(TEXT_MUT);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        th.setReorderingAllowed(false);

        sorter = new TableRowSorter<>(tModel);
        masterTable.setRowSorter(sorter);
        masterTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                onRowSelected();
        });

        JScrollPane sc = new JScrollPane(masterTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(null);
        sc.getViewport().setBackground(BG_PANEL);
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
        outer.add(buildFormScroll(), BorderLayout.CENTER);
        outer.add(buildButtonBar(), BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildDetailHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_CARD);
        p.setBorder(new EmptyBorder(12, 20, 8, 20));
        JLabel title = new JLabel("Customer Record");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT_PRI);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        lblDirty = new JLabel("● Unsaved");
        lblDirty.setFont(F_SMALL);
        lblDirty.setForeground(WARNING);
        lblDirty.setVisible(false);
        lblBalBadge = styledBadge("", ACCENT2, new Color(10, 55, 32));
        lblArchiveBadge = styledBadge("", DANGER, new Color(70, 15, 15));
        lblArchiveBadge.setVisible(false);
        badges.add(lblDirty);
        badges.add(lblBalBadge);
        badges.add(lblArchiveBadge);
        p.add(title, BorderLayout.WEST);
        p.add(badges, BorderLayout.EAST);
        return p;
    }

    private JScrollPane buildFormScroll() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setBorder(new EmptyBorder(6, 18, 18, 18));

        // ── Identity ──────────────────────────────────────────────────
        form.add(sectionHeader("Identity"));
        JPanel id1 = gridPanel(3);
        fId = roField();
        fName = inputField();
        fMobile = inputField();
        id1.add(labeled("Customer ID (auto)", fId));
        id1.add(labeled("Name *", fName));
        id1.add(labeled("Mobile", fMobile));
        form.add(id1);

        JPanel id2 = gridPanel(2);
        fEmail = inputField();
        fGst = inputField();
        id2.add(labeled("Email", fEmail));
        id2.add(labeled("GST No", fGst));
        form.add(id2);

        // ── Address ───────────────────────────────────────────────────
        form.add(sectionHeader("Address"));
        fAddress = inputField();
        form.add(fullWidthLabeled("Address", fAddress));
        JPanel addr2 = gridPanel(2);
        fCity = inputField();
        fState = inputField();
        addr2.add(labeled("City", fCity));
        addr2.add(labeled("State", fState));
        form.add(addr2);

        // ── Financial ─────────────────────────────────────────────────
        form.add(sectionHeader("Opening Balance"));
        JPanel finPnl = gridPanel(2);
        fOpenBal = inputField();
        fBalType = new JComboBox<>(new String[] { "DR", "CR" });
        styleComboBox(fBalType);
        fBalType.addActionListener(e -> {
            updateBalBadge();
            setDirty(true);
        });
        finPnl.add(labeled("Opening Balance (₹)", fOpenBal));
        finPnl.add(labeled("Balance Type", fBalType));
        form.add(finPnl);

        // ── Flags ─────────────────────────────────────────────────────
        form.add(sectionHeader("Status"));
        JPanel flagPnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 6));
        flagPnl.setBackground(BG_CARD);
        flagPnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        fIsActive = styledCheck("Active");
        fIsActive.setSelected(true);
        fIsActive.addActionListener(e -> setDirty(true));
        flagPnl.add(fIsActive);
        form.add(flagPnl);

        // ── Timestamps ────────────────────────────────────────────────
        form.add(sectionHeader("Timestamps"));
        JPanel tsPnl = gridPanel(1);
        fCreatedAt = roField();
        tsPnl.add(labeled("Created At", fCreatedAt));
        form.add(tsPnl);

        // Dirty listeners
        DocumentListener dl = dl(() -> setDirty(true));
        for (JTextField tf : new JTextField[] { fName, fMobile, fEmail, fAddress, fCity, fState, fGst, fOpenBal })
            tf.getDocument().addDocumentListener(dl);
        fOpenBal.getDocument().addDocumentListener(dl(this::updateBalBadge));

        JScrollPane scroll = new JScrollPane(form,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll);
        return scroll;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        bar.setBackground(new Color(12, 18, 34));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));
        btnNew = makeBtn("＋ New", ACCENT, new Color(16, 62, 130));
        btnSave = makeBtn("💾 Save", ACCENT2, new Color(22, 108, 68));
        btnUpdate = makeBtn("✏ Update", GOLD, new Color(140, 96, 10));
        btnArchive = makeBtn("🗄 Archive", DANGER, new Color(140, 35, 35));
        btnRefresh = makeBtn("↺ Refresh", TEXT_MUT, BG_INPUT);
        btnNew.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveRecord());
        btnUpdate.addActionListener(e -> updateRecord());
        btnArchive.addActionListener(e -> archiveRecord());
        btnRefresh.addActionListener(e -> {
            loadMaster();
            setDirty(false);
        });
        bar.add(btnNew);
        bar.add(btnSave);
        bar.add(btnUpdate);
        bar.add(btnArchive);
        bar.add(btnRefresh);
        JLabel note = new JLabel("  🗄 Archive = deactivate, not delete");
        note.setFont(F_SMALL);
        note.setForeground(TEXT_MUT);
        bar.add(note);
        return bar;
    }

    private JPanel buildShortcutBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        p.setBackground(new Color(7, 10, 20));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(24, 34, 56)));
        for (String s : new String[] { "Ctrl+S  Save", "Ctrl+U  Update", "Ctrl+A  Archive",
                "Ctrl+N  New", "Esc  Refresh" }) {
            JLabel l = new JLabel(s);
            l.setFont(F_SMALL);
            l.setForeground(new Color(55, 78, 115));
            p.add(l);
        }
        toastLbl = new JLabel("");
        toastLbl.setFont(F_SMALL);
        toastLbl.setOpaque(true);
        toastLbl.setVisible(false);
        toastLbl.setBorder(new EmptyBorder(3, 12, 3, 12));
        p.add(Box.createHorizontalStrut(20));
        p.add(toastLbl);
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
        rp.registerKeyboardAction(e -> archiveRecord(),
                KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> clearForm(),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> {
            loadMaster();
            setDirty(false);
        },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data Operations
    // ═════════════════════════════════════════════════════════════════════
    private void loadMaster() {
        tModel.setRowCount(0);
        tot = 0;
        try {
            stmt = MdiForm.sconnect.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT id, customer_name, mobile, email, city, state, gst_no, " +
                            "opening_balance, balance_type, is_active FROM customers ORDER BY id");
            while (rs.next()) {
                tModel.addRow(new Object[] {
                        rs.getLong("id"),
                        rs.getString("customer_name"),
                        rs.getString("mobile"),
                        rs.getString("email"),
                        rs.getString("city"),
                        rs.getString("state"),
                        rs.getString("gst_no"),
                        rs.getDouble("opening_balance"),
                        rs.getString("balance_type"),
                        rs.getInt("is_active") == 1 ? "Active" : "Archived"
                });
                tot++;
            }
            lblCount.setText(tot + " customer" + (tot == 1 ? "" : "s"));
        } catch (SQLException ex) {
            showToast("Load error: " + ex.getMessage(), DANGER);
        }
    }

    private void onRowSelected() {
        int vr = masterTable.getSelectedRow();
        if (vr < 0)
            return;
        long pid = (Long) tModel.getValueAt(masterTable.convertRowIndexToModel(vr), 0);
        try {
            pstmt = MdiForm.sconnect.prepareStatement("SELECT * FROM customers WHERE id=?");
            pstmt.setLong(1, pid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                fillForm(rs);
                setDirty(false);
            }
        } catch (SQLException ex) {
            showToast("Load error: " + ex.getMessage(), DANGER);
        }
    }

    private void fillForm(ResultSet rs) throws SQLException {
        currentId = rs.getLong("id");
        fId.setText(String.valueOf(currentId));
        fName.setText(nvl(rs.getString("customer_name")));
        fMobile.setText(nvl(rs.getString("mobile")));
        fEmail.setText(nvl(rs.getString("email")));
        fAddress.setText(nvl(rs.getString("address")));
        fCity.setText(nvl(rs.getString("city")));
        fState.setText(nvl(rs.getString("state")));
        fGst.setText(nvl(rs.getString("gst_no")));
        fOpenBal.setText(fmt(rs.getDouble("opening_balance")));
        String bt = rs.getString("balance_type");
        fBalType.setSelectedItem(bt != null ? bt : "DR");
        fIsActive.setSelected(rs.getInt("is_active") == 1);

        Timestamp ca = rs.getTimestamp("created_at");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        fCreatedAt.setText(ca != null ? ca.toLocalDateTime().format(dtf) : "");

        boolean archived = rs.getInt("is_active") != 1;
        lblArchiveBadge.setText(archived ? "🗄 ARCHIVED" : "");
        lblArchiveBadge.setVisible(archived);
        updateBalBadge();
    }

    private void saveRecord() {
        if (fName.getText().trim().isEmpty()) {
            showToast("Customer Name is required!", WARNING);
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet generatedKeys = null;

        try {
            conn = MdiForm.sconnect;

            if (conn == null || conn.isClosed()) {
                showToast("❌ Database connection is not available!", DANGER);
                return;
            }

            // Use getGeneratedKeys() instead of RETURNING INTO — works reliably across
            // drivers
            ps = conn.prepareStatement(
                    "INSERT INTO customers(customer_name, mobile, email, address, city, state, " +
                            "gst_no, opening_balance, balance_type, is_active) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    new String[] { "ID" } // tell JDBC to return the generated "ID" column
            );

            bindAll(ps);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                showToast("❌ Insert failed — 0 rows affected. Check DB constraints.", DANGER);
                return;
            }

            generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys != null && generatedKeys.next()) {
                currentId = generatedKeys.getLong(1);
                fId.setText(String.valueOf(currentId));
                showToast("✔ Customer saved — ID: " + currentId, ACCENT2);
            } else {
                showToast("✔ Saved, but could not retrieve generated ID.", WARNING);
            }

            loadMaster();
            setDirty(false);

        } catch (SQLException ex) {
            // Print full stack trace to console for debugging
            ex.printStackTrace();

            String detail = ex.getMessage();
            int errCode = ex.getErrorCode();

            // Friendly messages for common Oracle error codes
            if (errCode == 1)
                detail = "Duplicate entry — a customer with this data already exists.";
            else if (errCode == 1400)
                detail = "A required field is NULL. Check Name and Balance Type.";
            else if (errCode == 2290)
                detail = "Check constraint violated — invalid Balance Type or Active flag.";
            else if (errCode == 17002 || errCode == 17008)
                detail = "Connection to database lost. Please restart the application.";

            showToast("❌ Save error [ORA-" + errCode + "]: " + detail, DANGER);

        } catch (Exception ex) {
            // Catch unexpected non-SQL errors (e.g. NullPointerException,
            // ClassCastException)
            ex.printStackTrace();
            showToast("❌ Unexpected error: " + ex.getClass().getSimpleName() + " — " + ex.getMessage(), DANGER);

        } finally {
            // Always close resources
            try {
                if (generatedKeys != null)
                    generatedKeys.close();
            } catch (Exception ignored) {
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void updateRecord() {
        if (currentId < 0) {
            showToast("Select a customer first!", WARNING);
            return;
        }
        if (fName.getText().trim().isEmpty()) {
            showToast("Name is required!", WARNING);
            return;
        }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                    "UPDATE customers SET customer_name=?,mobile=?,email=?,address=?,city=?,state=?," +
                            "gst_no=?,opening_balance=?,balance_type=?,is_active=? WHERE id=?");
            bindAll(pstmt);
            pstmt.setLong(11, currentId);
            int r = pstmt.executeUpdate();
            if (r > 0) {
                loadMaster();
                setDirty(false);
                showToast("✔ Customer updated.", ACCENT2);
            } else
                showToast("No customer matched ID " + currentId, WARNING);
        } catch (SQLException ex) {
            showToast("Update error: " + ex.getMessage(), DANGER);
        }
    }

    private void archiveRecord() {
        if (currentId < 0) {
            showToast("Select a customer to archive!", WARNING);
            return;
        }
        boolean alreadyArchived = !fIsActive.isSelected();
        String action = alreadyArchived ? "Restore" : "Archive";
        String msg = alreadyArchived
                ? "Restore customer ID " + currentId + "? They will become active again."
                : "Archive customer ID " + currentId + "? They will be deactivated (not deleted).";
        int ans = JOptionPane.showConfirmDialog(this, msg, action + " Customer",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION)
            return;
        try {
            pstmt = MdiForm.sconnect.prepareStatement("UPDATE customers SET is_active=? WHERE id=?");
            pstmt.setInt(1, alreadyArchived ? 1 : 0);
            pstmt.setLong(2, currentId);
            pstmt.executeUpdate();
            fIsActive.setSelected(alreadyArchived);
            lblArchiveBadge.setVisible(!alreadyArchived);
            loadMaster();
            showToast(alreadyArchived ? "✔ Customer restored." : "🗄 Customer archived.", WARNING);
        } catch (SQLException ex) {
            showToast("Archive error: " + ex.getMessage(), DANGER);
        }
    }

    private void bindAll(PreparedStatement ps) throws SQLException {
        String name = fName.getText().trim();
        String mobile = fMobile.getText().trim();
        String email = fEmail.getText().trim();
        String address = fAddress.getText().trim();
        String city = fCity.getText().trim();
        String state = fState.getText().trim();
        String gst = fGst.getText().trim();
        String balStr = fOpenBal.getText().trim();
        String balType = (String) fBalType.getSelectedItem();

        // Validate before binding
        if (name.isEmpty())
            throw new SQLException("Customer Name cannot be empty.");
        if (mobile.length() > 15)
            throw new SQLException("Mobile number too long (max 15 chars).");
        if (email.length() > 150)
            throw new SQLException("Email too long (max 150 chars).");
        if (balType == null || (!balType.equals("DR") && !balType.equals("CR")))
            throw new SQLException("Balance Type must be DR or CR.");

        double openingBal = 0;
        if (!balStr.isEmpty()) {
            try {
                openingBal = Double.parseDouble(balStr);
            } catch (NumberFormatException e) {
                throw new SQLException("Opening Balance must be a valid number. Got: '" + balStr + "'");
            }
        }

        ps.setString(1, name);
        setStrOrNull(ps, 2, mobile);
        setStrOrNull(ps, 3, email);
        setStrOrNull(ps, 4, address);
        setStrOrNull(ps, 5, city);
        setStrOrNull(ps, 6, state);
        setStrOrNull(ps, 7, gst);
        ps.setDouble(8, openingBal);
        ps.setString(9, balType);
        ps.setInt(10, fIsActive.isSelected() ? 1 : 0);
    }

    private void clearForm() {
        currentId = -1;
        for (JTextField tf : new JTextField[] { fId, fName, fMobile, fEmail, fAddress,
                fCity, fState, fGst, fOpenBal, fCreatedAt })
            tf.setText("");
        fId.setText("(auto)");
        fBalType.setSelectedIndex(0);
        fIsActive.setSelected(true);
        lblBalBadge.setText("");
        lblArchiveBadge.setVisible(false);
        masterTable.clearSelection();
        setDirty(false);
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
                        RowFilter.regexFilter("(?i)" + gs, 2),
                        RowFilter.regexFilter("(?i)" + gs, 3))));
            } catch (Exception ignored) {
            }
        }
        addColFilter(filters, txtFName.getText(), 1);
        addColFilter(filters, txtFMobile.getText(), 2);
        addColFilter(filters, txtFCity.getText(), 4);
        addColFilter(filters, txtFState.getText(), 5);
        addColFilter(filters, txtFGst.getText(), 6);

        String ac = (String) cmbFActive.getSelectedItem();
        if ("Active".equals(ac))
            addColFilter(filters, "Active", 9);
        if ("Archived".equals(ac))
            addColFilter(filters, "Archived", 9);
        String bt = (String) cmbFBalType.getSelectedItem();
        if (!"All".equals(bt))
            addColFilter(filters, bt, 8);

        try {
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        } catch (Exception ignored) {
        }
        lblCount.setText(masterTable.getRowCount() + " / " + tot + " customers");
    }

    private void addColFilter(List<RowFilter<DefaultTableModel, Object>> list, String val, int col) {
        if (val == null || val.trim().isEmpty())
            return;
        try {
            list.add(RowFilter.regexFilter("(?i)" + val.trim(), col));
        } catch (Exception ignored) {
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI Helpers
    // ═════════════════════════════════════════════════════════════════════
    private void updateBalBadge() {
        try {
            double bal = parseDbl(fOpenBal.getText());
            String bt = (String) fBalType.getSelectedItem();
            if (bal == 0) {
                lblBalBadge.setText("Nil Balance");
                lblBalBadge.setBackground(BG_INPUT);
                lblBalBadge.setForeground(TEXT_MUT);
            } else if ("DR".equals(bt)) {
                lblBalBadge.setText("DR ₹" + String.format("%.2f", bal));
                lblBalBadge.setBackground(new Color(70, 18, 18));
                lblBalBadge.setForeground(DANGER);
            } else {
                lblBalBadge.setText("CR ₹" + String.format("%.2f", bal));
                lblBalBadge.setBackground(new Color(10, 55, 32));
                lblBalBadge.setForeground(ACCENT2);
            }
        } catch (Exception e) {
            lblBalBadge.setText("");
        }
    }

    // ── Shared UI factory methods ─────────────────────────────────────────
    private JPanel sectionHeader(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SECTION_BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p.setAlignmentX(0f);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(4, 8, 4, 8)));
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(F_SEC);
        l.setForeground(ACCENT2);
        p.add(l, BorderLayout.WEST);
        return p;
    }

    private JPanel gridPanel(int cols) {
        JPanel p = new JPanel(new GridLayout(1, cols, 8, 0));
        p.setBackground(BG_CARD);
        p.setBorder(new EmptyBorder(6, 0, 4, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        p.setAlignmentX(0f);
        return p;
    }

    private JPanel labeled(String lbl, Component field) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setBackground(BG_CARD);
        JLabel l = new JLabel(lbl);
        l.setFont(F_LABEL);
        l.setForeground(TEXT_MUT);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel fullWidthLabeled(String lbl, Component field) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setBackground(BG_CARD);
        p.setAlignmentX(0f);
        p.setBorder(new EmptyBorder(6, 0, 4, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 32));
        JLabel l = new JLabel(lbl);
        l.setFont(F_LABEL);
        l.setForeground(TEXT_MUT);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JTextField inputField() {
        JTextField tf = new JTextField();
        tf.setFont(F_INPUT);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRI);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    private JTextField roField() {
        JTextField tf = inputField();
        tf.setEditable(false);
        tf.setBackground(new Color(14, 20, 38));
        tf.setForeground(ACCENT2);
        return tf;
    }

    private JCheckBox styledCheck(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setFont(F_INPUT);
        cb.setBackground(BG_CARD);
        cb.setForeground(TEXT_PRI);
        cb.setFocusPainted(false);
        return cb;
    }

    private void styleComboBox(JComboBox<?> cb) {
        cb.setFont(F_INPUT);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRI);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COL));
    }

    private JTextField styledField(String ph) {
        JTextField tf = new JTextField() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    g.setColor(TEXT_MUT);
                    g.setFont(F_SMALL);
                    g.drawString(ph, 10, getHeight() / 2 + 4);
                }
            }
        };
        tf.setFont(F_INPUT);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRI);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    private JTextField miniField(String ph) {
        JTextField tf = styledField(ph);
        tf.setPreferredSize(new Dimension(0, 28));
        return tf;
    }

    private JComboBox<String> miniCombo(String label, String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(F_SMALL);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRI);
        cb.setPreferredSize(new Dimension(0, 28));
        return cb;
    }

    private JLabel styledBadge(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text);
        l.setFont(F_SMALL);
        l.setForeground(fg);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        return l;
    }

    private JButton makeBtn(String text, Color fg, Color bg) {
        JButton b = new JButton(text) {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(fg.brighter());
                g2.setFont(F_BTN);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(112, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleScrollBar(JScrollPane sp) {
        for (JScrollBar sb : new JScrollBar[] { sp.getVerticalScrollBar(), sp.getHorizontalScrollBar() }) {
            if (sb == null)
                continue;
            sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                protected void configureScrollBarColors() {
                    thumbColor = new Color(46, 66, 108);
                    trackColor = BG_PANEL;
                }

                protected JButton createDecreaseButton(int o) {
                    return zb();
                }

                protected JButton createIncreaseButton(int o) {
                    return zb();
                }

                private JButton zb() {
                    JButton b = new JButton();
                    b.setPreferredSize(new Dimension(0, 0));
                    return b;
                }
            });
        }
    }

    private void setDirty(boolean d) {
        dirty = d;
        SwingUtilities.invokeLater(() -> lblDirty.setVisible(d));
    }

    private void showToast(String msg, Color bg) {
        if (toastLbl == null) {
            System.err.println("[Toast] " + msg);
            return;
        }
        toastLbl.setText(msg);
        toastLbl.setBackground(bg);
        toastLbl.setForeground(Color.WHITE);
        toastLbl.setVisible(true);
        if (toastTimer != null && toastTimer.isRunning())
            toastTimer.stop();
        toastTimer = new javax.swing.Timer(3400, e -> toastLbl.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    private DocumentListener dl(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            public void changedUpdate(DocumentEvent e) {
            }
        };
    }

    private double parseDbl(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String fmt(double d) {
        return d == 0 ? "" : String.format("%.2f", d);
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private void setStrOrNull(PreparedStatement ps, int idx, String val) throws SQLException {
        if (val == null || val.isEmpty())
            ps.setNull(idx, Types.VARCHAR);
        else
            ps.setString(idx, val);
    }
}