import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

class Book implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String title;
    private String author;
    private String genre;
    private int publicationYear;
    private boolean isAvailable;
    private String borrower;
    private Date dueDate;
    private double averageRating = 0;
    private int ratingCount = 0;
    private Map<String, Integer> userRatings = new HashMap<>();

    public Book(String id, String title, String author, String genre, int publicationYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publicationYear = publicationYear;
        this.isAvailable = true;
        this.borrower = null;
        this.dueDate = null;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public int getPublicationYear() { return publicationYear; }
    public boolean isAvailable() { return isAvailable; }
    public String getBorrower() { return borrower; }
    public Date getDueDate() { return dueDate; }
    public double getAverageRating() { return averageRating; }
    public int getRatingCount() { return ratingCount; }
    public Integer getUserRating(String username) { return userRatings.get(username); }
    
    public void setAvailable(boolean available) { isAvailable = available; }
    public void setBorrower(String borrower) { this.borrower = borrower; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public void addRating(String username, int rating) {
        if (rating < 1 || rating > 5) return;
        
        Integer previousRating = userRatings.put(username, rating);
        
        if (previousRating != null) {
            averageRating = (averageRating * ratingCount - previousRating + rating) / ratingCount;
        } else {
            averageRating = (averageRating * ratingCount + rating) / (ratingCount + 1);
            ratingCount++;
        }
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s) ★%.1f", 
                title, author, 
                isAvailable ? "Available" : "Borrowed",
                averageRating);
    }
}

class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private boolean isAdmin;
    private boolean isActive;
    private List<String> borrowedBooks;

    public User(String username, String password, String fullName, String email, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.isAdmin = isAdmin;
        this.isActive = true;
        this.borrowedBooks = new ArrayList<>();
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return isAdmin; }
    public boolean isActive() { return isActive; }
    public List<String> getBorrowedBooks() { return borrowedBooks; }
    
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setActive(boolean active) { isActive = active; }
    
    public void addBorrowedBook(String bookId) { borrowedBooks.add(bookId); }
    public void removeBorrowedBook(String bookId) { borrowedBooks.remove(bookId); }

    @Override
    public String toString() {
        return String.format("%s (%s)", username, isAdmin ? "Admin" : "User");
    }
}

