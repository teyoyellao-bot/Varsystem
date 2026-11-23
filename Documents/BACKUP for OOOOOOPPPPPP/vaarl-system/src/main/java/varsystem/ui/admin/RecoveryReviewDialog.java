package varsystem.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import varsystem.data.DB;
import varsystem.model.RecoveryRequest;

/** Admin-only review of an account recovery request and its proof photo. */
public final class RecoveryReviewDialog {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a");

    private RecoveryReviewDialog() { }

    public static void show(Component parent, String recoveryId,
                            Runnable afterDecision) {

        RecoveryRequest r = DB.getRecovery(recoveryId);

        if (r == null) {
            JOptionPane.showMessageDialog(parent,
                    "This recovery request no longer exists.",
                    "Recovery Request", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window owner = parent == null
                ? null : SwingUtilities.getWindowAncestor(parent);

        JDialog dialog = new JDialog(owner, "Account Recovery Request",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        // ---- details ----
        JTextArea details = new JTextArea(
                "Name: " + r.accountName
                + "\nAccount type: " + r.accountRole
                + "\nUsername: " + r.username
                + "\nEmail: " + r.accountEmail
                + "\nPhone: " + (r.phone == null || r.phone.isBlank()
                        ? "Not provided" : r.phone)
                + "\nSubmitted: " + r.submittedAt.format(FMT)
                + "\nStatus: " + r.status
                + (r.decidedAt == null ? ""
                        : "\nDecided: " + r.decidedAt.format(FMT))
                + (r.adminNote == null || r.adminNote.isBlank() ? ""
                        : "\nNote: " + r.adminNote)
                + (r.resetCompleted
                        ? "\nPassword reset: completed" : ""));
        details.setEditable(false);
        details.setOpaque(false);
        details.setFont(new Font("SansSerif", Font.PLAIN, 13));

        // ---- proof photo ----
        JLabel photo = new JLabel("", SwingConstants.CENTER);
        photo.setPreferredSize(new Dimension(380, 280));
        photo.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        BufferedImage full = null;

        if (r.proofPhotoPath != null && !r.proofPhotoPath.isBlank()) {
            try {
                full = ImageIO.read(new File(r.proofPhotoPath));
            } catch (IOException ignored) { }
        }

        if (full != null) {
            double scale = Math.min(380.0 / full.getWidth(),
                    280.0 / full.getHeight());
            scale = Math.min(scale, 1.0);
            photo.setIcon(new ImageIcon(full.getScaledInstance(
                    Math.max(1, (int) (full.getWidth() * scale)),
                    Math.max(1, (int) (full.getHeight() * scale)),
                    Image.SCALE_SMOOTH)));
            photo.setToolTipText("Click to view full size");
            photo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            final BufferedImage big = full;

            photo.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JScrollPane scroll = new JScrollPane(
                            new JLabel(new ImageIcon(big)));
                    scroll.setPreferredSize(new Dimension(
                            Math.min(big.getWidth() + 20, 900),
                            Math.min(big.getHeight() + 20, 650)));
                    JOptionPane.showMessageDialog(dialog, scroll,
                            "Proof Photo", JOptionPane.PLAIN_MESSAGE);
                }
            });
        } else {
            photo.setText(r.resetCompleted
                    ? "Proof photo deleted after password reset."
                    : "Proof photo is missing or cannot be opened.");
        }

        // ---- actions ----
        JButton approve = new JButton("Approve");
        JButton reject = new JButton("Reject");
        JButton close = new JButton("Close");

        boolean pending = RecoveryRequest.PENDING.equals(r.status);
        approve.setEnabled(pending && full != null);
        reject.setEnabled(pending);

        approve.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(dialog,
                    "Approve password reset for " + r.accountName + "?\n\n"
                    + "Only approve if the photo and details prove the "
                    + "requester owns this account.",
                    "Confirm Approval", JOptionPane.YES_NO_OPTION);

            if (ok == JOptionPane.YES_OPTION
                    && DB.decideRecovery(r.id, true, "")) {
                dialog.dispose();
                if (afterDecision != null) afterDecision.run();
            }
        });

        reject.addActionListener(e -> {
            String reason = JOptionPane.showInputDialog(dialog,
                    "Reason for rejection (shown to the user):",
                    "Reject Request", JOptionPane.QUESTION_MESSAGE);

            if (reason == null) return;

            if (reason.isBlank()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter a reason.", "Reject Request",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (DB.decideRecovery(r.id, false, reason)) {
                dialog.dispose();
                if (afterDecision != null) afterDecision.run();
            }
        });

        close.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(approve);
        buttons.add(reject);
        buttons.add(close);

        root.add(details, BorderLayout.NORTH);
        root.add(photo, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
