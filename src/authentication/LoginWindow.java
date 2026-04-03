package authentication;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginWindow extends JFrame implements Runnable, ActionListener {
    public static String user = "";
    String pass = "";
    private JLabel lblUserType, lblPassword;
    private JComboBox<String> cmbUserType;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnCancel;
    private JProgressBar pbr;
    private Thread thread1;
    private Container c;
    private Connection conn;
    private ResultSet srecord;
    private Statement stmt;
    private boolean flag = false;

    public void connectDB() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");

            String url = "jdbc:oracle:thin:@localhost:1521:free";
            conn = DriverManager.getConnection(url, "SYSTEM", "PassTheWord");

            System.out.println("Connected!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
        }
    }

    public LoginWindow() {

        c = getContentPane();
        c.setLayout(null);
        lblUserType = new JLabel("User Type");
        cmbUserType = new JComboBox<>(new String[] { "ADMIN", "Employee", "Guest" });
        lblPassword = new JLabel("Password");
        txtPassword = new JPasswordField();

        pbr = new JProgressBar();
        pbr.setStringPainted(true);
        btnLogin = new JButton("Login");
        btnCancel = new JButton("Cancel");

        cmbUserType.setEditable(true);
        cmbUserType.setSelectedItem("");
        cmbUserType.setEditable(false);
        lblUserType.setBounds(85, 50, 100, 30);
        cmbUserType.setBounds(195, 50, 100, 30);

        lblPassword.setBounds(85, 90, 100, 30);
        txtPassword.setBounds(195, 90, 100, 30);
        pbr.setBounds(50, 130, 280, 30);
        btnLogin.setBounds(90, 170, 100, 30);
        btnCancel.setBounds(200, 170, 100, 30);
        c.add(lblUserType);
        c.add(cmbUserType);
        c.add(lblPassword);
        c.add(txtPassword);
        c.add(pbr);
        c.add(btnLogin);
        c.add(btnCancel);
        btnLogin.addActionListener(this);
        btnCancel.addActionListener(this);
        setSize(450, 300);
        setLocation(300, 200);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setTitle("Billing Control Automation System for BAJAJ Enterprises");
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btnCancel) {
            System.exit(0);
        } else if (ae.getSource() == btnLogin) {
            thread1 = new Thread(this);
            flag = true;
            thread1.start();
        }
    }

    @Override
    public void run() {
        try {
            while (flag) {
                if (pbr.getValue() < 100) {
                    pbr.setValue(pbr.getValue() + 5);
                    Thread.sleep(200);
                } else {
                    if (conn == null) {
                        JOptionPane.showMessageDialog(this, "Not connected to DB yet. Please wait.", "Login Window", 0);
                        pbr.setValue(0);
                        flag = false;
                        return;
                    }
                    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    srecord = stmt.executeQuery("SELECT * FROM login_window");
                    boolean flag1 = false;
                    String utype = cmbUserType.getSelectedItem().toString();
                    String upass = "";
                    while (srecord.next()) {
                        utype = srecord.getString(1);
                        upass = srecord.getString(2);
                        if (utype.equals(cmbUserType.getSelectedItem().toString())
                                && upass.equals(new String(txtPassword.getPassword()))) {
                            user = utype;
                            flag1 = true;
                            break;
                        }
                    }
                    if (flag1) {
                        JOptionPane.showMessageDialog(this, "Login", "Login Window", 1);
                        forms.MdiForm mdi = new forms.MdiForm();
                        mdi.setVisible(true);
                        flag = false;
                        this.dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid User Type/Password", "Login Window", 0);
                        cmbUserType.setEditable(true);
                        cmbUserType.setSelectedItem("");
                        cmbUserType.setEditable(false);
                        txtPassword.setText("");
                        pbr.setValue(0);
                        flag = false;
                    }
                }
            }
        } catch (InterruptedException ie) {
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Unable To Create Connection Object:  " + e.getMessage(),
                    "Login Window", 0);
        }
    }

    public static void main(String[] args) {
        LoginWindow loginWindow = new LoginWindow();
        loginWindow.setTitle("Login Window");
        loginWindow.setSize(400, 300);
        loginWindow.setLocationRelativeTo(null);
    }
}
