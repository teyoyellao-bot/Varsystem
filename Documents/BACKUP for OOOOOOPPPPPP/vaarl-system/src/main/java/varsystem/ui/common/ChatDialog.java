
package varsystem.ui.common;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import varsystem.data.DB;
import static varsystem.data.Session.*;
import varsystem.model.Req;
import static varsystem.ui.components.Dialogs.msg;
import static varsystem.ui.frame.MainFrame.APP;

public final class ChatDialog {

    private ChatDialog() {
    }

    private static final Color NAVY = new Color(3, 25, 62);
    private static final Color BLUE = new Color(0, 105, 255);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT = new Color(0, 55, 125);
    private static final Color GRAY = new Color(195, 207, 222);
    private static final Color LIGHT = new Color(240, 245, 252);

    private static final Font TITLE_FONT =
            new Font("SansSerif", Font.BOLD, 25);

    private static final Font NAME_FONT =
            new Font("SansSerif", Font.BOLD, 17);

    private static final Font NORMAL_FONT =
            new Font("SansSerif", Font.PLAIN, 15);

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a");

    // ROUNDED MESSAGE BUBBLE

    private static class RoundedBubble extends JTextArea {

        private final Color bubbleColor;

        RoundedBubble(String message, Color color, Color textColor) {

            super(message);

            this.bubbleColor = color;

            setEditable(false);
            setFocusable(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setFont(NORMAL_FONT);
            setForeground(textColor);
            setOpaque(false);
            setBorder(new EmptyBorder(10, 14, 10, 14));

            FontMetrics fm = getFontMetrics(NORMAL_FONT);

            int textWidth = fm.stringWidth(message);
            int width = Math.min(350, Math.max(60, textWidth + 28));

            setSize(width, Short.MAX_VALUE);

            Dimension preferred = getPreferredSize();

            setPreferredSize(new Dimension(
                    width,
                    Math.max(42, preferred.height)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(bubbleColor);

            g2.fillRoundRect(
                    0, 0,
                    getWidth(), getHeight(),
                    24, 24
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // ROUNDED CONTACT BUTTON

    private static class RoundedContactButton extends JButton {

        RoundedContactButton() {

            setLayout(new BorderLayout(15, 0));
            setBackground(WHITE);
            setBorder(new EmptyBorder(12, 12, 12, 15));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);

            setCursor(Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color color = getBackground();

            if (getModel().isPressed()) {

                color = new Color(225, 235, 248);

            } else if (getModel().isRollover()) {

                color = new Color(245, 248, 253);
            }

            g2.setColor(color);

            g2.fillRoundRect(
                    0, 0,
                    getWidth(), getHeight(),
                    22, 22
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // ROUNDED AVATAR

    private static class RoundedAvatar extends JLabel {

        RoundedAvatar(String text) {

            super(text, SwingConstants.CENTER);

            setFont(new Font("SansSerif", Font.BOLD, 23));
            setForeground(TEXT);
            setPreferredSize(new Dimension(76, 76));
            setMinimumSize(new Dimension(76, 76));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(new Color(218, 240, 250));

            g2.fillOval(
                    0, 0,
                    getWidth(), getHeight()
            );

            g2.setColor(new Color(45, 105, 150));
            g2.setStroke(new BasicStroke(2));

            g2.drawOval(
                    1, 1,
                    getWidth() - 3,
                    getHeight() - 3
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    private static final class Contact {

        final String displayName;
        final String userEmail;
        final String shopName;

        boolean active;

        Contact(
                String displayName,
                String userEmail,
                String shopName,
                boolean active) {

            this.displayName = displayName;
            this.userEmail = userEmail;
            this.shopName = shopName;
            this.active = active;
        }
    }

    // OPEN CHAT

    public static void open(String preselect) {

        if (ME == null) {

            msg("Please log in first.");
            return;
        }

        final boolean isUser =
                "User".equalsIgnoreCase(ME.role);

        final String currentShop = getCurrentShopName();

        if (!isUser && currentShop.isEmpty()) {

            msg("Shop account is not properly configured.");
            return;
        }

        Map<String, Contact> contacts =
                getContacts(isUser, currentShop);

        if (contacts.isEmpty()) {

            msg(isUser
                    ? "You don't have any repair shop conversations yet."
                    : "No customers have conversations with your shop yet.");

            return;
        }

        final JDialog dialog =
                new JDialog(APP, "Chats", true);

        dialog.setSize(1050, 650);
        dialog.setMinimumSize(new Dimension(650, 450));
        dialog.setLocationRelativeTo(APP);
        dialog.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );

        dialog.setLayout(new BorderLayout());

        CardLayout cardLayout = new CardLayout();

        JPanel mainCards = new JPanel(cardLayout);

        // CHAT LIST SCREEN

        JPanel listScreen = new JPanel(new BorderLayout());

        listScreen.setBackground(NAVY);
        listScreen.setBorder(
                new EmptyBorder(12, 18, 15, 18)
        );

        JPanel top = new JPanel();

        top.setBackground(NAVY);

        top.setLayout(
                new BoxLayout(top, BoxLayout.Y_AXIS)
        );

        JPanel heading = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0)
        );

        heading.setBackground(NAVY);

        JButton backButton = new JButton("\u2190");

        backButton.setFont(
                new Font("SansSerif", Font.BOLD, 32)
        );

        backButton.setForeground(BLUE);
        backButton.setBackground(NAVY);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setContentAreaFilled(false);

        backButton.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
        ));

        backButton.addActionListener(e -> dialog.dispose());

        JLabel title = new JLabel("Chats");

        title.setFont(TITLE_FONT);
        title.setForeground(WHITE);

        heading.add(backButton);
        heading.add(title);

        JPanel filters = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 12, 10)
        );

        filters.setBackground(NAVY);
        filters.setBorder(
                new EmptyBorder(0, 22, 0, 0)
        );

        JButton allButton = new JButton("All");
        JButton activeButton = new JButton("Active");

        styleFilter(allButton, true);
        styleFilter(activeButton, false);

        filters.add(allButton);
        filters.add(activeButton);

        top.add(heading);
        top.add(filters);

        listScreen.add(top, BorderLayout.NORTH);

        JPanel contactListPanel = new JPanel();

        contactListPanel.setBackground(NAVY);

        contactListPanel.setLayout(
                new BoxLayout(
                        contactListPanel,
                        BoxLayout.Y_AXIS
                )
        );

        JScrollPane contactScroll =
                new JScrollPane(contactListPanel);

        contactScroll.setBorder(null);
        contactScroll.setBackground(NAVY);

        contactScroll.getViewport().setBackground(NAVY);

        contactScroll.getVerticalScrollBar()
                .setUnitIncrement(18);

        listScreen.add(
                contactScroll,
                BorderLayout.CENTER
        );

        // CONVERSATION SCREEN

        JPanel chatScreen = new JPanel(
                new BorderLayout()
        );

        chatScreen.setBackground(WHITE);

        JPanel chatHeader = new JPanel(
                new BorderLayout(10, 0)
        );

        chatHeader.setBackground(NAVY);

        chatHeader.setBorder(
                new EmptyBorder(12, 15, 12, 15)
        );

        JButton chatBack = new JButton("\u2190");

        chatBack.setForeground(WHITE);
        chatBack.setBackground(NAVY);

        chatBack.setFont(
                new Font("SansSerif", Font.BOLD, 27)
        );

        chatBack.setBorderPainted(false);
        chatBack.setFocusPainted(false);
        chatBack.setContentAreaFilled(false);

        JLabel chatName = new JLabel();

        chatName.setForeground(WHITE);
        chatName.setFont(NAME_FONT);

        chatHeader.add(chatBack, BorderLayout.WEST);
        chatHeader.add(chatName, BorderLayout.CENTER);

        JPanel messagesPanel = new JPanel();

        messagesPanel.setLayout(
                new BoxLayout(
                        messagesPanel,
                        BoxLayout.Y_AXIS
                )
        );

        messagesPanel.setBackground(LIGHT);

        messagesPanel.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        JScrollPane historyScroll =
                new JScrollPane(messagesPanel);

        historyScroll.setBorder(null);

        historyScroll.getViewport()
                .setBackground(LIGHT);

        historyScroll.getVerticalScrollBar()
                .setUnitIncrement(16);

        JTextField messageInput = new JTextField();

        messageInput.setFont(NORMAL_FONT);

        messageInput.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                GRAY, 1, true
                        ),
                        new EmptyBorder(10, 12, 10, 12)
                )
        );

        messageInput.setBackground(WHITE);

        JButton sendButton = new JButton("Send");

        sendButton.setFont(
                new Font("SansSerif", Font.BOLD, 14)
        );

        sendButton.setForeground(WHITE);
        sendButton.setBackground(BLUE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);

        sendButton.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
        ));

