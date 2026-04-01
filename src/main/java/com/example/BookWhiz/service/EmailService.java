package com.example.BookWhiz.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends HTML emails asynchronously so they never block the request thread.
 *
 * Configure in application.properties:
 *   spring.mail.host=smtp.gmail.com
 *   spring.mail.port=587
 *   spring.mail.username=your@gmail.com
 *   spring.mail.password=your-app-password
 *   spring.mail.properties.mail.smtp.auth=true
 *   spring.mail.properties.mail.smtp.starttls.enable=true
 *   bookwhiz.mail.from=BookWhiz <your@gmail.com>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // Optional — app starts even without spring.mail.* configured
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${bookwhiz.mail.from:BookWhiz <no-reply@bookwhiz.com>}")
    private String from;

    @Async
    public void send(String to, String subject, String htmlBody) {
        if (mailSender == null) {
            log.warn("Email skipped (no mail config) — to={} subject={}", to, subject);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);  // true = HTML
            mailSender.send(msg);
            log.info("Email sent to {} — {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ── Pre-built email templates ─────────────────────────────

    /** Sent to SuperAdmin(s) when a new request arrives from Admin/Manager */
    public void sendNewRequestAlert(String superAdminEmail, String requestedByName,
                                    String requestType, String summary) {
        String subject = "📥 New " + requestType + " Request — BookWhiz";
        String body = html(
            "New Request Received",
            "🔔 New Request from " + requestedByName,
            "<p style='color:#555;'>A new <strong>" + requestType + "</strong> request has been submitted and awaits your review.</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>" +
            "  <tr><td style='padding:8px 0;color:#888;font-size:13px;'>From</td><td style='padding:8px 0;font-weight:600;'>" + requestedByName + "</td></tr>" +
            "  <tr><td style='padding:8px 0;color:#888;font-size:13px;'>Type</td><td style='padding:8px 0;font-weight:600;'>" + requestType + "</td></tr>" +
            "  <tr><td style='padding:8px 0;color:#888;font-size:13px;'>Summary</td><td style='padding:8px 0;'>" + summary + "</td></tr>" +
            "</table>",
            "Login to BookWhiz to review this request.",
            "#ef4444", "Review Request"
        );
        send(superAdminEmail, subject, body);
    }

    /** Sent to Partner when their application is approved */
    public void sendPartnerApproved(String partnerEmail, String partnerName,
                                     String theatreName) {
        String subject = "🎉 Your Partner Application is Approved — BookWhiz";
        String body = html(
            "Application Approved",
            "🎉 Welcome to BookWhiz, " + partnerName + "!",
            "<p style='color:#555;'>Congratulations! Your theatre <strong>\"" + theatreName + "\"</strong> has been approved and your partner account is now active.</p>" +
            "<p style='color:#555;margin-top:8px;'>You can now log in and start adding shows, managing bookings, and growing your audience.</p>",
            "Log in with your registered email and the password you set during registration.",
            "#22c55e", "Log In Now"
        );
        send(partnerEmail, subject, body);
    }

    /** Sent to Partner when their application is rejected */
    public void sendPartnerRejected(String partnerEmail, String partnerName,
                                     String theatreName, String reason) {
        String subject = "❌ Partner Application Update — BookWhiz";
        String body = html(
            "Application Status",
            "Update on Your Application",
            "<p style='color:#555;'>Thank you for applying to join BookWhiz with your theatre <strong>\"" + theatreName + "\"</strong>.</p>" +
            "<p style='color:#555;margin-top:8px;'>Unfortunately, your application was not approved at this time.</p>" +
            (reason != null && !reason.isBlank()
                ? "<div style='background:#fff3f3;border-left:4px solid #ef4444;padding:12px 16px;margin:16px 0;border-radius:4px;'>" +
                  "<strong style='color:#ef4444;'>Reason:</strong><br><span style='color:#555;'>" + reason + "</span></div>"
                : "") +
            "<p style='color:#555;'>You may re-apply after addressing the above concerns.</p>",
            "If you have questions, please contact our support team.",
            "#ef4444", "Contact Support"
        );
        send(partnerEmail, subject, body);
    }

    /** Sent to partner/admin when their request is approved by SuperAdmin */
    public void sendRequestApproved(String recipientEmail, String recipientName,
                                     String requestType, String summary) {
        String subject = "✅ Your " + requestType + " Request Approved — BookWhiz";
        String body = html(
            "Request Approved",
            "✅ Request Approved",
            "<p style='color:#555;'>Great news, <strong>" + recipientName + "</strong>! Your " + requestType + " request has been approved.</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>" +
            "  <tr><td style='padding:8px 0;color:#888;font-size:13px;'>Request</td><td style='padding:8px 0;'>" + summary + "</td></tr>" +
            "</table>",
            "Log in to BookWhiz to see the changes applied.",
            "#22c55e", "Go to Dashboard"
        );
        send(recipientEmail, subject, body);
    }

    /** Sent to user when their locked booking is auto-cancelled */
    public void sendBookingExpired(String userEmail, String userName,
                                    String movieTitle, String showDate, String venueName) {
        String subject = "⏰ Your Booking Has Expired — BookWhiz";
        String body = html(
            "Booking Expired",
            "⏰ Booking Expired",
            "<p style='color:#555;'>Hi <strong>" + userName + "</strong>, your seat reservation for the following show was not completed within 30 minutes and has been released.</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>" +
            "  <tr><td style='padding:8px 0;color:#888;font-size:13px;'>Movie</td><td style='padding:8px 0;font-weight:600;'>" + movieTitle + "</td></tr>" +
            "  <tr><td style='padding:8px 0;color:#888;font-size:13px;'>Show Date</td><td style='padding:8px 0;'>" + showDate + "</td></tr>" +
            "  <tr><td style='padding:8px 0;color:#888;font-size:13px;'>Venue</td><td style='padding:8px 0;'>" + venueName + "</td></tr>" +
            "</table>" +
            "<p style='color:#555;'>The seats are now available again. Book quickly next time!</p>",
            "Your seats are now available for others to book.",
            "#f59e0b", "Book Again"
        );
        send(userEmail, subject, body);
    }

    // ── HTML template ─────────────────────────────────────────
    private String html(String preheader, String heading, String content,
                        String footer, String btnColor, String btnText) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='" +
               "margin:0;padding:0;background:#f5f5f5;font-family:DM Sans,Arial,sans-serif;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 16px;'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:16px;" +
               "box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;'>" +

               // Header
               "<tr><td style='background:linear-gradient(135deg,#1a1a1a,#2d2d2d);padding:32px 40px;text-align:center;'>" +
               "<h1 style='color:#fff;margin:0;font-size:24px;font-weight:800;letter-spacing:-0.5px;'>🎬 BookWhiz</h1>" +
               "<p style='color:rgba(255,255,255,0.4);margin:6px 0 0;font-size:13px;'>" + preheader + "</p>" +
               "</td></tr>" +

               // Body
               "<tr><td style='padding:40px;'>" +
               "<h2 style='color:#1a1a1a;margin:0 0 20px;font-size:20px;font-weight:800;'>" + heading + "</h2>" +
               content +
               "</td></tr>" +

               // Footer
               "<tr><td style='background:#f9f9f9;border-top:1px solid #f0f0f0;padding:24px 40px;text-align:center;'>" +
               "<p style='color:#aaa;font-size:12px;margin:0;'>" + footer + "</p>" +
               "<p style='color:#ddd;font-size:11px;margin:8px 0 0;'>© " + java.time.Year.now().getValue() + " BookWhiz. All rights reserved.</p>" +
               "</td></tr>" +

               "</table></td></tr></table></body></html>";
    }
}