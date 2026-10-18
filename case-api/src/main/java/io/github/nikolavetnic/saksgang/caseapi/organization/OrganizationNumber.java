package io.github.nikolavetnic.saksgang.caseapi.organization;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The annotated string must be a Norwegian organization number: nine digits where the last one is a
 * valid mod11 check digit. {@code null} is considered valid; combine with {@code @NotNull} if required.
 */
@Documented
@Constraint(validatedBy = OrganizationNumberValidator.class)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface OrganizationNumber {

	String message() default "must be a valid organization number";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