        sendButton.setBorder(
                new EmptyBorder(10, 20, 10, 20)
        );

        JPanel composer = new JPanel(
                new BorderLayout(10, 0)
        );

        composer.setBackground(WHITE);

        composer.setBorder(
                new EmptyBorder(12, 15, 15, 15)
        );

        composer.add(
                messageInput,
                BorderLayout.CENTER
        );

        composer.add(
                sendButton,
                BorderLayout.EAST
        );

        chatScreen.add(chatHeader, BorderLayout.NORTH);
        chatScreen.add(historyScroll, BorderLayout.CENTER);
        chatScreen.add(composer, BorderLayout.SOUTH);

        mainCards.add(listScreen, "LIST");
        mainCards.add(chatScreen, "CHAT");

        dialog.add(mainCards, BorderLayout.CENTER);

        // SELECTED CONVERSATION

        final Contact[] selectedContact = {null};

        final boolean[] messageOptionsOpen = {false};

        // LOAD MESSAGES

        final Runnable[] loadMessages = new Runnable[1];

        loadMessages[0] = () -> {

            Contact contact = selectedContact[0];

            if (contact == null) {
                return;
            }

            String key = conversationKey(
                    contact.userEmail,
                    contact.shopName
            );

            List<String> messages;

            // FINAL LOCAL REFERENCE:
            // Lock the same map instance being accessed.

            final var chats = DB.chats;

            synchronized (chats) {

                messages = new ArrayList<>(
                        chats.getOrDefault(
                                key,
                                new ArrayList<>()
                        )
                );
            }

            messagesPanel.removeAll();

            if (messages.isEmpty()) {

                JLabel empty = new JLabel(
                        "<html><center>No messages yet.<br>"
                        + "Start your conversation here.</center></html>"
                );

                empty.setFont(NORMAL_FONT);
                empty.setForeground(TEXT);

                empty.setAlignmentX(
                        Component.CENTER_ALIGNMENT
                );

                messagesPanel.add(Box.createVerticalGlue());
                messagesPanel.add(empty);
                messagesPanel.add(Box.createVerticalGlue());

            } else {

                for (String raw : messages) {

                    if (raw == null) {
                        continue;
                    }

                    String sender;
                    String message;

                    if (raw.contains("||")) {

                        String[] parts =
                                raw.split("\\|\\|", 3);

                        sender = parts.length > 0
                                ? parts[0] : "";

                        message = parts.length > 2
                                ? parts[2] : "";

                    } else {

                        int separator = raw.indexOf(": ");

                        sender = separator >= 0
                                ? raw.substring(0, separator)
                                : "";

                        message = separator >= 0
                                ? raw.substring(separator + 2)
                                : raw;
                    }

                    boolean mine = isUser
                            ? same(sender, ME.name)
                            : same(sender, currentShop);

                    JPanel row = new JPanel(
                            new FlowLayout(
                                    mine
                                            ? FlowLayout.RIGHT
                                            : FlowLayout.LEFT,
                                    5,
                                    5
                            )
                    );

                    row.setOpaque(false);

                    row.setAlignmentX(
                            Component.LEFT_ALIGNMENT
                    );

                    Color bubbleColor = mine
                            ? new Color(22, 114, 232)
                            : new Color(230, 235, 243);

                    Color textColor = mine
                            ? WHITE
                            : new Color(35, 45, 60);

                    RoundedBubble bubbleText =
                            new RoundedBubble(
                                    message,
                                    bubbleColor,
                                    textColor
                            );

                    // THREE-DOT BUTTON

                    JButton moreButton =
                            new JButton("\u22EE");

                    moreButton.setFont(
                            new Font(
                                    "SansSerif",
                                    Font.BOLD,
                                    20
                            )
                    );

                    moreButton.setForeground(
                            new Color(90, 100, 115)
                    );

                    moreButton.setBackground(LIGHT);
                    moreButton.setBorderPainted(false);
                    moreButton.setContentAreaFilled(false);
                    moreButton.setFocusPainted(false);

                    moreButton.setCursor(
                            Cursor.getPredefinedCursor(
                                    Cursor.HAND_CURSOR
                            )
                    );

                    moreButton.addActionListener(e -> {

                        messageOptionsOpen[0] = true;

                        showMessageOptions(
                                moreButton,
                                key,
                                raw,
                                sender,
                                message,
                                mine,
                                loadMessages[0],
                                () -> messageOptionsOpen[0] = false
                        );
                    });

                    row.add(bubbleText);
                    row.add(moreButton);

                    row.setMaximumSize(
                            new Dimension(
                                    Integer.MAX_VALUE,
                                    row.getPreferredSize().height
                            )
                    );

                    messagesPanel.add(row);
                }
            }

            messagesPanel.revalidate();
            messagesPanel.repaint();

            SwingUtilities.invokeLater(() -> {

                JScrollBar bar =
                        historyScroll.getVerticalScrollBar();

                bar.setValue(bar.getMaximum());
            });
        };

