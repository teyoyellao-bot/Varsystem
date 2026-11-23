
package varsystem.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.LayoutManager2;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Responsive utilities for VAARL System.
 *
 * Intended for User and Mechanic pages.
 * Does not automatically change Admin or Shop Manager pages.
 */
public final class Responsive {

    private Responsive() {
    }

    // ==================================================
    // SCREEN BREAKPOINTS
    // ==================================================

    public static final int PHONE_MAX = 820;
    public static final int TABLET_MAX = 1100;

    // ==================================================
    // RESPONSIVE ROLE CHECK
    // ==================================================

    public static boolean isResponsiveRole() {

        if (varsystem.data.Session.ME == null) {
            return false;
        }

        String role = varsystem.data.Session.ME.role;

        return "User".equalsIgnoreCase(role)
                || "Mechanic".equalsIgnoreCase(role);
    }

    // ==================================================
    // SCREEN SIZE CHECK
    // ==================================================

    public static boolean isPhone(Component component) {

        if (component == null) {
            return false;
        }

        return component.getWidth() < PHONE_MAX;
    }

    public static boolean isTablet(Component component) {

        if (component == null) {
            return false;
        }

        int width = component.getWidth();

        return width >= PHONE_MAX
                && width < TABLET_MAX;
    }

    public static boolean isDesktop(Component component) {

        if (component == null) {
            return false;
        }

        return component.getWidth() >= TABLET_MAX;
    }

    // ==================================================
    // RESPONSIVE GAP
    // ==================================================

    public static int gap(Component component) {

        if (isPhone(component)) {
            return 8;
        }

        if (isTablet(component)) {
            return 12;
        }

        return 16;
    }

    // ==================================================
    // RESPONSIVE PADDING
    // ==================================================

    public static Insets padding(Component component) {

        if (isPhone(component)) {
            return new Insets(8, 8, 8, 8);
        }

        if (isTablet(component)) {
            return new Insets(12, 12, 12, 12);
        }

        return new Insets(16, 20, 16, 20);
    }

    // ==================================================
    // WRAPPING PANEL
    // ==================================================

    /**
     * Components automatically move to another row when
     * there is not enough horizontal space.
     */
    public static JPanel wrapPanel() {
        return wrapPanel(FlowLayout.LEFT, 10, 10);
    }

    public static JPanel wrapPanel(
            int alignment,
            int horizontalGap,
            int verticalGap) {

        JPanel panel = new JPanel(
                new WrapLayout(
                        alignment,
                        horizontalGap,
                        verticalGap
                )
        );

        panel.setOpaque(false);

        return panel;
    }

    // ==================================================
    // WRAPPING BUTTON PANEL
    // ==================================================

    /**
     * Use this for groups of buttons.
     * Buttons wrap automatically on narrow windows.
     */
    public static JPanel buttons(
            Component... components) {

        JPanel panel = wrapPanel();

        if (components != null) {
            for (Component component : components) {
                if (component != null) {
                    panel.add(component);
                }
            }
        }

        return panel;
    }

    // ==================================================
    // RESPONSIVE SCROLL
    // ==================================================

    /**
     * Creates a scroll pane that follows the available width.
     */
    public static JScrollPane scroll(
            JComponent content) {

        JScrollPane scroll = new JScrollPane(content);

        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        scroll.getViewport().setScrollMode(
                javax.swing.JViewport.BLIT_SCROLL_MODE
        );

        return scroll;
    }

    // ==================================================
    // RESPONSIVE WIDTH
    // ==================================================

    public static int availableWidth(
            Container parent) {

        if (parent == null) {
            return 0;
        }

        Insets insets = parent.getInsets();

        return Math.max(
                0,
                parent.getWidth()
                        - insets.left
                        - insets.right
        );
    }

    // ==================================================
    // RESPONSIVE COMPONENT SIZE
    // ==================================================

