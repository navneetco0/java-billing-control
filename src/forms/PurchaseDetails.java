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

public class PurchaseDetails extends JInternalFrame {

    // ── DB ────────────────────────────────────────────────────────────────
    private PreparedStatement pstmt;
    private Statement stmt;
    private ResultSet rs;

    // ── Data ──────────────────────────────────────────────────────────────
    private final List<String[]> allRows = new ArrayList<>();
    private final List<String[]> filtered = new ArrayList<>();
    private int selectedFilteredIdx = -1;
    private int tot = 0;

    // Column indices
    private static final int C_PID = 0, C_DATE = 1, C_SUPID = 2, C_SUPNM = 3,
            C_PROID = 4, C_PRONM = 5, C_SKU = 6, C_QTY = 7,
            C_RATE = 8, C_TOT = 9, C_VAT = 10, C_CON = 11,
            C_NET = 12, C_NARR = 13;

    // ── Master table ──────────────────────────────────────────────────────
    private DefaultTableModel tModel;
    private JTable masterTable;
    private int hoveredRow = -1;
    private JLabel lblCount;

    // ── Filters ───────────────────────────────────────────────────────────
    private JTextField txtSearch;
    private JComboBox<String> cmbFilterSupplier;

    // ── Stat badges ───────────────────────────────────────────────────────
    private JLabel lblTotalRec, lblTotalAmt, lblTotalVat, lblAvgNet;

    // ── Detail panel ──────────────────────────────────────────────────────
    private JPanel detailPanel;
    private JLabel lblDetailId, lblDetailSub, lblDirty;

    // Read-only value labels
    private JLabel valDate, valSupId, valSupName, valProId, valProName,
            valSku, valQty, valRate, valTot, valVat, valCon, valNet, valNarr;

    // Edit fields
    private JTextField edDate, edQty, edVatPct, edConPct, edNarr;
    private JComboBox<String> edSupplier, edProduct;
    private JLabel edSku, edRate, edTot, edNet;

    private JPanel editPanel;
    private JButton btnEdit, btnSave, btnCancel, btnDelete, btnNew, btnRefresh;

    // ── Toast ─────────────────────────────────────────────────────────────
    private JLabel toastLbl;
    private javax.swing.Timer toastTimer;

