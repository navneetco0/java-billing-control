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

public class PurchaseReturn extends JInternalFrame {

    // ── DB ────────────────────────────────────────────────────────────────
    private PreparedStatement pstmt;
    private Statement stmt;
    private ResultSet rs;

    // ── Data ──────────────────────────────────────────────────────────────
    private final List<String[]> allRows  = new ArrayList<>();
    private final List<String[]> filtered = new ArrayList<>();
    private int selectedFilteredIdx = -1;
    private int tot = 0;

    // Column indices
    private static final int C_RETID=0,C_RETNO=1,C_RETDATE=2,C_PID=3,C_PDATE=4,
                             C_SUPID=5,C_SUPNM=6,C_PROID=7,C_PRONM=8,C_SKU=9,
                             C_QTY=10,C_RATE=11,C_TOT=12,C_VATPCT=13,
                             C_CONPCT=15,C_NET=17,C_NRET=18,C_RETAMT=19,C_NARR=20;

    // ── Master table ──────────────────────────────────────────────────────
    private DefaultTableModel tModel;
    private JTable masterTable;
    private int hoveredRow = -1;
    private JLabel lblCount;

    // ── Filters ───────────────────────────────────────────────────────────
    private JTextField    txtSearch;
    private JComboBox<String> cmbFilterPurchase;

    // ── Stat badges ───────────────────────────────────────────────────────
    private JLabel lblTotalRet, lblTotalRetAmt, lblAvgRetAmt, lblTotalItems;

    // ── Detail panel ──────────────────────────────────────────────────────
    private JPanel  detailPanel;
    private JLabel  lblDetailId, lblDetailSub, lblDirty;

    // Read-only value labels
    private JLabel valRetNo, valRetDate, valPid, valPdate,
                   valSupId, valSupName, valProId, valProName, valSku,
                   valQty, valRate, valTot, valVat, valCon, valNet,
                   valNret, valRetAmt, valNarr;

    // Edit fields
    private JTextField        edRetDate, edNret, edNarr;
    private JComboBox<String> edPurchaseId;
    private JLabel            edPdate, edSupId, edSupName, edProId, edProName,
                              edSku, edQty, edRate, edTot, edVat, edCon, edNet, edRetAmt;

    private JPanel editPanel;
    private JButton btnEdit, btnSave, btnCancel, btnDelete, btnNew, btnRefresh;

    // ── Toast ─────────────────────────────────────────────────────────────
    private JLabel toastLbl;
    private javax.swing.Timer toastTimer;

