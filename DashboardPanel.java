import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class DashboardPanel extends JPanel {
    private static final Color PINK=new Color(0xC9,0x63,0x7A),WHITE=Color.WHITE;
    private static final Color DARK=new Color(0x21,0x21,0x21),GRAY=new Color(0x75,0x75,0x75);
    private static final Color ALT=new Color(0xFF,0xF9,0xFA),BRD=new Color(0xF0,0xE0,0xE6);
    private static final Color SEARCH_BRD=new Color(0xED,0xD5,0xDC),TBL_BRD=new Color(0xF5,0xE8,0xEC);
    private static final Color TBL_HDR_BG=new Color(0xFF,0xF5,0xF7),TBL_HDR_FG=new Color(0x8B,0x45,0x58);
    private static final Color TBL_HDR_BR=new Color(0xF0,0xD8,0xDF);
    private static final Color CARD1_BG=new Color(0xFF,0xF0,0xF5),CARD2_BG=new Color(0xFF,0xFB,0xF0);
    private static final Color CARD3_BG=new Color(0xF5,0xF0,0xFF),CARD4_BG=new Color(0xF0,0xFF,0xF5);
    private static final Color BADGE_GREEN_BG=new Color(0xE8,0xF5,0xE9),BADGE_ORANGE_BG=new Color(0xFF,0xF3,0xE0);
    private static final Color BADGE_ORANGE_FG=new Color(0xE6,0x51,0x00);
    private static final Color GREEN=new Color(0x2E,0x7D,0x32),ORANGE=new Color(0xFF,0x8F,0x00);
    private static final Color RED=new Color(0xD3,0x2F,0x2F);
    private static final Font F12=new Font("Segoe UI",Font.PLAIN,12);
    private static final Font FB12=new Font("Segoe UI",Font.BOLD,12);
    private static final Font F13=new Font("Segoe UI",Font.PLAIN,13);
    private static final Font FB13=new Font("Segoe UI",Font.BOLD,13);
    private static final Font FB16=new Font("Segoe UI",Font.BOLD,16);
    private static final Font FB28=new Font("Segoe UI",Font.BOLD,28);
    private static DefaultTableModel sharedTableModel;
    private static JTable sharedTable;
    private static int currentPage = 1;
    private JTable medicinesTable;
    private JLabel showingLabel;
    private JLabel totalLbl,expLbl,lowLbl,revLbl;
    private JPanel pagPanel;
    static final int RPP=5;
    private String[] cols={"ID","Medicine Name","Category","Stock","Price (\u20b9)","Expiry Date","Status","Action"};

    private static void initializeSharedTable() {
        if(sharedTableModel != null && sharedTable != null) return;
        String[] columns = {"ID","Medicine Name","Category","Stock","Price (\u20b9)","Expiry Date","Status","Action"};
        sharedTableModel = new DefaultTableModel(columns, 0){
            public boolean isCellEditable(int r, int c){ return c == 7; }
        };
        sharedTableModel.addRow(new Object[]{1001,"Dolo 650","Tablet",28,"35.00","Jan 2027","In Stock",""});
        sharedTableModel.addRow(new Object[]{1002,"Paracetamol","Tablet",50,"20.00","Dec 2026","In Stock",""});
        sharedTableModel.addRow(new Object[]{1003,"Amoxicillin 500mg","Capsule",15,"60.00","Aug 2026","Low Stock",""});
        sharedTableModel.addRow(new Object[]{1004,"Azithromycin 250","Tablet",9,"45.00","Jul 2026","Low Stock",""});
        sharedTableModel.addRow(new Object[]{1005,"Cetrizine 10mg","Tablet",2,"18.00","Jun 2026","Critical",""});
        sharedTableModel.addRow(new Object[]{1006,"Ibuprofen 400mg","Tablet",35,"30.00","Mar 2027","In Stock",""});
        sharedTableModel.addRow(new Object[]{1007,"Metformin 500mg","Tablet",60,"25.00","Feb 2027","In Stock",""});
        sharedTableModel.addRow(new Object[]{1008,"Omeprazole 20mg","Capsule",5,"45.00","Sep 2026","Critical",""});
        sharedTableModel.addRow(new Object[]{1009,"Atorvastatin 10mg","Tablet",22,"55.00","Nov 2026","In Stock",""});
        sharedTableModel.addRow(new Object[]{1010,"Pantoprazole 40mg","Capsule",8,"40.00","Oct 2026","Low Stock",""});
        sharedTable = new JTable(sharedTableModel);
    }

    public DashboardPanel() {
        setLayout(new BorderLayout()); setOpaque(false);
        initializeSharedTable();
        initModel();
        JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(buildStatCards(),BorderLayout.NORTH);
        JPanel mid=new JPanel(new BorderLayout(16,0)); mid.setOpaque(false);
        mid.setBorder(BorderFactory.createEmptyBorder(20,0,0,0));
        mid.add(buildTableSection(),BorderLayout.CENTER);
        mid.add(buildRightPanel(),BorderLayout.EAST);
        top.add(mid,BorderLayout.CENTER);
        add(top,BorderLayout.CENTER);
    }

    private void initModel() {
        styleTable(sharedTable);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(sharedTableModel);
        sharedTable.setRowSorter(sorter);
        refreshTable();
    }
    void refreshTable(){
        int totalRows = sharedTableModel.getRowCount();
        int start = (currentPage - 1) * RPP;
        int end = Math.min(start + RPP, totalRows);

        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) sharedTable.getRowSorter();
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Object>() {
            public boolean include(Entry e) {
                int row = (int) e.getIdentifier();
                return row >= start && row < end;
            }
        });

        if(showingLabel != null) {
            showingLabel.setText("Showing " + (start + 1) + " to " + end + " of " + totalRows + " entries");
        }
        if(pagPanel != null) refreshPagination();
    }

    static String status(int s){return s>20?"In Stock":s>=10?"Low Stock":"Critical";}

    private JPanel buildStatCards() {
        JPanel row=new JPanel(new GridLayout(1,4,12,0)); row.setOpaque(false);
        row.setPreferredSize(new Dimension(0,130));
        row.setMinimumSize(new Dimension(0,120));
        totalLbl=new JLabel(String.valueOf(sharedTableModel.getRowCount()));
        expLbl=new JLabel("5"); lowLbl=new JLabel("8"); revLbl=new JLabel("₹12,450");
        row.add(statCard(statIcon(0,new Color(0xFF,0x98,0x00)),"Total Medicines",totalLbl,new Color(0xFF,0x98,0x00),"All medicines in stock",new Color(0x4C,0xAF,0x50),CARD1_BG));
        row.add(statCard(statIcon(1,new Color(0xFF,0xB3,0x00)),"Expiring Soon",expLbl,new Color(0xFF,0xB3,0x00),"Within next 30 days",new Color(0xFF,0x98,0x00),CARD2_BG));
        row.add(statCard(statIcon(2,new Color(0x7B,0x1F,0xA2)),"Low Stock",lowLbl,new Color(0x7B,0x1F,0xA2),"Stock below minimum",new Color(0x9E,0x9E,0x9E),CARD3_BG));
        row.add(statCard(statIcon(3,GREEN),"Total Revenue",revLbl,GREEN,"This Month",new Color(0x4C,0xAF,0x50),CARD4_BG));
        return row;
    }

    private JPanel statCard(Icon icon,String label,JLabel valLbl,Color ic,String sub,Color sc,Color bg) {
        JPanel c=new JPanel(){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xC9,0x63,0x7A,20));
                g2.fill(new RoundRectangle2D.Float(2,4,getWidth()-4,getHeight()-4,16,16));
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight()-2,16,16));
                g2.setColor(BRD);
                g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-3,16,16));
                g2.dispose();
            }
        };
        c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS)); c.setOpaque(false);
        c.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        JLabel il=new JLabel(icon); il.setFont(new Font("Segoe UI Emoji",Font.PLAIN,24));
        il.setForeground(ic); il.setAlignmentX(0f);
        valLbl.setFont(FB28); valLbl.setForeground(DARK); valLbl.setAlignmentX(0f);
        JLabel ll=new JLabel(label); ll.setFont(F13); ll.setForeground(GRAY); ll.setAlignmentX(0f);
        JLabel sl=new JLabel(sub); sl.setFont(FB12); sl.setForeground(sc); sl.setAlignmentX(0f);
        c.add(il);c.add(Box.createVerticalStrut(4));c.add(valLbl);
        c.add(Box.createVerticalStrut(2));c.add(ll);c.add(Box.createVerticalStrut(2));c.add(sl);
        return c;
    }

    private JPanel buildTableSection() {
        JPanel sec=new JPanel(new BorderLayout(0,10)); sec.setOpaque(false);
        JPanel hdr=new JPanel(new BorderLayout()); hdr.setOpaque(false);
        JLabel title=new JLabel(" Medicine Inventory"); title.setFont(FB16); title.setForeground(DARK);
        title.setIcon(sectionIcon(0)); title.setIconTextGap(6);
        JButton addBtn=pinkBtn("+ Add Medicine");
        addBtn.addActionListener(e->showAddDialog());
        hdr.add(title,BorderLayout.WEST); hdr.add(addBtn,BorderLayout.EAST);
        sec.add(hdr,BorderLayout.NORTH);
        JPanel body=new JPanel(new BorderLayout(0,8)); body.setOpaque(false);
        JTextField search=new JTextField("  Search medicine by name, category or company...");
        search.setFont(F13); search.setForeground(new Color(0xAA,0xAA,0xAA));
        search.setPreferredSize(new Dimension(0,42));
        search.setBorder(BorderFactory.createCompoundBorder(
            new AbstractBorder(){public void paintBorder(Component c,Graphics g,int x,int y,int w,int h){
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SEARCH_BRD);g2.draw(new RoundRectangle2D.Float(x,y,w-1,h-1,50,50));g2.dispose();}
                public Insets getBorderInsets(Component c){return new Insets(1,1,1,1);}},
            BorderFactory.createEmptyBorder(10,20,10,20)));
        search.addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e){if(search.getText().contains("Search")){search.setText("");search.setForeground(DARK);}}
            public void focusLost(FocusEvent e){if(search.getText().isEmpty()){search.setText("  Search medicine by name, category or company...");search.setForeground(new Color(0xAA,0xAA,0xAA));refreshTable();}}
        });
        search.addKeyListener(new KeyAdapter(){
            public void keyReleased(KeyEvent e){
                String t=search.getText().trim();
                TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) sharedTable.getRowSorter();
                if(t.isEmpty()||t.contains("Search")){
                    refreshTable();
                }
                else{
                    sorter.setRowFilter(RowFilter.orFilter(java.util.Arrays.asList(
                        RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(t),1),
                        RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(t),2))));
                }
            }
        });
        body.add(search,BorderLayout.NORTH);
        JScrollPane sp=new JScrollPane(sharedTable); sp.setBorder(BorderFactory.createLineBorder(TBL_BRD));
        sp.getViewport().setBackground(WHITE);
        body.add(sp,BorderLayout.CENTER);
        pagPanel=new JPanel(new BorderLayout()); pagPanel.setOpaque(false);
        pagPanel.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        showingLabel = new JLabel();
        showingLabel.setFont(F12); showingLabel.setForeground(GRAY);
        pagPanel.add(showingLabel, BorderLayout.WEST);
        refreshTable();
        body.add(pagPanel,BorderLayout.SOUTH);
        sec.add(body,BorderLayout.CENTER);
        return sec;
    }

    private void styleTable(JTable table) {
        table.setFont(F13); table.setRowHeight(40);
        table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionBackground(new Color(252,228,236));
        table.setSelectionForeground(new Color(180,60,90));
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class,null);
        table.setFocusable(true);
        table.getColumn("Action").setCellRenderer(new TableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int r,int c){
                JPanel ap=new JPanel(new FlowLayout(FlowLayout.CENTER,4,8));
                ap.setOpaque(true);ap.setBackground(sel?new Color(252,228,236):(r%2==0?WHITE:ALT));
                ap.add(iconLabel(0));ap.add(iconLabel(1));return ap;}});
        table.getColumn("Action").setCellEditor(new ActionEditor(this, table));
        table.setDefaultEditor(Object.class,null);
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int r,int c){
                JLabel l=(JLabel)super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                if(!sel)l.setBackground(r%2==0?WHITE:ALT);
                l.setForeground(DARK);l.setOpaque(true);
                l.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
                if(c==6){
                    String s=String.valueOf(v);
                    if("In Stock".equals(s)){l.setForeground(GREEN);l.setBackground(BADGE_GREEN_BG);}
                    else if("Low Stock".equals(s)){l.setForeground(BADGE_ORANGE_FG);l.setBackground(BADGE_ORANGE_BG);}
                    else{l.setForeground(RED);l.setBackground(new Color(0xFF,0xEB,0xEE));}
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                }
                return l;
            }
        });
        JTableHeader th=table.getTableHeader();
        th.setFont(FB13);th.setBackground(TBL_HDR_BG);th.setForeground(TBL_HDR_FG);
        th.setPreferredSize(new Dimension(0,40));
        th.setBorder(BorderFactory.createMatteBorder(0,0,1,0,TBL_HDR_BR));th.setReorderingAllowed(false);
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
            for(int i=0;i<sharedTableModel.getRowCount();i++){
                int id=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(i,0)));
                if(id>=nextId)nextId=id+1;
            }
            Object[] newRow=new Object[]{String.valueOf(nextId),nm,(String)catF.getSelectedItem(),stk,pr,exp,status(stkVal),""};
            sharedTableModel.addRow(newRow);
            currentPage=(int)Math.ceil(sharedTableModel.getRowCount()/(double)RPP);
            refreshTable();
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

    void updateStats(){totalLbl.setText(String.valueOf(sharedTableModel.getRowCount()));}

    private JPanel buildRightPanel() {
        JPanel r=new JPanel(); r.setLayout(new BoxLayout(r,BoxLayout.Y_AXIS));
        r.setOpaque(false); r.setPreferredSize(new Dimension(260,0));
        JPanel alerts=rndPanel(); alerts.setLayout(new BoxLayout(alerts,BoxLayout.Y_AXIS));
        alerts.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        alerts.setMaximumSize(new Dimension(260,250));
        JPanel ah=new JPanel(new BorderLayout()); ah.setOpaque(false);
        ah.setMaximumSize(new Dimension(260,24)); ah.setAlignmentX(0f);
        JLabel at=new JLabel(" Stock Alerts"); at.setFont(FB16); at.setForeground(DARK);
        at.setIcon(sectionIcon(1)); at.setIconTextGap(6);
        JLabel va=new JLabel("View All"); va.setFont(F12); va.setForeground(PINK);
        va.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ah.add(at,BorderLayout.WEST); ah.add(va,BorderLayout.EAST);
        alerts.add(ah); alerts.add(Box.createVerticalStrut(12));
        alerts.add(alertItem("Dolo 650",28));alerts.add(Box.createVerticalStrut(8));
        alerts.add(alertItem("Amoxicillin 500mg",15));alerts.add(Box.createVerticalStrut(8));
        alerts.add(alertItem("Cetrizine 10mg",2));
        r.add(alerts); r.add(Box.createVerticalStrut(14));
        JPanel qa=rndPanel(); qa.setLayout(new BoxLayout(qa,BoxLayout.Y_AXIS));
        qa.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        qa.setMaximumSize(new Dimension(260,250));
        JLabel qt=new JLabel(" Quick Actions"); qt.setFont(FB16); qt.setForeground(DARK); qt.setAlignmentX(0f);
        qt.setIcon(sectionIcon(2)); qt.setIconTextGap(6);
        qa.add(qt); qa.add(Box.createVerticalStrut(12));
        JPanel grid=new JPanel(new GridLayout(2,2,8,8)); grid.setOpaque(false);
        grid.setAlignmentX(0f); grid.setMaximumSize(new Dimension(240,160));
        grid.add(actionBtn(0,"Add Medicine")); grid.add(actionBtn(1,"New Bill"));
        grid.add(actionBtn(2,"Purchase")); grid.add(actionBtn(3,"View Reports"));
        qa.add(grid); r.add(qa); r.add(Box.createVerticalGlue());
        return r;
    }

    private JPanel alertItem(String name,int qty) {
        JPanel i=new JPanel(new BorderLayout(8,0)); i.setOpaque(false);
        i.setMaximumSize(new Dimension(240,48)); i.setAlignmentX(0f);
        i.setBorder(BorderFactory.createEmptyBorder(6,0,6,0));
        // Pill capsule icon drawn with Graphics2D
        JLabel ic=new JLabel(new Icon(){
            public int getIconWidth(){return 40;} public int getIconHeight(){return 20;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFC,0xE4,0xEC));
                g2.fillRoundRect(x,y,40,20,20,20);
                g2.setColor(PINK); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x,y,39,19,20,20);
                g2.drawLine(x+20,y,x+20,y+19);
                g2.dispose();
            }
        });
        JPanel tx=new JPanel(); tx.setLayout(new BoxLayout(tx,BoxLayout.Y_AXIS)); tx.setOpaque(false);
        JLabel n=new JLabel(name); n.setFont(FB13); n.setForeground(DARK);
        JLabel s=new JLabel("Only "+qty+" left in stock"); s.setFont(F12); s.setForeground(GRAY);
        tx.add(n); tx.add(s);
        i.add(ic,BorderLayout.WEST); i.add(tx,BorderLayout.CENTER);
        return i;
    }

    JPanel createMedicinesPage() {
        currentPage=1; refreshTable();
        JPanel p=new JPanel(new BorderLayout(0,10)); p.setOpaque(false);
        JPanel hdr=new JPanel(new BorderLayout()); hdr.setOpaque(false);
        JLabel t=new JLabel(" All Medicines"); t.setFont(FB16); t.setForeground(DARK);
        t.setIcon(sectionIcon(0)); t.setIconTextGap(6);
        JButton ab=pinkBtn("+ Add Medicine"); ab.addActionListener(e->showAddDialog());
        hdr.add(t,BorderLayout.WEST); hdr.add(ab,BorderLayout.EAST);
        p.add(hdr,BorderLayout.NORTH);
        medicinesTable = new JTable(sharedTableModel);
        styleTable(medicinesTable);
        medicinesTable.setRowSorter(null);
        JScrollPane sp=new JScrollPane(medicinesTable); sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE);
        applyTransparentScrollBar(sp);
        p.add(sp,BorderLayout.CENTER);
        return p;
    }

    private void applyTransparentScrollBar(JScrollPane sp) {
        JScrollBar bar = sp.getVerticalScrollBar();
        bar.setOpaque(false);
        bar.setUnitIncrement(12);
        bar.setUI(new BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                thumbColor = new Color(0xC9,0x63,0x7A,90);
                trackColor = new Color(0,0,0,0);
            }
            protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
            protected JButton createIncreaseButton(int orientation) { return zeroButton(); }
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if(!c.isEnabled() || r.width <= 0 || r.height <= 0) return;
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(r.x+3,r.y+3,r.width-6,r.height-6,8,8);
                g2.dispose();
            }
        });
    }

    private JButton zeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0,0));
        b.setMinimumSize(new Dimension(0,0));
        b.setMaximumSize(new Dimension(0,0));
        return b;
    }

    JPanel createStockAlertsPage() {
        JPanel p=new JPanel(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setOpaque(false);
        JLabel t=new JLabel(" Stock Alerts \u2014 Low & Critical Items"); t.setFont(FB16);
        t.setIcon(sectionIcon(1)); t.setIconTextGap(6);
        t.setForeground(DARK); t.setAlignmentX(0f);
        p.add(t); p.add(Box.createVerticalStrut(16));
        for(int i=0;i<sharedTableModel.getRowCount();i++){
            String st=String.valueOf(sharedTableModel.getValueAt(i, 6));
            if(!"In Stock".equals(st)){
                String nm=(String)sharedTableModel.getValueAt(i, 1);
                int qty=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(i, 3)));
                JPanel card=rndPanel(); card.setLayout(new BorderLayout(12,0));
                card.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE,60)); card.setAlignmentX(0f);
                JLabel ic=new JLabel(sectionIcon(0));
                JPanel info=new JPanel(); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS)); info.setOpaque(false);
                JLabel nl=new JLabel(nm+" \u2014 "+st); nl.setFont(FB13);
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

    static Icon statIcon(int type,Color color){
        return new Icon(){public int getIconWidth(){return 28;}public int getIconHeight(){return 28;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.setColor(color);g2.translate(x,y);
                if(type==0){
                    Polygon top=new Polygon(new int[]{14,4,14,24},new int[]{3,8,13,8},4);
                    Polygon left=new Polygon(new int[]{4,14,14,4},new int[]{8,13,25,20},4);
                    Polygon right=new Polygon(new int[]{14,24,24,14},new int[]{13,8,20,25},4);
                    g2.draw(top);g2.draw(left);g2.draw(right);
                    g2.drawLine(14,13,14,25);g2.drawLine(8,10,18,5);
                }else if(type==1){
                    g2.drawRoundRect(8,3,12,22,3,3);
                    g2.drawLine(7,3,21,3);g2.drawLine(7,25,21,25);
                    g2.drawLine(9,8,19,8);g2.drawLine(9,20,19,20);
                    g2.drawLine(10,8,18,20);g2.drawLine(18,8,10,20);
                }else if(type==2){
                    g2.drawLine(14,4,25,24);g2.drawLine(14,4,3,24);g2.drawLine(3,24,25,24);
                    g2.drawLine(14,10,14,17);g2.fillOval(13,20,3,3);
                }else{
                    g2.setFont(new Font("SansSerif",Font.BOLD,26));
                    g2.drawString("\u20B9",7,24);
                }
                g2.dispose();}};
    }

    // Section title icons (16x16)
    static Icon sectionIcon(int type){
        return new Icon(){public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PINK);g2.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g2.translate(x,y);
                if(type==0){
                    g2.rotate(-Math.PI/4,8,8);
                    g2.drawRoundRect(2,5,12,6,6,6);g2.drawLine(8,5,8,11);
                }
                else if(type==1){
                    g2.drawArc(4,2,8,8,0,180);g2.drawLine(4,6,4,11);g2.drawLine(12,6,12,11);
                    g2.drawLine(2,11,14,11);g2.fillOval(7,13,2,2);
                }
                else{
                    g2.drawLine(8,1,5,7);g2.drawLine(5,7,9,7);g2.drawLine(9,7,6,15);g2.drawLine(6,15,12,6);
                }
                g2.dispose();}};
    }
    // Action column icons
    static JLabel iconLabel(int type){
        JLabel l=new JLabel(new Icon(){public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g2.translate(x,y);
                if(type==0){
                    g2.setColor(PINK);g2.drawLine(11,2,14,5);g2.drawLine(14,5,6,13);
                    g2.drawLine(6,13,2,14);g2.drawLine(2,14,3,10);g2.drawLine(3,10,11,2);
                }
                else{
                    g2.setColor(new Color(0xE5,0x73,0x73));g2.drawLine(3,4,13,4);g2.drawRoundRect(5,4,6,10,2,2);
                    g2.drawLine(6,7,6,12);g2.drawLine(8,7,8,12);g2.drawLine(10,7,10,12);g2.drawLine(6,2,10,2);g2.drawLine(7,2,7,4);g2.drawLine(9,2,9,4);
                }
                g2.dispose();}});
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return l;
    }
    // Page button helper
    static JButton pageBtn(String text,boolean active){
        JButton b=new JButton(text){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                g2.setColor(PINK);g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose();super.paintComponent(g);}};
        b.setFont(F12);b.setForeground(PINK);b.setOpaque(false);b.setContentAreaFilled(false);
        b.setBorderPainted(false);b.setFocusPainted(false);
        b.setMargin(new Insets(0,0,0,0));
        b.setPreferredSize(new Dimension(32,32));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;
    }
    // Graphics2D quick action icons (32x32, centered)
    static Icon qaIcon(int type){
        return new Icon(){
            public int getIconWidth(){return 32;} public int getIconHeight(){return 32;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PINK); g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); g2.translate(x,y);
                switch(type){
                    case 0: g2.drawRoundRect(9,10,14,14,3,3);g2.drawLine(16,6,16,24);g2.drawLine(9,16,23,16);g2.drawArc(12,5,8,8,0,180);break;
                    case 1: g2.drawRoundRect(7,4,18,22,3,3);g2.drawLine(11,10,21,10);g2.drawLine(11,15,21,15);g2.drawLine(11,20,18,20);g2.fillRect(23,8,3,14);break;
                    case 2: g2.drawPolyline(new int[]{4,8,10,25,23},new int[]{5,5,22,22,10},5);g2.drawLine(11,10,24,10);g2.fillOval(10,25,3,3);g2.fillOval(22,25,3,3);break;
                    case 3: g2.fillRoundRect(5,20,5,9,2,2);g2.fillRoundRect(14,13,5,16,2,2);g2.fillRoundRect(23,6,5,23,2,2);break;
                }
                g2.dispose();
            }
        };
    }

    private JButton actionBtn(int iconType,String label) {
        JButton b=new JButton(label){
            boolean hov; {
                addMouseListener(new MouseAdapter(){
                    public void mouseEntered(MouseEvent e){hov=true;repaint();}
                    public void mouseExited(MouseEvent e){hov=false;repaint();}
                });
            }
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,20));
                g2.fill(new RoundRectangle2D.Float(1,2,getWidth()-2,getHeight()-2,12,12));
                g2.setColor(hov?new Color(0xFC,0xE4,0xEC):WHITE);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight()-1,12,12));
                g2.setColor(BRD);g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-2,12,12));
                g2.dispose();super.paintComponent(g);
            }
        };
        b.setIcon(qaIcon(iconType));
        b.setIconTextGap(8);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setFont(new Font("Segoe UI",Font.PLAIN,11));b.setForeground(PINK);
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
                g2.setColor(new Color(0,0,0,15));
                g2.fill(new RoundRectangle2D.Float(1,3,getWidth()-2,getHeight()-3,16,16));
                g2.setColor(WHITE);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight()-2,16,16));
                g2.setColor(BRD);g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-3,16,16));
                g2.dispose();
            }
        };
    }

    void refreshPagination(){
        pagPanel.removeAll();
        int totalRows = sharedTableModel.getRowCount();
        int totalPages = (int)Math.ceil(totalRows / (double)RPP);
        if(totalPages < 1) totalPages = 1;
        if(currentPage > totalPages) currentPage = totalPages;

        JPanel pages = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); pages.setOpaque(false);
        JButton left = pageBtn("\u2190", false); left.setEnabled(currentPage > 1);
        left.addActionListener(e -> { currentPage--; refreshTable(); });
        pages.add(left);

        for (int i = 1; i <= totalPages; i++) {
            int pg = i;
            boolean active = (pg == currentPage);
            JButton btn = new JButton(String.valueOf(pg)){
                protected void paintComponent(Graphics g){
                    Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    if(active){g2.setColor(PINK);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));}
                    else{g2.setColor(WHITE);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                        g2.setColor(PINK);g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,8,8));}
                    g2.dispose();super.paintComponent(g);}};
            btn.setFont(F12); btn.setForeground(active ? WHITE : PINK);
            btn.setOpaque(false); btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
            btn.setMargin(new Insets(0,0,0,0));
            btn.setPreferredSize(new Dimension(32, 32)); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> { currentPage = pg; refreshTable(); });
            pages.add(btn);
        }

        JButton right = pageBtn("\u2192", false); right.setEnabled(currentPage < totalPages);
        right.addActionListener(e -> { currentPage++; refreshTable(); });
        pages.add(right);

        pagPanel.add(pages, BorderLayout.EAST);
        if(showingLabel != null) pagPanel.add(showingLabel, BorderLayout.WEST);
        pagPanel.revalidate(); pagPanel.repaint();
    }

    void showEditDialog(int dataIdx){
        JDialog dlg=new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"Edit Medicine",true);
        dlg.setSize(500,400);dlg.setLocationRelativeTo(this);dlg.setResizable(false);
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(24,28,24,28));p.setBackground(WHITE);
        JLabel tl=new JLabel("Edit Medicine");tl.setFont(new Font("Segoe UI",Font.BOLD,20));tl.setForeground(DARK);tl.setAlignmentX(0f);
        p.add(tl);p.add(Box.createVerticalStrut(16));
        JTextField nf=addField(p,"Medicine Name");nf.setText((String)sharedTableModel.getValueAt(dataIdx,1));
        JComboBox<String> cf=new JComboBox<>(new String[]{"Tablet","Capsule","Syrup","Injection"});
        cf.setFont(F13);cf.setSelectedItem(sharedTableModel.getValueAt(dataIdx,2));cf.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        JPanel cp=new JPanel(new BorderLayout());cp.setOpaque(false);cp.setMaximumSize(new Dimension(Integer.MAX_VALUE,60));cp.setAlignmentX(0f);
        JLabel cl=new JLabel("Category");cl.setFont(FB13);cl.setForeground(DARK);
        cp.add(cl,BorderLayout.NORTH);cp.add(cf,BorderLayout.CENTER);p.add(cp);p.add(Box.createVerticalStrut(8));
        JTextField sf=addField(p,"Stock");sf.setText(String.valueOf(sharedTableModel.getValueAt(dataIdx,3)));
        JTextField pf=addField(p,"Price");pf.setText(String.valueOf(sharedTableModel.getValueAt(dataIdx,4)));
        JTextField ef=addField(p,"Expiry Date");ef.setText(String.valueOf(sharedTableModel.getValueAt(dataIdx,5)));
        p.add(Box.createVerticalStrut(12));
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));btns.setOpaque(false);btns.setAlignmentX(0f);btns.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        JButton cancel=new JButton("Cancel");cancel.setFont(FB13);cancel.setForeground(GRAY);cancel.setPreferredSize(new Dimension(100,36));
        cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));cancel.addActionListener(e->dlg.dispose());
        JButton save=pinkBtn("Save");save.setPreferredSize(new Dimension(120,36));
        save.addActionListener(e->{
            String nm=nf.getText().trim(),stk=sf.getText().trim(),pr=pf.getText().trim(),exp=ef.getText().trim();
            if(nm.isEmpty()||stk.isEmpty()||pr.isEmpty()||exp.isEmpty()){JOptionPane.showMessageDialog(dlg,"Fill all fields!");return;}
            int sv;try{sv=Integer.parseInt(stk);}catch(Exception ex){JOptionPane.showMessageDialog(dlg,"Stock must be number!");return;}
            sharedTableModel.setValueAt(nm, dataIdx, 1);
            sharedTableModel.setValueAt(cf.getSelectedItem(), dataIdx, 2);
            sharedTableModel.setValueAt(stk, dataIdx, 3);
            sharedTableModel.setValueAt(pr, dataIdx, 4);
            sharedTableModel.setValueAt(exp, dataIdx, 5);
            sharedTableModel.setValueAt(status(sv), dataIdx, 6);
            refreshTable();dlg.dispose();JOptionPane.showMessageDialog(this,"Medicine updated!");
        });
        btns.add(cancel);btns.add(save);p.add(btns);
        dlg.setContentPane(p);dlg.setVisible(true);
    }

    // Action column cell editor
    static class ActionEditor extends AbstractCellEditor implements TableCellEditor{
        JPanel panel;DashboardPanel dp;JTable table;int row;
        ActionEditor(DashboardPanel dp,JTable table){this.dp=dp;this.table=table;
            panel=new JPanel(new FlowLayout(FlowLayout.CENTER,4,8));panel.setOpaque(true);
            JButton edit=new JButton(iconLabel(0).getIcon());
            edit.setBorder(null);edit.setContentAreaFilled(false);edit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            edit.setPreferredSize(new Dimension(16,16));
            edit.addActionListener(e->{fireEditingStopped();int di=table.convertRowIndexToModel(row);if(di<sharedTableModel.getRowCount())dp.showEditDialog(di);});
            JButton del=new JButton(iconLabel(1).getIcon());
            del.setBorder(null);del.setContentAreaFilled(false);del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            del.setPreferredSize(new Dimension(16,16));
            del.addActionListener(e->{fireEditingStopped();int di=table.convertRowIndexToModel(row);if(di<sharedTableModel.getRowCount()){
                String nm=(String)sharedTableModel.getValueAt(di,1);
                if(JOptionPane.showConfirmDialog(dp,"Delete "+nm+"?","Delete",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                    sharedTableModel.removeRow(di);dp.refreshTable();dp.updateStats();}}});
            panel.add(edit);panel.add(del);}
        public Component getTableCellEditorComponent(JTable t,Object v,boolean sel,int r,int c){
            row=r;panel.setBackground(sel?new Color(252,228,236):WHITE);return panel;}
        public Object getCellEditorValue(){return "";}
    }
}