        // SEND MESSAGE

        Runnable sendMessage = () -> {

            Contact contact = selectedContact[0];

            if (contact == null) {
                return;
            }

            String message = messageInput.getText().trim();

            if (message.isEmpty()) {
                return;
            }

            String sender = isUser
                    ? ME.name
                    : currentShop;

            if (sender == null || sender.trim().isEmpty()) {

                sender = isUser
                        ? "Customer"
                        : "Shop";
            }

            String key = conversationKey(
                    contact.userEmail,
                    contact.shopName
            );

            String formattedMessage = sender
                    + "||"
                    + LocalDateTime.now().toString()
                    + "||"
                    + message;

            final var chats = DB.chats;

            synchronized (chats) {

                chats.computeIfAbsent(
                        key,
                        k -> new ArrayList<>()
                ).add(formattedMessage);
            }

            boolean saved = DB.saveData();

            messageInput.setText("");

            loadMessages[0].run();

            if (!saved) {
                msg("Message was sent.");
            }
        };

        sendButton.addActionListener(
                e -> sendMessage.run()
        );

        messageInput.addActionListener(
                e -> sendMessage.run()
        );

        // BACK TO CHAT LIST

        chatBack.addActionListener(e -> {

            selectedContact[0] = null;

            cardLayout.show(mainCards, "LIST");
        });

