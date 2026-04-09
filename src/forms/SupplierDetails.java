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
 * SupplierDetails — CRUA (Create, Read, Update, Archive) UI
 *
 * Table: suppliers
 * supplier_id, supplier_name, contact_person, phone, email,
 * address, city, state, pincode, country, gst_number, pan_number,
 * is_active (soft-archive flag — added if absent),
 * created_at, updated_at
 *
 * Features:
 * • Auto-creates / migrates table
 * • Master table with all key columns, sortable + filterable
 * • Real-time global search + per-field filters for every column
 * • Detail panel: all fields, scrollable
 * • Archive / Restore (is_active toggle, not DELETE)
 * • Toast notifications & unsaved-changes indicator
 * • Keyboard shortcuts Ctrl+S / U / A / N / Esc
 */
public class SupplierDetails extends JInternalFrame {

    // ── DB ─────────────────────────────────────────────────────────────────
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot = 0;
    private boolean dirty = false;
    private long currentId = -1;

    // ── Master table ───────────────────────────────────────────────────────
    private DefaultTableModel tModel;
    private JTable masterTable;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel lblCount;

    // ── Filter bar fields ──────────────────────────────────────────────────
    private JTextField txtSearch; // global
    private JTextField fltName, fltContact, fltPhone;
    private JTextField fltEmail, fltCity, fltState;
    private JTextField fltPincode, fltCountry, fltGst, fltPan;
    private JComboBox<String> cmbFltActive;

    // ── Detail form fields ─────────────────────────────────────────────────
    private JTextField fId, fName, fContact, fPhone, fEmail;
    private JTextArea fAddress;
    private JTextField fCity, fState, fPincode, fCountry;
    private JTextField fGst, fPan;
    private JTextField fCreatedAt, fUpdatedAt;
    private JCheckBox fIsActive;
    private JLabel lblDirty, lblArchiveBadge;

    // ── Buttons ────────────────────────────────────────────────────────────
    private JButton btnNew, btnSave, btnUpdate, btnArchive, btnRefresh;

    // ── Toast ──────────────────────────────────────────────────────────────
    private JLabel toastLbl;
    private javax.swing.Timer toastTimer;

