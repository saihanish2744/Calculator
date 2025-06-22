package com.gqt.core_java.projects;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class StandardCalculator extends JFrame {

    // --- Color Palettes for Theming ---
    // Dark Theme
    private static final Color DARK_BG = new Color(45, 45, 45);
    private static final Color DARK_DISPLAY_BG = new Color(60, 60, 60);
    private static final Color DARK_TEXT_COLOR = new Color(240, 240, 240);
    private static final Color DARK_SPECIAL_BTN_BG = new Color(80, 80, 80);
    // Light Theme
    private static final Color LIGHT_BG = new Color(245, 245, 245);
    private static final Color LIGHT_DISPLAY_BG = new Color(255, 255, 255);
    private static final Color LIGHT_TEXT_COLOR = new Color(20, 20, 20);
    private static final Color LIGHT_SPECIAL_BTN_BG = new Color(220, 220, 220);
    // Universal Accent
    private static final Color ACCENT_ORANGE = new Color(255, 159, 10);

    // --- State Variables ---
    private String currentInput = "";
    private String currentExpression = "";
    private Object result = null;
    private double memory = 0.0;
    private final List<String> history = new ArrayList<>();

    // --- UI Components ---
    private JLabel expressionLabel;
    private JLabel inputLabel;
    private JDialog historyDialog;
    private JPanel historyPanel;
    private List<JButton> allButtons = new ArrayList<>();
    private List<JPanel> allPanels = new ArrayList<>();

    // --- Engine for Calculation ---
    private final ScriptEngine engine;
    private boolean isLightMode = false;

    public StandardCalculator() {
        engine = new ScriptEngineManager().getEngineByName("JavaScript");
        initUI();
        setupKeyBindings();
        applyTheme(); // Apply initial dark theme
    }

    private void initUI() {
        setTitle("Standard Java Calculator");
        setSize(400, 580);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Display Panel ---
        JPanel displayPanel = new JPanel(new BorderLayout(10, 10));
        displayPanel.setBorder(new EmptyBorder(20, 10, 10, 10));
        allPanels.add(displayPanel);

        expressionLabel = new JLabel(" ", SwingConstants.RIGHT);
        expressionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        inputLabel = new JLabel("0", SwingConstants.RIGHT);
        inputLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));

        JButton historyButton = new JButton("H");
        historyButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        historyButton.setFocusable(false);
        historyButton.addActionListener(e -> toggleHistory());
        allButtons.add(historyButton);

        JPanel topDisplayWrapper = new JPanel(new BorderLayout());
        topDisplayWrapper.add(expressionLabel, BorderLayout.NORTH);
        topDisplayWrapper.add(inputLabel, BorderLayout.CENTER);
        allPanels.add(topDisplayWrapper);

        displayPanel.add(topDisplayWrapper, BorderLayout.CENTER);
        displayPanel.add(historyButton, BorderLayout.EAST);
        add(displayPanel, BorderLayout.NORTH);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        allPanels.add(buttonPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.insets = new Insets(4, 4, 4, 4);

        String[][] buttonLayout = {
            {"MC", "M+", "M-", "MR"},
            {"C", "CE", "%", "/"},
            {"7", "8", "9", "*"},
            {"4", "5", "6", "-"},
            {"1", "2", "3", "+"},
            {"0", ".", "="}
        };

        for (int row = 0; row < buttonLayout.length; row++) {
            for (int col = 0; col < buttonLayout[row].length; col++) {
                String text = buttonLayout[row][col];
                gbc.gridx = col;
                gbc.gridy = row;
                gbc.gridwidth = 1;

                if ("0".equals(text)) { gbc.gridwidth = 2; }
                if (".".equals(text)) { gbc.gridx = 2; }
                if ("=".equals(text)) { gbc.gridx = 3; }

                JButton button = new JButton(text);
                button.setFont(new Font("Segoe UI", Font.BOLD, 18));
                button.addActionListener(this::handleButtonPress);
                button.setFocusable(false);
                button.setBorder(BorderFactory.createEtchedBorder());
                allButtons.add(button); // Add to list for theming
                buttonPanel.add(button, gbc);

                if ("0".equals(text)) { col++; }
            }
        }
        add(buttonPanel, BorderLayout.CENTER);
    }

    private void applyTheme() {
        Color bgColor = isLightMode ? LIGHT_BG : DARK_BG;
        Color displayBg = isLightMode ? LIGHT_DISPLAY_BG : DARK_DISPLAY_BG;
        Color textColor = isLightMode ? LIGHT_TEXT_COLOR : DARK_TEXT_COLOR;
        Color specialBtnBg = isLightMode ? LIGHT_SPECIAL_BTN_BG : DARK_SPECIAL_BTN_BG;

        // Apply to frame and panels
        getContentPane().setBackground(bgColor);
        for(JPanel panel : allPanels) {
            panel.setBackground(displayBg);
            // The main button grid should match the window background
            if(panel.getLayout() instanceof GridBagLayout) {
                panel.setBackground(bgColor);
            }
        }

        // Apply to labels
        inputLabel.setForeground(textColor);
        expressionLabel.setForeground(textColor.darker());

        // Apply to buttons
        for (JButton button : allButtons) {
            String text = button.getText();
            button.setForeground(textColor);
            if ("/*-+=C".contains(text)) {
                button.setBackground(ACCENT_ORANGE);
                button.setForeground(Color.WHITE);
            } else if ("MC M+ M- MR % H".contains(text)) {
                button.setBackground(specialBtnBg);
            } else { // Digit buttons
                button.setBackground(displayBg);
            }
        }
        
        // Also update the history dialog if it's open
        if (historyDialog != null && historyDialog.isVisible()) {
            SwingUtilities.updateComponentTreeUI(historyDialog);
            historyDialog.getContentPane().setBackground(bgColor);
            historyPanel.setBackground(displayBg);
            for(Component comp : historyPanel.getComponents()) {
                comp.setForeground(textColor);
            }
        }
    }

    // --- Logic for Handling Input ---

    private void handleButtonPress(ActionEvent e) {
        String command = ((JButton) e.getSource()).getText();
        processCommand(command);
    }
    
    private void setupKeyBindings() {
        JRootPane rootPane = this.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        class KeyAction extends AbstractAction {
            private final String command;
            public KeyAction(String command) { this.command = command; }
            @Override public void actionPerformed(ActionEvent e) { processCommand(command); }
        }

        for (char i = '0'; i <= '9'; i++) {
            inputMap.put(KeyStroke.getKeyStroke(i), String.valueOf(i));
            actionMap.put(String.valueOf(i), new KeyAction(String.valueOf(i)));
        }
        inputMap.put(KeyStroke.getKeyStroke('.'), "."); actionMap.put(".", new KeyAction("."));
        inputMap.put(KeyStroke.getKeyStroke('/'), "/"); actionMap.put("/", new KeyAction("/"));
        inputMap.put(KeyStroke.getKeyStroke('*'), "*"); actionMap.put("*", new KeyAction("*"));
        inputMap.put(KeyStroke.getKeyStroke('+'), "+"); actionMap.put("+", new KeyAction("+"));
        inputMap.put(KeyStroke.getKeyStroke('-'), "-"); actionMap.put("-", new KeyAction("-"));
        inputMap.put(KeyStroke.getKeyStroke('%'), "%"); actionMap.put("%", new KeyAction("%"));
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "="); actionMap.put("=", new KeyAction("="));
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "CE"); actionMap.put("CE", new KeyAction("CE"));
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "C"); actionMap.put("C", new KeyAction("C"));
    }

    private void processCommand(String command) {
        if ("0123456789.".contains(command)) {
            handleDigit(command);
        } else if ("/*-+".contains(command)) {
            handleOperator(command);
        } else if ("%".equals(command)) {
            handlePercent();
        } else if ("=".equals(command)) {
            handleEquals();
        } else if ("C".equals(command)) {
            clearAll();
        } else if ("CE".equals(command)) {
            clearEntry();
        } else if (command.startsWith("M")) {
            handleMemory(command);
        }
        updateDisplay();
    }
    
    // The core calculation logic remains identical
    private void handleDigit(String digit) { if (result != null) { currentInput = ""; result = null; } if (digit.equals(".") && currentInput.contains(".")) return; currentInput += digit; }
    private void handleOperator(String op) { if (currentInput.isEmpty() && result != null) { currentExpression = result + " " + op + " "; } else if (!currentInput.isEmpty()) { currentExpression += currentInput + " " + op + " "; } currentInput = ""; result = null; }
    private void handleEquals() { if (currentInput.isEmpty() && currentExpression.isEmpty()) return; String fullExpr = (currentExpression + currentInput).trim(); try { result = engine.eval(fullExpr); history.add(fullExpr + " = " + result); currentExpression = ""; currentInput = ""; } catch (ScriptException e) { result = "Error"; } }
    private void handlePercent() { if (currentInput.isEmpty()) return; try { double value = Double.parseDouble(currentInput) / 100.0; currentInput = String.valueOf(value); } catch (NumberFormatException ignored) {} }
    private void clearAll() { currentInput = ""; currentExpression = ""; result = null; }
    private void clearEntry() { if (!currentInput.isEmpty()) { currentInput = currentInput.substring(0, currentInput.length() - 1); } else if (result != null) { clearAll(); } }
    private void handleMemory(String op) { double value = 0; try { if (!currentInput.isEmpty()) value = Double.parseDouble(currentInput); else if (result != null) value = Double.parseDouble(result.toString()); } catch (NumberFormatException | NullPointerException e) { return; } switch (op) { case "MC": memory = 0; break; case "M+": memory += value; break; case "M-": memory -= value; break; case "MR": currentInput = String.valueOf(memory); if (currentInput.endsWith(".0")) { currentInput = currentInput.substring(0, currentInput.length() - 2); } result = null; break; } }
    private void updateDisplay() { expressionLabel.setText(currentExpression); if (result != null) { String resText = result.toString(); if (resText.endsWith(".0")) resText = resText.substring(0, resText.length() - 2); inputLabel.setText(resText); } else if (currentInput.isEmpty()) { inputLabel.setText("0"); } else { inputLabel.setText(currentInput); } }

    private void toggleHistory() {
        if (historyDialog == null) {
            historyDialog = new JDialog(this, "History", false);
            historyDialog.setSize(300, 400);
            historyDialog.setLayout(new BorderLayout(10, 10));
            historyDialog.getRootPane().setBorder(new EmptyBorder(10,10,10,10));

            historyPanel = new JPanel();
            historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
            JScrollPane scrollPane = new JScrollPane(historyPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 10));
            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(e -> { history.clear(); updateHistoryPanel(); });
            
            JToggleButton themeToggle = new JToggleButton("Light Mode");
            themeToggle.addActionListener(e -> {
                isLightMode = themeToggle.isSelected();
                applyTheme(); // Apply theme to main window and history dialog
            });
            allButtons.add(clearButton); // Add for theming
            bottomPanel.add(clearButton);
            bottomPanel.add(themeToggle); // JToggleButton themes well enough by default
            
            historyDialog.add(scrollPane, BorderLayout.CENTER);
            historyDialog.add(bottomPanel, BorderLayout.SOUTH);
        }
        updateHistoryPanel();
        historyDialog.setLocationRelativeTo(this);
        historyDialog.setVisible(!historyDialog.isVisible());
        applyTheme(); // Ensure dialog colors are correct when opened
    }

    private void updateHistoryPanel() {
        historyPanel.removeAll();
        for (int i = history.size() - 1; i >= 0; i--) {
            JLabel entryLabel = new JLabel("  " + history.get(i));
            entryLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
            historyPanel.add(entryLabel);
        }
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    public static void main(String[] args) {
        // Set a modern-ish Look and Feel if available (Nimbus)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, fall back to the default L&F
        }

        SwingUtilities.invokeLater(() -> {
            StandardCalculator calculator = new StandardCalculator();
            calculator.setVisible(true);
        });
    }
}