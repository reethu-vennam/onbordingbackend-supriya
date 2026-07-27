package com.sabbpe.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.notification.from-email:noreply@sabbpe.com}")
    private String fromEmail;

    @Value("${app.notification.onboarding-team-email:vendor.onboarding@sabbpe.com}")
    private String onboardingTeamEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public boolean sendEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public void notifyMerchantStatusChange(String toEmail, String merchantName,
                                            String oldStatus, String newStatus) {
        String subject = "Your SabbPe onboarding status has been updated";
        String html = String.format("""
                <h2>SabbPe Merchant Onboarding</h2>
                <p>Dear %s,</p>
                <p>Your onboarding status has been updated from <b>%s</b> to <b>%s</b>.</p>
                <p>Log in to your dashboard for more details.</p>
                """, merchantName, oldStatus, newStatus);
        sendEmail(toEmail, subject, html);
    }

    public void notifyOnboardingTeamApproval(String merchantName, String businessName,
                                              String email, String mobile) {
        String subject = "Merchant submitted for approval: " + businessName;
        String html = String.format("""
                <h2>Merchant Approval Request</h2>
                <p><b>Name:</b> %s</p>
                <p><b>Business:</b> %s</p>
                <p><b>Email:</b> %s</p>
                <p><b>Mobile:</b> %s</p>
                <p>Please review the merchant profile in the admin dashboard.</p>
                """, merchantName, businessName, email, mobile);
        sendEmail(onboardingTeamEmail, subject, html);
    }

    public boolean sendMerchantCredentialsEmail(String merchantEmail, String merchantName,
                                              String password, Double commission, String distributorName) {
        String commissionHtml = commission != null
                ? String.format("<p><b>Commission Rate:</b> %.2f%%</p>", commission)
                : "";
        String distributorHtml = distributorName != null
                ? String.format("<p><b>Onboarded by:</b> %s</p>", distributorName)
                : "";
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #2563eb; padding: 24px; text-align: center;">
                        <h1 style="color: white; margin: 0;">Welcome to SabbPe</h1>
                    </div>
                    <div style="padding: 24px; background: #f9fafb;">
                        <p>Dear %s,</p>
                        <p>Your merchant account has been created. Use the credentials below to log in:</p>
                        <div style="background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; margin: 16px 0;">
                            <p><b>Email:</b> %s</p>
                            <p><b>Password:</b> <code style="background: #f3f4f6; padding: 2px 6px; border-radius: 4px;">%s</code></p>
                            %s
                            %s
                        </div>
                        <a href="%s/auth" style="display: inline-block; background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 16px;">Login to Dashboard</a>
                    </div>
                </div>
                """, merchantName, merchantEmail, password, commissionHtml, distributorHtml, frontendUrl);
        return sendEmail(merchantEmail, "Your SabbPe merchant account has been created", html);
    }

    public void sendDistributorCredentialsEmail(String distributorEmail, String distributorName, String password) {
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #2563eb; padding: 24px; text-align: center;">
                        <h1 style="color: white; margin: 0;">SabbPe Distributor Access</h1>
                    </div>
                    <div style="padding: 24px; background: #f9fafb;">
                        <p>Dear %s,</p>
                        <p>Your distributor account is ready. Use the credentials below to log in:</p>
                        <div style="background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; margin: 16px 0;">
                            <p><b>Email:</b> %s</p>
                            <p><b>Password:</b> <code style="background: #f3f4f6; padding: 2px 6px; border-radius: 4px;">%s</code></p>
                        </div>
                        <a href="%s/auth" style="display: inline-block; background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 16px;">Login to Dashboard</a>
                    </div>
                </div>
                """, distributorName, distributorEmail, password, frontendUrl);
        sendEmail(distributorEmail, "Your SabbPe distributor account is ready", html);
    }

    public void sendEmployeeCredentialsEmail(String employeeEmail, String employeeName, String password) {
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #059669; padding: 24px; text-align: center;">
                        <h1 style="color: white; margin: 0;">SabbPe Employee Access</h1>
                    </div>
                    <div style="padding: 24px; background: #f9fafb;">
                        <p>Dear %s,</p>
                        <p>Your employee account has been created. Use the credentials below to log in:</p>
                        <div style="background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; margin: 16px 0;">
                            <p><b>Email:</b> %s</p>
                            <p><b>Password:</b> <code style="background: #f3f4f6; padding: 2px 6px; border-radius: 4px;">%s</code></p>
                        </div>
                        <a href="%s/auth" style="display: inline-block; background: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 16px;">Login to Dashboard</a>
                    </div>
                </div>
                """, employeeName, employeeEmail, password, frontendUrl);
        sendEmail(employeeEmail, "Your SabbPe employee account has been created", html);
    }

    public void sendMerchantInviteEmail(String merchantEmail, String merchantName,
                                         String inviteLink, String distributorName) {
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #2563eb; padding: 24px; text-align: center;">
                        <h1 style="color: white; margin: 0;">You're Invited to SabbPe</h1>
                    </div>
                    <div style="padding: 24px; background: #f9fafb;">
                        <p>Dear %s,</p>
                        <p><b>%s</b> has invited you to join SabbPe as a merchant partner.</p>
                        <p>Click the button below to start your onboarding:</p>
                        <a href="%s" style="display: inline-block; background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 16px;">Start Onboarding</a>
                    </div>
                </div>
                """, merchantName, distributorName, inviteLink);
        sendEmail(merchantEmail, distributorName + " invited you to join SabbPe", html);
    }

    public void sendDistributorAgreementEmail(String distributorEmail, String distributorName,
                                               String onboardingUrl) {
        String html = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #2563eb; padding: 24px; text-align: center;">
                        <h1 style="color: white; margin: 0;">Distributor Agreement</h1>
                    </div>
                    <div style="padding: 24px; background: #f9fafb;">
                        <p>Dear %s,</p>
                        <p>Your distributor agreement is ready for review. Please click the button below to access the onboarding portal, download the agreement, and upload a signed copy.</p>
                        <a href="%s" style="display: inline-block; background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 16px;">Access Onboarding Portal</a>
                        <p style="margin-top: 16px; font-size: 12px; color: #6b7280;">This link will expire in 14 days.</p>
                    </div>
                </div>
                """, distributorName, onboardingUrl);
        sendEmail(distributorEmail, "Your SabbPe distributor agreement is ready", html);
    }

    public void notifyAdminNewSubmission(String merchantName, String businessName,
                                          String email, String mobile) {
        String subject = "New merchant application submitted";
        String html = String.format("""
                <h2>New Merchant Submission</h2>
                <p><b>Name:</b> %s</p>
                <p><b>Business:</b> %s</p>
                <p><b>Email:</b> %s</p>
                <p><b>Mobile:</b> %s</p>
                """, merchantName, businessName, email, mobile);
        sendEmail(onboardingTeamEmail, subject, html);
    }
}