    // ──────────────────────────────────────────────────────────────────────
    public SupplierDetails() {
        super("Supplier Details", true, true, true, true);
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

            // Create suppliers table (Oracle syntax)
            try {
                stmt.executeUpdate(
                        "CREATE TABLE suppliers (" +
                                "  id             NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                                "  supplier_name  VARCHAR2(255) NOT NULL," +
                                "  contact_person VARCHAR2(255)," +
                                "  phone          VARCHAR2(50)  NOT NULL," +
                                "  email          VARCHAR2(255)," +
                                "  address        CLOB," +
                                "  city           VARCHAR2(100)," +
                                "  state          VARCHAR2(100)," +
                                "  pincode        VARCHAR2(20)," +
                                "  country        VARCHAR2(100) DEFAULT 'India'," +
                                "  gst_number     VARCHAR2(50)," +
                                "  pan_number     VARCHAR2(50)," +
                                "  is_active      NUMBER(1)     DEFAULT 1," +
                                "  created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP," +
                                "  updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP," +
                                "  CONSTRAINT chk_sup_active CHECK (is_active IN (0,1))" +
                                ")");
            } catch (SQLException ignored) {
                /* table already exists */ }

            // Add is_active if missing (migration)
            try {
                stmt.executeUpdate(
                        "ALTER TABLE suppliers ADD is_active NUMBER(1) DEFAULT 1");
            } catch (SQLException ignored) {
                /* column already exists */ }

            // Add updated_at trigger
            try {
                stmt.executeUpdate(
                        "CREATE OR REPLACE TRIGGER trg_suppliers_updated_at " +
                                "BEFORE UPDATE ON suppliers " +
                                "FOR EACH ROW BEGIN " +
                                "  :NEW.updated_at := CURRENT_TIMESTAMP; " +
                                "END;");
            } catch (SQLException ignored) {
            }

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
                g2.setPaint(new GradientPaint(0, 0, new Color(18, 38, 78),
                        getWidth(), 0, new Color(10, 14, 26)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(PURPLE);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        hdr.setPreferredSize(new Dimension(0, 54));
        hdr.setBorder(new EmptyBorder(0, 22, 0, 22));

        JLabel title = new JLabel("🏭  Supplier Management");
        title.setFont(F_HEAD);
        title.setForeground(TEXT_PRI);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        lblCount = styledBadge("0 suppliers", TEXT_MUT, BG_INPUT);
        right.add(lblCount);

        hdr.add(title, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    // ── Split ──────────────────────────────────────────────────────────────
    private JSplitPane buildSplit() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        sp.setDividerLocation(620);
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

    // ── Filter bar ─────────────────────────────────────────────────────────
    private JPanel buildFilterBar() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_PANEL);
        outer.setBorder(new EmptyBorder(10, 12, 8, 12));

        // Global search
        txtSearch = styledField("🔍  Search by Name, Phone, Email, City, GST…");
        txtSearch.setPreferredSize(new Dimension(0, 34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));

        // Row 1: name, contact, phone, email
        JPanel r1 = new JPanel(new GridLayout(1, 4, 5, 0));
        r1.setBackground(BG_PANEL);
        r1.setBorder(new EmptyBorder(6, 0, 3, 0));
        fltName = miniField("Name…");
        fltContact = miniField("Contact…");
        fltPhone = miniField("Phone…");
        fltEmail = miniField("Email…");
        r1.add(fltName);
        r1.add(fltContact);
        r1.add(fltPhone);
        r1.add(fltEmail);

        // Row 2: city, state, pincode, country
        JPanel r2 = new JPanel(new GridLayout(1, 4, 5, 0));
        r2.setBackground(BG_PANEL);
        r2.setBorder(new EmptyBorder(3, 0, 3, 0));
        fltCity = miniField("City…");
        fltState = miniField("State…");
        fltPincode = miniField("Pincode…");
        fltCountry = miniField("Country…");
        r2.add(fltCity);
        r2.add(fltState);
        r2.add(fltPincode);
        r2.add(fltCountry);

        // Row 3: GST, PAN, Active status
        JPanel r3 = new JPanel(new GridLayout(1, 4, 5, 0));
        r3.setBackground(BG_PANEL);
        r3.setBorder(new EmptyBorder(3, 0, 0, 0));
        fltGst = miniField("GST Number…");
        fltPan = miniField("PAN Number…");
        cmbFltActive = miniCombo("Status", "All", "Active", "Archived");
        JLabel spacer = new JLabel();
        spacer.setOpaque(false);
        r3.add(fltGst);
        r3.add(fltPan);
        r3.add(cmbFltActive);
        r3.add(spacer);

        // Wire listeners
        for (JTextField tf : new JTextField[] {
                fltName, fltContact, fltPhone, fltEmail,
                fltCity, fltState, fltPincode, fltCountry, fltGst, fltPan })
            tf.getDocument().addDocumentListener(dl(this::applyFilter));
        cmbFltActive.addActionListener(e -> applyFilter());

        outer.add(txtSearch, BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(3, 1, 0, 0));
        rows.setBackground(BG_PANEL);
        rows.add(r1);
        rows.add(r2);
        rows.add(r3);
        outer.add(rows, BorderLayout.CENTER);
        return outer;
    }

    // ── Master table ───────────────────────────────────────────────────────
    // COL map: 0=ID 1=Name 2=Contact 3=Phone 4=Email 5=City 6=State
    // 7=Country 8=GST 9=PAN 10=Status
    private JScrollPane buildMasterTable() {
        String[] cols = {
                "ID", "Name", "Contact Person", "Phone", "Email",
                "City", "State", "Country", "GST No.", "PAN No.", "Status"
        };
        tModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            public Class<?> getColumnClass(int c) {
                return c == 0 ? Integer.class : String.class;
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
                String status = "";
                try {
                    status = tModel.getValueAt(
                            masterTable.convertRowIndexToModel(row), 10).toString();
                } catch (Exception ignored) {
                }
                boolean archived = "Archived".equals(status);

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
                        comp.setForeground(PURPLE);
                    if (mc == 10)
                        comp.setForeground(archived ? DANGER : ACCENT2);
                    if (mc == 3)
                        comp.setForeground(GOLD);
                    if (mc == 8 || mc == 9)
                        comp.setForeground(ACCENT);
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

        int[] widths = { 48, 150, 120, 100, 155, 80, 80, 70, 110, 90, 72 };
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

        JLabel title = new JLabel("Supplier Record");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT_PRI);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        lblDirty = new JLabel("● Unsaved");
        lblDirty.setFont(F_SMALL);
        lblDirty.setForeground(WARNING);
        lblDirty.setVisible(false);
        lblArchiveBadge = styledBadge("🗄 ARCHIVED", DANGER, new Color(70, 15, 15));
        lblArchiveBadge.setVisible(false);
        badges.add(lblDirty);
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

        // ── Identity ───────────────────────────────────────────────────
        form.add(sectionHeader("Identity"));
        JPanel id1 = gridPanel(3);
        fId = roField();
        fName = inputField();
        fContact = inputField();
        id1.add(labeled("Supplier ID (auto)", fId));
        id1.add(labeled("Supplier Name *", fName));
        id1.add(labeled("Contact Person", fContact));
        form.add(id1);

        JPanel id2 = gridPanel(2);
        fPhone = inputField();
        fEmail = inputField();
        id2.add(labeled("Phone *", fPhone));
        id2.add(labeled("Email", fEmail));
        form.add(id2);

        // ── Address ────────────────────────────────────────────────────
        form.add(sectionHeader("Address"));
        fAddress = new JTextArea(3, 1);
        styleTextArea(fAddress);
        JScrollPane addrScroll = new JScrollPane(fAddress);
        addrScroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        addrScroll.setPreferredSize(new Dimension(0, 72));
        addrScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        styleScrollBar(addrScroll);
        form.add(fullWidthLabeled("Address", addrScroll));

        JPanel addrGrid = gridPanel(4);
        fCity = inputField();
        fState = inputField();
        fPincode = inputField();
        fCountry = inputField();
        fCountry.setText("India");
        addrGrid.add(labeled("City", fCity));
        addrGrid.add(labeled("State", fState));
        addrGrid.add(labeled("Pincode", fPincode));
        addrGrid.add(labeled("Country", fCountry));
        form.add(addrGrid);

        // ── Tax / Compliance ───────────────────────────────────────────
        form.add(sectionHeader("Tax & Compliance"));
        JPanel taxGrid = gridPanel(2);
        fGst = inputField();
        fPan = inputField();
        taxGrid.add(labeled("GST Number", fGst));
        taxGrid.add(labeled("PAN Number", fPan));
        form.add(taxGrid);

        // ── Status ─────────────────────────────────────────────────────
        form.add(sectionHeader("Status"));
        JPanel flagPnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 6));
        flagPnl.setBackground(BG_CARD);
        flagPnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        fIsActive = styledCheck("Active Supplier");
        fIsActive.setSelected(true);
        fIsActive.addActionListener(e -> setDirty(true));
        flagPnl.add(fIsActive);
        form.add(flagPnl);

        // ── Timestamps ────────────────────────────────────────────────
        form.add(sectionHeader("Timestamps"));
        JPanel tsPnl = gridPanel(2);
        fCreatedAt = roField();
        fUpdatedAt = roField();
        tsPnl.add(labeled("Created At", fCreatedAt));
        tsPnl.add(labeled("Updated At", fUpdatedAt));
        form.add(tsPnl);

        // Dirty listeners
        DocumentListener dirtyl = dl(() -> setDirty(true));
        for (JTextField tf : new JTextField[] {
                fName, fContact, fPhone, fEmail,
                fCity, fState, fPincode, fCountry, fGst, fPan })
            tf.getDocument().addDocumentListener(dirtyl);
        fAddress.getDocument().addDocumentListener(dirtyl);

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
        for (String s : new String[] {
                "Ctrl+S  Save", "Ctrl+U  Update", "Ctrl+A  Archive",
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
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> updateRecord(),
                KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> archiveRecord(),
                KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> clearForm(),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> {
            loadMaster();
            setDirty(false);
        },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
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
                    "SELECT id, supplier_name, contact_person, phone, email, " +
                            "city, state, country, gst_number, pan_number, is_active " +
                            "FROM suppliers ORDER BY id");
            while (rs.next()) {
                int active = rs.getInt("is_active"); // NUMBER(1), not boolean
                tModel.addRow(new Object[] {
                        rs.getLong("id"),
                        rs.getString("supplier_name"),
                        rs.getString("contact_person"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("city"),
                        rs.getString("state"),
                        rs.getString("country"),
                        rs.getString("gst_number"),
                        rs.getString("pan_number"),
                        active == 1 ? "Active" : "Archived"
                });
                tot++;
            }
            lblCount.setText(tot + " supplier" + (tot == 1 ? "" : "s"));
        } catch (SQLException ex) {
            showToast("Load error: " + ex.getMessage(), DANGER);
        }
    }

    private void onRowSelected() {
        int vr = masterTable.getSelectedRow();
        if (vr < 0)
            return;
        long sid = (Long) tModel.getValueAt(
                masterTable.convertRowIndexToModel(vr), 0);
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                    "SELECT * FROM suppliers WHERE id = ?");
            pstmt.setLong(1, sid);
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
        currentId = rs.getLong("id"); // was "supplier_id"
        fId.setText(String.valueOf(currentId));
        fName.setText(nvl(rs.getString("supplier_name")));
        fContact.setText(nvl(rs.getString("contact_person")));
        fPhone.setText(nvl(rs.getString("phone")));
        fEmail.setText(nvl(rs.getString("email")));
        fAddress.setText(nvl(rs.getString("address")));
        fCity.setText(nvl(rs.getString("city")));
        fState.setText(nvl(rs.getString("state")));
        fPincode.setText(nvl(rs.getString("pincode")));
        fCountry.setText(nvl(rs.getString("country")));
        fGst.setText(nvl(rs.getString("gst_number")));
        fPan.setText(nvl(rs.getString("pan_number")));

        boolean active = rs.getInt("is_active") == 1; // NUMBER(1), not getBoolean
        fIsActive.setSelected(active);

        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        fCreatedAt.setText(ca != null ? ca.toLocalDateTime().format(fmt) : "");
        fUpdatedAt.setText(ua != null ? ua.toLocalDateTime().format(fmt) : "");

        lblArchiveBadge.setVisible(!active);
    }

