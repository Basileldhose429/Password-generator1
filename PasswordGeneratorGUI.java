import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;

public class PasswordGeneratorGUI extends JFrame {

    private static final String LOWERCASE =
            "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL =
            "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/password_generator";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "your_mysql_password";

    private final SecureRandom random = new SecureRandom();

    private JTextField passwordField;
    private JSpinner lengthSpinner;
    private JCheckBox upperCheckBox;
    private JCheckBox lowerCheckBox;
    private JCheckBox digitsCheckBox;
    private JCheckBox specialCheckBox;
    private JLabel strengthLabel;

    public PasswordGeneratorGUI() {
        setTitle("Java Password Generator");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
        initializeDatabase();
    }

    private Connection connectDatabase() throws SQLException {
        return DriverManager.getConnection(
                DB_URL, DB_USER, DB_PASSWORD
        );
    }

    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS passwords ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "password VARCHAR(64) NOT NULL, "
                + "length INT NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";

        try (Connection con = connectDatabase();
             Statement stmt = con.createStatement()) {

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "MySQL connection failed!\n"
                            + e.getMessage()
                            + "\n\nCheck MySQL is running, "
                            + "credentials, and database setup.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        passwordField = new JTextField();
        passwordField.setFont(
                new Font("Monospaced", Font.BOLD, 16)
        );
        passwordField.setEditable(false);
        passwordField.setBackground(Color.WHITE);

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> copyToClipboard());

        topPanel.add(passwordField, BorderLayout.CENTER);
        topPanel.add(copyButton, BorderLayout.EAST);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(5, 2, 5, 5));
        optionsPanel.setBorder(
                BorderFactory.createTitledBorder("Options")
        );

        optionsPanel.add(new JLabel("Password Length:"));

        lengthSpinner = new JSpinner(
                new SpinnerNumberModel(12, 4, 64, 1)
        );
        optionsPanel.add(lengthSpinner);

        lowerCheckBox =
                new JCheckBox("Include Lowercase (a-z)", true);
        upperCheckBox =
                new JCheckBox("Include Uppercase (A-Z)", true);
        digitsCheckBox =
                new JCheckBox("Include Numbers (0-9)", true);
        specialCheckBox =
                new JCheckBox("Include Symbols (!@#$)", true);

        optionsPanel.add(lowerCheckBox);
        optionsPanel.add(upperCheckBox);
        optionsPanel.add(digitsCheckBox);
        optionsPanel.add(specialCheckBox);

        optionsPanel.add(new JLabel("Strength:"));

        strengthLabel = new JLabel("N/A");
        strengthLabel.setFont(
                new Font("SansSerif", Font.BOLD, 12)
        );
        optionsPanel.add(strengthLabel);

        JButton generateButton =
                new JButton("Generate Password");
        generateButton.setFont(
                new Font("SansSerif", Font.BOLD, 14)
        );
        generateButton.addActionListener(e -> generatePassword());

        JButton saveButton = new JButton("Save Password");
        saveButton.addActionListener(e -> savePassword());

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(generateButton);
        bottomPanel.add(saveButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(optionsPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void generatePassword() {
        int length = (Integer) lengthSpinner.getValue();

        StringBuilder allowedChars = new StringBuilder();

        if (lowerCheckBox.isSelected()) {
            allowedChars.append(LOWERCASE);
        }
        if (upperCheckBox.isSelected()) {
            allowedChars.append(UPPERCASE);
        }
        if (digitsCheckBox.isSelected()) {
            allowedChars.append(DIGITS);
        }
        if (specialCheckBox.isSelected()) {
            allowedChars.append(SPECIAL);
        }

        if (allowedChars.length() == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select at least one character type!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(allowedChars.length());
            password.append(allowedChars.charAt(index));
        }

        passwordField.setText(password.toString());
        updateStrengthIndicator(length);
    }

    private void updateStrengthIndicator(int length) {
        int selectedTypes = 0;

        if (lowerCheckBox.isSelected()) selectedTypes++;
        if (upperCheckBox.isSelected()) selectedTypes++;
        if (digitsCheckBox.isSelected()) selectedTypes++;
        if (specialCheckBox.isSelected()) selectedTypes++;

        if (length < 8 || selectedTypes < 2) {
            strengthLabel.setText("WEAK");
            strengthLabel.setForeground(Color.RED);
        } else if (length < 12 || selectedTypes < 3) {
            strengthLabel.setText("MEDIUM");
            strengthLabel.setForeground(Color.ORANGE);
        } else {
            strengthLabel.setText("STRONG");
            strengthLabel.setForeground(new Color(0, 150, 0));
        }
    }

    private void savePassword() {
        String pwd = passwordField.getText();

        if (pwd.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Generate a password before saving!",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String sql =
                "INSERT INTO passwords (password, length) VALUES (?, ?)";

        try (Connection con = connectDatabase();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, pwd);
            pstmt.setInt(2, pwd.length());

            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(
                    this,
                    "Password saved successfully to MySQL!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save password:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void copyToClipboard() {
        String pwd = passwordField.getText();

        if (pwd.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No password to copy!",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        StringSelection selection = new StringSelection(pwd);
        Clipboard clipboard =
                Toolkit.getDefaultToolkit().getSystemClipboard();

        clipboard.setContents(selection, null);

        JOptionPane.showMessageDialog(
                this,
                "Password copied to clipboard!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            new PasswordGeneratorGUI().setVisible(true);
        });
    }
}
