package forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class MdiForm extends JFrame implements ActionListener {
    public static Connection sconnect;
    private JMenuBar mbr;
    private JMenu menuMaster, menuEmp, menuPurchase, menuSale, menuQuery, menuReport;
    private JMenuItem menuEmpdetails, menuEmpAttendence, menuEmpSalary;
    private JMenuItem menuItemItemDtl, menuItemSupplier, menuItemGodown, menuItemStock;
    private JMenuItem manuItemPurmaster, menuItemreturn, menuItemSalemaster, menuItemSalereturn;
    private JMenuItem menuItemEmpQuery, menuItemSaleQuery, menuItemPurchaseQuery, menuItemGodownQuery;
    private JMenuItem menuItemEmpReport, menuItemSaleReport, menuItemPurchaseReport, menuItemGodownReport;
    private JDesktopPane desktop;
    private Container c;
    private MdiFormBackground obj1;
    private MdiFormHeading obj2;

    public void initializeDatabase() {
        try {
            // 1. Check and Create 'login_window'
            if (!tableExists("LOGIN_WINDOW")) {
                executeSQL("CREATE TABLE login_window (user_type VARCHAR2(100), password VARCHAR2(100))");
                executeSQL("INSERT INTO login_window VALUES ('ADMIN', 'ADMIN123')");
                System.out.println("Login table created and initialized.");
            }

            // 2. Check and Create 'employee_details'
            if (!tableExists("EMPLOYEE_DETAILS")) {
                String sql = "CREATE TABLE employee_details ("
                        + "emp_id VARCHAR2(20) PRIMARY KEY, "
                        + "emp_name VARCHAR2(100), "
                        + "gender VARCHAR2(10), "
                        + "dob VARCHAR2(20), "
                        + "doj VARCHAR2(20), "
                        + "address VARCHAR2(200), "
                        + "phone VARCHAR2(15), "
                        + "mobile_no VARCHAR2(15), "
                        + "email_id VARCHAR2(100), "
                        + "bsalary NUMBER(10, 2), "
                        + "narration VARCHAR2(200))";
                executeSQL(sql);
                System.out.println("Employee table created.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper method to check if a table exists in Oracle
    private boolean tableExists(String tableName) throws SQLException {
        // Oracle stores table names in UPPERCASE by default
        String sql = "SELECT count(*) FROM user_tables WHERE table_name = '" + tableName.toUpperCase() + "'";
        try (Statement st = sconnect.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // Helper method to run an update
    private void executeSQL(String sql) throws SQLException {
        try (Statement st = sconnect.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private class MdiFormBackground extends JPanel {
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Image img1 = Toolkit.getDefaultToolkit()
                    .getImage(getClass().getResource("/assets/home_appliances.jpg"));
            MediaTracker track1 = new MediaTracker(this);
            track1.addImage(img1, 0);
            try {
                track1.waitForID(0);
            } catch (InterruptedException e) {
            }

            // ✅ FIX: Use getWidth()/getHeight() so image fills whatever size desktop is
            g.drawImage(img1, 0, 0, getWidth(), getHeight(), this);

            g.setFont(new Font("Lucida Calligraphy", Font.BOLD, 16));
            g.setColor(Color.RED);
            g.drawString("* Developed by Navneet Kumar", getWidth() - 400, getHeight() - 80);
            g.drawString("* Enrollment No. - 2300025744", getWidth() - 400, getHeight() - 50);
        }
    }

    private class MdiFormHeading extends JPanel {
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(173, 216, 230));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            setBackground(Color.pink);
            g.setFont(new Font("Lucida Calligraphy", Font.BOLD, 25));
            g.drawString("Billing Control Automation System for BAJAJ Enterprises", 40, 50);
            setForeground(Color.white);
        }
    }

    public MdiForm() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            String url = "jdbc:oracle:thin:@localhost:1521:free";
            String userDB = "SYSTEM";
            String passDB = "PassTheWord";
            sconnect = DriverManager.getConnection(url, userDB, passDB);
            initializeDatabase();
            System.out.println("Connected to the database successfully!");

            mbr = new JMenuBar();
            desktop = new JDesktopPane();

            c = getContentPane();
            c.setLayout(new BorderLayout());

            obj1 = new MdiFormBackground();
            obj2 = new MdiFormHeading();

            obj2.setPreferredSize(new Dimension(1100, 100));

            setJMenuBar(mbr);

            desktop.setOpaque(false);

            // Background panel fills the desktop
            obj1.setBounds(0, 0, 3000, 3000);
            desktop.add(obj1, JLayeredPane.FRAME_CONTENT_LAYER);

            // ✅ Add each component ONCE only
            c.add(obj2, BorderLayout.NORTH);
            c.add(desktop, BorderLayout.CENTER);

            menuMaster = new JMenu("Entry");
            mbr.add(menuMaster);
            menuEmp = new JMenu("Employee");
            menuMaster.add(menuEmp);
            menuPurchase = new JMenu("Purchase");
            menuMaster.add(menuPurchase);
            menuSale = new JMenu("Sale");
            menuMaster.add(menuSale);
            menuQuery = new JMenu("Query");
            mbr.add(menuQuery);
            menuItemEmpQuery = new JMenuItem("Employee Query");
            menuQuery.add(menuItemEmpQuery);
            menuItemSaleQuery = new JMenuItem("Sale Query");
            menuQuery.add(menuItemSaleQuery);
            menuItemPurchaseQuery = new JMenuItem("Purchase Query");
            menuQuery.add(menuItemPurchaseQuery);
            menuItemGodownQuery = new JMenuItem("Godown Query");
            menuQuery.add(menuItemGodownQuery);
            menuReport = new JMenu("Report");
            mbr.add(menuReport);

            menuItemEmpReport = new JMenuItem("Employee Report");
            menuReport.add(menuItemEmpReport);
            menuItemSaleReport = new JMenuItem("Sale Report");
            menuReport.add(menuItemSaleReport);
            menuItemPurchaseReport = new JMenuItem("Purchase Report");
            menuReport.add(menuItemPurchaseReport);
            menuItemGodownReport = new JMenuItem("Godown Report");
            menuReport.add(menuItemGodownReport);

            menuEmpdetails = new JMenuItem("Employee Details");
            menuEmp.add(menuEmpdetails);
            menuEmpAttendence = new JMenuItem("Employee Attendence");
            menuEmp.add(menuEmpAttendence);
            menuEmpSalary = new JMenuItem("Employee Salary");
            menuEmp.add(menuEmpSalary);
            manuItemPurmaster = new JMenuItem("Purchase Details");
            menuPurchase.add(manuItemPurmaster);
            menuItemreturn = new JMenuItem("Purchase Return");
            menuPurchase.add(menuItemreturn);
            menuItemSalemaster = new JMenuItem("Sale Details");
            menuSale.add(menuItemSalemaster);
            menuItemSalereturn = new JMenuItem("Sale Return");
            menuSale.add(menuItemSalereturn);

            menuItemItemDtl = new JMenuItem("Item");
            menuMaster.add(menuItemItemDtl);
            menuItemSupplier = new JMenuItem("Supplier");
            menuMaster.add(menuItemSupplier);
            menuItemGodown = new JMenuItem("Godown");
            menuMaster.add(menuItemGodown);
            menuItemStock = new JMenuItem("Stock");
            menuMaster.add(menuItemStock);

            menuEmpdetails.addActionListener(this);
            menuEmpAttendence.addActionListener(this);
            menuEmpSalary.addActionListener(this);
            manuItemPurmaster.addActionListener(this);
            menuItemreturn.addActionListener(this);
            menuItemSalemaster.addActionListener(this);
            menuItemSalereturn.addActionListener(this);
            menuItemEmpQuery.addActionListener(this);
            menuItemSaleQuery.addActionListener(this);
            menuItemPurchaseQuery.addActionListener(this);
            menuItemGodownQuery.addActionListener(this);
            menuItemEmpReport.addActionListener(this);
            menuItemSaleReport.addActionListener(this);
            menuItemPurchaseReport.addActionListener(this);
            menuItemGodownReport.addActionListener(this);
            menuItemStock.addActionListener(this);
            menuItemEmpQuery.addActionListener(this);
            menuItemSaleQuery.addActionListener(this);
            menuItemPurchaseQuery.addActionListener(this);
            menuItemEmpReport.addActionListener(this);
            menuItemSaleReport.addActionListener(this);
            menuItemPurchaseReport.addActionListener(this);
            menuItemItemDtl.addActionListener(this);
            menuItemSupplier.addActionListener(this);
            menuItemGodown.addActionListener(this);

            setVisible(true);
            setExtendedState(MAXIMIZED_BOTH);
            setResizable(false);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Unable To Load Driver1: " + e.getMessage(), "MDI Form", 0);
            System.exit(0);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Unable To Load Driver2: " + e.getMessage(), "MDI Form", 0);
            System.exit(0);
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (menuEmpdetails.isArmed()) {
            EmployeeDetails employeeDetails = new EmployeeDetails();
            desktop.add(employeeDetails);
            employeeDetails.setLocation(180, 110);
            try {
                employeeDetails.setSelected(true);
            } catch (java.beans.PropertyVetoException j) {
            }
        } else if (menuEmpAttendence.isArmed()) {
            EmployeeAttendence employeeAttendence = new EmployeeAttendence();
            desktop.add(employeeAttendence);
            employeeAttendence.setLocation(180, 110);
            try {
                employeeAttendence.setSelected(true);
            } catch (java.beans.PropertyVetoException j) {
            }
        }
        // else if(menuEmpSalary.isArmed()){
        // EmployeeSalary employeeSalary = new EmployeeSalary();
        // desktop.add(employeeSalary);
        // employeeSalary.setLocation(180, 110);
        // try {
        // employeeSalary.setSelected(true);
        // } catch (java.beans.PropertyVetoException j) {}
        // }
        else if (manuItemPurmaster.isArmed()) {
            PurchaseDetails purchaseDetails = new PurchaseDetails();
            desktop.add(purchaseDetails);
            purchaseDetails.setLocation(180, 110);
            try {
                purchaseDetails.setSelected(true);
            } catch (java.beans.PropertyVetoException j) {
            }
        } else if (menuItemreturn.isArmed()) {
            PurchaseReturn purchaseReturn = new PurchaseReturn();
            desktop.add(purchaseReturn);
            purchaseReturn.setLocation(180, 110);
            try {
                purchaseReturn.setSelected(true);
            } catch (java.beans.PropertyVetoException j) {
            }
        } else if (menuItemItemDtl.isArmed()) {
            ItemDetails itemDetails = new ItemDetails();
            desktop.add(itemDetails);
            itemDetails.setLocation(180, 110);
            try {
                itemDetails.setSelected(true);
            } catch (java.beans.PropertyVetoException j) {
            }
        } else if (menuItemSupplier.isArmed()) {
            SupplierDetails supplierDetails = new SupplierDetails();
            desktop.add(supplierDetails);
            supplierDetails.setLocation(180, 110);
            try {
                supplierDetails.setSelected(true);
            } catch (java.beans.PropertyVetoException j) {
            }
        } else if (menuItemSalemaster.isArmed()) {
            SaleDetails saleDetails = new SaleDetails();
            desktop.add(saleDetails);
            saleDetails.setLocation(180, 110);
            try {
                saleDetails.setSelected(true);
            } catch (java.beans.PropertyVetoException j) {
            }
        } else if (menuItemSalereturn.isArmed()) {
            SaleReturn saleReturn = new SaleReturn();
            desktop.add(saleReturn);
            saleReturn.setLocation(180, 110);
            try {
                saleReturn.setSelected(true);
            } catch (java.beans.PropertyVetoException j) {
            }
        } else if (menuItemGodown.isArmed()) {
            GodownDetails godownDetails = new GodownDetails();
            desktop.add(godownDetails);
            godownDetails.setLocation(180, 110);
            try {
                godownDetails.setSelected(true);
            } catch (java.beans.PropertyVetoException j) {
            }
        }
        // else if(menuItemEmpQuery.isArmed()){
        // EmployeeQuery employeeQuery = new EmployeeQuery();
        // desktop.add(employeeQuery);
        // employeeQuery.setLocation(180, 110);
        // try {
        // employeeQuery.setSelected(true);
        // } catch (java.beans.PropertyVetoException j) {}
        // }
        // else if(menuItemSaleQuery.isArmed()){
        // SaleQuery saleQuery = new SaleQuery();
        // desktop.add(saleQuery);
        // saleQuery.setLocation(180, 110);
        // try {
        // saleQuery.setSelected(true);
        // } catch (java.beans.PropertyVetoException j) {}
        // }
        // else if(menuItemPurchaseQuery.isArmed()){
        // PurchaseQuery purchaseQuery = new PurchaseQuery();
        // desktop.add(purchaseQuery);
        // purchaseQuery.setLocation(180, 110);
        // try {
        // purchaseQuery.setSelected(true);
        // } catch (java.beans.PropertyVetoException j) {}
        // }
        // else if(menuItemEmpReport.isArmed()){
        // EmployeeReport employeeReport = new EmployeeReport();
        // desktop.add(employeeReport);
        // employeeReport.setLocation(180, 110);
        // try {
        // employeeReport.setSelected(true);
        // } catch (java.beans.PropertyVetoException j) {}
        // }
        // else if(menuItemSaleReport.isArmed()){
        // SaleReport saleReport = new SaleReport();
        // desktop.add(saleReport);
        // saleReport.setLocation(180, 110);
        // try {
        // saleReport.setSelected(true);
        // } catch (java.beans.PropertyVetoException j) {}
        // }
        // else if(menuItemPurchaseReport.isArmed()){
        // PurchaseReport purchaseReport = new PurchaseReport();
        // desktop.add(purchaseReport);
        // purchaseReport.setLocation(180, 110);
        // try {
        // purchaseReport.setSelected(true);
        // } catch (java.beans.PropertyVetoException j) {}
        // }
        // else if(menuItemPurchaseReport.isArmed()){
        // PurchaseReport purchaseReport = new PurchaseReport();
        // desktop.add(purchaseReport);
        // purchaseReport.setLocation(180, 110);
        // try {
        // purchaseReport.setSelected(true);
        // } catch (java.beans.PropertyVetoException j) {}
        // }
    }

    public static void main(String[] args) {
        new MdiForm();
    }
}