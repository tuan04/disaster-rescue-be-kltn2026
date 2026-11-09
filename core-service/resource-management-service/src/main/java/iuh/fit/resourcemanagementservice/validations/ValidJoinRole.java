package iuh.fit.resourcemanagementservice.validations;

import iuh.fit.resourcemanagementservice.enums.TeamRole;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidJoinRole.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidJoinRole {

    String message() default "Không thể xin gia nhập với vai trò Đội trưởng";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidJoinRole, TeamRole> {
        @Override
        public boolean isValid(TeamRole value, ConstraintValidatorContext context) {
            if (value == null) {
                return true; // Để @NotNull xử lý nếu bị null
            }
            return value != TeamRole.LEADER;
        }
    }
}
