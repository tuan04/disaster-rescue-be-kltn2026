package iuh.fit.resourcemanagementservice.validations;

import iuh.fit.resourcemanagementservice.dtos.request.AddMemberRequest;
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
@Constraint(validatedBy = ValidMember.Validator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMember {

    String message() default "Thông tin thành viên không hợp lệ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidMember, AddMemberRequest> {
        @Override
        public boolean isValid(AddMemberRequest request, ConstraintValidatorContext context) {
            if (request == null) {
                return true;
            }

            boolean isValid = true;
            context.disableDefaultConstraintViolation();

            if (request.roleInTeam() == TeamRole.LEADER) {
                context.buildConstraintViolationWithTemplate("Không thể gán vai trò Đội trưởng cho thành viên được thêm")
                        .addPropertyNode("roleInTeam")
                        .addConstraintViolation();
                isValid = false;
            }

            if (request.memberId() == null) {
                if (request.fullName() == null || request.fullName().isBlank()) {
                    context.buildConstraintViolationWithTemplate("Vui lòng nhập họ tên cho thành viên")
                            .addPropertyNode("fullName")
                            .addConstraintViolation();
                    isValid = false;
                }

                if (request.phone() == null || request.phone().isBlank()) {
                    context.buildConstraintViolationWithTemplate("Vui lòng nhập số điện thoại cho thành viên")
                            .addPropertyNode("phone")
                            .addConstraintViolation();
                    isValid = false;
                }
            }

            return isValid;
        }
    }
}
