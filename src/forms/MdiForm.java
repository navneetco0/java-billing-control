package forms;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;
import static ui.UIConstants.*;

public class MdiForm extends JFrame implements ActionListener {

    public static Connection sconnect;

    // ── Menu items ────────────────────────────────────────────────────────
    private JMenuBar mbr;
    private JMenu menuMaster, menuEmp, menuPurchase, menuSale, menuQuery, menuReport;
    private JMenuItem menuEmpdetails, menuEmpAttendence, menuEmpSalary;
    private JMenuItem menuCustomerDetail, menuProductDetail, menuItemSupplier, menuItemGodown;
    private JMenuItem manuItemPurmaster, menuItemreturn, menuItemSalemaster, menuItemSalereturn;
    private JMenuItem menuItemEmpQuery, menuItemSaleQuery, menuItemPurchaseQuery, menuItemGodownQuery;
    private JMenuItem menuItemEmpReport, menuItemSaleReport, menuItemPurchaseReport, menuItemGodownReport;

    // ── Desktop ───────────────────────────────────────────────────────────
    private JDesktopPane desktop;

    // ── Dashboard stat labels ─────────────────────────────────────────────
    private JLabel lblProducts, lblSales, lblPurchases, lblEmployees;
    private JLabel lblCustomers, lblSuppliers, lblSaleRet, lblPurRet;
    private JLabel lblGodowns, lblSaleAmt, lblPurAmt;

    // ── Trend data ────────────────────────────────────────────────────────
    private int[] saleTrend = new int[6];
    private int[] purchaseTrend = new int[6];
    private double[] saleAmtTrend = new double[6];
    private String[] monthLabels = new String[6];

    // ── Dashboard panel reference ─────────────────────────────────────────
    private JPanel dashboardPanel;

