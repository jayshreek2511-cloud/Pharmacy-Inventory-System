import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PharmacyInventorySystem extends JFrame {
    static final Color PINK=new Color(0xE9,0x1E,0x63), PINK_BG=new Color(0xFF,0xF0,0xF3);
    static final Color PINK_HL=new Color(0xFC,0xE4,0xEC), SIDEBAR_TXT=new Color(0x88,0x00,0x3B);
    static final Color WHITE=Color.WHITE, DARK=new Color(0x21,0x21,0x21), GRAY=new Color(0x75,0x75,0x75);
    static final Color DIVIDER=new Color(0xF0,0xD0,0xD8), LOGOUT_RED=new Color(0xD3,0x2F,0x2F);
    static final Font FNAV=new Font("Segoe UI",Font.PLAIN,14), FNAVB=new Font("Segoe UI",Font.BOLD,14);
    static final Font FBRAND=new Font("Segoe UI",Font.BOLD,20), FSUB=new Font("Segoe UI",Font.PLAIN,12);
    private String[] navLabels={"Dashboard","Medicines","Billing","Purchases","Stock Alerts","Reports","Users","Settings"};
    private String[] navIcons={"📊","💊","🧾","🛒","⚠️","📈","👥","⚙️"};
    private int selectedIndex=0;
    private java.util.List<NavButton> navButtons=new java.util.ArrayList<>();
    private JLabel clockLabel;
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
        JLabel footer=new JLabel("💗 Pharmacy Inventory System | All rights reserved © 2025",SwingConstants.CENTER);
        footer.setFont(FSUB); footer.setForeground(GRAY);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,new Color(0xEE,0xEE,0xEE)),
            BorderFactory.createEmptyBorder(8,0,8,0)));
        footer.setOpaque(true); footer.setBackground(WHITE);
        center.add(footer,BorderLayout.SOUTH);
        root.add(center,BorderLayout.CENTER);
        setContentPane(root);
        Timer t=new Timer(1000,e->updateClock()); t.setInitialDelay(0); t.start();
        setVisible(true);
    }

    private JPanel createSidebar() {
        JPanel sb=new JPanel(); sb.setLayout(new BoxLayout(sb,BoxLayout.Y_AXIS));
        sb.setPreferredSize(new Dimension(220,800)); sb.setMinimumSize(new Dimension(220,800));
        sb.setMaximumSize(new Dimension(220,Integer.MAX_VALUE));
        sb.setBackground(PINK_BG);
        sb.setBorder(BorderFactory.createMatteBorder(0,0,0,1,DIVIDER));
        JPanel bp=new JPanel(); bp.setLayout(new BoxLayout(bp,BoxLayout.Y_AXIS));
        bp.setOpaque(false); bp.setBorder(BorderFactory.createEmptyBorder(28,20,18,20));
        JLabel ic=new JLabel("💊"); ic.setFont(new Font("Segoe UI Emoji",Font.PLAIN,36)); ic.setAlignmentX(0f);
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
            NavButton b=new NavButton(navIcons[i]+"   "+navLabels[i],i);
            int idx=i; b.addActionListener(e->selectNav(idx));
            navButtons.add(b); np.add(b); np.add(Box.createVerticalStrut(4));
        }
        sb.add(np); sb.add(Box.createVerticalGlue());
        JPanel lp=new JPanel(); lp.setLayout(new BoxLayout(lp,BoxLayout.X_AXIS));
        lp.setOpaque(false); lp.setBorder(BorderFactory.createEmptyBorder(0,10,24,10));
        lp.setMaximumSize(new Dimension(220,48));
        NavButton lb=new NavButton("🚪   Logout",-1);
        lb.setForeground(LOGOUT_RED); lb.hoverColor=new Color(0xFF,0xEB,0xEE);
        lb.addActionListener(e->{
            if(JOptionPane.showConfirmDialog(this,"Are you sure you want to logout?","Logout",
                JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) System.exit(0);
        });
        lp.add(lb); sb.add(lp);
        return sb;
    }

    private JPanel createTopBar() {
        JPanel tb=new JPanel(new BorderLayout()); tb.setBackground(WHITE);
        tb.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(0xEE,0xEE,0xEE)),
            BorderFactory.createEmptyBorder(18,28,18,28)));
        JLabel wl=new JLabel("Welcome, Admin 💊");
        wl.setFont(new Font("Segoe UI",Font.BOLD,20)); wl.setForeground(DARK);
        clockLabel=new JLabel(); clockLabel.setFont(new Font("Segoe UI",Font.PLAIN,14));
        clockLabel.setForeground(GRAY); updateClock();
        tb.add(wl,BorderLayout.WEST); tb.add(clockLabel,BorderLayout.EAST);
        return tb;
    }

    private void updateClock() {
        clockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy  •  hh:mm:ss a")));
    }

    private void selectNav(int idx) {
        selectedIndex=idx;
        for(NavButton b:navButtons) b.setSelected(b.index==idx);
        contentPanel.removeAll();
        switch(idx){
            case 0: contentPanel.add(dashPanel); break;
            case 1: contentPanel.add(dashPanel.createMedicinesPage()); break;
            case 4: contentPanel.add(dashPanel.createStockAlertsPage()); break;
            default: contentPanel.add(createPlaceholder(navLabels[idx],navIcons[idx])); break;
        }
        contentPanel.revalidate(); contentPanel.repaint();
    }

    private JPanel createPlaceholder(String name,String icon) {
        JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false);
        JPanel in=new JPanel(); in.setLayout(new BoxLayout(in,BoxLayout.Y_AXIS)); in.setOpaque(false);
        JLabel il=new JLabel(icon); il.setFont(new Font("Segoe UI Emoji",Font.PLAIN,56)); il.setAlignmentX(0.5f);
        JLabel nl=new JLabel(name); nl.setFont(new Font("Segoe UI",Font.BOLD,24)); nl.setForeground(PINK); nl.setAlignmentX(0.5f);
        JLabel sl=new JLabel("Coming Soon"); sl.setFont(new Font("Segoe UI",Font.PLAIN,14)); sl.setForeground(GRAY); sl.setAlignmentX(0.5f);
        in.add(il); in.add(Box.createVerticalStrut(12)); in.add(nl); in.add(Box.createVerticalStrut(6)); in.add(sl);
        p.add(in); return p;
    }

    class NavButton extends JButton {
        final int index; boolean isSel,isHov; Color hoverColor=PINK_HL;
        NavButton(String text,int index){
            super(text); this.index=index; isSel=(index==0);
            setFont(isSel?FNAVB:FNAV); setForeground(isSel?PINK:SIDEBAR_TXT);
            setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(200,44)); setPreferredSize(new Dimension(200,44));
            setBorder(BorderFactory.createEmptyBorder(0,14,0,14));
            addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){isHov=true;if(!isSel)setForeground(PINK);repaint();}
                public void mouseExited(MouseEvent e){isHov=false;if(!isSel)setForeground(SIDEBAR_TXT);repaint();}
            });
        }
        public void setSelected(boolean s){isSel=s;setFont(s?FNAVB:FNAV);setForeground(s?PINK:SIDEBAR_TXT);repaint();}
        protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(isSel){g2.setColor(PINK_HL);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),14,14));}
            else if(isHov){g2.setColor(hoverColor);g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),14,14));}
            g2.dispose(); super.paintComponent(g);
        }
    }
}
