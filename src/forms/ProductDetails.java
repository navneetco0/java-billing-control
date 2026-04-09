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
 * ProductDetails — CRUA (Create, Read, Update, Archive) UI
 *
 * Tables:
 * suppliers: id, name, contact_person, email, phone, address, is_active
 * products: id, name, slug, ..., supplier_id (FK → suppliers.id), ...
 *
 * Supplier Features:
 * • Auto-creates suppliers table if absent
 * • supplier_id FK column added to products
 * • Supplier searchable combo-dropdown in form (type to filter)
 * • Supplier column shown in master table
 * • Seeded with sample suppliers for demo
 */
public class ProductDetails extends JInternalFrame {


    // ── Supplier model ─────────────────────────────────────────────────────
    /** Lightweight supplier entry for the dropdown */
    private static class Supplier {
        long id;
        String name;

        Supplier(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return id + " — " + name;
        }
    }

    // ── DB state ───────────────────────────────────────────────────────────
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot = 0;
    private boolean dirty = false;
    private long currentId = -1;

    /** Full supplier list loaded once; filtered live in the combo */
    private final List<Supplier> supplierList = new ArrayList<>();

    // ── Master table ───────────────────────────────────────────────────────
    private DefaultTableModel tModel;
    private JTable masterTable;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel lblCount;

    // ── Search / filter bar ────────────────────────────────────────────────
    private JTextField txtSearch;
    private JTextField txtFName, txtFSku, txtFBrand, txtFCat, txtFSupplier;
    private JComboBox<String> cmbFStatus, cmbFActive, cmbFFeatured;

    // ── Form fields ────────────────────────────────────────────────────────
    private JTextField fId, fName, fSlug, fSku, fBarcode, fShortDesc;
    private JTextArea fDesc;
    private JTextField fPrice, fSalePrice, fCostPrice;
    private JTextField fStockQty, fCatId, fBrandId;
    private JTextField fWeight, fLength, fWidth, fHeight, fImageUrl;
    private JComboBox<String> fStockStatus;
    private JCheckBox fIsActive, fIsFeatured;
    private JTextField fCreatedAt, fUpdatedAt;
    private JLabel lblDirty, lblStockBadge, lblArchiveBadge;

    // ── Supplier searchable combo ──────────────────────────────────────────
    /** The text field sitting inside / replacing the combo editor */
    private JTextField supplierSearch;
    /** Popup list shown while typing */
    private JPopupMenu supplierPopup;
    /** The list inside the popup */
    private JList<Supplier> supplierPopupList;
    private DefaultListModel<Supplier> supplierPopupModel;
    /** Currently selected supplier (null = none) */
    private Supplier selectedSupplier = null;
    /** Label showing selected supplier beside the search field */
    private JLabel lblSupplierSelected;

    // ── Buttons ────────────────────────────────────────────────────────────
    private JButton btnNew, btnSave, btnUpdate, btnArchive, btnRefresh;

    // ── Toast ──────────────────────────────────────────────────────────────
    private JLabel toastLbl;
    private javax.swing.Timer toastTimer;

