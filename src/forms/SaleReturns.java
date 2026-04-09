package forms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import static ui.UIConstants.*;

/**
 * SaleReturns — CRUA (Create, Read, Update, Delete) UI
 *
 * Table: sale_returns
 *   return_id, sale_id (FK→sale_details), customer_id (FK→customers),
 *   return_date, total_amt, vat_amt, discount_amt, net_amt, narration, created_at
 *
 * When a original Sale ID is selected, amounts are pre-filled from that sale.
 */
public class SaleReturns extends JInternalFrame {

    // ── Lookup models ──────────────────────────────────────────────────────
    private static class SaleRef {
        long id; String display; long customerId; String customerName;
        double totalAmt; double vatAmt; double discAmt; double netAmt;
        SaleRef(long id, String display, long cid, String cname,
                double tot, double vat, double disc, double net) {
            this.id=id; this.display=display; this.customerId=cid; this.customerName=cname;
            this.totalAmt=tot; this.vatAmt=vat; this.discAmt=disc; this.netAmt=net;
        }
        public String toString() { return id + " — " + display; }
    }

    private static class Customer {
        long id; String name;
        Customer(long id, String name) { this.id=id; this.name=name; }
        public String toString() { return id + " — " + name; }
    }

    private final List<SaleRef>  saleRefList   = new ArrayList<>();
    private final List<Customer> customerList  = new ArrayList<>();

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

    // ── Filters ────────────────────────────────────────────────────────────
    private JTextField txtSearch;
    private JTextField txtFDate, txtFCustomer, txtFSaleId;

    // ── Form fields ────────────────────────────────────────────────────────
    private JTextField fId, fReturnDate, fTotalAmt, fVatAmt, fDiscAmt, fNetAmt, fCreatedAt;
    private JTextArea  fNarration;
    private JLabel lblDirty, lblNetBadge;

    // ── Original Sale searchable combo ────────────────────────────────────
    private JTextField  saleSearch;
    private JPopupMenu  salePopup;
    private JList<SaleRef> salePopupList;
    private DefaultListModel<SaleRef> salePopupModel;
    private SaleRef selectedSale = null;
    private JLabel  lblSaleSelected;

    // ── Customer searchable combo ──────────────────────────────────────────
    private JTextField  customerSearch;
    private JPopupMenu  customerPopup;
    private JList<Customer> customerPopupList;
    private DefaultListModel<Customer> customerPopupModel;
    private Customer selectedCustomer = null;
    private JLabel   lblCustomerSelected;

    // ── Buttons ────────────────────────────────────────────────────────────
    private JButton btnNew, btnSave, btnUpdate, btnDelete, btnRefresh;

    // ── Toast ──────────────────────────────────────────────────────────────
    private JLabel toastLbl;
    private javax.swing.Timer toastTimer;

