package za.gov.helpdesk.comment.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

@Component
@Getter
public class CommentMetrics {

    private final Counter added;
    private final Counter internalNoteAdded;
    private final Counter edited;
    private final Counter deleted;

    public CommentMetrics(final MeterRegistry registry) {

        this.added =
                Counter.builder("helpdesk.comment.added")
                        .description("Public comments and replies posted on tickets")
                        .register(registry);

        this.internalNoteAdded =
                Counter.builder("helpdesk.comment.internal.note.added")
                        .description(
                                "Internal agent notes posted on tickets (not visible to end users)")
                        .register(registry);

        this.edited =
                Counter.builder("helpdesk.comment.edited")
                        .description("Comment body updates")
                        .register(registry);

        this.deleted =
                Counter.builder("helpdesk.comment.deleted")
                        .description("Comments removed from tickets")
                        .register(registry);
    }

    public void incrementAdded() {
        this.added.increment();
    }

    public void incrementInternalNoteAdded() {
        this.internalNoteAdded.increment();
    }

    public void incrementEdited() {
        this.edited.increment();
    }

    public void incrementDeleted() {
        this.deleted.increment();
    }
}
