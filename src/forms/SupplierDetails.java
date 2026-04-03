package forms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SupplierDetails extends JInternalFrame implements MouseListener, ActionListener, ItemListener, KeyListener {
    private ResultSet srecord;
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot, cur;
    private boolean flag = false;

    private JLabel lblSaleNo, lblSaleDate, lblItemCode, lblDescription, lblQt;
    private JLabel lblRate, lblTotalAmt, lblVatAmt, lblConAmt, lblNetAmt;

    private JTextField txtSaleNo, txtSaleDate, txtDescription, txtQty, txtRate;
    private JTextField txtTotalAmt, txtVatAmt, txtConAmt, txtNetAmt;
    private JComboBox<String> cmbItemCode;

    private JButton btnSave, btnDelete, btnUpdate, btnFind, btnRefresh;
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JTextField txtNavigation;

    private Container c;
    private SupplierDetailsDtlBackground obj1;
    private SupplierDetailsDtlHeading obj2;

    private class SupplierDetailsDtlBackground extends JPanel {
        private SupplierDetailsDtlBackground() {
            setLayout(null);

            lblSaleNo = new JLabel("Sale No");
            lblSaleDate = new JLabel("Sale Date");
            lblItemCode = new JLabel("Item Code");
            lblDescription = new JLabel("Description");
            lblQt = new JLabel("Qty");
            lblRate = new JLabel("Rate");
            lblTotalAmt = new JLabel("Total Amt");
            lblVatAmt = new JLabel("Vat Amt");
            lblConAmt = new JLabel("Con Amt");
            lblNetAmt = new JLabel("Net Amt");

            txtSaleNo = new JTextField();
            txtSaleDate = new JTextField();
            cmbItemCode = new JComboBox<>();
            txtDescription = new JTextField();
            txtDescription.setEditable(false);
            txtQty = new JTextField();
            txtQty.setEditable(false);
            txtRate = new JTextField();
            txtRate.setEditable(false);
            txtTotalAmt = new JTextField();
            txtTotalAmt.setEditable(false);
            txtVatAmt = new JTextField();
            txtVatAmt.setEditable(false);
            txtConAmt = new JTextField();
            txtConAmt.setEditable(false);
            txtNetAmt = new JTextField();
            txtNetAmt.setEditable(false);

            btnSave = new JButton("Save");
            btnDelete = new JButton("Delete");
            btnUpdate = new JButton("Update");
            btnFind = new JButton("Find");
            btnRefresh = new JButton("Refresh");

            btnFirst = new JButton("|<<");
            btnPrev = new JButton("<<");
            txtNavigation = new JTextField();
            btnNext = new JButton(">>");
            btnLast = new JButton(">>|");

            lblSaleNo.setBounds(50, 30, 100, 30);
            txtSaleNo.setBounds(150, 30, 100, 30);
            lblSaleDate.setBounds(50, 70, 100, 30);
            txtSaleDate.setBounds(150, 70, 100, 30);
            lblItemCode.setBounds(50, 110, 100, 30);
            cmbItemCode.setBounds(150, 110, 100, 30);
            lblDescription.setBounds(50, 150, 100, 30);
            txtDescription.setBounds(150, 150, 100, 30);
            lblQt.setBounds(50, 190, 100, 30);
            txtQty.setBounds(150, 190, 100, 30);
            lblRate.setBounds(50, 230, 100, 30);
            txtRate.setBounds(150, 230, 100, 30);
            lblTotalAmt.setBounds(50, 270, 100, 30);
            txtTotalAmt.setBounds(150, 270, 100, 30);
            lblVatAmt.setBounds(50, 310, 100, 30);
            txtVatAmt.setBounds(150, 310, 100, 30);
            lblConAmt.setBounds(50, 350, 100, 30);
            txtConAmt.setBounds(150, 350, 100, 30);
            lblNetAmt.setBounds(50, 390, 100, 30);
            txtNetAmt.setBounds(150, 390, 100, 30);

            btnSave.setBounds(50, 430, 80, 30);
            btnDelete.setBounds(140, 430, 80, 30);
            btnUpdate.setBounds(230, 430, 80, 30);
            btnFind.setBounds(320, 430, 80, 30);
            btnRefresh.setBounds(410, 430, 80, 30);
            btnFirst.setBounds(50, 470, 80, 30);
            btnPrev.setBounds(140, 470, 80, 30);
            txtNavigation.setBounds(230, 470, 80, 30);
            btnNext.setBounds(320, 470, 80, 30);
            btnLast.setBounds(410, 470, 80, 30);

            add(lblSaleNo);
            add(txtSaleNo);
            add(lblSaleDate);
            add(txtSaleDate);
            add(lblItemCode);
            add(cmbItemCode);
            add(lblDescription);
            add(txtDescription);
            add(lblQt);
            add(txtQty);
            add(lblRate);
            add(txtRate);
            add(lblTotalAmt);
            add(txtTotalAmt);
            add(lblVatAmt);
            add(txtVatAmt);
            add(lblConAmt);
            add(txtConAmt);
            add(lblNetAmt);
            add(txtNetAmt);
            add(btnSave);
            add(btnDelete);
            add(btnUpdate);
            add(btnFind);
            add(btnRefresh);
            add(btnFirst);
            add(btnPrev);
            add(txtNavigation);
            add(btnNext);
            add(btnLast);

            setBackground(new Color(255, 255, 204));
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(255, 153, 0));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(20, 20, 480, 450);
        }
    }

    private class SupplierDetailsDtlHeading extends JPanel {
        private SupplierDetailsDtlHeading() {
            setLayout(null);
            JLabel lblHeading = new JLabel("Sale Details");
            lblHeading.setFont(new Font("Arial", Font.BOLD, 24));
            lblHeading.setBounds(150, 10, 200, 30);
            add(lblHeading);
        }
    }

    public SupplierDetails() {
        super("Sale Details", false, true, false, true);
        c = getContentPane();
        c.setLayout(null);
        obj1 = new SupplierDetailsDtlBackground();
        obj2 = new SupplierDetailsDtlHeading();
        obj1.setBounds(10, 50, 520, 520);
        obj2.setBounds(10, 10, 520, 40);

        btnSave.addMouseListener(this);
        btnDelete.addMouseListener(this);
        btnUpdate.addMouseListener(this);
        btnFind.addMouseListener(this);
        btnRefresh.addMouseListener(this);
        btnFirst.addMouseListener(this);
        btnPrev.addMouseListener(this);
        btnNext.addMouseListener(this);
        btnLast.addMouseListener(this);

        btnSave.addActionListener(this);
        btnDelete.addActionListener(this);
        btnUpdate.addActionListener(this);
        btnFind.addActionListener(this);
        btnRefresh.addActionListener(this);
        btnFirst.addActionListener(this);
        btnPrev.addActionListener(this);
        btnNext.addActionListener(this);
        btnLast.addActionListener(this);

        cmbItemCode.addItemListener(this);
        txtQty.addKeyListener(this);

        c.add(obj1);
        c.add(obj2);
        setSize(550, 620);
        setLocation(100, 50);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
        try {
            String str1 = "select item_code from item_details";
            pstmt = MdiForm.sconnect.prepareStatement(str1);
            srecord = pstmt.executeQuery();
            while (srecord.next()) {
                cmbItemCode.addItem(srecord.getString(1));
            }
            cmbItemCode.setEditable(true);
            cmbItemCode.setSelectedItem("");
            cmbItemCode.setEditable(false);

            String str = "select * from sale_details";
            pstmt = MdiForm.sconnect.prepareStatement(str);
            srecord = pstmt.executeQuery(str);
            while (srecord.next()) {
                tot++;
            }
            if (tot > 0)
                cur = 1;

            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Sale Details", 0);
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
            btnFirst.setForeground(Color.darkGray);
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
            btnFirst.setForeground(Color.red);
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
                String str = "select * from sale_details";
                srecord = stmt.executeQuery(str);
                srecord.absolute(ctr);

                txtSaleNo.setText(srecord.getString("sale_no"));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sdf.parse(srecord.getString("sale_date"));
                sdf = new SimpleDateFormat("dd-MM-yyyy");
                txtSaleDate.setText(srecord.getString(sdf.format(date)));
                cmbItemCode.setEditable(true);
                cmbItemCode.setSelectedItem(srecord.getString("item_code"));
                cmbItemCode.setEditable(false);
                txtDescription.setText(srecord.getString("description"));
                txtQty.setText(srecord.getString("qty"));
                txtRate.setText(srecord.getString("rate"));
                txtTotalAmt.setText(srecord.getString("total_amt"));
                txtVatAmt.setText(srecord.getString("vat_amt"));
                txtConAmt.setText(srecord.getString("con_amt"));
                txtNetAmt.setText(srecord.getString("net_amt"));
            } catch (SQLException | java.text.ParseException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Sale Details", 0);
            }
        }

        if (tot > 0) {
            btnDelete.setEnabled(true);
            btnUpdate.setEnabled(true);
            btnFind.setEnabled(true);
        } else {
            btnDelete.setEnabled(false);
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

        txtNavigation.setText("Cur: " + cur + "/Tot: " + tot);
    }

    public void setAllControlsEmpty() {
        flag = false;
        txtSaleNo.setText("");
        txtSaleDate.setText("");
        cmbItemCode.setEditable(true);
        cmbItemCode.setSelectedItem("");
        cmbItemCode.setEditable(false);
        txtDescription.setText("");
        txtQty.setText("");
        txtRate.setText("");
        txtTotalAmt.setText("");
        txtVatAmt.setText("");
        txtConAmt.setText("");
        txtNetAmt.setText("");
        flag = true;
    }

    public void deleteRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from sale_details";
            srecord = stmt.executeQuery(str);
            srecord.absolute(ctr);
            srecord.deleteRow();

            if (cur < tot) {
                tot--;
            } else if (cur == tot && cur > 1) {
                cur = --tot;
            } else if (cur == tot && cur == 1) {
                cur = tot = 0;
                setAllControlsEmpty();
            }
            JOptionPane.showMessageDialog(this, "Record Deleted", "Sale Details", 1);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Sale Details", 0);
        }
    }

    public void addRecord() {
        try {
            String str = "insert into sale_details values(?,?,?,?,?,?,?,?,?,?)";
            pstmt = MdiForm.sconnect.prepareStatement(str);

            pstmt.setString(1, txtSaleNo.getText());
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            Date date = sdf.parse(txtSaleDate.getText());
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            pstmt.setString(2, sdf.format(date));
            pstmt.setString(3, cmbItemCode.getSelectedItem().toString());
            pstmt.setString(4, txtDescription.getText());
            pstmt.setInt(5, Integer.parseInt(txtQty.getText()));
            pstmt.setInt(6, Integer.parseInt(txtRate.getText()));
            pstmt.setInt(7, Integer.parseInt(txtTotalAmt.getText()));
            pstmt.setInt(8, Integer.parseInt(txtVatAmt.getText()));
            pstmt.setInt(9, Integer.parseInt(txtConAmt.getText()));
            pstmt.setInt(10, Integer.parseInt(txtNetAmt.getText()));

            pstmt.executeUpdate();
            ++tot;
            JOptionPane.showMessageDialog(this, "Record Added", "Sale Details", 1);
            showRecord(cur);
        } catch (SQLException | java.text.ParseException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Sale Details", 0);
        }
    }

    public void findRecord() {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from sale_details";
            srecord = stmt.executeQuery(str);
            String sale_no = "";
            int ctr = 0;
            boolean flag = false;
            while (srecord.next()) {
                sale_no = srecord.getString("sale_no");
                if (sale_no.equalsIgnoreCase(txtSaleNo.getText())) {
                    cur = ++ctr;
                    flag = true;
                    break;
                }
                ctr++;
            }
            if (!flag) {
                JOptionPane.showMessageDialog(this, "Record Not Found", "Sale Details", 0);
            }
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Sale Details", 0);
        }
    }

    public void udpateRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from sale_details";
            srecord = stmt.executeQuery(str);
            srecord.absolute(ctr);

            srecord.updateString(1, txtSaleNo.getText());
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            Date date = sdf.parse(txtSaleDate.getText());
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            srecord.updateString(2, sdf.format(date));
            srecord.updateString(3, cmbItemCode.getSelectedItem().toString());
            srecord.updateString(4, txtDescription.getText());
            srecord.updateInt(5, Integer.parseInt(txtQty.getText()));
            srecord.updateInt(6, Integer.parseInt(txtRate.getText()));
            srecord.updateInt(7, Integer.parseInt(txtTotalAmt.getText()));
            srecord.updateInt(8, Integer.parseInt(txtVatAmt.getText()));
            srecord.updateInt(9, Integer.parseInt(txtConAmt.getText()));
            srecord.updateInt(10, Integer.parseInt(txtNetAmt.getText()));

            srecord.updateRow();
            JOptionPane.showMessageDialog(this, "Record Updated", "Sale Details", 1);
        } catch (SQLException | java.text.ParseException e) {
            JOptionPane.showMessageDialog(this, "Exception In Update Record:" + e.getMessage(), "Sale Details", 0);
        }
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btnFirst) {
            cur = 1;
            showRecord(cur);
        } else if (ae.getSource() == btnPrev) {
            if (cur > 1)
                cur--;
            showRecord(cur);
        } else if (ae.getSource() == btnNext) {
            if (cur < tot)
                cur++;
            showRecord(cur);
        } else if (ae.getSource() == btnLast) {
            cur = tot;
            showRecord(cur);
        } else if (ae.getSource() == btnDelete) {
            deleteRecord(cur);
        } else if (ae.getSource() == btnRefresh) {
            showRecord(cur);
        } else if (ae.getSource() == btnSave) {
            addRecord();
        } else if (ae.getSource() == btnFind) {
            findRecord();
        } else if (ae.getSource() == btnUpdate) {
            udpateRecord(cur);
        }
    }

    public void itemStateChanged(ItemEvent ie) {
        try {
            if(ie.getSource()==cmbItemCode&&flag){
                String str="select description,rate from item_details where item_code='"+cmbItemCode.getSelectedItem().toString()+"'";
                pstmt=MdiForm.sconnect.prepareStatement(str);
                ResultSet srecord1=pstmt.executeQuery();
                srecord1.next();

                txtDescription.setEditable(true);
                txtDescription.setText(srecord1.getString(1));
                txtDescription.setEditable(false);
                txtQty.setEditable(true);
                txtQty.setText(srecord1.getString(2));
                txtQty.setEditable(false);
                txtRate.setEditable(true);
                txtRate.setText(srecord1.getString(3));
                txtRate.setEditable(false);

                String str1, str2, str3, str4;
                double qty, rate, total, vat, concession, net;

                qty = Double.parseDouble(txtQty.getText());
                rate = Double.parseDouble(txtRate.getText());
                total = qty * rate;
                vat = total * 1/100;
                concession = total * 10/100;
                net = total + vat - concession;

                str1 = String.valueOf(total);
                txtTotalAmt.setEditable(true);
                txtTotalAmt.setText(str1);
                txtTotalAmt.setEditable(false);

                str2 = String.valueOf(vat);
                txtVatAmt.setEditable(true);
                txtVatAmt.setText(str2);
                txtVatAmt.setEditable(false);

                str3 = String.valueOf(concession);
                txtConAmt.setEditable(true);
                txtConAmt.setText(str3);
                txtConAmt.setEditable(false);

                str4 = String.valueOf(net);
                txtNetAmt.setEditable(true);
                txtNetAmt.setText(str4);
                txtNetAmt.setEditable(false);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,"Exception in Item Selection: " + e.getMessage(), "Sale Details", 0);
        }
    }

    public void keyPressed(KeyEvent ke) {
    }
    public void keyReleased(KeyEvent ke) {
        if(ke.getSource()==txtQty){
            String str1, str2, str3, str4;
            double qty, rate, total, vat, concession, net;
            qty = Double.parseDouble(txtQty.getText());
            rate = Double.parseDouble(txtRate.getText());
            total = qty * rate;
            vat = total * 1/100;
            concession = total * 10/100;
            net = total + vat - concession;

            str1 = String.valueOf(total);
            txtTotalAmt.setEditable(true);
            txtTotalAmt.setText(str1);
            txtTotalAmt.setEditable(false);

            str2 = String.valueOf(vat);
            txtVatAmt.setEditable(true);
            txtVatAmt.setText(str2);
            txtVatAmt.setEditable(false);

            str3 = String.valueOf(concession);
            txtConAmt.setEditable(true);
            txtConAmt.setText(str3);
            txtConAmt.setEditable(false);

            str4 = String.valueOf(net);
            txtNetAmt.setEditable(true);
            txtNetAmt.setText(str4);    
            txtNetAmt.setEditable(false);
        }
    }
    public void keyTyped(KeyEvent ke) {
    }

    public static void main(String[] args) {
        new SupplierDetails();
    }
}
