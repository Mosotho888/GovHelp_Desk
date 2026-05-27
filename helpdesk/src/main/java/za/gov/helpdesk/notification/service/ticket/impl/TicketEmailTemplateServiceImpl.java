package za.gov.helpdesk.notification.service.ticket.impl;

import org.springframework.stereotype.Service;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.notification.service.ticket.TicketEmailTemplateService;

@Service
public class TicketEmailTemplateServiceImpl implements TicketEmailTemplateService {

    @Override
    public String ticketCreatedCustomer(TicketEmailNotificationMessage msg) {
        return baseTemplate(
                "Ticket Created",
                msg.getTicketNumber(),
                "<p>Dear <strong>" + msg.getCustomerName() + "</strong>,</p>" +
                        "<p>Your support ticket has been successfully created. " +
                        "Our team will review it shortly and get back to you.</p>" +
                        detailsTable(msg)
        );
    }

    @Override
    public String ticketCreatedAgent(TicketEmailNotificationMessage msg) {
        return baseTemplate(
                "New Ticket Submitted",
                msg.getTicketNumber(),
                "<p>Dear <strong>" + msg.getAgentName() + "</strong>,</p>" +
                        "<p>A new support ticket has been submitted and assigned to you. " +
                        "Please log in to the helpdesk portal to review and respond.</p>" +
                        detailsTable(msg)
        );
    }

    @Override
    public String ticketAssignedCustomer(TicketEmailNotificationMessage msg) {
        return baseTemplate(
                "Ticket Assigned",
                msg.getTicketNumber(),
                "<p>Dear <strong>" + msg.getCustomerName() + "</strong>,</p>" +
                        "<p>Your ticket has been assigned to <strong>" + msg.getAgentName() + "</strong>, " +
                        "who will be in touch with you soon.</p>" +
                        detailsTable(msg)
        );
    }

    @Override
    public String ticketAssignedAgent(TicketEmailNotificationMessage msg) {
        return baseTemplate(
                "Ticket Assigned to You",
                msg.getTicketNumber(),
                "<p>Dear <strong>" + msg.getAgentName() + "</strong>,</p>" +
                        "<p>You have been assigned the following support ticket. " +
                        "Please review and respond as soon as possible.</p>" +
                        detailsTable(msg)
        );
    }

    @Override
    public String statusChangedCustomer(TicketEmailNotificationMessage msg) {
        return baseTemplate(
                "Ticket Status Updated",
                msg.getTicketNumber(),
                "<p>Dear <strong>" + msg.getCustomerName() + "</strong>,</p>" +
                        "<p>The status of your ticket has been updated to " +
                        statusBadge(msg.getTicketStatus()) + ".</p>" +
                        detailsTable(msg)
        );
    }

    @Override
    public String commentAddedCustomer(TicketEmailNotificationMessage msg) {
        return baseTemplate(
                "New Reply on Your Ticket",
                msg.getTicketNumber(),
                "<p>Dear <strong>" + msg.getCustomerName() + "</strong>,</p>" +
                        "<p>A new reply has been added to your ticket:</p>" +
                        commentBox(msg.getComment()) +
                        detailsTable(msg)
        );
    }

    @Override
    public String commentAddedAgent(TicketEmailNotificationMessage msg) {
        return baseTemplate(
                "Customer Replied",
                msg.getTicketNumber(),
                "<p>Dear <strong>" + msg.getAgentName() + "</strong>,</p>" +
                        "<p>The customer has added a reply on ticket " +
                        "<strong>" + msg.getTicketNumber() + "</strong>:</p>" +
                        commentBox(msg.getComment()) +
                        detailsTable(msg)
        );
    }

    @Override
    public String ticketClosedCustomer(TicketEmailNotificationMessage msg) {
        return baseTemplate(
                "Ticket Closed",
                msg.getTicketNumber(),
                "<p>Dear <strong>" + msg.getCustomerName() + "</strong>,</p>" +
                        "<p>Your support ticket has been resolved and closed. " +
                        "If your issue has not been fully addressed, please open a new ticket " +
                        "and reference <strong>" + msg.getTicketNumber() + "</strong>.</p>" +
                        (msg.getComment() != null
                                ? "<p><strong>Resolution note:</strong></p>" + commentBox(msg.getComment())
                                : "") +
                        detailsTable(msg)
        );
    }

