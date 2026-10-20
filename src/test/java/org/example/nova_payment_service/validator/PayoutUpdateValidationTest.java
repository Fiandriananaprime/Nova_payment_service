package org.example.nova_payment_service.validator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayoutUpdateValidationTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void rejectsPatchWhenNoFieldWasProvided() {
        PayoutUpdate request = new PayoutUpdate(null, null, null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsExplicitFalseAsAProvidedBooleanValue() {
        PayoutUpdate request = new PayoutUpdate(null, false, null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsLabelAsTheOnlyProvidedField() {
        PayoutUpdate request = new PayoutUpdate("Bank account", null, null);

        assertTrue(validator.validate(request).isEmpty());
    }
}