        // OPEN CONTACT

        final Runnable[] refreshList = {null};

        java.util.function.Consumer<Contact> openContact =
                contact -> {

                    selectedContact[0] = contact;

                    chatName.setText(contact.displayName);

                    loadMessages[0].run();

                    cardLayout.show(mainCards, "CHAT");
                };

        // FILTER CONTACTS

        final boolean[] activeFilter = {false};

        refreshList[0] = () -> {

            contactListPanel.removeAll();

            List<Contact> sortedContacts =
                    new ArrayList<>(contacts.values());

            sortedContacts.sort(
                    Comparator.comparing(
                            ChatDialog::getLastMessageDate
                    ).reversed()
            );

            for (Contact contact : sortedContacts) {

                if (activeFilter[0] && !contact.active) {
                    continue;
                }

                JButton contactButton =
                        new RoundedContactButton();

                JLabel avatar = new RoundedAvatar(
                        getInitial(contact.displayName)
                );

                JPanel details = new JPanel();

                details.setOpaque(false);

                details.setLayout(
                        new BoxLayout(
                                details,
                                BoxLayout.Y_AXIS
                        )
                );

                JLabel name = new JLabel(
                        contact.displayName
                );

                name.setFont(NAME_FONT);
                name.setForeground(TEXT);

                JLabel preview = new JLabel(
                        getLastMessage(contact)
                );

                preview.setFont(NORMAL_FONT);
                preview.setForeground(TEXT);

                details.add(Box.createVerticalGlue());
                details.add(name);
                details.add(Box.createVerticalStrut(7));
                details.add(preview);
                details.add(Box.createVerticalGlue());

                JPanel right = new JPanel(
                        new BorderLayout(0, 8)
                );

                right.setOpaque(false);

                JLabel time = new JLabel(
                        getLastMessageTime(contact)
                );

                time.setFont(
                        new Font(
                                "SansSerif",
                                Font.PLAIN,
                                12
                        )
                );

                time.setForeground(
                        new Color(100, 110, 125)
                );

                time.setHorizontalAlignment(
                        SwingConstants.RIGHT
                );

                JLabel status = new JLabel(
                        contact.active ? "Active" : ""
                );

                status.setFont(
                        new Font(
                                "SansSerif",
                                Font.BOLD,
                                12
                        )
                );

                status.setForeground(
                        new Color(25, 145, 85)
                );

                right.add(time, BorderLayout.NORTH);
                right.add(status, BorderLayout.SOUTH);

                contactButton.add(
                        avatar,
                        BorderLayout.WEST
                );

                contactButton.add(
                        details,
                        BorderLayout.CENTER
                );

                contactButton.add(
                        right,
                        BorderLayout.EAST
                );

                contactButton.setMaximumSize(
                        new Dimension(
                                Integer.MAX_VALUE,
                                110
                        )
                );

                contactButton.setPreferredSize(
                        new Dimension(850, 110)
                );

                contactButton.addActionListener(
                        e -> openContact.accept(contact)
                );

                contactListPanel.add(contactButton);

                contactListPanel.add(
                        Box.createVerticalStrut(12)
                );
            }

            if (contactListPanel.getComponentCount() == 0) {

                JLabel empty = new JLabel(
                        "No active conversations.",
                        SwingConstants.CENTER
                );

                empty.setForeground(WHITE);
                empty.setFont(NORMAL_FONT);

                empty.setAlignmentX(
                        Component.CENTER_ALIGNMENT
                );

                contactListPanel.add(
                        Box.createVerticalStrut(40)
                );

                contactListPanel.add(empty);
            }

            contactListPanel.revalidate();
            contactListPanel.repaint();
        };

