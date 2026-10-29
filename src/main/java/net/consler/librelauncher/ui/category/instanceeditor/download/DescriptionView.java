package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DescriptionView extends JPanel
{
    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().escapeHtml(false).build();

    private final JLabel titleLabel = new JLabel("Select a project");
    private final JEditorPane bodyPane = new JEditorPane();

    public DescriptionView()
    {
        super(new BorderLayout(0, 8));
        setOpaque(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        add(titleLabel, BorderLayout.NORTH);

        bodyPane.setContentType("text/html");
        bodyPane.setEditable(false);
        bodyPane.setOpaque(false);
        bodyPane.setBorder(null);
        bodyPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        JScrollPane bodyScrollPane = new JScrollPane(bodyPane);
        bodyScrollPane.setBorder(null);
        bodyScrollPane.setOpaque(false);
        bodyScrollPane.getViewport().setOpaque(false);
        bodyScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(bodyScrollPane, BorderLayout.CENTER);

        setBorder(new EmptyBorder(8, 10, 8, 4));
        showMessage("Click a project to load its description.");
    }

    public void showLoading(String title)
    {
        titleLabel.setText(title);
        showMessage("Loading description...");
    }

    public void showProject(ModrinthProject project)
    {
        titleLabel.setText(project.title() == null || project.title().isBlank() ? project.slug() : project.title());
        String body = project.body();
        if (body == null || body.isBlank())
        {
            showMessage("No description available.");
            return;
        }

        String renderedBody = HTML_RENDERER.render(MARKDOWN_PARSER.parse(body));
        bodyPane.setText("<html><body>" + renderedBody + "</body></html>");
        bodyPane.setCaretPosition(0);
    }

    public void showError()
    {
        showMessage("Could not load this project's description.");
    }

    public void clear()
    {
        titleLabel.setText("");
        showMessage("");
    }

    public void showMessage(String message)
    {
        bodyPane.setText("<html><body>" + message + "</body></html>");
        bodyPane.setCaretPosition(0);
    }
}
