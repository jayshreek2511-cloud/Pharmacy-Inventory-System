import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class LoginFrame extends JFrame {
    private static final Color PINK = new Color(0xC9, 0x63, 0x7A);
    private static final Color PINK_DARK = new Color(0xA8, 0x4E, 0x63);
    private static final Color PINK_BG = new Color(0xFF, 0xF0, 0xF3);
    private static final Color BORDER = new Color(0xED, 0xD5, 0xDC);
    private static final Color GRAY = new Color(0x75, 0x75, 0x75);
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private final Runnable onLoginSuccess;
    private RoundedPanel formCard;
    private PlaceholderTextField usernameField;
    private PlaceholderPasswordField passwordField;
    private JButton loginButton;
    private JLabel errorLabel;
    private int failedAttempts = 0;
    private Timer lockoutTimer;
    private boolean passwordVisible = false;

    public LoginFrame(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        setTitle("Pharmacy Login");
        setSize(420, 550);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setContentPane(createRoot());
        setVisible(true);
    }

    private JPanel createRoot() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(PINK_BG);
        root.setBorder(BorderFactory.createEmptyBorder(34, 34, 28, 34));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel icon = new JLabel(new PillIcon());
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel brand = new JLabel("PHARMACY");
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 24));
        brand.setForeground(PINK);
        JLabel sub = new JLabel("INVENTORY SYSTEM");
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(GRAY);

        top.add(icon);
        top.add(Box.createVerticalStrut(8));
        top.add(brand);
        top.add(Box.createVerticalStrut(2));
        top.add(sub);
        top.add(Box.createVerticalStrut(20));

        formCard = new RoundedPanel(16, Color.WHITE, true);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        formCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        formCard.setMaximumSize(new Dimension(340, 330));

        JLabel title = new JLabel("Welcome Back");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PINK);

        JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(GRAY);

        usernameField = new PlaceholderTextField("Username");
        JPanel passwordPanel = createPasswordField();

        JCheckBox remember = new JCheckBox("Remember Me");
        remember.setOpaque(false);
        remember.setFocusPainted(false);
        remember.setFont(FONT);
        remember.setForeground(new Color(0x55, 0x55, 0x55));
        remember.setIcon(new CheckIcon(false));
        remember.setSelectedIcon(new CheckIcon(true));
        remember.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginButton = new RoundedButton("Login");
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> attemptLogin());

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(new Color(0xD3, 0x2F, 0x2F));
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton forgot = new LinkButton("Forgot Password?");
        forgot.setAlignmentX(Component.CENTER_ALIGNMENT);
        forgot.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Contact admin@pharmacy.com", "Forgot Password", JOptionPane.INFORMATION_MESSAGE));

        formCard.add(title);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(subtitle);
        formCard.add(Box.createVerticalStrut(22));
        formCard.add(usernameField);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(passwordPanel);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(remember);
        formCard.add(Box.createVerticalStrut(16));
        formCard.add(loginButton);
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(errorLabel);
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(forgot);

        root.add(top);
        root.add(formCard);
        root.add(Box.createVerticalGlue());
        getRootPane().setDefaultButton(loginButton);
        return root;
    }

    private JPanel createPasswordField() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new RoundedBorder(BORDER, 10, 1));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        panel.setPreferredSize(new Dimension(280, 42));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new PlaceholderPasswordField("Password");
        passwordField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
        passwordField.setEchoChar('\u2022');

        JButton toggle = new JButton(new EyeIcon(false));
        toggle.setRolloverIcon(new EyeIcon(true));
        toggle.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 10));
        toggle.setContentAreaFilled(false);
        toggle.setFocusPainted(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> {
            passwordVisible = !passwordVisible;
            passwordField.setEchoChar(passwordVisible ? (char) 0 : '\u2022');
            toggle.setIcon(new EyeIcon(passwordVisible));
        });

        panel.add(passwordField, BorderLayout.CENTER);
        panel.add(toggle, BorderLayout.EAST);
        return panel;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        if (username.isEmpty() || password.length == 0) {
            failedAttempts++;
            shakeCard();
            errorLabel.setText("Enter username and password");
            if (failedAttempts >= 3) {
                startLockout();
            }
            return;
        }

        DatabaseManager.UserAccount user = DatabaseManager.authenticateUser(username, password);
        java.util.Arrays.fill(password, '\0');
        if (user != null) {
            dispose();
            onLoginSuccess.run();
            return;
        }

        failedAttempts++;
        shakeCard();
        errorLabel.setText("Invalid username or password");
        if (failedAttempts >= 3) {
            startLockout();
        }
    }

    private void shakeCard() {
        final Point origin = formCard.getLocation();
        final int[] offsets = {0, -10, 10, -10, 10, -6, 6, 0};
        final int[] i = {0};
        Timer timer = new Timer(35, null);
        timer.addActionListener(e -> {
            formCard.setLocation(origin.x + offsets[i[0]], origin.y);
            i[0]++;
            if (i[0] == offsets.length) {
                timer.stop();
                formCard.setLocation(origin);
            }
        });
        timer.start();
    }

    private void startLockout() {
        loginButton.setEnabled(false);
        final int[] seconds = {30};
        errorLabel.setText("Try again in 30s...");
        lockoutTimer = new Timer(1000, e -> {
            seconds[0]--;
            if (seconds[0] <= 0) {
                lockoutTimer.stop();
                failedAttempts = 0;
                loginButton.setEnabled(true);
                loginButton.setText("Login");
                errorLabel.setText(" ");
            } else {
                errorLabel.setText("Try again in " + seconds[0] + "s...");
            }
        });
        lockoutTimer.start();
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;
        private final boolean shadow;

        RoundedPanel(int radius, Color bg, boolean shadow) {
            this.radius = radius;
            this.bg = bg;
            this.shadow = shadow;
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (shadow) {
                g2.setColor(new Color(0, 0, 0, 25));
                g2.fillRoundRect(5, 7, getWidth() - 10, getHeight() - 10, radius, radius);
            }
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 10, getHeight() - 10, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final int thickness;

        RoundedBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            setFont(FONT);
            setOpaque(false);
            setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(BORDER, 10, 1),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setPreferredSize(new Dimension(280, 42));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                drawPlaceholder(g, this, placeholder);
            }
        }
    }

    private static class PlaceholderPasswordField extends JPasswordField {
        private final String placeholder;

        PlaceholderPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setFont(FONT);
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && !isFocusOwner()) {
                drawPlaceholder(g, this, placeholder);
            }
        }
    }

    private static void drawPlaceholder(Graphics g, JComponent c, String text) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0x99, 0x99, 0x99));
        g2.setFont(FONT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, 10, (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
    }

    private static class RoundedButton extends JButton {
        RoundedButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setBackground(PINK);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setPreferredSize(new Dimension(280, 42));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) setBackground(PINK_DARK);
                }

                public void mouseExited(MouseEvent e) {
                    setBackground(PINK);
                }
            });
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isEnabled() ? getBackground() : new Color(0xD9, 0xA6, 0xB4));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class LinkButton extends JButton {
        LinkButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setForeground(PINK);
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private static class PillIcon implements Icon {
        public int getIconWidth() { return 40; }
        public int getIconHeight() { return 40; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.rotate(-Math.PI / 4, 20, 20);
            g2.setColor(PINK);
            g2.fillRoundRect(7, 13, 26, 14, 14, 14);
            g2.setColor(Color.WHITE);
            g2.drawLine(20, 13, 20, 27);
            g2.dispose();
        }
    }

    private static class EyeIcon implements Icon {
        private final boolean active;

        EyeIcon(boolean active) {
            this.active = active;
        }

        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(active ? PINK : GRAY);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.translate(x, y);
            Path2D eye = new Path2D.Double();
            eye.moveTo(2, 10);
            eye.curveTo(6, 4, 14, 4, 18, 10);
            eye.curveTo(14, 16, 6, 16, 2, 10);
            g2.draw(eye);
            g2.drawOval(7, 7, 6, 6);
            if (!active) {
                g2.drawLine(4, 17, 17, 3);
            }
            g2.dispose();
        }
    }

    private static class CheckIcon implements Icon {
        private final boolean selected;

        CheckIcon(boolean selected) {
            this.selected = selected;
        }

        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(selected ? PINK : Color.WHITE);
            g2.fillRoundRect(1, 1, 15, 15, 5, 5);
            g2.setColor(selected ? PINK : BORDER);
            g2.drawRoundRect(1, 1, 15, 15, 5, 5);
            if (selected) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(5, 9, 8, 12);
                g2.drawLine(8, 12, 13, 6);
            }
            g2.dispose();
        }
    }
}
