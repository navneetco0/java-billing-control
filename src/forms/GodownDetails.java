package forms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.sql.*;

public class GodownDetails extends JInternalFrame implements MouseListener, ActionListener {
    private ResultSet srecord;
    private Statement stmt;
    private PreparedStatement pstmt;
    private int tot, cur;

    private JLabel lblGodownNo, lblName, lblAddr, lblPhone1, lblPhone2, lblInchargeName, lblNarr;

    private JTextField txtGodownNo, txtName, txtPhone1, txtPhone2, txtInchargeName;

    private JTextArea txtAddr, txtNarr;
    private JScrollPane jscAddr, jscNarr;

    private JButton btnSave, btnDelete, btnUpdate, btnFind, btnRefresh;
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JTextField txtNavigation;

    private Container c;
    private GodownDetailsDtlBackground obj1;
    private GodownDetailsDtlHeading obj2;

    private class GodownDetailsDtlBackground extends JPanel {
        private GodownDetailsDtlBackground() {
            setLayout(null);

            lblGodownNo=new JLabel("Godown No");
            lblName = new JLabel("Name");
            lblAddr = new JLabel("Address");
            lblPhone1 = new JLabel("Phone No 1");
            lblPhone2 = new JLabel("Phone No 2");
            lblInchargeName = new JLabel("Incharge Name");
            lblNarr = new JLabel("Narration");

            txtGodownNo=new JTextField();
            txtName = new JTextField();
            txtAddr = new JTextArea();
            jscAddr = new JScrollPane(txtAddr);
            txtPhone1 = new JTextField();
            txtPhone2 = new JTextField();
            txtInchargeName = new JTextField();
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

            lblGodownNo.setBounds(50, 30, 100, 30);
            txtGodownNo.setBounds(150, 30, 100, 30);
            lblName.setBounds(50, 70, 100, 30);
            txtName.setBounds(150, 70, 100, 30);
            lblAddr.setBounds(50, 110, 100, 30);
            txtAddr.setBounds(150, 110, 100, 30);
            lblPhone1.setBounds(50, 150, 100, 30);
            txtPhone1.setBounds(150, 150, 100, 30);
            lblPhone2.setBounds(50, 190, 100, 30);
            txtPhone2.setBounds(150, 190, 100, 30);
            lblNarr.setBounds(50, 230, 100, 30);
            txtNarr.setBounds(150, 230, 100, 30);

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

            add(lblGodownNo);
            add(txtGodownNo);
            add(lblName);
            add(txtName);
            add(lblAddr);
            add(txtAddr);
            add(lblPhone1);
            add(txtPhone1);
            add(lblPhone2);
            add(txtPhone2);
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

    private class GodownDetailsDtlHeading extends JPanel {
        private GodownDetailsDtlHeading() {
            setLayout(null);
            JLabel lblHeading = new JLabel("Sale Details");
            lblHeading.setFont(new Font("Arial", Font.BOLD, 24));
            lblHeading.setBounds(150, 10, 200, 30);
            add(lblHeading);
        }
    }

    public GodownDetails() {
        super("Sale Details", false, true, false, true);
        c = getContentPane();
        c.setLayout(null);
        obj1 = new GodownDetailsDtlBackground();
        obj2 = new GodownDetailsDtlHeading();
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
            String str1 = "select * from godown_details";
            pstmt = MdiForm.sconnect.prepareStatement(str1);
            srecord = pstmt.executeQuery();

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
                String str = "select * from godown_details";
                srecord = stmt.executeQuery(str);
                srecord.absolute(ctr);

                txtGodownNo.setText(srecord.getString("godown_no"));
                txtName.setText(srecord.getString("godown_name"));
                txtAddr.setText(srecord.getString("address"));
                txtPhone1.setText(srecord.getString("phone1"));
                txtPhone2.setText(srecord.getString("phone2"));
                txtInchargeName.setText(srecord.getString("incharge_name"));
                txtNarr.setText(srecord.getString("narration"));
            } catch (SQLException e) {
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
        txtGodownNo.setText("");
        txtName.setText("");
        txtAddr.setText("");
        txtPhone1.setText("");
        txtPhone2.setText("");
        txtInchargeName.setText("");
        txtNarr.setText("");
    }

    public void deleteRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from godown_details";
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
            String str = "insert into godown_details values(?,?,?,?,?,?,?,?,?,?)";
            pstmt = MdiForm.sconnect.prepareStatement(str);

            pstmt.setString(1, txtGodownNo.getText());
            pstmt.setInt(2, Integer.parseInt(txtName.getText()));
            pstmt.setInt(3, Integer.parseInt(txtAddr.getText()));
            pstmt.setInt(4, Integer.parseInt(txtPhone1.getText()));
            pstmt.setInt(5, Integer.parseInt(txtPhone2.getText()));
            pstmt.setInt(6, Integer.parseInt(txtInchargeName.getText()));
            pstmt.setInt(7, Integer.parseInt(txtNarr.getText()));

            pstmt.executeUpdate();
            ++tot;
            JOptionPane.showMessageDialog(this, "Record Added", "Sale Details", 1);
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Sale Details", 0);
        }
    }

    public void findRecord() {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from godown_details";
            srecord = stmt.executeQuery(str);
            String godno = "";
            int ctr = 0;
            boolean flag = false;
            while (srecord.next()) {
                godno = srecord.getString("godown_no");
                if (godno.equalsIgnoreCase(txtGodownNo.getText())) {
                    cur = ++ctr;
                    flag = true;
                    break;
                }
                ctr++;
            }
            if (!flag) {
                JOptionPane.showMessageDialog(this, "Record Not Found", "Godown Details", 0);
            }
            showRecord(cur);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Godown Details", 0);
        }
    }

    public void udpateRecord(int ctr) {
        try {
            stmt = MdiForm.sconnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            String str = "select * from godown_details";
            srecord = stmt.executeQuery(str);
            srecord.absolute(ctr);

            srecord.updateString(1, txtGodownNo.getText());
            srecord.updateString(2, txtName.getText());
            srecord.updateString(3, txtAddr.getText());
            srecord.updateInt(4, Integer.parseInt(txtPhone1.getText()));
            srecord.updateInt(5, Integer.parseInt(txtPhone2.getText()));
            srecord.updateInt(6, Integer.parseInt(txtInchargeName.getText()));
            srecord.updateInt(7, Integer.parseInt(txtNarr.getText()));

            srecord.updateRow();
            JOptionPane.showMessageDialog(this, "Record Updated", "Sale Details", 1);
        } catch (SQLException e) {
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

    public static void main(String[] args) {
        new GodownDetails();
    }
}
