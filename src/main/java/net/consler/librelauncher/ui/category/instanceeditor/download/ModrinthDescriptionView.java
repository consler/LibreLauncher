package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class ModrinthDescriptionView extends JPanel
{
    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().escapeHtml(false).build();

    private final JLabel titleLabel = new JLabel("Select a project");
    private final JEditorPane bodyPane = new JEditorPane();

    public ModrinthDescriptionView()
    {
        super(new BorderLayout(0, 8));
        setOpaque(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        add(titleLabel, BorderLayout.NORTH);

        bodyPane.setContentType("text/html");
        bodyPane.setEditorKit(new BoundedImageEditorKit());
        bodyPane.setEditable(false);
        bodyPane.setOpaque(false);
        bodyPane.setBorder(null);
        bodyPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        JScrollPane bodyScrollPane = new JScrollPane(bodyPane);
        bodyScrollPane.setBorder(null);
        bodyScrollPane.setOpaque(false);
        bodyScrollPane.getViewport().setOpaque(false);
        bodyScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        bodyScrollPane.getViewport().addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent event)
            {
                bodyPane.revalidate();
                bodyPane.repaint();
            }
        });
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

    private final class BoundedImageEditorKit extends HTMLEditorKit
    {
        private final ViewFactory viewFactory = new HTMLFactory()
        {
            @Override
            public View create(Element element)
            {
                Object name = element.getAttributes().getAttribute(StyleConstants.NameAttribute);
                if (name == HTML.Tag.IMG) return new BoundedImageView(element);
                return super.create(element);
            }
        };

        @Override
        public ViewFactory getViewFactory()
        {
            return viewFactory;
        }
    }

    private final class BoundedImageView extends ImageView
    {
        private BoundedImageView(Element element)
        {
            super(element);
        }

        @Override
        public float getPreferredSpan(int axis)
        {
            float width = super.getPreferredSpan(X_AXIS);
            float height = super.getPreferredSpan(Y_AXIS);
            if (width <= 0 || height <= 0) return super.getPreferredSpan(axis);

            Dimension maxSize = getMaximumImageSize();
            float scale = Math.min(1f, Math.min(maxSize.width / width, maxSize.height / height));
            return super.getPreferredSpan(axis) * scale;
        }

        @Override
        public void paint(Graphics graphics, Shape allocation)
        {
            Image image = getImage();
            int imageWidth = image == null ? -1 : image.getWidth(bodyPane);
            int imageHeight = image == null ? -1 : image.getHeight(bodyPane);
            if (imageWidth <= 0 || imageHeight <= 0)
            {
                super.paint(graphics, allocation);
                return;
            }

            Rectangle bounds = allocation.getBounds();
            Dimension maxSize = getMaximumImageSize();
            double scale = Math.min(1d, Math.min(
                    Math.min((double) maxSize.width / imageWidth, (double) maxSize.height / imageHeight),
                    Math.min((double) bounds.width / imageWidth, (double) bounds.height / imageHeight)));
            int width = Math.max(1, (int) Math.round(imageWidth * scale));
            int height = Math.max(1, (int) Math.round(imageHeight * scale));
            graphics.drawImage(image, bounds.x, bounds.y, width, height, bodyPane);
        }

        private Dimension getMaximumImageSize()
        {
            Container parent = bodyPane.getParent();
            if (parent instanceof JViewport viewport)
            {
                Dimension extent = viewport.getExtentSize();
                if (extent.width > 0 && extent.height > 0)
                    return new Dimension(Math.max(1, extent.width - 16), Math.max(1, extent.height - 16));
            }

            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            return new Dimension(Math.max(1, screen.width * 4 / 5), Math.max(1, screen.height * 4 / 5));
        }
    }
}
