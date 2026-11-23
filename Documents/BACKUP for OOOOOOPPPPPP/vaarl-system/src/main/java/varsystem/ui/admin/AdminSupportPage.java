package varsystem.ui.admin;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import varsystem.data.DB;
import varsystem.data.DB.SupportMessage;
import varsystem.model.Shop;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.*;

public final class AdminSupportPage {

    private AdminSupportPage() { }

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a");

    private static String selectedEmail = "";
    private static String selectedRole = "";

    private static final Color PAGE_BG = NAVY2;
    private static final Color BORDER = new Color(30, 74, 140);
    private static final Color ADMIN_BG = new Color(24, 65, 120);
    private static final Color SENDER_BG = new Color(35, 60, 100);
    private static final Color MUTED = new Color(159, 181, 213);

    private static final Color CONVERSATION_BG =
            new Color(20, 43, 78);

    private static final Color CONVERSATION_SELECTED =
            new Color(24, 65, 120);

    private static final Color CONVERSATION_HOVER =
            new Color(32, 72, 120);

    public static JComponent build() {

        JComponent[] p = page(
                "Support Inbox",
                "View and respond to User and Shop support messages"
        );

        JPanel body = (JPanel) p[1];

        JPanel main = new JPanel(new BorderLayout(14, 0));
        main.setOpaque(false);

        LocalDate[] selectedDate = {null};
        String[] selectedShop = {"All Shops"};

        // =========================================
        // LEFT SIDE - CONVERSATIONS
        // =========================================

        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBackground(NAVY2);

        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JPanel leftHeader = new JPanel(new BorderLayout());
        leftHeader.setOpaque(false);

        JLabel conversationTitle =
                lbl("Conversations", 16, true, Color.WHITE);

        JLabel conversationSubtitle =
                lbl("User and Shop inquiries", 11, false, MUTED);

        JPanel leftTitlePanel = new JPanel();

        leftTitlePanel.setOpaque(false);

        leftTitlePanel.setLayout(
                new BoxLayout(leftTitlePanel, BoxLayout.Y_AXIS)
        );

        leftTitlePanel.add(conversationTitle);
        leftTitlePanel.add(Box.createVerticalStrut(4));
        leftTitlePanel.add(conversationSubtitle);

        leftHeader.add(leftTitlePanel, BorderLayout.CENTER);

        JPanel conversations = new JPanel();

        conversations.setLayout(
                new BoxLayout(conversations, BoxLayout.Y_AXIS)
        );

        conversations.setBackground(CONVERSATION_BG);

        JScrollPane leftScroll = new JScrollPane(conversations);

        leftScroll.setBorder(null);
        leftScroll.setBackground(CONVERSATION_BG);

        leftScroll.getViewport().setBackground(CONVERSATION_BG);
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);

        leftScroll.setPreferredSize(
                new Dimension(300, 450)
        );

        left.add(leftHeader, BorderLayout.NORTH);
        left.add(leftScroll, BorderLayout.CENTER);

        // =========================================
        // RIGHT SIDE - CHAT
        // =========================================

        JPanel chat = new JPanel(new BorderLayout(0, 12));

        chat.setBackground(NAVY2);

        chat.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JPanel chatHeader = new JPanel(new BorderLayout());

        chatHeader.setOpaque(false);

        chatHeader.setBorder(
                new EmptyBorder(0, 0, 12, 0)
        );

        JLabel heading =
                lbl("Select a conversation", 17, true, Color.WHITE);

        JLabel chatSubtitle = lbl(
                "Choose a message from the left to start replying.",
                11,
                false,
                MUTED
        );

        JPanel headingPanel = new JPanel();

        headingPanel.setOpaque(false);

        headingPanel.setLayout(
                new BoxLayout(headingPanel, BoxLayout.Y_AXIS)
        );

        headingPanel.add(heading);
        headingPanel.add(Box.createVerticalStrut(5));
        headingPanel.add(chatSubtitle);

