package com.service.notification.gateway;

import com.service.notification.email.EmailProcessor;
import org.springframework.stereotype.Component;


@Component
public class EmailProcessorSelector {
    private final Map<EmailGatewayIdentifier, EmailProcessor> emailProcessors;

    // Constructor that initializes the emailProcessors map from a list of EmailProcessor instances.
    public EmailProcessorSelector(List<EmailProcessor> paymentProcessorsList) {
        // Collect EmailProcessors into a map using their supported gateway identifiers as keys.
        this.emailProcessors = paymentProcessorsList.stream()
                .collect(Collectors.toMap(
                        EmailProcessor::getSupportEmailGateway,
                        processor -> processor
                ));
    }

    public EmailProcessor getEmailProcessor(EmailGatewayIdentifier identifier) {
        EmailProcessor emailProcessor = this.emailProcessors.get(identifier);
        if (emailProcessor == null) {
            throw new IllegalArgumentException("Unsupported card provider");
        }
        return emailProcessor;
    }

    public void sendEmailNotification(String subject, String to, String body, String filePath, MultipartFile file) {
        LOGGER.info("sendEmailMessage, subject [" + subject + "] to [" + to + "] body [" + body + "]");
        Properties properties = new Properties();
        InputStream input = getClass().getResourceAsStream("/application.properties");
        try {
            properties.load(input);
            Properties props = new Properties();
            props.put("mail.smtp.host", properties.getProperty("mail.smtp.host"));
            props.put("mail.smtp.port", properties.getProperty("mail.smtp.port"));
            props.put("mail.smtp.auth", properties.getProperty("mail.smtp.auth"));
            props.put("mail.smtp.starttls.enable", properties.getProperty("mail.smtp.starttls.enable"));
            Session session = Session.getInstance(props, new javax.mail.Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(properties.getProperty("mail.smtp.username"), properties.getProperty("mail.smtp.password"));
                }
            });
            MimeMessage msg = new MimeMessage(session);
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");
            msg.setFrom(new InternetAddress(properties.getProperty("mail.smtp.username")));
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            BodyPart messageBodyPart = new MimeBodyPart();

            if (filePath != null) {
                messageBodyPart.setContent(body, "text/html; charset=utf-8");
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                messageBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(filePath);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(fileName);
                multipart.addBodyPart(messageBodyPart);
                msg.setContent(multipart);
            } else if(file != null && !file.isEmpty()) {
                Multipart multipart = new MimeMultipart();
                messageBodyPart.setContent(body, "text/html; charset=utf-8");
                multipart.addBodyPart(messageBodyPart);

                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setContent(file.getBytes(), file.getContentType());
                messageBodyPart.setFileName(file.getOriginalFilename());
                messageBodyPart.setDisposition(Part.ATTACHMENT);
                multipart.addBodyPart(messageBodyPart);

                msg.setContent(multipart);
            } else {
                messageBodyPart.setContent(body, "text/html; charset=utf-8");
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                msg.setContent(multipart);
            }

            Transport.send(msg);
            LOGGER.info("Email Sent Successfully to [" + to + "]");
        } catch (IOException e) {
            LOGGER.error("there is error in sending Email to [" + to + "]", e);
        } catch (MessagingException e) {
            LOGGER.error("there is error in sending Email to [" + to + "]", e);
        }
    }
}