import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

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
    private StockOverviewPanel stockChartPanel;
    private java.util.function.IntConsumer navigator;
    private Timer expirySweepTimer;
    static final int RPP=5;
    private String[] cols={"ID","Medicine Name","Category","Stock","Price (\u20b9)","Expiry Date","Status","Action"};

    private static void initializeSharedTable() {
        if(sharedTableModel != null && sharedTable != null) return;
        String[] columns = {"ID","Medicine Name","Category","Stock","Price (\u20b9)","Expiry Date","Status","Action"};
        sharedTableModel = new DefaultTableModel(columns, 0){
            public boolean isCellEditable(int r, int c){ return c == 7; }
        };
        loadMedicinesFromDatabase();
        sharedTable = new JTable(sharedTableModel);
    }

    private static void loadMedicinesFromDatabase() {
        archiveExpiredMedicines();
        Vector<Vector> data = sharedTableModel.getDataVector();
        data.clear();
        for(Object[] row:DatabaseManager.getAllMedicines()){
            Vector<Object> values=new Vector<>();
            for(Object value:row) values.add(value);
            data.add(values);
        }
        sharedTableModel.fireTableDataChanged();
    }

    private static void archiveExpiredMedicines() {
        for(Object[] row:DatabaseManager.getAllMedicines()){
            LocalDate expiry=parseExpiryDate(String.valueOf(row[5]));
            if(expiry!=null&&expiry.isBefore(LocalDate.now())){
                DatabaseManager.archiveExpiredMedicine(Integer.parseInt(String.valueOf(row[0])));
            }
        }
    }

    public DashboardPanel() {
        setLayout(new BorderLayout()); setOpaque(false);
        initializeSharedTable();
        initModel();
        startExpirySweepTimer();
        JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(buildDashboardSummary(),BorderLayout.NORTH);
        JPanel mid=new JPanel(new BorderLayout(16,0)); mid.setOpaque(false);
        mid.setBorder(BorderFactory.createEmptyBorder(20,0,0,0));
        mid.add(buildTableSection(),BorderLayout.CENTER);
        mid.add(buildRightPanel(),BorderLayout.EAST);
        top.add(mid,BorderLayout.CENTER);
        add(top,BorderLayout.CENTER);
    }

    private void startExpirySweepTimer() {
        expirySweepTimer=new Timer(60*60*1000,e->{
            loadMedicinesFromDatabase();
            refreshTable();
            updateStats();
        });
        expirySweepTimer.start();
    }

    void setNavigator(java.util.function.IntConsumer navigator) {
        this.navigator = navigator;
    }

    private void navigateTo(int index) {
        if(navigator != null) navigator.accept(index);
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
        expLbl=new JLabel(String.valueOf(getExpiringSoonRows().size()));
        lowLbl=new JLabel(String.valueOf(getLowStockRows().size()));
        revLbl=new JLabel("\u20b9"+String.format("%.2f",DatabaseManager.getTotalRevenue()));
        JPanel totalCard=statCard(statIcon(0,new Color(0xFF,0x98,0x00)),"Total Medicines",totalLbl,new Color(0xFF,0x98,0x00),"All medicines in stock",new Color(0x4C,0xAF,0x50),CARD1_BG);
        makeClickable(totalCard,()->navigateTo(1));
        row.add(totalCard);
        JPanel expCard=statCard(statIcon(1,new Color(0xFF,0xB3,0x00)),"Expiring Soon",expLbl,new Color(0xFF,0xB3,0x00),"Red: 1 month, Yellow: 2 months",new Color(0xFF,0x98,0x00),CARD2_BG);
        makeClickable(expCard,this::showExpiryTabs);
        row.add(expCard);
        JPanel lowCard=statCard(statIcon(2,new Color(0x7B,0x1F,0xA2)),"Low Stock",lowLbl,new Color(0x7B,0x1F,0xA2),"Stock below minimum",new Color(0x9E,0x9E,0x9E),CARD3_BG);
        makeClickable(lowCard,()->showMedicineList("Low Stock Medicines",getLowStockRows()));
        row.add(lowCard);
        JPanel revenueCard=statCard(statIcon(3,GREEN),"Total Revenue",revLbl,GREEN,"This Month",new Color(0x4C,0xAF,0x50),CARD4_BG);
        makeClickable(revenueCard,this::showRevenueHistory);
        row.add(revenueCard);
        return row;
    }

    private JPanel buildDashboardSummary() {
        JPanel wrap=new JPanel();
        wrap.setLayout(new BoxLayout(wrap,BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(12,0,0,0));
        JButton reportStatus=pinkBtn("Report Status");
        reportStatus.addActionListener(e->{
            stockChartPanel.setVisible(true);
            stockChartPanel.restartAnimation();
            wrap.revalidate();
            wrap.repaint();
        });
        actions.add(reportStatus);
        stockChartPanel=new StockOverviewPanel();
        stockChartPanel.setVisible(false);
        stockChartPanel.setAlignmentX(0f);
        stockChartPanel.setCloseAction(()->{
            stockChartPanel.setVisible(false);
            wrap.revalidate();
            wrap.repaint();
        });
        sharedTableModel.addTableModelListener(e->stockChartPanel.refreshData());
        wrap.add(buildStatCards());
        wrap.add(actions);
        wrap.add(Box.createVerticalStrut(12));
        wrap.add(stockChartPanel);
        return wrap;
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
        hdr.add(title,BorderLayout.WEST); hdr.add(exportButtons(addBtn),BorderLayout.EAST);
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
        JTextField priceF=addField(p,"Price â‚¹");
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
            double priceVal;
            try{stkVal=validStock(stk);}catch(Exception ex){
                JOptionPane.showMessageDialog(dlg,"Stock must be zero or more.","Error",JOptionPane.ERROR_MESSAGE);return;}
            try{priceVal=validPrice(pr);}catch(Exception ex){
                JOptionPane.showMessageDialog(dlg,"Price must be greater than zero.","Error",JOptionPane.ERROR_MESSAGE);return;}
            LocalDate expiryDate=parseExpiryDate(exp);
            if(expiryDate==null){
                JOptionPane.showMessageDialog(dlg,"Expiry date must be like MM/YYYY, Jan 2027, or YYYY-MM-DD.","Error",JOptionPane.ERROR_MESSAGE);return;
            }
            if(expiryDate.isBefore(LocalDate.now())){
                JOptionPane.showMessageDialog(dlg,"Expiry date has passed. Cannot add this medicine.","Error",JOptionPane.ERROR_MESSAGE);return;
            }
            int nextId=1006;
            for(int i=0;i<sharedTableModel.getRowCount();i++){
                int id=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(i,0)));
                if(id>=nextId)nextId=id+1;
            }
            Object[] newRow=new Object[]{String.valueOf(nextId),nm,(String)catF.getSelectedItem(),stkVal,String.format("%.2f",priceVal),exp,status(stkVal),co};
            DatabaseManager.addMedicine(newRow);
            loadMedicinesFromDatabase();
            currentPage=(int)Math.ceil(sharedTableModel.getRowCount()/(double)RPP);
            refreshTable();
            updateStats(); dlg.dispose();
            JOptionPane.showMessageDialog(this,"Medicine added successfully!","Success",JOptionPane.INFORMATION_MESSAGE);
        });
        btns.add(cancel); btns.add(addB);
        p.add(btns);
        JScrollPane scroll=new JScrollPane(p);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        applyTransparentScrollBar(scroll);
        dlg.setContentPane(scroll); dlg.setVisible(true);
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

    void updateStats(){
        totalLbl.setText(String.valueOf(sharedTableModel.getRowCount()));
        if(expLbl!=null)expLbl.setText(String.valueOf(getExpiringSoonRows().size()));
        if(lowLbl!=null)lowLbl.setText(String.valueOf(getLowStockRows().size()));
        if(revLbl!=null)revLbl.setText("\u20b9"+String.format("%.2f",DatabaseManager.getTotalRevenue()));
        if(stockChartPanel!=null)stockChartPanel.refreshData();
    }

    private void makeClickable(JComponent component,Runnable action) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){action.run();}
        });
    }

    private List<Object[]> getLowStockRows() {
        List<Object[]> rows=new ArrayList<>();
        for(int i=0;i<sharedTableModel.getRowCount();i++){
            String st=String.valueOf(sharedTableModel.getValueAt(i,6));
            if("Low Stock".equals(st)||"Critical".equals(st)) rows.add(medicineDialogRow(i));
        }
        return rows;
    }

    private List<Object[]> getExpiringSoonRows() {
        List<Object[]> rows=new ArrayList<>();
        LocalDate today=LocalDate.now();
        LocalDate limit=today.plusMonths(2);
        for(int i=0;i<sharedTableModel.getRowCount();i++){
            LocalDate expiry=parseExpiryDate(String.valueOf(sharedTableModel.getValueAt(i,5)));
            if(expiry!=null&&!expiry.isBefore(today)&&!expiry.isAfter(limit)) rows.add(medicineDialogRow(i));
        }
        return rows;
    }

    private Object[] medicineDialogRow(int row) {
        return new Object[]{
            sharedTableModel.getValueAt(row,0),
            sharedTableModel.getValueAt(row,1),
            sharedTableModel.getValueAt(row,2),
            sharedTableModel.getValueAt(row,3),
            sharedTableModel.getValueAt(row,4),
            sharedTableModel.getValueAt(row,5),
            sharedTableModel.getValueAt(row,6)
        };
    }

    private static LocalDate parseExpiryDate(String text) {
        String value=text.trim();
        DateTimeFormatter monthName=DateTimeFormatter.ofPattern("MMM yyyy",Locale.ENGLISH);
        DateTimeFormatter monthNumber=DateTimeFormatter.ofPattern("MM/yyyy",Locale.ENGLISH);
        try{return YearMonth.parse(value,monthName).atEndOfMonth();}catch(DateTimeParseException ignored){}
        try{return YearMonth.parse(value,monthNumber).atEndOfMonth();}catch(DateTimeParseException ignored){}
        try{return LocalDate.parse(value);}catch(DateTimeParseException ignored){}
        return null;
    }

    private int validStock(String text) {
        int stock=Integer.parseInt(text.trim());
        if(stock<0)throw new NumberFormatException();
        return stock;
    }

    private double validPrice(String text) {
        double price=Double.parseDouble(text.trim());
        if(price<=0)throw new NumberFormatException();
        return price;
    }

    private void showMedicineList(String title,List<Object[]> rows) {
        String[] columns={"ID","Medicine","Category","Stock","Price","Expiry","Status"};
        Object[][] data=rows.toArray(new Object[0][]);
        JTable table=simpleTable(columns,data);
        if("Expiring Soon".equals(title)) applyExpiryUrgencyRenderer(table);
        JScrollPane sp=new JScrollPane(table);
        sp.setPreferredSize(new Dimension(760,320));
        sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE);
        applyTransparentScrollBar(sp);
        if(rows.isEmpty()){
            JOptionPane.showMessageDialog(this,"No medicines found for this list.",title,JOptionPane.INFORMATION_MESSAGE);
        }else{
            JOptionPane.showMessageDialog(this,sp,title,JOptionPane.PLAIN_MESSAGE);
        }
    }

    private void showRevenueHistory() {
        List<DatabaseManager.BillSummary> bills=DatabaseManager.getBillHistory();
        String[] columns={"Bill No","Customer","Payment","Subtotal","Tax","Total","Date & Time"};
        Object[][] data=new Object[bills.size()][columns.length];
        for(int i=0;i<bills.size();i++){
            DatabaseManager.BillSummary bill=bills.get(i);
            data[i]=new Object[]{
                "#"+bill.id,
                bill.customer,
                bill.payment,
                "\u20b9"+String.format("%.2f",bill.subtotal),
                "\u20b9"+String.format("%.2f",bill.tax),
                "\u20b9"+String.format("%.2f",bill.total),
                bill.billDate
            };
        }
        JTable table=simpleTable(columns,data);
        JScrollPane sp=new JScrollPane(table);
        sp.setPreferredSize(new Dimension(820,340));
        sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE);
        applyTransparentScrollBar(sp);
        if(bills.isEmpty()){
            JOptionPane.showMessageDialog(this,"No revenue history yet. Generate a bill to create the first entry.","Revenue History",JOptionPane.INFORMATION_MESSAGE);
        }else{
            JOptionPane.showMessageDialog(this,sp,"Revenue History",JOptionPane.PLAIN_MESSAGE);
        }
    }

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
        va.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){navigateTo(4);}});
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
        JButton addMedicine=actionBtn(0,"Add Medicine");
        addMedicine.addActionListener(e->showAddDialog());
        JButton newBill=actionBtn(1,"New Bill");
        newBill.addActionListener(e->navigateTo(2));
        JButton purchase=actionBtn(2,"Purchase");
        purchase.addActionListener(e->navigateTo(3));
        JButton reports=actionBtn(3,"View Reports");
        reports.addActionListener(e->navigateTo(5));
        grid.add(addMedicine); grid.add(newBill);
        grid.add(purchase); grid.add(reports);
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
        hdr.add(t,BorderLayout.WEST); hdr.add(exportButtons(ab),BorderLayout.EAST);
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

    JPanel createBillingPage() {
        JPanel p=new JPanel(new BorderLayout(14,14)); p.setOpaque(false);
        JPanel main=new JPanel(new BorderLayout(0,12)); main.setOpaque(false);
        JLabel t=new JLabel(" Billing"); t.setFont(FB16); t.setForeground(DARK);
        t.setIcon(qaIcon(1)); t.setIconTextGap(8);
        main.add(t,BorderLayout.NORTH);

        JPanel form=rndPanel(); form.setLayout(new GridLayout(2,5,10,10));
        form.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JComboBox<String> medBox=new JComboBox<>(medicineNames()); medBox.setFont(F13);
        JComboBox<String> paymentField=new JComboBox<>(new String[]{"Cash","UPI","Card"}); paymentField.setFont(F13);
        JTextField qtyField=input("1"), discountField=input("0");
        JTextField customerField=input("Walk-in"), mobileField=input("");
        form.add(labeled("Medicine",medBox));
        form.add(labeled("Qty",qtyField));
        form.add(labeled("Discount %",discountField));
        JButton add=pinkBtn("+ Add Item"); form.add(add);
        JButton remove=pinkBtn("Remove Item"); form.add(remove);
        form.add(labeled("Customer",customerField));
        form.add(labeled("Mobile",mobileField));
        form.add(labeled("Payment",paymentField));
        JButton print=pinkBtn("Generate Bill"); form.add(print);
        JButton history=pinkBtn("Bill History"); history.addActionListener(e->showRevenueHistory()); form.add(history);
        main.add(form,BorderLayout.CENTER);

        String[] cols={"Item","Qty","Price","Amount"};
        JTable billTable=simpleTable(cols,new Object[][]{});
        DefaultTableModel billModel=(DefaultTableModel)billTable.getModel();
        JScrollPane sp=new JScrollPane(billTable); sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE); applyTransparentScrollBar(sp);
        main.add(sp,BorderLayout.SOUTH);

        JPanel side=new JPanel(); side.setOpaque(false); side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(260,0));
        JLabel subtotalValue=new JLabel("\u20b90.00"), taxValue=new JLabel("\u20b90.00"), totalValue=new JLabel("\u20b90.00");
        side.add(totalCard("Subtotal",subtotalValue,sectionIcon(0),new Color(0xFF,0xF7,0xE8),new Color(0xE6,0x51,0x00)));
        side.add(Box.createVerticalStrut(12));
        side.add(totalCard("Tax",taxValue,sectionIcon(2),new Color(0xF5,0xF0,0xFF),new Color(0x7B,0x1F,0xA2)));
        side.add(Box.createVerticalStrut(12));
        side.add(totalCard("Total",totalValue,statIcon(3,GREEN),new Color(0xE8,0xF5,0xE9),GREEN));
        p.add(main,BorderLayout.CENTER); p.add(side,BorderLayout.EAST);

        Runnable updateTotals=()->{
            double subtotal=0;
            for(int i=0;i<billModel.getRowCount();i++) subtotal+=Double.parseDouble(String.valueOf(billModel.getValueAt(i,3)));
            double discount;
            try{discount=validDiscount(discountField.getText().trim());}catch(IllegalArgumentException ex){discount=0;}
            subtotal=subtotal-(subtotal*discount/100.0);
            double tax=subtotal*0.05,total=subtotal+tax;
            subtotalValue.setText("\u20b9"+String.format("%.2f",subtotal));
            taxValue.setText("\u20b9"+String.format("%.2f",tax));
            totalValue.setText("\u20b9"+String.format("%.2f",total));
        };
        add.addActionListener(e->{
            if(medBox.getItemCount()==0){JOptionPane.showMessageDialog(this,"No medicines available for billing.","Billing",JOptionPane.WARNING_MESSAGE);return;}
            int qty;
            try{qty=Integer.parseInt(qtyField.getText().trim()); if(qty<=0)throw new NumberFormatException();}catch(Exception ex){
                JOptionPane.showMessageDialog(this,"Enter a valid quantity.","Billing",JOptionPane.ERROR_MESSAGE);return;}
            int idx=medBox.getSelectedIndex();
            String name=String.valueOf(sharedTableModel.getValueAt(idx,1));
            int available=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(idx,3)));
            int alreadyInBill=billedQuantity(billModel,name);
            if(qty+alreadyInBill>available){
                JOptionPane.showMessageDialog(this,"Only "+available+" units available for "+name+".","Billing",JOptionPane.WARNING_MESSAGE);return;
            }
            double price=Double.parseDouble(String.valueOf(sharedTableModel.getValueAt(idx,4)));
            billModel.addRow(new Object[]{name,qty,String.format("%.2f",price),String.format("%.2f",qty*price)});
            updateTotals.run();
        });
        remove.addActionListener(e->{
            int row=billTable.getSelectedRow();
            if(row<0){JOptionPane.showMessageDialog(this,"Select an item to remove.","Billing",JOptionPane.INFORMATION_MESSAGE);return;}
            billModel.removeRow(billTable.convertRowIndexToModel(row));
            updateTotals.run();
        });
        print.addActionListener(e->{
            if(billModel.getRowCount()==0){JOptionPane.showMessageDialog(this,"Add at least one item before generating a bill.","Billing",JOptionPane.WARNING_MESSAGE);return;}
            if(!isValidMobile(mobileField.getText().trim())){JOptionPane.showMessageDialog(this,"Enter a valid 10 digit mobile number or leave it blank.","Billing",JOptionPane.ERROR_MESSAGE);return;}
            try{validDiscount(discountField.getText().trim());}catch(IllegalArgumentException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Billing",JOptionPane.ERROR_MESSAGE);return;}
            if(!hasAvailableStockForBill(billModel))return;
            BillTotals totals=calculateBillTotals(billModel,discountField);
            String preview="Customer: "+customerField.getText().trim()+
                "\nMobile: "+mobileField.getText().trim()+
                "\nPayment: "+paymentField.getSelectedItem()+
                "\nItems: "+billModel.getRowCount()+
                "\nTotal: \u20b9"+String.format("%.2f",totals.total)+
                "\n\nGenerate this bill?";
            if(JOptionPane.showConfirmDialog(this,preview,"Confirm Bill",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            int billId=BillingService.saveBillAndUpdateStock(
                customerField.getText().trim(),
                mobileField.getText().trim(),
                String.valueOf(paymentField.getSelectedItem()),
                totals.subtotal,
                totals.tax,
                totals.total,
                buildBillLines(billModel)
            );
            loadMedicinesFromDatabase();
            refreshTable();
            updateStats();
            JOptionPane.showMessageDialog(this,
                "Bill #"+billId+" generated for "+customerField.getText().trim()+"\nMobile: "+mobileField.getText().trim()+
                "\nPayment: "+paymentField.getSelectedItem()+"\nTotal: "+totalValue.getText(),
                "Bill Generated",JOptionPane.INFORMATION_MESSAGE);
            billModel.setRowCount(0);
            updateTotals.run();
        });
        updateTotals.run();
        return p;
    }

    private boolean hasAvailableStockForBill(DefaultTableModel billModel) {
        for(int i=0;i<billModel.getRowCount();i++){
            String name=String.valueOf(billModel.getValueAt(i,0));
            int row=findMedicineRowByName(name);
            if(row<0){
                JOptionPane.showMessageDialog(this,name+" is no longer available in inventory.","Billing",JOptionPane.ERROR_MESSAGE);
                return false;
            }
            int requested=billedQuantity(billModel,name);
            int available=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(row,3)));
            if(requested>available){
                JOptionPane.showMessageDialog(this,"Only "+available+" units available for "+name+".","Billing",JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        return true;
    }

    private List<BillingService.BillLine> buildBillLines(DefaultTableModel billModel) {
        List<BillingService.BillLine> lines=new ArrayList<>();
        for(int i=0;i<billModel.getRowCount();i++){
            String name=String.valueOf(billModel.getValueAt(i,0));
            int row=findMedicineRowByName(name);
            int qty=Integer.parseInt(String.valueOf(billModel.getValueAt(i,1)));
            double price=Double.parseDouble(String.valueOf(billModel.getValueAt(i,2)));
            double amount=Double.parseDouble(String.valueOf(billModel.getValueAt(i,3)));
            int stock=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(row,3)));
            int updatedStock=stock-qty;
            Object[] medicineRow=new Object[]{
                sharedTableModel.getValueAt(row,0),
                sharedTableModel.getValueAt(row,1),
                sharedTableModel.getValueAt(row,2),
                stock,
                sharedTableModel.getValueAt(row,4),
                sharedTableModel.getValueAt(row,5),
                sharedTableModel.getValueAt(row,6)
            };
            lines.add(new BillingService.BillLine(medicineRow,qty,price,amount,updatedStock,status(updatedStock)));
        }
        return lines;
    }

    private BillTotals calculateBillTotals(DefaultTableModel billModel,JTextField discountField) {
        double subtotal=0;
        for(int i=0;i<billModel.getRowCount();i++) subtotal+=Double.parseDouble(String.valueOf(billModel.getValueAt(i,3)));
        double discount=validDiscount(discountField.getText().trim());
        subtotal=subtotal-(subtotal*discount/100.0);
        double tax=subtotal*0.05,total=subtotal+tax;
        return new BillTotals(subtotal,tax,total);
    }

    private double validDiscount(String text) {
        try{
            double discount=Double.parseDouble(text.isEmpty()?"0":text);
            if(discount<0||discount>100)throw new NumberFormatException();
            return discount;
        }catch(Exception ex){
            throw new IllegalArgumentException("Discount must be between 0 and 100.");
        }
    }

    private void showExpiryTabs() {
        loadMedicinesFromDatabase();
        refreshTable();
        updateStats();
        JTabbedPane tabs=new JTabbedPane();
        tabs.setFont(FB13);
        tabs.addTab("Expiring Soon", expiryTablePanel(getExpiringSoonRows(),true));
        tabs.addTab("Expired", expiryTablePanel(DatabaseManager.getExpiredMedicines(),false));
        tabs.setPreferredSize(new Dimension(780,360));
        JOptionPane.showMessageDialog(this,tabs,"Expiry Medicines",JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel expiryTablePanel(List<Object[]> rows,boolean colorByUrgency) {
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        if(rows.isEmpty()){
            JLabel empty=new JLabel(colorByUrgency?"No medicines expiring in the next 2 months.":"No expired medicines found.");
            empty.setFont(F13);
            empty.setForeground(GRAY);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(empty,BorderLayout.CENTER);
            return panel;
        }
        String[] columns={"ID","Medicine","Category","Stock","Price","Expiry","Status"};
        JTable table=simpleTable(columns,rows.toArray(new Object[0][]));
        if(colorByUrgency) applyExpiryUrgencyRenderer(table);
        else applyExpiredRenderer(table);
        JScrollPane sp=new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE);
        applyTransparentScrollBar(sp);
        panel.add(sp,BorderLayout.CENTER);
        return panel;
    }

    private void applyExpiredRenderer(JTable table) {
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int r,int c){
                JLabel l=(JLabel)super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                if(!sel)l.setBackground(new Color(0xFF,0xEB,0xEE));
                l.setForeground(sel?new Color(180,60,90):RED);
                l.setOpaque(true);
                l.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
                return l;
            }
        });
    }

    private boolean isValidMobile(String mobile) {
        return mobile.isEmpty()||mobile.matches("\\d{10}");
    }

    private static class BillTotals {
        final double subtotal,tax,total;
        BillTotals(double subtotal,double tax,double total){
            this.subtotal=subtotal;
            this.tax=tax;
            this.total=total;
        }
    }

    private int billedQuantity(DefaultTableModel billModel,String medicineName) {
        int total=0;
        for(int i=0;i<billModel.getRowCount();i++){
            if(medicineName.equals(String.valueOf(billModel.getValueAt(i,0)))){
                total+=Integer.parseInt(String.valueOf(billModel.getValueAt(i,1)));
            }
        }
        return total;
    }

    private int findMedicineRowByName(String medicineName) {
        for(int i=0;i<sharedTableModel.getRowCount();i++){
            if(medicineName.equals(String.valueOf(sharedTableModel.getValueAt(i,1)))) return i;
        }
        return -1;
    }

    JPanel createPurchasesPage() {
        JPanel p=new JPanel(new BorderLayout(0,12)); p.setOpaque(false);
        JPanel hdr=new JPanel(new BorderLayout()); hdr.setOpaque(false);
        JLabel t=new JLabel(" Purchases"); t.setFont(FB16); t.setForeground(DARK);
        t.setIcon(qaIcon(2)); t.setIconTextGap(8);
        JButton order=pinkBtn("+ New Purchase"); hdr.add(t,BorderLayout.WEST); hdr.add(order,BorderLayout.EAST);
        p.add(hdr,BorderLayout.NORTH);

        String[] cols={"PO No","Supplier","Items","Amount","Status","Date & Time"};
        Object[][] rows=purchaseRows();
        JTable tbl=simpleTable(cols,rows);
        DefaultTableModel purchaseModel=(DefaultTableModel)tbl.getModel();

        JLabel pendingValue=new JLabel();
        JLabel monthValue=new JLabel();
        JLabel supplierValue=new JLabel();
        Runnable updatePurchaseStats=()->{
            pendingValue.setText(String.valueOf(DatabaseManager.getPendingPurchaseCount()));
            monthValue.setText("\u20b9"+String.format("%,.0f",DatabaseManager.getPurchaseTotalThisMonth()));
            supplierValue.setText(String.valueOf(DatabaseManager.getSupplierCount()));
        };

        JPanel cards=new JPanel(new GridLayout(1,3,12,0)); cards.setOpaque(false);
        cards.add(totalCard("Pending Orders",pendingValue,sectionIcon(2),new Color(0xF5,0xF0,0xFF),new Color(0x7B,0x1F,0xA2)));
        cards.add(totalCard("This Month",monthValue,statIcon(3,GREEN),new Color(0xE8,0xF5,0xE9),GREEN));
        cards.add(totalCard("Suppliers",supplierValue,qaIcon(0),new Color(0xFF,0xF0,0xF5),PINK));
        updatePurchaseStats.run();
        p.add(cards,BorderLayout.CENTER);

        order.addActionListener(e->{
            if(!showPurchaseDialog())return;
            reloadPurchaseModel(purchaseModel);
            updatePurchaseStats.run();
        });
        JScrollPane sp=new JScrollPane(tbl); sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE); applyTransparentScrollBar(sp);
        p.add(sp,BorderLayout.SOUTH);
        return p;
    }

    private Object[][] purchaseRows() {
        List<Object[]> purchases=DatabaseManager.getAllPurchases();
        Object[][] rows=new Object[purchases.size()][];
        for(int i=0;i<purchases.size();i++) rows[i]=purchases.get(i);
        return rows;
    }

    private void reloadPurchaseModel(DefaultTableModel model) {
        model.setRowCount(0);
        for(Object[] row:DatabaseManager.getAllPurchases()) model.addRow(row);
    }

    private boolean showPurchaseDialog() {
        JPanel form=new JPanel(new GridLayout(4,1,8,8));
        JTextField supplier=input("");
        JTextField items=input("");
        JTextField amount=input("");
        JComboBox<String> status=new JComboBox<>(new String[]{"Pending","Received"});
        status.setFont(F13);
        form.add(labeled("Supplier",supplier));
        form.add(labeled("Items",items));
        form.add(labeled("Amount",amount));
        form.add(labeled("Status",status));
        int result=JOptionPane.showConfirmDialog(this,form,"New Purchase",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result!=JOptionPane.OK_OPTION)return false;
        String supplierText=supplier.getText().trim();
        String itemsText=items.getText().trim();
        if(supplierText.isEmpty()||itemsText.isEmpty()){
            JOptionPane.showMessageDialog(this,"Supplier and items are required.","Purchase",JOptionPane.ERROR_MESSAGE);
            return false;
        }
        double amountValue;
        try{
            amountValue=Double.parseDouble(amount.getText().replaceAll("[^0-9.]",""));
            if(amountValue<=0)throw new NumberFormatException();
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Enter a valid purchase amount.","Purchase",JOptionPane.ERROR_MESSAGE);
            return false;
        }
        DatabaseManager.addPurchase(supplierText,itemsText,amountValue,String.valueOf(status.getSelectedItem()));
        return true;
    }

    private int countPurchaseRowsByStatus(DefaultTableModel model,String status) {
        int count=0;
        for(int i=0;i<model.getRowCount();i++){
            if(status.equals(String.valueOf(model.getValueAt(i,4)))) count++;
        }
        return count;
    }

    private int countPurchaseSuppliers(DefaultTableModel model) {
        java.util.Set<String> suppliers=new java.util.HashSet<>();
        for(int i=0;i<model.getRowCount();i++){
            String supplier=String.valueOf(model.getValueAt(i,1)).trim();
            if(!supplier.isEmpty()) suppliers.add(supplier.toLowerCase(Locale.ENGLISH));
        }
        return suppliers.size();
    }

    private double sumPurchaseAmounts(DefaultTableModel model) {
        double total=0;
        for(int i=0;i<model.getRowCount();i++) total+=parsePurchaseAmount(model.getValueAt(i,3));
        return total;
    }

    private double parsePurchaseAmount(Object value) {
        String amount=String.valueOf(value).replaceAll("[^0-9.]","");
        if(amount.isEmpty()) return 0;
        try{return Double.parseDouble(amount);}catch(NumberFormatException ex){return 0;}
    }

    JPanel createReportsPage() {
        JPanel p=new JPanel(new BorderLayout(0,12)); p.setOpaque(false);
        JLabel t=new JLabel(" Reports"); t.setFont(FB16); t.setForeground(DARK);
        t.setIcon(qaIcon(3)); t.setIconTextGap(8);
        p.add(t,BorderLayout.NORTH);

        JPanel cards=new JPanel(new GridLayout(1,4,12,0)); cards.setOpaque(false);
        JPanel inventoryCard=coloredSummaryCard("Inventory Value","\u20b9"+String.format("%.2f",DatabaseManager.getInventoryValue()),statIcon(3,GREEN),new Color(0xE8,0xF5,0xE9),GREEN);
        makeClickable(inventoryCard,()->showInventoryValueList());
        cards.add(inventoryCard);
        JPanel lowStockCard=coloredSummaryCard("Low Stock",String.valueOf(DatabaseManager.getLowStockCount()),sectionIcon(1),new Color(0xFF,0xF7,0xE8),new Color(0xE6,0x51,0x00));
        makeClickable(lowStockCard,()->showMedicineList("Low Stock Medicines",getLowStockOnlyRows()));
        cards.add(lowStockCard);
        JPanel criticalCard=coloredSummaryCard("Critical",String.valueOf(DatabaseManager.getCriticalStockCount()),sectionIcon(2),new Color(0xFF,0xEB,0xEE),RED);
        makeClickable(criticalCard,()->showMedicineList("Critical Stock Medicines",getCriticalStockRows()));
        cards.add(criticalCard);
        JPanel revenueCard=coloredSummaryCard("Revenue","\u20b9"+String.format("%.2f",DatabaseManager.getTotalRevenue()),sectionIcon(0),new Color(0xFF,0xF0,0xF5),PINK);
        makeClickable(revenueCard,this::showRevenueHistory);
        cards.add(revenueCard);

        String[] cols={"Medicine","Stock","Value","Status"};
        Object[][] rows=getInventoryValueRows();
        JPanel center=new JPanel(); center.setOpaque(false);
        center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE,92));
        center.add(cards);
        center.add(Box.createVerticalStrut(12));
        center.add(reportInsights());
        p.add(center,BorderLayout.CENTER);
        JTable tbl=simpleTable(cols,rows);
        JScrollPane sp=new JScrollPane(tbl); sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE); applyTransparentScrollBar(sp);
        p.add(sp,BorderLayout.SOUTH);
        return p;
    }

    private List<Object[]> getLowStockOnlyRows() {
        List<Object[]> rows=new ArrayList<>();
        for(int i=0;i<sharedTableModel.getRowCount();i++){
            if("Low Stock".equals(String.valueOf(sharedTableModel.getValueAt(i,6)))) rows.add(medicineDialogRow(i));
        }
        return rows;
    }

    private List<Object[]> getCriticalStockRows() {
        List<Object[]> rows=new ArrayList<>();
        for(int i=0;i<sharedTableModel.getRowCount();i++){
            if("Critical".equals(String.valueOf(sharedTableModel.getValueAt(i,6)))) rows.add(medicineDialogRow(i));
        }
        return rows;
    }

    private Object[][] getInventoryValueRows() {
        Object[][] rows=new Object[sharedTableModel.getRowCount()][4];
        for(int i=0;i<sharedTableModel.getRowCount();i++){
            int stock=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(i,3)));
            double price=Double.parseDouble(String.valueOf(sharedTableModel.getValueAt(i,4)));
            rows[i][0]=sharedTableModel.getValueAt(i,1);
            rows[i][1]=stock;
            rows[i][2]="\u20b9"+String.format("%.2f",stock*price);
            rows[i][3]=sharedTableModel.getValueAt(i,6);
        }
        return rows;
    }

    private void showInventoryValueList() {
        JTable table=simpleTable(new String[]{"Medicine","Stock","Value","Status"},getInventoryValueRows());
        JScrollPane sp=new JScrollPane(table);
        sp.setPreferredSize(new Dimension(760,320));
        sp.setBorder(BorderFactory.createLineBorder(BRD));
        sp.getViewport().setBackground(WHITE);
        applyTransparentScrollBar(sp);
        JOptionPane.showMessageDialog(this,sp,"Inventory Value",JOptionPane.PLAIN_MESSAGE);
    }

    JPanel createUsersPage() {
        JPanel p=new JPanel(new BorderLayout(0,12)); p.setOpaque(false);
        JPanel header=new JPanel(new BorderLayout()); header.setOpaque(false);
        JLabel t=new JLabel(" Users & Login History"); t.setFont(FB16); t.setForeground(DARK);
        t.setIcon(qaIcon(0)); t.setIconTextGap(8);
        JButton addUser=pinkBtn("+ Add User");
        addUser.setPreferredSize(new Dimension(120,34));
        addUser.addActionListener(e->showAddUserDialog());
        header.add(t,BorderLayout.WEST);
        header.add(addUser,BorderLayout.EAST);
        p.add(header,BorderLayout.NORTH);

        List<Object[]> usersData=DatabaseManager.getAllUsers();
        List<Object[]> loginData=DatabaseManager.getLoginHistory();
        JPanel cards=new JPanel(new GridLayout(1,3,12,0)); cards.setOpaque(false);
        cards.add(coloredSummaryCard("Users",String.valueOf(usersData.size()),qaIcon(0),new Color(0xFF,0xF0,0xF5),PINK));
        cards.add(coloredSummaryCard("Recent Logins",String.valueOf(loginData.size()),sectionIcon(0),new Color(0xF5,0xF0,0xFF),new Color(0x7B,0x1F,0xA2)));
        cards.add(coloredSummaryCard("Access","Admin + Staff",sectionIcon(2),new Color(0xE8,0xF5,0xE9),GREEN));
        p.add(cards,BorderLayout.CENTER);

        JPanel tables=new JPanel(new GridLayout(1,2,12,0)); tables.setOpaque(false);
        JTable users=simpleTable(new String[]{"Username","Role","Created"},listRows(usersData));
        JTable history=simpleTable(new String[]{"Username","Result","Time"},listRows(loginData));
        JScrollPane userSp=new JScrollPane(users); userSp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BRD),"Users"));
        JScrollPane historySp=new JScrollPane(history); historySp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BRD),"Recent Logins"));
        userSp.getViewport().setBackground(WHITE); historySp.getViewport().setBackground(WHITE);
        applyTransparentScrollBar(userSp); applyTransparentScrollBar(historySp);
        tables.add(userSp); tables.add(historySp);
        p.add(tables,BorderLayout.SOUTH);
        return p;
    }

    private void showAddUserDialog() {
        JDialog dlg=new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"Add User",true);
        dlg.setSize(430,340);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        JPanel p=new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(24,28,24,28));
        p.setBackground(WHITE);
        JLabel title=new JLabel("Add User");
        title.setFont(new Font("Segoe UI",Font.BOLD,20));
        title.setForeground(DARK);
        title.setAlignmentX(0f);
        p.add(title);
        p.add(Box.createVerticalStrut(18));
        JTextField username=addField(p,"Username");
        JPasswordField password=passwordField(p,"Password");
        JPasswordField confirm=passwordField(p,"Confirm Password");
        JComboBox<String> role=new JComboBox<>(new String[]{"Staff","Admin"});
        role.setFont(F13);
        role.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
        JPanel rolePanel=new JPanel(new BorderLayout());
        rolePanel.setOpaque(false);
        rolePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,60));
        rolePanel.setAlignmentX(0f);
        JLabel roleLabel=new JLabel("Role");
        roleLabel.setFont(FB13);
        roleLabel.setForeground(DARK);
        rolePanel.add(roleLabel,BorderLayout.NORTH);
        rolePanel.add(role,BorderLayout.CENTER);
        p.add(rolePanel);
        p.add(Box.createVerticalStrut(16));
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        btns.setOpaque(false);
        btns.setAlignmentX(0f);
        btns.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        JButton cancel=new JButton("Cancel");
        cancel.setFont(FB13);
        cancel.setForeground(GRAY);
        cancel.setBackground(WHITE);
        cancel.setBorder(BorderFactory.createLineBorder(BRD,1,true));
        cancel.setPreferredSize(new Dimension(100,36));
        cancel.addActionListener(e->dlg.dispose());
        JButton save=pinkBtn("Create User");
        save.setPreferredSize(new Dimension(130,36));
        save.addActionListener(e->{
            String name=username.getText().trim();
            char[] pass=password.getPassword();
            char[] pass2=confirm.getPassword();
            if(name.isEmpty()||pass.length==0||pass2.length==0){
                JOptionPane.showMessageDialog(dlg,"Please fill all fields.","Add User",JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(name.length()<3){
                JOptionPane.showMessageDialog(dlg,"Username must be at least 3 characters.","Add User",JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(pass.length<4){
                JOptionPane.showMessageDialog(dlg,"Password must be at least 4 characters.","Add User",JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!java.util.Arrays.equals(pass,pass2)){
                JOptionPane.showMessageDialog(dlg,"Passwords do not match.","Add User",JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(DatabaseManager.userExists(name)){
                JOptionPane.showMessageDialog(dlg,"This username already exists.","Add User",JOptionPane.ERROR_MESSAGE);
                return;
            }
            DatabaseManager.addUser(name,new String(pass),String.valueOf(role.getSelectedItem()));
            java.util.Arrays.fill(pass,'\0');
            java.util.Arrays.fill(pass2,'\0');
            dlg.dispose();
            JOptionPane.showMessageDialog(this,"User added successfully. They can now log in.","Add User",JOptionPane.INFORMATION_MESSAGE);
            navigateTo(6);
        });
        btns.add(cancel);
        btns.add(save);
        p.add(btns);
        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private JPasswordField passwordField(JPanel p,String label) {
        JPanel fp=new JPanel(new BorderLayout());
        fp.setOpaque(false);
        fp.setMaximumSize(new Dimension(Integer.MAX_VALUE,52));
        fp.setAlignmentX(0f);
        JLabel l=new JLabel(label);
        l.setFont(FB13);
        l.setForeground(DARK);
        JPasswordField pf=new JPasswordField();
        pf.setFont(F13);
        pf.setPreferredSize(new Dimension(0,32));
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BRD,1,true),BorderFactory.createEmptyBorder(4,10,4,10)));
        fp.add(l,BorderLayout.NORTH);
        fp.add(pf,BorderLayout.CENTER);
        p.add(fp);
        p.add(Box.createVerticalStrut(8));
        return pf;
    }

    private Object[][] listRows(List<Object[]> rows) {
        Object[][] data=new Object[rows.size()][];
        for(int i=0;i<rows.size();i++) data[i]=rows.get(i);
        return data;
    }

    JPanel createSettingsPage() {
        JPanel p=new JPanel(new BorderLayout(0,14)); p.setOpaque(false);
        JLabel t=new JLabel(" Settings"); t.setFont(FB16); t.setForeground(DARK);
        t.setIcon(sectionIcon(2)); t.setIconTextGap(8);
        p.add(t,BorderLayout.NORTH);

        JPanel grid=new JPanel(new GridLayout(2,2,14,14)); grid.setOpaque(false);
        grid.add(settingsCard("Pharmacy Profile",new JComponent[]{
            inputRow("Pharmacy Name","PHARMACY INVENTORY SYSTEM"),
            inputRow("Owner","Admin"),
            inputRow("Contact","98765 43210")
        }));
        grid.add(settingsCard("Billing Preferences",new JComponent[]{
            inputRow("Default Tax","5%"),
            inputRow("Payment Mode","Cash / UPI"),
            checkRow("Auto print bill",true)
        }));
        grid.add(settingsCard("Stock Rules",new JComponent[]{
            inputRow("Low stock limit","10"),
            inputRow("Critical limit","5"),
            checkRow("Show expiry alerts",true)
        }));
        grid.add(settingsCard("Notifications",new JComponent[]{
            checkRow("Daily stock summary",true),
            checkRow("Purchase reminders",false),
            checkRow("Low stock popup",true)
        }));
        p.add(grid,BorderLayout.CENTER);

        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); bottom.setOpaque(false);
        JButton reset=new JButton("Reset");
        reset.setFont(FB13);reset.setForeground(GRAY);reset.setBackground(WHITE);
        reset.setBorder(BorderFactory.createLineBorder(BRD,1,true));
        reset.setPreferredSize(new Dimension(100,34));
        JButton save=pinkBtn("Save Settings"); save.setPreferredSize(new Dimension(140,34));
        save.addActionListener(e->JOptionPane.showMessageDialog(this,"Settings saved successfully.","Settings",JOptionPane.INFORMATION_MESSAGE));
        bottom.add(reset); bottom.add(save);
        p.add(bottom,BorderLayout.SOUTH);
        return p;
    }

    private JPanel settingsCard(String title,JComponent[] rows) {
        JPanel card=rndPanel(); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        JLabel h=new JLabel(title); h.setFont(FB16); h.setForeground(PINK); h.setAlignmentX(0f);
        card.add(h); card.add(Box.createVerticalStrut(12));
        for(JComponent row:rows){row.setAlignmentX(0f);card.add(row);card.add(Box.createVerticalStrut(10));}
        return card;
    }

    private JPanel inputRow(String label,String value) {
        JPanel row=new JPanel(new BorderLayout(8,0)); row.setOpaque(false);
        JLabel l=new JLabel(label); l.setFont(F12); l.setForeground(GRAY); l.setPreferredSize(new Dimension(110,30));
        JTextField f=input(value); f.setPreferredSize(new Dimension(0,30));
        row.add(l,BorderLayout.WEST); row.add(f,BorderLayout.CENTER);
        return row;
    }

    private JPanel checkRow(String label,boolean selected) {
        JPanel row=new JPanel(new BorderLayout()); row.setOpaque(false);
        JCheckBox cb=new JCheckBox(label,selected); cb.setFont(F13); cb.setForeground(DARK);
        cb.setOpaque(false); cb.setFocusPainted(false); cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.add(cb,BorderLayout.WEST);
        return row;
    }

    private JPanel simpleField(String label,String value) {
        JPanel p=new JPanel(new BorderLayout(0,4)); p.setOpaque(false);
        JLabel l=new JLabel(label); l.setFont(FB12); l.setForeground(DARK);
        JTextField f=new JTextField(value); f.setFont(F13);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BRD,1,true),BorderFactory.createEmptyBorder(4,8,4,8)));
        p.add(l,BorderLayout.NORTH); p.add(f,BorderLayout.CENTER);
        return p;
    }

    private JPanel comboField(String label,String[] values) {
        JPanel p=new JPanel(new BorderLayout(0,4)); p.setOpaque(false);
        JLabel l=new JLabel(label); l.setFont(FB12); l.setForeground(DARK);
        JComboBox<String> cb=new JComboBox<>(values); cb.setFont(F13);
        p.add(l,BorderLayout.NORTH); p.add(cb,BorderLayout.CENTER);
        return p;
    }

    private JTextField input(String value) {
        JTextField f=new JTextField(value); f.setFont(F13);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BRD,1,true),BorderFactory.createEmptyBorder(4,8,4,8)));
        return f;
    }

    private JPanel labeled(String label,JComponent field) {
        JPanel p=new JPanel(new BorderLayout(0,4)); p.setOpaque(false);
        JLabel l=new JLabel(label); l.setFont(FB12); l.setForeground(DARK);
        p.add(l,BorderLayout.NORTH); p.add(field,BorderLayout.CENTER);
        return p;
    }

    private String[] medicineNames() {
        String[] names=new String[sharedTableModel.getRowCount()];
        for(int i=0;i<names.length;i++) names[i]=String.valueOf(sharedTableModel.getValueAt(i,1));
        return names;
    }

    private JTable simpleTable(String[] columns,Object[][] rows) {
        JTable t=new JTable(new DefaultTableModel(rows,columns){public boolean isCellEditable(int r,int c){return false;}});
        t.setFont(F13); t.setRowHeight(38); t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0,0));
        t.setSelectionBackground(new Color(252,228,236)); t.setSelectionForeground(new Color(180,60,90));
        t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table,Object v,boolean sel,boolean foc,int r,int c){
                JLabel l=(JLabel)super.getTableCellRendererComponent(table,v,sel,foc,r,c);
                if(!sel)l.setBackground(r%2==0?WHITE:ALT);
                l.setForeground(DARK); l.setOpaque(true); l.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
                return l;
            }
        });
        JTableHeader h=t.getTableHeader(); h.setFont(FB13); h.setBackground(TBL_HDR_BG); h.setForeground(TBL_HDR_FG);
        h.setPreferredSize(new Dimension(0,38)); h.setReorderingAllowed(false);
        return t;
    }

    private JPanel summaryCard(String label,String value,Icon icon) {
        JPanel c=rndPanel(); c.setLayout(new BorderLayout(10,0));
        c.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JLabel ic=new JLabel(icon); c.add(ic,BorderLayout.WEST);
        JPanel tx=new JPanel(); tx.setLayout(new BoxLayout(tx,BoxLayout.Y_AXIS)); tx.setOpaque(false);
        JLabel v=new JLabel(value); v.setFont(FB16); v.setForeground(DARK);
        JLabel l=new JLabel(label); l.setFont(F12); l.setForeground(GRAY);
        tx.add(v); tx.add(Box.createVerticalStrut(3)); tx.add(l);
        c.add(tx,BorderLayout.CENTER);
        return c;
    }

    private JPanel coloredSummaryCard(String label,String value,Icon icon,Color bg,Color fg) {
        JLabel v=new JLabel(value);
        return totalCard(label,v,icon,bg,fg);
    }

    private JPanel reportInsights() {
        int tablet=0,capsule=0,inStock=0,low=0,critical=0,totalStock=0;
        String lowestName="",highestName="";
        int lowest=Integer.MAX_VALUE,highest=0;
        for(int i=0;i<sharedTableModel.getRowCount();i++){
            int stock=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(i,3)));
            String cat=String.valueOf(sharedTableModel.getValueAt(i,2));
            String st=String.valueOf(sharedTableModel.getValueAt(i,6));
            String name=String.valueOf(sharedTableModel.getValueAt(i,1));
            if("Capsule".equals(cat)) capsule+=stock; else tablet+=stock;
            if("In Stock".equals(st)) inStock++; else if("Low Stock".equals(st)) low++; else critical++;
            totalStock+=stock;
            if(stock<lowest){lowest=stock;lowestName=name;}
            if(stock>highest){highest=stock;highestName=name;}
        }
        JPanel wrap=new JPanel(new GridLayout(1,4,12,0)); wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE,150));
        wrap.add(insightPanel("Stock Health",
            new String[]{"In Stock: "+inStock,"Low Stock: "+low,"Critical: "+critical},
            new Color(0xE8,0xF5,0xE9),GREEN));
        wrap.add(insightPanel("Category Split",
            new String[]{"Tablet units: "+tablet,"Capsule units: "+capsule,"Total units: "+totalStock},
            new Color(0xFF,0xF7,0xE8),new Color(0xE6,0x51,0x00)));
        wrap.add(insightPanel("Attention",
            new String[]{"Lowest: "+lowestName+" ("+lowest+")","Highest: "+highestName+" ("+highest+")","Top sale: "+DatabaseManager.getTopSellingMedicine()},
            new Color(0xFF,0xF0,0xF5),PINK));
        wrap.add(insightPanel("Revenue",
            new String[]{"Today: \u20b9"+String.format("%.2f",DatabaseManager.getRevenueToday()),"This month: \u20b9"+String.format("%.2f",DatabaseManager.getRevenueThisMonth()),"Bills: "+DatabaseManager.getBillCount()},
            new Color(0xF5,0xF0,0xFF),new Color(0x7B,0x1F,0xA2)));
        return wrap;
    }

    private JPanel insightPanel(String title,String[] lines,Color bg,Color accent) {
        JPanel p=new JPanel(){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight()-2,14,14));
                g2.setColor(BRD);g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-3,14,14));
                g2.dispose();
            }
        };
        p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(16,18,16,18));
        JLabel h=new JLabel(title);h.setFont(FB13);h.setForeground(accent);h.setAlignmentX(0f);
        p.add(h);p.add(Box.createVerticalStrut(10));
        for(String line:lines){
            JLabel l=new JLabel(line);l.setFont(F12);l.setForeground(DARK);l.setAlignmentX(0f);
            p.add(l);p.add(Box.createVerticalStrut(6));
        }
        return p;
    }

    private JPanel totalCard(String label,JLabel value,Icon icon,Color bg,Color fg) {
        JPanel c=new JPanel(){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight()-2,14,14));
                g2.setColor(BRD);g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-3,14,14));
                g2.dispose();
            }
        };
        c.setOpaque(false); c.setLayout(new BorderLayout(10,0));
        c.setBorder(BorderFactory.createEmptyBorder(18,16,18,16));
        JLabel ic=new JLabel(icon); c.add(ic,BorderLayout.WEST);
        JPanel tx=new JPanel(); tx.setLayout(new BoxLayout(tx,BoxLayout.Y_AXIS)); tx.setOpaque(false);
        value.setFont(FB28); value.setForeground(fg);
        JLabel l=new JLabel(label); l.setFont(F12); l.setForeground(GRAY);
        tx.add(value); tx.add(Box.createVerticalStrut(3)); tx.add(l);
        c.add(tx,BorderLayout.CENTER);
        return c;
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

    private JPanel exportButtons(JButton addButton) {
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        buttons.setOpaque(false);
        JButton csv=outlinePinkBtn("Export CSV");
        JButton pdf=outlinePinkBtn("Export PDF");
        csv.setIcon(exportIcon("CSV"));
        pdf.setIcon(exportIcon("PDF"));
        csv.setIconTextGap(6);
        pdf.setIconTextGap(6);
        csv.addActionListener(e->exportCsv());
        pdf.addActionListener(e->exportPdf());
        buttons.add(csv);
        buttons.add(pdf);
        buttons.add(addButton);
        return buttons;
    }

    private JButton outlinePinkBtn(String text) {
        JButton b=new JButton(text){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),20,20));
                g2.setColor(PINK);g2.setStroke(new BasicStroke(1.4f));
                g2.draw(new RoundRectangle2D.Float(0,0,getWidth()-1,getHeight()-1,20,20));
                g2.dispose();super.paintComponent(g);
            }
        };
        b.setFont(FB13);b.setForeground(PINK);b.setOpaque(false);b.setContentAreaFilled(false);
        b.setBorderPainted(false);b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(150,32));
        return b;
    }

    private Icon exportIcon(String label) {
        return new Icon(){
            public int getIconWidth(){return 18;}
            public int getIconHeight(){return 18;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x,y);
                g2.setColor(WHITE);
                g2.fillRoundRect(2,1,13,16,3,3);
                g2.setColor(PINK);
                g2.setStroke(new BasicStroke(1.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g2.drawRoundRect(2,1,13,16,3,3);
                g2.drawLine(5,6,12,6);
                g2.drawLine(5,9,12,9);
                g2.drawLine(5,12,10,12);
                g2.setFont(new Font("Segoe UI",Font.BOLD,5));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(label,2+(13-fm.stringWidth(label))/2,16);
                g2.dispose();
            }
        };
    }

    private void exportCsv() {
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Save CSV");
        chooser.setSelectedFile(new File("medicine_inventory.csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File file=ensureExtension(chooser.getSelectedFile(),".csv");
        try(PrintWriter out=new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"))){
            out.println("ID,Medicine Name,Category,Stock,Price,Expiry Date,Status");
            for(int r=0;r<sharedTableModel.getRowCount();r++){
                for(int c=0;c<7;c++){
                    if(c>0) out.print(",");
                    out.print(csvValue(sharedTableModel.getValueAt(r,c)));
                }
                out.println();
            }
            JOptionPane.showMessageDialog(this,"CSV exported successfully!","Success",JOptionPane.INFORMATION_MESSAGE);
        }catch(IOException ex){
            JOptionPane.showMessageDialog(this,"Unable to export CSV: "+ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csvValue(Object value) {
        String text=String.valueOf(value==null?"":value);
        if(text.contains(",")||text.contains("\"")||text.contains("\n")||text.contains("\r")){
            text="\""+text.replace("\"","\"\"")+"\"";
        }
        return text;
    }

    private void exportPdf() {
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Save PDF");
        chooser.setSelectedFile(new File("medicine_inventory.pdf"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File file=ensureExtension(chooser.getSelectedFile(),".pdf");
        try{
            System.setProperty("sun.java2d.print.enableAWT","true");
            PrintService pdfService=findPdfPrintService();
            if(pdfService==null){
                JOptionPane.showMessageDialog(this,"No PDF print service found on this computer.","Export Error",JOptionPane.ERROR_MESSAGE);
                return;
            }
            PrinterJob job=PrinterJob.getPrinterJob();
            job.setPrintService(pdfService);
            PageFormat format=job.defaultPage();
            Book book=new Book();
            book.append(new InventoryReportPrintable(),format);
            job.setPageable(book);
            PrintRequestAttributeSet attrs=new HashPrintRequestAttributeSet();
            attrs.add(new Destination(file.toURI()));
            job.print(attrs);
            JOptionPane.showMessageDialog(this,"PDF exported successfully!","Success",JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Unable to export PDF: "+ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private PrintService findPdfPrintService() {
        PrintService[] services=PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE,null);
        for(PrintService service:services){
            String name=service.getName().toLowerCase();
            if(name.contains("pdf")) return service;
        }
        return null;
    }

    private void applyExpiryUrgencyRenderer(JTable table) {
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int r,int c){
                JLabel l=(JLabel)super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                LocalDate expiry=parseExpiryDate(String.valueOf(t.getValueAt(r,5)));
                Color fg=DARK;
                Color bg=r%2==0?WHITE:ALT;
                if(expiry!=null){
                    long days=ChronoUnit.DAYS.between(LocalDate.now(),expiry);
                    if(days<=30){
                        fg=RED;
                        bg=new Color(0xFF,0xEB,0xEE);
                    }else if(days<=62){
                        fg=BADGE_ORANGE_FG;
                        bg=new Color(0xFF,0xF8,0xE1);
                    }
                }
                if(!sel)l.setBackground(bg);
                l.setForeground(sel?new Color(180,60,90):fg);
                l.setOpaque(true);
                l.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
                return l;
            }
        });
    }

    private File ensureExtension(File file,String extension) {
        String path=file.getAbsolutePath();
        if(!path.toLowerCase().endsWith(extension)) return new File(path+extension);
        return file;
    }

    private class InventoryReportPrintable implements Printable {
        public int print(Graphics graphics,PageFormat pageFormat,int pageIndex) {
            if(pageIndex>0) return NO_SUCH_PAGE;
            Graphics2D g=(Graphics2D)graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int x=(int)pageFormat.getImageableX()+24;
            int y=(int)pageFormat.getImageableY()+32;
            int width=(int)pageFormat.getImageableWidth()-48;
            String date=new SimpleDateFormat("dd MMM yyyy").format(new Date());

            g.setColor(DARK);
            g.setFont(new Font("Segoe UI",Font.BOLD,18));
            drawCentered(g,"Pharmacy Inventory System",x,y,width);
            y+=26;
            g.setFont(new Font("Segoe UI",Font.PLAIN,11));
            drawCentered(g,"Medicine Inventory Report - "+date,x,y,width);
            y+=24;

            String[] headers={"ID","Name","Category","Stock","Price","Expiry","Status"};
            int[] colWidths={48,140,78,48,62,80,82};
            int totalWidth=0;
            for(int w:colWidths) totalWidth+=w;
            if(totalWidth>width){
                double scale=width/(double)totalWidth;
                totalWidth=0;
                for(int i=0;i<colWidths.length;i++){colWidths[i]=(int)Math.floor(colWidths[i]*scale);totalWidth+=colWidths[i];}
            }

            int rowHeight=22;
            int tableX=x+(width-totalWidth)/2;
            int tableY=y;
            int rows=sharedTableModel.getRowCount()+1;
            int tableHeight=rows*rowHeight;
            g.setColor(Color.BLACK);
            g.drawRect(tableX,tableY,totalWidth,tableHeight);
            int cx=tableX;
            for(int i=0;i<colWidths.length;i++){
                if(i>0) g.drawLine(cx,tableY,cx,tableY+tableHeight);
                cx+=colWidths[i];
            }
            for(int r=1;r<=rows;r++) g.drawLine(tableX,tableY+(r*rowHeight),tableX+totalWidth,tableY+(r*rowHeight));

            g.setFont(new Font("Segoe UI",Font.BOLD,9));
            cx=tableX;
            for(int c=0;c<headers.length;c++){
                drawCellText(g,headers[c],cx+4,tableY+15,colWidths[c]-8);
                cx+=colWidths[c];
            }

            g.setFont(new Font("Segoe UI",Font.PLAIN,9));
            for(int r=0;r<sharedTableModel.getRowCount();r++){
                cx=tableX;
                for(int c=0;c<7;c++){
                    drawCellText(g,String.valueOf(sharedTableModel.getValueAt(r,c)),cx+4,tableY+rowHeight*(r+1)+15,colWidths[c]-8);
                    cx+=colWidths[c];
                }
            }

            int footerY=(int)(pageFormat.getImageableY()+pageFormat.getImageableHeight()-22);
            g.setFont(new Font("Segoe UI",Font.PLAIN,9));
            drawCentered(g,"Generated on "+date+" | Page 1 of 1",x,footerY,width);
            return PAGE_EXISTS;
        }
    }

    private void drawCentered(Graphics2D g,String text,int x,int y,int width) {
        FontMetrics fm=g.getFontMetrics();
        g.drawString(text,x+(width-fm.stringWidth(text))/2,y);
    }

    private void drawCellText(Graphics2D g,String text,int x,int y,int maxWidth) {
        FontMetrics fm=g.getFontMetrics();
        String value=text;
        while(value.length()>0&&fm.stringWidth(value)>maxWidth) value=value.substring(0,value.length()-1);
        if(!value.equals(text)&&value.length()>3) value=value.substring(0,value.length()-3)+"...";
        g.drawString(value,x,y);
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
            int sv;double pv;
            try{sv=validStock(stk);}catch(Exception ex){JOptionPane.showMessageDialog(dlg,"Stock must be zero or more!");return;}
            try{pv=validPrice(pr);}catch(Exception ex){JOptionPane.showMessageDialog(dlg,"Price must be greater than zero!");return;}
            LocalDate expiryDate=parseExpiryDate(exp);
            if(expiryDate==null){JOptionPane.showMessageDialog(dlg,"Expiry date must be like MM/YYYY, Jan 2027, or YYYY-MM-DD.");return;}
            if(expiryDate.isBefore(LocalDate.now())){JOptionPane.showMessageDialog(dlg,"Expiry date has passed. Cannot keep this medicine in stock.");return;}
            Object[] row=new Object[]{
                sharedTableModel.getValueAt(dataIdx,0),
                nm,
                cf.getSelectedItem(),
                sv,
                String.format("%.2f",pv),
                exp,
                status(sv)
            };
            DatabaseManager.updateMedicine(row);
            loadMedicinesFromDatabase();
            refreshTable();updateStats();dlg.dispose();JOptionPane.showMessageDialog(this,"Medicine updated!");
        });
        btns.add(cancel);btns.add(save);p.add(btns);
        dlg.setContentPane(p);dlg.setVisible(true);
    }

    private static class StockOverviewPanel extends JPanel {
        private final String[] categories={"Tablet","Capsule","Syrup","Injection"};
        private final int[] totals=new int[categories.length];
        private final Rectangle[] bars=new Rectangle[categories.length];
        private Rectangle closeRect=new Rectangle();
        private Runnable closeAction;
        private int hover=-1,frame=0;
        private boolean closeHover=false;
        private Timer anim;

        StockOverviewPanel(){
            setOpaque(false);
            setPreferredSize(new Dimension(0,180));
            setMaximumSize(new Dimension(Integer.MAX_VALUE,180));
            setToolTipText("");
            refreshData();
            MouseAdapter ma=new MouseAdapter(){
                public void mouseMoved(MouseEvent e){updateHover(e.getPoint());}
                public void mouseExited(MouseEvent e){hover=-1;setToolTipText(null);repaint();}
                public void mouseClicked(MouseEvent e){
                    if(closeRect.contains(e.getPoint())&&closeAction!=null)closeAction.run();
                }
            };
            addMouseMotionListener(ma);
            addMouseListener(ma);
        }

        void setCloseAction(Runnable closeAction){
            this.closeAction=closeAction;
        }

        void refreshData(){
            for(int i=0;i<totals.length;i++)totals[i]=0;
            if(sharedTableModel!=null){
                for(int r=0;r<sharedTableModel.getRowCount();r++){
                    String cat=String.valueOf(sharedTableModel.getValueAt(r,2));
                    int stock=0;
                    try{stock=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(r,3)));}catch(Exception ignored){}
                    for(int i=0;i<categories.length;i++)if(categories[i].equals(cat))totals[i]+=stock;
                }
            }
            repaint();
        }

        void restartAnimation(){
            frame=0;
            if(anim!=null&&anim.isRunning())anim.stop();
            anim=new Timer(16,e->{
                frame++;
                repaint();
                if(frame>=20)((Timer)e.getSource()).stop();
            });
            anim.start();
        }

        public String getToolTipText(MouseEvent e){
            updateHover(e.getPoint());
            if(hover>=0)return "Category: "+categories[hover]+" | Total Stock: "+totals[hover];
            return null;
        }

        private void updateHover(Point p){
            boolean nextClose=closeRect.contains(p);
            int next=-1;
            if(!nextClose)for(int i=0;i<bars.length;i++)if(bars[i]!=null&&bars[i].contains(p)){next=i;break;}
            if(next!=hover||nextClose!=closeHover){
                hover=next;
                closeHover=nextClose;
                setCursor(Cursor.getPredefinedCursor(closeHover?Cursor.HAND_CURSOR:Cursor.DEFAULT_CURSOR));
                if(hover>=0)setToolTipText("Category: "+categories[hover]+" | Total Stock: "+totals[hover]);
                else setToolTipText(null);
                repaint();
            }
        }

        protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();
            g2.setColor(WHITE);
            g2.fill(new RoundRectangle2D.Float(0,0,w-1,h-1,12,12));
            g2.setColor(new Color(0xF0,0xD8,0xDF));
            g2.draw(new RoundRectangle2D.Float(0,0,w-1,h-1,12,12));

            int pad=16;
            g2.setFont(new Font("Segoe UI Emoji",Font.BOLD,14));
            g2.setColor(DARK);
            g2.drawString("\uD83D\uDCCA Stock Overview by Category",pad,28);
            g2.setFont(F12);
            g2.setColor(GRAY);
            g2.drawString("Live from your medicine table",w-pad-170,28);
            closeRect.setBounds(w-pad-28,10,20,20);
            g2.setColor(closeHover?new Color(0xFC,0xE4,0xEC):WHITE);
            g2.fill(new RoundRectangle2D.Float(closeRect.x,closeRect.y,closeRect.width,closeRect.height,8,8));
            g2.setColor(closeHover?PINK:GRAY);
            g2.setStroke(new BasicStroke(1.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Float(closeRect.x,closeRect.y,closeRect.width-1,closeRect.height-1,8,8));
            g2.drawLine(closeRect.x+6,closeRect.y+6,closeRect.x+14,closeRect.y+14);
            g2.drawLine(closeRect.x+14,closeRect.y+6,closeRect.x+6,closeRect.y+14);

            int chartX=pad+40,chartY=42,chartW=w-pad*2-55,chartH=82;
            int axisY=chartY+chartH;
            int max=1;
            for(int v:totals)if(v>max)max=v;
            Stroke oldStroke=g2.getStroke();
            g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
            FontMetrics fm=g2.getFontMetrics();
            for(int i=0;i<=5;i++){
                int val=(int)Math.round(max-(max*(i/5.0)));
                int y=chartY+(int)Math.round((i/5.0)*chartH);
                g2.setColor(GRAY);
                String label=String.valueOf(val);
                g2.drawString(label,chartX-10-fm.stringWidth(label),y+4);
                g2.setColor(new Color(0xF0,0xE8,0xEA));
                g2.setStroke(new BasicStroke(1f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[]{5f,5f},0));
                g2.drawLine(chartX,y,chartX+chartW,y);
            }
            g2.setStroke(oldStroke);
            g2.setColor(new Color(0xE0,0xD0,0xD5));
            g2.drawLine(chartX,axisY,chartX+chartW,axisY);

            int barW=40,gap=20,totalBarW=categories.length*barW+(categories.length-1)*gap;
            int startX=chartX+(chartW-totalBarW)/2;
            double progress=Math.min(1.0,frame/20.0);
            if(!isVisible())progress=0;
            for(int i=0;i<categories.length;i++){
                int fullH=max==0?0:(int)Math.round((totals[i]/(double)max)*(chartH-12));
                int bh=(int)Math.round(fullH*progress);
                int x=startX+i*(barW+gap),y=axisY-bh;
                bars[i]=new Rectangle(x,y,barW,bh);
                g2.setColor(i==hover?new Color(0xE9,0x1E,0x63):new Color(0xC9,0x63,0x7A,200));
                g2.fill(new RoundRectangle2D.Float(x,y,barW,bh,8,8));
                g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
                g2.setColor(PINK);
                String value=String.valueOf(totals[i]);
                g2.drawString(value,x+(barW-fm.stringWidth(value))/2,y-5);
                g2.setColor(GRAY);
                g2.drawString(categories[i],x+(barW-fm.stringWidth(categories[i]))/2,axisY+18);
            }

            g2.setColor(new Color(0xF0,0xD8,0xDF));
            g2.drawLine(pad,h-32,w-pad,h-32);
            int statsY=h-13;
            g2.setFont(F12);
            drawMiniStat(g2,"Most Stocked: Paracetamol (50)",GREEN,pad,statsY);
            int x2=pad+190;
            g2.setColor(new Color(0xB0,0xA0,0xA5));g2.drawString("|",x2,statsY);
            drawMiniStat(g2,"Critical Items: 2",RED,x2+18,statsY);
            int x3=x2+130;
            g2.setColor(new Color(0xB0,0xA0,0xA5));g2.drawString("|",x3,statsY);
            drawMiniStat(g2,"Expiring This Month: 1",ORANGE,x3+18,statsY);
            g2.dispose();
        }

        private void drawMiniStat(Graphics2D g2,String text,Color color,int x,int y){
            g2.setColor(color);
            g2.drawString(text,x,y);
        }
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
                    int id=Integer.parseInt(String.valueOf(sharedTableModel.getValueAt(di,0)));
                    DatabaseManager.deleteMedicine(id);
                    loadMedicinesFromDatabase();
                    dp.refreshTable();dp.updateStats();}}});
            panel.add(edit);panel.add(del);}
        public Component getTableCellEditorComponent(JTable t,Object v,boolean sel,int r,int c){
            row=r;panel.setBackground(sel?new Color(252,228,236):WHITE);return panel;}
        public Object getCellEditorValue(){return "";}
    }
}
