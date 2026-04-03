package forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PurchaseReturn extends JInternalFrame implements MouseListener, ActionListener, ItemListener, KeyListener {
    private ResultSet srecord;
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot, cur;
    private boolean flag = false;

    private JLabel lblPurchaseRetNo, lblPurchaseRetDate, lblPurchaseId, lblPurchaseDate, lblSuplierId, lblItemCode,
            lblDescription,
            lblQty;
    private JLabel lblRate, lbtlTotalAmt, lblVatAmt, lblConAmt, lblNetAmt, lblNoOfRetItem, lblTotalRetAmt, lblNarr;
    private JTextField txtPurchaseRetNo, txtPurchaseRetDate, txtPurchaseDate, txtDescription, txtQty, txtRate;
    private JTextField txtTotalAmt, txtVatAmt, txtConAmt, txtNetAmt, txtNoOfRetItem, txtTotalRetAmt, txtSuplierId,
            txtItemCode;
    private JComboBox<String> cmbPurchaseId;
    private JTextArea txtNarr;
    private JScrollPane jscNarr;

    private JButton btnSave, btnDelete, btnUpdate, btnFind, btnRefresh;
    private JButton btnFirst, btnPrev, btnNext, btnLast;

    private JTextField txtNavigation;

    private Container c;
    private PurchaseReturnDtlBackground obj1;
    private PurchaseReturnDtlHeading obj2;

    private class PurchaseReturnDtlBackground extends JPanel {

        private PurchaseReturnDtlBackground() {
            setLayout(null);
            lblPurchaseRetNo = new JLabel("Purchase Return No");
            lblPurchaseRetDate = new JLabel("Purchase Return Date");
            lblPurchaseId = new JLabel("Purchase Id");
            lblPurchaseDate = new JLabel("Purchase Date");
            lblSuplierId = new JLabel("Suplier Id");
            lblItemCode = new JLabel("Item Code");
            lblDescription = new JLabel("Description");
            lblQty = new JLabel("Quantity");
            lblRate = new JLabel("Rate");
            lbtlTotalAmt = new JLabel("Total Amount");
            lblVatAmt = new JLabel("Vat Amount");
            lblConAmt = new JLabel("Concession Amount");
            lblNetAmt = new JLabel("Net Amount");
            lblNoOfRetItem = new JLabel("No of Return Item");
            lblTotalRetAmt = new JLabel("Total Return Amount");
            lblNarr = new JLabel("Narration");
            txtPurchaseRetNo = new JTextField();
            txtPurchaseRetDate = new JTextField();
            cmbPurchaseId = new JComboBox<>();
            txtPurchaseDate = new JTextField();
            txtSuplierId = new JTextField();
            txtItemCode = new JTextField();
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
            txtNoOfRetItem = new JTextField();
            txtTotalRetAmt = new JTextField();
            txtTotalRetAmt.setEditable(false);
            txtNarr = new JTextArea();
            jscNarr = new JScrollPane(txtNarr);

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

            lblPurchaseRetNo.setBounds(50, 50, 150, 30);
            txtPurchaseRetNo.setBounds(220, 50, 150, 30);
            lblPurchaseRetDate.setBounds(400, 50, 150, 30);
            txtPurchaseRetDate.setBounds(570, 50, 150, 30);
            lblPurchaseId.setBounds(50, 100, 150, 30);
            cmbPurchaseId.setBounds(220, 100, 150, 30);
            lblPurchaseDate.setBounds(400, 100, 150, 30);
            txtPurchaseDate.setBounds(570, 100, 150, 30);
            lblSuplierId.setBounds(50, 150, 150, 30);
            txtSuplierId.setBounds(220, 150, 150, 30);
            lblItemCode.setBounds(400, 150, 150, 30);
            txtItemCode.setBounds(570, 150, 150, 30);
            lblDescription.setBounds(50, 200, 150, 30);
            txtDescription.setBounds(220, 200, 500, 30);
            lblQty.setBounds(50, 250, 150, 30);
            txtQty.setBounds(220, 250, 150, 30);
            lblRate.setBounds(400, 250, 150, 30);
            txtRate.setBounds(570, 250, 150, 30);
            lbtlTotalAmt.setBounds(50, 300, 150, 30);
            txtTotalAmt.setBounds(220, 300, 150, 30);
            lblVatAmt.setBounds(400, 300, 150, 30);
            txtVatAmt.setBounds(570, 300, 150, 30);
            lblConAmt.setBounds(50, 350, 150, 30);

            txtConAmt.setBounds(220, 350, 150, 30);
            lblNetAmt.setBounds(400, 350, 150, 30);
            txtNetAmt.setBounds(570, 350, 150, 30);
            lblNoOfRetItem.setBounds(50, 400, 150, 30);
            txtNoOfRetItem.setBounds(220, 400, 150, 30);
            lblTotalRetAmt.setBounds(400, 400, 150, 30);
            txtTotalRetAmt.setBounds(570, 400, 150, 30);
            lblNarr.setBounds(50, 450, 150, 30);
            jscNarr.setBounds(220, 450, 500, 100);

            btnSave.setBounds(150, 600, 100, 30);
            btnDelete.setBounds(260, 600, 100, 30);
            btnUpdate.setBounds(370, 600, 100, 30);
            btnFind.setBounds(480, 600, 100, 30);
            btnRefresh.setBounds(590, 600, 100, 30);
            btnFirst.setBounds(150, 650, 100, 30);
            btnPrev.setBounds(260, 650, 100, 30);
            txtNavigation.setBounds(370, 650, 100, 30);
            btnNext.setBounds(480, 650, 100, 30);
            btnLast.setBounds(590, 650, 100, 30);

            add(lblPurchaseRetNo);
            add(txtPurchaseRetNo);
            add(lblPurchaseRetDate);
            add(txtPurchaseRetDate);
            add(lblPurchaseId);
            add(cmbPurchaseId);
            add(lblPurchaseDate);
            add(txtPurchaseDate);
            add(lblSuplierId);
            add(txtSuplierId);
            add(lblItemCode);
            add(txtItemCode);
            add(lblDescription);
            add(txtDescription);
            add(lblQty);
            add(txtQty);
            add(lblRate);
            add(txtRate);
            add(lbtlTotalAmt);
            add(txtTotalAmt);
            add(lblVatAmt);
            add(txtVatAmt);
            add(lblConAmt);
            add(txtConAmt);
            add(lblNetAmt);
            add(txtNetAmt);
            add(lblNoOfRetItem);
            add(txtNoOfRetItem);
            add(lblTotalRetAmt);
            add(txtTotalRetAmt);
            add(lblNarr);
            add(jscNarr);
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
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(173, 216, 230));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private class PurchaseReturnDtlHeading extends JPanel {
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(173, 216, 230));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Purchase Details", 20, 30);
        }
    }

    public PurchaseReturn() {
        super("Purchase Return", false, true, false, true);
        c = getContentPane();
        c.setLayout(null);
        obj1 = new PurchaseReturnDtlBackground();
        obj2 = new PurchaseReturnDtlHeading();
        obj1.setBounds(0, 50, 800, 700);
        obj2.setBounds(0, 0, 800, 50);

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

        cmbPurchaseId.addItemListener(this);
        txtPurchaseRetNo.addKeyListener(this);

        c.add(obj1);
        c.add(obj2);

        setSize(820, 800);
        setVisible(true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        try {
            String str1 = "select purchase_id from purchase_details";
            pstmt = MdiForm.sconnect.prepareStatement(str1);
            srecord = pstmt.executeQuery();
            while (srecord.next()) {
                cmbPurchaseId.addItem(srecord.getString(1));
            }
            cmbPurchaseId.setEditable(true);
            cmbPurchaseId.setSelectedItem("");
            cmbPurchaseId.setEditable(false);

            String str = "select * from purchase_return";
            pstmt = MdiForm.sconnect.prepareStatement(str);
            srecord = pstmt.executeQuery();
            while (srecord.next()) {
                tot++;
            }
            if (tot > 0) {
                cur = 1;
                // showRecord(cur);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Purchase Return Details", 0);
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
                String str = "select * from purchase_return";
                srecord = stmt.executeQuery(str);
                srecord.absolute(ctr);

                txtPurchaseRetNo.setText(srecord.getString("purchase_ret_no"));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sdf.parse(srecord.getString("purchase_ret_date"));
                sdf = new SimpleDateFormat("dd-MM-yyyy");
                txtPurchaseRetDate.setText(sdf.format(srecord.getDate(sdf.format(date))));
                cmbPurchaseId.setEditable(true);
                cmbPurchaseId.setSelectedItem(srecord.getString("purchase_id"));
                cmbPurchaseId.setEditable(false);
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                Date date1 = sdf.parse(srecord.getString("purchase_date"));
                sdf1 = new SimpleDateFormat("dd-MM-yyyy");
                txtPurchaseDate.setText(sdf1.format(srecord.getDate(sdf1.format(date1))));
                txtSuplierId.setText(srecord.getString("suplier_id"));
                txtItemCode.setText(srecord.getString("item_code"));
                txtDescription.setText(srecord.getString("description"));
                txtQty.setText(srecord.getString("qty"));
                txtRate.setText(srecord.getString("rate"));
                txtTotalAmt.setText(srecord.getString("total_amt"));
                txtVatAmt.setText(srecord.getString("vat_amt"));
                txtConAmt.setText(srecord.getString("con_amt"));
                txtNetAmt.setText(srecord.getString("net_amt"));
                txtNoOfRetItem.setText(srecord.getString("no_of_ret_item"));
                txtTotalRetAmt.setText(srecord.getString("total_ret_amt"));
                txtNarr.setText(srecord.getString("narr"));
                txtNavigation.setText(ctr + "/" + tot);
            } catch (SQLException | java.text.ParseException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(),
                        "Purchase Return Details", 0);
            }
        }
        if (tot > 0) {
            btnFirst.setEnabled(true);
            btnPrev.setEnabled(true);
            btnFind.setEnabled(true);
        } else {
            btnFirst.setEnabled(false);
            btnPrev.setEnabled(false);
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

    public void setAllControlEmpty() {
        flag = false;
        txtPurchaseRetNo.setText("");
        txtPurchaseRetDate.setText("");
        cmbPurchaseId.setEditable(true);
        cmbPurchaseId.setSelectedItem("");
        cmbPurchaseId.setEditable(false);
        txtPurchaseDate.setText("");
        txtSuplierId.setText("");
        txtItemCode.setText("");
        txtDescription.setText("");
        txtQty.setText("");
        txtRate.setText("");
        txtTotalAmt.setText("");
        txtVatAmt.setText("");
        txtConAmt.setText("");
        txtNetAmt.setText("");
        txtNoOfRetItem.setText("");
        txtTotalRetAmt.setText("");
        txtNarr.setText("");
        flag = true;
    }

    public void deleteRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from purchase_return";
            srecord = stmt.executeQuery(str);
            srecord.absolute(ctr);
            srecord.deleteRow();

            if (cur < tot) {
                tot--;
            } else if (cur == tot && cur > 1) {
                cur = --tot;
            } else if (cur == tot && cur == 1) {
                cur = tot = 0;
                setAllControlEmpty();
            }
            JOptionPane.showMessageDialog(this, "Record Deleted",
                    "Purchase Return Details", 1);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Purchase Return Details", 0);
        }
    }

    public void addRecord() {
        try {
            String str = "insert into purchase_return values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            pstmt = MdiForm.sconnect.prepareStatement(str);
            pstmt.setString(1, txtPurchaseRetNo.getText());
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            Date date = sdf.parse(txtPurchaseRetDate.getText());
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            pstmt.setString(2, sdf.format(date));
            pstmt.setString(3, cmbPurchaseId.getSelectedItem().toString());
            SimpleDateFormat sdf1 = new SimpleDateFormat("dd-MM-yyyy");
            Date date1 = sdf1.parse(txtPurchaseDate.getText());
            sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            pstmt.setString(4, sdf1.format(date1));
            pstmt.setString(5, txtSuplierId.getText());
            pstmt.setString(6, txtItemCode.getText());
            pstmt.setString(7, txtDescription.getText());
            pstmt.setString(8, txtQty.getText());
            pstmt.setString(9, txtRate.getText());
            pstmt.setString(10, txtTotalAmt.getText());
            pstmt.setString(11, txtVatAmt.getText());
            pstmt.setString(12, txtConAmt.getText());
            pstmt.setString(13, txtNetAmt.getText());
            pstmt.setString(14, txtNoOfRetItem.getText());
            pstmt.setString(15, txtTotalRetAmt.getText());
            pstmt.setString(16, txtNarr.getText());
            pstmt.executeUpdate();
            ++tot;

            JOptionPane.showMessageDialog(this, "Record Added",
                    "Purchase Return Details", 1);
        } catch (SQLException | java.text.ParseException e) {
            JOptionPane.showMessageDialog(this, "Exception In Add Record: " + e.getMessage(),
                    "Purchase Return Details", 0);
        }
    }

    public void findRecord() {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from purchase_return";
            srecord = stmt.executeQuery(str);
            String pu_ret_no = "";
            int ctr = 0;
            boolean flag = false;
            while (srecord.next()) {
                pu_ret_no = srecord.getString("purchase_ret_no");
                if (pu_ret_no.equalsIgnoreCase(txtPurchaseRetNo.getText())) {
                    cur = ++ctr;
                    flag = true;
                    break;
                }
                ctr++;
            }
            if (flag == false) {
                JOptionPane.showMessageDialog(this, "Record Not Found",
                        "Purchase Return Details", 0);
            }
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Exception In Find Record: " + e.getMessage(),
                    "Purchase Return Details", 0);
        }
    }

    public void updateRecord() {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from purchase_return";
            srecord = stmt.executeQuery(str);
            srecord.absolute(cur);

            srecord.updateString(1, txtPurchaseRetNo.getText());
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            Date date = sdf.parse(txtPurchaseRetDate.getText());
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            srecord.updateString(2, sdf.format(date));
            srecord.updateString(3, cmbPurchaseId.getSelectedItem().toString());
            SimpleDateFormat sdf1 = new SimpleDateFormat("dd-MM-yyyy");
            Date date1 = sdf1.parse(txtPurchaseDate.getText());
            sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            srecord.updateString(4, sdf1.format(date1));
            srecord.updateString(5, txtSuplierId.getText());
            srecord.updateString(6, txtItemCode.getText());
            srecord.updateString(7, txtDescription.getText());
            srecord.updateString(8, txtQty.getText());
            srecord.updateString(9, txtRate.getText());
            srecord.updateString(10, txtTotalAmt.getText());
            srecord.updateString(11, txtVatAmt.getText());
            srecord.updateString(12, txtConAmt.getText());
            srecord.updateString(13, txtNetAmt.getText());
            srecord.updateString(14, txtNoOfRetItem.getText());
            srecord.updateString(15, txtTotalRetAmt.getText());
            srecord.updateString(16, txtNarr.getText());

            srecord.updateRow();

            JOptionPane.showMessageDialog(this, "Record Updated",
                    "Purchase Return Details", 1);
        } catch (SQLException | java.text.ParseException e) {
            JOptionPane.showMessageDialog(this, "Exception In Update Record: " + e.getMessage(),
                    "Purchase Return Details", 0);
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
            setAllControlEmpty();
            txtPurchaseRetNo.requestFocus();
        } else if (ae.getSource() == btnSave) {
            addRecord();
        } else if (ae.getSource() == btnFind) {
            findRecord();
        } else if (ae.getSource() == btnUpdate) {
            updateRecord();
        }
    }

    public void itemStateChanged(ItemEvent ie) {
        try {
            if (ie.getSource() == cmbPurchaseId && flag) {
                String str = "select purchase_date, suplier_id, item_code, description, qty, rate, total_amt, vat_amt, con_amt, net_amt from purchase_details where purchase_id='"
                        + cmbPurchaseId.getSelectedItem().toString() + "'";
                pstmt = MdiForm.sconnect.prepareStatement(str);
                ResultSet srecord1 = pstmt.executeQuery();
                srecord1.next();
                txtPurchaseDate.setEditable(true);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sdf.parse(srecord1.getString("purchase_date"));
                sdf = new SimpleDateFormat("dd-MM-yyyy");
                txtPurchaseDate.setText(sdf.format(srecord1.getDate(sdf.format(date))));
                txtPurchaseDate.setEditable(false);
                txtSuplierId.setEditable(true);
                txtSuplierId.setText(srecord1.getString(2));
                txtSuplierId.setEditable(false);
                txtItemCode.setEditable(true);
                txtItemCode.setText(srecord1.getString(3));
                txtItemCode.setEditable(false);
                txtDescription.setEditable(true);
                txtDescription.setText(srecord1.getString(4));
                txtDescription.setEditable(false);
                txtQty.setEditable(true);
                txtQty.setText(srecord1.getString(5));
                txtQty.setEditable(false);
                txtRate.setEditable(true);
                txtRate.setText(srecord1.getString(6));
                txtRate.setEditable(false);
                txtTotalAmt.setEditable(true);
                txtTotalAmt.setText(srecord1.getString(7));
                txtTotalAmt.setEditable(false);
                txtVatAmt.setEditable(true);
                txtVatAmt.setText(srecord1.getString(8));
                txtVatAmt.setEditable(false);
                txtConAmt.setEditable(true);
                txtConAmt.setText(srecord1.getString(9));
                txtConAmt.setEditable(false);
                txtNetAmt.setEditable(true);
                txtNetAmt.setText(srecord1.getString(10));
                txtNetAmt.setEditable(false);
            }
        } catch (SQLException | java.text.ParseException e) {
            JOptionPane.showMessageDialog(this, "Exception In Item State Changed: " + e.getMessage(),
                    "Purchase Return Details", 0);
        }
    }

    public void keyPressed(KeyEvent ke) {
    }

    public void keyReleased(KeyEvent ke) {
        if (ke.getSource() == txtNoOfRetItem) {
            String str1;
            double no_of_ret_item, rate, total_ret_amt;
            no_of_ret_item = Double.parseDouble(txtNoOfRetItem.getText());
            rate = Double.parseDouble(txtRate.getText());
            total_ret_amt = no_of_ret_item * rate;
            str1 = String.valueOf(total_ret_amt);
            txtTotalRetAmt.setEditable(true);
            txtTotalRetAmt.setText(str1);
            txtTotalRetAmt.setEditable(false);
        }
    }

    public void keyTyped(KeyEvent ke) {
    }

    public static void main(String[] args) {
        new PurchaseReturn();
    }
}
