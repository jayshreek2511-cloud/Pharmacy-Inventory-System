import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class DashboardPanel extends JPanel {
    private static final Color PINK=new Color(0xE9,0x1E,0x63),WHITE=Color.WHITE;
    private static final Color DARK=new Color(0x21,0x21,0x21),GRAY=new Color(0x75,0x75,0x75);
    private static final Color ALT=new Color(0xFF,0xF9,0xFA),BRD=new Color(0xE0,0xE0,0xE0);
    private static final Color GREEN=new Color(0x2E,0x7D,0x32),ORANGE=new Color(0xFF,0x8F,0x00);
    private static final Color RED=new Color(0xD3,0x2F,0x2F);
    private static final Font F12=new Font("Segoe UI",Font.PLAIN,12);
    private static final Font F13=new Font("Segoe UI",Font.PLAIN,13);
    private static final Font FB13=new Font("Segoe UI",Font.BOLD,13);
    private static final Font FB16=new Font("Segoe UI",Font.BOLD,16);
    private static final Font FB28=new Font("Segoe UI",Font.BOLD,28);
    private DefaultTableModel model;
    private JTable table;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel totalLbl,expLbl,lowLbl,revLbl;
    private String[] cols={"ID","Medicine Name","Category","Stock","Price (₹)","Expiry Date","Status","Action"};

    public DashboardPanel() {
        setLayout(new BorderLayout()); setOpaque(false);
        initModel();
        JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(buildStatCards(),BorderLayout.NORTH);
        JPanel mid=new JPanel(new BorderLayout(16,0)); mid.setOpaque(false);
        mid.setBorder(BorderFactory.createEmptyBorder(18,0,0,0));
        mid.add(buildTableSection(),BorderLayout.CENTER);
        mid.add(buildRightPanel(),BorderLayout.EAST);
        top.add(mid,BorderLayout.CENTER);
        add(top,BorderLayout.CENTER);
    }

    private void initModel() {
        model=new DefaultTableModel(cols,0){
            public boolean isCellEditable(int r,int c){return false;}
        };
        model.addRow(new Object[]{"1001","Dolo 650","Tablet","28","35.00","Jan 2027",status(28),""});
        model.addRow(new Object[]{"1002","Paracetamol","Tablet","50","20.00","Dec 2026",status(50),""});
        model.addRow(new Object[]{"1003","Amoxicillin 500mg","Capsule","15","60.00","Aug 2026",status(15),""});
        model.addRow(new Object[]{"1004","Azithromycin 250","Tablet","9","45.00","Jul 2026",status(9),""});
        model.addRow(new Object[]{"1005","Cetrizine 10mg","Tablet","2","18.00","Jun 2026",status(2),""});
        table=new JTable(model);
        sorter=new TableRowSorter<>(model);
        table.setRowSorter(sorter);
    }

    static String status(int s){return s>20?"In Stock":s>=10?"Low Stock":"Critical";}

    private JPanel buildStatCards() {
        JPanel row=new JPanel(new GridLayout(1,4,12,0)); row.setOpaque(false);
        row.setPreferredSize(new Dimension(0,120));
        totalLbl=new JLabel(String.valueOf(model.getRowCount()));
        expLbl=new JLabel("5"); lowLbl=new JLabel("8"); revLbl=new JLabel("₹12,450");
        row.add(statCard("📦","Total Medicines",totalLbl,new Color(0xFF,0x98,0x00),"All medicines in stock",GREEN));
        row.add(statCard("⏳","Expiring Soon",expLbl,new Color(0xFF,0xB3,0x00),"Within next 30 days",ORANGE));
        row.add(statCard("⚠️","Low Stock",lowLbl,new Color(0x7B,0x1F,0xA2),"Stock below minimum",GRAY));
        row.add(statCard("₹","Total Revenue",revLbl,GREEN,"This Month",GREEN));
        return row;
    }

    private JPanel statCard(String icon,String label,JLabel valLbl,Color ic,String sub,Color sc) {
        JPanel c=rndPanel(); c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS));
        c.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JLabel il=new JLabel(icon); il.setFont(new Font("Segoe UI Emoji",Font.PLAIN,24));
        il.setForeground(ic); il.setAlignmentX(0f);
        valLbl.setFont(FB28); valLbl.setForeground(DARK); valLbl.setAlignmentX(0f);
        JLabel ll=new JLabel(label); ll.setFont(F13); ll.setForeground(GRAY); ll.setAlignmentX(0f);
        JLabel sl=new JLabel(sub); sl.setFont(F12); sl.setForeground(sc); sl.setAlignmentX(0f);
        c.add(il);c.add(Box.createVerticalStrut(4));c.add(valLbl);
        c.add(Box.createVerticalStrut(2));c.add(ll);c.add(Box.createVerticalStrut(2));c.add(sl);
        return c;
    }

    private JPanel buildTableSection() {
        JPanel sec=new JPanel(new BorderLayout(0,10)); sec.setOpaque(false);
        JPanel hdr=new JPanel(new BorderLayout()); hdr.setOpaque(false);
        JLabel title=new JLabel("💊 Medicine Inventory"); title.setFont(FB16); title.setForeground(DARK);
        JButton addBtn=pinkBtn("+ Add Medicine");
        addBtn.addActionListener(e->showAddDialog());
        hdr.add(title,BorderLayout.WEST); hdr.add(addBtn,BorderLayout.EAST);
        sec.add(hdr,BorderLayout.NORTH);
        JPanel body=new JPanel(new BorderLayout(0,8)); body.setOpaque(false);
        JTextField search=new JTextField("  Search medicine by name, category or company...");
        search.setFont(F13); search.setForeground(new Color(0xAA,0xAA,0xAA));
        search.setPreferredSize(new Dimension(0,36));
        search.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BRD,1,true),BorderFactory.createEmptyBorder(4,12,4,12)));
        search.addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e){if(search.getText().contains("Search")){search.setText("");search.setForeground(DARK);}}
            public void focusLost(FocusEvent e){if(search.getText().isEmpty()){search.setText("  Search medicine by name, category or company...");search.setForeground(new Color(0xAA,0xAA,0xAA));sorter.setRowFilter(null);}}
        });
        search.addKeyListener(new KeyAdapter(){
            public void keyReleased(KeyEvent e){
                String t=search.getText().trim();
                if(t.isEmpty()||t.contains("Search")){sorter.setRowFilter(null);}
                else{sorter.setRowFilter(RowFilter.orFilter(java.util.Arrays.asList(
                    RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(t),1),
                    RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(t),2))));}
            }
        });
        body.add(search,BorderLayout.NORTH);
        styleTable();
        JScrollPane sp=new JScrollPane(table); sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE);
        body.add(sp,BorderLayout.CENTER);
        JPanel pag=new JPanel(new BorderLayout()); pag.setOpaque(false);
        pag.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        JLabel info=new JLabel("Showing 1 to "+model.getRowCount()+" of "+model.getRowCount()+" entries");
        info.setFont(F12); info.setForeground(GRAY);
        JPanel pages=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0)); pages.setOpaque(false);
        for(String p:new String[]{"1","2","3","...","24"}){
            boolean first=p.equals("1");
            JButton pb=new JButton(p){
                protected void paintComponent(Graphics g){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(first?PINK:new Color(0xF5,0xF5,0xF5));
                    g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                    g2.dispose();super.paintComponent(g);
                }
            };
            pb.setFont(F12);pb.setForeground(first?WHITE:DARK);
            pb.setOpaque(false);pb.setContentAreaFilled(false);pb.setBorderPainted(false);pb.setFocusPainted(false);
            pb.setPreferredSize(new Dimension(32,28));pb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pages.add(pb);
        }
        pag.add(info,BorderLayout.WEST); pag.add(pages,BorderLayout.EAST);
        body.add(pag,BorderLayout.SOUTH);
        sec.add(body,BorderLayout.CENTER);
        return sec;
    }

    private void styleTable() {
        table.setFont(F13); table.setRowHeight(40);
        table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionBackground(new Color(0xFC,0xE4,0xEC));
        table.setSelectionForeground(DARK); table.setFocusable(false);
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int r,int c){
                JLabel l=(JLabel)super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                int mr=t.convertRowIndexToModel(r);
                if(!sel) l.setBackground(r%2==0?WHITE:ALT);
                l.setForeground(DARK);
                l.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
                if(c==6){
                    String s=String.valueOf(v);
                    if("In Stock".equals(s))l.setForeground(GREEN);
                    else if("Low Stock".equals(s))l.setForeground(ORANGE);
                    else l.setForeground(RED);
                }
                if(c==7){l.setText("✏️  🗑️");l.setHorizontalAlignment(SwingConstants.CENTER);}
                return l;
            }
        });
        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                int col=table.columnAtPoint(e.getPoint());
                int row=table.rowAtPoint(e.getPoint());
                if(col==7&&row>=0){
                    int mr=table.convertRowIndexToModel(row);
                    Rectangle cell=table.getCellRect(row,col,false);
                    int relX=e.getX()-cell.x;
                    if(relX>cell.width/2){
                        String name=(String)model.getValueAt(mr,1);
                        if(JOptionPane.showConfirmDialog(table,"Are you sure you want to delete "+name+"?",
                            "Delete",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                            model.removeRow(mr); updateStats();
                        }
                    }
                }
            }
        });
        JTableHeader th=table.getTableHeader();
        th.setFont(FB13);th.setBackground(new Color(0xF5,0xF5,0xF5));th.setForeground(DARK);
        th.setPreferredSize(new Dimension(0,40));
        th.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BRD));th.setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);
        table.getColumnModel().getColumn(7).setPreferredWidth(70);
    }

    private void showAddDialog() {
        JDialog dlg=new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"Add New Medicine",true);
        dlg.setSize(500,450); dlg.setLocationRelativeTo(this); dlg.setResizable(false);
        JPanel p=new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(24,28,24,28)); p.setBackground(WHITE);
        JLabel t=new JLabel("Add New Medicine"); t.setFont(new Font("Segoe UI",Font.BOLD,20));
        t.setForeground(DARK); t.setAlignmentX(0f);
        p.add(t); p.add(Box.createVerticalStrut(20));
        JTextField nameF=addField(p,"Medicine Name");
        JComboBox<String> catF=new JComboBox<>(new String[]{"Tablet","Capsule","Syrup","Injection"});
        catF.setFont(F13); catF.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        JPanel cp=new JPanel(new BorderLayout()); cp.setOpaque(false); cp.setMaximumSize(new Dimension(Integer.MAX_VALUE,60));
        cp.setAlignmentX(0f);
        JLabel cl=new JLabel("Category"); cl.setFont(FB13); cl.setForeground(DARK);
        cp.add(cl,BorderLayout.NORTH); cp.add(catF,BorderLayout.CENTER);
        p.add(cp); p.add(Box.createVerticalStrut(10));
        JTextField stockF=addField(p,"Stock (number)");
        JTextField priceF=addField(p,"Price ₹");
        JTextField expF=addField(p,"Expiry Date (MM/YYYY)");
        JTextField compF=addField(p,"Company Name");
        p.add(Box.createVerticalStrut(16));
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); btns.setOpaque(false);
        btns.setAlignmentX(0f); btns.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        JButton cancel=new JButton("Cancel");
        cancel.setFont(FB13);cancel.setForeground(GRAY);cancel.setBackground(WHITE);
        cancel.setBorder(BorderFactory.createLineBorder(BRD,1,true));
        cancel.setPreferredSize(new Dimension(100,36));
        cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancel.addActionListener(e->dlg.dispose());
        JButton addB=pinkBtn("Add Medicine");
        addB.setPreferredSize(new Dimension(140,36));
        addB.addActionListener(e->{
            String nm=nameF.getText().trim(), stk=stockF.getText().trim();
            String pr=priceF.getText().trim(), exp=expF.getText().trim(), co=compF.getText().trim();
            if(nm.isEmpty()||stk.isEmpty()||pr.isEmpty()||exp.isEmpty()||co.isEmpty()){
                JOptionPane.showMessageDialog(dlg,"Please fill all fields!","Error",JOptionPane.ERROR_MESSAGE);return;
            }
            int stkVal;
            try{stkVal=Integer.parseInt(stk);}catch(Exception ex){
                JOptionPane.showMessageDialog(dlg,"Stock must be a number!","Error",JOptionPane.ERROR_MESSAGE);return;}
            try{Double.parseDouble(pr);}catch(Exception ex){
                JOptionPane.showMessageDialog(dlg,"Price must be a valid number!","Error",JOptionPane.ERROR_MESSAGE);return;}
            int nextId=1006;
            for(int i=0;i<model.getRowCount();i++){
                int id=Integer.parseInt(String.valueOf(model.getValueAt(i,0)));
                if(id>=nextId)nextId=id+1;
            }
            model.addRow(new Object[]{String.valueOf(nextId),nm,(String)catF.getSelectedItem(),stk,pr,exp,status(stkVal),""});
            updateStats(); dlg.dispose();
            JOptionPane.showMessageDialog(this,"Medicine added successfully!","Success",JOptionPane.INFORMATION_MESSAGE);
        });
        btns.add(cancel); btns.add(addB);
        p.add(btns);
        dlg.setContentPane(p); dlg.setVisible(true);
    }

    private JTextField addField(JPanel p,String label) {
        JPanel fp=new JPanel(new BorderLayout()); fp.setOpaque(false);
        fp.setMaximumSize(new Dimension(Integer.MAX_VALUE,52)); fp.setAlignmentX(0f);
        JLabel l=new JLabel(label); l.setFont(FB13); l.setForeground(DARK);
        JTextField tf=new JTextField(); tf.setFont(F13);
        tf.setPreferredSize(new Dimension(0,32));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BRD,1,true),BorderFactory.createEmptyBorder(4,10,4,10)));
        fp.add(l,BorderLayout.NORTH); fp.add(tf,BorderLayout.CENTER);
        p.add(fp); p.add(Box.createVerticalStrut(8));
        return tf;
    }

    void updateStats(){totalLbl.setText(String.valueOf(model.getRowCount()));}

    private JPanel buildRightPanel() {
        JPanel r=new JPanel(); r.setLayout(new BoxLayout(r,BoxLayout.Y_AXIS));
        r.setOpaque(false); r.setPreferredSize(new Dimension(260,0));
        JPanel alerts=rndPanel(); alerts.setLayout(new BoxLayout(alerts,BoxLayout.Y_AXIS));
        alerts.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        alerts.setMaximumSize(new Dimension(260,250));
        JPanel ah=new JPanel(new BorderLayout()); ah.setOpaque(false);
        ah.setMaximumSize(new Dimension(260,24)); ah.setAlignmentX(0f);
        JLabel at=new JLabel("🔔 Stock Alerts"); at.setFont(FB16); at.setForeground(DARK);
        JLabel va=new JLabel("View All"); va.setFont(F12); va.setForeground(PINK);
        va.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ah.add(at,BorderLayout.WEST); ah.add(va,BorderLayout.EAST);
        alerts.add(ah); alerts.add(Box.createVerticalStrut(12));
        alerts.add(alertItem("Dolo 650",28));alerts.add(Box.createVerticalStrut(8));
        alerts.add(alertItem("Amoxicillin 500mg",15));alerts.add(Box.createVerticalStrut(8));
        alerts.add(alertItem("Cetrizine 10mg",2));
        r.add(alerts); r.add(Box.createVerticalStrut(14));
        JPanel qa=rndPanel(); qa.setLayout(new BoxLayout(qa,BoxLayout.Y_AXIS));
        qa.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        qa.setMaximumSize(new Dimension(260,250));
        JLabel qt=new JLabel("⚡ Quick Actions"); qt.setFont(FB16); qt.setForeground(DARK); qt.setAlignmentX(0f);
        qa.add(qt); qa.add(Box.createVerticalStrut(12));
        JPanel grid=new JPanel(new GridLayout(2,2,8,8)); grid.setOpaque(false);
        grid.setAlignmentX(0f); grid.setMaximumSize(new Dimension(240,160));
        grid.add(actionBtn("💊","Add Medicine")); grid.add(actionBtn("🧾","New Bill"));
        grid.add(actionBtn("🛒","Purchase")); grid.add(actionBtn("📈","View Reports"));
        qa.add(grid); r.add(qa); r.add(Box.createVerticalGlue());
        return r;
    }

    private JPanel alertItem(String name,int qty) {
        JPanel i=new JPanel(new BorderLayout(8,0)); i.setOpaque(false);
        i.setMaximumSize(new Dimension(240,44)); i.setAlignmentX(0f);
        i.setBorder(BorderFactory.createEmptyBorder(6,0,6,0));
        JLabel ic=new JLabel("💊"); ic.setFont(new Font("Segoe UI Emoji",Font.PLAIN,18));
        JPanel tx=new JPanel(); tx.setLayout(new BoxLayout(tx,BoxLayout.Y_AXIS)); tx.setOpaque(false);
        JLabel n=new JLabel(name); n.setFont(FB13); n.setForeground(DARK);
        JLabel s=new JLabel("Only "+qty+" left in stock"); s.setFont(F12); s.setForeground(GRAY);
        tx.add(n); tx.add(s);
        i.add(ic,BorderLayout.WEST); i.add(tx,BorderLayout.CENTER);
        return i;
    }

    JPanel createMedicinesPage() {
        JPanel p=new JPanel(new BorderLayout(0,10)); p.setOpaque(false);
        JPanel hdr=new JPanel(new BorderLayout()); hdr.setOpaque(false);
        JLabel t=new JLabel("💊 All Medicines"); t.setFont(FB16); t.setForeground(DARK);
        JButton ab=pinkBtn("+ Add Medicine"); ab.addActionListener(e->showAddDialog());
        hdr.add(t,BorderLayout.WEST); hdr.add(ab,BorderLayout.EAST);
        p.add(hdr,BorderLayout.NORTH);
        JScrollPane sp=new JScrollPane(table); sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE);
        p.add(sp,BorderLayout.CENTER);
        return p;
    }

    JPanel createStockAlertsPage() {
        JPanel p=new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setOpaque(false);
        JLabel t=new JLabel("⚠️ Stock Alerts — Low & Critical Items"); t.setFont(FB16);
        t.setForeground(DARK); t.setAlignmentX(0f);
        p.add(t); p.add(Box.createVerticalStrut(16));
        for(int i=0;i<model.getRowCount();i++){
            String st=String.valueOf(model.getValueAt(i,6));
            if(!"In Stock".equals(st)){
                String nm=(String)model.getValueAt(i,1);
                int qty=Integer.parseInt(String.valueOf(model.getValueAt(i,3)));
                JPanel card=rndPanel(); card.setLayout(new BorderLayout(12,0));
                card.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE,60)); card.setAlignmentX(0f);
                JLabel ic=new JLabel("💊"); ic.setFont(new Font("Segoe UI Emoji",Font.PLAIN,22));
                JPanel info=new JPanel(); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS)); info.setOpaque(false);
                JLabel nl=new JLabel(nm+" — "+st); nl.setFont(FB13);
                nl.setForeground("Critical".equals(st)?RED:ORANGE);
                JLabel sl=new JLabel("Only "+qty+" left in stock"); sl.setFont(F12); sl.setForeground(GRAY);
                info.add(nl); info.add(sl);
                card.add(ic,BorderLayout.WEST); card.add(info,BorderLayout.CENTER);
                p.add(card); p.add(Box.createVerticalStrut(8));
            }
        }
        if(p.getComponentCount()==2){
            JLabel none=new JLabel("All medicines are well stocked!");
            none.setFont(F13);none.setForeground(GREEN);none.setAlignmentX(0f);
            p.add(none);
        }
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JButton pinkBtn(String text) {
        JButton b=new JButton(text){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PINK);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),20,20));
                g2.dispose();super.paintComponent(g);
            }
        };
        b.setFont(FB13);b.setForeground(WHITE);b.setOpaque(false);b.setContentAreaFilled(false);
        b.setBorderPainted(false);b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140,32));
        return b;
    }

    private JButton actionBtn(String icon,String label) {
        JButton b=new JButton("<html><center>"+icon+"<br><span style='font-size:10px'>"+label+"</span></center></html>"){
            boolean hov; {
                addMouseListener(new MouseAdapter(){
                    public void mouseEntered(MouseEvent e){hov=true;repaint();}
                    public void mouseExited(MouseEvent e){hov=false;repaint();}
                });
            }
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov?new Color(0xFC,0xE4,0xEC):WHITE);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),12,12));
                g2.setColor(BRD);g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,12,12));
                g2.dispose();super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI",Font.PLAIN,20));b.setForeground(PINK);
        b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(100,70));
        return b;
    }

    private JPanel rndPanel() {
        return new JPanel(){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),12,12));
                g2.setColor(BRD);g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,12,12));
                g2.dispose();
            }
        };
    }
}
