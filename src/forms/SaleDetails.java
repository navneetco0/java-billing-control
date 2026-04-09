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
 * SaleDetails — CRUA (Create, Read, Update, Archive/Delete) UI
 *
 * Table: sale_details
 *   sale_id, sale_date, customer_id, product_id, qty, rate, total_amt,
 *   vat_pct, vat_amt, discount_pct, discount_amt, net_amt, narration, created_at
 */
public class SaleDetails extends JInternalFrame {

    // ── Lookup models ──────────────────────────────────────────────────────
    private static class Customer {
        long id; String name;
        Customer(long id, String name) { this.id=id; this.name=name; }
        public String toString() { return id + " — " + name; }
    }

    private static class Product {
        long id; String name; double price;
        Product(long id, String name, double price) { this.id=id; this.name=name; this.price=price; }
        public String toString() { return id + " — " + name; }
    }

    private final List<Customer> customerList = new ArrayList<>();
    private final List<Product>  productList  = new ArrayList<>();

    // ── DB ─────────────────────────────────────────────────────────────────
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot = 0;
    private long currentId = -1;
    private boolean dirty = false;
    private boolean recalcLock = false; // prevent re-entrant recalc

    // ── Master table ───────────────────────────────────────────────────────
    private DefaultTableModel tModel;
    private JTable masterTable;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel lblCount;

    // ── Filters ────────────────────────────────────────────────────────────
    private JTextField txtSearch;
    private JTextField txtFDate, txtFCustomer, txtFProduct;

    // ── Form fields ────────────────────────────────────────────────────────
    private JTextField fId, fSaleDate, fCreatedAt;
    private JTextField fQty, fRate, fTotalAmt;
    private JTextField fVatPct, fVatAmt;
    private JTextField fDiscPct, fDiscAmt, fNetAmt;
    private JTextArea  fNarration;
    private JLabel lblDirty, lblNetBadge;

    // ── Customer searchable combo ──────────────────────────────────────────
    private JTextField     customerSearch;
    private JPopupMenu     customerPopup;
    private JList<Customer> customerPopupList;
    private DefaultListModel<Customer> customerPopupModel;
    private Customer selectedCustomer = null;
    private JLabel   lblCustomerSelected;

    // ── Product searchable combo ───────────────────────────────────────────
    private JTextField    productSearch;
    private JPopupMenu    productPopup;
    private JList<Product> productPopupList;
    private DefaultListModel<Product> productPopupModel;
    private Product selectedProduct = null;
    private JLabel  lblProductSelected;

    // ── Buttons ────────────────────────────────────────────────────────────
    private JButton btnNew, btnSave, btnUpdate, btnDelete, btnRefresh;

    // ── Toast ──────────────────────────────────────────────────────────────
    private JLabel toastLbl;
    private javax.swing.Timer toastTimer;

