package org.example.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class UiHelpers {

    public static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setForeground(Colors.COLOR_TEXT_GRAY);
        return l;
    }

    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return l;
    }

    public static JSpinner spinnerField(int value) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, 0, 9999, 1));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
        return s;
    }

    public static void styleField(JTextField f) {
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        f.setBorder(new CompoundBorder(new LineBorder(Colors.COLOR_BORDER), new EmptyBorder(2, 6, 2, 6)));
    }

    /**
     * Wraps text that starts with a Unicode icon (▷ ⏸ ■ ↺ ⇄ ⚙ ● ✕ ✓ ⏱ ▣ ...) so the
     * icon renders with "Segoe UI Emoji" while the rest keeps the component's own font.
     * "Segoe UI" alone is missing these glyphs on Windows, which shows up as blank boxes.
     */
    public static String iconText(String text) {
        if (text == null || text.isEmpty()) return text;
        int cp = text.codePointAt(0);
        int charCount = Character.charCount(cp);
        String icon = text.substring(0, charCount);
        String rest = escapeHtml(text.substring(charCount));
        StringBuilder sb = new StringBuilder();
        sb.append("<html><span style='font-family:Segoe UI Emoji;'>");
        sb.append(icon);
        sb.append("</span>");
        sb.append(rest);
        sb.append("</html>");
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
