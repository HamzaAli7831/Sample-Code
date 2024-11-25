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
}