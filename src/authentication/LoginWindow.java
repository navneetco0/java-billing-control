package authentication;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import javax.imageio.ImageIO;

public class LoginWindow extends JFrame implements ActionListener {

    public static String user = "";

    // ── Palette ───────────────────────────────────────────────────────────
    private static final Color BG_DARK = new Color(7, 11, 22);
    private static final Color BG_CARD = new Color(13, 20, 40);
    private static final Color BG_INPUT = new Color(20, 30, 58);
    private static final Color ACCENT = new Color(56, 176, 240);
    private static final Color GOLD = new Color(255, 195, 50);
    private static final Color TEXT_PRI = new Color(215, 228, 252);
    private static final Color TEXT_MUT = new Color(90, 120, 165);
    private static final Color BORDER_COL = new Color(36, 52, 90);
    private static final Color DANGER = new Color(225, 65, 65);
    private static final Color SUCCESS = new Color(56, 210, 130);

    private static final Font F_BRAND = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font F_SUB = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_LABEL = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font F_INPUT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font F_BTN = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font F_FOOTER = new Font("Segoe UI", Font.PLAIN, 10);

    // ── Components ───────────────────────────────────────────────────────
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblStatus;
    private JProgressBar pbr;

    // ── DB ───────────────────────────────────────────────────────────────
    private Connection conn;