    // ──────────────────────────────────────────────────────────────────────
    public SaleReturns() {
        super("Sale Returns", true, true, true, true);
        setSize(1380, 780);
        setLocation(50, 50);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ensureTable();
        loadSaleRefs();
        loadCustomers();
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
                    "CREATE TABLE sale_returns (" +
                    "  return_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "  sale_id       NUMBER," +
                    "  customer_id   NUMBER," +
                    "  return_date   VARCHAR2(20)," +
                    "  total_amt     NUMBER(12,2) DEFAULT 0," +
                    "  vat_amt       NUMBER(10,2) DEFAULT 0," +
                    "  discount_amt  NUMBER(10,2) DEFAULT 0," +
                    "  net_amt       NUMBER(12,2) DEFAULT 0," +
                    "  narration     VARCHAR2(500)," +
                    "  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  CONSTRAINT fk_sr_sale FOREIGN KEY (sale_id)" +
                    "    REFERENCES sale_details(sale_id) ON DELETE SET NULL," +
                    "  CONSTRAINT fk_sr_customer FOREIGN KEY (customer_id)" +
                    "    REFERENCES customers(id) ON DELETE SET NULL" +
                    ")");
            } catch (SQLException ignored) {}
        } catch (SQLException ex) {
            showToast("DB Init Error: " + ex.getMessage(), DANGER);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Load lookup lists
    // ═════════════════════════════════════════════════════════════════════
    private void loadSaleRefs() {
        saleRefList.clear();
        saleRefList.add(new SaleRef(0, "(none)", 0, "", 0, 0, 0, 0));
        try {
            stmt = MdiForm.sconnect.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT sd.sale_id, sd.sale_date, " +
                "COALESCE(c.customer_name,'—') AS cname, " +
                "COALESCE(sd.customer_id, 0) AS cid, " +
                "sd.total_amt, sd.vat_amt, sd.discount_amt, sd.net_amt " +
                "FROM sale_details sd " +
                "LEFT JOIN customers c ON sd.customer_id = c.id " +
                "ORDER BY sd.sale_id DESC");
            while (rs.next()) {
                long sid = rs.getLong("sale_id");
                String display = rs.getString("sale_date") + "  " + rs.getString("cname");
                saleRefList.add(new SaleRef(sid, display,
                    rs.getLong("cid"), rs.getString("cname"),
                    rs.getDouble("total_amt"), rs.getDouble("vat_amt"),
                    rs.getDouble("discount_amt"), rs.getDouble("net_amt")));
            }
        } catch (SQLException ex) {
            showToast("Sale ref load error: " + ex.getMessage(), DANGER);
        }
    }

    private void loadCustomers() {
        customerList.clear();
        customerList.add(new Customer(0, "(none)"));
        try {
            stmt = MdiForm.sconnect.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT id, customer_name FROM customers WHERE is_active=1 ORDER BY customer_name");
            while (rs.next())
                customerList.add(new Customer(rs.getLong("id"), rs.getString("customer_name")));
        } catch (SQLException ex) {
            showToast("Customer load error: " + ex.getMessage(), DANGER);
        }
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

    // ── Header ─────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout()) {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(38, 14, 14), getWidth(), 0, new Color(10, 14, 26)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(CORAL);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        hdr.setPreferredSize(new Dimension(0, 54));
        hdr.setBorder(new EmptyBorder(0, 22, 0, 22));
        JLabel title = new JLabel("↩  Sale Returns");
        title.setFont(F_HEAD); title.setForeground(TEXT_PRI);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        lblCount = styledBadge("0 returns", TEXT_MUT, BG_INPUT);
        right.add(lblCount);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    private JSplitPane buildSplit() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        sp.setDividerLocation(620);
        sp.setDividerSize(4); sp.setBorder(null); sp.setBackground(BG_MAIN);
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

        txtSearch = styledField("🔍  Search by Customer or Return Date…");
        txtSearch.setPreferredSize(new Dimension(0, 34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));

        JPanel r1 = new JPanel(new GridLayout(1, 3, 5, 0));
        r1.setBackground(BG_PANEL); r1.setBorder(new EmptyBorder(6, 0, 4, 0));
        txtFDate     = miniField("Return Date…");
        txtFCustomer = miniField("Customer…");
        txtFSaleId   = miniField("Sale ID…");
        r1.add(txtFDate); r1.add(txtFCustomer); r1.add(txtFSaleId);

        for (JTextField tf : new JTextField[]{txtFDate, txtFCustomer, txtFSaleId})
            tf.getDocument().addDocumentListener(dl(this::applyFilter));

        outer.add(txtSearch, BorderLayout.NORTH);
        outer.add(r1, BorderLayout.CENTER);
        return outer;
    }

    private JScrollPane buildMasterTable() {
        // COL: 0=ReturnID 1=SaleID 2=ReturnDate 3=Customer 4=Total 5=VAT 6=Disc 7=Net
        String[] cols = {"ID","Sale ID","Return Date","Customer",
                         "Total (₹)","VAT (₹)","Disc (₹)","Net (₹)"};
        tModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 0 || c == 1) return Long.class;
                if (c == 4 || c == 5 || c == 6 || c == 7) return Double.class;
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
                if (sel) {
                    comp.setBackground(BG_ROW_SEL); comp.setForeground(Color.WHITE);
                } else if (row == hoverRow) {
                    comp.setBackground(BG_ROW_HOVER); comp.setForeground(TEXT_PRI);
                } else {
                    comp.setBackground(row % 2 == 0 ? BG_ROW_EVEN : BG_ROW_ODD);
                    comp.setForeground(TEXT_PRI);
                }
                if (!sel) {
                    int mc = masterTable.convertColumnIndexToModel(col);
                    if (mc == 0)  comp.setForeground(ACCENT);
                    if (mc == 1)  comp.setForeground(PURPLE);
                    if (mc == 3)  comp.setForeground(TEAL);
                    if (mc == 7)  comp.setForeground(CORAL);
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

        int[] widths = {55, 65, 95, 150, 80, 72, 72, 90};
        for (int i = 0; i < widths.length; i++)
            masterTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

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
        JLabel title = new JLabel("Return Record");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14)); title.setForeground(TEXT_PRI);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        lblDirty = new JLabel("● Unsaved");
        lblDirty.setFont(F_SMALL); lblDirty.setForeground(WARNING); lblDirty.setVisible(false);
        lblNetBadge = styledBadge("", CORAL, new Color(60, 20, 10));
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
        form.add(sectionHeader("Return Identity"));
        JPanel id1 = gridPanel(2);
        fId         = roField();
        fReturnDate = inputField();
        fReturnDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        id1.add(labeled("Return ID (auto)",              fId));
        id1.add(labeled("Return Date (dd-MM-yyyy) *",    fReturnDate));
        form.add(id1);

        // ── Original Sale ─────────────────────────────────────────────
        form.add(sectionHeader("Original Sale Reference"));
        form.add(buildSaleSearchRow());

        // ── Customer ──────────────────────────────────────────────────
        form.add(sectionHeader("Customer"));
        form.add(buildCustomerSearchRow());

        // ── Amounts (pre-filled from sale, editable) ───────────────────
        form.add(sectionHeader("Return Amounts (₹)"));
        JPanel amtPnl = gridPanel(2);
        fTotalAmt = inputField();
        fVatAmt   = inputField();
        amtPnl.add(labeled("Total Amount *", fTotalAmt));
        amtPnl.add(labeled("VAT Amount",     fVatAmt));
        form.add(amtPnl);

        JPanel amt2Pnl = gridPanel(2);
        fDiscAmt = inputField();
        fNetAmt  = inputField();
        amt2Pnl.add(labeled("Discount Amount", fDiscAmt));
        amt2Pnl.add(labeled("Net Amount",       fNetAmt));
        form.add(amt2Pnl);

        // auto net calc
        DocumentListener calcDl = dl(this::recalcNet);
        fTotalAmt.getDocument().addDocumentListener(calcDl);
        fVatAmt.getDocument().addDocumentListener(calcDl);
        fDiscAmt.getDocument().addDocumentListener(calcDl);

        // ── Narration ─────────────────────────────────────────────────
        form.add(sectionHeader("Narration"));
        fNarration = new JTextArea(3, 1);
        styleTextArea(fNarration);
        JScrollPane narScroll = new JScrollPane(fNarration);
        narScroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        narScroll.setPreferredSize(new Dimension(0, 72));
        narScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        styleScrollBar(narScroll);
        form.add(fullWidthLabeled("Narration", narScroll));

        // ── Timestamps ────────────────────────────────────────────────
        form.add(sectionHeader("Timestamps"));
        JPanel tsPnl = gridPanel(1);
        fCreatedAt = roField();
        tsPnl.add(labeled("Created At", fCreatedAt));
        form.add(tsPnl);

        // Dirty listeners
        DocumentListener dl = dl(() -> setDirty(true));
        for (JTextField tf : new JTextField[]{fReturnDate, fTotalAmt, fVatAmt, fDiscAmt, fNetAmt})
            tf.getDocument().addDocumentListener(dl);
        fNarration.getDocument().addDocumentListener(dl);

        JScrollPane scroll = new JScrollPane(form,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null); scroll.getViewport().setBackground(BG_CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll);
        return scroll;
    }

    // ── Net recalc ─────────────────────────────────────────────────────────
    private void recalcNet() {
        double total = parseDbl(fTotalAmt.getText());
        double vat   = parseDbl(fVatAmt.getText());
        double disc  = parseDbl(fDiscAmt.getText());
        double net   = total + vat - disc;
        // Only auto-fill net if user hasn't overridden it manually
        fNetAmt.setText(String.format("%.2f", net));
        if (net > 0) {
            lblNetBadge.setText("Return Net ₹" + String.format("%.2f", net));
            lblNetBadge.setForeground(CORAL);
            lblNetBadge.setBackground(new Color(60, 20, 10));
        } else {
            lblNetBadge.setText("");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Searchable Sale row
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildSaleSearchRow() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 0));
        wrapper.setBackground(BG_CARD); wrapper.setAlignmentX(0f);
        wrapper.setBorder(new EmptyBorder(6, 0, 8, 0));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        JPanel leftPart = new JPanel(new BorderLayout(0, 3));
        leftPart.setBackground(BG_CARD);
        JLabel lbl = new JLabel("Original Sale  (type Sale ID or Customer Name)");
        lbl.setFont(F_LABEL); lbl.setForeground(TEXT_MUT);

        saleSearch = new JTextField();
        saleSearch.setFont(F_INPUT); saleSearch.setBackground(BG_INPUT);
        saleSearch.setForeground(TEXT_PRI); saleSearch.setCaretColor(ACCENT);
        saleSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 100, 80, 140)),
                new EmptyBorder(5, 8, 5, 8)));
        saleSearch.setToolTipText("Type Sale ID or customer name to filter…");
        leftPart.add(lbl, BorderLayout.NORTH);
        leftPart.add(saleSearch, BorderLayout.CENTER);

        JPanel rightPart = new JPanel(new BorderLayout(0, 3));
        rightPart.setBackground(BG_CARD); rightPart.setPreferredSize(new Dimension(220, 0));
        JLabel selLbl = new JLabel("Selected:");
        selLbl.setFont(F_LABEL); selLbl.setForeground(TEXT_MUT);
        JPanel selRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selRow.setBackground(BG_CARD);
        lblSaleSelected = styledBadge("(none)", TEXT_MUT, new Color(22, 32, 58));
        JButton btnClear = new JButton("✕");
        btnClear.setFont(F_SMALL); btnClear.setForeground(TEXT_MUT);
        btnClear.setBackground(new Color(35, 20, 20));
        btnClear.setBorder(new EmptyBorder(3, 7, 3, 7)); btnClear.setFocusPainted(false);
        btnClear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> {
            selectedSale = null; saleSearch.setText("");
            lblSaleSelected.setText("(none)"); lblSaleSelected.setForeground(TEXT_MUT);
            lblSaleSelected.setBackground(new Color(22,32,58)); setDirty(true);
        });
        selRow.add(lblSaleSelected); selRow.add(btnClear);
        rightPart.add(selLbl, BorderLayout.NORTH); rightPart.add(selRow, BorderLayout.CENTER);

        wrapper.add(leftPart, BorderLayout.CENTER);
        wrapper.add(rightPart, BorderLayout.EAST);

        // Popup
        salePopupModel = new DefaultListModel<>();
        salePopupList  = new JList<>(salePopupModel);
        salePopupList.setFont(F_INPUT); salePopupList.setBackground(new Color(20,30,54));
        salePopupList.setForeground(TEXT_PRI); salePopupList.setSelectionBackground(BG_ROW_SEL);
        salePopupList.setSelectionForeground(Color.WHITE); salePopupList.setFixedCellHeight(28);
        salePopupList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object v, int idx, boolean s, boolean f) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l,v,idx,s,f);
                SaleRef sr = (SaleRef) v;
                c.setText("<html><font color='#ff6450'>" + sr.id + "</font>  " +
                        escHtml(sr.display) +
                        "  <font color='#ffc332'>₹" + String.format("%.2f", sr.netAmt) + "</font></html>");
                c.setBorder(new EmptyBorder(4,12,4,12));
                if (!s) c.setBackground(idx%2==0 ? new Color(20,30,54) : new Color(24,36,62));
                return c;
            }
        });
        salePopup = new JPopupMenu();
        salePopup.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        salePopup.setBackground(new Color(20,30,54));
        JScrollPane ps = new JScrollPane(salePopupList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ps.setBorder(null); ps.setPreferredSize(new Dimension(360, 180)); styleScrollBar(ps);
        salePopup.add(ps);

        saleSearch.getDocument().addDocumentListener(dl(() -> {
            filterSalePopup(saleSearch.getText().trim()); setDirty(true);
        }));
        saleSearch.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { filterSalePopup(saleSearch.getText().trim()); }
        });
        saleSearch.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { filterSalePopup(saleSearch.getText().trim()); }
        });
        saleSearch.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (!salePopup.isVisible()) return;
                if (e.getKeyCode()==KeyEvent.VK_DOWN) {
                    salePopupList.requestFocusInWindow();
                    if (salePopupList.getModel().getSize()>0) salePopupList.setSelectedIndex(0);
                    e.consume();
                }
                if (e.getKeyCode()==KeyEvent.VK_ESCAPE) salePopup.setVisible(false);
            }
        });
        salePopupList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { if (e.getClickCount()>=1) commitSaleSelection(); }
        });
        salePopupList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ENTER) commitSaleSelection();
                if (e.getKeyCode()==KeyEvent.VK_ESCAPE) salePopup.setVisible(false);
            }
        });
        return wrapper;
    }

    private void filterSalePopup(String query) {
        salePopupModel.clear();
        String q = query.toLowerCase();
        for (SaleRef sr : saleRefList) {
            if (q.isEmpty() || String.valueOf(sr.id).contains(q) || sr.display.toLowerCase().contains(q))
                salePopupModel.addElement(sr);
        }
        if (salePopupModel.isEmpty()) { salePopup.setVisible(false); return; }
        if (!salePopup.isVisible()) salePopup.show(saleSearch, 0, saleSearch.getHeight());
        salePopup.revalidate(); salePopup.repaint();
    }

    private void commitSaleSelection() {
        SaleRef sr = salePopupList.getSelectedValue();
        if (sr == null) return;
        salePopup.setVisible(false);
        if (sr.id == 0) {
            selectedSale = null; saleSearch.setText("");
            lblSaleSelected.setText("(none)"); lblSaleSelected.setForeground(TEXT_MUT);
            lblSaleSelected.setBackground(new Color(22,32,58));
        } else {
            selectedSale = sr; saleSearch.setText("Sale #" + sr.id + " — " + sr.display);
            lblSaleSelected.setText("#" + sr.id);
            lblSaleSelected.setForeground(CORAL);
            lblSaleSelected.setBackground(new Color(60, 20, 10));
            // Pre-fill amounts from original sale
            fTotalAmt.setText(fmt(sr.totalAmt));
            fVatAmt.setText(fmt(sr.vatAmt));
            fDiscAmt.setText(fmt(sr.discAmt));
            fNetAmt.setText(fmt(sr.netAmt));
            // Pre-fill customer if not yet chosen
            if (selectedCustomer == null && sr.customerId > 0) {
                Customer found = customerList.stream()
                        .filter(c -> c.id == sr.customerId).findFirst().orElse(null);
                if (found != null) {
                    selectedCustomer = found; customerSearch.setText(found.name);
                    lblCustomerSelected.setText("ID " + found.id + "  " + found.name);
                    lblCustomerSelected.setForeground(TEAL);
                    lblCustomerSelected.setBackground(new Color(12,44,44));
                }
            }
        }
        setDirty(true); saleSearch.requestFocusInWindow();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Customer search row
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildCustomerSearchRow() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 0));
        wrapper.setBackground(BG_CARD); wrapper.setAlignmentX(0f);
        wrapper.setBorder(new EmptyBorder(6, 0, 8, 0));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        JPanel leftPart = new JPanel(new BorderLayout(0, 3));
        leftPart.setBackground(BG_CARD);
        JLabel lbl = new JLabel("Customer  (type to search)");
        lbl.setFont(F_LABEL); lbl.setForeground(TEXT_MUT);

        customerSearch = new JTextField();
        customerSearch.setFont(F_INPUT); customerSearch.setBackground(BG_INPUT);
        customerSearch.setForeground(TEXT_PRI); customerSearch.setCaretColor(ACCENT);
        customerSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 200, 200, 140)),
                new EmptyBorder(5, 8, 5, 8)));
        leftPart.add(lbl, BorderLayout.NORTH);
        leftPart.add(customerSearch, BorderLayout.CENTER);

        JPanel rightPart = new JPanel(new BorderLayout(0, 3));
        rightPart.setBackground(BG_CARD); rightPart.setPreferredSize(new Dimension(210, 0));
        JLabel selLbl = new JLabel("Selected:");
        selLbl.setFont(F_LABEL); selLbl.setForeground(TEXT_MUT);
        JPanel selRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selRow.setBackground(BG_CARD);
        lblCustomerSelected = styledBadge("(none)", TEXT_MUT, new Color(22,32,58));
        JButton btnClear = new JButton("✕");
        btnClear.setFont(F_SMALL); btnClear.setForeground(TEXT_MUT);
        btnClear.setBackground(new Color(35, 20, 20));
        btnClear.setBorder(new EmptyBorder(3, 7, 3, 7)); btnClear.setFocusPainted(false);
        btnClear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> {
            selectedCustomer = null; customerSearch.setText("");
            lblCustomerSelected.setText("(none)"); lblCustomerSelected.setForeground(TEXT_MUT);
            lblCustomerSelected.setBackground(new Color(22,32,58)); setDirty(true);
        });
        selRow.add(lblCustomerSelected); selRow.add(btnClear);
        rightPart.add(selLbl, BorderLayout.NORTH); rightPart.add(selRow, BorderLayout.CENTER);

        wrapper.add(leftPart, BorderLayout.CENTER);
        wrapper.add(rightPart, BorderLayout.EAST);

        // Popup
        customerPopupModel = new DefaultListModel<>();
        customerPopupList  = new JList<>(customerPopupModel);
        customerPopupList.setFont(F_INPUT); customerPopupList.setBackground(new Color(20,30,54));
        customerPopupList.setForeground(TEXT_PRI); customerPopupList.setSelectionBackground(BG_ROW_SEL);
        customerPopupList.setSelectionForeground(Color.WHITE); customerPopupList.setFixedCellHeight(28);
        customerPopupList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object v, int idx, boolean s, boolean f) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l,v,idx,s,f);
                Customer cu = (Customer) v;
                c.setText("<html><font color='#32c8c8'>" + cu.id + "</font>  " + escHtml(cu.name) + "</html>");
                c.setBorder(new EmptyBorder(4,12,4,12));
                if (!s) c.setBackground(idx%2==0 ? new Color(20,30,54) : new Color(24,36,62));
                return c;
            }
        });
        customerPopup = new JPopupMenu();
        customerPopup.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        customerPopup.setBackground(new Color(20,30,54));
        JScrollPane ps = new JScrollPane(customerPopupList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ps.setBorder(null); ps.setPreferredSize(new Dimension(320, 160)); styleScrollBar(ps);
        customerPopup.add(ps);

        customerSearch.getDocument().addDocumentListener(dl(() -> {
            filterCustomerPopup(customerSearch.getText().trim()); setDirty(true);
        }));
        customerSearch.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { filterCustomerPopup(customerSearch.getText().trim()); }
        });
        customerSearch.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { filterCustomerPopup(customerSearch.getText().trim()); }
        });
        customerSearch.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (!customerPopup.isVisible()) return;
                if (e.getKeyCode()==KeyEvent.VK_DOWN) {
                    customerPopupList.requestFocusInWindow();
                    if (customerPopupList.getModel().getSize()>0) customerPopupList.setSelectedIndex(0);
                    e.consume();
                }
                if (e.getKeyCode()==KeyEvent.VK_ESCAPE) customerPopup.setVisible(false);
            }
        });
        customerPopupList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { if (e.getClickCount()>=1) commitCustomerSelection(); }
        });
        customerPopupList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ENTER) commitCustomerSelection();
                if (e.getKeyCode()==KeyEvent.VK_ESCAPE) customerPopup.setVisible(false);
            }
        });
        return wrapper;
    }

    private void filterCustomerPopup(String query) {
        customerPopupModel.clear();
        String q = query.toLowerCase();
        for (Customer c : customerList) {
            if (q.isEmpty() || String.valueOf(c.id).contains(q) || c.name.toLowerCase().contains(q))
                customerPopupModel.addElement(c);
        }
        if (customerPopupModel.isEmpty()) { customerPopup.setVisible(false); return; }
        if (!customerPopup.isVisible())
            customerPopup.show(customerSearch, 0, customerSearch.getHeight());
        customerPopup.revalidate(); customerPopup.repaint();
    }

    private void commitCustomerSelection() {
        Customer c = customerPopupList.getSelectedValue();
        if (c == null) return;
        customerPopup.setVisible(false);
        if (c.id == 0) {
            selectedCustomer = null; customerSearch.setText("");
            lblCustomerSelected.setText("(none)"); lblCustomerSelected.setForeground(TEXT_MUT);
            lblCustomerSelected.setBackground(new Color(22,32,58));
        } else {
            selectedCustomer = c; customerSearch.setText(c.name);
            lblCustomerSelected.setText("ID " + c.id + "  " + c.name);
            lblCustomerSelected.setForeground(TEAL);
            lblCustomerSelected.setBackground(new Color(12,44,44));
        }
        setDirty(true); customerSearch.requestFocusInWindow();
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        bar.setBackground(new Color(12, 18, 34));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));
        btnNew     = makeBtn("＋ New",     ACCENT,  new Color(16, 62, 130));
        btnSave    = makeBtn("💾 Save",    ACCENT2, new Color(22, 108, 68));
        btnUpdate  = makeBtn("✏ Update",  GOLD,    new Color(140, 96, 10));
        btnDelete  = makeBtn("🗑 Delete",  DANGER,  new Color(140, 35, 35));
        btnRefresh = makeBtn("↺ Refresh", TEXT_MUT, BG_INPUT);
        btnNew.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveRecord());
        btnUpdate.addActionListener(e -> updateRecord());
        btnDelete.addActionListener(e -> deleteRecord());
        btnRefresh.addActionListener(e -> { loadSaleRefs(); loadCustomers(); loadMaster(); setDirty(false); });
        bar.add(btnNew); bar.add(btnSave); bar.add(btnUpdate);
        bar.add(btnDelete); bar.add(btnRefresh);
        JLabel note = new JLabel("  ↩ Selecting a Sale auto-fills amounts");
        note.setFont(F_SMALL); note.setForeground(TEXT_MUT);
        bar.add(note);
        return bar;
    }

    private JPanel buildShortcutBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        p.setBackground(new Color(7, 10, 20));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(24, 34, 56)));
        for (String s : new String[]{"Ctrl+S  Save","Ctrl+U  Update","Ctrl+D  Delete",
                "Ctrl+N  New","Esc  Refresh"}) {
            JLabel l = new JLabel(s);
            l.setFont(F_SMALL); l.setForeground(new Color(55, 78, 115));
            p.add(l);
        }
        toastLbl = new JLabel("");
        toastLbl.setFont(F_SMALL); toastLbl.setOpaque(true); toastLbl.setVisible(false);
        toastLbl.setBorder(new EmptyBorder(3, 12, 3, 12));
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
        rp.registerKeyboardAction(e -> { loadSaleRefs(); loadCustomers(); loadMaster(); setDirty(false); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Data Operations
    // ═════════════════════════════════════════════════════════════════════
    private void loadMaster() {
        tModel.setRowCount(0); tot = 0;
        try {
            stmt = MdiForm.sconnect.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT sr.return_id, sr.sale_id, sr.return_date, " +
                "COALESCE(c.customer_name,'—') AS cname, " +
                "sr.total_amt, sr.vat_amt, sr.discount_amt, sr.net_amt " +
                "FROM sale_returns sr " +
                "LEFT JOIN customers c ON sr.customer_id = c.id " +
                "ORDER BY sr.return_id DESC");
            while (rs.next()) {
                tModel.addRow(new Object[]{
                    rs.getLong("return_id"),
                    rs.getLong("sale_id"),
                    rs.getString("return_date"),
                    rs.getString("cname"),
                    rs.getDouble("total_amt"),
                    rs.getDouble("vat_amt"),
                    rs.getDouble("discount_amt"),
                    rs.getDouble("net_amt")
                });
                tot++;
            }
            lblCount.setText(tot + " return" + (tot == 1 ? "" : "s"));
        } catch (SQLException ex) {
            showToast("Load error: " + ex.getMessage(), DANGER);
        }
    }

    private void onRowSelected() {
        int vr = masterTable.getSelectedRow();
        if (vr < 0) return;
        long rid = (Long) tModel.getValueAt(masterTable.convertRowIndexToModel(vr), 0);
        try {
            pstmt = MdiForm.sconnect.prepareStatement("SELECT * FROM sale_returns WHERE return_id=?");
            pstmt.setLong(1, rid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { fillForm(rs); setDirty(false); }
        } catch (SQLException ex) {
            showToast("Load error: " + ex.getMessage(), DANGER);
        }
    }

    private void fillForm(ResultSet rs) throws SQLException {
        currentId = rs.getLong("return_id");
        fId.setText(String.valueOf(currentId));
        fReturnDate.setText(nvl(rs.getString("return_date")));
        fTotalAmt.setText(fmt(rs.getDouble("total_amt")));
        fVatAmt.setText(fmt(rs.getDouble("vat_amt")));
        fDiscAmt.setText(fmt(rs.getDouble("discount_amt")));
        fNetAmt.setText(fmt(rs.getDouble("net_amt")));
        fNarration.setText(nvl(rs.getString("narration")));

        // Original sale
        long sid = rs.getLong("sale_id");
        if (rs.wasNull() || sid == 0) {
            selectedSale = null; saleSearch.setText("");
            lblSaleSelected.setText("(none)"); lblSaleSelected.setForeground(TEXT_MUT);
            lblSaleSelected.setBackground(new Color(22,32,58));
        } else {
            SaleRef found = saleRefList.stream().filter(s -> s.id == sid).findFirst().orElse(null);
            if (found != null) {
                selectedSale = found; saleSearch.setText("Sale #" + found.id + " — " + found.display);
                lblSaleSelected.setText("#" + found.id); lblSaleSelected.setForeground(CORAL);
                lblSaleSelected.setBackground(new Color(60,20,10));
            } else {
                selectedSale = new SaleRef(sid,"#"+sid,0,"",0,0,0,0);
                saleSearch.setText("Sale #" + sid);
                lblSaleSelected.setText("#" + sid + " (?)");
                lblSaleSelected.setForeground(WARNING); lblSaleSelected.setBackground(new Color(50,40,10));
            }
        }

        // Customer
        long cid = rs.getLong("customer_id");
        if (rs.wasNull() || cid == 0) {
            selectedCustomer = null; customerSearch.setText("");
            lblCustomerSelected.setText("(none)"); lblCustomerSelected.setForeground(TEXT_MUT);
            lblCustomerSelected.setBackground(new Color(22,32,58));
        } else {
            Customer found = customerList.stream().filter(c -> c.id == cid).findFirst().orElse(null);
            if (found != null) {
                selectedCustomer = found; customerSearch.setText(found.name);
                lblCustomerSelected.setText("ID " + found.id + "  " + found.name);
                lblCustomerSelected.setForeground(TEAL); lblCustomerSelected.setBackground(new Color(12,44,44));
            } else {
                selectedCustomer = new Customer(cid, "ID " + cid);
                customerSearch.setText("ID " + cid);
                lblCustomerSelected.setText("ID " + cid + " (?)");
                lblCustomerSelected.setForeground(WARNING); lblCustomerSelected.setBackground(new Color(50,40,10));
            }
        }

        Timestamp ca = rs.getTimestamp("created_at");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        fCreatedAt.setText(ca != null ? ca.toLocalDateTime().format(dtf) : "");

        double net = rs.getDouble("net_amt");
        if (net > 0) {
            lblNetBadge.setText("Return Net ₹" + String.format("%.2f", net));
            lblNetBadge.setForeground(CORAL); lblNetBadge.setBackground(new Color(60,20,10));
        } else { lblNetBadge.setText(""); }
    }

    private void saveRecord() {
        if (fReturnDate.getText().trim().isEmpty()) { showToast("Return Date is required!", WARNING); return; }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                "INSERT INTO sale_returns(sale_id,customer_id,return_date," +
                "total_amt,vat_amt,discount_amt,net_amt,narration) " +
                "VALUES(?,?,?,?,?,?,?,?) RETURNING return_id INTO ?");
            bindAll(pstmt);
            ((oracle.jdbc.OraclePreparedStatement) pstmt).registerReturnParameter(9, Types.NUMERIC);
            pstmt.executeUpdate();
            ResultSet keys = ((oracle.jdbc.OraclePreparedStatement) pstmt).getReturnResultSet();
            if (keys != null && keys.next()) {
                currentId = keys.getLong(1); fId.setText(String.valueOf(currentId));
            }
            showToast("✔ Return saved — ID: " + currentId, ACCENT2);
            loadMaster(); setDirty(false);
        } catch (SQLException ex) {
            showToast("Save error: " + ex.getMessage(), DANGER);
        }
    }

    private void updateRecord() {
        if (currentId < 0) { showToast("Select a return first!", WARNING); return; }
        if (fReturnDate.getText().trim().isEmpty()) { showToast("Return Date is required!", WARNING); return; }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                "UPDATE sale_returns SET sale_id=?,customer_id=?,return_date=?," +
                "total_amt=?,vat_amt=?,discount_amt=?,net_amt=?,narration=? WHERE return_id=?");
            bindAll(pstmt);
            pstmt.setLong(9, currentId);
            int r = pstmt.executeUpdate();
            if (r > 0) { loadMaster(); setDirty(false); showToast("✔ Return updated.", ACCENT2); }
            else showToast("No return matched ID " + currentId, WARNING);
        } catch (SQLException ex) {
            showToast("Update error: " + ex.getMessage(), DANGER);
        }
    }

    private void deleteRecord() {
        if (currentId < 0) { showToast("Select a return to delete!", WARNING); return; }
        int ans = JOptionPane.showConfirmDialog(this,
            "Permanently delete Return ID " + currentId + "?\nThis cannot be undone.",
            "Delete Return", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;
        try {
            pstmt = MdiForm.sconnect.prepareStatement("DELETE FROM sale_returns WHERE return_id=?");
            pstmt.setLong(1, currentId);
            pstmt.executeUpdate();
            clearForm(); loadMaster();
            showToast("🗑 Return deleted.", DANGER);
        } catch (SQLException ex) {
            showToast("Delete error: " + ex.getMessage(), DANGER);
        }
    }

    private void bindAll(PreparedStatement ps) throws SQLException {
        if (selectedSale != null && selectedSale.id > 0) ps.setLong(1, selectedSale.id);
        else ps.setNull(1, Types.NUMERIC);
        if (selectedCustomer != null && selectedCustomer.id > 0) ps.setLong(2, selectedCustomer.id);
        else ps.setNull(2, Types.NUMERIC);
        ps.setString(3, fReturnDate.getText().trim());
        ps.setDouble(4, parseDbl(fTotalAmt.getText()));
        ps.setDouble(5, parseDbl(fVatAmt.getText()));
        ps.setDouble(6, parseDbl(fDiscAmt.getText()));
        ps.setDouble(7, parseDbl(fNetAmt.getText()));
        setStrOrNull(ps, 8, fNarration.getText().trim());
    }

    private void clearForm() {
        currentId = -1;
        fId.setText("(auto)");
        fReturnDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        fTotalAmt.setText(""); fVatAmt.setText(""); fDiscAmt.setText(""); fNetAmt.setText("");
        fNarration.setText(""); fCreatedAt.setText("");
        lblNetBadge.setText("");
        // Reset sale
        selectedSale = null; saleSearch.setText("");
        lblSaleSelected.setText("(none)"); lblSaleSelected.setForeground(TEXT_MUT);
        lblSaleSelected.setBackground(new Color(22,32,58));
        // Reset customer
        selectedCustomer = null; customerSearch.setText("");
        lblCustomerSelected.setText("(none)"); lblCustomerSelected.setForeground(TEXT_MUT);
        lblCustomerSelected.setBackground(new Color(22,32,58));
        masterTable.clearSelection(); setDirty(false);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Filter
    // ═════════════════════════════════════════════════════════════════════
    private void applyFilter() {
        List<RowFilter<DefaultTableModel,Object>> filters = new ArrayList<>();
        String gs = txtSearch.getText().trim();
        if (!gs.isEmpty()) {
            try {
                filters.add(RowFilter.orFilter(Arrays.asList(
                    RowFilter.regexFilter("(?i)" + gs, 3),
                    RowFilter.regexFilter("(?i)" + gs, 2))));
            } catch (Exception ignored) {}
        }
        addColFilter(filters, txtFDate.getText(),     2);
        addColFilter(filters, txtFCustomer.getText(), 3);
        addColFilter(filters, txtFSaleId.getText(),   1);
        try {
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        } catch (Exception ignored) {}
        lblCount.setText(masterTable.getRowCount() + " / " + tot + " returns");
    }

    private void addColFilter(List<RowFilter<DefaultTableModel,Object>> list, String val, int col) {
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
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(4, 8, 4, 8)));
        JLabel l = new JLabel(text.toUpperCase()); l.setFont(F_SEC); l.setForeground(CORAL);
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

    private JPanel fullWidthLabeled(String lbl, Component field) {
        JPanel p = new JPanel(new BorderLayout(0, 3)); p.setBackground(BG_CARD);
        p.setAlignmentX(0f); p.setBorder(new EmptyBorder(6, 0, 4, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 32));
        JLabel l = new JLabel(lbl); l.setFont(F_LABEL); l.setForeground(TEXT_MUT);
        p.add(l, BorderLayout.NORTH); p.add(field, BorderLayout.CENTER); return p;
    }

    private JTextField inputField() {
        JTextField tf = new JTextField();
        tf.setFont(F_INPUT); tf.setBackground(BG_INPUT); tf.setForeground(TEXT_PRI);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL), new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    private JTextField roField() {
        JTextField tf = inputField(); tf.setEditable(false);
        tf.setBackground(new Color(14, 20, 38)); tf.setForeground(CORAL); return tf;
    }

    private void styleTextArea(JTextArea ta) {
        ta.setFont(F_INPUT); ta.setBackground(BG_INPUT); ta.setForeground(TEXT_PRI);
        ta.setCaretColor(ACCENT); ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setBorder(new EmptyBorder(6, 8, 6, 8));
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
                protected void configureScrollBarColors() {
                    thumbColor = new Color(46, 66, 108); trackColor = BG_PANEL;
                }
                protected JButton createDecreaseButton(int o) { return zb(); }
                protected JButton createIncreaseButton(int o) { return zb(); }
                private JButton zb() { JButton b=new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
            });
        }
    }

    private void setDirty(boolean d) {
        dirty = d; SwingUtilities.invokeLater(() -> lblDirty.setVisible(d));
    }

    private void showToast(String msg, Color bg) {
        if (toastLbl == null) { System.err.println("[Toast] " + msg); return; }
        toastLbl.setText(msg); toastLbl.setBackground(bg); toastLbl.setForeground(Color.WHITE);
        toastLbl.setVisible(true);
        if (toastTimer != null && toastTimer.isRunning()) toastTimer.stop();
        toastTimer = new javax.swing.Timer(3400, e -> toastLbl.setVisible(false));
        toastTimer.setRepeats(false); toastTimer.start();
    }

    private DocumentListener dl(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) {}
        };
    }

    private double parseDbl(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; } }
    private String fmt(double d)      { return d == 0 ? "" : String.format("%.2f", d); }
    private String nvl(String s)      { return s == null ? "" : s; }
    private String escHtml(String s)  { return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;"); }
    private void setStrOrNull(PreparedStatement ps, int idx, String val) throws SQLException {
        if (val == null || val.isEmpty()) ps.setNull(idx, Types.VARCHAR); else ps.setString(idx, val);
    }
}