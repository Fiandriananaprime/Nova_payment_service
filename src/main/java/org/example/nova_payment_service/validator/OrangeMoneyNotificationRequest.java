package org.example.nova_payment_service.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrangeMoneyNotificationRequest(
        @NotBlank
        @Pattern(regexp = "SUCCESS|FAILED|EXPIRED")
        String status,

        @NotBlank
        @Size(max = 255)
        String notif_token,

        @Size(max = 255)
        String txnid
) {
}
