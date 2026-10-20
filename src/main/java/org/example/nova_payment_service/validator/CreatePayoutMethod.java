package org.example.nova_payment_service.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.nova_payment_service.entity.PayoutMethodType;

public record CreatePayoutMethod(

        @NotNull
        PayoutMethodType type,

        @Size(max = 100)
        String label,

        @NotBlank
        @Size(max = 255)
        String account,

        Boolean isDefault

) {
}
