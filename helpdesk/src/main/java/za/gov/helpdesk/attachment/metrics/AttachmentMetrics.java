package za.gov.helpdesk.attachment.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AttachmentMetrics {

    private final Counter uploaded;
    private final Counter downloaded;
    private final Counter deleted;

    private final DistributionSummary uploadBytes;

    public AttachmentMetrics(MeterRegistry registry) {

        this.uploaded = Counter.builder("helpdesk.attachment.uploaded")
                .description("Total files uploaded to tickets")
                .register(registry);

        this.downloaded = Counter.builder("helpdesk.attachment.downloaded")
                .description("Total attachment download events")
                .register(registry);

        this.deleted = Counter.builder("helpdesk.attachment.deleted")
                .description("Total attachments deleted from tickets")
                .register(registry);

        this.uploadBytes = DistributionSummary.builder("helpdesk.attachment.upload.bytes")
                .description("Distribution of uploaded file sizes in bytes")
                .baseUnit("bytes")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void incrementUploaded() {
        this.uploaded.increment();
    }
    public void incrementDownloaded() {
        this.downloaded.increment();
    }

    public void incrementDeleted() {
        this.deleted.increment();
    }

    public void recordUploadedSize(long sizeBytes) {
        this.uploadBytes.record(sizeBytes);
    }
}
