package forms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PurchaseDetails extends JInternalFrame
        implements MouseListener, ActionListener, ItemListener, KeyListener {
    private ResultSet srecord;
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot, cur;
    private boolean flag = false;

    private JLabel lblPurchaseId, lblPurchaseDate, lblSuplierId, lblItemCode, lblDescription, lblQty;
    private JLabel lblRate, lblTotalAmt, lblVatAmt, lblConAmt, lblNetAmt, lblNarr;

    private JTextField txtPurchaseId, txtPurchaseDate, txtDescription, txtQty, txtRate, txtTotalAmt, txtVatAmt,
            txtConAmt, txtNetAmt;
    private JComboBox<String> cmbSuplierId, cmbItemCode;
    private JTextArea txtNarr;


    private JButton btnDelete, btnUpdate, btnFind, btnRefresh, btnSave;
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JTextField txtNavigation;

    private Container c;
    private PurchaseDtlBackground obj1;
    private PurchaseDtlHeading obj2;

    private class PurchaseDtlBackground extends JPanel {

        private PurchaseDtlBackground() {
            setLayout(null);
            lblPurchaseId = new JLabel("Purchase ID");
            lblPurchaseDate = new JLabel("Purchase Date");
            lblSuplierId = new JLabel("Suplier ID");
            lblItemCode = new JLabel("Item Code");
            lblDescription = new JLabel("Description");
            lblQty = new JLabel("Quantity");
            lblRate = new JLabel("Rate");
            lblTotalAmt = new JLabel("Total Amount");
            lblVatAmt = new JLabel("VAT Amount");
            lblConAmt = new JLabel("Concession Amount");
            lblNetAmt = new JLabel("Net Amount");
            lblNarr = new JLabel("Narration");

            txtPurchaseId = new JTextField();
            txtPurchaseDate = new JTextField();
            cmbSuplierId = new JComboBox<>();
            cmbItemCode = new JComboBox<>();
            txtDescription = new JTextField();
            txtDescription.setEditable(false);
            txtQty = new JTextField();
            txtRate = new JTextField();
            txtTotalAmt = new JTextField();
            txtVatAmt = new JTextField();
            txtConAmt = new JTextField();
            txtNetAmt = new JTextField();
            txtNarr = new JTextArea();

            btnSave = new JButton("Save");
            btnDelete = new JButton("Delete");
            btnUpdate = new JButton("Update");
            btnFind = new JButton("Find");
            btnRefresh = new JButton("Refresh");

            btnFirst = new JButton("|<<");
            btnPrev = new JButton("<<");
            btnNext = new JButton(">>");
            btnLast = new JButton(">>|");

            lblPurchaseId.setBounds(50, 50, 100, 30);
            txtPurchaseId.setBounds(160, 50, 100, 30);
            lblPurchaseDate.setBounds(50, 90, 100, 30);
            txtPurchaseDate.setBounds(160, 90, 100, 30);
            lblSuplierId.setBounds(50, 130, 100, 30);
            cmbSuplierId.setBounds(160, 130, 100, 30);
            lblItemCode.setBounds(50, 170, 100, 30);
            cmbItemCode.setBounds(160, 170, 100, 30);
            lblDescription.setBounds(50, 210, 100, 30);
            txtDescription.setBounds(160, 210, 200, 30);
            lblQty.setBounds(50, 250, 100, 30);
            txtQty.setBounds(160, 250, 100, 30);
            lblRate.setBounds(50, 290, 100, 30);
            txtRate.setBounds(160, 290, 100, 30);
            lblTotalAmt.setBounds(50, 330, 100, 30);
            txtTotalAmt.setBounds(160, 330, 100, 30);
            lblVatAmt.setBounds(50, 370, 100, 30);
            txtVatAmt.setBounds(160, 370, 100, 30);
            lblConAmt.setBounds(50, 410, 100, 30);
            txtConAmt.setBounds(160, 410, 100, 30);
            lblNetAmt.setBounds(50, 450, 100, 30);
            txtNetAmt.setBounds(160, 450, 100, 30);
            lblNarr.setBounds(50, 490, 100, 30);
            txtNarr.setBounds(160, 490, 200, 60);

            btnSave.setBounds(50, 560, 80, 30);
            btnDelete.setBounds(140, 560, 80, 30);
            btnUpdate.setBounds(230, 560, 80, 30);
            btnFind.setBounds(320, 560, 80, 30);
            btnRefresh.setBounds(410, 560, 80, 30);

            btnFirst.setBounds(50, 610, 80, 30);
            btnPrev.setBounds(140, 610, 80, 30);
            txtNavigation.setBounds(230, 610, 80, 30);
            btnNext.setBounds(320, 610, 80, 30);
            btnLast.setBounds(410, 610, 80, 30);

            add(lblPurchaseId);
            add(txtPurchaseId);
            add(lblPurchaseDate);
            add(txtPurchaseDate);
            add(lblSuplierId);
            add(cmbSuplierId);
            add(lblItemCode);
            add(cmbItemCode);
            add(lblDescription);
            add(txtDescription);
            add(lblQty);
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
            add(lblNarr);
            add(txtNarr);
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

    private class PurchaseDtlHeading extends JPanel {
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(173, 216, 230));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Purchase Details", 20, 30);
        }
    }

    public PurchaseDetails() {
        super("PurchaseDetails", false, true, false, true);
        c = getContentPane();
        c.setLayout(null);
        obj1 = new PurchaseDtlBackground();
        obj1.setBounds(0, 50, 600, 700);
        obj2 = new PurchaseDtlHeading();
        obj2.setBounds(0, 0, 600, 50);

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

        setVisible(true);
        setSize(620, 800);
        setLocation(200, 50);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        try {
            String str = "select supplier_id from supplier_details";
            pstmt = MdiForm.sconnect.prepareStatement(str);
            srecord = pstmt.executeQuery();
            while (srecord.next()) {
                cmbSuplierId.addItem(srecord.getString(1));
            }
            cmbSuplierId.setEditable(true);
            cmbSuplierId.setSelectedItem("");
            cmbSuplierId.setEditable(false);

            String str1 = "select item_code from item_details";
            pstmt = MdiForm.sconnect.prepareStatement(str1);
            srecord = pstmt.executeQuery();
            while (srecord.next()) {
                cmbItemCode.addItem(srecord.getString(1));
            }
            cmbItemCode.setEditable(true);
            cmbItemCode.setSelectedItem("");
            cmbItemCode.setEditable(false);

            str = "select * from purchase_details order by purchase_id";
            pstmt = MdiForm.sconnect.prepareStatement(str);
            srecord = pstmt.executeQuery();
            while (srecord.next()) {
                tot++;
            }
            if (tot > 0)
                cur = 1;

            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Purchase Details", 0);
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
                String str = "select * from purchase_details";
                srecord = stmt.executeQuery(str);
                srecord.absolute(ctr);
                txtPurchaseId.setText(srecord.getString("purchase_id"));

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sdf.parse(srecord.getString("purchase_date"));
                sdf = new SimpleDateFormat("dd-MM-yyyy");
                txtPurchaseDate.setText(sdf.format(date));
                cmbSuplierId.setSelectedItem(true);
                cmbSuplierId.setSelectedItem(srecord.getString("supplier_id"));
                cmbSuplierId.setEditable(false);
                cmbItemCode.setSelectedItem(true);
                cmbItemCode.setSelectedItem(srecord.getString("item_code"));
                cmbItemCode.setEditable(false);
                txtDescription.setText(srecord.getString("description"));
                txtQty.setText(srecord.getString("qty"));
                txtRate.setText(srecord.getString("rate"));
                txtTotalAmt.setText(srecord.getString("total_amt"));
                txtVatAmt.setText(srecord.getString("vat_amt"));
                txtConAmt.setText(srecord.getString("con_amt"));
                txtNetAmt.setText(srecord.getString("net_amt"));
                txtNarr.setText(srecord.getString("narration"));

                String str1 = "select description from items_details where item_code="
                        + cmbItemCode.getSelectedItem().toString();
                pstmt = MdiForm.sconnect.prepareStatement(str1);
                ;
                srecord = pstmt.executeQuery();
                srecord.next();
                txtDescription.setText(srecord.getString(1));
            } catch (SQLException | java.text.ParseException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Purchase Details", 0);
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

    public void setAllControlEmpty() {
        flag = false;
        txtPurchaseId.setText(" ");
        txtPurchaseDate.setText("");
        cmbSuplierId.setEditable(true);
        cmbSuplierId.setSelectedItem("");
        cmbSuplierId.setEditable(false);
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
        txtNarr.setText("");
        flag = true;
    }

    public void deleteRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from purchase_details";
            srecord = stmt.executeQuery(str);
            srecord.absolute(ctr);
            srecord.deleteRow();
            if (cur < tot)
                tot--;
            else if (cur == 1 && cur > 1)
                cur = --tot;
            else if (cur == tot && cur == 1) {
                cur = tot = 0;
                setAllControlEmpty();
            }
            JOptionPane.showMessageDialog(this, "Record Deleted", "Purchase Details", 1);
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Exception In Delete Record: " + e.getMessage(), "Purchase Details", 0);
        }
    }

    public void addRecord() {
        try {
            String str = "insert into purchase_details values(?,?,?,?,?,?,?,?,?,?,?,?)";
            pstmt = MdiForm.sconnect.prepareStatement(str);
            pstmt.setString(1, txtPurchaseId.getText());
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            java.util.Date date = sdf.parse(txtPurchaseDate.getText());
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            pstmt.setString(2, sdf.format(date));
            pstmt.setString(3, cmbSuplierId.getSelectedItem().toString());
            pstmt.setString(4, cmbItemCode.getSelectedItem().toString());
            pstmt.setString(5, txtDescription.getText());
            pstmt.setInt(6, Integer.parseInt(txtQty.getText()));
            pstmt.setFloat(7, Float.parseFloat(txtRate.getText()));
            pstmt.setFloat(8, Float.parseFloat(txtTotalAmt.getText()));
            pstmt.setFloat(9, Float.parseFloat(txtVatAmt.getText()));
            pstmt.setFloat(10, Float.parseFloat(txtConAmt.getText()));
            pstmt.setFloat(11, Float.parseFloat(txtNetAmt.getText()));
            pstmt.setString(12, txtNarr.getText());

            pstmt.executeUpdate();
            ++tot;
            JOptionPane.showMessageDialog(this, "Record Added", "Purchase Details", 1);
            showRecord(cur);
        } catch (SQLException | java.text.ParseException e) {
            JOptionPane.showMessageDialog(this, "Exception In Add Record: " + e.getMessage(), "Purchase Details", 0);
        }
    }

    public void findRecord() {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from purchase_details";
            srecord = stmt.executeQuery(str);
            String purid = "";
            int ctr = 0;
            while (srecord.next()) {
                purid = srecord.getString("purchase_id");
                if (purid.equalsIgnoreCase(txtPurchaseId.getText())) {
                    cur = ++ctr;
                    flag = true;
                    break;
                }
                ctr++;
            }
            if (flag == false)
                JOptionPane.showMessageDialog(this, "Record Not Found", "Purchase Details", 0);
            else
                showRecord(cur);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Exception In Find Record: " + e.getMessage(), "Purchase Details", 0);
        }
    }

    public void updateRecord() {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from purchase_details";
            srecord = stmt.executeQuery(str);
            srecord.absolute(cur);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            java.util.Date date = sdf.parse(txtPurchaseDate.getText());
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            srecord.updateString("purchase_date", sdf.format(date));
            srecord.updateString("supplier_id", cmbSuplierId.getSelectedItem().toString());
            srecord.updateString("item_code", cmbItemCode.getSelectedItem().toString());
            srecord.updateString("description", txtDescription.getText());
            srecord.updateInt("qty", Integer.parseInt(txtQty.getText()));
            srecord.updateFloat("rate", Float.parseFloat(txtRate.getText()));
            srecord.updateFloat("total_amt", Float.parseFloat(txtTotalAmt.getText()));
            srecord.updateFloat("vat_amt", Float.parseFloat(txtVatAmt.getText()));
            srecord.updateFloat("con_amt", Float.parseFloat(txtConAmt.getText()));
            srecord.updateFloat("net_amt", Float.parseFloat(txtNetAmt.getText()));
            srecord.updateString("narration", txtNarr.getText());
            srecord.updateRow();
            JOptionPane.showMessageDialog(this, "Record Updated", "Purchase Details", 1);
            showRecord(cur);
        } catch (SQLException | java.text.ParseException e) {
            JOptionPane.showMessageDialog(this, "Exception In Update Record: " + e.getMessage(), "Purchase Details", 0);
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
            txtPurchaseId.requestFocus();
        } else if (ae.getSource() == btnSave) {
            addRecord();   
        } else if (ae.getSource() == btnFind) {
            findRecord();
        } else if (ae.getSource() == btnUpdate) {
            updateRecord();
        } 
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        try {
            if(ie.getSource() == cmbItemCode && flag) {
                String str = "select description, rate from item_details where item_code="
                        + cmbItemCode.getSelectedItem().toString();
                pstmt = MdiForm.sconnect.prepareStatement(str);
                ResultSet srecord1 = pstmt.executeQuery();
                srecord1.next();
                txtDescription.setEditable(true);
                txtDescription.setText(srecord1.getString(1));
                txtDescription.setEditable(false);
                txtRate.setEditable(true);
                txtRate.setText(srecord1.getString(2));
                txtRate.setEditable(false);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Exception In Item Code Item State Changed: " + e.getMessage(),
                    "Purchase Details", 0);
        }
    }

    public void keyPressed(KeyEvent ke) {}

    public void keyReleased(KeyEvent ke) {
        if(ke.getSource() == txtQty) {
            String str1, str2, str3, str4;
            double qty, rate, total, vat, concessaion, net;
            qty = Double.parseDouble(txtQty.getText());
            rate = Double.parseDouble(txtRate.getText());
            total = qty * rate;
            vat = total * 1/100;
            concessaion = total * 10/100;
            net = total + vat - concessaion;
            str1 = String.format("%.2f", total);
            txtTotalAmt.setEditable(true);
            txtTotalAmt.setText(str1);
            txtTotalAmt.setEditable(false);

            str2 = String.format("%.2f", vat);
            txtVatAmt.setEditable(true);
            txtVatAmt.setText(str2);
            txtVatAmt.setEditable(false);

            str3 = String.format("%.2f", concessaion);
            txtConAmt.setEditable(true);
            txtConAmt.setText(str3);
            txtConAmt.setEditable(false);

            str4 = String.format("%.2f", net);
            txtNetAmt.setEditable(true);
            txtNetAmt.setText(str4);
            txtNetAmt.setEditable(false);
        }
    }

    public void keyTyped(KeyEvent ke) {}

    public static void main(String[] args) {
        new PurchaseDetails();
    }

}