    private void saveRecord() {
        if (fName.getText().trim().isEmpty()) {
            showToast("Supplier Name is required!", WARNING);
            return;
        }
        if (fPhone.getText().trim().isEmpty()) {
            showToast("Phone is required!", WARNING);
            return;
        }
        try {
            // Oracle: use RETURNING INTO to get generated ID
            pstmt = MdiForm.sconnect.prepareStatement(
                    "INSERT INTO suppliers " +
                            "(supplier_name, contact_person, phone, email, address, " +
                            " city, state, pincode, country, gst_number, pan_number, is_active) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?) " +
                            "RETURNING id INTO ?"); // <-- Oracle way
            bindAll(pstmt);
            // register OUT parameter for the returned ID
            ((oracle.jdbc.OraclePreparedStatement) pstmt)
                    .registerReturnParameter(13, java.sql.Types.NUMERIC);
            pstmt.executeUpdate();
            ResultSet keys = ((oracle.jdbc.OraclePreparedStatement) pstmt)
                    .getReturnResultSet();
            if (keys != null && keys.next()) {
                currentId = keys.getLong(1);
                fId.setText(String.valueOf(currentId));
            }
            showToast("✔ Supplier saved — ID: " + currentId, ACCENT2);
            loadMaster();
            setDirty(false);
        } catch (SQLException ex) {
            showToast("Save error: " + ex.getMessage(), DANGER);
        }
    }

