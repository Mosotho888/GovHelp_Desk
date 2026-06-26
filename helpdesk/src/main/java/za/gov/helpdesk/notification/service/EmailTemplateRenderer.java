package za.gov.helpdesk.notification.service;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.RequiredArgsConstructor;

/**
 * Service component responsible for compiling dynamic email notification content. Wraps the
 * Thymeleaf {@link SpringTemplateEngine} to process structured HTML templates and inject
 * context-specific runtime variable maps.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    /**
     * Renders a targeted HTML template file into a fully processed string payload. Binds a
     * key-value map of parameters into the template engine context wrapper to evaluate inline
     * expressions dynamically.
     *
     * @param templatePath the relative resource file path location of the targeted template
     * @param variables a {@link Map} containing the context parameters and data models to inject
     * @return the processed, fully structured HTML or text document string
     */
    public String render(final String templatePath, final Map<String, Object> variables) {
        final Context ctx = new Context();
        ctx.setVariables(variables);
        return templateEngine.process(templatePath, ctx);
    }
}