    // ─────────────────────────────────────────────────────────────────────
    public MdiForm() {
        // ── Fix 1: Prevent popup menus appearing behind JInternalFrames ───
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        // ── Fix 2: Override menu selection colors so text never goes white ─
        UIManager.put("MenuItem.selectionBackground", MENU_SEL);
        UIManager.put("MenuItem.selectionForeground", TEXT_PRI);
        UIManager.put("Menu.selectionBackground", MENU_SEL);
        UIManager.put("Menu.selectionForeground", TEXT_PRI);
        UIManager.put("PopupMenu.background", BG_CARD);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER_COL));
        UIManager.put("MenuItem.background", BG_CARD);
        UIManager.put("MenuItem.foreground", TEXT_PRI);
        UIManager.put("Menu.background", BG_CARD);
        UIManager.put("Menu.foreground", TEXT_PRI);

        connectDB();
        initializeDatabase();
        buildUI();
        buildMenus();
        loadDashboardData();

        setTitle("BAJAJ Enterprises — Billing Control Automation System");
        setExtendedState(MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Logo loader (same approach as LoginWindow — removes black bg)
    // ═════════════════════════════════════════════════════════════════════
    private ImageIcon loadBajajLogo(int w, int h) {
        try {
            BufferedImage src = ImageIO.read(getClass().getClassLoader().getResource("assets/bajaj_logo.png"));
            if (src == null)
                return null;
            BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = argb.createGraphics();
            g2.drawImage(src, 0, 0, null);
            g2.dispose();
            // Make near-black pixels transparent
            for (int y = 0; y < argb.getHeight(); y++) {
                for (int x = 0; x < argb.getWidth(); x++) {
                    int rgba = argb.getRGB(x, y);
                    int r = (rgba >> 16) & 0xFF, gv = (rgba >> 8) & 0xFF, b = rgba & 0xFF;
                    if (r < 30 && gv < 30 && b < 30)
                        argb.setRGB(x, y, 0x00000000);
                }
            }
            return new ImageIcon(argb.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception ex) {
            return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // DB
    // ═════════════════════════════════════════════════════════════════════
    private void connectDB() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            sconnect = DriverManager.getConnection(
                    "jdbc:oracle:thin:@localhost:1521:free", "SYSTEM", "PassTheWord");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
            System.exit(0);
        }
    }

    public void initializeDatabase() {
        try {
            if (!tableExists("LOGIN_WINDOW")) {
                exec("CREATE TABLE login_window (user_type VARCHAR2(100), password VARCHAR2(100))");
                exec("INSERT INTO login_window VALUES ('ADMIN','ADMIN123')");
            }
            if (!tableExists("EMPLOYEE_DETAILS")) {
                exec("CREATE TABLE employee_details (emp_id VARCHAR2(20) PRIMARY KEY, emp_name VARCHAR2(150), " +
                        "gender VARCHAR2(10), dob VARCHAR2(20), doj VARCHAR2(20), phone VARCHAR2(20), " +
                        "mobile_no VARCHAR2(20), email_id VARCHAR2(150), bsalary NUMBER(12,2) DEFAULT 0, " +
                        "narration VARCHAR2(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean tableExists(String n) throws SQLException {
        try (Statement st = sconnect.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM user_tables WHERE table_name='" + n.toUpperCase() + "'")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void exec(String sql) throws SQLException {
        try (Statement st = sconnect.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private int queryCount(String sql) {
        try (Statement st = sconnect.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double queryDouble(String sql) {
        try (Statement st = sconnect.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Build UI
    // ═════════════════════════════════════════════════════════════════════
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_MAIN);
        setContentPane(root);

        root.add(buildNavBar(), BorderLayout.NORTH);

        desktop = new JDesktopPane() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, BG_MAIN, 0, getHeight(), new Color(9, 15, 30)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(56, 176, 240, 14));
                for (int x = 0; x < getWidth(); x += 32)
                    for (int y = 0; y < getHeight(); y += 32)
                        g2.fillOval(x, y, 2, 2);
                g2.dispose();
            }
        };
        desktop.setBackground(BG_MAIN);
        root.add(desktop, BorderLayout.CENTER);

        dashboardPanel = buildDashboard();
        desktop.add(dashboardPanel, JLayeredPane.DEFAULT_LAYER);

        // ── Fix 3: Resize dashboard whenever the desktop resizes ──────────
        desktop.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                dashboardPanel.setBounds(0, 0, desktop.getWidth(), desktop.getHeight());
            }

            @Override
            public void componentShown(ComponentEvent e) {
                dashboardPanel.setBounds(0, 0, desktop.getWidth(), desktop.getHeight());
            }
        });
        // Initial bounds set after pack/maximize
        SwingUtilities.invokeLater(() -> dashboardPanel.setBounds(0, 0,
                Math.max(desktop.getWidth(), 1200),
                Math.max(desktop.getHeight(), 900)));
    }

    // ── Nav bar ──────────────────────────────────────────────────────────
    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(11, 18, 38), getWidth(), 0, BG_MAIN));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                g2.dispose();
            }
        };
        nav.setPreferredSize(new Dimension(0, 54));
        nav.setBorder(new EmptyBorder(0, 14, 0, 18));

        // ── Fix 4: Replace ⚡ emoji with actual Bajaj logo ─────────────────
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandPanel.setOpaque(false);

        ImageIcon logoIcon = loadBajajLogo(30, 36);
        if (logoIcon != null) {
            JLabel logoLbl = new JLabel(logoIcon);
            logoLbl.setVerticalAlignment(SwingConstants.CENTER);
            brandPanel.add(logoLbl);
        }

        JLabel brandTxt = new JLabel("BAJAJ Enterprises");
        brandTxt.setFont(F_BRAND);
        brandTxt.setForeground(TEXT_PRI);
        brandPanel.add(brandTxt);

        mbr = new JMenuBar();
        mbr.setOpaque(false);
        mbr.setBorder(null);
        nav.add(brandPanel, BorderLayout.WEST);
        nav.add(mbr, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel userBadge = new JLabel("  ADMIN  ");
        userBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        userBadge.setForeground(GOLD);
        userBadge.setOpaque(true);
        userBadge.setBackground(new Color(58, 42, 6));
        userBadge.setBorder(new EmptyBorder(3, 8, 3, 8));
        JLabel clock = new JLabel();
        clock.setFont(F_SMALL);
        clock.setForeground(TEXT_MUT);
        new javax.swing.Timer(1000, e -> clock.setText(new java.text.SimpleDateFormat("HH:mm:ss  dd MMM yyyy")
                .format(new java.util.Date()))).start();
        right.add(clock);
        right.add(userBadge);
        nav.add(right, BorderLayout.EAST);

        return nav;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Dashboard
    // ═════════════════════════════════════════════════════════════════════
    private JPanel buildDashboard() {
        JPanel dash = new JPanel(new BorderLayout());
        dash.setOpaque(false);

        // ── Fix 5: Wrap content in a BorderLayout outer panel so it always
        // stretches to the full viewport width (fixes right-side cutoff) ──
        JPanel outerContent = new JPanel(new BorderLayout());
        outerContent.setOpaque(false);
        outerContent.add(buildDashboardContent(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(outerContent,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        styleScrollBar(scroll);
        dash.add(scroll, BorderLayout.CENTER);
        return dash;
    }

    private JPanel buildDashboardContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(22, 24, 24, 24));

        JLabel greeting = new JLabel("Welcome back, Administrator");
        greeting.setFont(new Font("Segoe UI", Font.BOLD, 20));
        greeting.setForeground(TEXT_PRI);
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dateLabel = new JLabel(
                new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy").format(new java.util.Date()));
        dateLabel.setFont(F_SMALL);
        dateLabel.setForeground(TEXT_MUT);
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(greeting);
        content.add(Box.createVerticalStrut(2));
        content.add(dateLabel);
        content.add(Box.createVerticalStrut(20));

        // ── Stat cards ────────────────────────────────────────────────────
        content.add(sectionLabel("BUSINESS OVERVIEW"));
        content.add(Box.createVerticalStrut(10));

        JPanel row1 = rowPanel(4);
        lblProducts = statCard(row1, "Products", "0", "📦", ACCENT, "Catalogue");
        lblSales = statCard(row1, "Sales", "0", "🛍", ACCENT2, "Orders");
        lblPurchases = statCard(row1, "Purchases", "0", "🛒", GOLD, "POs");
        lblEmployees = statCard(row1, "Employees", "0", "👥", PURPLE, "Staff");
        content.add(row1);
        content.add(Box.createVerticalStrut(12));

        JPanel row2 = rowPanel(4);
        lblCustomers = statCard(row2, "Customers", "0", "🤝", ROSE, "Accounts");
        lblSuppliers = statCard(row2, "Suppliers", "0", "🏭", WARNING, "Vendors");
        lblSaleRet = statCard(row2, "Sale Returns", "0", "↩", DANGER, "Returns");
        lblPurRet = statCard(row2, "Pur. Returns", "0", "↩", new Color(255, 120, 60), "Returns");
        content.add(row2);
        content.add(Box.createVerticalStrut(12));

        JPanel row3 = rowPanel(3);
        lblGodowns = statCard(row3, "Godowns", "0", "🏗", new Color(80, 200, 180), "Warehouses");
        lblSaleAmt = statCard(row3, "Sale Revenue", "₹0", "💰", GOLD, "Total");
        lblPurAmt = statCard(row3, "Purchase Amt", "₹0", "📤", ACCENT, "Spent");
        content.add(row3);
        content.add(Box.createVerticalStrut(24));

        // ── Charts ────────────────────────────────────────────────────────
        content.add(sectionLabel("ACTIVITY TRENDS  (Last 6 Months)"));
        content.add(Box.createVerticalStrut(10));

        JPanel chartsRow = chartRowPanel(2);
        chartsRow.add(buildBarChart("Monthly Sales Volume", saleTrend, monthLabels, ACCENT2));
        chartsRow.add(buildBarChart("Monthly Purchase Volume", purchaseTrend, monthLabels, GOLD));
        content.add(chartsRow);
        content.add(Box.createVerticalStrut(12));

        JPanel chartsRow2 = chartRowPanel(2);
        chartsRow2.add(buildLineChartPanel());
        chartsRow2.add(buildModuleStatusPanel());
        content.add(chartsRow2);
        content.add(Box.createVerticalStrut(24));

        // ── Quick access ──────────────────────────────────────────────────
        content.add(sectionLabel("QUICK ACCESS"));
        content.add(Box.createVerticalStrut(10));
        content.add(buildQuickAccess());

        return content;
    }

    // ── Stat card factory ─────────────────────────────────────────────────
    private JLabel statCard(JPanel parent, String title, String val, String icon, Color accent, String subtitle) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.setPaint(new GradientPaint(0, 0,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30),
                        0, 40, new Color(0, 0, 0, 0)));
                g2.fillRect(0, 0, getWidth(), 40);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 18, 14, 14));

        JLabel ico = new JLabel(icon);
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel num = new JLabel(val);
        num.setFont(F_CARD_N);
        num.setForeground(accent);

        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(F_CARD_L);
        lbl.setForeground(TEXT_MUT);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(F_SMALL);
        sub.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(lbl);
        left.add(Box.createVerticalStrut(4));
        left.add(num);
        left.add(sub);

        card.add(ico, BorderLayout.EAST);
        card.add(left, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accent, 1, true),
                        new EmptyBorder(13, 17, 13, 13)));
                card.repaint();
            }

            public void mouseExited(MouseEvent e) {
                card.setBorder(new EmptyBorder(14, 18, 14, 14));
                card.repaint();
            }
        });

        parent.add(card);
        return num;
    }

    // ── Bar chart ─────────────────────────────────────────────────────────
    private JPanel buildBarChart(String title, int[] data, String[] labels, Color barColor) {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(16, 16, 14, 16));

        JLabel ttl = new JLabel(title);
        ttl.setFont(F_SEC);
        ttl.setForeground(TEXT_PRI);
        ttl.setBorder(new EmptyBorder(0, 0, 8, 0));
        wrap.add(ttl, BorderLayout.NORTH);

        JPanel chart = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int n = data.length;
                int max = 1;
                for (int v : data)
                    max = Math.max(max, v);
                int padL = 10, padR = 10, padT = 10, padB = 24;
                int chartW = w - padL - padR, chartH = h - padT - padB;
                int barW = Math.max(4, chartW / n - 8);

                g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10, new float[] { 4, 4 }, 0));
                g2.setColor(BORDER_COL);
                for (int step = 1; step <= 4; step++) {
                    int y = padT + chartH - (int) (chartH * step / 4.0);
                    g2.drawLine(padL, y, padL + chartW, y);
                }
                g2.setStroke(new BasicStroke(1));

                for (int i = 0; i < n; i++) {
                    int bh = max == 0 ? 2 : Math.max(2, (int) (chartH * data[i] / (double) max));
                    int bx = padL + i * (chartW / n) + (chartW / n - barW) / 2;
                    int by = padT + chartH - bh;
                    g2.setPaint(new GradientPaint(bx, by, barColor, bx, padT + chartH,
                            new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 80)));
                    g2.fillRoundRect(bx, by, barW, bh, 4, 4);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    g2.setColor(TEXT_PRI);
                    String sv = String.valueOf(data[i]);
                    int sw = g2.getFontMetrics().stringWidth(sv);
                    if (bh > 14)
                        g2.drawString(sv, bx + (barW - sw) / 2, by + 12);
                    g2.setFont(F_CHART);
                    g2.setColor(TEXT_MUT);
                    String ml = labels != null && i < labels.length ? labels[i] : "";
                    int lw = g2.getFontMetrics().stringWidth(ml);
                    g2.drawString(ml, bx + (barW - lw) / 2, h - 6);
                }
                g2.dispose();
            }
        };
        chart.setOpaque(false);
        chart.setPreferredSize(new Dimension(0, 170));
        wrap.add(chart, BorderLayout.CENTER);
        return wrap;
    }

    // ── Line chart ────────────────────────────────────────────────────────
    private JPanel buildLineChartPanel() {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(16, 16, 14, 16));

        JLabel ttl = new JLabel("Sale Revenue Trend (₹)");
        ttl.setFont(F_SEC);
        ttl.setForeground(TEXT_PRI);
        ttl.setBorder(new EmptyBorder(0, 0, 8, 0));
        wrap.add(ttl, BorderLayout.NORTH);

        JPanel chart = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                double[] d = saleAmtTrend;
                double max = 1;
                for (double v : d)
                    max = Math.max(max, v);
                int pad = 14, padB = 24;
                int cw = w - pad * 2, ch = h - pad - padB;
                int n = d.length;

                g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10, new float[] { 4, 4 }, 0));
                g2.setColor(BORDER_COL);
                for (int s = 1; s <= 4; s++) {
                    int y = pad + ch - (int) (ch * s / 4.0);
                    g2.drawLine(pad, y, pad + cw, y);
                }
                g2.setStroke(new BasicStroke(1));

                if (n < 2) {
                    g2.dispose();
                    return;
                }
                int[] px = new int[n], py = new int[n];
                for (int i = 0; i < n; i++) {
                    px[i] = pad + i * cw / (n - 1);
                    py[i] = pad + ch - (int) (ch * d[i] / max);
                }
                int[] fx = Arrays.copyOf(px, n + 2);
                int[] fy = Arrays.copyOf(py, n + 2);
                fx[n] = px[n - 1];
                fy[n] = pad + ch;
                fx[n + 1] = px[0];
                fy[n + 1] = pad + ch;
                g2.setPaint(new GradientPaint(0, pad, new Color(255, 195, 50, 60), 0, pad + ch, new Color(0, 0, 0, 0)));
                g2.fillPolygon(fx, fy, n + 2);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(GOLD);
                for (int i = 0; i < n - 1; i++)
                    g2.drawLine(px[i], py[i], px[i + 1], py[i + 1]);
                for (int i = 0; i < n; i++) {
                    g2.setColor(GOLD);
                    g2.fillOval(px[i] - 4, py[i] - 4, 8, 8);
                    g2.setFont(F_CHART);
                    g2.setColor(TEXT_MUT);
                    String ml = monthLabels != null && i < monthLabels.length ? monthLabels[i] : "";
                    int lw = g2.getFontMetrics().stringWidth(ml);
                    g2.drawString(ml, px[i] - lw / 2, h - 6);
                }
                g2.dispose();
            }
        };
        chart.setOpaque(false);
        chart.setPreferredSize(new Dimension(0, 170));
        wrap.add(chart, BorderLayout.CENTER);
        return wrap;
    }

    // ── Module status panel ───────────────────────────────────────────────
    private JPanel buildModuleStatusPanel() {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(16, 16, 14, 16));

        JLabel ttl = new JLabel("Module Health");
        ttl.setFont(F_SEC);
        ttl.setForeground(TEXT_PRI);
        wrap.add(ttl, BorderLayout.NORTH);

        String[][] modules = {
                { "Products", "📦", "in_stock" },
                { "Sale Details", "🛍", "live" },
                { "Purchase Det.", "🛒", "live" },
                { "Employees", "👥", "active" },
                { "Customers", "🤝", "active" },
                { "Suppliers", "🏭", "active" },
                { "Sale Returns", "↩", "monitored" },
                { "Godown", "🏗", "active" },
        };
        Color[] colors = { ACCENT, ACCENT2, GOLD, PURPLE, ROSE, WARNING, DANGER, new Color(80, 200, 180) };

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setBorder(new EmptyBorder(8, 0, 0, 0));

        for (int i = 0; i < modules.length; i++) {
            final Color col = colors[i % colors.length];
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            JLabel name = new JLabel(modules[i][1] + "  " + modules[i][0]);
            name.setFont(F_SMALL);
            name.setForeground(TEXT_PRI);
            JLabel status = new JLabel("● " + modules[i][2].toUpperCase());
            status.setFont(new Font("Segoe UI", Font.BOLD, 9));
            status.setForeground(col);
            row.add(name, BorderLayout.WEST);
            row.add(status, BorderLayout.EAST);
            list.add(row);
            if (i < modules.length - 1) {
                JSeparator sep = new JSeparator();
                sep.setForeground(BORDER_COL);
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                list.add(sep);
            }
        }
        wrap.add(list, BorderLayout.CENTER);
        return wrap;
    }

    // ── Quick access buttons ──────────────────────────────────────────────
    private JPanel buildQuickAccess() {
        JPanel row = new JPanel(new GridLayout(1, 0, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        Object[][] btns = {
                { "＋ New Sale", ACCENT2, (Runnable) (() -> openInternalFrame(new SaleDetails())) },
                { "＋ New Purchase", GOLD, (Runnable) (() -> openInternalFrame(new PurchaseDetails())) },
                { "👥 Employees", PURPLE, (Runnable) (() -> openInternalFrame(new EmployeeDetails())) },
                { "📦 Products", ACCENT, (Runnable) (() -> openInternalFrame(new ProductDetails())) },
                { "🤝 Customers", ROSE, (Runnable) (() -> openInternalFrame(new CustomerDetails())) },
                { "🏭 Suppliers", WARNING, (Runnable) (() -> openInternalFrame(new SupplierDetails())) },
        };
        for (Object[] b : btns)
            row.add(quickBtn((String) b[0], (Color) b[1], (Runnable) b[2]));
        return row;
    }

    // ── Fix 6: quickBtn — explicit foreground so text can never go white ──
    private JButton quickBtn(String text, Color color, Runnable action) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()
                        ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 90)
                        : getModel().isRollover()
                                ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 55)
                                : new Color(color.getRed(), color.getGreen(), color.getBlue(), 28);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                if (getModel().isRollover() || getModel().isPressed()) {
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 140));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                }
                // Always paint text in the accent color — never white
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(color);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setForeground(color); // explicit — overrides any LAF default
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Dashboard data loading
    // ═════════════════════════════════════════════════════════════════════
    private void loadDashboardData() {
        new Thread(() -> {
            int products = queryCount("SELECT COUNT(*) FROM products WHERE is_active=1");
            int sales = queryCount("SELECT COUNT(*) FROM sale_details");
            int purchases = queryCount("SELECT COUNT(*) FROM purchase_details");
            int employees = queryCount("SELECT COUNT(*) FROM employee_details");
            int customers = queryCount("SELECT COUNT(*) FROM customers WHERE is_active=1");
            int suppliers = queryCount("SELECT COUNT(*) FROM suppliers WHERE is_active=1");
            int saleRet = queryCount("SELECT COUNT(*) FROM sale_returns");
            int purRet = queryCount("SELECT COUNT(*) FROM purchase_return");
            int godowns = queryCount("SELECT COUNT(*) FROM godowns");
            double saleAmt = queryDouble("SELECT NVL(SUM(net_amt),0) FROM sale_details");
            double purAmt = queryDouble("SELECT NVL(SUM(net_amt),0) FROM purchase_details");

            buildMonthLabels();
            for (int i = 0; i < 6; i++) {
                String[] range = monthRange(i);
                saleTrend[i] = queryCount("SELECT COUNT(*) FROM sale_details WHERE sale_date BETWEEN '"
                        + range[0] + "' AND '" + range[1] + "'");
                purchaseTrend[i] = queryCount("SELECT COUNT(*) FROM purchase_details WHERE purchase_date BETWEEN '"
                        + range[0] + "' AND '" + range[1] + "'");
                saleAmtTrend[i] = queryDouble("SELECT NVL(SUM(net_amt),0) FROM sale_details WHERE sale_date BETWEEN '"
                        + range[0] + "' AND '" + range[1] + "'");
            }

            SwingUtilities.invokeLater(() -> {
                lblProducts.setText(String.valueOf(products));
                lblSales.setText(String.valueOf(sales));
                lblPurchases.setText(String.valueOf(purchases));
                lblEmployees.setText(String.valueOf(employees));
                lblCustomers.setText(String.valueOf(customers));
                lblSuppliers.setText(String.valueOf(suppliers));
                lblSaleRet.setText(String.valueOf(saleRet));
                lblPurRet.setText(String.valueOf(purRet));
                lblGodowns.setText(String.valueOf(godowns));
                lblSaleAmt.setText("₹" + fmt(saleAmt));
                lblPurAmt.setText("₹" + fmt(purAmt));
                dashboardPanel.repaint();
            });
        }).start();
    }

    private void buildMonthLabels() {
        Calendar cal = Calendar.getInstance();
        for (int i = 5; i >= 0; i--) {
            cal.setTime(new java.util.Date());
            cal.add(Calendar.MONTH, -i);
            monthLabels[5 - i] = new java.text.SimpleDateFormat("MMM").format(cal.getTime());
        }
    }

    private String[] monthRange(int monthsBack) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -(5 - monthsBack));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String start = new java.text.SimpleDateFormat("dd-MM-yyyy").format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String end = new java.text.SimpleDateFormat("dd-MM-yyyy").format(cal.getTime());
        return new String[] { start, end };
    }

    // ═════════════════════════════════════════════════════════════════════
    // Menus
    // ═════════════════════════════════════════════════════════════════════
    private void buildMenus() {
        menuMaster = darkMenu("Entry");
        menuEmp = darkMenu("Employee");
        menuPurchase = darkMenu("Purchase");
        menuSale = darkMenu("Sale");
        menuQuery = darkMenu("Query");
        menuReport = darkMenu("Report");

        mbr.add(menuMaster);
        mbr.add(menuQuery);
        mbr.add(menuReport);

        menuMaster.add(menuEmp);
        menuMaster.add(menuPurchase);
        menuMaster.add(menuSale);

        menuEmpdetails = darkItem("Employee Details");
        menuEmpAttendence = darkItem("Employee Attendance");
        menuEmpSalary = darkItem("Employee Salary");
        menuEmp.add(menuEmpdetails);
        menuEmp.add(menuEmpAttendence);
        menuEmp.add(menuEmpSalary);

        manuItemPurmaster = darkItem("Purchase Details");
        menuItemreturn = darkItem("Purchase Return");
        menuPurchase.add(manuItemPurmaster);
        menuPurchase.add(menuItemreturn);

        menuItemSalemaster = darkItem("Sale Details");
        menuItemSalereturn = darkItem("Sale Return");
        menuSale.add(menuItemSalemaster);
        menuSale.add(menuItemSalereturn);

        menuProductDetail = darkItem("Products");
        menuCustomerDetail = darkItem("Customer");
        menuItemSupplier = darkItem("Supplier");
        menuItemGodown = darkItem("Godown");
        menuMaster.add(menuCustomerDetail);
        menuMaster.add(menuProductDetail);
        menuMaster.add(menuItemSupplier);
        menuMaster.add(menuItemGodown);

        menuItemEmpQuery = darkItem("Employee Query");
        menuItemSaleQuery = darkItem("Sale Query");
        menuItemPurchaseQuery = darkItem("Purchase Query");
        menuItemGodownQuery = darkItem("Godown Query");
        menuQuery.add(menuItemEmpQuery);
        menuQuery.add(menuItemSaleQuery);
        menuQuery.add(menuItemPurchaseQuery);
        menuQuery.add(menuItemGodownQuery);

        menuItemEmpReport = darkItem("Employee Report");
        menuItemSaleReport = darkItem("Sale Report");
        menuItemPurchaseReport = darkItem("Purchase Report");
        menuItemGodownReport = darkItem("Godown Report");
        menuReport.add(menuItemEmpReport);
        menuReport.add(menuItemSaleReport);
        menuReport.add(menuItemPurchaseReport);
        menuReport.add(menuItemGodownReport);

        for (JMenuItem mi : new JMenuItem[] {
                menuEmpdetails, menuEmpAttendence, menuEmpSalary,
                manuItemPurmaster, menuItemreturn,
                menuItemSalemaster, menuItemSalereturn,
                menuCustomerDetail, menuProductDetail, menuItemSupplier, menuItemGodown,
                menuItemEmpQuery, menuItemSaleQuery, menuItemPurchaseQuery, menuItemGodownQuery,
                menuItemEmpReport, menuItemSaleReport, menuItemPurchaseReport, menuItemGodownReport
        })
            mi.addActionListener(this);
    }

    // ── Fix 7: darkMenu / darkItem — proper selection colors ─────────────
    private JMenu darkMenu(String text) {
        JMenu m = new JMenu(text);
        m.setFont(F_MENU);
        m.setForeground(TEXT_PRI);
        m.setOpaque(false);
        // Popup inherits UIManager colors set in constructor
        m.getPopupMenu().setBackground(BG_CARD);
        m.getPopupMenu().setBorder(BorderFactory.createLineBorder(BORDER_COL));
        return m;
    }

    private JMenuItem darkItem(String text) {
        JMenuItem mi = new JMenuItem(text) {
            // Override to prevent the L&F from overwriting our selection foreground
            @Override
            public Color getForeground() {
                return TEXT_PRI;
            }
        };
        mi.setFont(F_SMALL);
        mi.setForeground(TEXT_PRI);
        mi.setBackground(BG_CARD);
        mi.setOpaque(true);
        return mi;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Action dispatch
    // ═════════════════════════════════════════════════════════════════════
    @Override
    public void actionPerformed(ActionEvent ae) {
        Object s = ae.getSource();
        if (s == menuEmpdetails)
            openInternalFrame(new EmployeeDetails());
        else if (s == menuEmpAttendence)
            openInternalFrame(new EmployeeAttendance());
        else if (s == menuEmpSalary)
            openInternalFrame(new EmployeeSalary());
        else if (s == manuItemPurmaster)
            openInternalFrame(new PurchaseDetails());
        else if (s == menuItemreturn)
            openInternalFrame(new PurchaseReturn());
        else if (s == menuCustomerDetail)
            openInternalFrame(new CustomerDetails());
        else if (s == menuProductDetail)
            openInternalFrame(new ProductDetails());
        else if (s == menuItemSupplier)
            openInternalFrame(new SupplierDetails());
        else if (s == menuItemSalemaster)
            openInternalFrame(new SaleDetails());
        else if (s == menuItemSalereturn)
            openInternalFrame(new SaleReturns());
        else if (s == menuItemGodown)
            openInternalFrame(new GodownDetails());
        else if (s == menuItemEmpQuery)
            openInternalFrame(new EmployeeQuery());
        else if (s == menuItemSaleQuery)
            openInternalFrame(new SaleQuery());
        else if (s == menuItemPurchaseQuery)
            openInternalFrame(new PurchaseQuery());
        else if (s == menuItemGodownQuery)
            openInternalFrame(new GodownReport());
        else if (s == menuItemEmpReport)
            openInternalFrame(new EmployeeReport());
        else if (s == menuItemSaleReport)
            openInternalFrame(new SaleReport());
        else if (s == menuItemPurchaseReport)
            openInternalFrame(new PurchaseReport());
        else if (s == menuItemGodownReport)
            openInternalFrame(new GodownReport());
    }

    // ── Fix 8: Clamp internal frame so it can't slide under the nav bar ──
    private void openInternalFrame(JInternalFrame frame) {
        desktop.add(frame);
        frame.setLocation(80, 20);
        frame.setVisible(true);
        desktop.setLayer(frame, JLayeredPane.MODAL_LAYER);
        try {
            frame.setSelected(true);
        } catch (Exception ignored) {
        }

        // Prevent frame from being dragged above y = 0 (behind nav bar)
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (frame.getY() < 0)
                    frame.setLocation(frame.getX(), 0);
            }
        });

        loadDashboardData();
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI helpers
    // ═════════════════════════════════════════════════════════════════════

    /** Stat card row — height 115 px (was 90, too small for 28pt font + padding) */
    private JPanel rowPanel(int cols) {
        JPanel p = new JPanel(new GridLayout(1, cols, 12, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /** Chart row — taller to give bar/line charts room to breathe */
    private JPanel chartRowPanel(int cols) {
        JPanel p = new JPanel(new GridLayout(1, cols, 12, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_MUT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
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

    private static String fmt(double v) {
        return String.format("%,.0f", v);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MdiForm::new);
    }
}