        allButton.addActionListener(e -> {

            activeFilter[0] = false;

            styleFilter(allButton, true);
            styleFilter(activeButton, false);

            refreshList[0].run();
        });

        activeButton.addActionListener(e -> {

            activeFilter[0] = true;

            styleFilter(allButton, false);
            styleFilter(activeButton, true);

            refreshList[0].run();
        });

        refreshList[0].run();

        if (preselect != null && !preselect.trim().isEmpty()) {

            for (Contact contact : contacts.values()) {

                if (same(contact.displayName, preselect)) {

                    openContact.accept(contact);
                    break;
                }
            }
        }

        // AUTOMATIC REFRESH

        javax.swing.Timer refreshTimer =
                new javax.swing.Timer(1000, e -> {

                    if (messageOptionsOpen[0]) {
                        return;
                    }

                    if (selectedContact[0] != null) {

                        loadMessages[0].run();

                    } else {

                        refreshList[0].run();
                    }
                });

        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                refreshTimer.stop();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                refreshTimer.stop();
            }
        });

        refreshTimer.start();

        dialog.setVisible(true);

        refreshTimer.stop();
    }

    // MESSAGE OPTIONS: EDIT AND DELETE

    private static void showMessageOptions(
            Component invoker,
            String key,
            String raw,
            String sender,
            String message,
            boolean mine,
            Runnable reload,
            Runnable onClose) {

        JPopupMenu popup = new JPopupMenu();

        final boolean[] actionSelected = {false};

        if (mine) {

            JMenuItem editItem =
                    new JMenuItem("Edit Message");

            JMenuItem deleteItem =
                    new JMenuItem("Delete Message");

            // EDIT MESSAGE

            editItem.addActionListener(e -> {

                actionSelected[0] = true;

                try {

                    String updatedMessage =
                            JOptionPane.showInputDialog(
                                    invoker,
                                    "Edit your message:",
                                    message
                            );

                    if (updatedMessage == null) {
                        return;
                    }

                    updatedMessage = updatedMessage.trim();

                    if (updatedMessage.isEmpty()) {

                        msg("Message cannot be empty.");
                        return;
                    }

                    boolean changed = false;

                    final var chats = DB.chats;

                    synchronized (chats) {

                        List<String> messages =
                                chats.get(key);

                        if (messages != null) {

                            int index = messages.indexOf(raw);

                            if (index >= 0) {

                                String edited = sender
                                        + "||"
                                        + LocalDateTime.now().toString()
                                        + "||"
                                        + updatedMessage;

                                messages.set(index, edited);

                                changed = true;
                            }
                        }
                    }

                    if (changed) {

                        boolean saved = DB.saveData();

                        reload.run();

                        if (!saved) {

                            msg(
                                    "Message was edited, but could not be saved permanently."
                            );
                        }

                    } else {

                        msg("Message could not be found.");
                    }

                } finally {

                    onClose.run();
                }
            });

            // DELETE OWN MESSAGE

            deleteItem.addActionListener(e -> {

                actionSelected[0] = true;

                try {

                    int choice = JOptionPane.showConfirmDialog(
                            invoker,
                            "Are you sure you want to delete this message?",
                            "Delete Message",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }

                    boolean deleted = false;

                    final var chats = DB.chats;

                    synchronized (chats) {

                        List<String> messages =
                                chats.get(key);

                        if (messages != null) {
                            deleted = messages.remove(raw);
                        }
                    }

                    if (deleted) {

                        boolean saved = DB.saveData();

                        reload.run();

                        if (!saved) {

                            msg(
                                    "Message was deleted, but the change could not be saved permanently."
                            );
                        }

                    } else {

                        msg("Message could not be found.");
                    }

                } finally {

                    onClose.run();
                }
            });

            popup.add(editItem);
            popup.add(deleteItem);

        } else {

            // DELETE RECEIVED MESSAGE ONLY

            JMenuItem deleteItem =
                    new JMenuItem("Delete Message");

            deleteItem.addActionListener(e -> {

                actionSelected[0] = true;

                try {

                    int choice = JOptionPane.showConfirmDialog(
                            invoker,
                            "Are you sure you want to delete this message?",
                            "Delete Message",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }

                    boolean deleted = false;

                    final var chats = DB.chats;

                    synchronized (chats) {

                        List<String> messages =
                                chats.get(key);

                        if (messages != null) {
                            deleted = messages.remove(raw);
                        }
                    }

                    if (deleted) {

                        boolean saved = DB.saveData();

                        reload.run();

                        if (!saved) {

                            msg(
                                    "Message was deleted, but the change could not be saved permanently."
                            );
                        }

                    } else {

                        msg("Message could not be found.");
                    }

                } finally {

                    onClose.run();
                }
            });

            popup.add(deleteItem);
        }

        popup.addPopupMenuListener(
                new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(
                    PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(
                    PopupMenuEvent e) {

                if (!actionSelected[0]) {
                    onClose.run();
                }
            }

            @Override
            public void popupMenuCanceled(
                    PopupMenuEvent e) {

                if (!actionSelected[0]) {
                    onClose.run();
                }
            }
        });

        popup.show(
                invoker,
                0,
                invoker.getHeight()
        );
    }

    // GET CONTACTS FROM REQUESTS AND SAVED CHATS

    private static Map<String, Contact> getContacts(
            boolean isUser,
            String currentShop) {

        Map<String, Contact> contacts =
                new LinkedHashMap<>();

        // FIRST: GET CONTACTS FROM ACTUAL REQUESTS

        final var requests = DB.reqs;

        synchronized (requests) {

            for (Req r : requests) {

                if (r == null) {
                    continue;
                }

                if (!notBlank(r.shop)
                        || !notBlank(r.uemail)) {
                    continue;
                }

                if (isUser) {

                    if (!same(r.uemail, ME.email)) {
                        continue;
                    }

                    String key =
                            r.shop.trim().toLowerCase();

                    Contact existing =
                            contacts.get(key);

                    if (existing == null) {

                        contacts.put(
                                key,
                                new Contact(
                                        r.shop,
                                        ME.email,
                                        r.shop,
                                        isActive(r.status)
                                )
                        );

                    } else if (isActive(r.status)) {

                        existing.active = true;
                    }

                } else {

                    if (!same(r.shop, currentShop)) {
                        continue;
                    }

                    String key =
                            r.uemail.trim().toLowerCase();

                    String customerName =
                            notBlank(r.uname)
                                    ? r.uname
                                    : r.uemail;

                    Contact existing =
                            contacts.get(key);

                    if (existing == null) {

                        contacts.put(
                                key,
                                new Contact(
                                        customerName,
                                        r.uemail,
                                        currentShop,
                                        isActive(r.status)
                                )
                        );

                    } else if (isActive(r.status)) {

                        existing.active = true;
                    }
                }
            }
        }

        // SECOND: COPY SAVED CHAT KEYS.
        // Do not lock DB.chats while accessing DB.users.

        final var chats = DB.chats;

        List<String> savedChatKeys;

        synchronized (chats) {

            savedChatKeys = new ArrayList<>(
                    chats.keySet()
            );
        }

        // PROCESS SAVED CONVERSATIONS

        for (String chatKey : savedChatKeys) {

            if (chatKey == null
                    || !chatKey.startsWith("chat|")) {
                continue;
            }

            String[] parts =
                    chatKey.split("\\|", 3);

            if (parts.length != 3) {
                continue;
            }

            String userEmail = parts[1].trim();
            String shopName = parts[2].trim();

            if (!notBlank(userEmail)
                    || !notBlank(shopName)) {
                continue;
            }

            if (isUser) {

                if (!same(userEmail, ME.email)) {
                    continue;
                }

                String contactKey =
                        shopName.toLowerCase();

                if (!contacts.containsKey(contactKey)) {

                    contacts.put(
                            contactKey,
                            new Contact(
                                    shopName,
                                    userEmail,
                                    shopName,
                                    false
                            )
                    );
                }

            } else {

                if (!same(shopName, currentShop)) {
                    continue;
                }

                String contactKey =
                        userEmail.toLowerCase();

                if (!contacts.containsKey(contactKey)) {

                    String customerName = userEmail;

                    // SEPARATE USER LOCK:
                    // This is outside the DB.chats lock.

                    final var users = DB.users;

                    synchronized (users) {

                        for (var user : users) {

                            if (user != null
                                    && same(user.email, userEmail)) {

                                if (notBlank(user.name)) {
                                    customerName = user.name;
                                }

                                break;
                            }
                        }
                    }

                    contacts.put(
                            contactKey,
                            new Contact(
                                    customerName,
                                    userEmail,
                                    shopName,
                                    false
                            )
                    );
                }
            }
        }

        return contacts;
    }

    // CHECK ACTIVE REQUEST STATUS

    private static boolean isActive(String status) {

        if (status == null) {
            return false;
        }

        return status.equalsIgnoreCase("Pending")
                || status.equalsIgnoreCase("Accepted")
                || status.equalsIgnoreCase("On Going");
    }

    // MESSAGE TIME

    private static String getLastMessageTime(
            Contact contact) {

        LocalDateTime dateTime =
                getLastMessageDate(contact);

        if (dateTime.equals(LocalDateTime.MIN)) {
            return "";
        }

        return dateTime.format(TIME_FORMAT);
    }

    private static LocalDateTime getLastMessageDate(
            Contact contact) {

        String key = conversationKey(
                contact.userEmail,
                contact.shopName
        );

        final var chats = DB.chats;

        synchronized (chats) {

            List<String> messages =
                    chats.get(key);

            if (messages == null
                    || messages.isEmpty()) {

                return LocalDateTime.MIN;
            }

            String latest =
                    messages.get(messages.size() - 1);

            try {

                if (latest.contains("||")) {

                    String[] parts =
                            latest.split("\\|\\|", 3);

                    if (parts.length > 1) {

                        return LocalDateTime.parse(
                                parts[1]
                        );
                    }
                }

            } catch (Exception ignored) {
            }

            return LocalDateTime.MIN;
        }
    }

    // LATEST MESSAGE PREVIEW

    private static String getLastMessage(
            Contact contact) {

        String key = conversationKey(
                contact.userEmail,
                contact.shopName
        );

        final var chats = DB.chats;

        synchronized (chats) {

            List<String> messages =
                    chats.get(key);

            if (messages == null
                    || messages.isEmpty()) {

                return "Message...";
            }

            String latest =
                    messages.get(messages.size() - 1);

            String preview;

            if (latest.contains("||")) {

                String[] parts =
                        latest.split("\\|\\|", 3);

                preview = parts.length > 2
                        ? parts[2]
                        : "";

            } else {

                int separator =
                        latest.indexOf(": ");

                preview = separator >= 0
                        ? latest.substring(separator + 2)
                        : latest;
            }

            if (preview.length() > 35) {

                preview =
                        preview.substring(0, 35) + "...";
            }

            return preview;
        }
    }

    // FILTER BUTTON STYLE

    private static void styleFilter(
            JButton button,
            boolean selected) {

        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        16
                )
        );

        button.setForeground(Color.BLACK);

        button.setBackground(
                selected ? BLUE : GRAY
        );

        button.setBorder(
                BorderFactory.createEmptyBorder(
                        7, 22, 7, 22
                )
        );

        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );
    }

    // HELPERS

    private static String getInitial(String name) {

        if (name == null
                || name.trim().isEmpty()) {

            return "?";
        }

        return name.trim()
                .substring(0, 1)
                .toUpperCase();
    }

    private static String getCurrentShopName() {

        if (MYSHOP != null
                && notBlank(MYSHOP.name)) {

            return MYSHOP.name.trim();
        }

        if (ME != null
                && notBlank(ME.shop)) {

            return ME.shop.trim();
        }

        return "";
    }

    private static String conversationKey(
            String userEmail,
            String shopName) {

        return "chat|"
                + safe(userEmail).toLowerCase()
                + "|"
                + safe(shopName).toLowerCase();
    }

    private static boolean same(
            String a,
            String b) {

        return a != null
                && b != null
                && a.trim().equalsIgnoreCase(b.trim());
    }

    private static boolean notBlank(String value) {

        return value != null
                && !value.trim().isEmpty();
    }

    private static String safe(String value) {

        return value == null
                ? ""
                : value.trim();
    }

    // CONTACT SHOP

    public static void contact(
            String name,
            String phone) {

        Object[] options = {
            "Copy Number",
            "Message",
            "Close"
        };

        int choice = JOptionPane.showOptionDialog(
                APP,
                "Contact " + name + "\nPhone: " + phone,
                "Contact",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[2]
        );

        if (choice == 0) {

            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(
                            new java.awt.datatransfer.StringSelection(
                                    phone
                            ),
                            null
                    );

            msg("Number copied to clipboard.");

        } else if (choice == 1) {

            open(name);
        }
    }
}