    // ──────────────────────────────────────────────────────────────────────
    public ProductDetails() {
        super("Product Details", true, true, true, true);
        setSize(1340, 780);
        setLocation(20, 20);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ensureTables();
        loadSuppliers();
        buildUI();
        registerShortcuts();
        loadMaster();
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // DDL (suppliers + products)
    // ═════════════════════════════════════════════════════════════════════
    private void ensureTables() {
        try {
            stmt = MdiForm.sconnect.createStatement();

            // ── products (suppliers already exists) ─────────────────────
            try {
                stmt.executeUpdate(
                        "CREATE TABLE products (" +
                                "  id                NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                                "  name              VARCHAR2(255)  NOT NULL," +
                                "  slug              VARCHAR2(255)  UNIQUE," +
                                "  description       CLOB," +
                                "  short_description VARCHAR2(500)," +
                                "  sku               VARCHAR2(100)  UNIQUE," +
                                "  barcode           VARCHAR2(100)," +
                                "  price             NUMBER(12,2)   DEFAULT 0," +
                                "  sale_price        NUMBER(12,2)," +
                                "  cost_price        NUMBER(12,2)," +
                                "  stock_quantity    NUMBER(10)     DEFAULT 0," +
                                "  stock_status      VARCHAR2(20)   DEFAULT 'IN_STOCK'," +
                                "  category_id       NUMBER," +
                                "  brand_id          NUMBER," +
                                "  supplier_id       NUMBER," +
                                "  weight            NUMBER(8,2)," +
                                "  length            NUMBER(8,2)," +
                                "  width             NUMBER(8,2)," +
                                "  height            NUMBER(8,2)," +
                                "  image_url         VARCHAR2(500)," +
                                "  is_active         NUMBER(1)      DEFAULT 1," +
                                "  is_featured       NUMBER(1)      DEFAULT 0," +
                                "  created_at        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP," +
                                "  updated_at        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP," +
                                "  CONSTRAINT chk_prod_stock   CHECK (stock_status IN ('IN_STOCK','OUT_OF_STOCK','BACKORDER')),"
                                +
                                "  CONSTRAINT chk_prod_active  CHECK (is_active   IN (0,1))," +
                                "  CONSTRAINT chk_prod_feature CHECK (is_featured IN (0,1))," +
                                "  CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id)" +
                                "    REFERENCES suppliers(id) ON DELETE SET NULL" +
                                ")");
            } catch (SQLException ignored) {
                /* table already exists */ }

            // Add supplier_id if missing (migration)
            try {
                stmt.executeUpdate("ALTER TABLE products ADD supplier_id NUMBER NULL");
            } catch (SQLException ignored) {
                /* column already exists */ }

            // Add FK if missing
            try {
                stmt.executeUpdate(
                        "ALTER TABLE products ADD CONSTRAINT fk_product_supplier " +
                                "FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL");
            } catch (SQLException ignored) {
                /* constraint already exists */ }

            // updated_at trigger
            try {
                stmt.executeUpdate(
                        "CREATE OR REPLACE TRIGGER trg_products_updated_at " +
                                "BEFORE UPDATE ON products " +
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
    // Load suppliers into memory
    // ═════════════════════════════════════════════════════════════════════
    private void loadSuppliers() {
        supplierList.clear();
        supplierList.add(new Supplier(0, "(none)"));
        try {
            stmt = MdiForm.sconnect.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT id, supplier_name FROM suppliers WHERE is_active = 1 ORDER BY supplier_name");
            while (rs.next())
                supplierList.add(new Supplier(rs.getLong("id"), rs.getString("supplier_name")));
        } catch (SQLException ex) {
            showToast("Supplier load error: " + ex.getMessage(), DANGER);
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
                g2.setColor(ACCENT);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        hdr.setPreferredSize(new Dimension(0, 54));
        hdr.setBorder(new EmptyBorder(0, 22, 0, 22));

        JLabel title = new JLabel("🛒  Product Management");
        title.setFont(F_HEAD);
        title.setForeground(TEXT_PRI);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        lblCount = styledBadge("0 products", TEXT_MUT, BG_INPUT);
        right.add(lblCount);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    // ── Split pane ─────────────────────────────────────────────────────────
    private JSplitPane buildSplit() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        sp.setDividerLocation(600);
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

        txtSearch = styledField("🔍  Search by Name or SKU…");
        txtSearch.setPreferredSize(new Dimension(0, 34));
        txtSearch.getDocument().addDocumentListener(dl(this::applyFilter));

        JPanel r1 = new JPanel(new GridLayout(1, 5, 5, 0));
        r1.setBackground(BG_PANEL);
        r1.setBorder(new EmptyBorder(6, 0, 4, 0));
        txtFName = miniField("Name…");
        txtFSku = miniField("SKU…");
        txtFBrand = miniField("Brand ID…");
        txtFCat = miniField("Category ID…");
        txtFSupplier = miniField("Supplier…");
        r1.add(txtFName);
        r1.add(txtFSku);
        r1.add(txtFBrand);
        r1.add(txtFCat);
        r1.add(txtFSupplier);

        JPanel r2 = new JPanel(new GridLayout(1, 3, 6, 0));
        r2.setBackground(BG_PANEL);
        cmbFStatus = miniCombo("Stock Status", "All", "IN_STOCK", "OUT_OF_STOCK", "BACKORDER");
        cmbFActive = miniCombo("Active", "All", "Active", "Archived");
        cmbFFeatured = miniCombo("Featured", "All", "Featured", "Not Featured");
        r2.add(cmbFStatus);
        r2.add(cmbFActive);
        r2.add(cmbFFeatured);

        for (JTextField tf : new JTextField[] { txtFName, txtFSku, txtFBrand, txtFCat, txtFSupplier })
            tf.getDocument().addDocumentListener(dl(this::applyFilter));
        for (JComboBox<?> cb : new JComboBox[] { cmbFStatus, cmbFActive, cmbFFeatured })
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
        // COL indices: 0=ID 1=Name 2=SKU 3=Price 4=Sale 5=Qty 6=Status
        // 7=Active 8=Featured 9=Brand 10=Cat 11=Supplier
        String[] cols = {
                "ID", "Name", "SKU", "Price (₹)", "Sale (₹)", "Qty",
                "Status", "Active", "Featured", "Brand", "Cat", "Supplier"
        };
        tModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            public Class<?> getColumnClass(int c) {
                return c == 0 ? Long.class : (c == 3 || c == 4) ? Double.class : c == 5 ? Integer.class : String.class;
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
                            masterTable.convertRowIndexToModel(row), 7).toString();
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
                    if (mc == 6)
                        comp.setForeground(statusColor(getValueAt(row, col).toString()));
                    if (mc == 7)
                        comp.setForeground(archived ? DANGER : ACCENT2);
                    if (mc == 8)
                        comp.setForeground("Yes".equals(getValueAt(row, col)) ? GOLD : TEXT_MUT);
                    if (mc == 11)
                        comp.setForeground(PURPLE); // supplier column
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

        int[] widths = { 50, 145, 85, 82, 76, 48, 100, 68, 68, 56, 50, 130 };
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
        JLabel title = new JLabel("Product Record");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT_PRI);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        lblDirty = new JLabel("● Unsaved");
        lblDirty.setFont(F_SMALL);
        lblDirty.setForeground(WARNING);
        lblDirty.setVisible(false);
        lblStockBadge = styledBadge("", ACCENT2, new Color(10, 55, 32));
        lblArchiveBadge = styledBadge("", DANGER, new Color(70, 15, 15));
        lblArchiveBadge.setVisible(false);
        badges.add(lblDirty);
        badges.add(lblStockBadge);
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
        fSlug = inputField();
        id1.add(labeled("Product ID (auto)", fId));
        id1.add(labeled("Name *", fName));
        id1.add(labeled("Slug", fSlug));
        form.add(id1);

        JPanel id2 = gridPanel(3);
        fSku = inputField();
        fBarcode = inputField();
        fStockStatus = new JComboBox<>(new String[] { "IN_STOCK", "OUT_OF_STOCK", "BACKORDER" });
        styleComboBox(fStockStatus);
        fStockStatus.addActionListener(e -> setDirty(true));
        id2.add(labeled("SKU", fSku));
        id2.add(labeled("Barcode", fBarcode));
        id2.add(labeled("Stock Status", fStockStatus));
        form.add(id2);

        // ── Supplier (searchable dropdown) ─────────────────────────────
        form.add(sectionHeader("Supplier"));
        form.add(buildSupplierRow());

        // ── Description ────────────────────────────────────────────────
        form.add(sectionHeader("Description"));
        fShortDesc = inputField();
        form.add(fullWidthLabeled("Short Description", fShortDesc));
        fDesc = new JTextArea(4, 1);
        styleTextArea(fDesc);
        JScrollPane descScroll = new JScrollPane(fDesc);
        descScroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        descScroll.setPreferredSize(new Dimension(0, 90));
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        styleScrollBar(descScroll);
        form.add(fullWidthLabeled("Description", descScroll));

        // ── Pricing ────────────────────────────────────────────────────
        form.add(sectionHeader("Pricing  (₹ INR)"));
        JPanel pricePnl = gridPanel(3);
        fPrice = inputField();
        fSalePrice = inputField();
        fCostPrice = inputField();
        pricePnl.add(labeled("Price *", fPrice));
        pricePnl.add(labeled("Sale Price", fSalePrice));
        pricePnl.add(labeled("Cost Price", fCostPrice));
        form.add(pricePnl);

        // ── Stock ──────────────────────────────────────────────────────
        form.add(sectionHeader("Stock & Classification"));
        JPanel stockPnl = gridPanel(3);
        fStockQty = inputField();
        fCatId = inputField();
        fBrandId = inputField();
        stockPnl.add(labeled("Stock Quantity", fStockQty));
        stockPnl.add(labeled("Category ID", fCatId));
        stockPnl.add(labeled("Brand ID", fBrandId));
        form.add(stockPnl);

        // ── Dimensions ────────────────────────────────────────────────
        form.add(sectionHeader("Dimensions & Weight"));
        JPanel dimPnl = gridPanel(4);
        fWeight = inputField();
        fLength = inputField();
        fWidth = inputField();
        fHeight = inputField();
        dimPnl.add(labeled("Weight (kg)", fWeight));
        dimPnl.add(labeled("Length (cm)", fLength));
        dimPnl.add(labeled("Width (cm)", fWidth));
        dimPnl.add(labeled("Height (cm)", fHeight));
        form.add(dimPnl);

        // ── Media & Flags ─────────────────────────────────────────────
        form.add(sectionHeader("Media & Flags"));
        fImageUrl = inputField();
        form.add(fullWidthLabeled("Image URL", fImageUrl));
        JPanel flagPnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 6));
        flagPnl.setBackground(BG_CARD);
        flagPnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        fIsActive = styledCheck("Active");
        fIsFeatured = styledCheck("Featured");
        fIsActive.setSelected(true);
        fIsActive.addActionListener(e -> setDirty(true));
        fIsFeatured.addActionListener(e -> setDirty(true));
        flagPnl.add(fIsActive);
        flagPnl.add(fIsFeatured);
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
        for (JTextField tf : new JTextField[] { fName, fSlug, fSku, fBarcode, fShortDesc,
                fPrice, fSalePrice, fCostPrice, fStockQty, fCatId, fBrandId,
                fWeight, fLength, fWidth, fHeight, fImageUrl })
            tf.getDocument().addDocumentListener(dirtyl);
        fDesc.getDocument().addDocumentListener(dirtyl);
        fStockQty.getDocument().addDocumentListener(dl(this::updateStockBadge));

        JScrollPane scroll = new JScrollPane(form,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Supplier searchable row
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildSupplierRow() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 0));
        wrapper.setBackground(BG_CARD);
        wrapper.setAlignmentX(0f);
        wrapper.setBorder(new EmptyBorder(6, 0, 8, 0));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        // Left: label + search field
        JPanel leftPart = new JPanel(new BorderLayout(0, 3));
        leftPart.setBackground(BG_CARD);

        JLabel lbl = new JLabel("Supplier  (type to search or select)");
        lbl.setFont(F_LABEL);
        lbl.setForeground(TEXT_MUT);

        supplierSearch = new JTextField();
        supplierSearch.setFont(F_INPUT);
        supplierSearch.setBackground(BG_INPUT);
        supplierSearch.setForeground(TEXT_PRI);
        supplierSearch.setCaretColor(ACCENT);
        supplierSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 176, 240, 120)),
                new EmptyBorder(5, 8, 5, 8)));
        supplierSearch.setToolTipText("Type supplier name or ID to filter…");

        leftPart.add(lbl, BorderLayout.NORTH);
        leftPart.add(supplierSearch, BorderLayout.CENTER);

        // Right: selected supplier badge + clear button
        JPanel rightPart = new JPanel(new BorderLayout(0, 3));
        rightPart.setBackground(BG_CARD);
        rightPart.setPreferredSize(new Dimension(200, 0));

        JLabel selLbl = new JLabel("Selected:");
        selLbl.setFont(F_LABEL);
        selLbl.setForeground(TEXT_MUT);

        JPanel selRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selRow.setBackground(BG_CARD);
        lblSupplierSelected = styledBadge("(none)", TEXT_MUT, new Color(22, 32, 58));
        JButton btnClearSup = new JButton("✕");
        btnClearSup.setFont(F_SMALL);
        btnClearSup.setForeground(TEXT_MUT);
        btnClearSup.setBackground(new Color(35, 20, 20));
        btnClearSup.setBorder(new EmptyBorder(3, 7, 3, 7));
        btnClearSup.setFocusPainted(false);
        btnClearSup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClearSup.setToolTipText("Clear supplier selection");
        btnClearSup.addActionListener(e -> {
            selectedSupplier = null;
            supplierSearch.setText("");
            lblSupplierSelected.setText("(none)");
            lblSupplierSelected.setForeground(TEXT_MUT);
            lblSupplierSelected.setBackground(new Color(22, 32, 58));
            setDirty(true);
        });

        selRow.add(lblSupplierSelected);
        selRow.add(btnClearSup);
        rightPart.add(selLbl, BorderLayout.NORTH);
        rightPart.add(selRow, BorderLayout.CENTER);

        wrapper.add(leftPart, BorderLayout.CENTER);
        wrapper.add(rightPart, BorderLayout.EAST);

        // ── Popup list ────────────────────────────────────────────────
        supplierPopupModel = new DefaultListModel<>();
        supplierPopupList = new JList<>(supplierPopupModel);
        supplierPopupList.setFont(F_INPUT);
        supplierPopupList.setBackground(new Color(20, 30, 54));
        supplierPopupList.setForeground(TEXT_PRI);
        supplierPopupList.setSelectionBackground(BG_ROW_SEL);
        supplierPopupList.setSelectionForeground(Color.WHITE);
        supplierPopupList.setFixedCellHeight(28);
        supplierPopupList.setBorder(new EmptyBorder(2, 0, 2, 0));

        // Custom renderer: highlight ID in accent color
        supplierPopupList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int idx, boolean sel, boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, idx, sel, focus);
                Supplier s = (Supplier) value;
                c.setText("<html><font color='#38b0f0'>" + s.id + "</font>  " +
                        escHtml(s.name) + "</html>");
                c.setBorder(new EmptyBorder(4, 12, 4, 12));
                if (!sel) {
                    c.setBackground(idx % 2 == 0 ? new Color(20, 30, 54) : new Color(24, 36, 62));
                }
                return c;
            }
        });

        supplierPopup = new JPopupMenu();
        supplierPopup.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        supplierPopup.setBackground(new Color(20, 30, 54));

        JScrollPane popupScroll = new JScrollPane(supplierPopupList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        popupScroll.setBorder(null);
        popupScroll.setPreferredSize(new Dimension(320, 160));
        styleScrollBar(popupScroll);
        supplierPopup.add(popupScroll);

        // Type-to-filter
        supplierSearch.getDocument().addDocumentListener(dl(() -> {
            filterSupplierPopup(supplierSearch.getText().trim());
            setDirty(true);
        }));

        // Show popup on focus / click
        supplierSearch.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                filterSupplierPopup(supplierSearch.getText().trim());
            }
        });
        supplierSearch.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                filterSupplierPopup(supplierSearch.getText().trim());
            }
        });

        // Arrow-key navigation into popup
        supplierSearch.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (!supplierPopup.isVisible())
                    return;
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    supplierPopupList.requestFocusInWindow();
                    if (supplierPopupList.getModel().getSize() > 0)
                        supplierPopupList.setSelectedIndex(0);
                    e.consume();
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    supplierPopup.setVisible(false);
                }
            }
        });

        // Select from popup via mouse
        supplierPopupList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1)
                    commitSupplierSelection();
            }
        });
        // Select from popup via Enter key
        supplierPopupList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    commitSupplierSelection();
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                    supplierPopup.setVisible(false);
            }
        });

        return wrapper;
    }

    /** Rebuild popup items that match the query; show popup if any match */
    private void filterSupplierPopup(String query) {
        supplierPopupModel.clear();
        String q = query.toLowerCase();
        for (Supplier s : supplierList) {
            if (q.isEmpty() || String.valueOf(s.id).contains(q) ||
                    s.name.toLowerCase().contains(q)) {
                supplierPopupModel.addElement(s);
            }
        }
        if (supplierPopupModel.isEmpty()) {
            supplierPopup.setVisible(false);
            return;
        }
        if (!supplierPopup.isVisible()) {
            supplierPopup.show(supplierSearch, 0, supplierSearch.getHeight());
        }
        supplierPopup.revalidate();
        supplierPopup.repaint();
    }

    /** Commit the highlighted popup row as the selected supplier */
    private void commitSupplierSelection() {
        Supplier s = supplierPopupList.getSelectedValue();
        if (s == null)
            return;
        supplierPopup.setVisible(false);
        if (s.id == 0) {
            // sentinel "(none)"
            selectedSupplier = null;
            supplierSearch.setText("");
            lblSupplierSelected.setText("(none)");
            lblSupplierSelected.setForeground(TEXT_MUT);
            lblSupplierSelected.setBackground(new Color(22, 32, 58));
        } else {
            selectedSupplier = s;
            supplierSearch.setText(s.name);
            lblSupplierSelected.setText("ID " + s.id + "  " + s.name);
            lblSupplierSelected.setForeground(PURPLE);
            lblSupplierSelected.setBackground(new Color(35, 20, 60));
        }
        setDirty(true);
        supplierSearch.requestFocusInWindow();
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
        for (String s : new String[] { "Ctrl+S  Save", "Ctrl+U  Update", "Ctrl+A  Archive", "Ctrl+N  New",
                "Esc  Refresh" }) {
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
                    "SELECT p.id, p.name, p.sku, p.price, p.sale_price, p.stock_quantity, " +
                            "p.stock_status, p.is_active, p.is_featured, p.brand_id, p.category_id, " +
                            "COALESCE(s.supplier_name, '—') AS supplier_name " +
                            "FROM products p " +
                            "LEFT JOIN suppliers s ON p.supplier_id = s.id " +
                            "ORDER BY p.id");
            while (rs.next()) {
                tModel.addRow(new Object[] {
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("sku"),
                        rs.getDouble("price"),
                        rs.getDouble("sale_price"),
                        rs.getInt("stock_quantity"),
                        rs.getString("stock_status"),
                        rs.getInt("is_active") == 1 ? "Active" : "Archived", // NUMBER(1)
                        rs.getInt("is_featured") == 1 ? "Yes" : "No", // NUMBER(1)
                        rs.getLong("brand_id"),
                        rs.getLong("category_id"),
                        rs.getString("supplier_name")
                });
                tot++;
            }
            lblCount.setText(tot + " product" + (tot == 1 ? "" : "s"));
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
            pstmt = MdiForm.sconnect.prepareStatement("SELECT * FROM products WHERE id=?");
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
        fName.setText(nvl(rs.getString("name")));
        fSlug.setText(nvl(rs.getString("slug")));
        fSku.setText(nvl(rs.getString("sku")));
        fBarcode.setText(nvl(rs.getString("barcode")));
        fShortDesc.setText(nvl(rs.getString("short_description")));
        fDesc.setText(nvl(rs.getString("description")));
        fPrice.setText(fmt(rs.getDouble("price")));
        fSalePrice.setText(fmt(rs.getDouble("sale_price")));
        fCostPrice.setText(fmt(rs.getDouble("cost_price")));
        fStockQty.setText(String.valueOf(rs.getInt("stock_quantity")));
        fCatId.setText(String.valueOf(rs.getLong("category_id")));
        fBrandId.setText(String.valueOf(rs.getLong("brand_id")));
        fWeight.setText(fmt(rs.getDouble("weight")));
        fLength.setText(fmt(rs.getDouble("length")));
        fWidth.setText(fmt(rs.getDouble("width")));
        fHeight.setText(fmt(rs.getDouble("height")));
        fImageUrl.setText(nvl(rs.getString("image_url")));

        fIsActive.setSelected(rs.getInt("is_active") == 1); // NUMBER(1)
        fIsFeatured.setSelected(rs.getInt("is_featured") == 1); // NUMBER(1)
        fStockStatus.setSelectedItem(rs.getString("stock_status"));

        // Supplier
        long sid = rs.getLong("supplier_id");
        if (rs.wasNull() || sid == 0) {
            selectedSupplier = null;
            supplierSearch.setText("");
            lblSupplierSelected.setText("(none)");
            lblSupplierSelected.setForeground(TEXT_MUT);
            lblSupplierSelected.setBackground(new Color(22, 32, 58));
        } else {
            Supplier found = supplierList.stream()
                    .filter(s -> s.id == sid).findFirst().orElse(null);
            if (found != null) {
                selectedSupplier = found;
                supplierSearch.setText(found.name);
                lblSupplierSelected.setText("ID " + found.id + "  " + found.name);
                lblSupplierSelected.setForeground(PURPLE);
                lblSupplierSelected.setBackground(new Color(35, 20, 60));
            } else {
                selectedSupplier = new Supplier(sid, "ID " + sid);
                supplierSearch.setText("ID " + sid);
                lblSupplierSelected.setText("ID " + sid + " (inactive?)");
                lblSupplierSelected.setForeground(WARNING);
                lblSupplierSelected.setBackground(new Color(50, 40, 10));
            }
        }

        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        fCreatedAt.setText(ca != null ? ca.toLocalDateTime().format(fmt) : "");
        fUpdatedAt.setText(ua != null ? ua.toLocalDateTime().format(fmt) : "");

        boolean archived = rs.getInt("is_active") != 1;
        lblArchiveBadge.setText(archived ? "🗄 ARCHIVED" : "");
        lblArchiveBadge.setVisible(archived);
        updateStockBadge();
    }

    // Add this helper used in fillForm
    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private void saveRecord() {
        if (fName.getText().trim().isEmpty() || fPrice.getText().trim().isEmpty()) {
            showToast("Name and Price are required!", WARNING);
            return;
        }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                    "INSERT INTO products(name,slug,description,short_description,sku,barcode," +
                            "price,sale_price,cost_price,stock_quantity,stock_status,category_id,brand_id," +
                            "supplier_id,weight,length,width,height,image_url,is_active,is_featured) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                            "RETURNING id INTO ?");
            bindAll(pstmt);
            ((oracle.jdbc.OraclePreparedStatement) pstmt)
                    .registerReturnParameter(22, Types.NUMERIC);
            pstmt.executeUpdate();
            ResultSet keys = ((oracle.jdbc.OraclePreparedStatement) pstmt)
                    .getReturnResultSet();
            if (keys != null && keys.next()) {
                currentId = keys.getLong(1);
                fId.setText(String.valueOf(currentId));
            }
            showToast("✔ Product saved — ID: " + currentId, ACCENT2);
            loadMaster();
            setDirty(false);
        } catch (SQLException ex) {
            showToast("Save error: " + ex.getMessage(), DANGER);
        }
    }

    private void updateRecord() {
        if (currentId < 0) {
            showToast("Select a product first!", WARNING);
            return;
        }
        if (fName.getText().trim().isEmpty() || fPrice.getText().trim().isEmpty()) {
            showToast("Name and Price are required!", WARNING);
            return;
        }
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                    "UPDATE products SET name=?,slug=?,description=?,short_description=?,sku=?,barcode=?," +
                            "price=?,sale_price=?,cost_price=?,stock_quantity=?,stock_status=?,category_id=?,brand_id=?,"
                            +
                            "supplier_id=?,weight=?,length=?,width=?,height=?,image_url=?,is_active=?,is_featured=? " +
                            "WHERE id=?");
            bindAll(pstmt);
            pstmt.setLong(22, currentId);
            int r = pstmt.executeUpdate();
            if (r > 0) {
                loadMaster();
                setDirty(false);
                showToast("✔ Product updated.", ACCENT2);
            } else
                showToast("No product matched ID " + currentId, WARNING);
        } catch (SQLException ex) {
            showToast("Update error: " + ex.getMessage(), DANGER);
        }
    }

    private void archiveRecord() {
        if (currentId < 0) {
            showToast("Select a product to archive!", WARNING);
            return;
        }
        boolean alreadyArchived = !fIsActive.isSelected();
        String action = alreadyArchived ? "Restore" : "Archive";
        String msg = alreadyArchived
                ? "Restore product ID " + currentId + "? It will become active again."
                : "Archive product ID " + currentId + "? It will be deactivated (not deleted).";
        int ans = JOptionPane.showConfirmDialog(this, msg, action + " Product",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION)
            return;
        try {
            pstmt = MdiForm.sconnect.prepareStatement(
                    "UPDATE products SET is_active=? WHERE id=?");
            pstmt.setInt(1, alreadyArchived ? 1 : 0); // NUMBER(1), not boolean
            pstmt.setLong(2, currentId);
            pstmt.executeUpdate();
            fIsActive.setSelected(alreadyArchived);
            lblArchiveBadge.setVisible(!alreadyArchived);
            loadMaster();
            showToast(alreadyArchived ? "✔ Product restored." : "🗄 Product archived.", WARNING);
        } catch (SQLException ex) {
            showToast("Archive error: " + ex.getMessage(), DANGER);
        }
    }

    /**
     * Bind parameters: 1–13 match original columns, 14 = supplier_id (new),
     * 15–21 = dimensions / flags.
     */
    private void bindAll(PreparedStatement ps) throws SQLException {
        ps.setString(1, fName.getText().trim());
        setStrOrNull(ps, 2, fSlug.getText().trim());
        setStrOrNull(ps, 3, fDesc.getText().trim());
        setStrOrNull(ps, 4, fShortDesc.getText().trim());
        setStrOrNull(ps, 5, fSku.getText().trim());
        setStrOrNull(ps, 6, fBarcode.getText().trim());
        ps.setDouble(7, parseDbl(fPrice.getText()));
        ps.setDouble(8, parseDbl(fSalePrice.getText()));
        ps.setDouble(9, parseDbl(fCostPrice.getText()));
        ps.setInt(10, parseInt(fStockQty.getText()));
        ps.setString(11, (String) fStockStatus.getSelectedItem());
        ps.setLong(12, parseLong(fCatId.getText()));
        ps.setLong(13, parseLong(fBrandId.getText()));
        // supplier_id
        if (selectedSupplier == null || selectedSupplier.id == 0)
            ps.setNull(14, Types.NUMERIC); // NUMERIC not BIGINT
        else
            ps.setLong(14, selectedSupplier.id);
        ps.setDouble(15, parseDbl(fWeight.getText()));
        ps.setDouble(16, parseDbl(fLength.getText()));
        ps.setDouble(17, parseDbl(fWidth.getText()));
        ps.setDouble(18, parseDbl(fHeight.getText()));
        setStrOrNull(ps, 19, fImageUrl.getText().trim());
        ps.setInt(20, fIsActive.isSelected() ? 1 : 0); // NUMBER(1)
        ps.setInt(21, fIsFeatured.isSelected() ? 1 : 0); // NUMBER(1)
    }

    private void clearForm() {
        currentId = -1;
        for (JTextField tf : new JTextField[] { fId, fName, fSlug, fSku, fBarcode, fShortDesc,
                fPrice, fSalePrice, fCostPrice, fStockQty, fCatId, fBrandId,
                fWeight, fLength, fWidth, fHeight, fImageUrl, fCreatedAt, fUpdatedAt })
            tf.setText("");
        fId.setText("(auto)");
        fDesc.setText("");
        fStockStatus.setSelectedIndex(0);
        fIsActive.setSelected(true);
        fIsFeatured.setSelected(false);
        lblStockBadge.setText("");
        lblArchiveBadge.setVisible(false);
        // Reset supplier
        selectedSupplier = null;
        supplierSearch.setText("");
        lblSupplierSelected.setText("(none)");
        lblSupplierSelected.setForeground(TEXT_MUT);
        lblSupplierSelected.setBackground(new Color(22, 32, 58));
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
                        RowFilter.regexFilter("(?i)" + gs, 2))));
            } catch (Exception ignored) {
            }
        }
        addColFilter(filters, txtFName.getText(), 1);
        addColFilter(filters, txtFSku.getText(), 2);
        addColFilter(filters, txtFBrand.getText(), 9);
        addColFilter(filters, txtFCat.getText(), 10);
        addColFilter(filters, txtFSupplier.getText(), 11); // ← filter by supplier name

        String st = (String) cmbFStatus.getSelectedItem();
        if (!"All".equals(st))
            addColFilter(filters, st, 6);
        String ac = (String) cmbFActive.getSelectedItem();
        if ("Active".equals(ac))
            addColFilter(filters, "Active", 7);
        if ("Archived".equals(ac))
            addColFilter(filters, "Archived", 7);
        String ft = (String) cmbFFeatured.getSelectedItem();
        if ("Featured".equals(ft))
            addColFilter(filters, "Yes", 8);
        if ("Not Featured".equals(ft))
            addColFilter(filters, "No", 8);

        try {
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        } catch (Exception ignored) {
        }
        lblCount.setText(masterTable.getRowCount() + " / " + tot + " products");
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
    private void updateStockBadge() {
        try {
            int qty = Integer.parseInt(fStockQty.getText().trim());
            String status = (String) fStockStatus.getSelectedItem();
            if ("out_of_stock".equals(status) || qty <= 0) {
                lblStockBadge.setText("⚠ Out of Stock");
                lblStockBadge.setBackground(new Color(70, 18, 18));
                lblStockBadge.setForeground(DANGER);
            } else if ("backorder".equals(status)) {
                lblStockBadge.setText("⏳ Backorder");
                lblStockBadge.setBackground(new Color(60, 45, 10));
                lblStockBadge.setForeground(WARNING);
            } else if (qty < 10) {
                lblStockBadge.setText("⚡ Low: " + qty + " units");
                lblStockBadge.setBackground(new Color(70, 52, 10));
                lblStockBadge.setForeground(WARNING);
            } else {
                lblStockBadge.setText("✔ In Stock: " + qty);
                lblStockBadge.setBackground(new Color(10, 52, 30));
                lblStockBadge.setForeground(ACCENT2);
            }
        } catch (NumberFormatException e) {
            lblStockBadge.setText("");
        }
    }

    private Color statusColor(String s) {
        if (s == null)
            return TEXT_PRI;
        return switch (s) {
            case "IN_STOCK" -> ACCENT2;
            case "OUT_OF_STOCK" -> DANGER;
            case "BACKORDER" -> WARNING;
            default -> TEXT_MUT;
        };
    }

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
        tf.setForeground(ACCENT);
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

    // ── Misc ────────────────────────────────────────────────────────────
    private void setDirty(boolean d) {
        dirty = d;
        SwingUtilities.invokeLater(() -> lblDirty.setVisible(d));
    }

    private void showToast(String msg, Color bg) {
        if (toastLbl == null) {
            System.err.println("[Toast] " + msg); // fallback before UI is ready
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

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String fmt(double d) {
        return d == 0 ? "" : String.format("%.2f", d);
    }

    private String escHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;");
    }

    private void setStrOrNull(PreparedStatement ps, int idx, String val) throws SQLException {
        if (val == null || val.isEmpty())
            ps.setNull(idx, Types.VARCHAR);
        else
            ps.setString(idx, val);
    }

    // ─────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Test ProductDetails");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JDesktopPane dp = new JDesktopPane();
            dp.setBackground(new Color(8, 12, 20));
            f.setContentPane(dp);
            f.setSize(1380, 820);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            dp.add(new ProductDetails());
        });
    }
}