    // ─────────────────────────────────────────────────────────────────────
    public PurchaseDetails() {
        super("Purchase Details", true, true, true, true);
        setSize(1340, 780);
        setLocation(20, 20);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ensureTableExists();
        buildUI();
        registerShortcuts();
        loadFromDB();
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // DDL
    // ═════════════════════════════════════════════════════════════════════
    private void ensureTableExists() {
        try {
            String sql = "CREATE TABLE purchase_details (" +
                    "  purchase_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "  purchase_date VARCHAR2(20), supplier_id NUMBER, product_id NUMBER, " +
                    "  qty NUMBER(10) DEFAULT 0, rate NUMBER(10,2) DEFAULT 0, " +
                    "  total_amt NUMBER(12,2) DEFAULT 0, vat_pct NUMBER(5,2) DEFAULT 0, " +
                    "  vat_amt NUMBER(10,2) DEFAULT 0, con_pct NUMBER(5,2) DEFAULT 0, " +
                    "  con_amt NUMBER(10,2) DEFAULT 0, net_amt NUMBER(12,2) DEFAULT 0, " +
                    "  narration VARCHAR2(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "  CONSTRAINT fk_pd_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL, "
                    +
                    "  CONSTRAINT fk_pd_product  FOREIGN KEY (product_id)  REFERENCES products(id)  ON DELETE SET NULL)";
            try (Statement st = MdiForm.sconnect.createStatement()) {
                st.executeUpdate(sql);
            }
        } catch (SQLException ignored) {
        }
    }

    private boolean tableExists(String name) throws SQLException {
        try (Statement st = MdiForm.sconnect.createStatement();
                ResultSet r = st.executeQuery(
                        "SELECT COUNT(*) FROM user_tables WHERE table_name='" + name.toUpperCase() + "'")) {
            return r.next() && r.getInt(1) > 0;
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

    // ── Header ────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout()) {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(18, 38, 78), getWidth(), 0, new Color(10, 14, 26)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        hdr.setPreferredSize(new Dimension(0, 54));
        hdr.setBorder(new EmptyBorder(0, 22, 0, 22));

        JLabel title = new JLabel("🛒  Purchase Details");
        title.setFont(F_HEAD);
        title.setForeground(TEXT_PRI);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        lblCount = styledBadge("0 records", TEXT_MUT, BG_INPUT);
        lblTotalRec = styledBadge("0 POs", ACCENT, new Color(14, 38, 72));
        lblTotalAmt = styledBadge("₹0 Total", GOLD, new Color(40, 30, 8));
        lblTotalVat = styledBadge("₹0 VAT", PURPLE, new Color(30, 18, 56));
        lblAvgNet = styledBadge("₹0 Avg", ACCENT2, new Color(10, 40, 28));
        right.add(lblTotalRec);
        right.add(lblTotalAmt);
        right.add(lblTotalVat);
        right.add(lblAvgNet);
        right.add(Box.createHorizontalStrut(12));
        right.add(lblCount);

        hdr.add(title, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    // ── Split pane ────────────────────────────────────────────────────────
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
    // LEFT — Master table + search (no pagination)
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

        txtSearch = styledField("🔍  Search by ID, supplier or product…");
        txtSearch.setPreferredSize(new Dimension(0, 34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        row.setBackground(BG_PANEL);
        cmbFilterSupplier = miniComboRaw("All suppliers");
        loadSupplierFilter();
        cmbFilterSupplier.addActionListener(e -> applyFilter());

        btnNew = makeBtn("＋ New", ACCENT, new Color(16, 62, 130));
        btnRefresh = makeBtn("↺ Refresh", TEXT_MUT, BG_INPUT);
        btnNew.addActionListener(e -> openNewDialog());
        btnRefresh.addActionListener(e -> loadFromDB());

        row.add(new JLabel("Supplier:") {
            {
                setForeground(TEXT_MUT);
                setFont(F_SMALL);
            }
        });
        row.add(cmbFilterSupplier);
        row.add(Box.createHorizontalStrut(8));
        row.add(btnNew);
        row.add(btnRefresh);

        outer.add(txtSearch, BorderLayout.NORTH);
        outer.add(row, BorderLayout.CENTER);
        return outer;
    }

    private JScrollPane buildMasterTable() {
        String[] cols = { "ID", "Date", "Supplier", "Product", "SKU", "Qty", "Net Amt (₹)" };
        tModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        masterTable = new JTable(tModel) {
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component comp = super.prepareRenderer(tcr, row, col);
                boolean sel = isRowSelected(row);
                if (sel) {
                    comp.setBackground(BG_ROW_SEL);
                    comp.setForeground(Color.WHITE);
                } else if (row == hoveredRow) {
                    comp.setBackground(BG_ROW_HOVER);
                    comp.setForeground(TEXT_PRI);
                } else {
                    comp.setBackground(row % 2 == 0 ? BG_ROW_EVEN : BG_ROW_ODD);
                    comp.setForeground(TEXT_PRI);
                }
                if (!sel) {
                    int mc = masterTable.convertColumnIndexToModel(col);
                    if (mc == 0) comp.setForeground(ACCENT);
                    if (mc == 6) comp.setForeground(GOLD);
                    if (mc == 2) comp.setForeground(PURPLE);
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

        int[] ws = { 60, 95, 160, 165, 90, 50, 110 };
        for (int i = 0; i < ws.length; i++)
            masterTable.getColumnModel().getColumn(i).setPreferredWidth(ws[i]);

        JTableHeader th = masterTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBackground(BG_CARD);
        th.setForeground(TEXT_MUT);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        th.setReorderingAllowed(false);

        masterTable.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int r = masterTable.rowAtPoint(e.getPoint());
                if (r != hoveredRow) {
                    hoveredRow = r;
                    masterTable.repaint();
                }
            }
        });
        masterTable.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                masterTable.repaint();
            }
        });
        masterTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
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
    // RIGHT — Detail / Edit form
    // ═════════════════════════════════════════════════════════════════════
    private JPanel buildRightPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_CARD);
        outer.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COL));

        JPanel dhdr = new JPanel(new BorderLayout());
        dhdr.setBackground(BG_CARD);
        dhdr.setBorder(new EmptyBorder(12, 20, 8, 20));

        JLabel title = new JLabel("Purchase Record");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT_PRI);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        lblDirty = new JLabel("● Unsaved");
        lblDirty.setFont(F_SMALL);
        lblDirty.setForeground(WARNING);
        lblDirty.setVisible(false);
        lblDetailId = new JLabel("—");
        lblDetailId.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDetailId.setForeground(ACCENT);
        lblDetailSub = new JLabel("");
        lblDetailSub.setFont(F_SMALL);
        lblDetailSub.setForeground(TEXT_MUT);
        badges.add(lblDirty);
        badges.add(lblDetailId);

        dhdr.add(title, BorderLayout.WEST);
        dhdr.add(badges, BorderLayout.EAST);

        JLabel hint = new JLabel("← Select a purchase order from the list");
        hint.setFont(F_INPUT);
        hint.setForeground(TEXT_MUT);
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        detailPanel = buildDetailPanel();

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_CARD);
        content.add(hint, BorderLayout.NORTH);
        content.add(detailPanel, BorderLayout.CENTER);
        detailPanel.setVisible(false);

        outer.add(dhdr, BorderLayout.NORTH);
        outer.add(new JScrollPane(content) {
            {
                setBorder(null);
                getViewport().setBackground(BG_CARD);
                getVerticalScrollBar().setUnitIncrement(16);
                styleScrollBar(this);
            }
        }, BorderLayout.CENTER);
        outer.add(buildButtonBar(), BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(new EmptyBorder(6, 18, 18, 18));

        panel.add(sectionHeader("Purchase Identity"));
        JPanel id1 = gridPanel(3);
        valDate = roValLabel();
        valSupId = roValLabel();
        valSupName = roValLabel();
        id1.add(labeled("Purchase Date", valDate));
        id1.add(labeled("Supplier ID", valSupId));
        id1.add(labeled("Supplier Name", valSupName));
        panel.add(id1);

        JPanel id2 = gridPanel(3);
        valProId = roValLabel();
        valProName = roValLabel();
        valSku = roValLabel();
        id2.add(labeled("Product ID", valProId));
        id2.add(labeled("Product Name", valProName));
        id2.add(labeled("SKU", valSku));
        panel.add(id2);

        panel.add(sectionHeader("Amounts  (₹ INR)"));
        JPanel amt1 = gridPanel(3);
        valQty = roValLabel();
        valRate = roValLabel();
        valTot = roValLabel();
        amt1.add(labeled("Quantity", valQty));
        amt1.add(labeled("Rate (₹)", valRate));
        amt1.add(labeled("Total Amount", valTot));
        panel.add(amt1);

        JPanel amt2 = gridPanel(3);
        valVat = roValLabel();
        valCon = roValLabel();
        valNet = roValLabel();
        amt2.add(labeled("VAT %", valVat));
        amt2.add(labeled("Concession %", valCon));
        amt2.add(labeled("Net Amount", valNet));
        panel.add(amt2);

        panel.add(sectionHeader("Narration"));
        JPanel nPanel = gridPanel(1);
        valNarr = roValLabel();
        nPanel.add(labeled("Narration", valNarr));
        panel.add(nPanel);

        panel.add(sectionHeader("Edit Fields"));
        editPanel = new JPanel(new GridLayout(0, 2, 10, 8));
        editPanel.setBackground(BG_CARD);
        editPanel.setBorder(new EmptyBorder(6, 0, 6, 0));
        editPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999));
        editPanel.setAlignmentX(0f);

        edDate = inputField();
        edSupplier = new JComboBox<>();
        styleComboBox(edSupplier);
        edProduct = new JComboBox<>();
        styleComboBox(edProduct);
        edSku = roValLabel();
        edRate = roValLabel();
        edQty = inputField();
        edTot = roValLabel();
        edVatPct = inputField();
        edConPct = inputField();
        edNet = roValLabel();
        edNarr = inputField();

        populateSupplierCombo(edSupplier);
        populateProductCombo(edProduct);

        KeyAdapter calc = new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                recalcAmounts();
            }
        };
        edQty.addKeyListener(calc);
        edVatPct.addKeyListener(calc);
        edConPct.addKeyListener(calc);
        edProduct.addActionListener(e -> fillProductRate(edSku, edRate));

        DocumentListener dirtyl = dl(() -> lblDirty.setVisible(true));
        for (JTextField tf : new JTextField[] { edDate, edQty, edVatPct, edConPct, edNarr })
            tf.getDocument().addDocumentListener(dirtyl);

        editPanel.add(labeled("Purchase Date", edDate));
        editPanel.add(labeled("Supplier", edSupplier));
        editPanel.add(labeled("Product", edProduct));
        editPanel.add(labeled("SKU", edSku));
        editPanel.add(labeled("Rate (₹)", edRate));
        editPanel.add(labeled("Quantity", edQty));
        editPanel.add(labeled("VAT %", edVatPct));
        editPanel.add(labeled("Concession %", edConPct));
        editPanel.add(labeled("Total Amount", edTot));
        editPanel.add(labeled("Net Amount", edNet));
        editPanel.add(labeled("Narration", edNarr));
        editPanel.setVisible(false);
        panel.add(editPanel);

        return panel;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        bar.setBackground(new Color(12, 18, 34));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));

        btnEdit = makeBtn("✏ Edit", GOLD, new Color(140, 96, 10));
        btnSave = makeBtn("💾 Save", ACCENT2, new Color(22, 108, 68));
        btnCancel = makeBtn("✕ Cancel", TEXT_MUT, BG_INPUT);
        btnDelete = makeBtn("🗑 Delete", DANGER, new Color(140, 35, 35));

        btnEdit.addActionListener(e -> enterEditMode());
        btnSave.addActionListener(e -> saveEdit());
        btnCancel.addActionListener(e -> cancelEdit());
        btnDelete.addActionListener(e -> deleteRecord());

        btnSave.setVisible(false);
        btnCancel.setVisible(false);

        bar.add(btnEdit);
        bar.add(btnDelete);
        bar.add(btnSave);
        bar.add(btnCancel);
        JLabel note = new JLabel("  Ctrl+S Save  |  Ctrl+N New  |  Esc Refresh");
        note.setFont(F_SMALL);
        note.setForeground(new Color(55, 78, 115));
        bar.add(note);
        return bar;
    }

    private JPanel buildShortcutBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        p.setBackground(new Color(7, 10, 20));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(24, 34, 56)));
        for (String s : new String[] { "Ctrl+S  Save Edit", "Ctrl+N  New Purchase", "Esc  Refresh" }) {
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
    // Keyboard shortcuts
    // ═════════════════════════════════════════════════════════════════════
    private void registerShortcuts() {
        JRootPane rp = getRootPane();
        rp.registerKeyboardAction(e -> saveEdit(),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> openNewDialog(),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        rp.registerKeyboardAction(e -> loadFromDB(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data Operations
    // ═════════════════════════════════════════════════════════════════════
    private void loadFromDB() {
        allRows.clear();
        tot = 0;
        try {
            stmt = MdiForm.sconnect.createStatement();
            rs = stmt.executeQuery(
                    "SELECT pd.purchase_id, pd.purchase_date, " +
                            "  pd.supplier_id, COALESCE(s.supplier_name,'—') AS supplier_name, " +
                            "  pd.product_id,  COALESCE(p.name,'—') AS product_name, " +
                            "  COALESCE(p.sku,'—') AS sku, " +
                            "  pd.qty, pd.rate, pd.total_amt, pd.vat_pct, pd.con_pct, " +
                            "  pd.net_amt, pd.narration " +
                            "FROM purchase_details pd " +
                            "LEFT JOIN suppliers s ON pd.supplier_id = s.id " +
                            "LEFT JOIN products  p ON pd.product_id  = p.id " +
                            "ORDER BY pd.purchase_id DESC");
            while (rs.next()) {
                allRows.add(new String[] {
                        ns(rs.getString("purchase_id")), ns(rs.getString("purchase_date")),
                        ns(rs.getString("supplier_id")), ns(rs.getString("supplier_name")),
                        ns(rs.getString("product_id")), ns(rs.getString("product_name")),
                        ns(rs.getString("sku")), ns(rs.getString("qty")),
                        ns(rs.getString("rate")), ns(rs.getString("total_amt")),
                        ns(rs.getString("vat_pct")), ns(rs.getString("con_pct")),
                        ns(rs.getString("net_amt")), ns(rs.getString("narration"))
                });
                tot++;
            }
        } catch (Exception ex) {
            showToast("DB error: " + ex.getMessage(), DANGER);
        }
        applyFilter();
        refreshStats();
        detailPanel.setVisible(false);
        lblDetailId.setText("—");
        lblDetailSub.setText("");
        selectedFilteredIdx = -1;
    }

    private void onRowSelected() {
        int vr = masterTable.getSelectedRow();
        if (vr < 0) return;
        if (vr >= filtered.size()) return;
        selectedFilteredIdx = vr;
        showDetail(false);
    }

    private void showDetail(boolean editMode) {
        if (selectedFilteredIdx < 0 || selectedFilteredIdx >= filtered.size()) return;
        String[] r = filtered.get(selectedFilteredIdx);

        lblDetailId.setText("PO #" + r[C_PID]);
        lblDetailSub.setText(r[C_SUPNM] + " · " + r[C_DATE]);

        valDate.setText(r[C_DATE]);
        valSupId.setText(r[C_SUPID]);
        valSupName.setText(r[C_SUPNM]);
        valProId.setText(r[C_PROID]);
        valProName.setText(r[C_PRONM]);
        valSku.setText(r[C_SKU]);
        valQty.setText(r[C_QTY]);
        valRate.setText("₹" + fmt(r[C_RATE]));
        valTot.setText("₹" + fmt(r[C_TOT]));
        valVat.setText(r[C_VAT] + " %");
        valCon.setText(r[C_CON] + " %");
        valNet.setText("₹" + fmt(r[C_NET]));
        valNarr.setText(r[C_NARR]);

        if (editMode) {
            edDate.setText(r[C_DATE]);
            setComboById(edSupplier, r[C_SUPID]);
            setComboById(edProduct, r[C_PROID]);
            edSku.setText(r[C_SKU]);
            edRate.setText("₹" + fmt(r[C_RATE]));
            edQty.setText(r[C_QTY]);
            edVatPct.setText(r[C_VAT]);
            edConPct.setText(r[C_CON]);
            edTot.setText("₹" + fmt(r[C_TOT]));
            edNet.setText("₹" + fmt(r[C_NET]));
            edNarr.setText(r[C_NARR]);
        }

        editPanel.setVisible(editMode);
        btnEdit.setVisible(!editMode);
        btnDelete.setVisible(!editMode);
        btnSave.setVisible(editMode);
        btnCancel.setVisible(editMode);
        lblDirty.setVisible(false);
        detailPanel.setVisible(true);
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private void enterEditMode() {
        showDetail(true);
    }

    private void cancelEdit() {
        showDetail(false);
    }

    private void fillProductRate(JLabel skuLbl, JLabel rateLbl) {
        if (edProduct.getSelectedItem() == null) return;
        String item = edProduct.getSelectedItem().toString();
        if (item.isEmpty()) return;
        try {
            PreparedStatement ps = MdiForm.sconnect.prepareStatement(
                    "SELECT sku, cost_price FROM products WHERE id=?");
            ps.setLong(1, Long.parseLong(item.split(" — ")[0].trim()));
            ResultSet r = ps.executeQuery();
            if (r.next()) {
                skuLbl.setText(ns(r.getString("sku")));
                rateLbl.setText("₹" + fmt(r.getDouble("cost_price")));
                recalcAmounts();
            }
        } catch (Exception ignored) {
        }
    }

    private void recalcAmounts() {
        try {
            double rate = parseAmt(edRate.getText()), qty = parseDbl(edQty.getText()),
                    vp = parseDbl(edVatPct.getText()), cp = parseDbl(edConPct.getText()),
                    tot = qty * rate, vat = tot * vp / 100, con = tot * cp / 100;
            edTot.setText("₹" + fmt(tot));
            edNet.setText("₹" + fmt(tot + vat - con));
        } catch (Exception ignored) {
        }
    }

    private void saveEdit() {
        if (selectedFilteredIdx < 0) {
            showToast("Select a record first!", WARNING);
            return;
        }
        String[] r = filtered.get(selectedFilteredIdx);
        double rate, qty, vp, cp;
        try {
            rate = parseAmt(edRate.getText());
            qty = parseDbl(edQty.getText());
            vp = parseDbl(edVatPct.getText());
            cp = parseDbl(edConPct.getText());
        } catch (NumberFormatException ex) {
            showToast("Enter valid numbers!", WARNING);
            return;
        }
        double tot = qty * rate, vat = tot * vp / 100, con = tot * cp / 100, net = tot + vat - con;
        long supId = selectedId(edSupplier), proId = selectedId(edProduct);
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                    "UPDATE purchase_details SET purchase_date=?,supplier_id=?,product_id=?,qty=?,rate=?," +
                            "total_amt=?,vat_pct=?,vat_amt=?,con_pct=?,con_amt=?,net_amt=?,narration=? WHERE purchase_id=?");
            pstmt.setString(1, edDate.getText().trim());
            pstmt.setLong(2, supId);
            pstmt.setLong(3, proId);
            pstmt.setDouble(4, qty);
            pstmt.setDouble(5, rate);
            pstmt.setDouble(6, tot);
            pstmt.setDouble(7, vp);
            pstmt.setDouble(8, vat);
            pstmt.setDouble(9, cp);
            pstmt.setDouble(10, con);
            pstmt.setDouble(11, net);
            pstmt.setString(12, edNarr.getText().trim());
            pstmt.setLong(13, Long.parseLong(r[C_PID]));
            pstmt.executeUpdate();
            showToast("✔ Record updated", ACCENT2);
            loadFromDB();
        } catch (Exception ex) {
            showToast("DB error: " + ex.getMessage(), DANGER);
        }
    }

    private void deleteRecord() {
        if (selectedFilteredIdx < 0) {
            showToast("Select a record first!", WARNING);
            return;
        }
        String[] r = filtered.get(selectedFilteredIdx);
        int ans = JOptionPane.showConfirmDialog(this,
                "Delete Purchase ID " + r[C_PID] + "? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                    "DELETE FROM purchase_details WHERE purchase_id=?");
            pstmt.setLong(1, Long.parseLong(r[C_PID]));
            pstmt.executeUpdate();
            showToast("🗑 Record deleted", WARNING);
            loadFromDB();
        } catch (Exception ex) {
            showToast("DB error: " + ex.getMessage(), DANGER);
        }
    }

    // ── New Purchase Dialog ───────────────────────────────────────────────
    private void openNewDialog() {
        JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "New Purchase", true);
        dlg.setSize(560, 560);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_MAIN);
        dlg.setLayout(new BorderLayout());

        JPanel dHdr = new JPanel(new BorderLayout()) {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(18, 38, 78), getWidth(), 0, BG_MAIN));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        dHdr.setPreferredSize(new Dimension(0, 46));
        dHdr.setBorder(new EmptyBorder(0, 18, 0, 18));
        JLabel dTitle = new JLabel("🛒  New Purchase Order");
        dTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        dTitle.setForeground(TEXT_PRI);
        dHdr.add(dTitle, BorderLayout.WEST);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBackground(BG_CARD);
        form.setBorder(new EmptyBorder(16, 18, 12, 18));

        JTextField nDate = inputField(), nQty = inputField(), nVat = inputField(),
                nCon = inputField(), nNarr = inputField();
        JLabel nSku = roValLabel(), nRate = roValLabel(), nTot = roValLabel(), nNet = roValLabel();
        JComboBox<String> nSup = new JComboBox<>(), nPro = new JComboBox<>();
        styleComboBox(nSup);
        styleComboBox(nPro);
        populateSupplierCombo(nSup);
        populateProductCombo(nPro);
        nDate.setText(new SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date()));

        Runnable calc = () -> {
            try {
                double r2 = parseAmt(nRate.getText()), q = parseDbl(nQty.getText()),
                        vp = parseDbl(nVat.getText()), cp = parseDbl(nCon.getText()), t = q * r2;
                nTot.setText("₹" + fmt(t));
                nNet.setText("₹" + fmt(t + t * vp / 100 - t * cp / 100));
            } catch (Exception ignored) {
            }
        };
        nPro.addActionListener(e -> {
            if (nPro.getSelectedItem() == null) return;
            String item = nPro.getSelectedItem().toString();
            if (item.isEmpty()) return;
            try {
                PreparedStatement ps = MdiForm.sconnect
                        .prepareStatement("SELECT sku,cost_price FROM products WHERE id=?");
                ps.setLong(1, Long.parseLong(item.split(" — ")[0].trim()));
                ResultSet r2 = ps.executeQuery();
                if (r2.next()) {
                    nSku.setText(ns(r2.getString("sku")));
                    nRate.setText("₹" + fmt(r2.getDouble("cost_price")));
                    calc.run();
                }
            } catch (Exception ignored) {
            }
        });
        KeyAdapter ka = new KeyAdapter() {
            public void keyReleased(KeyEvent ke) {
                calc.run();
            }
        };
        nQty.addKeyListener(ka);
        nVat.addKeyListener(ka);
        nCon.addKeyListener(ka);

        form.add(labeled("Purchase Date", nDate));
        form.add(labeled("Supplier", nSup));
        form.add(labeled("Product", nPro));
        form.add(labeled("SKU", nSku));
        form.add(labeled("Rate (₹)", nRate));
        form.add(labeled("Quantity", nQty));
        form.add(labeled("VAT %", nVat));
        form.add(labeled("Concession %", nCon));
        form.add(labeled("Total Amount", nTot));
        form.add(labeled("Net Amount", nNet));
        form.add(labeled("Narration", nNarr));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        btns.setBackground(new Color(12, 18, 34));
        btns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));
        JButton save = makeBtn("💾 Save", ACCENT2, new Color(22, 108, 68));
        JButton cancel = makeBtn("✕ Cancel", TEXT_MUT, BG_INPUT);
        save.addActionListener(e -> {
            try {
                double rate = parseAmt(nRate.getText()), qty = parseDbl(nQty.getText()),
                        vp = parseDbl(nVat.getText()), cp = parseDbl(nCon.getText()),
                        tot = qty * rate, vat = tot * vp / 100, con = tot * cp / 100, net = tot + vat - con;
                pstmt = MdiForm.sconnect.prepareStatement(
                        "INSERT INTO purchase_details(purchase_date,supplier_id,product_id,qty,rate," +
                                "total_amt,vat_pct,vat_amt,con_pct,con_amt,net_amt,narration) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)");
                pstmt.setString(1, nDate.getText().trim());
                pstmt.setLong(2, selectedId(nSup));
                pstmt.setLong(3, selectedId(nPro));
                pstmt.setDouble(4, qty);
                pstmt.setDouble(5, rate);
                pstmt.setDouble(6, tot);
                pstmt.setDouble(7, vp);
                pstmt.setDouble(8, vat);
                pstmt.setDouble(9, cp);
                pstmt.setDouble(10, con);
                pstmt.setDouble(11, net);
                pstmt.setString(12, nNarr.getText().trim());
                pstmt.executeUpdate();
                dlg.dispose();
                loadFromDB();
                showToast("✔ Purchase added", ACCENT2);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancel.addActionListener(e -> dlg.dispose());
        btns.add(cancel);
        btns.add(save);

        dlg.add(dHdr, BorderLayout.NORTH);
        dlg.add(new JScrollPane(form) {
            {
                setBorder(null);
                getViewport().setBackground(BG_CARD);
            }
        }, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Filter (no pagination)
    // ═════════════════════════════════════════════════════════════════════
    private void applyFilter() {
        String q = txtSearch != null ? txtSearch.getText().toLowerCase().trim() : "";
        String s = cmbFilterSupplier != null ? (String) cmbFilterSupplier.getSelectedItem() : "All suppliers";
        filtered.clear();
        for (String[] r : allRows) {
            boolean mQ = q.isEmpty() || r[C_PID].contains(q) || r[C_SUPNM].toLowerCase().contains(q)
                    || r[C_PRONM].toLowerCase().contains(q) || r[C_SKU].toLowerCase().contains(q);
            boolean mS = "All suppliers".equals(s) || s.startsWith(r[C_SUPID] + " —");
            if (mQ && mS) filtered.add(r);
        }
        selectedFilteredIdx = -1;
        refreshTable();
        lblCount.setText(filtered.size() + " / " + tot + " records");
    }

    private void refreshTable() {
        tModel.setRowCount(0);
        for (String[] r : filtered) {
            tModel.addRow(new Object[] {
                r[C_PID], r[C_DATE], r[C_SUPNM], r[C_PRONM], r[C_SKU], r[C_QTY], "₹" + fmt(r[C_NET])
            });
        }
    }

    private void refreshStats() {
        double totAmt = 0, totVat = 0, netSum = 0;
        for (String[] r : allRows) {
            try { totAmt += Double.parseDouble(r[C_TOT]); } catch (Exception ignored) {}
            try { totVat += Double.parseDouble(r[C_VAT]); } catch (Exception ignored) {}
            try { netSum += Double.parseDouble(r[C_NET]); } catch (Exception ignored) {}
        }
        double avgNet = allRows.isEmpty() ? 0 : netSum / allRows.size();
        lblTotalRec.setText(allRows.size() + " POs");
        lblTotalAmt.setText("₹" + fmt(totAmt) + " Total");
        lblTotalVat.setText("₹" + fmt(totVat) + " VAT");
        lblAvgNet.setText("₹" + fmt(avgNet) + " Avg");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Combo helpers
    // ═════════════════════════════════════════════════════════════════════
    private void loadSupplierFilter() {
        try {
            if (!tableExists("SUPPLIERS")) return;
            Statement st = MdiForm.sconnect.createStatement();
            ResultSet r = st.executeQuery(
                    "SELECT id,supplier_name FROM suppliers WHERE is_active=1 ORDER BY supplier_name");
            while (r.next())
                cmbFilterSupplier.addItem(r.getLong(1) + " — " + r.getString(2));
        } catch (Exception ignored) {}
    }

    private void populateSupplierCombo(JComboBox<String> cmb) {
        cmb.addItem("");
        try {
            if (!tableExists("SUPPLIERS")) return;
            Statement st = MdiForm.sconnect.createStatement();
            ResultSet r = st.executeQuery(
                    "SELECT id,supplier_name FROM suppliers WHERE is_active=1 ORDER BY supplier_name");
            while (r.next())
                cmb.addItem(r.getLong(1) + " — " + r.getString(2));
        } catch (Exception ignored) {}
    }

    private void populateProductCombo(JComboBox<String> cmb) {
        cmb.addItem("");
        try {
            if (!tableExists("PRODUCTS")) return;
            Statement st = MdiForm.sconnect.createStatement();
            ResultSet r = st.executeQuery("SELECT id,name FROM products WHERE is_active=1 ORDER BY name");
            while (r.next())
                cmb.addItem(r.getLong(1) + " — " + r.getString(2));
        } catch (Exception ignored) {}
    }

    private long selectedId(JComboBox<String> cmb) {
        if (cmb.getSelectedItem() == null) return 0;
        String item = cmb.getSelectedItem().toString().trim();
        if (item.isEmpty()) return 0;
        try {
            return Long.parseLong(item.split(" — ")[0].trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void setComboById(JComboBox<String> cmb, String idStr) {
        for (int i = 0; i < cmb.getItemCount(); i++)
            if (cmb.getItemAt(i).startsWith(idStr + " —")) {
                cmb.setSelectedIndex(i);
                return;
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
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(F_SEC);
        l.setForeground(ACCENT);
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

    private JLabel roValLabel() {
        JLabel l = new JLabel("—");
        l.setFont(F_INPUT);
        l.setOpaque(true);
        l.setBackground(new Color(14, 20, 38));
        l.setForeground(ACCENT);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        return l;
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

    private void styleComboBox(JComboBox<?> cb) {
        cb.setFont(F_INPUT);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRI);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COL));
    }

    private JComboBox<String> miniComboRaw(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(F_SMALL);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRI);
        cb.setPreferredSize(new Dimension(200, 28));
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
            if (sb == null) continue;
            sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                protected void configureScrollBarColors() {
                    thumbColor = new Color(46, 66, 108);
                    trackColor = BG_PANEL;
                }
                protected JButton createDecreaseButton(int o) { return zb(); }
                protected JButton createIncreaseButton(int o) { return zb(); }
                private JButton zb() {
                    JButton b = new JButton();
                    b.setPreferredSize(new Dimension(0, 0));
                    return b;
                }
            });
        }
    }

    private void showToast(String msg, Color bg) {
        if (toastLbl == null) return;
        toastLbl.setText(msg);
        toastLbl.setBackground(bg);
        toastLbl.setForeground(Color.WHITE);
        toastLbl.setVisible(true);
        if (toastTimer != null && toastTimer.isRunning()) toastTimer.stop();
        toastTimer = new javax.swing.Timer(3000, e -> toastLbl.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    private DocumentListener dl(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) {}
        };
    }

    private double parseAmt(String s) {
        try {
            return Double.parseDouble(s.replace("₹", "").replace(",", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDbl(String s) {
        try {
            return Double.parseDouble(s.trim().isEmpty() ? "0" : s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String ns(String s) { return s != null ? s : ""; }

    private static String fmt(double v) { return String.format("%,.2f", v); }

    private static String fmt(String s) {
        try {
            return fmt(Double.parseDouble(s));
        } catch (Exception e) {
            return s != null ? s : "0.00";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Test");
            JDesktopPane d = new JDesktopPane();
            d.setBackground(new Color(8, 12, 20));
            f.setContentPane(d);
            f.setSize(1380, 820);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            d.add(new PurchaseDetails());
        });
    }
}