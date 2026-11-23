package varsystem.ui.user;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.Feedback;
import static varsystem.ui.frame.MainFrame.APP;

public final class FeedbackDialog {

    private FeedbackDialog() { }

    public static void show(String requestId, String shopName) {

        if (DB.hasFeedback(requestId)) {
            JOptionPane.showMessageDialog(
                    APP,
                    "You have already submitted feedback for this request.",
                    "Feedback",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JComboBox<Integer> rating = new JComboBox<>(
                new Integer[]{5, 4, 3, 2, 1}
        );

        JTextArea comment = new JTextArea(5, 30);
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        comment.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JScrollPane scroll = new JScrollPane(comment);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setPreferredSize(new Dimension(350, 230));

        JLabel title = new JLabel("Rate your experience");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel shop = new JLabel("Repair Shop: " + shopName);
        JLabel ratingLabel = new JLabel("Your Rating (1-5 stars):");
        JLabel commentLabel = new JLabel("Your Comment:");

        panel.add(title);
        panel.add(Box.createVerticalStrut(12));
        panel.add(shop);
        panel.add(Box.createVerticalStrut(12));
        panel.add(ratingLabel);
        panel.add(rating);
        panel.add(Box.createVerticalStrut(12));
        panel.add(commentLabel);
        panel.add(scroll);

        int result = JOptionPane.showConfirmDialog(
                APP,
                panel,
                "Submit Feedback",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Feedback feedback = new Feedback(
                "FB-" + requestId,
                requestId,
                shopName,
                Session.ME.email,
                Session.ME.name,
                (Integer) rating.getSelectedItem(),
                comment.getText().trim()
        );

        if (DB.addFeedback(feedback)) {
            varsystem.service.NotificationService.newFeedback(
                    shopName, requestId, Session.ME.name);
            JOptionPane.showMessageDialog(
                    APP,
                    "Thank you! Your feedback has been submitted.",
                    "Feedback Submitted",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                    APP,
                    "Unable to submit feedback. Please check your request status.",
                    "Submission Failed",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }
}
