package varsystem.ui.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.Req;
import varsystem.service.NotificationService;
import varsystem.service.NotificationService.NotificationItem;
import static varsystem.ui.frame.MainFrame.APP;
import static varsystem.ui.frame.MainFrame.refreshBadges;

public final class NotificationsDialog {

    private static JDialog dialog;
    private static DefaultListModel<NotificationItem> model;
    private static JList<NotificationItem> list;

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a");

    private NotificationsDialog() {}

    // OPEN NOTIFICATION WINDOW
    public static void show() {

        if (Session.ME == null) {
            JOptionPane.showMessageDialog(
                    APP,
                    "Please log in first.",
                    "Notifications",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        dialog = new JDialog(APP, "Notifications", true);
        dialog.setSize(780, 590);
        dialog.setMinimumSize(new Dimension(640, 460));
        dialog.setLocationRelativeTo(APP);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );
        root.setBackground(new Color(8, 22, 48));

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);

        JLabel title = new JLabel("Notifications");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(new Color(238, 245, 255));

        JLabel role = new JLabel(
                "Account: " + Session.ME.role
        );
        role.setForeground(new Color(160, 190, 225));

        JPanel heading = new JPanel(new BorderLayout(4, 4));
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(role, BorderLayout.SOUTH);

        header.add(heading, BorderLayout.WEST);

        JButton markAll = new JButton("✓  Mark all as read");
        JButton clearAll = new JButton("Clear all");

        styleAction(markAll, new Color(35, 116, 225));
        styleAction(clearAll, new Color(40, 58, 82));

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 7, 0)
        );
        actions.setOpaque(false);
        actions.add(markAll);
        actions.add(clearAll);

        header.add(actions, BorderLayout.EAST);

        model = new DefaultListModel<>();
        list = new JList<>(model);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(100);
        list.setFont(new Font("SansSerif", Font.PLAIN, 13));
        list.setBackground(new Color(12, 31, 62));
        list.setForeground(new Color(235, 242, 255));

        list.setCellRenderer(new DefaultListCellRenderer() {

            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> owner,
                    Object value,
                    int index,
                    boolean selected,
                    boolean focus) {

                JLabel label = (JLabel) super
                        .getListCellRendererComponent(
                                owner,
                                value,
                                index,
                                selected,
                                focus
                        );

                if (value instanceof NotificationItem n) {

                    String time = n.createdAt == null
                            ? ""
                            : n.createdAt.format(FORMAT);

                    String status = n.read ? "" : "  • NEW";

                    label.setText(
                            "<html><div style='padding:8px 7px;line-height:1.35;'>"
                            + "<b style='font-size:14px;color:#EAF3FF;'>"
                            + escape(n.title)
                            + "</b>"
                            + "<span style='color:#55A5FF;font-weight:bold;'>"
                            + escape(status)
                            + "</span><br>"
                            + "<span style='color:#B9CBE0;'>"
                            + escape(n.message)
                            + "</span>"
                            + "<br><span style='color:#7E9AB9;font-size:10px;'>"
                            + escape(time)
                            + "</span></div></html>"
                    );

                    if (!selected) {
                        label.setBackground(
                                n.read
                                        ? new Color(12, 31, 62)
                                        : new Color(17, 48, 89)
                        );
                    }
                }

                label.setBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(
                                        0, 0, 1, 0,
                                        new Color(35, 58, 90)
                                ),
                                BorderFactory.createEmptyBorder(
                                        7, 12, 7, 12
                                )
                        )
                );

                label.setOpaque(true);
                label.setForeground(new Color(235, 242, 255));

                return label;
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(
                BorderFactory.createLineBorder(new Color(35, 58, 90))
        );
        scroll.getViewport().setBackground(new Color(12, 31, 62));

        // MARK ALL AS READ
        markAll.addActionListener(e -> {
            NotificationService.markAllAsRead();
            loadNotifications();
            refreshBadges();
        });

        // CLEAR ALL
        clearAll.addActionListener(e -> {

            int answer = JOptionPane.showConfirmDialog(
                    dialog,
                    "Clear all notifications for this account?",
                    "Clear Notifications",
                    JOptionPane.YES_NO_OPTION
            );

            if (answer == JOptionPane.YES_OPTION) {
                NotificationService.clearAll();
                loadNotifications();
                refreshBadges();
            }
        });

        // DOUBLE CLICK NOTIFICATION
        list.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getClickCount() != 2) {
                    return;
                }

                NotificationItem n = list.getSelectedValue();

                if (n == null
                        || n.id == null
                        || n.id.isBlank()) {
                    return;
                }

                open(n);

                loadNotifications();
                refreshBadges();
            }
        });

        JPanel footer = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );
        footer.setOpaque(false);

        JButton close = new JButton("Close");

        // CLOSE BUTTON
        styleAction(close, new Color(40, 58, 82));

        close.addActionListener(e -> dialog.dispose());
        footer.add(close);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.add(root);

        loadNotifications();

        dialog.setVisible(true);
    }

    // OPEN A SINGLE NOTIFICATION
    public static void open(NotificationItem n) {

        if (n == null) {
            return;
        }

        if (Session.ME == null) {
            JOptionPane.showMessageDialog(
                    APP,
                    "Please log in first.",
                    "Notifications",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (n.id != null && !n.id.isBlank()) {
            NotificationService.markAsRead(n.id);
        }

        openRequest(n);

        refreshBadges();
    }

    // LOAD NOTIFICATIONS
    private static void loadNotifications() {

        if (model == null) {
            return;
        }

        model.clear();

        List<NotificationItem> notifications =
                NotificationService.getNotifications();

        for (NotificationItem n : notifications) {
            model.addElement(n);
        }

        if (notifications.isEmpty()) {

            model.addElement(new NotificationItem(
                    "",
                    "",
                    "",
                    "",
                    "No notifications yet",
                    "Updates about your account will appear here.",
                    null,
                    true
            ));
        }
    }

    // OPEN RELATED REQUEST
    private static void openRequest(NotificationItem n) {

        if (n.requestId == null || n.requestId.isBlank()) {

            JOptionPane.showMessageDialog(
                    dialog != null && dialog.isShowing() ? dialog : APP,
                    n.message,
                    n.title,
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        Req request = DB.reqs.stream()
                .filter(r -> r != null
                        && r.id != null
                        && n.requestId.equalsIgnoreCase(r.id))
                .findFirst()
                .orElse(null);

        if (request == null) {

            JOptionPane.showMessageDialog(
                    dialog != null && dialog.isShowing() ? dialog : APP,
                    "The related request could not be found.",
                    "Request Details",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        RequestDetailDialog.show(
                request,
                "Admin".equalsIgnoreCase(Session.ME.role)
        );
    }

    // BUTTON STYLE
    private static void styleAction(
            JButton button,
            Color color) {

        button.setForeground(Color.BLACK); // UPDATED: BLACK TEXT
        button.setBackground(color);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);

        button.setBorder(
                BorderFactory.createEmptyBorder(
                        10, 14, 10, 14
                )
        );

        button.setCursor(
                java.awt.Cursor.getPredefinedCursor(
                        java.awt.Cursor.HAND_CURSOR
                )
        );
    }

    // HTML ESCAPE
    private static String escape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}