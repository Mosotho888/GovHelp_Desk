package za.gov.helpdesk.notification.service.auth.impl;

import org.springframework.stereotype.Service;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.notification.service.auth.AuthEmailTemplateService;
import za.gov.helpdesk.notification.service.ticket.TicketEmailTemplateService;

@Service
public class AuthEmailTemplateServiceImpl implements AuthEmailTemplateService {

    @Override
    public String passwordResetOtp(PasswordResetEmailNotificationMessage message) {
        return baseTemplate(
                "Password Reset Request",
                "SYSTEM",
                "<p>Dear <strong>" + message.getActorName() + "</strong>,</p>" +
                        "<p>We received a request to reset your password. " +
                        "Use the OTP code below to complete the process:</p>" +
                        "<div style='text-align:center;margin:32px 0;'>" +
                        "<span style='font-size:36px;font-weight:bold;letter-spacing:12px;" +
                        "color:#1a56db;background:#f0f4ff;padding:16px 24px;" +
                        "border-radius:8px;display:inline-block;'>" +
                        message.getOtp() +
                        "</span>" +
                        "</div>" +
                        "<p style='text-align:center;color:#666;font-size:14px;'>" +
                        "This code expires in <strong>" + message.getOptExpiryMin() + " minutes</strong>." +
                        "</p>" +
                        "<p style='text-align:center;color:#999;font-size:13px;'>" +
                        "If you did not request a password reset, ignore this email. " +
                        "Your account remains secure." +
                        "</p>"
        );
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