    private void updateRecord() {
        if (currentId < 0) {
            showToast("Select a supplier first!", WARNING);
            return;
        }
        if (fName.getText().trim().isEmpty()) {
            showToast("Supplier Name is required!", WARNING);
            return;
        }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                    "UPDATE suppliers SET " +
                            "supplier_name=?, contact_person=?, phone=?, email=?, address=?, " +
                            "city=?, state=?, pincode=?, country=?, gst_number=?, pan_number=?, is_active=? " +
                            "WHERE id=?"); // was supplier_id
            bindAll(pstmt);
            pstmt.setLong(13, currentId);
            int r = pstmt.executeUpdate();
            if (r > 0) {
                loadMaster();
                setDirty(false);
                showToast("✔ Supplier updated.", ACCENT2);
            } else {
                showToast("No supplier matched ID " + currentId, WARNING);
            }
        } catch (SQLException ex) {
            showToast("Update error: " + ex.getMessage(), DANGER);
        }
    }

    private void archiveRecord() {
        if (currentId < 0) {
            showToast("Select a supplier to archive!", WARNING);
            return;
        }
        boolean alreadyArchived = !fIsActive.isSelected();
        String action = alreadyArchived ? "Restore" : "Archive";
        String msg = alreadyArchived
                ? "Restore supplier ID " + currentId + "?"
                : "Archive supplier ID " + currentId + "? (not deleted)";
        int ans = JOptionPane.showConfirmDialog(this, msg, action + " Supplier",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION)
            return;
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                    "UPDATE suppliers SET is_active=? WHERE id=?"); // was supplier_id
            pstmt.setInt(1, alreadyArchived ? 1 : 0); // NUMBER(1), not boolean
            pstmt.setLong(2, currentId);
            pstmt.executeUpdate();
            fIsActive.setSelected(alreadyArchived);
            lblArchiveBadge.setVisible(!alreadyArchived);
            loadMaster();
            showToast(alreadyArchived ? "✔ Supplier restored." : "🗄 Supplier archived.", WARNING);
        } catch (SQLException ex) {
            showToast("Archive error: " + ex.getMessage(), DANGER);
        }
    }

    /** Binds indices 1–12 (supplier_name … is_active) */
    private void bindAll(PreparedStatement ps) throws SQLException {
        ps.setString(1, fName.getText().trim());
        setStrOrNull(ps, 2, fContact.getText().trim());
        ps.setString(3, fPhone.getText().trim());
        setStrOrNull(ps, 4, fEmail.getText().trim());
        setStrOrNull(ps, 5, fAddress.getText().trim());
        setStrOrNull(ps, 6, fCity.getText().trim());
        setStrOrNull(ps, 7, fState.getText().trim());
        setStrOrNull(ps, 8, fPincode.getText().trim());
        ps.setString(9, fCountry.getText().trim().isEmpty()
                ? "India"
                : fCountry.getText().trim());
        setStrOrNull(ps, 10, fGst.getText().trim());
        setStrOrNull(ps, 11, fPan.getText().trim());
        ps.setInt(12, fIsActive.isSelected() ? 1 : 0); // NUMBER(1), not boolean
    }

    private void clearForm() {
        currentId = -1;
        for (JTextField tf : new JTextField[] {
                fId, fName, fContact, fPhone, fEmail,
                fCity, fState, fPincode, fGst, fPan, fCreatedAt, fUpdatedAt })
            tf.setText("");
        fId.setText("(auto)");
        fCountry.setText("India");
        fAddress.setText("");
        fIsActive.setSelected(true);
        lblArchiveBadge.setVisible(false);
        masterTable.clearSelection();
        setDirty(false);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Filter (all 11 visible columns searchable)
    // ═════════════════════════════════════════════════════════════════════
    private void applyFilter() {
        List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();

        // Global: checks name(1), contact(2), phone(3), email(4),
        // city(5), state(6), country(7), gst(8), pan(9)
        String gs = txtSearch.getText().trim();
        if (!gs.isEmpty()) {
            try {
                List<RowFilter<DefaultTableModel, Object>> orList = new ArrayList<>();
                for (int col : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 })
                    orList.add(RowFilter.regexFilter("(?i)" + gs, col));
                filters.add(RowFilter.orFilter(orList));
            } catch (Exception ignored) {
            }
        }

        // Per-column filters
        addColFilter(filters, fltName.getText(), 1);
        addColFilter(filters, fltContact.getText(), 2);
        addColFilter(filters, fltPhone.getText(), 3);
        addColFilter(filters, fltEmail.getText(), 4);
        addColFilter(filters, fltCity.getText(), 5);
        addColFilter(filters, fltState.getText(), 6);
        addColFilter(filters, fltPincode.getText(), -1); // pincode not in master cols → skip
        addColFilter(filters, fltCountry.getText(), 7);
        addColFilter(filters, fltGst.getText(), 8);
        addColFilter(filters, fltPan.getText(), 9);

        String ac = (String) cmbFltActive.getSelectedItem();
        if ("Active".equals(ac))
            addColFilter(filters, "Active", 10);
        if ("Archived".equals(ac))
            addColFilter(filters, "Archived", 10);

        try {
            sorter.setRowFilter(filters.isEmpty()
                    ? null
                    : RowFilter.andFilter(filters));
        } catch (Exception ignored) {
        }

        lblCount.setText(masterTable.getRowCount() + " / " + tot + " suppliers");
    }

    private void addColFilter(
            List<RowFilter<DefaultTableModel, Object>> list, String val, int col) {
        if (col < 0 || val == null || val.trim().isEmpty())
            return;
        try {
            list.add(RowFilter.regexFilter("(?i)" + val.trim(), col));
        } catch (Exception ignored) {
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI helpers
    // ═════════════════════════════════════════════════════════════════════
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
        l.setForeground(PURPLE);
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
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                field.getPreferredSize().height + 32));
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
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    private JTextField roField() {
        JTextField tf = inputField();
        tf.setEditable(false);
        tf.setBackground(new Color(14, 20, 38));
        tf.setForeground(PURPLE);
        return tf;
    }

    private void styleTextArea(JTextArea ta) {
        ta.setFont(F_INPUT);
        ta.setBackground(BG_INPUT);
        ta.setForeground(TEXT_PRI);
        ta.setCaretColor(ACCENT);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(new EmptyBorder(6, 8, 6, 8));
    }

    private JCheckBox styledCheck(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setFont(F_INPUT);
        cb.setBackground(BG_CARD);
        cb.setForeground(TEXT_PRI);
        cb.setFocusPainted(false);
        return cb;
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
                BorderFactory.createLineBorder(BORDER_COL),
                new EmptyBorder(4, 8, 4, 8)));
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(fg.brighter());
                g2.setFont(F_BTN);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
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
        for (JScrollBar sb : new JScrollBar[] {
                sp.getVerticalScrollBar(), sp.getHorizontalScrollBar() }) {
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

    // ── Misc helpers ────────────────────────────────────────────────────
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

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private void setStrOrNull(PreparedStatement ps, int idx, String val)
            throws SQLException {
        if (val == null || val.isEmpty())
            ps.setNull(idx, Types.VARCHAR);
        else
            ps.setString(idx, val);
    }

    // ─────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Test SupplierDetails");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JDesktopPane dp = new JDesktopPane();
            dp.setBackground(new Color(8, 12, 20));
            f.setContentPane(dp);
            f.setSize(1340, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            dp.add(new SupplierDetails());
        });
    }
}