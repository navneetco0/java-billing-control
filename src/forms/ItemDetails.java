package forms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ItemDetails extends JInternalFrame implements MouseListener, ActionListener {
    private ResultSet srecord;
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot, cur;

    private JLabel lblItemCode, lblDescription, lblRate, lblDate, lblNarr;

    private JTextField txtItemCode, txtDescription, txtRate, txtDate;
    private JTextArea txtNarr;
    private JScrollPane jscNarr;

    private JButton btnSave, btnDelete, btnUpdate, btnFind, btnRefresh;
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JTextField txtNavigation;

    private Container c;
    private ItemDtlBackgorund obj1;
    private ItemDtlHeading obj2;

    private class ItemDtlBackgorund extends JPanel {
        private ItemDtlBackgorund() {
            setLayout(null);

            lblItemCode = new JLabel("Item Code");
            lblDescription = new JLabel("Description");
            lblRate = new JLabel("Rate");
            lblDate = new JLabel("Date");
            lblNarr = new JLabel("Narration");

            txtItemCode = new JTextField();
            txtDescription = new JTextField();
            txtRate = new JTextField();
            txtDate = new JTextField();
            txtNarr = new JTextArea();
            jscNarr = new JScrollPane(txtNarr, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

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

            lblItemCode.setBounds(50, 50, 100, 30);
            txtItemCode.setBounds(160, 50, 100, 30);    
            lblDescription.setBounds(50, 90, 100, 30);
            txtDescription.setBounds(160, 90, 100, 30);
            lblRate.setBounds(50, 130, 100, 30);
            txtRate.setBounds(160, 130, 100, 30);
            lblDate.setBounds(50, 170, 100, 30);
            txtDate.setBounds(160, 170, 100, 30);
            lblNarr.setBounds(50, 210, 100, 30);
            jscNarr.setBounds(160, 210, 200, 100);  

            btnSave.setBounds(50, 330, 80, 30);
            btnDelete.setBounds(140, 330, 80, 30);
            btnUpdate.setBounds(230, 330, 80, 30);
            btnFind.setBounds(320, 330, 80, 30);
            btnRefresh.setBounds(410, 330, 80, 30);
            btnFirst.setBounds(50, 370, 80, 30);
            btnPrev.setBounds(140, 370, 80, 30);
            txtNavigation.setBounds(230, 370, 80, 30);
            btnNext.setBounds(320, 370, 80, 30);
            btnLast.setBounds(410, 370, 80, 30);

            add(txtItemCode);
            add(lblDescription);
            add(txtDescription);
            add(lblRate);
            add(txtRate);
            add(lblDate);
            add(txtDate);
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

    private class ItemDtlHeading extends JPanel {
        private ItemDtlHeading() {
            setLayout(null);
            JLabel lblHeading = new JLabel("Sale Details");
            lblHeading.setFont(new Font("Arial", Font.BOLD, 24));
            lblHeading.setBounds(150, 10, 200, 30);
            add(lblHeading);
        }
    }

    public ItemDetails() {
        super("Item Details", false, true, false, true);
        c = getContentPane();
        c.setLayout(null);
        obj1 = new ItemDtlBackgorund();
        obj2 = new ItemDtlHeading();
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

        c.add(obj1);
        c.add(obj2);
        setSize(550, 620);
        setLocation(100, 50);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
        try {
            String str1 = "select * from item_details";
            pstmt = MdiForm.sconnect.prepareStatement(str1);
            srecord = pstmt.executeQuery();
            while (srecord.next()) {
                tot++;
            }
            if(tot>0)
                cur=1;

            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Item Details", 0);
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
                String str = "select * from item_details";
                srecord = stmt.executeQuery(str);
                srecord.absolute(ctr);

                txtItemCode.setText(srecord.getString("item_code"));
                txtDescription.setText(srecord.getString("description"));
                txtRate.setText(srecord.getString("rate"));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sdf.parse(srecord.getString("date"));
                sdf = new SimpleDateFormat("dd-MM-yyyy");
                txtDate.setText(sdf.format(date));
                txtNarr.setText(srecord.getString("narration"));
            } catch (SQLException | java.text.ParseException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Item Details", 0);
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
        txtItemCode.setText("");
        txtDescription.setText("");
        txtRate.setText("");
        txtDate.setText("");
        txtNarr.setText("");
    }

    public void deleteRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from item_details";
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
            String str = "insert into item_details values(?,?,?,?,?,?,?,?,?,?)";
            pstmt = MdiForm.sconnect.prepareStatement(str);

            pstmt.setString(1, txtItemCode.getText());
            pstmt.setString(2, txtDate.getText());
            pstmt.setString(3, txtDescription.getText());
            pstmt.setString(4, txtRate.getText());
            pstmt.setString(5, txtNarr.getText());
          

            pstmt.executeUpdate();
            ++tot;
            JOptionPane.showMessageDialog(this, "Record Added", "Item Details", 1);
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Item Details", 0);
        }
    }

    public void findRecord() {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from item_details";
            srecord = stmt.executeQuery(str);
            String icode = "";
            int ctr = 0;
            boolean flag = false;
            while (srecord.next()) {
                icode = srecord.getString("icode");
                if (icode.equalsIgnoreCase(txtItemCode.getText())) {
                    cur = ++ctr;
                    flag = true;
                    break;
                }
                ctr++;
            }
            if (!flag) {
                JOptionPane.showMessageDialog(this, "Record Not Found", "Item Details", 0);
            }
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Sale Details", 0);
        }
    }

    public void udpateRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from item_details";
            srecord = stmt.executeQuery(str);
            srecord.absolute(ctr);

            srecord.updateString(1, txtItemCode.getText());
            srecord.updateString(2, txtDate.getText());
            srecord.updateString(3, txtDescription.getText());
            srecord.updateString(4, txtRate.getText());
            srecord.updateString(5, txtNarr.getText());

            srecord.updateRow();
            JOptionPane.showMessageDialog(this, "Record Updated", "Item Details", 1);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Exception In Update Record:" + e.getMessage(), "Item Details", 0);
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

    public static void main(String[] args) {
        new ItemDetails();
    }
}
