import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PharmacyInventorySystem extends JFrame {
    static final Color PINK=new Color(0xC9,0x63,0x7A), PINK_BG=new Color(0xFF,0xF0,0xF3);
    static final Color PINK_HL=new Color(0xFC,0xE4,0xEC), SIDEBAR_TXT=new Color(0x88,0x00,0x3B);
    static final Color WHITE=Color.WHITE, DARK=new Color(0x21,0x21,0x21), GRAY=new Color(0x75,0x75,0x75);
    static final Color DIVIDER=new Color(0xF0,0xD0,0xD8), LOGOUT_RED=new Color(0xD3,0x2F,0x2F);
    static final Font FNAV=new Font("Segoe UI",Font.PLAIN,14), FNAVB=new Font("Segoe UI",Font.BOLD,14);
    static final Font FBRAND=new Font("Segoe UI",Font.BOLD,20), FSUB=new Font("Segoe UI",Font.PLAIN,12);
    private String[] navLabels={"Dashboard","Medicines","Billing","Purchases","Stock Alerts","Reports","Users","Settings"};
    private int selectedIndex=0;
    private java.util.List<NavButton> navButtons=new java.util.ArrayList<>();
    private JLabel dateLabel,timeLabel;
    private JPanel contentPanel;
    private DashboardPanel dashPanel;

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext","true");
        SwingUtilities.invokeLater(()->{
            try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e){}
            new PharmacyInventorySystem();
        });
    }

    PharmacyInventorySystem() {
        setTitle("Pharmacy Inventory System");
        setSize(1280,800); setResizable(false); setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        BufferedImage icon=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig=icon.createGraphics();
        ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        ig.setColor(PINK); ig.fillOval(2,2,28,28);
        ig.setColor(WHITE); ig.setFont(new Font("SansSerif",Font.BOLD,18)); ig.drawString("+",9,23);
        ig.dispose(); setIconImage(icon);
        JPanel root=new JPanel(new BorderLayout()); root.setBackground(WHITE);
        root.add(createSidebar(),BorderLayout.WEST);
        JPanel center=new JPanel(new BorderLayout()); center.setBackground(WHITE);
        center.add(createTopBar(),BorderLayout.NORTH);
        contentPanel=new JPanel(new BorderLayout());
        contentPanel.setBackground(WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20,24,0,24));
        dashPanel=new DashboardPanel();
        contentPanel.add(dashPanel);
        center.add(contentPanel,BorderLayout.CENTER);
        JLabel footer=new JLabel("  Pharmacy Inventory System | All rights reserved \u00A9 2025",SwingConstants.CENTER);
        footer.setIcon(new Icon(){public int getIconWidth(){return 12;}public int getIconHeight(){return 12;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xE9,0x1E,0x63));g2.translate(x,y);
                g2.fill(new java.awt.geom.Path2D.Float(){{moveTo(6,11);curveTo(0,7,0,2,3,2);curveTo(6,2,6,5,6,5);curveTo(6,5,6,2,9,2);curveTo(12,2,12,7,6,11);}});
                g2.dispose();}});
        footer.setFont(FSUB); footer.setForeground(PINK);
        footer.setPreferredSize(new Dimension(0,32));
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,new Color(0xF0,0xD8,0xDF)),
            BorderFactory.createEmptyBorder(8,0,8,0)));
        footer.setOpaque(true); footer.setBackground(PINK_BG);
        center.add(footer,BorderLayout.SOUTH);
        root.add(center,BorderLayout.CENTER);
        setContentPane(root);
        Timer t=new Timer(1000,e->updateClock()); t.setInitialDelay(0); t.start();
        setVisible(true);
    }

    // --- Graphics2D Nav Icons (18x18, #E91E63) ---
    static Icon makeNavIcon(int type, Color color) {
        return new Icon() {
            public int getIconWidth(){return 18;} public int getIconHeight(){return 18;}
            public void paintIcon(Component c,Graphics g,int x,int y) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color); g2.setStroke(new BasicStroke(1.8f));
                g2.translate(x,y);
                switch(type) {
                    case 0: // House
                        g2.fillPolygon(new int[]{9,1,17},new int[]{1,8,8},3);
                        g2.fillRect(3,8,12,9); g2.setColor(PINK_BG);
                        g2.fillRect(7,11,4,6); break;
                    case 1: // Pill capsule
                        g2.fillRoundRect(1,5,16,8,8,8);
                        g2.setColor(PINK_BG); g2.drawLine(9,5,9,13); break;
                    case 2: // Receipt
                        g2.drawRoundRect(3,1,12,15,3,3);
                        g2.drawLine(6,5,12,5); g2.drawLine(6,8,12,8); g2.drawLine(6,11,10,11); break;
                    case 3: // Shopping cart
                        g2.drawPolyline(new int[]{1,4,5,15,14},new int[]{2,2,12,12,5},5);
                        g2.fillOval(5,14,3,3); g2.fillOval(12,14,3,3); break;
                    case 4: // Bell
                        g2.fillArc(3,2,12,12,0,180);
                        g2.fillRect(3,8,12,5); g2.fillRect(1,13,16,2);
                        g2.fillOval(7,15,4,3); break;
                    case 5: // Bar chart
                        g2.fillRect(2,10,4,7); g2.fillRect(7,5,4,12); g2.fillRect(12,1,4,16); break;
                    case 6: // Person
                        g2.fillOval(6,1,6,6); g2.fillArc(3,9,12,10,0,180); break;
                    case 7: // Gear
                        g2.fillOval(5,5,8,8);
                        g2.setColor(PINK_BG); g2.fillOval(7,7,4,4); g2.setColor(color);
                        for(int i=0;i<6;i++){double a=Math.toRadians(i*60);
                            g2.fillRect((int)(9+Math.cos(a)*7)-2,(int)(9+Math.sin(a)*7)-2,4,4);} break;
                    case 8: // Logout arrow
                        g2.drawRect(2,2,8,14); g2.setColor(PINK_BG); g2.fillRect(6,3,4,13);
                        g2.setColor(color);
                        g2.drawLine(8,9,17,9); g2.drawLine(14,6,17,9); g2.drawLine(14,12,17,9); break;
                }
                g2.dispose();
            }
        };
    }

    private JPanel createSidebar() {
        JPanel sb=new JPanel(); sb.setLayout(new BoxLayout(sb,BoxLayout.Y_AXIS));
        sb.setPreferredSize(new Dimension(220,800)); sb.setMinimumSize(new Dimension(220,800));
        sb.setMaximumSize(new Dimension(220,Integer.MAX_VALUE));
        sb.setBackground(PINK_BG);
        sb.setBorder(BorderFactory.createMatteBorder(0,0,0,1,DIVIDER));
        JPanel bp=new JPanel(); bp.setLayout(new BoxLayout(bp,BoxLayout.Y_AXIS));
        bp.setOpaque(false); bp.setBorder(BorderFactory.createEmptyBorder(28,20,18,20));
        // Brand pill icon drawn with Graphics2D
        JLabel ic=new JLabel(makeNavIcon(1,PINK));
        ic.setFont(new Font("SansSerif",Font.PLAIN,36)); ic.setAlignmentX(0f);
        JLabel bn=new JLabel("PHARMACY"); bn.setFont(FBRAND); bn.setForeground(PINK); bn.setAlignmentX(0f);
        JLabel bs=new JLabel("INVENTORY SYSTEM"); bs.setFont(FSUB); bs.setForeground(SIDEBAR_TXT); bs.setAlignmentX(0f);
        bp.add(ic); bp.add(Box.createVerticalStrut(6)); bp.add(bn); bp.add(bs);
        sb.add(bp);
        JSeparator sep=new JSeparator(SwingConstants.HORIZONTAL);
        sep.setMaximumSize(new Dimension(200,1)); sep.setForeground(DIVIDER);
        sb.add(sep); sb.add(Box.createVerticalStrut(10));
        JPanel np=new JPanel(); np.setLayout(new BoxLayout(np,BoxLayout.Y_AXIS));
        np.setOpaque(false); np.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
        for(int i=0;i<navLabels.length;i++){
            NavButton b=new NavButton(navLabels[i],i,makeNavIcon(i,PINK));
            int idx=i; b.addActionListener(e->selectNav(idx));
            navButtons.add(b); np.add(b); np.add(Box.createVerticalStrut(4));
        }
        sb.add(np); sb.add(Box.createVerticalGlue());
        JPanel lp=new JPanel(); lp.setLayout(new BoxLayout(lp,BoxLayout.X_AXIS));
        lp.setOpaque(false); lp.setBorder(BorderFactory.createEmptyBorder(0,10,24,10));
        lp.setMaximumSize(new Dimension(220,48));
        NavButton lb=new NavButton("Logout",-1,makeNavIcon(8,LOGOUT_RED));
        lb.setForeground(LOGOUT_RED); lb.hoverColor=new Color(0xFF,0xEB,0xEE);
        lb.addActionListener(e->{
            if(JOptionPane.showConfirmDialog(this,"Are you sure you want to logout?","Logout",
                JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) System.exit(0);
        });
        lp.add(lb); sb.add(lp);
        return sb;
    }

    static Icon miniIcon(int type){
        return new Icon(){public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GRAY);g2.setStroke(new BasicStroke(1.4f));g2.translate(x,y);
                if(type==0){g2.drawRoundRect(2,3,12,11,2,2);g2.drawLine(2,7,14,7);g2.fillRect(5,1,2,3);g2.fillRect(10,1,2,3);}
                else{g2.drawOval(1,1,14,14);g2.drawLine(8,4,8,8);g2.drawLine(8,8,11,10);}
                g2.dispose();}};
    }
    private JPanel createTopBar() {
        JPanel tb=new JPanel(new BorderLayout()); tb.setBackground(WHITE);
        tb.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(0xEE,0xEE,0xEE)),
            BorderFactory.createEmptyBorder(18,28,18,28)));
        JLabel wl=new JLabel("Welcome, Admin ");
        wl.setIcon(makeNavIcon(1,PINK));
        wl.setFont(new Font("Segoe UI",Font.BOLD,20)); wl.setForeground(DARK);
        JPanel clockPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0));clockPanel.setOpaque(false);
        dateLabel=new JLabel();dateLabel.setFont(new Font("Segoe UI",Font.PLAIN,13));dateLabel.setForeground(DARK);
        dateLabel.setIcon(miniIcon(0));dateLabel.setIconTextGap(6);
        dateLabel.setOpaque(true);dateLabel.setBackground(new Color(0xF5,0xF5,0xF5));
        dateLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xE0,0xE0,0xE0),1,true),BorderFactory.createEmptyBorder(4,10,4,10)));
        timeLabel=new JLabel();timeLabel.setFont(new Font("Segoe UI",Font.PLAIN,13));timeLabel.setForeground(DARK);
        timeLabel.setIcon(miniIcon(1));timeLabel.setIconTextGap(6);
        timeLabel.setOpaque(true);timeLabel.setBackground(new Color(0xF5,0xF5,0xF5));
        timeLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xE0,0xE0,0xE0),1,true),BorderFactory.createEmptyBorder(4,10,4,10)));
        clockPanel.add(dateLabel);clockPanel.add(timeLabel);
        updateClock();
        tb.add(wl,BorderLayout.WEST); tb.add(clockPanel,BorderLayout.EAST);
        return tb;
    }

    private void updateClock() {
        LocalDateTime now=LocalDateTime.now();
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")));
        timeLabel.setText(now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
    }

    private void selectNav(int idx) {
        selectedIndex=idx;
        for(NavButton b:navButtons) b.setSelected(b.index==idx);
        contentPanel.removeAll();
        switch(idx){
            case 0: contentPanel.add(dashPanel); break;
            case 1: contentPanel.add(dashPanel.createMedicinesPage()); break;
            case 4: contentPanel.add(dashPanel.createStockAlertsPage()); break;
            default: contentPanel.add(createPlaceholder(navLabels[idx],idx)); break;
        }
        contentPanel.revalidate(); contentPanel.repaint();
    }

    private JPanel createPlaceholder(String name,int iconType) {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false);
        JPanel in=new JPanel(); in.setLayout(new BoxLayout(in,BoxLayout.Y_AXIS)); in.setOpaque(false);
        JLabel il=new JLabel(makeNavIcon(iconType,PINK)); il.setAlignmentX(0.5f);
        JLabel nl=new JLabel(name); nl.setFont(new Font("Segoe UI",Font.BOLD,24)); nl.setForeground(PINK); nl.setAlignmentX(0.5f);
        JLabel sl=new JLabel("Coming Soon"); sl.setFont(new Font("Segoe UI",Font.PLAIN,14)); sl.setForeground(GRAY); sl.setAlignmentX(0.5f);
        in.add(il); in.add(Box.createVerticalStrut(12)); in.add(nl); in.add(Box.createVerticalStrut(6)); in.add(sl);
        p.add(in); return p;
    }

    class NavButton extends JButton {
        final int index; boolean isSel,isHov; Color hoverColor=PINK_HL;
        NavButton(String text,int index,Icon icon){
            super("  "+text); this.index=index; isSel=(index==0);
            setIcon(icon); setIconTextGap(8);
            setFont(isSel?FNAVB:FNAV); setForeground(isSel?PINK:SIDEBAR_TXT);
            setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(200,44)); setPreferredSize(new Dimension(200,44));
            setBorder(BorderFactory.createEmptyBorder(0,20,0,20));
            addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){isHov=true;if(!isSel)setForeground(PINK);repaint();}
                public void mouseExited(MouseEvent e){isHov=false;if(!isSel)setForeground(SIDEBAR_TXT);repaint();}
            });
        }
        public void setSelected(boolean s){isSel=s;setFont(s?FNAVB:FNAV);setForeground(s?PINK:SIDEBAR_TXT);repaint();}
        protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(isSel){g2.setColor(PINK_HL);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),12,12));}
            else if(isHov){g2.setColor(hoverColor);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),12,12));}
            g2.dispose(); super.paintComponent(g);
        }
    }
}