class Library implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Book> books;
    private List<User> users;
    private static final String DATA_FILE = "EWULibraryManagementSystem.ser";
    private static final int MAX_BOOKS_PER_USER = 5;
    private static final int LOAN_PERIOD_DAYS = 14;

    public Library() {
        books = new ArrayList<>();
        users = new ArrayList<>();
        loadData();
        
        if (users.isEmpty()) {
            users.add(new User("admin", "admin123", "System Admin", "admin@ewu.edu", true));
        }
    }

    public User login(String username, String password) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password) && u.isActive())
                .findFirst()
                .orElse(null);
    }

    public boolean register(String username, String password, String fullName, String email, boolean isAdmin) {
        if (users.stream().anyMatch(u -> u.getUsername().equals(username))) {
            return false;
        }
        users.add(new User(username, password, fullName, email, isAdmin));
        saveData();
        return true;
    }

    public List<Book> searchBooks(String query) {
        String lcQuery = query.toLowerCase();
        return books.stream()
                .filter(b -> b.getTitle().toLowerCase().contains(lcQuery) ||
                            b.getAuthor().toLowerCase().contains(lcQuery) ||
                            b.getGenre().toLowerCase().contains(lcQuery) ||
                            b.getId().toLowerCase().contains(lcQuery))
                .collect(Collectors.toList());
    }

    public String borrowBook(String bookId, String username) {
        Book book = findBookById(bookId);
        User user = findUserByUsername(username);
        
        if (book == null) return "Book not found!";
        if (user == null) return "User not found!";
        if (!book.isAvailable()) return "Book already borrowed!";
        if (user.getBorrowedBooks().size() >= MAX_BOOKS_PER_USER) return "Borrow limit reached!";
        
        book.setAvailable(false);
        book.setBorrower(username);
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, LOAN_PERIOD_DAYS);
        book.setDueDate(cal.getTime());
        
        user.addBorrowedBook(bookId);
        saveData();
        return "Book borrowed! Due: " + String.format("%tF", book.getDueDate());
    }

    public String returnBook(String bookId) {
        Book book = findBookById(bookId);
        if (book == null) return "Book not found!";
        if (book.isAvailable()) return "Book wasn't borrowed!";
        
        User user = findUserByUsername(book.getBorrower());
        if (user != null) user.removeBorrowedBook(bookId);
        
        book.setAvailable(true);
        book.setBorrower(null);
        book.setDueDate(null);
        saveData();
        return "Book returned successfully!";
    }

    public String rateBook(String bookId, String username, int rating) {
        Book book = findBookById(bookId);
        if (book == null) return "Book not found!";
        
        book.addRating(username, rating);
        saveData();
        return "Rating submitted!";
    }

    public boolean addBook(String id, String title, String author, String genre, int year) {
        if (findBookById(id) != null) return false;
        books.add(new Book(id, title, author, genre, year));
        saveData();
        return true;
    }

    public boolean removeBook(String bookId) {
        Book book = findBookById(bookId);
        if (book != null) {
            users.forEach(u -> u.removeBorrowedBook(bookId));
            books.remove(book);
            saveData();
            return true;
        }
        return false;
    }

    private Book findBookById(String id) {
        return books.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
    }
    
    private User findUserByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            Library saved = (Library) ois.readObject();
            this.books = saved.books;
            this.users = saved.users;
        } catch (FileNotFoundException e) {
            // First run
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Book> getAllBooks() { return new ArrayList<>(books); }
    public List<User> getAllUsers() { return new ArrayList<>(users); }

    public void exportDataToTextFiles() {
        exportAdminsToText();
        exportBooksToText();
        exportUsersToText();
    }

    private void exportAdminsToText() {
        try (PrintWriter writer = new PrintWriter("EWU_Admins.txt")) {
            writer.println("Username,Password,Full Name,Email,Active");
            for (User user : getAllUsers()) {
                if (user.isAdmin()) {
                    writer.printf("%s,%s,%s,%s,%b%n",
                            user.getUsername(),
                            user.getPassword(),
                            user.getFullName(),
                            user.getEmail(),
                            user.isActive());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportBooksToText() {
        try (PrintWriter writer = new PrintWriter("EWU_Books.txt")) {
            writer.println("ID,Title,Author,Genre,Year,Available,Borrower,Due Date,Average Rating,Rating Count");
            for (Book book : getAllBooks()) {
                writer.printf("%s,%s,%s,%s,%d,%b,%s,%s,%.2f,%d%n",
                        book.getId(),
                        book.getTitle(),
                        book.getAuthor(),
                        book.getGenre(),
                        book.getPublicationYear(),
                        book.isAvailable(),
                        book.getBorrower() != null ? book.getBorrower() : "",
                        book.getDueDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(book.getDueDate()) : "",
                        book.getAverageRating(),
                        book.getRatingCount());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportUsersToText() {
        try (PrintWriter writer = new PrintWriter("EWU_Users.txt")) {
            writer.println("Username,Password,Full Name,Email,Active,Borrowed Books");
            for (User user : getAllUsers()) {
                if (!user.isAdmin()) {
                    writer.printf("%s,%s,%s,%s,%b,%s%n",
                            user.getUsername(),
                            user.getPassword(),
                            user.getFullName(),
                            user.getEmail(),
                            user.isActive(),
                            String.join(";", user.getBorrowedBooks()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class EWULibraryManagementSystem {
    private Library library;
    private User currentUser;
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private DefaultListModel<Book> bookListModel;
    private DefaultListModel<User> userListModel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new EWULibraryManagementSystem();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public EWULibraryManagementSystem() {
        library = new Library();
        showLogin();
    }

    private void showLogin() {
        JFrame loginFrame = new JFrame("EWU Library Management System - Login");
        loginFrame.setSize(350, 250);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField userField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        JButton exportBtn = new JButton("Export Data");

        gbc.gridx = 0;
        gbc.gridy = 0;
        loginFrame.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        loginFrame.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        loginFrame.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        loginFrame.add(passField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        loginFrame.add(loginBtn, gbc);

        gbc.gridy = 3;
        loginFrame.add(registerBtn, gbc);

        gbc.gridy = 4;
        loginFrame.add(exportBtn, gbc);

        loginBtn.addActionListener(e -> {
            currentUser = library.login(userField.getText(), new String(passField.getPassword()));
            if (currentUser != null) {
                loginFrame.dispose();
                createMainWindow();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid login!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerBtn.addActionListener(e -> showRegistration(loginFrame));
        exportBtn.addActionListener(e -> {
            library.exportDataToTextFiles();
            JOptionPane.showMessageDialog(loginFrame, "Data exported to text files!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        });

        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    private void showRegistration(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Register New User", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JCheckBox adminCheck = new JCheckBox("Admin");
        JButton registerBtn = new JButton("Register");

        dialog.add(new JLabel("Username:"));
        dialog.add(userField);
        dialog.add(new JLabel("Password:"));
        dialog.add(passField);
        dialog.add(new JLabel("Full Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Email:"));
        dialog.add(emailField);
        dialog.add(new JLabel("Admin Privileges:"));
        dialog.add(adminCheck);
        dialog.add(new JLabel());
        dialog.add(registerBtn);

        registerBtn.addActionListener(e -> {
            if (library.register(userField.getText(), new String(passField.getPassword()),
                    nameField.getText(), emailField.getText(), adminCheck.isSelected())) {
                JOptionPane.showMessageDialog(dialog, "Registration successful!");
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Username already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private void createMainWindow() {
        frame = new JFrame("EWU Library Management System - " + currentUser.getUsername());
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        JButton logoutBtn = new JButton("Logout (" + currentUser.getUsername() + ")");
        logoutBtn.addActionListener(e -> logout());
        JButton exportBtn = new JButton("Export Data");
        exportBtn.addActionListener(e -> {
            library.exportDataToTextFiles();
            JOptionPane.showMessageDialog(frame, "Data exported to text files!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        });

        header.add(new JLabel("EWU Library Management System", SwingConstants.CENTER), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(exportBtn);
        buttonPanel.add(logoutBtn);
        header.add(buttonPanel, BorderLayout.EAST);
        frame.add(header, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Books", createBooksTab());
        if (currentUser.isAdmin()) tabbedPane.addTab("Users", createUsersTab());
        tabbedPane.addTab("My Account", createAccountTab());
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createBooksTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel searchPanel = new JPanel();
        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        JButton showAllBtn = new JButton("Show All");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(showAllBtn);

        bookListModel = new DefaultListModel<>();
        JList<Book> bookList = new JList<>(bookListModel);
        bookList.setCellRenderer(new BookCellRenderer());
        JScrollPane scrollPane = new JScrollPane(bookList);

        JPanel actionPanel = new JPanel();
        JButton borrowBtn = new JButton("Borrow");
        JButton returnBtn = new JButton("Return");
        JButton detailsBtn = new JButton("Details");

        if (currentUser.isAdmin()) {
            JButton addBtn = new JButton("Add Book");
            JButton removeBtn = new JButton("Remove Book");
            actionPanel.add(addBtn);
            actionPanel.add(removeBtn);

            addBtn.addActionListener(e -> showAddBookDialog());
            removeBtn.addActionListener(e -> {
                Book selected = bookList.getSelectedValue();
                if (selected != null) {
                    if (library.removeBook(selected.getId())) {
                        JOptionPane.showMessageDialog(frame, "Book removed successfully!");
                        refreshBooks();
                    } else {
                        JOptionPane.showMessageDialog(frame, "Failed to remove book!", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }

        actionPanel.add(borrowBtn);
        actionPanel.add(returnBtn);
        actionPanel.add(detailsBtn);

        searchBtn.addActionListener(e -> {
            List<Book> results = library.searchBooks(searchField.getText());
            bookListModel.clear();
            results.forEach(bookListModel::addElement);
        });

        showAllBtn.addActionListener(e -> refreshBooks());

        borrowBtn.addActionListener(e -> {
            Book selected = bookList.getSelectedValue();
            if (selected != null && selected.isAvailable()) {
                String result = library.borrowBook(selected.getId(), currentUser.getUsername());
                JOptionPane.showMessageDialog(frame, result);
                refreshBooks();
            }
        });

        returnBtn.addActionListener(e -> {
            Book selected = bookList.getSelectedValue();
            if (selected != null && !selected.isAvailable() && 
                selected.getBorrower().equals(currentUser.getUsername())) {
                String result = library.returnBook(selected.getId());
                JOptionPane.showMessageDialog(frame, result);
                refreshBooks();
            }
        });

        detailsBtn.addActionListener(e -> {
            Book selected = bookList.getSelectedValue();
            if (selected != null) showBookDetails(selected);
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        refreshBooks();
        return panel;
    }

    private JPanel createUsersTab() {
        JPanel panel = new JPanel(new BorderLayout());
        userListModel = new DefaultListModel<>();
        JList<User> userList = new JList<>(userListModel);
        userList.setCellRenderer(new UserCellRenderer());

        JPanel buttonPanel = new JPanel();
        JButton toggleBtn = new JButton("Toggle Status");
        JButton passBtn = new JButton("Reset Password");

        toggleBtn.addActionListener(e -> {
            User selected = userList.getSelectedValue();
            if (selected != null) {
                selected.setActive(!selected.isActive());
                refreshUsers();
            }
        });

        passBtn.addActionListener(e -> {
            User selected = userList.getSelectedValue();
            if (selected != null) {
                String newPass = JOptionPane.showInputDialog(frame, "Enter new password:");
                if (newPass != null) {
                    selected.setPassword(newPass);
                    JOptionPane.showMessageDialog(frame, "Password updated!");
                }
            }
        });

        buttonPanel.add(toggleBtn);
        buttonPanel.add(passBtn);
        panel.add(new JScrollPane(userList), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        refreshUsers();
        return panel;
    }

    private JPanel createAccountTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel infoPanel = new JPanel(new GridLayout(5, 1));
        infoPanel.add(new JLabel("Username: " + currentUser.getUsername()));
        infoPanel.add(new JLabel("Name: " + currentUser.getFullName()));
        infoPanel.add(new JLabel("Email: " + currentUser.getEmail()));
        infoPanel.add(new JLabel("Status: " + (currentUser.isActive() ? "Active" : "Inactive")));
        infoPanel.add(new JLabel("Role: " + (currentUser.isAdmin() ? "Admin" : "User")));

        DefaultListModel<Book> borrowedModel = new DefaultListModel<>();
        library.getAllBooks().stream()
            .filter(b -> !b.isAvailable() && b.getBorrower().equals(currentUser.getUsername()))
            .forEach(borrowedModel::addElement);

        JPanel buttonPanel = new JPanel();
        JButton passBtn = new JButton("Change Password");
        JButton updateBtn = new JButton("Update Info");

        passBtn.addActionListener(e -> {
            String newPass = JOptionPane.showInputDialog(frame, "Enter new password:");
            if (newPass != null) {
                currentUser.setPassword(newPass);
                JOptionPane.showMessageDialog(frame, "Password changed!");
            }
        });

        updateBtn.addActionListener(e -> {
            JTextField nameField = new JTextField(currentUser.getFullName());
            JTextField emailField = new JTextField(currentUser.getEmail());
            
            JPanel updatePanel = new JPanel(new GridLayout(2, 2));
            updatePanel.add(new JLabel("Full Name:"));
            updatePanel.add(nameField);
            updatePanel.add(new JLabel("Email:"));
            updatePanel.add(emailField);
            
            if (JOptionPane.showConfirmDialog(frame, updatePanel, "Update Info", 
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                currentUser.setFullName(nameField.getText());
                currentUser.setEmail(emailField.getText());
                JOptionPane.showMessageDialog(frame, "Information updated!");
            }
        });

        buttonPanel.add(passBtn);
        buttonPanel.add(updateBtn);

        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JList<>(borrowedModel)), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void showAddBookDialog() {
        JDialog dialog = new JDialog(frame, "Add New Book", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));

        JTextField idField = new JTextField();
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JTextField genreField = new JTextField();
        JTextField yearField = new JTextField();
        JButton addBtn = new JButton("Add Book");

        dialog.add(new JLabel("Book ID:"));
        dialog.add(idField);
        dialog.add(new JLabel("Title:"));
        dialog.add(titleField);
        dialog.add(new JLabel("Author:"));
        dialog.add(authorField);
        dialog.add(new JLabel("Genre:"));
        dialog.add(genreField);
        dialog.add(new JLabel("Year:"));
        dialog.add(yearField);
        dialog.add(new JLabel());
        dialog.add(addBtn);

        addBtn.addActionListener(e -> {
            try {
                if (library.addBook(idField.getText(), titleField.getText(), 
                        authorField.getText(), genreField.getText(), 
                        Integer.parseInt(yearField.getText()))) {
                    JOptionPane.showMessageDialog(dialog, "Book added!");
                    refreshBooks();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Book ID already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid year!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void showBookDetails(Book book) {
        if (book == null) return;
        
        JDialog dialog = new JDialog(frame, "Book Details", true);
        dialog.setSize(350, 250);
        dialog.setLayout(new BorderLayout());

        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setText(String.format(
            "ID: %s\nTitle: %s\nAuthor: %s\nGenre: %s\nYear: %d\nStatus: %s\nRating: %.1f (%d ratings)",
            book.getId(), book.getTitle(), book.getAuthor(), book.getGenre(),
            book.getPublicationYear(), book.isAvailable() ? "Available" : 
            "Borrowed by " + book.getBorrower() + " until " + String.format("%tF", book.getDueDate()),
            book.getAverageRating(), book.getRatingCount()
        ));

        JPanel ratingPanel = new JPanel(new FlowLayout());
        ratingPanel.add(new JLabel("Your Rating: "));
        
        JPanel starsPanel = new JPanel(new GridLayout(1, 5));
        Integer userRating = book.getUserRating(currentUser.getUsername());
        
        for (int i = 1; i <= 5; i++) {
            JButton starBtn = new JButton("★");
            starBtn.setFont(new Font("Arial", Font.PLAIN, 20));
            starBtn.setForeground(userRating != null && i <= userRating ? Color.YELLOW : Color.GRAY);
            final int ratingValue = i;
            starBtn.addActionListener(e -> {
                library.rateBook(book.getId(), currentUser.getUsername(), ratingValue);
                JOptionPane.showMessageDialog(dialog, "Thank you for your rating!");
                dialog.dispose();
                showBookDetails(book);
            });
            starsPanel.add(starBtn);
        }
        
        ratingPanel.add(starsPanel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(ratingPanel, BorderLayout.NORTH);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(closeBtn, BorderLayout.SOUTH);

        dialog.add(new JScrollPane(details), BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void refreshBooks() {
        bookListModel.clear();
        library.getAllBooks().forEach(bookListModel::addElement);
    }

    private void refreshUsers() {
        userListModel.clear();
        library.getAllUsers().forEach(userListModel::addElement);
    }

    private void logout() {
        if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to logout?", 
            "Confirm Logout", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            frame.dispose();
            currentUser = null;
            showLogin();
        }
    }

    private static class BookCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                    boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Book book = (Book) value;
            setText(String.format("%s - %s (%s) ★%.1f", 
                    book.getTitle(), book.getAuthor(),
                    book.isAvailable() ? "Available" : "Borrowed",
                    book.getAverageRating()));
            setForeground(book.isAvailable() ? Color.BLACK : Color.RED);
            return this;
        }
    }

    private static class UserCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            User user = (User) value;
            setText(user.getUsername() + (user.isAdmin() ? " (Admin)" : " (User)"));
            setForeground(user.isActive() ? Color.BLACK : Color.GRAY);
            return this;
        }
    }
}