    // ─────────────────────────────────────────────────────────────────────
    public PurchaseReturn() {
        super("Purchase Return", true, true, true, true);
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
            String sql =
                "CREATE TABLE purchase_return (" +
                "  ret_id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "  purchase_ret_no   VARCHAR2(30) UNIQUE NOT NULL, " +
                "  purchase_ret_date VARCHAR2(20), purchase_id NUMBER, purchase_date VARCHAR2(20), " +
                "  supplier_id NUMBER, product_id NUMBER, " +
                "  qty NUMBER(10) DEFAULT 0, rate NUMBER(10,2) DEFAULT 0, " +
                "  total_amt NUMBER(12,2) DEFAULT 0, vat_pct NUMBER(5,2) DEFAULT 0, " +
                "  vat_amt NUMBER(10,2) DEFAULT 0, con_pct NUMBER(5,2) DEFAULT 0, " +
                "  con_amt NUMBER(10,2) DEFAULT 0, net_amt NUMBER(12,2) DEFAULT 0, " +
                "  no_of_ret_item NUMBER(10) DEFAULT 0, total_ret_amt NUMBER(12,2) DEFAULT 0, " +
                "  narration VARCHAR2(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "  CONSTRAINT fk_pr_purchase FOREIGN KEY (purchase_id) REFERENCES purchase_details(purchase_id) ON DELETE SET NULL, " +
                "  CONSTRAINT fk_pr_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL, " +
                "  CONSTRAINT fk_pr_product  FOREIGN KEY (product_id)  REFERENCES products(id)  ON DELETE SET NULL)";
            try (Statement st=MdiForm.sconnect.createStatement()) { st.executeUpdate(sql); }
        } catch (SQLException ignored) {}
    }

    private boolean tableExists(String name) throws SQLException {
        try (Statement st=MdiForm.sconnect.createStatement();
             ResultSet r=st.executeQuery("SELECT COUNT(*) FROM user_tables WHERE table_name='"+name.toUpperCase()+"'")) {
            return r.next() && r.getInt(1)>0;
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

    // ── Header ────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout()) {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0,0,new Color(50,22,8),getWidth(),0,BG_MAIN));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(ACCENT); g2.fillRect(0,getHeight()-2,getWidth(),2);
            }
        };
        hdr.setPreferredSize(new Dimension(0,54));
        hdr.setBorder(new EmptyBorder(0,22,0,22));

        JLabel title = new JLabel("↩  Purchase Return");
        title.setFont(F_HEAD); title.setForeground(TEXT_PRI);

        // Warning badge
        JLabel warn = new JLabel("⚠ Returns reverse inventory");
        warn.setFont(F_SMALL); warn.setOpaque(true);
        warn.setBackground(new Color(80,40,5)); warn.setForeground(WARNING);
        warn.setBorder(new EmptyBorder(3,10,3,10));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        right.setOpaque(false);
        lblTotalRet    = styledBadge("0 Returns",  ACCENT,  new Color(60,25,5));
        lblTotalRetAmt = styledBadge("₹0 Total",   GOLD,    new Color(40,30,8));
        lblAvgRetAmt   = styledBadge("₹0 Avg",     PURPLE,  new Color(30,18,56));
        lblTotalItems  = styledBadge("0 Items",     ACCENT2, new Color(10,40,28));
        lblCount       = styledBadge("0 records",  TEXT_MUT, BG_INPUT);
        right.add(warn);
        right.add(lblTotalRet); right.add(lblTotalRetAmt);
        right.add(lblAvgRetAmt); right.add(lblTotalItems);
        right.add(Box.createHorizontalStrut(8)); right.add(lblCount);

        hdr.add(title, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    // ── Split pane ────────────────────────────────────────────────────────
    private JSplitPane buildSplit() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        sp.setDividerLocation(620); sp.setDividerSize(4);
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
        outer.setBorder(new EmptyBorder(10,12,8,12));

        txtSearch = styledField("🔍  Search by return no, supplier or product…");
        txtSearch.setPreferredSize(new Dimension(0,34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));
        row.setBackground(BG_PANEL);

        cmbFilterPurchase = miniComboRaw("All purchases");
        loadPurchaseFilter();
        cmbFilterPurchase.addActionListener(e -> applyFilter());

        btnNew     = makeBtn("＋ New Return", ACCENT, new Color(80,40,5));
        btnRefresh = makeBtn("↺ Refresh",     TEXT_MUT, BG_INPUT);
        btnNew    .addActionListener(e -> openNewDialog());
        btnRefresh.addActionListener(e -> loadFromDB());

        row.add(new JLabel("Purchase:"){{ setForeground(TEXT_MUT); setFont(F_SMALL); }});
        row.add(cmbFilterPurchase);
        row.add(Box.createHorizontalStrut(8));
        row.add(btnNew); row.add(btnRefresh);

        outer.add(txtSearch, BorderLayout.NORTH);
        outer.add(row,       BorderLayout.CENTER);
        return outer;
    }

    private JScrollPane buildMasterTable() {
        String[] cols = {"Ret ID","Return No","Ret Date","PO ID","Supplier","Items Ret","Ret Amt (₹)"};
        tModel = new DefaultTableModel(cols,0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        masterTable = new JTable(tModel) {
            public Component prepareRenderer(TableCellRenderer tcr, int row, int col) {
                Component comp = super.prepareRenderer(tcr,row,col);
                boolean sel = isRowSelected(row);
                if      (sel)            { comp.setBackground(BG_ROW_SEL);   comp.setForeground(Color.WHITE); }
                else if (row==hoveredRow){ comp.setBackground(BG_ROW_HOVER); comp.setForeground(TEXT_PRI); }
                else                    { comp.setBackground(row%2==0?BG_ROW_EVEN:BG_ROW_ODD); comp.setForeground(TEXT_PRI); }
                if (!sel) {
                    int mc = masterTable.convertColumnIndexToModel(col);
                    if (mc==0) comp.setForeground(ACCENT);
                    if (mc==6) comp.setForeground(GOLD);
                    if (mc==4) comp.setForeground(PURPLE);
                    if (mc==1) comp.setForeground(BLUE);
                }
                ((JComponent)comp).setBorder(new EmptyBorder(4,8,4,8));
                return comp;
            }
        };
        masterTable.setFont(F_TABLE); masterTable.setRowHeight(30);
        masterTable.setShowGrid(false); masterTable.setIntercellSpacing(new Dimension(0,0));
        masterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        masterTable.setBackground(BG_PANEL); masterTable.setForeground(TEXT_PRI);
        masterTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] ws = {60,120,95,65,160,80,110};
        for (int i=0;i<ws.length;i++) masterTable.getColumnModel().getColumn(i).setPreferredWidth(ws[i]);

        JTableHeader th = masterTable.getTableHeader();
        th.setFont(new Font("Segoe UI",Font.BOLD,11));
        th.setBackground(BG_CARD); th.setForeground(TEXT_MUT);
        th.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER_COL));
        th.setReorderingAllowed(false);

        masterTable.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int r=masterTable.rowAtPoint(e.getPoint());
                if (r!=hoveredRow) { hoveredRow=r; masterTable.repaint(); }
            }
        });
        masterTable.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) { hoveredRow=-1; masterTable.repaint(); }
        });
        masterTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        JScrollPane sc = new JScrollPane(masterTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(null); sc.getViewport().setBackground(BG_PANEL);
        styleScrollBar(sc); return sc;
    }

    // ═════════════════════════════════════════════════════════════════════
    // RIGHT — Detail / Edit form
    // ═════════════════════════════════════════════════════════════════════
    private JPanel buildRightPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_CARD);
        outer.setBorder(BorderFactory.createMatteBorder(0,1,0,0,BORDER_COL));

        JPanel dhdr = new JPanel(new BorderLayout());
        dhdr.setBackground(BG_CARD); dhdr.setBorder(new EmptyBorder(12,20,8,20));
        JLabel title = new JLabel("Return Record");
        title.setFont(new Font("Segoe UI",Font.BOLD,14)); title.setForeground(TEXT_PRI);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0)); badges.setOpaque(false);
        lblDirty   = new JLabel("● Unsaved"); lblDirty.setFont(F_SMALL); lblDirty.setForeground(WARNING); lblDirty.setVisible(false);
        lblDetailId= new JLabel("—"); lblDetailId.setFont(new Font("Segoe UI",Font.BOLD,13)); lblDetailId.setForeground(ACCENT);
        lblDetailSub=new JLabel(""); lblDetailSub.setFont(F_SMALL); lblDetailSub.setForeground(TEXT_MUT);
        badges.add(lblDirty); badges.add(lblDetailId);
        dhdr.add(title, BorderLayout.WEST); dhdr.add(badges, BorderLayout.EAST);

        JLabel hint = new JLabel("← Select a return record from the list");
        hint.setFont(F_INPUT); hint.setForeground(TEXT_MUT); hint.setHorizontalAlignment(SwingConstants.CENTER);

        detailPanel = buildDetailPanel();
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_CARD);
        content.add(hint, BorderLayout.NORTH);
        content.add(detailPanel, BorderLayout.CENTER);
        detailPanel.setVisible(false);

        outer.add(dhdr, BorderLayout.NORTH);
        outer.add(new JScrollPane(content){{
            setBorder(null); getViewport().setBackground(BG_CARD);
            getVerticalScrollBar().setUnitIncrement(16); styleScrollBar(this);
        }}, BorderLayout.CENTER);
        outer.add(buildButtonBar(), BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(new EmptyBorder(6,18,18,18));

        // ── Return Identity ──────────────────────────────────────────────
        panel.add(sectionHeader("Return Identity"));
        JPanel id1 = gridPanel(3);
        valRetNo = roValLabel(); valRetDate = roValLabel(); valPid = roValLabel();
        id1.add(labeled("Return No",   valRetNo));
        id1.add(labeled("Return Date", valRetDate));
        id1.add(labeled("Purchase ID", valPid));
        panel.add(id1);

        // ── Source Purchase ──────────────────────────────────────────────
        panel.add(sectionHeader("Source Purchase Details"));
        JPanel src1 = gridPanel(3);
        valPdate = roValLabel(); valSupId = roValLabel(); valSupName = roValLabel();
        src1.add(labeled("Purchase Date", valPdate));
        src1.add(labeled("Supplier ID",   valSupId));
        src1.add(labeled("Supplier Name", valSupName));
        panel.add(src1);

        JPanel src2 = gridPanel(3);
        valProId = roValLabel(); valProName = roValLabel(); valSku = roValLabel();
        src2.add(labeled("Product ID",   valProId));
        src2.add(labeled("Product Name", valProName));
        src2.add(labeled("SKU",          valSku));
        panel.add(src2);

        // ── Original Amounts ─────────────────────────────────────────────
        panel.add(sectionHeader("Original Purchase Amounts  (₹ INR)"));
        JPanel amt1 = gridPanel(3);
        valQty = roValLabel(); valRate = roValLabel(); valTot = roValLabel();
        amt1.add(labeled("Qty (original)", valQty));
        amt1.add(labeled("Rate (₹)",       valRate));
        amt1.add(labeled("Total Amt",      valTot));
        panel.add(amt1);

        JPanel amt2 = gridPanel(3);
        valVat = roValLabel(); valCon = roValLabel(); valNet = roValLabel();
        amt2.add(labeled("VAT %",       valVat));
        amt2.add(labeled("Concession %",valCon));
        amt2.add(labeled("Net Amt",     valNet));
        panel.add(amt2);

        // ── Return Summary ───────────────────────────────────────────────
        panel.add(sectionHeader("Return Summary"));
        JPanel ret1 = gridPanel(3);
        valNret = roValLabel(); valRetAmt = roValLabel(); valNarr = roValLabel();
        valRetAmt.setForeground(GOLD);
        ret1.add(labeled("Items Returned",  valNret));
        ret1.add(labeled("Total Return Amt",valRetAmt));
        ret1.add(labeled("Narration",       valNarr));
        panel.add(ret1);

        // ── Edit panel ───────────────────────────────────────────────────
        panel.add(sectionHeader("Edit Fields"));
        editPanel = new JPanel(new GridLayout(0,2,10,8));
        editPanel.setBackground(BG_CARD);
        editPanel.setBorder(new EmptyBorder(6,0,6,0));
        editPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,9999));
        editPanel.setAlignmentX(0f);

        edRetDate   = inputField();
        edPurchaseId= new JComboBox<>(); styleComboBox(edPurchaseId);
        loadPurchaseComboEdit(edPurchaseId);

        edPdate=roValLabel(); edSupId=roValLabel(); edSupName=roValLabel();
        edProId=roValLabel(); edProName=roValLabel(); edSku=roValLabel();
        edQty=roValLabel(); edRate=roValLabel(); edTot=roValLabel();
        edVat=roValLabel(); edCon=roValLabel(); edNet=roValLabel();
        edRetAmt=roValLabel(); edRetAmt.setForeground(GOLD);

        edNret = inputField(); edNarr = inputField();

        edPurchaseId.addActionListener(e -> fillFromPurchase());
        edNret.addKeyListener(new KeyAdapter(){ public void keyReleased(KeyEvent ke){ calcReturnAmt(); } });

        DocumentListener dirtyl = dl(() -> lblDirty.setVisible(true));
        edRetDate.getDocument().addDocumentListener(dirtyl);
        edNret   .getDocument().addDocumentListener(dirtyl);
        edNarr   .getDocument().addDocumentListener(dirtyl);

        editPanel.add(labeled("Return Date",     edRetDate));
        editPanel.add(labeled("Purchase ID",     edPurchaseId));
        editPanel.add(labeled("Purchase Date",   edPdate));
        editPanel.add(labeled("Supplier ID",     edSupId));
        editPanel.add(labeled("Supplier Name",   edSupName));
        editPanel.add(labeled("Product ID",      edProId));
        editPanel.add(labeled("Product Name",    edProName));
        editPanel.add(labeled("SKU",             edSku));
        editPanel.add(labeled("Qty (original)",  edQty));
        editPanel.add(labeled("Rate (₹)",        edRate));
        editPanel.add(labeled("Total Amt",       edTot));
        editPanel.add(labeled("VAT %",           edVat));
        editPanel.add(labeled("Concession %",    edCon));
        editPanel.add(labeled("Net Amt",         edNet));
        editPanel.add(labeled("Items to Return", edNret));
        editPanel.add(labeled("Total Return Amt",edRetAmt));
        editPanel.add(labeled("Narration",       edNarr));
        editPanel.setVisible(false);
        panel.add(editPanel);

        return panel;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,10));
        bar.setBackground(new Color(12,18,34));
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,BORDER_COL));

        btnEdit   = makeBtn("✏ Edit",    GOLD,    new Color(140,96,10));
        btnSave   = makeBtn("💾 Save",   ACCENT2, new Color(22,108,68));
        btnCancel = makeBtn("✕ Cancel",  TEXT_MUT, BG_INPUT);
        btnDelete = makeBtn("🗑 Delete", DANGER,  new Color(140,35,35));

        btnEdit  .addActionListener(e -> enterEditMode());
        btnSave  .addActionListener(e -> saveEdit());
        btnCancel.addActionListener(e -> cancelEdit());
        btnDelete.addActionListener(e -> deleteRecord());

        btnSave.setVisible(false); btnCancel.setVisible(false);

        bar.add(btnEdit); bar.add(btnDelete);
        bar.add(btnSave); bar.add(btnCancel);
        JLabel note = new JLabel("  Ctrl+S Save  |  Ctrl+N New  |  Esc Refresh");
        note.setFont(F_SMALL); note.setForeground(new Color(55,78,115));
        bar.add(note);
        return bar;
    }

    private JPanel buildShortcutBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT,16,4));
        p.setBackground(new Color(7,10,20));
        p.setBorder(BorderFactory.createMatteBorder(1,0,0,0,new Color(24,34,56)));
        for (String s : new String[]{"Ctrl+S  Save Edit","Ctrl+N  New Return","Esc  Refresh"}) {
            JLabel l=new JLabel(s); l.setFont(F_SMALL); l.setForeground(new Color(55,78,115)); p.add(l);
        }
        toastLbl=new JLabel(""); toastLbl.setFont(F_SMALL); toastLbl.setOpaque(true); toastLbl.setVisible(false);
        toastLbl.setBorder(new EmptyBorder(3,12,3,12));
        p.add(Box.createHorizontalStrut(20)); p.add(toastLbl);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Shortcuts
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
        allRows.clear(); tot=0;
        try {
            stmt=MdiForm.sconnect.createStatement();
            rs=stmt.executeQuery(
                "SELECT pr.ret_id, pr.purchase_ret_no, pr.purchase_ret_date, " +
                "  pr.purchase_id, pr.purchase_date, " +
                "  pr.supplier_id, COALESCE(s.supplier_name,'—') AS supplier_name, " +
                "  pr.product_id,  COALESCE(p.name,'—') AS product_name, " +
                "  COALESCE(p.sku,'—') AS sku, " +
                "  pr.qty, pr.rate, pr.total_amt, pr.vat_pct, pr.vat_amt, " +
                "  pr.con_pct, pr.con_amt, pr.net_amt, " +
                "  pr.no_of_ret_item, pr.total_ret_amt, pr.narration " +
                "FROM purchase_return pr " +
                "LEFT JOIN suppliers s ON pr.supplier_id = s.id " +
                "LEFT JOIN products  p ON pr.product_id  = p.id " +
                "ORDER BY pr.ret_id DESC");
            while (rs.next()) {
                allRows.add(new String[]{
                    ns(rs.getString("ret_id")),
                    ns(rs.getString("purchase_ret_no")),
                    ns(rs.getString("purchase_ret_date")),
                    ns(rs.getString("purchase_id")),
                    ns(rs.getString("purchase_date")),
                    ns(rs.getString("supplier_id")),
                    ns(rs.getString("supplier_name")),
                    ns(rs.getString("product_id")),
                    ns(rs.getString("product_name")),
                    ns(rs.getString("sku")),
                    ns(rs.getString("qty")),
                    ns(rs.getString("rate")),
                    ns(rs.getString("total_amt")),
                    ns(rs.getString("vat_pct")),
                    ns(rs.getString("vat_amt")),
                    ns(rs.getString("con_pct")),
                    ns(rs.getString("con_amt")),
                    ns(rs.getString("net_amt")),
                    ns(rs.getString("no_of_ret_item")),
                    ns(rs.getString("total_ret_amt")),
                    ns(rs.getString("narration"))
                });
                tot++;
            }
        } catch (Exception ex) { showToast("DB error: "+ex.getMessage(), DANGER); }
        applyFilter(); refreshStats();
        detailPanel.setVisible(false);
        lblDetailId.setText("—"); selectedFilteredIdx=-1;
    }

    private void onRowSelected() {
        int vr=masterTable.getSelectedRow(); if (vr<0) return;
        if (vr<0||vr>=filtered.size()) return;
        selectedFilteredIdx=vr; showDetail(false);
    }

    private void showDetail(boolean editMode) {
        if (selectedFilteredIdx<0||selectedFilteredIdx>=filtered.size()) return;
        String[] r=filtered.get(selectedFilteredIdx);

        lblDetailId .setText("RET #"+r[C_RETNO]);
        lblDetailSub.setText("PO "+r[C_PID]+" · "+r[C_RETDATE]);

        valRetNo  .setText(r[C_RETNO]);   valRetDate.setText(r[C_RETDATE]);
        valPid    .setText(r[C_PID]);     valPdate  .setText(r[C_PDATE]);
        valSupId  .setText(r[C_SUPID]);   valSupName.setText(r[C_SUPNM]);
        valProId  .setText(r[C_PROID]);   valProName.setText(r[C_PRONM]);
        valSku    .setText(r[C_SKU]);
        valQty    .setText(r[C_QTY]);     valRate   .setText("₹"+fmt(r[C_RATE]));
        valTot    .setText("₹"+fmt(r[C_TOT]));
        valVat    .setText(r[C_VATPCT]+" %");
        valCon    .setText(r[C_CONPCT]+" %");
        valNet    .setText("₹"+fmt(r[C_NET]));
        valNret   .setText(r[C_NRET]);
        valRetAmt .setText("₹"+fmt(r[C_RETAMT]));
        valNarr   .setText(r[C_NARR]);

        if (editMode) {
            edRetDate.setText(r[C_RETDATE]);
            setComboByValue(edPurchaseId, r[C_PID]);
            edNret.setText(r[C_NRET]); edNarr.setText(r[C_NARR]);
            fillFromPurchase();
        }

        editPanel.setVisible(editMode);
        btnEdit  .setVisible(!editMode); btnDelete.setVisible(!editMode);
        btnSave  .setVisible(editMode);  btnCancel.setVisible(editMode);
        lblDirty .setVisible(false);
        detailPanel.setVisible(true);
        detailPanel.revalidate(); detailPanel.repaint();
    }

    private void enterEditMode() { showDetail(true); }
    private void cancelEdit()    { showDetail(false); }

    private void fillFromPurchase() {
        if (edPurchaseId.getSelectedItem()==null) return;
        String pid=edPurchaseId.getSelectedItem().toString().trim(); if (pid.isEmpty()) return;
        try {
            if (!tableExists("PURCHASE_DETAILS")) return;
            PreparedStatement ps=MdiForm.sconnect.prepareStatement(
                "SELECT pd.purchase_date, pd.supplier_id, COALESCE(s.supplier_name,'—') AS supplier_name, " +
                "  pd.product_id, COALESCE(p.name,'—') AS product_name, COALESCE(p.sku,'—') AS sku, " +
                "  pd.qty, pd.rate, pd.total_amt, pd.vat_pct, pd.con_pct, pd.net_amt " +
                "FROM purchase_details pd " +
                "LEFT JOIN suppliers s ON pd.supplier_id=s.id " +
                "LEFT JOIN products  p ON pd.product_id=p.id " +
                "WHERE pd.purchase_id=?");
            ps.setLong(1,Long.parseLong(pid));
            ResultSet r=ps.executeQuery();
            if (r.next()) {
                edPdate  .setText(ns(r.getString("purchase_date")));
                edSupId  .setText(ns(r.getString("supplier_id")));
                edSupName.setText(ns(r.getString("supplier_name")));
                edProId  .setText(ns(r.getString("product_id")));
                edProName.setText(ns(r.getString("product_name")));
                edSku    .setText(ns(r.getString("sku")));
                edQty    .setText(ns(r.getString("qty")));
                edRate   .setText("₹"+fmt(r.getString("rate")));
                edTot    .setText("₹"+fmt(r.getString("total_amt")));
                edVat    .setText(ns(r.getString("vat_pct"))+" %");
                edCon    .setText(ns(r.getString("con_pct"))+" %");
                edNet    .setText("₹"+fmt(r.getString("net_amt")));
                calcReturnAmt();
            }
        } catch (Exception ignored) {}
    }

    private void calcReturnAmt() {
        try {
            double rate=parseAmt(edRate.getText()), nret=parseDbl(edNret.getText());
            edRetAmt.setText("₹"+fmt(rate*nret));
        } catch (Exception ignored) {}
    }

    private void saveEdit() {
        if (selectedFilteredIdx<0) { showToast("Select a record first!", WARNING); return; }
        String[] r=filtered.get(selectedFilteredIdx);
        int nret; try { nret=Integer.parseInt(edNret.getText().trim()); }
        catch (NumberFormatException ex) { showToast("Enter valid number for items!", WARNING); return; }
        double rate=parseAmt(edRate.getText()), retAmt=rate*nret;
        long pidLong=0;
        try { pidLong=Long.parseLong(edPurchaseId.getSelectedItem()!=null?edPurchaseId.getSelectedItem().toString().trim():"0"); }
        catch (Exception ignored) {}
        long supId=parseLng(edSupId.getText()), proId=parseLng(edProId.getText());
        try {
            pstmt=MdiForm.sconnect.prepareStatement(
                "UPDATE purchase_return SET purchase_ret_date=?,purchase_id=?,purchase_date=?," +
                "supplier_id=?,product_id=?,qty=?,rate=?,total_amt=?,vat_pct=?,vat_amt=?," +
                "con_pct=?,con_amt=?,net_amt=?,no_of_ret_item=?,total_ret_amt=?,narration=? WHERE ret_id=?");
            pstmt.setString(1,edRetDate.getText().trim()); pstmt.setLong(2,pidLong);
            pstmt.setString(3,edPdate.getText());
            pstmt.setLong(4,supId); pstmt.setLong(5,proId);
            pstmt.setInt(6,parseInt(edQty.getText())); pstmt.setDouble(7,rate);
            pstmt.setDouble(8,parseAmt(edTot.getText()));
            pstmt.setDouble(9,parseVatCon(edVat.getText()));
            pstmt.setDouble(10,parseAmt(edTot.getText())*parseVatCon(edVat.getText())/100);
            pstmt.setDouble(11,parseVatCon(edCon.getText()));
            pstmt.setDouble(12,parseAmt(edTot.getText())*parseVatCon(edCon.getText())/100);
            pstmt.setDouble(13,parseAmt(edNet.getText()));
            pstmt.setInt(14,nret); pstmt.setDouble(15,retAmt);
            pstmt.setString(16,edNarr.getText().trim());
            pstmt.setLong(17,Long.parseLong(r[C_RETID]));
            pstmt.executeUpdate();
            showToast("✔ Record updated", ACCENT2); loadFromDB();
        } catch (Exception ex) { showToast("DB error: "+ex.getMessage(), DANGER); }
    }

    private void deleteRecord() {
        if (selectedFilteredIdx<0) { showToast("Select a record first!", WARNING); return; }
        String[] r=filtered.get(selectedFilteredIdx);
        int ans=JOptionPane.showConfirmDialog(this,
            "Delete Return "+r[C_RETNO]+"? This cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans!=JOptionPane.YES_OPTION) return;
        try {
            pstmt=MdiForm.sconnect.prepareStatement("DELETE FROM purchase_return WHERE ret_id=?");
            pstmt.setLong(1,Long.parseLong(r[C_RETID]));
            pstmt.executeUpdate();
            showToast("🗑 Record deleted", WARNING); loadFromDB();
        } catch (Exception ex) { showToast("DB error: "+ex.getMessage(), DANGER); }
    }

    // ── New Return Dialog ─────────────────────────────────────────────────
    private void openNewDialog() {
        JDialog dlg=new JDialog((JFrame)SwingUtilities.getWindowAncestor(this),"New Purchase Return",true);
        dlg.setSize(580,580); dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_MAIN); dlg.setLayout(new BorderLayout());

        JPanel dHdr=new JPanel(new BorderLayout()){{
            setPreferredSize(new Dimension(0,46)); setBorder(new EmptyBorder(0,18,0,18));
            setBackground(new Color(50,22,8));
        }};
        JLabel dTitle=new JLabel("↩  New Purchase Return");
        dTitle.setFont(new Font("Segoe UI",Font.BOLD,15)); dTitle.setForeground(TEXT_PRI);
        dHdr.add(dTitle, BorderLayout.WEST);

        JPanel form=new JPanel(new GridLayout(0,2,10,8));
        form.setBackground(BG_CARD); form.setBorder(new EmptyBorder(16,18,12,18));

        JTextField nRetNo=inputField(), nRetDate=inputField(), nNret=inputField(), nNarr=inputField();
        JLabel nPdate=roValLabel(), nSupId=roValLabel(), nSupName=roValLabel(),
               nProId=roValLabel(), nProName=roValLabel(), nSku=roValLabel(),
               nQty=roValLabel(), nRate=roValLabel(), nTot=roValLabel(),
               nVat=roValLabel(), nCon=roValLabel(), nNet=roValLabel(), nRetAmt=roValLabel();
        nRetAmt.setForeground(GOLD);

        JComboBox<String> nPid=new JComboBox<>(); styleComboBox(nPid); loadPurchaseComboEdit(nPid);
        nRetDate.setText(new SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date()));

        nPid.addActionListener(e -> {
            if (nPid.getSelectedItem()==null) return;
            String pidStr=nPid.getSelectedItem().toString().trim(); if (pidStr.isEmpty()) return;
            try {
                PreparedStatement ps=MdiForm.sconnect.prepareStatement(
                    "SELECT pd.purchase_date, pd.supplier_id, COALESCE(s.supplier_name,'—') AS supplier_name, " +
                    "  pd.product_id, COALESCE(p.name,'—') AS product_name, COALESCE(p.sku,'—') AS sku, " +
                    "  pd.qty, pd.rate, pd.total_amt, pd.vat_pct, pd.con_pct, pd.net_amt " +
                    "FROM purchase_details pd LEFT JOIN suppliers s ON pd.supplier_id=s.id " +
                    "LEFT JOIN products p ON pd.product_id=p.id WHERE pd.purchase_id=?");
                ps.setLong(1,Long.parseLong(pidStr));
                ResultSet r2=ps.executeQuery();
                if (r2.next()) {
                    nPdate.setText(ns(r2.getString("purchase_date")));
                    nSupId.setText(ns(r2.getString("supplier_id")));
                    nSupName.setText(ns(r2.getString("supplier_name")));
                    nProId.setText(ns(r2.getString("product_id")));
                    nProName.setText(ns(r2.getString("product_name")));
                    nSku.setText(ns(r2.getString("sku")));
                    nQty.setText(ns(r2.getString("qty")));
                    nRate.setText("₹"+fmt(r2.getString("rate")));
                    nTot.setText("₹"+fmt(r2.getString("total_amt")));
                    nVat.setText(ns(r2.getString("vat_pct"))+" %");
                    nCon.setText(ns(r2.getString("con_pct"))+" %");
                    nNet.setText("₹"+fmt(r2.getString("net_amt")));
                }
            } catch (Exception ignored) {}
        });
        nNret.addKeyListener(new KeyAdapter(){
            public void keyReleased(KeyEvent ke) {
                try { nRetAmt.setText("₹"+fmt(parseAmt(nRate.getText())*parseDbl(nNret.getText()))); }
                catch (Exception ignored) {}
            }
        });

        form.add(labeled("Return No",       nRetNo));
        form.add(labeled("Return Date",     nRetDate));
        form.add(labeled("Purchase ID",     nPid));
        form.add(labeled("Purchase Date",   nPdate));
        form.add(labeled("Supplier ID",     nSupId));
        form.add(labeled("Supplier Name",   nSupName));
        form.add(labeled("Product ID",      nProId));
        form.add(labeled("Product Name",    nProName));
        form.add(labeled("SKU",             nSku));
        form.add(labeled("Qty (original)",  nQty));
        form.add(labeled("Rate (₹)",        nRate));
        form.add(labeled("Total Amt",       nTot));
        form.add(labeled("VAT %",           nVat));
        form.add(labeled("Concession %",    nCon));
        form.add(labeled("Net Amt",         nNet));
        form.add(labeled("Items to Return", nNret));
        form.add(labeled("Total Return Amt",nRetAmt));
        form.add(labeled("Narration",       nNarr));

        JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,10));
        btns.setBackground(new Color(12,18,34));
        btns.setBorder(BorderFactory.createMatteBorder(1,0,0,0,BORDER_COL));
        JButton save=makeBtn("💾 Save",ACCENT2,new Color(22,108,68));
        JButton cancel=makeBtn("✕ Cancel",TEXT_MUT,BG_INPUT);

        save.addActionListener(e -> {
            try {
                if (nRetNo.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(dlg,"Return No is required!"); return; }
                int nretVal=Integer.parseInt(nNret.getText().trim());
                double rate=parseAmt(nRate.getText()), retAmt=rate*nretVal;
                long pidLong=0;
                try{pidLong=Long.parseLong(nPid.getSelectedItem()!=null?nPid.getSelectedItem().toString().trim():"0");}catch(Exception ignored){}
                long supId=parseLng(nSupId.getText()), proId=parseLng(nProId.getText());
                pstmt=MdiForm.sconnect.prepareStatement(
                    "INSERT INTO purchase_return(purchase_ret_no,purchase_ret_date,purchase_id,purchase_date," +
                    "supplier_id,product_id,qty,rate,total_amt,vat_pct,vat_amt,con_pct,con_amt,net_amt," +
                    "no_of_ret_item,total_ret_amt,narration) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                pstmt.setString(1,nRetNo.getText().trim()); pstmt.setString(2,nRetDate.getText().trim());
                pstmt.setLong(3,pidLong); pstmt.setString(4,nPdate.getText());
                pstmt.setLong(5,supId); pstmt.setLong(6,proId);
                pstmt.setInt(7,parseInt(nQty.getText())); pstmt.setDouble(8,rate);
                pstmt.setDouble(9,parseAmt(nTot.getText()));
                pstmt.setDouble(10,parseVatCon(nVat.getText()));
                pstmt.setDouble(11,parseAmt(nTot.getText())*parseVatCon(nVat.getText())/100);
                pstmt.setDouble(12,parseVatCon(nCon.getText()));
                pstmt.setDouble(13,parseAmt(nTot.getText())*parseVatCon(nCon.getText())/100);
                pstmt.setDouble(14,parseAmt(nNet.getText()));
                pstmt.setInt(15,nretVal); pstmt.setDouble(16,retAmt);
                pstmt.setString(17,nNarr.getText().trim());
                pstmt.executeUpdate();
                dlg.dispose(); loadFromDB(); showToast("✔ Return added", ACCENT2);
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(dlg,"Enter valid number for items to return!"); }
            catch (Exception ex) { JOptionPane.showMessageDialog(dlg,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
        });
        cancel.addActionListener(e -> dlg.dispose());
        btns.add(cancel); btns.add(save);

        dlg.add(dHdr, BorderLayout.NORTH);
        dlg.add(new JScrollPane(form){{ setBorder(null); getViewport().setBackground(BG_CARD); }}, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Filter (no pagination)
    // ═════════════════════════════════════════════════════════════════════
    private void applyFilter() {
        String q=txtSearch!=null?txtSearch.getText().toLowerCase().trim():"";
        String p=cmbFilterPurchase!=null?(String)cmbFilterPurchase.getSelectedItem():"All purchases";
        filtered.clear();
        for (String[] r : allRows) {
            boolean mQ=q.isEmpty()||r[C_RETNO].toLowerCase().contains(q)||r[C_SUPNM].toLowerCase().contains(q)
                      ||r[C_PRONM].toLowerCase().contains(q)||r[C_PID].contains(q);
            boolean mP="All purchases".equals(p)||p.equals(r[C_PID]);
            if (mQ&&mP) filtered.add(r);
        }
        selectedFilteredIdx=-1;
        refreshTable();
        lblCount.setText(filtered.size()+" / "+tot+" records");
    }

    private void refreshTable() {
        tModel.setRowCount(0);
        for (String[] r : filtered) {
            tModel.addRow(new Object[]{r[C_RETID],r[C_RETNO],r[C_RETDATE],r[C_PID],r[C_SUPNM],r[C_NRET],"₹"+fmt(r[C_RETAMT])});
        }
    }

    private void refreshStats() {
        double totRet=0; int totalItems=0;
        for (String[] r : allRows) {
            try{totRet+=Double.parseDouble(r[C_RETAMT]);}catch(Exception ignored){}
            try{totalItems+=Integer.parseInt(r[C_NRET]);}catch(Exception ignored){}
        }
        double avg=allRows.isEmpty()?0:totRet/allRows.size();
        lblTotalRet   .setText(allRows.size()+" Returns");
        lblTotalRetAmt.setText("₹"+fmt(totRet)+" Total");
        lblAvgRetAmt  .setText("₹"+fmt(avg)+" Avg");
        lblTotalItems .setText(totalItems+" Items");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Combo helpers
    // ═════════════════════════════════════════════════════════════════════
    private void loadPurchaseFilter() {
        try {
            if (!tableExists("PURCHASE_DETAILS")) return;
            Statement st=MdiForm.sconnect.createStatement();
            ResultSet r=st.executeQuery("SELECT purchase_id FROM purchase_details ORDER BY purchase_id");
            while (r.next()) cmbFilterPurchase.addItem(r.getString(1));
        } catch (Exception ignored) {}
    }

    private void loadPurchaseComboEdit(JComboBox<String> cmb) {
        cmb.addItem("");
        try {
            if (!tableExists("PURCHASE_DETAILS")) return;
            Statement st=MdiForm.sconnect.createStatement();
            ResultSet r=st.executeQuery("SELECT purchase_id FROM purchase_details ORDER BY purchase_id");
            while (r.next()) cmb.addItem(r.getString(1));
        } catch (Exception ignored) {}
    }

    private void setComboByValue(JComboBox<String> cmb, String val) {
        for (int i=0;i<cmb.getItemCount();i++)
            if (val.equals(cmb.getItemAt(i))) { cmb.setSelectedIndex(i); return; }
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI helpers
    // ═════════════════════════════════════════════════════════════════════
    private JPanel sectionHeader(String text) {
        JPanel p=new JPanel(new BorderLayout());
        p.setBackground(SECTION_BG); p.setMaximumSize(new Dimension(Integer.MAX_VALUE,28)); p.setAlignmentX(0f);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,BORDER_COL), new EmptyBorder(4,8,4,8)));
        JLabel l=new JLabel(text.toUpperCase()); l.setFont(F_SEC); l.setForeground(ACCENT);
        p.add(l, BorderLayout.WEST); return p;
    }

    private JPanel gridPanel(int cols) {
        JPanel p=new JPanel(new GridLayout(1,cols,8,0));
        p.setBackground(BG_CARD); p.setBorder(new EmptyBorder(6,0,4,0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,62)); p.setAlignmentX(0f);
        return p;
    }

    private JPanel labeled(String lbl, Component field) {
        JPanel p=new JPanel(new BorderLayout(0,3)); p.setBackground(BG_CARD);
        JLabel l=new JLabel(lbl); l.setFont(F_LABEL); l.setForeground(TEXT_MUT);
        p.add(l,BorderLayout.NORTH); p.add(field,BorderLayout.CENTER); return p;
    }

    private JLabel roValLabel() {
        JLabel l=new JLabel("—"); l.setFont(F_INPUT);
        l.setOpaque(true); l.setBackground(new Color(14,20,38)); l.setForeground(ACCENT);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4,8,4,8)));
        return l;
    }

    private JTextField inputField() {
        JTextField tf=new JTextField(); tf.setFont(F_INPUT);
        tf.setBackground(BG_INPUT); tf.setForeground(TEXT_PRI); tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4,8,4,8)));
        return tf;
    }

    private JTextField styledField(String ph) {
        JTextField tf=new JTextField(){
            public void paintComponent(Graphics g){
                super.paintComponent(g);
                if (getText().isEmpty()&&!isFocusOwner()){g.setColor(TEXT_MUT);g.setFont(F_SMALL);g.drawString(ph,10,getHeight()/2+4);}
            }
        };
        tf.setFont(F_INPUT); tf.setBackground(BG_INPUT); tf.setForeground(TEXT_PRI); tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL),new EmptyBorder(4,8,4,8)));
        return tf;
    }

    private void styleComboBox(JComboBox<?> cb) {
        cb.setFont(F_INPUT); cb.setBackground(BG_INPUT); cb.setForeground(TEXT_PRI);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COL));
    }

    private JComboBox<String> miniComboRaw(String... items) {
        JComboBox<String> cb=new JComboBox<>(items);
        cb.setFont(F_SMALL); cb.setBackground(BG_INPUT); cb.setForeground(TEXT_PRI);
        cb.setPreferredSize(new Dimension(200,28)); return cb;
    }

    private JLabel styledBadge(String text, Color fg, Color bg) {
        JLabel l=new JLabel(text); l.setFont(F_SMALL); l.setForeground(fg);
        l.setOpaque(true); l.setBackground(bg); l.setBorder(new EmptyBorder(3,10,3,10));
        return l;
    }

    private JButton makeBtn(String text, Color fg, Color bg) {
        JButton b=new JButton(text){
            public void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()?bg.darker():getModel().isRollover()?bg.brighter():bg);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(fg.brighter()); g2.setFont(F_BTN);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(118,34)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleScrollBar(JScrollPane sp) {
        for (JScrollBar sb : new JScrollBar[]{sp.getVerticalScrollBar(),sp.getHorizontalScrollBar()}) {
            if (sb==null) continue;
            sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
                protected void configureScrollBarColors(){thumbColor=new Color(46,66,108);trackColor=BG_PANEL;}
                protected JButton createDecreaseButton(int o){return zb();}
                protected JButton createIncreaseButton(int o){return zb();}
                private JButton zb(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b;}
            });
        }
    }

    private void showToast(String msg, Color bg) {
        if (toastLbl==null) return;
        toastLbl.setText(msg); toastLbl.setBackground(bg); toastLbl.setForeground(Color.WHITE);
        toastLbl.setVisible(true);
        if (toastTimer!=null&&toastTimer.isRunning()) toastTimer.stop();
        toastTimer=new javax.swing.Timer(3000,e->toastLbl.setVisible(false));
        toastTimer.setRepeats(false); toastTimer.start();
    }

    private DocumentListener dl(Runnable r) {
        return new DocumentListener(){
            public void insertUpdate(DocumentEvent e){r.run();}
            public void removeUpdate(DocumentEvent e){r.run();}
            public void changedUpdate(DocumentEvent e){}
        };
    }

    private double parseAmt(String s){try{return Double.parseDouble(s.replace("₹","").replace(",","").trim());}catch(Exception e){return 0;}}
    private double parseVatCon(String s){try{return Double.parseDouble(s.replace("%","").replace(",","").trim());}catch(Exception e){return 0;}}
    private double parseDbl(String s){try{return Double.parseDouble(s.trim().isEmpty()?"0":s.trim());}catch(Exception e){return 0;}}
    private int parseInt(String s){try{return Integer.parseInt(s.replace(",","").trim().isEmpty()?"0":s.replace(",","").trim());}catch(Exception e){return 0;}}
    private long parseLng(String s){try{return Long.parseLong(s.trim());}catch(Exception e){return 0;}}
    private String ns(String s){return s!=null?s:"";}
    private static String fmt(double v){return String.format("%,.2f",v);}
    private static String fmt(String s){try{return fmt(Double.parseDouble(s));}catch(Exception e){return s!=null?s:"0.00";}}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f=new JFrame("Test"); JDesktopPane d=new JDesktopPane();
            d.setBackground(new Color(8,12,20)); f.setContentPane(d);
            f.setSize(1380,820); f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationRelativeTo(null); f.setVisible(true);
            d.add(new PurchaseReturn());
        });
    }
}