    // ─────────────────────────────────────────────────────────────────────
    public LoginWindow() {
        setUndecorated(true);
        setSize(480, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        connectDB();
        buildUI();
        setVisible(true);
        addWindowDrag();
    }

    // ── DB ───────────────────────────────────────────────────────────────
    private void connectDB() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            conn = DriverManager.getConnection(
                    "jdbc:oracle:thin:@localhost:1521:free", "SYSTEM", "PassTheWord");
        } catch (Exception e) {
            // will surface on login attempt
        }
    }

    // ── Drag support ─────────────────────────────────────────────────────
    private Point dragStart;

    private void addWindowDrag() {
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragStart.x,
                        loc.y + e.getY() - dragStart.y);
            }
        });
    }

    // ── Logo loader ──────────────────────────────────────────────────────
    /**
     * Loads the Bajaj logo, removes its black background, scales it,
     * and returns a JLabel. Falls back to the ⚡ emoji if not found.
     */
    private JLabel buildLogoLabel(int targetW, int targetH) {
        try {
            BufferedImage src = ImageIO.read(getClass().getClassLoader().getResource("assets/bajaj_logo.png"));
            if (src == null)
                throw new Exception("null image");

            // Convert to ARGB and make pure-black pixels transparent
            BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = argb.createGraphics();
            g2.drawImage(src, 0, 0, null);
            g2.dispose();

            // Threshold: pixels darker than this become transparent
            for (int y = 0; y < argb.getHeight(); y++) {
                for (int x = 0; x < argb.getWidth(); x++) {
                    int rgba = argb.getRGB(x, y);
                    int r = (rgba >> 16) & 0xFF;
                    int gv = (rgba >> 8) & 0xFF;
                    int b = rgba & 0xFF;
                    if (r < 30 && gv < 30 && b < 30) {
                        argb.setRGB(x, y, 0x00000000); // fully transparent
                    }
                }
            }

            Image scaled = argb.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
            JLabel lbl = new JLabel(new ImageIcon(scaled));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            return lbl;
        } catch (Exception ex) {
            // Fallback: emoji
            JLabel fallback = new JLabel("⚡");
            fallback.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 38));
            fallback.setAlignmentX(Component.CENTER_ALIGNMENT);
            return fallback;
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────
    private void buildUI() {
        setShape(new RoundRectangle2D.Double(0, 0, 480, 620, 24, 24));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, BG_DARK, 0, getHeight(), new Color(10, 18, 38)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(56, 176, 240, 18));
                g2.fillOval(260, -120, 340, 340);
                g2.setColor(new Color(255, 195, 50, 10));
                g2.fillOval(-80, 360, 280, 280);
                g2.setColor(new Color(56, 176, 240, 22));
                for (int x = 0; x < getWidth(); x += 28)
                    for (int y = 0; y < getHeight(); y += 28)
                        g2.fillOval(x, y, 2, 2);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createLineBorder(new Color(36, 58, 100), 1));
        setContentPane(root);

        // ── Top bar (close/min) ──────────────────────────────────────────
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        topBar.setOpaque(false);
        JButton btnClose = winBtn("✕", DANGER);
        JButton btnMin = winBtn("–", TEXT_MUT);
        btnClose.addActionListener(e -> System.exit(0));
        btnMin.addActionListener(e -> setState(ICONIFIED));
        topBar.add(btnMin);
        topBar.add(btnClose);
        root.add(topBar, BorderLayout.NORTH);

        // ── Center card ─────────────────────────────────────────────────
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                // top accent line — blue to gold gradient
                g2.setPaint(new GradientPaint(0, 0, ACCENT, getWidth(), 0, GOLD));
                g2.fillRect(32, 0, getWidth() - 64, 3);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(28, 40, 32, 40));

        // ── Bajaj logo (replaces ⚡ emoji) ─────────────────────────────
        // Logo is 80 wide × 96 tall to respect the portrait aspect ratio
        JLabel logoLabel = buildLogoLabel(80, 96);

        JLabel brand = new JLabel("BAJAJ Enterprises");
        brand.setFont(F_BRAND);
        brand.setForeground(TEXT_PRI);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Billing Control Automation System");
        sub.setFont(F_SUB);
        sub.setForeground(TEXT_MUT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Admin badge
        JLabel adminBadge = new JLabel("  🔐  ADMIN ACCESS  ");
        adminBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        adminBadge.setForeground(GOLD);
        adminBadge.setOpaque(true);
        adminBadge.setBackground(new Color(60, 44, 8));
        adminBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        adminBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COL);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        // Password label + field
        JLabel lblPwd = fieldLabel("PASSWORD");
        txtPassword = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        txtPassword.setFont(F_INPUT);
        txtPassword.setBackground(new Color(0, 0, 0, 0));
        txtPassword.setOpaque(false);
        txtPassword.setForeground(TEXT_PRI);
        txtPassword.setCaretColor(ACCENT);
        txtPassword.setEchoChar('●');
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1, true),
                new EmptyBorder(10, 14, 10, 14)));
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        txtPassword.addActionListener(e -> doLogin());

        txtPassword.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                txtPassword.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT, 1, true),
                        new EmptyBorder(10, 14, 10, 14)));
            }

            public void focusLost(FocusEvent e) {
                txtPassword.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COL, 1, true),
                        new EmptyBorder(10, 14, 10, 14)));
            }
        });

        // Status label
        lblStatus = new JLabel(" ");
        lblStatus.setFont(F_SUB);
        lblStatus.setForeground(DANGER);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Progress bar
        pbr = new JProgressBar(0, 100);
        pbr.setStringPainted(false);
        pbr.setOpaque(false);
        pbr.setBorderPainted(false);
        pbr.setBackground(BG_INPUT);
        pbr.setForeground(ACCENT);
        pbr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        pbr.setVisible(false);
        pbr.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected Color getSelectionForeground() {
                return ACCENT;
            }

            @Override
            protected Color getSelectionBackground() {
                return ACCENT;
            }
        });

        // Login button
        btnLogin = new JButton("LOGIN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isPressed()
                        ? new Color(30, 120, 200)
                        : getModel().isRollover()
                                ? new Color(70, 190, 255)
                                : ACCENT;
                g2.setPaint(new GradientPaint(0, 0, base, 0, getHeight(), base.darker()));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(56, 176, 240, 40));
                    g2.setStroke(new BasicStroke(6));
                    g2.drawRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 10, 10);
                }
                g2.setFont(F_BTN);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btnLogin.setOpaque(false);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(this);

        // Assemble card
        card.add(logoLabel); // ← Bajaj logo
        card.add(Box.createVerticalStrut(10));
        card.add(brand);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(14));
        card.add(adminBadge);
        card.add(Box.createVerticalStrut(22));
        card.add(sep);
        card.add(Box.createVerticalStrut(26));
        card.add(lblPwd);
        card.add(Box.createVerticalStrut(6));
        card.add(txtPassword);
        card.add(Box.createVerticalStrut(8));
        card.add(pbr);
        card.add(Box.createVerticalStrut(8));
        card.add(lblStatus);
        card.add(Box.createVerticalStrut(20));
        card.add(btnLogin);

        // Wrap card with padding
        JPanel cardWrap = new JPanel(new GridBagLayout());
        cardWrap.setOpaque(false);
        cardWrap.add(card, new GridBagConstraints() {
            {
                fill = BOTH;
                weightx = 1;
                weighty = 1;
                insets = new Insets(20, 36, 20, 36);
            }
        });
        root.add(cardWrap, BorderLayout.CENTER);

        // ── Footer ──────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        footer.setOpaque(false);
        JLabel footLbl = new JLabel("© 2025 Bajaj Enterprises  ·  v2.0");
        footLbl.setFont(F_FOOTER);
        footLbl.setForeground(new Color(55, 78, 115));
        footer.add(footLbl);
        root.add(footer, BorderLayout.SOUTH);
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL);
        l.setForeground(TEXT_MUT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JButton winBtn(String text, Color fg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 40));
                    g2.fillOval(0, 0, getWidth(), getHeight());
                }
                g2.setFont(getFont());
                g2.setColor(getModel().isRollover() ? fg : TEXT_MUT);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(28, 28));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Login logic ──────────────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btnLogin)
            doLogin();
    }

    private void doLogin() {
        String pwd = new String(txtPassword.getPassword());
        if (pwd.isEmpty()) {
            flash("Enter password", DANGER);
            return;
        }
        btnLogin.setEnabled(false);
        pbr.setValue(0);
        pbr.setVisible(true);
        lblStatus.setText(" ");

        new Thread(() -> {
            try {
                for (int i = 0; i <= 80; i += 5) {
                    final int v = i;
                    SwingUtilities.invokeLater(() -> pbr.setValue(v));
                    Thread.sleep(60);
                }
                boolean ok = false;
                if (conn != null) {
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM login_window WHERE UPPER(user_type)='ADMIN' AND password=?");
                    ps.setString(1, pwd);
                    ResultSet rs = ps.executeQuery();
                    ok = rs.next();
                }
                final boolean success = ok;
                for (int i = 80; i <= 100; i += 4) {
                    final int v = i;
                    SwingUtilities.invokeLater(() -> pbr.setValue(v));
                    Thread.sleep(30);
                }
                Thread.sleep(120);
                SwingUtilities.invokeLater(() -> {
                    pbr.setVisible(false);
                    btnLogin.setEnabled(true);
                    if (success) {
                        user = "ADMIN";
                        flash("Access granted!", SUCCESS);
                        pbr.setForeground(SUCCESS);
                        new javax.swing.Timer(600, ev -> {
                            ((javax.swing.Timer) ev.getSource()).stop();
                            forms.MdiForm mdi = new forms.MdiForm();
                            mdi.setVisible(true);
                            dispose();
                        }).start();
                    } else {
                        flash("Invalid password — Admin access only", DANGER);
                        txtPassword.setText("");
                        pbr.setForeground(DANGER);
                        pbr.setVisible(true);
                        pbr.setValue(100);
                        new javax.swing.Timer(800, ev -> {
                            ((javax.swing.Timer) ev.getSource()).stop();
                            pbr.setVisible(false);
                            pbr.setForeground(ACCENT);
                            pbr.setValue(0);
                        }).start();
                        shakeWindow();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    pbr.setVisible(false);
                    btnLogin.setEnabled(true);
                    flash("DB error: " + ex.getMessage(), DANGER);
                });
            }
        }).start();
    }

    private void flash(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }

    private void shakeWindow() {
        Point orig = getLocation();
        new Thread(() -> {
            try {
                int[] deltas = { -10, 10, -8, 8, -5, 5, -2, 2, 0 };
                for (int d : deltas) {
                    final int fd = d;
                    SwingUtilities.invokeLater(() -> setLocation(orig.x + fd, orig.y));
                    Thread.sleep(40);
                }
                SwingUtilities.invokeLater(() -> setLocation(orig));
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginWindow::new);
    }
}