    // ──────────────────────────────────────────────────────────────────────
    public SaleDetails() {
        super("Sale Details", true, true, true, true);
        setSize(1380, 780);
        setLocation(40, 40);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ensureTable();
        loadCustomers();
        loadProducts();
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
                    "CREATE TABLE sale_details (" +
                    "  sale_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "  sale_date     VARCHAR2(20)," +
                    "  customer_id   NUMBER," +
                    "  product_id    NUMBER," +
                    "  qty           NUMBER(10)   DEFAULT 0," +
                    "  rate          NUMBER(10,2) DEFAULT 0," +
                    "  total_amt     NUMBER(12,2) DEFAULT 0," +
                    "  vat_pct       NUMBER(5,2)  DEFAULT 0," +
                    "  vat_amt       NUMBER(10,2) DEFAULT 0," +
                    "  discount_pct  NUMBER(5,2)  DEFAULT 0," +
                    "  discount_amt  NUMBER(10,2) DEFAULT 0," +
                    "  net_amt       NUMBER(12,2) DEFAULT 0," +
                    "  narration     VARCHAR2(500)," +
                    "  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id)" +
                    "    REFERENCES customers(id) ON DELETE SET NULL," +
                    "  CONSTRAINT fk_sale_product FOREIGN KEY (product_id)" +
                    "    REFERENCES products(id) ON DELETE SET NULL" +
                    ")");
            } catch (SQLException ignored) {}
        } catch (SQLException ex) {
            showToast("DB Init Error: " + ex.getMessage(), DANGER);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Load lookup lists
    // ═════════════════════════════════════════════════════════════════════
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

    private void loadProducts() {
        productList.clear();
        productList.add(new Product(0, "(none)", 0));
        try {
            stmt = MdiForm.sconnect.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT id, name, price FROM products WHERE is_active=1 ORDER BY name");
            while (rs.next())
                productList.add(new Product(rs.getLong("id"), rs.getString("name"), rs.getDouble("price")));
        } catch (SQLException ex) {
            showToast("Product load error: " + ex.getMessage(), DANGER);
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
                g2.setPaint(new GradientPaint(0, 0, new Color(14, 38, 28), getWidth(), 0, new Color(10, 14, 26)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(GOLD);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        hdr.setPreferredSize(new Dimension(0, 54));
        hdr.setBorder(new EmptyBorder(0, 22, 0, 22));
        JLabel title = new JLabel("🧾  Sale Details");
        title.setFont(F_HEAD); title.setForeground(TEXT_PRI);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        lblCount = styledBadge("0 sales", TEXT_MUT, BG_INPUT);
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

        txtSearch = styledField("🔍  Search by Customer or Product…");
        txtSearch.setPreferredSize(new Dimension(0, 34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));

        JPanel r1 = new JPanel(new GridLayout(1, 3, 5, 0));
        r1.setBackground(BG_PANEL); r1.setBorder(new EmptyBorder(6, 0, 4, 0));
        txtFDate     = miniField("Date (dd-MM-yyyy)…");
        txtFCustomer = miniField("Customer…");
        txtFProduct  = miniField("Product…");
        r1.add(txtFDate); r1.add(txtFCustomer); r1.add(txtFProduct);

        for (JTextField tf : new JTextField[]{txtFDate, txtFCustomer, txtFProduct})
            tf.getDocument().addDocumentListener(dl(this::applyFilter));

        outer.add(txtSearch, BorderLayout.NORTH);
        outer.add(r1, BorderLayout.CENTER);
        return outer;
    }

    private JScrollPane buildMasterTable() {
        // COL: 0=ID 1=Date 2=Customer 3=Product 4=Qty 5=Rate 6=Total 7=VAT% 8=Disc% 9=Net
        String[] cols = {"ID","Date","Customer","Product","Qty","Rate (₹)","Total (₹)","VAT%","Disc%","Net (₹)"};
        tModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 0 || c == 4) return Long.class;
                if (c == 5 || c == 6 || c == 9) return Double.class;
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
                    if (mc == 2)  comp.setForeground(TEAL);
                    if (mc == 3)  comp.setForeground(PURPLE);
                    if (mc == 9)  comp.setForeground(GOLD);
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

        int[] widths = {50, 90, 130, 130, 48, 78, 80, 52, 52, 90};
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
        JLabel title = new JLabel("Sale Record");
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
        form.add(sectionHeader("Sale Identity"));
        JPanel id1 = gridPanel(2);
        fId       = roField();
        fSaleDate = inputField();
        fSaleDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        id1.add(labeled("Sale ID (auto)", fId));
        id1.add(labeled("Sale Date (dd-MM-yyyy) *", fSaleDate));
        form.add(id1);

        // ── Customer ──────────────────────────────────────────────────
        form.add(sectionHeader("Customer"));
        form.add(buildSearchableRow("Customer  (type to search)",
                "customerSearch", true));

        // ── Product ───────────────────────────────────────────────────
        form.add(sectionHeader("Product"));
        form.add(buildSearchableRow("Product  (type to search)",
                "productSearch", false));

        // ── Pricing ───────────────────────────────────────────────────
        form.add(sectionHeader("Quantity & Rate"));
        JPanel qrPnl = gridPanel(3);
        fQty      = inputField();
        fRate     = inputField();
        fTotalAmt = roField();
        qrPnl.add(labeled("Qty *",        fQty));
        qrPnl.add(labeled("Rate (₹) *",   fRate));
        qrPnl.add(labeled("Total (₹)",    fTotalAmt));
        form.add(qrPnl);

        // ── VAT ───────────────────────────────────────────────────────
        form.add(sectionHeader("VAT / Tax"));
        JPanel vatPnl = gridPanel(2);
        fVatPct = inputField();
        fVatAmt = roField();
        vatPnl.add(labeled("VAT %",    fVatPct));
        vatPnl.add(labeled("VAT Amt (₹)", fVatAmt));
        form.add(vatPnl);

        // ── Discount ──────────────────────────────────────────────────
        form.add(sectionHeader("Discount"));
        JPanel discPnl = gridPanel(3);
        fDiscPct = inputField();
        fDiscAmt = roField();
        fNetAmt  = roField();
        discPnl.add(labeled("Discount %",      fDiscPct));
        discPnl.add(labeled("Discount Amt (₹)", fDiscAmt));
        discPnl.add(labeled("Net Amount (₹)",   fNetAmt));
        form.add(discPnl);

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

        // Recalculation listeners
        DocumentListener recalcDl = dl(this::recalc);
        fQty.getDocument().addDocumentListener(recalcDl);
        fRate.getDocument().addDocumentListener(recalcDl);
        fVatPct.getDocument().addDocumentListener(recalcDl);
        fDiscPct.getDocument().addDocumentListener(recalcDl);

        DocumentListener dirtyDl = dl(() -> setDirty(true));
        for (JTextField tf : new JTextField[]{fSaleDate, fQty, fRate, fVatPct, fDiscPct})
            tf.getDocument().addDocumentListener(dirtyDl);
        fNarration.getDocument().addDocumentListener(dirtyDl);

        JScrollPane scroll = new JScrollPane(form,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null); scroll.getViewport().setBackground(BG_CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Searchable dropdown row builder (reusable for Customer and Product)
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildSearchableRow(String labelText, String fieldKey, boolean isCustomer) {
        JPanel wrapper = new JPanel(new BorderLayout(8, 0));
        wrapper.setBackground(BG_CARD);
        wrapper.setAlignmentX(0f);
        wrapper.setBorder(new EmptyBorder(6, 0, 8, 0));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        JPanel leftPart = new JPanel(new BorderLayout(0, 3));
        leftPart.setBackground(BG_CARD);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(F_LABEL); lbl.setForeground(TEXT_MUT);

        JTextField searchField = new JTextField();
        searchField.setFont(F_INPUT); searchField.setBackground(BG_INPUT);
        searchField.setForeground(TEXT_PRI); searchField.setCaretColor(ACCENT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 176, 240, 120)),
                new EmptyBorder(5, 8, 5, 8)));
        searchField.setToolTipText("Type to filter…");

        leftPart.add(lbl, BorderLayout.NORTH);
        leftPart.add(searchField, BorderLayout.CENTER);

        JPanel rightPart = new JPanel(new BorderLayout(0, 3));
        rightPart.setBackground(BG_CARD);
        rightPart.setPreferredSize(new Dimension(210, 0));
        JLabel selLbl = new JLabel("Selected:");
        selLbl.setFont(F_LABEL); selLbl.setForeground(TEXT_MUT);
        JPanel selRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selRow.setBackground(BG_CARD);

        JLabel selectedLbl = styledBadge("(none)", TEXT_MUT, new Color(22, 32, 58));

        JButton btnClear = new JButton("✕");
        btnClear.setFont(F_SMALL); btnClear.setForeground(TEXT_MUT);
        btnClear.setBackground(new Color(35, 20, 20));
        btnClear.setBorder(new EmptyBorder(3, 7, 3, 7));
        btnClear.setFocusPainted(false);
        btnClear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        selRow.add(selectedLbl); selRow.add(btnClear);
        rightPart.add(selLbl, BorderLayout.NORTH);
        rightPart.add(selRow, BorderLayout.CENTER);

        wrapper.add(leftPart, BorderLayout.CENTER);
        wrapper.add(rightPart, BorderLayout.EAST);

        // ── Popup list ────────────────────────────────────────────────
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        popup.setBackground(new Color(20, 30, 54));

        if (isCustomer) {
            DefaultListModel<Customer> model = new DefaultListModel<>();
            JList<Customer> list = new JList<>(model);
            list.setFont(F_INPUT); list.setBackground(new Color(20,30,54));
            list.setForeground(TEXT_PRI); list.setSelectionBackground(BG_ROW_SEL);
            list.setSelectionForeground(Color.WHITE); list.setFixedCellHeight(28);
            list.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList<?> l, Object v, int idx, boolean s, boolean f) {
                    JLabel c = (JLabel) super.getListCellRendererComponent(l,v,idx,s,f);
                    Customer cu = (Customer) v;
                    c.setText("<html><font color='#32c8c8'>" + cu.id + "</font>  " + escHtml(cu.name) + "</html>");
                    c.setBorder(new EmptyBorder(4,12,4,12));
                    if (!s) c.setBackground(idx%2==0 ? new Color(20,30,54) : new Color(24,36,62));
                    return c;
                }
            });
            JScrollPane ps = new JScrollPane(list,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            ps.setBorder(null); ps.setPreferredSize(new Dimension(320, 160));
            styleScrollBar(ps); popup.add(ps);

            customerSearch = searchField;
            customerPopup = popup;
            customerPopupList = list;
            customerPopupModel = model;
            lblCustomerSelected = selectedLbl;

            // wire events
            searchField.getDocument().addDocumentListener(dl(() -> {
                filterCustomerPopup(searchField.getText().trim());
                setDirty(true);
            }));
            searchField.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { filterCustomerPopup(searchField.getText().trim()); }
            });
            searchField.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { filterCustomerPopup(searchField.getText().trim()); }
            });
            searchField.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (!popup.isVisible()) return;
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        list.requestFocusInWindow();
                        if (list.getModel().getSize() > 0) list.setSelectedIndex(0);
                        e.consume();
                    }
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) popup.setVisible(false);
                }
            });
            list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { if (e.getClickCount()>=1) commitCustomerSelection(); }
            });
            list.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode()==KeyEvent.VK_ENTER) commitCustomerSelection();
                    if (e.getKeyCode()==KeyEvent.VK_ESCAPE) popup.setVisible(false);
                }
            });
            btnClear.addActionListener(e -> {
                selectedCustomer = null; searchField.setText("");
                selectedLbl.setText("(none)"); selectedLbl.setForeground(TEXT_MUT);
                selectedLbl.setBackground(new Color(22,32,58)); setDirty(true);
            });
        } else {
            // Product
            DefaultListModel<Product> model = new DefaultListModel<>();
            JList<Product> list = new JList<>(model);
            list.setFont(F_INPUT); list.setBackground(new Color(20,30,54));
            list.setForeground(TEXT_PRI); list.setSelectionBackground(BG_ROW_SEL);
            list.setSelectionForeground(Color.WHITE); list.setFixedCellHeight(28);
            list.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList<?> l, Object v, int idx, boolean s, boolean f) {
                    JLabel c = (JLabel) super.getListCellRendererComponent(l,v,idx,s,f);
                    Product pr = (Product) v;
                    c.setText("<html><font color='#a064ff'>" + pr.id + "</font>  " +
                            escHtml(pr.name) + "  <font color='#ffc332'>₹"+String.format("%.2f",pr.price)+"</font></html>");
                    c.setBorder(new EmptyBorder(4,12,4,12));
                    if (!s) c.setBackground(idx%2==0 ? new Color(20,30,54) : new Color(24,36,62));
                    return c;
                }
            });
            JScrollPane ps = new JScrollPane(list,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            ps.setBorder(null); ps.setPreferredSize(new Dimension(360, 160));
            styleScrollBar(ps); popup.add(ps);

            productSearch = searchField;
            productPopup = popup;
            productPopupList = list;
            productPopupModel = model;
            lblProductSelected = selectedLbl;

            searchField.getDocument().addDocumentListener(dl(() -> {
                filterProductPopup(searchField.getText().trim());
                setDirty(true);
            }));
            searchField.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { filterProductPopup(searchField.getText().trim()); }
            });
            searchField.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { filterProductPopup(searchField.getText().trim()); }
            });
            searchField.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (!popup.isVisible()) return;
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        list.requestFocusInWindow();
                        if (list.getModel().getSize() > 0) list.setSelectedIndex(0);
                        e.consume();
                    }
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) popup.setVisible(false);
                }
            });
            list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { if (e.getClickCount()>=1) commitProductSelection(); }
            });
            list.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode()==KeyEvent.VK_ENTER) commitProductSelection();
                    if (e.getKeyCode()==KeyEvent.VK_ESCAPE) popup.setVisible(false);
                }
            });
            btnClear.addActionListener(e -> {
                selectedProduct = null; searchField.setText("");
                selectedLbl.setText("(none)"); selectedLbl.setForeground(TEXT_MUT);
                selectedLbl.setBackground(new Color(22,32,58)); setDirty(true);
            });
        }

        return wrapper;
    }

    // ── Supplier popup filter/commit ───────────────────────────────────────
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
            lblCustomerSelected.setBackground(new Color(12, 44, 44));
        }
        setDirty(true); customerSearch.requestFocusInWindow();
    }

    private void filterProductPopup(String query) {
        productPopupModel.clear();
        String q = query.toLowerCase();
        for (Product pr : productList) {
            if (q.isEmpty() || String.valueOf(pr.id).contains(q) || pr.name.toLowerCase().contains(q))
                productPopupModel.addElement(pr);
        }
        if (productPopupModel.isEmpty()) { productPopup.setVisible(false); return; }
        if (!productPopup.isVisible())
            productPopup.show(productSearch, 0, productSearch.getHeight());
        productPopup.revalidate(); productPopup.repaint();
    }

    private void commitProductSelection() {
        Product pr = productPopupList.getSelectedValue();
        if (pr == null) return;
        productPopup.setVisible(false);
        if (pr.id == 0) {
            selectedProduct = null; productSearch.setText("");
            lblProductSelected.setText("(none)"); lblProductSelected.setForeground(TEXT_MUT);
            lblProductSelected.setBackground(new Color(22,32,58));
        } else {
            selectedProduct = pr; productSearch.setText(pr.name);
            lblProductSelected.setText("ID " + pr.id + "  " + pr.name);
            lblProductSelected.setForeground(PURPLE);
            lblProductSelected.setBackground(new Color(35, 20, 60));
            // Auto-fill rate from product price
            if (fRate.getText().trim().isEmpty() || parseDbl(fRate.getText()) == 0)
                fRate.setText(String.format("%.2f", pr.price));
        }
        setDirty(true); productSearch.requestFocusInWindow();
    }

    // ── Auto-recalculation ─────────────────────────────────────────────────
    private void recalc() {
        if (recalcLock) return;
        recalcLock = true;
        try {
            double qty      = parseDbl(fQty.getText());
            double rate     = parseDbl(fRate.getText());
            double total    = qty * rate;
            double vatPct   = parseDbl(fVatPct.getText());
            double vatAmt   = total * vatPct / 100.0;
            double discPct  = parseDbl(fDiscPct.getText());
            double discAmt  = (total + vatAmt) * discPct / 100.0;
            double net      = total + vatAmt - discAmt;
            fTotalAmt.setText(String.format("%.2f", total));
            fVatAmt.setText(String.format("%.2f", vatAmt));
            fDiscAmt.setText(String.format("%.2f", discAmt));
            fNetAmt.setText(String.format("%.2f", net));
            if (net > 0) {
                lblNetBadge.setText("Net ₹" + String.format("%.2f", net));
                lblNetBadge.setForeground(GOLD);
                lblNetBadge.setBackground(new Color(60, 46, 10));
            } else {
                lblNetBadge.setText("");
            }
        } finally {
            recalcLock = false;
        }
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
        btnRefresh.addActionListener(e -> { loadMaster(); setDirty(false); });
        bar.add(btnNew); bar.add(btnSave); bar.add(btnUpdate);
        bar.add(btnDelete); bar.add(btnRefresh);
        JLabel note = new JLabel("  ✏ VAT & Discount auto-calculated");
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
        rp.registerKeyboardAction(e -> { loadMaster(); setDirty(false); },
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
                "SELECT sd.sale_id, sd.sale_date, " +
                "COALESCE(c.customer_name,'—') AS cname, " +
                "COALESCE(p.name,'—') AS pname, " +
                "sd.qty, sd.rate, sd.total_amt, sd.vat_pct, sd.discount_pct, sd.net_amt " +
                "FROM sale_details sd " +
                "LEFT JOIN customers c ON sd.customer_id = c.id " +
                "LEFT JOIN products p  ON sd.product_id  = p.id " +
                "ORDER BY sd.sale_id DESC");
            while (rs.next()) {
                tModel.addRow(new Object[]{
                    rs.getLong("sale_id"),
                    rs.getString("sale_date"),
                    rs.getString("cname"),
                    rs.getString("pname"),
                    rs.getLong("qty"),
                    rs.getDouble("rate"),
                    rs.getDouble("total_amt"),
                    rs.getDouble("vat_pct"),
                    rs.getDouble("discount_pct"),
                    rs.getDouble("net_amt")
                });
                tot++;
            }
            lblCount.setText(tot + " sale" + (tot == 1 ? "" : "s"));
        } catch (SQLException ex) {
            showToast("Load error: " + ex.getMessage(), DANGER);
        }
    }

    private void onRowSelected() {
        int vr = masterTable.getSelectedRow();
        if (vr < 0) return;
        long pid = (Long) tModel.getValueAt(masterTable.convertRowIndexToModel(vr), 0);
        try {
            pstmt = MdiForm.sconnect.prepareStatement("SELECT * FROM sale_details WHERE sale_id=?");
            pstmt.setLong(1, pid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { fillForm(rs); setDirty(false); }
        } catch (SQLException ex) {
            showToast("Load error: " + ex.getMessage(), DANGER);
        }
    }

    private void fillForm(ResultSet rs) throws SQLException {
        recalcLock = true;
        try {
            currentId = rs.getLong("sale_id");
            fId.setText(String.valueOf(currentId));
            fSaleDate.setText(nvl(rs.getString("sale_date")));
            fQty.setText(String.valueOf(rs.getLong("qty")));
            fRate.setText(fmt(rs.getDouble("rate")));
            fTotalAmt.setText(fmt(rs.getDouble("total_amt")));
            fVatPct.setText(fmt(rs.getDouble("vat_pct")));
            fVatAmt.setText(fmt(rs.getDouble("vat_amt")));
            fDiscPct.setText(fmt(rs.getDouble("discount_pct")));
            fDiscAmt.setText(fmt(rs.getDouble("discount_amt")));
            fNetAmt.setText(fmt(rs.getDouble("net_amt")));
            fNarration.setText(nvl(rs.getString("narration")));

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
                    lblCustomerSelected.setForeground(TEAL);
                    lblCustomerSelected.setBackground(new Color(12, 44, 44));
                } else {
                    selectedCustomer = new Customer(cid, "ID " + cid);
                    customerSearch.setText("ID " + cid);
                    lblCustomerSelected.setText("ID " + cid + " (?)");
                    lblCustomerSelected.setForeground(WARNING);
                    lblCustomerSelected.setBackground(new Color(50,40,10));
                }
            }

            // Product
            long pid = rs.getLong("product_id");
            if (rs.wasNull() || pid == 0) {
                selectedProduct = null; productSearch.setText("");
                lblProductSelected.setText("(none)"); lblProductSelected.setForeground(TEXT_MUT);
                lblProductSelected.setBackground(new Color(22,32,58));
            } else {
                Product found = productList.stream().filter(p -> p.id == pid).findFirst().orElse(null);
                if (found != null) {
                    selectedProduct = found; productSearch.setText(found.name);
                    lblProductSelected.setText("ID " + found.id + "  " + found.name);
                    lblProductSelected.setForeground(PURPLE);
                    lblProductSelected.setBackground(new Color(35, 20, 60));
                } else {
                    selectedProduct = new Product(pid, "ID " + pid, 0);
                    productSearch.setText("ID " + pid);
                    lblProductSelected.setText("ID " + pid + " (?)");
                    lblProductSelected.setForeground(WARNING);
                    lblProductSelected.setBackground(new Color(50,40,10));
                }
            }

            Timestamp ca = rs.getTimestamp("created_at");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            fCreatedAt.setText(ca != null ? ca.toLocalDateTime().format(dtf) : "");

            double net = rs.getDouble("net_amt");
            lblNetBadge.setText(net > 0 ? "Net ₹" + String.format("%.2f", net) : "");
        } finally {
            recalcLock = false;
        }
    }

    private void saveRecord() {
        if (fSaleDate.getText().trim().isEmpty()) { showToast("Sale Date is required!", WARNING); return; }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                "INSERT INTO sale_details(sale_date,customer_id,product_id,qty,rate,total_amt," +
                "vat_pct,vat_amt,discount_pct,discount_amt,net_amt,narration) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?) RETURNING sale_id INTO ?");
            bindAll(pstmt);
            ((oracle.jdbc.OraclePreparedStatement) pstmt).registerReturnParameter(13, Types.NUMERIC);
            pstmt.executeUpdate();
            ResultSet keys = ((oracle.jdbc.OraclePreparedStatement) pstmt).getReturnResultSet();
            if (keys != null && keys.next()) {
                currentId = keys.getLong(1); fId.setText(String.valueOf(currentId));
            }
            showToast("✔ Sale saved — ID: " + currentId, ACCENT2);
            loadMaster(); setDirty(false);
        } catch (SQLException ex) {
            showToast("Save error: " + ex.getMessage(), DANGER);
        }
    }

    private void updateRecord() {
        if (currentId < 0) { showToast("Select a sale first!", WARNING); return; }
        if (fSaleDate.getText().trim().isEmpty()) { showToast("Sale Date is required!", WARNING); return; }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                "UPDATE sale_details SET sale_date=?,customer_id=?,product_id=?,qty=?,rate=?," +
                "total_amt=?,vat_pct=?,vat_amt=?,discount_pct=?,discount_amt=?,net_amt=?,narration=? " +
                "WHERE sale_id=?");
            bindAll(pstmt);
            pstmt.setLong(13, currentId);
            int r = pstmt.executeUpdate();
            if (r > 0) { loadMaster(); setDirty(false); showToast("✔ Sale updated.", ACCENT2); }
            else showToast("No sale matched ID " + currentId, WARNING);
        } catch (SQLException ex) {
            showToast("Update error: " + ex.getMessage(), DANGER);
        }
    }

    private void deleteRecord() {
        if (currentId < 0) { showToast("Select a sale to delete!", WARNING); return; }
        int ans = JOptionPane.showConfirmDialog(this,
            "Permanently delete Sale ID " + currentId + "?\nThis cannot be undone.",
            "Delete Sale", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;
        try {
            pstmt = MdiForm.sconnect.prepareStatement("DELETE FROM sale_details WHERE sale_id=?");
            pstmt.setLong(1, currentId);
            pstmt.executeUpdate();
            clearForm(); loadMaster();
            showToast("🗑 Sale deleted.", DANGER);
        } catch (SQLException ex) {
            showToast("Delete error: " + ex.getMessage(), DANGER);
        }
    }

    private void bindAll(PreparedStatement ps) throws SQLException {
        ps.setString(1, fSaleDate.getText().trim());
        if (selectedCustomer != null && selectedCustomer.id > 0) ps.setLong(2, selectedCustomer.id);
        else ps.setNull(2, Types.NUMERIC);
        if (selectedProduct != null && selectedProduct.id > 0) ps.setLong(3, selectedProduct.id);
        else ps.setNull(3, Types.NUMERIC);
        ps.setLong(4,   parseLong(fQty.getText()));
        ps.setDouble(5, parseDbl(fRate.getText()));
        ps.setDouble(6, parseDbl(fTotalAmt.getText()));
        ps.setDouble(7, parseDbl(fVatPct.getText()));
        ps.setDouble(8, parseDbl(fVatAmt.getText()));
        ps.setDouble(9, parseDbl(fDiscPct.getText()));
        ps.setDouble(10, parseDbl(fDiscAmt.getText()));
        ps.setDouble(11, parseDbl(fNetAmt.getText()));
        setStrOrNull(ps, 12, fNarration.getText().trim());
    }

    private void clearForm() {
        currentId = -1;
        fId.setText("(auto)");
        fSaleDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        fQty.setText(""); fRate.setText(""); fTotalAmt.setText("");
        fVatPct.setText(""); fVatAmt.setText("");
        fDiscPct.setText(""); fDiscAmt.setText(""); fNetAmt.setText("");
        fNarration.setText(""); fCreatedAt.setText("");
        lblNetBadge.setText("");
        // Reset customer
        selectedCustomer = null; customerSearch.setText("");
        lblCustomerSelected.setText("(none)"); lblCustomerSelected.setForeground(TEXT_MUT);
        lblCustomerSelected.setBackground(new Color(22,32,58));
        // Reset product
        selectedProduct = null; productSearch.setText("");
        lblProductSelected.setText("(none)"); lblProductSelected.setForeground(TEXT_MUT);
        lblProductSelected.setBackground(new Color(22,32,58));
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
                    RowFilter.regexFilter("(?i)" + gs, 2),
                    RowFilter.regexFilter("(?i)" + gs, 3))));
            } catch (Exception ignored) {}
        }
        addColFilter(filters, txtFDate.getText(),     1);
        addColFilter(filters, txtFCustomer.getText(), 2);
        addColFilter(filters, txtFProduct.getText(),  3);
        try {
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        } catch (Exception ignored) {}
        lblCount.setText(masterTable.getRowCount() + " / " + tot + " sales");
    }

    private void addColFilter(List<RowFilter<DefaultTableModel,Object>> list, String val, int col) {
        if (val == null || val.trim().isEmpty()) return;
        try { list.add(RowFilter.regexFilter("(?i)" + val.trim(), col)); }
        catch (Exception ignored) {}
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI Helpers — same set as CustomerDetails
    // ═════════════════════════════════════════════════════════════════════
    private JPanel sectionHeader(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SECTION_BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); p.setAlignmentX(0f);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(4, 8, 4, 8)));
        JLabel l = new JLabel(text.toUpperCase()); l.setFont(F_SEC); l.setForeground(GOLD);
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
        tf.setBackground(new Color(14, 20, 38)); tf.setForeground(GOLD); return tf;
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
    private long parseLong(String s)  { try { return Long.parseLong(s.trim()); }    catch (Exception e) { return 0; } }
    private String fmt(double d)      { return d == 0 ? "" : String.format("%.2f", d); }
    private String nvl(String s)      { return s == null ? "" : s; }
    private String escHtml(String s)  { return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;"); }
    private void setStrOrNull(PreparedStatement ps, int idx, String val) throws SQLException {
        if (val == null || val.isEmpty()) ps.setNull(idx, Types.VARCHAR); else ps.setString(idx, val);
    }
}