    private String detailsTable(TicketEmailNotificationMessage msg) {
        return "<table>" +
                row("Ticket Number", msg.getTicketNumber()) +
                row("Subject",       msg.getTicketSubject()) +
                row("Status",        formatStatus(msg.getTicketStatus())) +
                row("Priority",      formatPriority(msg.getTicketPriority())) +
                "</table>";
    }

    private String row(String label, String value) {
        return "<tr><td>" + label + "</td>" +
                "<td>" + (value != null ? value : "—") + "</td></tr>";
    }

    private String commentBox(String comment) {
        return "<div class='comment-box'>" +
                (comment != null ? comment : "") +
                "</div>";
    }

    private String statusBadge(String status) {
        String cssClass = switch (status != null ? status : "") {
            case "IN_PROGRESS" -> "badge-progress";
            case "RESOLVED"    -> "badge-resolved";
            case "CLOSED"      -> "badge-closed";
            default            -> "badge-open";
        };
        return "<span class='" + cssClass + "'>" + formatStatus(status) + "</span>";
    }

    private String formatStatus(String status) {
        if (status == null) return "—";
        return switch (status) {
            case "OPEN"        -> "Open";
            case "IN_PROGRESS" -> "In Progress";
            case "RESOLVED"    -> "Resolved";
            case "CLOSED"      -> "Closed";
            default            -> status;
        };
    }

    private String formatPriority(String priority) {
        if (priority == null) return "—";
        return switch (priority) {
            case "LOW"      -> "🟢 Low";
            case "MEDIUM"   -> "🟡 Medium";
            case "HIGH"     -> "🔴 High";
            case "URGENT" -> "🚨 Urgent";
            default         -> priority;
        };
    }

    private String baseTemplate(String heading, String ticketNumber, String body) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8"/>
          %s
        </head>
        <body>
          <div class="wrapper">
            <div class="container">
              <div class="header">
                <h2>%s</h2>
                <span>Reference: %s</span>
              </div>
              <div class="body">
                %s
              </div>
              <div class="footer">
                This is an automated message from the Government Helpdesk System.
                Please do not reply directly to this email.
              </div>
            </div>
          </div>
        </body>
        </html>
        """.formatted(styles(), heading, ticketNumber, body);
    }

    private String styles() {
        return """
        <style>
          body{font-family:Arial,sans-serif;color:#333;margin:0;padding:0;background:#f4f4f4;}
          .wrapper{max-width:620px;margin:32px auto;}
          .container{background:#fff;border-radius:8px;overflow:hidden;
                      border:1px solid #e0e0e0;}
          .header{background:#1a56db;color:#fff;padding:20px 28px;}
          .header h2{margin:0 0 4px;font-size:20px;}
          .header span{font-size:13px;opacity:0.85;}
          .body{padding:28px;}
          .body p{margin:0 0 14px;line-height:1.6;font-size:15px;}
          table{width:100%;border-collapse:collapse;margin-top:16px;}
          td{padding:10px 14px;border:1px solid #e0e0e0;font-size:14px;}
          td:first-child{background:#f7f7f7;font-weight:bold;width:38%;}
          .comment-box{background:#f0f4ff;border-left:4px solid #1a56db;
                       padding:14px 18px;margin:16px 0;border-radius:0 6px 6px 0;
                       font-style:italic;font-size:14px;line-height:1.6;}
          .badge-open    {background:#dbeafe;color:#1d4ed8;padding:2px 10px;
                          border-radius:12px;font-size:13px;font-weight:bold;}
          .badge-progress{background:#fef9c3;color:#92400e;padding:2px 10px;
                          border-radius:12px;font-size:13px;font-weight:bold;}
          .badge-resolved{background:#dcfce7;color:#166534;padding:2px 10px;
                          border-radius:12px;font-size:13px;font-weight:bold;}
          .badge-closed  {background:#f3f4f6;color:#374151;padding:2px 10px;
                          border-radius:12px;font-size:13px;font-weight:bold;}
          .footer{background:#f7f7f7;padding:16px 28px;font-size:12px;
                  color:#999;border-top:1px solid #e0e0e0;}
        </style>
    """;
    }
}
