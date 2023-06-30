package javavirtualwallet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class JavaVirtualWallet extends JFrame {
    private JTextArea outputArea;
    private VirtualWallet virtualWallet;
    private Connection connection;
    private boolean loggedIn = false;

    // Set up the JFrame
    public JavaVirtualWallet() {
        setTitle("VIRTUAL WALLET");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(Color.ORANGE);
        setContentPane(contentPane);
        
        // Set default font for UI components
        Font defaultFont = new Font("Times New Roman", Font.BOLD, 20);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("TextArea.font", defaultFont);
        
        // Create and configure the button panel
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(Color.ORANGE);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 10, 10, 10);

        JButton registerButton = new JButton("Register");
        JButton loginButton = new JButton("Login");
        
        // Set button colors
        registerButton.setBackground(Color.BLACK);
        registerButton.setForeground(Color.WHITE);
        loginButton.setBackground(Color.BLACK);
        loginButton.setForeground(Color.WHITE);
        
        // Add action listeners to buttons
        registerButton.addActionListener(e -> registerAccount());
        loginButton.addActionListener(e -> login());
        
        // Set button sizes
        Dimension buttonSize = new Dimension(250, 50);
        registerButton.setPreferredSize(buttonSize);
        loginButton.setPreferredSize(buttonSize);

        // Add buttons to the button panel
        constraints.gridx = 0;
        constraints.gridy = 0;
        buttonPanel.add(loginButton, constraints);

        constraints.gridy = 1;
        buttonPanel.add(registerButton, constraints);

        // Add the button panel and output area to the content pane
        contentPane.add(buttonPanel, BorderLayout.CENTER);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.SOUTH);
    }
            // Method for registering a new account
            public void registerAccount() {
                // Prompt the user for account details
                String accountNumString = JOptionPane.showInputDialog("Enter your account number:");
                int accountNum = Integer.parseInt(accountNumString);
                String pinString = JOptionPane.showInputDialog("Enter your PIN:");
                int pin = Integer.parseInt(pinString);
                String balanceString = JOptionPane.showInputDialog("Enter your initial balance:");
                double balance = Double.parseDouble(balanceString);

                try { // Connect to the database
                    String url = "jdbc:mysql://localhost:3306/walletdb";
                    String username = "root";
                    String password = "";
                    connection = DriverManager.getConnection(url, username, password);
                    System.out.println("Connected to the database");

                    // Insert the account details into the database
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO accounts (account_number, pin, balance) VALUES (?, ?, ?)");
                    statement.setInt(1, accountNum);
                    statement.setInt(2, pin);
                    statement.setDouble(3, balance);
                    statement.executeUpdate();
                    
                    // Create a new virtual wallet object
                    virtualWallet = new VirtualWallet(accountNum, pin, balance);

                    // Update the output area and switch to the menu panel
                    outputArea.append("Account registered successfully!\n");
                    outputArea.append("Hello, welcome to VIRTUAL WALLET:)\n");

                    remove(getContentPane().getComponent(0)); // Remove the button panel

                    createMenuPanel();

                    revalidate();
                    repaint();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            
            // Method for logging into an existing account
            public void login() {
                // Prompt the user for account details
                String accountNumString = JOptionPane.showInputDialog("Enter your account number:");
                int accountNum = Integer.parseInt(accountNumString);
                String pinString = JOptionPane.showInputDialog("Enter your PIN:");
                int pin = Integer.parseInt(pinString);

                try { // Connect to the database
                    String url = "jdbc:mysql://localhost:3306/walletdb";
                    String username = "root";
                    String password = "";
                    connection = DriverManager.getConnection(url, username, password);
                    System.out.println("Connected to the database");
                    
                    // Retrieve the account details from the database
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT * FROM accounts WHERE account_number = ? AND pin = ?");
                    statement.setInt(1, accountNum);
                    statement.setInt(2, pin);
                    ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        // If account details are valid, create a virtual wallet object and switch to the menu panel
                        double balance = resultSet.getDouble("balance");

                        virtualWallet = new VirtualWallet(accountNum, pin, balance);
                        loggedIn = true;
                        outputArea.append("Login successful!\n");
                        outputArea.append("Hello, welcome to VIRTUAL WALLET:)\n");

                        remove(getContentPane().getComponent(0)); // Remove the button panel

                        createMenuPanel();

                        revalidate();
                        repaint();
                    } else {
                        outputArea.append("Invalid account number or PIN. Please try again.\n");
                    }

                    resultSet.close();
                    statement.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            
            // Method for performing a transfer between accounts
            public boolean performTransfer(int senderAccountNumber, int recipientAccountNumber, double transferAmount) {
            try {
                // Check if the recipient account exists
                PreparedStatement checkRecipientStatement = connection.prepareStatement(
                        "SELECT * FROM accounts WHERE account_number = ?");
                checkRecipientStatement.setInt(1, recipientAccountNumber);
                ResultSet recipientResultSet = checkRecipientStatement.executeQuery();

                if (recipientResultSet.next()) {
                    // Update the sender and recipient balances in the database
                    PreparedStatement senderStatement = connection.prepareStatement(
                            "UPDATE accounts SET balance = balance - ? WHERE account_number = ?");
                    senderStatement.setDouble(1, transferAmount);
                    senderStatement.setInt(2, senderAccountNumber);
                    senderStatement.executeUpdate();

                    PreparedStatement recipientStatement = connection.prepareStatement(
                            "UPDATE accounts SET balance = balance + ? WHERE account_number = ?");
                    recipientStatement.setDouble(1, transferAmount);
                    recipientStatement.setInt(2, recipientAccountNumber);
                    recipientStatement.executeUpdate();
                    
                    // Update the virtual wallet balance
                    virtualWallet.setBalance(virtualWallet.getBalance() - transferAmount);

                    PreparedStatement updateBalanceStatement = connection.prepareStatement(
                            "UPDATE accounts SET balance = ? WHERE account_number = ?");
                    updateBalanceStatement.setDouble(1, virtualWallet.getBalance());
                    updateBalanceStatement.setInt(2, senderAccountNumber);
                    updateBalanceStatement.executeUpdate();

                    return true;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            return false;
        }
            
            // Method for creating the menu panel
            public void createMenuPanel() {
            JPanel menuPanel = new JPanel(new GridBagLayout());
            menuPanel.setBackground(Color.ORANGE);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(10, 10, 10, 10); 

            JButton checkBalanceButton = new JButton("Check Balance");
            JButton withdrawButton = new JButton("Withdraw");
            JButton depositButton = new JButton("Deposit");
            JButton transferButton = new JButton("Transfer");
            JButton exitButton = new JButton("Logout");
            
            checkBalanceButton.setBackground(Color.BLACK);
            checkBalanceButton.setForeground(Color.WHITE);
            withdrawButton.setBackground(Color.BLACK);
            withdrawButton.setForeground(Color.WHITE);
            depositButton.setBackground(Color.BLACK);
            depositButton.setForeground(Color.WHITE);
            transferButton.setBackground(Color.BLACK);
            transferButton.setForeground(Color.WHITE);
            exitButton.setBackground(Color.BLACK);
            exitButton.setForeground(Color.WHITE);

            Dimension buttonSize = new Dimension(200, 40);
            checkBalanceButton.setPreferredSize(buttonSize);
            withdrawButton.setPreferredSize(buttonSize);
            depositButton.setPreferredSize(buttonSize);
            transferButton.setPreferredSize(buttonSize);
            exitButton.setPreferredSize(buttonSize);
            
            // ActionListener for the check balance button
            checkBalanceButton.addActionListener(e -> {
                    if (loggedIn) {
                        double balance = virtualWallet.getBalance();
                        outputArea.append("Current balance: $" + balance + "\n");
                    } else {
                        outputArea.append("You are not logged in. Please log in to continue.\n");
                    }
                });

            // ActionListener for the withdraw button
                withdrawButton.addActionListener(e -> {
                    if (loggedIn) {
                        String withdrawAmountString = JOptionPane.showInputDialog("Enter the amount to withdraw:");
                        double withdrawAmount = Double.parseDouble(withdrawAmountString);

                        if (withdrawAmount <= virtualWallet.getBalance()) {
                            virtualWallet.setBalance(virtualWallet.getBalance() - withdrawAmount);
                            outputArea.append("Withdrawal of $" + withdrawAmount + " successful\n");
                        } else {
                            outputArea.append("Insufficient balance for withdrawal\n");
                        }
                    } else {
                        outputArea.append("You are not logged in. Please log in to continue.\n");
                    }
                });
                
                // ActionListener for the deposit button
                depositButton.addActionListener(e -> {
                    if (loggedIn) {
                        String depositAmountString = JOptionPane.showInputDialog("Enter the amount to deposit:");
                        double depositAmount = Double.parseDouble(depositAmountString);

                        virtualWallet.setBalance(virtualWallet.getBalance() + depositAmount);
                        outputArea.append("Deposit of $" + depositAmount + " successful\n");
                    } else {
                        outputArea.append("You are not logged in. Please log in to continue.\n");
                    }
                });
                
                // ActionListener for the transfer button
                transferButton.addActionListener(e -> {
                    if (loggedIn) {
                        String transferAmountString = JOptionPane.showInputDialog("Enter the amount to transfer:");
                        double transferAmount = Double.parseDouble(transferAmountString);

                        if (transferAmount <= virtualWallet.getBalance()) {
                            String recipientAccountString = JOptionPane.showInputDialog("Enter the recipient's account number:");
                            int recipientAccountNumber = Integer.parseInt(recipientAccountString);

                            boolean transferSuccess = performTransfer(virtualWallet.getAccountNumber(), recipientAccountNumber, transferAmount);
                            if (transferSuccess) {
                                outputArea.append("Transfer of $" + transferAmount + " to account " + recipientAccountNumber + " successful\n");
                            } else {
                                outputArea.append("Transfer failed. Please check the recipient's account number.\n");
                            }
                        } else {
                            outputArea.append("Insufficient balance for transfer\n");
                        }
                    } else {
                        outputArea.append("You are not logged in. Please log in to continue.\n");
                    }
                });
                
                // ActionListener for the logout button
                exitButton.addActionListener(e -> System.exit(0));

                // Add buttons to the menu panel
                menuPanel.add(checkBalanceButton);
                menuPanel.add(withdrawButton);
                menuPanel.add(depositButton);
                menuPanel.add(transferButton);
                menuPanel.add(exitButton);

                add(menuPanel, BorderLayout.CENTER);

            constraints.gridx = 0;
            constraints.gridy = 0;
            menuPanel.add(checkBalanceButton, constraints);

            constraints.gridy = 1;
            menuPanel.add(withdrawButton, constraints);

            constraints.gridy = 2;
            menuPanel.add(depositButton, constraints);

            constraints.gridy = 3;
            menuPanel.add(transferButton, constraints);

            constraints.gridy = 4;
            menuPanel.add(exitButton, constraints);

            add(menuPanel, BorderLayout.CENTER);
        }

            private class VirtualWallet {
                private int accountNumber;
                private int pin;
                private double balance;

                public VirtualWallet(int accountNumber, int pin, double balance) {
                    this.accountNumber = accountNumber;
                    this.pin = pin;
                    this.balance = balance;
                }

                public int getAccountNumber() {
                    return accountNumber;
                }

                public void setAccountNumber(int accountNumber) {
                    this.accountNumber = accountNumber;
                }

                public int getPin() {
                    return pin;
                }

                public void setPin(int pin) {
                    this.pin = pin;
                }

                public double getBalance() {
                    return balance;
                }

                public void setBalance(double balance) {
                    this.balance = balance;
                }
            }

            public static void main(String[] args) {
                SwingUtilities.invokeLater(() -> {
                    JavaVirtualWallet walletGUI = new JavaVirtualWallet();
                    walletGUI.setVisible(true);
                });
            }
        }