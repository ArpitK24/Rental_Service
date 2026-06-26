package com.rentalService.service;

import com.rentalService.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public MailNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    public void sendBookingCreatedMail(User vendor, User customer, String vehicleName, String bookingId) {
        if (mailSender == null) {
            log.info("Mail sender not configured; skipping booking email for bookingId={}", bookingId);
            return;
        }
        if (vendor == null || !StringUtils.hasText(vendor.getEmail())) {
            log.info("Vendor email missing; skipping booking email for bookingId={}", bookingId);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(fromAddress)) {
            message.setFrom(fromAddress);
        }
        message.setTo(vendor.getEmail());
        message.setSubject("New vehicle booking request");
        message.setText(buildBookingCreatedBody(vendor, customer, vehicleName, bookingId));
        mailSender.send(message);
    }

    private String buildBookingCreatedBody(User vendor, User customer, String vehicleName, String bookingId) {
        String vendorName = vendor != null && StringUtils.hasText(vendor.getName()) ? vendor.getName() : "Vendor";
        String customerName = customer != null && StringUtils.hasText(customer.getName()) ? customer.getName() : "Customer";
        String customerMobile = customer != null && StringUtils.hasText(customer.getMobile()) ? customer.getMobile() : "N/A";
        String carName = StringUtils.hasText(vehicleName) ? vehicleName : "your vehicle";

        return "Hello " + vendorName + ",\n\n"
                + "You have received a new booking request.\n\n"
                + "Vehicle: " + carName + "\n"
                + "Booking ID: " + bookingId + "\n"
                + "Customer Name: " + customerName + "\n"
                + "Customer Mobile: " + customerMobile + "\n\n"
                + "Please open the app/admin panel to review and respond to the request.\n\n"
                + "Regards,\nRentRover";
    }
}