        chatHeader.add(headingPanel, BorderLayout.CENTER);

        JPanel messages = new JPanel();

        messages.setLayout(
                new BoxLayout(messages, BoxLayout.Y_AXIS)
        );

        messages.setBackground(PAGE_BG);

        messages.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        JScrollPane messageScroll = new JScrollPane(messages);

        messageScroll.setBorder(
                BorderFactory.createLineBorder(BORDER)
        );

        messageScroll.getViewport().setBackground(PAGE_BG);

        messageScroll.getVerticalScrollBar().setUnitIncrement(18);

        // =========================================
        // REPLY FIELD
        // =========================================

        JTextField replyField = new JTextField();

        replyField.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );

        replyField.setBackground(PANEL);
        replyField.setForeground(Color.WHITE);
        replyField.setCaretColor(Color.WHITE);

        replyField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(10, 12, 10, 12)
                )
        );

        replyField.setPreferredSize(
                new Dimension(100, 42)
        );

        replyField.setEnabled(false);

        JPanel sendPanel = new JPanel(
                new BorderLayout(10, 0)
        );

        sendPanel.setOpaque(false);

        sendPanel.setBorder(
                new EmptyBorder(8, 0, 0, 0)
        );

        Runnable[] loadMessages = new Runnable[1];
        Runnable[] refreshConversations = new Runnable[1];

        refreshConversations[0] = () -> loadConversations(
                conversations,
                loadMessages,
                selectedDate[0],
                selectedShop[0]
        );

        // =========================================
        // LOAD CHAT MESSAGES
        // =========================================

        loadMessages[0] = () -> {

            messages.removeAll();

            if (selectedEmail == null || selectedEmail.isEmpty()) {

                JLabel empty = lbl(
                        "Select a conversation to view messages.",
                        14,
                        false,
                        MUTED
                );

                empty.setAlignmentX(
                        Component.CENTER_ALIGNMENT
                );

                messages.add(Box.createVerticalGlue());
                messages.add(empty);
                messages.add(Box.createVerticalGlue());

                heading.setText("Select a conversation");

                chatSubtitle.setText(
                        "Choose a message from the left to start replying."
                );

                replyField.setEnabled(false);

            } else {

                replyField.setEnabled(true);

                DB.markSupportReadByAdmin(
                        selectedEmail,
                        selectedRole
                );

                List<SupportMessage> history =
                        DB.getSupportMessages(
                                selectedEmail,
                                selectedRole
                        );

                if (history == null || history.isEmpty()) {

                    JLabel empty = lbl(
                            "No messages in this conversation.",
                            13,
                            false,
                            MUTED
                    );

                    empty.setAlignmentX(
                            Component.CENTER_ALIGNMENT
                    );

                    messages.add(empty);

                } else {

                    SupportMessage firstMessage = null;

                    for (SupportMessage m : history) {

                        if (m != null && !m.fromAdmin) {
                            firstMessage = m;
                            break;
                        }
                    }

                    heading.setText(
                            firstMessage == null
                                    ? "Conversation"
                                    : firstMessage.senderName
                    );

                    chatSubtitle.setText(
                            selectedRole + "  •  " + selectedEmail
                    );

                    for (SupportMessage m : history) {

                        if (m == null) continue;

                        boolean admin = m.fromAdmin;

                        JPanel row = new JPanel(
                                new BorderLayout()
                        );

                        row.setOpaque(false);

                        row.setBorder(
                                new EmptyBorder(3, 0, 3, 0)
                        );

                        JPanel bubble = new JPanel();

                        bubble.setLayout(
                                new BoxLayout(
                                        bubble,
                                        BoxLayout.Y_AXIS
                                )
                        );

                        bubble.setBorder(
                                new EmptyBorder(8, 10, 7, 10)
                        );

                        bubble.setBackground(
                                admin ? ADMIN_BG : SENDER_BG
                        );

                        bubble.setMaximumSize(
                                new Dimension(330, Integer.MAX_VALUE)
                        );

                        bubble.setAlignmentX(
                                admin
                                        ? Component.RIGHT_ALIGNMENT
                                        : Component.LEFT_ALIGNMENT
                        );

                        JLabel sender = lbl(
                                admin ? "Admin" : m.senderName,
                                11,
                                true,
                                Color.WHITE
                        );

                        String messageText = m.message == null
                                ? ""
                                : m.message;

                        JTextArea content = new JTextArea(messageText);

                        content.setLineWrap(true);
                        content.setWrapStyleWord(true);
                        content.setEditable(false);
                        content.setOpaque(false);

                        content.setFont(
                                new Font(
                                        "SansSerif",
                                        Font.PLAIN,
                                        12
                                )
                        );

                        content.setForeground(Color.WHITE);
                        content.setBorder(null);

                        FontMetrics fm =
                                content.getFontMetrics(
                                        content.getFont()
                                );

                        String[] textLines =
                                messageText.split("\n", -1);

                        int longestLineWidth = 0;

                        for (String line : textLines) {

                            longestLineWidth = Math.max(
                                    longestLineWidth,
                                    fm.stringWidth(line)
                            );
                        }

                        int contentWidth = Math.max(
                                45,
                                Math.min(
                                        280,
                                        longestLineWidth + 8
                                )
                        );

                        int rows = 0;

                        for (String line : textLines) {

                            int lineWidth = fm.stringWidth(line);

                            int lineRows = Math.max(
                                    1,
                                    (int) Math.ceil(
                                            lineWidth
                                                    / (double) contentWidth
                                    )
                            );

                            rows += lineRows;
                        }

                        rows = Math.max(1, rows);

                        int textHeight =
                                rows * fm.getHeight() + 4;

                        content.setPreferredSize(
                                new Dimension(
                                        contentWidth,
                                        textHeight
                                )
                        );

                        content.setMinimumSize(
                                new Dimension(
                                        contentWidth,
                                        textHeight
                                )
                        );

                        content.setMaximumSize(
                                new Dimension(
                                        280,
                                        Integer.MAX_VALUE
                                )
                        );

                        JLabel time = lbl(
                                m.sentAt == null
                                        ? ""
                                        : m.sentAt.format(DATE_FORMAT),
                                9,
                                false,
                                MUTED
                        );

                        time.setAlignmentX(
                                Component.LEFT_ALIGNMENT
                        );

                        sender.setAlignmentX(
                                Component.LEFT_ALIGNMENT
                        );

                        content.setAlignmentX(
                                Component.LEFT_ALIGNMENT
                        );

                        JPanel messageHeader =
                                new JPanel(
                                        new BorderLayout(8, 0)
                                );

                        messageHeader.setOpaque(false);

                        messageHeader.add(
                                sender,
                                BorderLayout.CENTER
                        );

                        if (admin) {

                            JButton moreButton = new JButton("⋮");

                            moreButton.setFont(
                                    new Font(
                                            "SansSerif",
                                            Font.BOLD,
                                            16
                                    )
                            );

                            moreButton.setForeground(Color.WHITE);
                            moreButton.setBackground(ADMIN_BG);
                            moreButton.setBorderPainted(false);
                            moreButton.setFocusPainted(false);
                            moreButton.setContentAreaFilled(false);

                            moreButton.setCursor(
                                    new Cursor(Cursor.HAND_CURSOR)
                            );

                            moreButton.setPreferredSize(
                                    new Dimension(24, 22)
                            );

                            moreButton.setToolTipText(
                                    "Message options"
                            );

                            messageHeader.add(
                                    moreButton,
                                    BorderLayout.EAST
                            );

                            Runnable refreshAfterMessageChange = () -> {
                                loadMessages[0].run();
                                refreshConversations[0].run();
                            };

                            addMessagePopup(
                                    moreButton,
                                    m,
                                    refreshAfterMessageChange,
                                    p[0]
                            );

                            addMessagePopup(
                                    bubble,
                                    m,
                                    refreshAfterMessageChange,
                                    p[0]
                            );

                            addMessagePopup(
                                    content,
                                    m,
                                    refreshAfterMessageChange,
                                    p[0]
                            );

                            addMessagePopup(
                                    sender,
                                    m,
                                    refreshAfterMessageChange,
                                    p[0]
                            );

                            addMessagePopup(
                                    time,
                                    m,
                                    refreshAfterMessageChange,
                                    p[0]
                            );
                        }

                        bubble.add(messageHeader);
                        bubble.add(Box.createVerticalStrut(4));
                        bubble.add(content);

                        String recoveryId = admin
                                ? null
                                : DB.recoveryIdFromMessage(messageText);

                        if (recoveryId != null) {

                            JButton reviewButton =
                                    new JButton("Review Recovery Request");

                            reviewButton.setAlignmentX(
                                    Component.LEFT_ALIGNMENT);

                            reviewButton.setCursor(new Cursor(
                                    Cursor.HAND_CURSOR));

                            reviewButton.addActionListener(ev ->
                                    RecoveryReviewDialog.show(
                                            p[0],
                                            recoveryId,
                                            () -> {
                                                loadMessages[0].run();
                                                refreshConversations[0].run();
                                            }));

                            bubble.add(Box.createVerticalStrut(6));
                            bubble.add(reviewButton);
                        }

                        bubble.add(Box.createVerticalStrut(4));
                        bubble.add(time);

                        if (admin) {
                            row.add(bubble, BorderLayout.EAST);
                        } else {
                            row.add(bubble, BorderLayout.WEST);
                        }

                        messages.add(row);
                    }
                }
            }

            messages.revalidate();
            messages.repaint();

            SwingUtilities.invokeLater(() -> {

                JScrollBar bar =
                        messageScroll.getVerticalScrollBar();

                bar.setValue(bar.getMaximum());
            });
        };

        // =========================================
        // FILTER CONTROLS
        // NO YEAR FILTER
        // =========================================

        JPanel filterPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 12, 8)
        );

        filterPanel.setBackground(NAVY2);

        filterPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(5, 10, 5, 10)
                )
        );

        // DATE FILTER

        filterPanel.add(
                lbl(
                        "Select Date:",
                        12,
                        true,
                        Color.WHITE
                )
        );

        JButton dateButton = btn(
                "Select Date",
                BLUE,
                e -> { }
        );

        dateButton.setPreferredSize(
                new Dimension(125, 34)
        );

        JButton clearDateButton = btn(
                "Clear Date",
                new Color(105, 115, 130),
                e -> { }
        );

        clearDateButton.setPreferredSize(
                new Dimension(105, 34)
        );

        filterPanel.add(dateButton);
        filterPanel.add(clearDateButton);

        // SHOP FILTER

        filterPanel.add(
                lbl(
                        "Select Shop:",
                        12,
                        true,
                        Color.WHITE
                )
        );

        JComboBox<String> shopFilter = new JComboBox<>();

        shopFilter.addItem("All Shops");

        Set<String> shopNames = new LinkedHashSet<>();

        for (Shop shop : DB.shops) {

            if (shop != null
                    && shop.name != null
                    && !shop.name.trim().isEmpty()) {

                shopNames.add(shop.name.trim());
            }
        }

        for (String shopName : shopNames) {
            shopFilter.addItem(shopName);
        }

        shopFilter.setPreferredSize(
                new Dimension(180, 34)
        );

        shopFilter.setBackground(PANEL);
        shopFilter.setForeground(Color.BLACK);

        shopFilter.setRenderer(
                new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                );

                setBackground(
                        isSelected ? BLUE : Color.WHITE
                );

                setForeground(Color.BLACK);

                return this;
            }
        });

        filterPanel.add(shopFilter);

        // =========================================
        // FILTER REFRESH
        // =========================================

        Runnable refreshFiltered = () -> {

            selectedEmail = "";
            selectedRole = "";

            loadConversations(
                    conversations,
                    loadMessages,
                    selectedDate[0],
                    selectedShop[0]
            );

            loadMessages[0].run();
        };

        // =========================================
        // DATE SELECTION
        // =========================================

        dateButton.addActionListener(e -> {

            LocalDate initialDate = selectedDate[0] == null
                    ? LocalDate.now()
                    : selectedDate[0];

            LocalDate chosenDate = chooseDate(
                    p[0],
                    initialDate
            );

            if (chosenDate != null) {

                selectedDate[0] = chosenDate;

                dateButton.setText(
                        chosenDate.format(
                                DateTimeFormatter.ofPattern(
                                        "MMM dd, yyyy"
                                )
                        )
                );

                refreshFiltered.run();
            }
        });

        // =========================================
        // CLEAR DATE
        // =========================================

        clearDateButton.addActionListener(e -> {

            selectedDate[0] = null;

            dateButton.setText("Select Date");

            refreshFiltered.run();
        });

        // =========================================
        // SHOP SELECTION
        // =========================================

        shopFilter.addActionListener(e -> {

            selectedShop[0] =
                    String.valueOf(shopFilter.getSelectedItem());

            refreshFiltered.run();
        });

        // =========================================
        // SEND REPLY
        // =========================================

        JButton sendButton = btn(
                "Send Reply",
                BLUE,
                e -> {

                    if (selectedEmail == null
                            || selectedEmail.isEmpty()) {

                        JOptionPane.showMessageDialog(
                                p[0],
                                "Please select a conversation first."
                        );

                        return;
                    }

                    String message = replyField.getText().trim();

                    if (message.isEmpty()) return;

                    boolean sent = DB.replySupportMessage(
                            selectedEmail,
                            selectedRole,
                            message
                    );

                    if (sent) {

                        replyField.setText("");

                        loadMessages[0].run();

                        refreshConversations[0].run();

                    } else {

                        JOptionPane.showMessageDialog(
                                p[0],
                                "Unable to send reply."
                        );
                    }
                }
        );

        sendButton.setPreferredSize(
                new Dimension(125, 42)
        );

        sendPanel.add(
                replyField,
                BorderLayout.CENTER
        );

        sendPanel.add(
                sendButton,
                BorderLayout.EAST
        );

        chat.add(
                chatHeader,
                BorderLayout.NORTH
        );

        chat.add(
                messageScroll,
                BorderLayout.CENTER
        );

        chat.add(
                sendPanel,
                BorderLayout.SOUTH
        );

        replyField.addActionListener(
                e -> sendButton.doClick()
        );

        // =========================================
        // INITIAL DISPLAY
        // =========================================

        main.add(left, BorderLayout.WEST);
        main.add(chat, BorderLayout.CENTER);

        JPanel content = new JPanel(
                new BorderLayout(0, 12)
        );

        content.setOpaque(false);

        content.add(
                filterPanel,
                BorderLayout.NORTH
        );

        content.add(
                main,
                BorderLayout.CENTER
        );

        body.add(
                content,
                BorderLayout.CENTER
        );

        loadConversations(
                conversations,
                loadMessages,
                selectedDate[0],
                selectedShop[0]
        );

        loadMessages[0].run();

        return p[0];
    }

    // =============================================
    // MESSAGE OPTIONS: EDIT AND DELETE
    // =============================================

    private static void addMessagePopup(
            JComponent component,
            SupportMessage message,
            Runnable refresh,
            Component parent) {

        if (component == null
                || message == null
                || !message.fromAdmin) {
            return;
        }

        JPopupMenu popup = new JPopupMenu();

        JMenuItem editItem =
                new JMenuItem("Edit Message");

        JMenuItem deleteItem =
                new JMenuItem("Delete Message");

        // EDIT MESSAGE

        editItem.addActionListener(e -> {

            JTextArea editor = new JTextArea(
                    message.message == null
                            ? ""
                            : message.message,
                    5,
                    30
            );

            editor.setLineWrap(true);
            editor.setWrapStyleWord(true);

            editor.setFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            13
                    )
            );

            JScrollPane scroll = new JScrollPane(editor);

            scroll.setPreferredSize(
                    new Dimension(350, 130)
            );

            int result = JOptionPane.showConfirmDialog(
                    parent,
                    scroll,
                    "Edit Message",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {

                String updatedMessage =
                        editor.getText().trim();

                if (updatedMessage.isEmpty()) {

                    JOptionPane.showMessageDialog(
                            parent,
                            "Message cannot be empty.",
                            "Invalid Message",
                            JOptionPane.WARNING_MESSAGE
                    );

                    return;
                }

                boolean updated = DB.editSupportMessage(
                        message.messageId,
                        updatedMessage
                );

                if (updated) {

                    refresh.run();

                    JOptionPane.showMessageDialog(
                            parent,
                            "Message updated successfully."
                    );

                } else {

                    JOptionPane.showMessageDialog(
                            parent,
                            "Unable to edit message.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // DELETE MESSAGE

        deleteItem.addActionListener(e -> {

            int confirm = JOptionPane.showConfirmDialog(
                    parent,
                    "Are you sure you want to delete this message?",
                    "Delete Message",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {

                boolean deleted = DB.deleteSupportMessage(
                        message.messageId
                );

                if (deleted) {

                    refresh.run();

                    JOptionPane.showMessageDialog(
                            parent,
                            "Message deleted successfully."
                    );

                } else {

                    JOptionPane.showMessageDialog(
                            parent,
                            "Unable to delete message.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        popup.add(editItem);
        popup.add(deleteItem);

        if (component instanceof JButton button) {

            button.addActionListener(e ->
                    popup.show(
                            button,
                            0,
                            button.getHeight()
                    )
            );

        } else {

            component.addMouseListener(
                    new MouseAdapter() {

                private void showPopup(MouseEvent e) {

                    if (e.isPopupTrigger()) {

                        popup.show(
                                e.getComponent(),
                                e.getX(),
                                e.getY()
                        );
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    showPopup(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    showPopup(e);
                }
            });
        }
    }

    // =============================================
    // DATE PICKER
    // =============================================

    private static LocalDate chooseDate(
            Component parent,
            LocalDate initialDate) {

        Date initial = Date.from(
                initialDate
                        .atStartOfDay(
                                ZoneId.systemDefault()
                        )
                        .toInstant()
        );

        SpinnerDateModel model = new SpinnerDateModel(
                initial,
                null,
                null,
                java.util.Calendar.DAY_OF_MONTH
        );

        JSpinner dateSpinner = new JSpinner(model);

        dateSpinner.setEditor(
                new JSpinner.DateEditor(
                        dateSpinner,
                        "MMMM dd, yyyy"
                )
        );

        int result = JOptionPane.showConfirmDialog(
                parent,
                dateSpinner,
                "Select Support Message Date",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {

            Date selected = (Date) dateSpinner.getValue();

            return selected.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        return null;
    }

    // =============================================
    // CHECK FILTERS
    // =============================================

    private static boolean matchesFilters(
            SupportMessage latest,
            String role,
            String senderName,
            LocalDate date,
            String shop) {

        if (latest == null || latest.sentAt == null) {
            return false;
        }

        // DATE FILTER

        if (date != null
                && !latest.sentAt.toLocalDate().equals(date)) {

            return false;
        }

        // SHOP FILTER

        if (shop != null
                && !"All Shops".equals(shop)) {

            if (!"shop".equalsIgnoreCase(role)) {
                return false;
            }

            if (senderName == null
                    || !senderName.equalsIgnoreCase(shop)) {

                return false;
            }
        }

        return true;
    }

    // =============================================
    // LOAD CONVERSATIONS
    // =============================================

    private static void loadConversations(
            JPanel panel,
            Runnable[] loadMessages,
            LocalDate selectedDate,
            String selectedShop) {

        panel.removeAll();

        List<SupportMessage> conversations =
                DB.getSupportConversations();

        if (conversations == null) {
            conversations = Collections.emptyList();
        }

        int displayedCount = 0;

        boolean selectedFound = false;

        for (SupportMessage latest : conversations) {

            if (latest == null
                    || latest.conversationId == null) {
                continue;
            }

            String[] parts =
                    latest.conversationId.split("\\|", 2);

            if (parts.length != 2) continue;

            String role = parts[0];
            String email = parts[1];

            List<SupportMessage> history =
                    DB.getSupportMessages(email, role);

            if (history == null || history.isEmpty()) {
                continue;
            }

            SupportMessage originalSender = null;

            for (SupportMessage message : history) {

                if (message != null && !message.fromAdmin) {

                    originalSender = message;
                    break;
                }
            }

            String senderName = originalSender == null
                    ? "Unknown Sender"
                    : originalSender.senderName;

            if (!matchesFilters(
                    latest,
                    role,
                    senderName,
                    selectedDate,
                    selectedShop)) {

                continue;
            }

            displayedCount++;

            boolean selected =
                    email.equalsIgnoreCase(
                            selectedEmail == null
                                    ? ""
                                    : selectedEmail
                    )
                    && role.equalsIgnoreCase(
                            selectedRole == null
                                    ? ""
                                    : selectedRole
                    );

            if (selected) {
                selectedFound = true;
            }

            String senderEmail = email;

            int unread = 0;

            for (SupportMessage message : history) {

                if (message != null
                        && !message.fromAdmin
                        && !message.readByAdmin) {

                    unread++;
                }
            }

            JButton item = new JButton() {

                @Override
                protected void paintComponent(Graphics g) {

                    Graphics2D g2 =
                            (Graphics2D) g.create();

                    Color bg = selected
                            ? CONVERSATION_SELECTED
                            : CONVERSATION_BG;

                    if (getModel().isRollover() && !selected) {
                        bg = CONVERSATION_HOVER;
                    }

                    g2.setColor(bg);

                    g2.fillRect(
                            0,
                            0,
                            getWidth(),
                            getHeight()
                    );

                    g2.dispose();

                    super.paintComponent(g);
                }
            };

            item.setLayout(
                    new BorderLayout(8, 5)
            );

            item.setOpaque(false);
            item.setContentAreaFilled(false);
            item.setBorderPainted(true);
            item.setFocusPainted(false);
            item.setRolloverEnabled(true);
            item.setForeground(Color.WHITE);

            item.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDER),
                            new EmptyBorder(11, 10, 11, 10)
                    )
            );

            item.setMaximumSize(
                    new Dimension(
                            Integer.MAX_VALUE,
                            95
                    )
            );

            item.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            JPanel details = new JPanel();

            details.setOpaque(false);

            details.setLayout(
                    new BoxLayout(
                            details,
                            BoxLayout.Y_AXIS
                    )
            );

            JPanel nameRow = new JPanel(
                    new BorderLayout(5, 0)
            );

            nameRow.setOpaque(false);

            JLabel name = lbl(
                    senderName,
                    13,
                    true,
                    Color.WHITE
            );

            JLabel roleLabel = lbl(
                    role,
                    10,
                    true,
                    new Color(125, 180, 255)
            );

            nameRow.add(
                    name,
                    BorderLayout.CENTER
            );

            nameRow.add(
                    roleLabel,
                    BorderLayout.EAST
            );

            JLabel emailLabel = lbl(
                    senderEmail,
                    10,
                    false,
                    MUTED
            );

            String preview = latest.message == null
                    ? ""
                    : latest.message.replace("\n", " ");

            if (preview.length() > 38) {
                preview = preview.substring(0, 38) + "...";
            }

            String previewText =
                    (latest.fromAdmin ? "You: " : "") + preview;

            JLabel previewLabel = lbl(
                    previewText,
                    11,
                    false,
                    MUTED
            );

            details.add(nameRow);
            details.add(Box.createVerticalStrut(5));
            details.add(emailLabel);
            details.add(Box.createVerticalStrut(5));
            details.add(previewLabel);

            item.add(
                    details,
                    BorderLayout.CENTER
            );

            // UNREAD BADGE

            if (unread > 0) {

                JLabel unreadBadge = lbl(
                        unread > 99
                                ? "99+"
                                : String.valueOf(unread),
                        10,
                        true,
                        Color.WHITE
                );

                unreadBadge.setOpaque(true);
                unreadBadge.setBackground(BLUE);

                unreadBadge.setHorizontalAlignment(
                        SwingConstants.CENTER
                );

                unreadBadge.setBorder(
                        new EmptyBorder(4, 8, 4, 8)
                );

                item.add(
                        unreadBadge,
                        BorderLayout.EAST
                );
            }

            // SELECT CONVERSATION

            item.addActionListener(e -> {

                selectedEmail = senderEmail;
                selectedRole = role;

                loadConversations(
                        panel,
                        loadMessages,
                        selectedDate,
                        selectedShop
                );

                loadMessages[0].run();
            });

            // =====================================
            // DELETE ENTIRE CONVERSATION
            // =====================================

            JPopupMenu conversationPopup =
                    new JPopupMenu();

            JMenuItem deleteConversationItem =
                    new JMenuItem("Delete Conversation");

            deleteConversationItem.addActionListener(e -> {

                int confirm = JOptionPane.showConfirmDialog(
                        panel,
                        """
                        Are you sure you want to delete this entire conversation?
                        All messages in this conversation will be deleted.
                        """,
                        "Delete Conversation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {

                    boolean deleted =
                            DB.deleteSupportConversation(
                                    senderEmail,
                                    role
                            );

                    if (deleted) {

                        if (senderEmail.equalsIgnoreCase(
                                selectedEmail == null
                                        ? ""
                                        : selectedEmail)
                                && role.equalsIgnoreCase(
                                selectedRole == null
                                        ? ""
                                        : selectedRole)) {

                            selectedEmail = "";
                            selectedRole = "";
                        }

                        loadConversations(
                                panel,
                                loadMessages,
                                selectedDate,
                                selectedShop
                        );

                        loadMessages[0].run();

                        JOptionPane.showMessageDialog(
                                panel,
                                "Conversation deleted successfully."
                        );

                    } else {

                        JOptionPane.showMessageDialog(
                                panel,
                                "Unable to delete conversation.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            });

            conversationPopup.add(
                    deleteConversationItem
            );

            item.addMouseListener(
                    new MouseAdapter() {

                private void showPopup(MouseEvent e) {

                    if (e.isPopupTrigger()) {

                        conversationPopup.show(
                                e.getComponent(),
                                e.getX(),
                                e.getY()
                        );
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    showPopup(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    showPopup(e);
                }
            });

            panel.add(item);

            panel.add(
                    Box.createVerticalStrut(5)
            );
        }

        // =========================================
        // EMPTY RESULT
        // =========================================

        if (displayedCount == 0) {

            JLabel empty = lbl(
                    "No conversations match the selected filters.",
                    13,
                    false,
                    MUTED
            );

            empty.setBorder(
                    new EmptyBorder(15, 5, 15, 5)
            );

            panel.add(empty);
        }

        // =========================================
        // CHECK SELECTED CONVERSATION
        // =========================================

        if (!selectedFound
                && selectedEmail != null
                && !selectedEmail.isEmpty()) {

            selectedEmail = "";
            selectedRole = "";

            loadMessages[0].run();
        }

        panel.revalidate();
        panel.repaint();
    }
}