    public static Dimension size(
            Component parent,
            int desktopWidth,
            int desktopHeight,
            int phoneWidth,
            int phoneHeight) {

        if (isPhone(parent)) {
            return new Dimension(
                    phoneWidth,
                    phoneHeight
            );
        }

        return new Dimension(
                desktopWidth,
                desktopHeight
        );
    }

    // ==================================================
    // RESPONSIVE GRID COLUMN COUNT
    // ==================================================

    /**
     * Returns a recommended column count based on
     * the available container width.
     */
    public static int columns(
            Component parent,
            int desktopColumns) {

        if (parent == null) {
            return desktopColumns;
        }

        int width = parent.getWidth();

        if (width < 480) {
            return 1;
        }

        if (width < PHONE_MAX) {
            return 2;
        }

        if (width < TABLET_MAX) {
            return Math.min(3, desktopColumns);
        }

        return desktopColumns;
    }

    // ==================================================
    // RESPONSIVE COMPONENT REFRESH
    // ==================================================

    /**
     * Revalidate and repaint a component after layout changes.
     */
    public static void refresh(
            JComponent component) {

        if (component == null) {
            return;
        }

        component.revalidate();
        component.repaint();
    }

    // ==================================================
    // WRAP LAYOUT
    // ==================================================

    /**
     * FlowLayout with a preferred height that accounts
     * for multiple wrapped rows.
     */
    public static class WrapLayout
            extends FlowLayout
            implements LayoutManager2 {

        public WrapLayout() {
            super();
        }

        public WrapLayout(int align) {
            super(align);
        }

        public WrapLayout(
                int align,
                int hgap,
                int vgap) {

            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(
                Container target) {

            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(
                Container target) {

            Dimension minimum = layoutSize(target, false);

            minimum.width -=
                    (getHgap() + 1);

            return minimum;
        }

        private Dimension layoutSize(
                Container target,
                boolean preferred) {

            synchronized (target.getTreeLock()) {

                int targetWidth = target.getWidth();

                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                int maxWidth = targetWidth;

                Insets insets = target.getInsets();

                maxWidth -=
                        insets.left
                        + insets.right
                        + getHgap() * 2;

                int width = 0;
                int height = 0;

                int rowWidth = 0;
                int rowHeight = 0;

                int visibleCount = 0;

                for (Component component :
                        target.getComponents()) {

                    if (!component.isVisible()) {
                        continue;
                    }

                    Dimension size = preferred
                            ? component.getPreferredSize()
                            : component.getMinimumSize();

                    if (rowWidth > 0
                            && rowWidth + getHgap()
                                    + size.width > maxWidth) {

                        width = Math.max(
                                width,
                                rowWidth
                        );

                        height +=
                                rowHeight
                                + getVgap();

                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth > 0) {
                        rowWidth += getHgap();
                    }

                    rowWidth += size.width;

                    rowHeight = Math.max(
                            rowHeight,
                            size.height
                    );

                    visibleCount++;
                }

                if (visibleCount > 0) {

                    width = Math.max(
                            width,
                            rowWidth
                    );

                    height += rowHeight;
                }

                height +=
                        insets.top
                        + insets.bottom
                        + getVgap() * 2;

                width +=
                        insets.left
                        + insets.right
                        + getHgap() * 2;

                return new Dimension(
                        width,
                        height
                );
            }
        }

        @Override
        public void addLayoutComponent(
                Component component,
                Object constraints) {
        }

        @Override
        public void addLayoutComponent(
                String name,
                Component component) {
        }

        @Override
        public void removeLayoutComponent(
                Component component) {
        }

        @Override
        public void invalidateLayout(
                Container target) {
        }

        @Override
        public Dimension maximumLayoutSize(
                Container target) {

            return new Dimension(
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE
            );
        }

        @Override
        public float getLayoutAlignmentX(
                Container target) {

            return 0.0f;
        }

        @Override
        public float getLayoutAlignmentY(
                Container target) {

            return 0.0f;
        }
    }
}
