package forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class EmployeeAttendence extends JInternalFrame implements MouseListener, ActionListener {
    private ResultSet srecord;
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot, cur;

    private JLabel lblEmpid, lblName, lblGender, lblDob, lblDoj, lblAddr, lblPhone, lblMobile, lblEmail, lblBSal, lblNarr;
    private JTextField txtEmpID, txtName, txtDob, txtDoj, txtPhone, txtMobile, txtEmail, txtBsal;
    private JComboBox<String> cmbGender;
    private JTextArea txtAddr, txtNarr;
    private JScrollPane jscAddr, jscNarr;
    private JButton btnSave, btnUpdate, btnDelete, btnFind, btnRefresh;
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JTextField txtNavigation;
    private Container c;
    private EmpDtlBackground obj1;
    private EmpDtlHeading obj2;

    private class EmpDtlBackground extends JPanel {
        private EmpDtlBackground() {
            setLayout(null);
            lblEmpid = new JLabel("Employee ID");
            lblName = new JLabel("Name");
            lblGender = new JLabel("Gender");
            lblDob = new JLabel("Date of Birth");
            lblDoj = new JLabel("Date of Join");
            lblAddr = new JLabel("Address");
            lblPhone = new JLabel("Phone No");
            lblMobile = new JLabel("Mobile No");
            lblEmail = new JLabel("Email Id");
            lblBSal = new JLabel("Basic Salary");
            lblNarr = new JLabel("Narration");

            txtEmpID = new JTextField();
            txtName = new JTextField();
            txtDob = new JTextField();
            cmbGender = new JComboBox<>(new String[] {
                    "Male", "Female"
            });
            txtDoj = new JTextField();
            txtPhone = new JTextField();
            txtAddr = new JTextArea();
            jscAddr = new JScrollPane(txtAddr);
            txtMobile = new JTextField();
            txtEmail = new JTextField();
            txtBsal = new JTextField();
            txtNarr = new JTextArea();
            jscNarr = new JScrollPane(txtNarr);

            btnSave = new JButton("Save");
            btnDelete = new JButton("Delete");
            btnUpdate = new JButton("Update");
            btnFind = new JButton("Find");
            btnRefresh = new JButton("Refresh");
            btnFirst = new JButton("|<<");
            btnPrev = new JButton("<<");
            txtNavigation=new JTextField();
            btnNext = new JButton(">>");
            btnLast = new JButton(">>|");

            lblEmpid.setBounds(10, 10, 100, 30);
            txtEmpID.setBounds(120, 10, 200, 30);
            lblName.setBounds(330, 10, 100, 30);
            txtName.setBounds(440, 10, 200, 30);
            lblGender.setBounds(10, 50, 100, 30);
            cmbGender.setBounds(120, 50, 200, 30);
            lblDob.setBounds(330, 50, 100, 30);
            txtDob.setBounds(440, 50, 200, 30);
            lblDoj.setBounds(10, 90, 100, 30);
            txtDoj.setBounds(120, 90, 200, 30);
            lblAddr.setBounds(330, 90, 100, 30);
            jscAddr.setBounds(440, 90, 200, 60);
            lblPhone.setBounds(10, 130, 100, 30);
            txtPhone.setBounds(120, 130, 200, 30);
            lblMobile.setBounds(330, 160, 100, 30);
            txtMobile.setBounds(440, 160, 200, 30);
            lblEmail.setBounds(10, 170, 100, 30);
            txtEmail.setBounds(120, 170, 200, 30);
            lblBSal.setBounds(330, 200, 100, 30);
            txtBsal.setBounds(440, 200, 200, 30);
            lblNarr.setBounds(10, 210, 100, 30);
            jscNarr.setBounds(120, 210, 200, 60);
            btnSave.setBounds(10, 280, 80, 30);
            btnUpdate.setBounds(100, 280, 80, 30);
            btnDelete.setBounds(190, 280, 80, 30);
            btnFind.setBounds(280, 280, 80, 30);
            btnRefresh.setBounds(370, 280, 80, 30);
            btnFirst.setBounds(10, 320, 80, 30);
            btnPrev.setBounds(100, 320, 80, 30);
            btnNext.setBounds(190, 320, 80, 30);
            btnLast.setBounds(280, 320, 80, 30);
            txtNavigation.setBounds(370, 320, 80, 30);
            add(lblEmpid);
            add(txtEmpID);
            add(lblName);
            add(txtName);
            add(lblGender);
            add(cmbGender);
            add(lblDob);
            add(txtDob);
            add(lblDoj);
            add(txtDoj);
            add(lblAddr);
            add(jscAddr);
            add(lblPhone);
            add(txtPhone);
            add(lblMobile);
            add(txtMobile);
            add(lblEmail);
            add(txtEmail);
            add(lblBSal);
            add(txtBsal);
            add(lblNarr);
            add(jscNarr);
            add(btnSave);
            add(btnUpdate);
            add(btnDelete);
            add(btnFind);
            add(btnRefresh);
            add(btnFirst);
            add(btnPrev);
            add(btnNext);
            add(btnLast);
            add(txtNavigation);
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(173, 216, 230));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private class EmpDtlHeading extends JPanel {
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(70, 130, 180));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Employee Details", 20, 30);
        }
    }

    public EmployeeAttendence() {
        super("Employee Attendance", false, true, false, true);
        c = getContentPane();
        c.setLayout(null);

        obj1 = new EmpDtlBackground();
        obj2 = new EmpDtlHeading();
        obj1.setBounds(0, 50, 660, 400);
        obj2.setBounds(0, 0, 660, 50);
        c.add(obj1);
        c.add(obj2);
        btnSave.addMouseListener(this);
        btnUpdate.addMouseListener(this);
        btnDelete.addMouseListener(this);
        btnFind.addMouseListener(this);
        btnRefresh.addMouseListener(this);

        btnFirst.addMouseListener(this);
        btnPrev.addMouseListener(this);
        btnNext.addMouseListener(this);
        btnLast.addMouseListener(this);

        btnSave.addActionListener(this);
        btnUpdate.addActionListener(this);
        btnDelete.addActionListener(this);
        btnFind.addActionListener(this);
        btnRefresh.addActionListener(this);

        btnFirst.addActionListener(this);
        btnPrev.addActionListener(this);
        btnNext.addActionListener(this);
        btnLast.addActionListener(this);

        setVisible(true);
        setSize(660, 530);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        cmbGender.addItem("Male");
        cmbGender.addItem("Female");
        cmbGender.setEditable(true);
        cmbGender.setSelectedItem("");
        cmbGender.setEditable(false);
        try {
            String str = "select * from employee_details";
            pstmt = MdiForm.sconnect.prepareStatement(str);
            srecord = pstmt.executeQuery();
            while (srecord.next())
                tot++;
            if (tot > 0)
                cur = 1;

            showRecord(cur);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Employee Details", 0);
        }
    }

    public void mouseExited(MouseEvent me) {
        if (me.getSource() == btnSave) {
            btnSave.setForeground(Color.darkGray);
        } else if (me.getSource() == btnDelete) {
            btnDelete.setForeground(Color.darkGray);
        } else if (me.getSource() == btnUpdate) {
            btnUpdate.setForeground(Color.darkGray);
        } else if (me.getSource() == btnFind) {
            btnFind.setForeground(Color.darkGray);
        } else if (me.getSource() == btnRefresh) {
            btnRefresh.setForeground(Color.darkGray);
        } else if (me.getSource() == btnFirst) {
            btnFind.setForeground(Color.darkGray);
        } else if (me.getSource() == btnPrev) {
            btnPrev.setForeground(Color.darkGray);
        } else if (me.getSource() == btnNext) {
            btnNext.setForeground(Color.darkGray);
        } else if (me.getSource() == btnLast) {
            btnLast.setForeground(Color.darkGray);
        }
    }

    public void mouseEntered(MouseEvent me) {
        if (me.getSource() == btnSave) {
            btnSave.setForeground(Color.red);
        } else if (me.getSource() == btnDelete) {
            btnDelete.setForeground(Color.red);
        } else if (me.getSource() == btnUpdate) {
            btnUpdate.setForeground(Color.red);
        } else if (me.getSource() == btnFind) {
            btnFind.setForeground(Color.red);
        } else if (me.getSource() == btnRefresh) {
            btnRefresh.setForeground(Color.red);
        } else if (me.getSource() == btnFirst) {
            btnFind.setForeground(Color.red);
        } else if (me.getSource() == btnPrev) {
            btnPrev.setForeground(Color.red);
        } else if (me.getSource() == btnNext) {
            btnNext.setForeground(Color.red);
        } else if (me.getSource() == btnLast) {
            btnLast.setForeground(Color.red);
        }
    }

    public void mouseClicked(MouseEvent me) {
    }

    public void mouseReleased(MouseEvent me) {
    }

    public void mousePressed(MouseEvent me) {
    }

    public void showRecord(int ctr) {
        if (ctr > 0) {
            try {
                stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                String str = "select * from employee_details";
                srecord = stmt.executeQuery(str);
                srecord.absolute(ctr);
                txtEmpID.setText(srecord.getString("emp_id"));
                txtName.setText(srecord.getString("emp_name"));
                cmbGender.setEditable(true);
                cmbGender.setSelectedItem(srecord.getString("gender"));
                cmbGender.setEditable(false);
                txtDob.setText(srecord.getString("dob"));
                txtDoj.setText(srecord.getString("doj"));
                txtAddr.setText(srecord.getString("address"));
                txtPhone.setText(srecord.getString("phone"));

                txtMobile.setText(srecord.getString("mobile_no"));
                txtEmail.setText(srecord.getString("email_id"));
                txtBsal.setText(srecord.getString("bsalary"));
                txtNarr.setText(srecord.getString("narration"));
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "EmployeeDetails", 0);
            }
        }

        if (tot > 0) {
            btnDelete.setEnabled((true));
            btnUpdate.setEnabled(true);
            btnFind.setEnabled(true);
        } else {
            btnDelete.setEnabled((false));
            btnUpdate.setEnabled(false);
            btnFind.setEnabled(false);
        }
        if (cur > 1) {
            btnFirst.setEnabled(true);
            btnPrev.setEnabled(true);
        } else {
            btnFirst.setEnabled(false);
            btnPrev.setEnabled(false);
        }
        if (cur == tot) {
            btnNext.setEnabled(false);
            btnLast.setEnabled(false);
        } else {
            btnNext.setEnabled(true);
            btnLast.setEnabled(true);
        }
        txtNavigation.setText("Cur:" + cur + "/Tot: " + tot);
    }

    public void setAllControlEmpty() {
        txtEmpID.setText(" ");
        txtName.setText(" ");
        cmbGender.setEditable(true);
        cmbGender.setSelectedItem(" ");
        cmbGender.setEditable(false);
        txtDob.setText(" ");
        txtDoj.setText(" ");
        txtAddr.setText(" ");
        txtPhone.setText(" ");
        txtMobile.setText(" ");
        txtEmail.setText(" ");
        txtBsal.setText(" ");
        txtNarr.setText(" ");
    }

    public void deleteRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from employee_details";
            srecord = stmt.executeQuery(str);
            srecord.absolute(ctr);
            srecord.deleteRow();

            if (cur < tot)
                tot--;
            else if (cur == tot && cur > 1)
                cur = --tot;
            else if (cur == tot && cur == 1) {
                cur = tot = 0;
                setAllControlEmpty();
            }
            JOptionPane.showMessageDialog(this, "Record Deleted", "EmployeeDetails", 1);
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Exception In Delete Record:" + e.getMessage(), "EmaployeeDetails", 0);
        }
    }

    public void addRecord() {
        try {
            String str = "insert into employee_details values(?,?,?,?,?,?,?,?,?,?,?)";
            pstmt = MdiForm.sconnect.prepareStatement(str);

            pstmt.setString(1, txtEmpID.getText());
            pstmt.setString(2, txtName.getText());
            pstmt.setString(3, "" + cmbGender.getSelectedItem());
            pstmt.setString(4, txtDob.getText());
            pstmt.setString(5, txtDoj.getText());
            pstmt.setString(6, txtAddr.getText());
            pstmt.setString(7, txtPhone.getText());
            pstmt.setString(8, txtMobile.getText());
            pstmt.setString(9, txtEmail.getText());
            pstmt.setFloat(10, Float.parseFloat(txtBsal.getText()));
            pstmt.setString(11, txtNarr.getText());

            pstmt.executeUpdate();
            ++tot;
            JOptionPane.showMessageDialog(this, "Recorded Added", "EmployeeDetails", 1);
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Exception in Add Record:" + e.getMessage(), "EmployeeDetails", 0);
        }
    }

    public void findRecord() {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from employee_details";
            srecord = stmt.executeQuery(str);
            String empid = "";
            int ctr = 0;
            boolean flag = false;

            while (srecord.next()) {
                empid = srecord.getString("emp_id");
                if (empid.equalsIgnoreCase(txtEmpID.getText())) {
                    cur = ++ctr;
                    flag = true;
                    break;
                }
                ctr++;
            }
            if (flag == false)
                JOptionPane.showMessageDialog(this, "Record Not Found!", "EmployeeDetails", 0);
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Exception In Find Record: " + e.getMessage(), "EmployeeDetails", 0);
        }
    }

    public void updateRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from employee_details";
            srecord = stmt.executeQuery(str);
            srecord.absolute(ctr);
            
             srecord.updateString(1, txtEmpID.getText());
            srecord.updateString(2, txtName.getText());
            srecord.updateString(3, "" + cmbGender.getSelectedItem());
            srecord.updateString(4, txtDob.getText());
            srecord.updateString(5, txtDoj.getText());
            srecord.updateString(6, txtAddr.getText());
            srecord.updateString(7, txtPhone.getText());
            srecord.updateString(8, txtMobile.getText());
            srecord.updateString(9, txtEmail.getText());
            srecord.updateFloat(10, Float.parseFloat(txtBsal.getText()));
            srecord.updateString(11, txtNarr.getText());

            srecord.updateRow();
            JOptionPane.showMessageDialog(this, "Record Updated", "EmployeeDetails", 1);
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Exception In Update Record: " + e.getMessage(), "EmployeeDetails", 0);
        }
    }

    public void actionPerformed(ActionEvent ae){
        if(ae.getSource()==btnFirst){
            cur=1;
            showRecord(cur);
        } else if(ae.getSource()==btnPrev){
            cur--;
            showRecord(cur);
        }else if(ae.getSource()==btnNext){
            cur++;
            showRecord(cur);
        }else if(ae.getSource()==btnLast){
            cur = tot;
            showRecord(cur);
        }else if(ae.getSource()==btnDelete){
            deleteRecord(cur);
        }else if(ae.getSource()==btnRefresh){
            setAllControlEmpty();
            txtEmpID.requestFocus();
        }else if(ae.getSource()==btnSave){
            addRecord();
        }else if(ae.getSource()==btnFind){
            findRecord();
        }
        else if(ae.getSource()==btnUpdate){
            updateRecord(cur);
        }
    }

    public static void main(String[] args) {
        new EmployeeAttendence();
    }

}