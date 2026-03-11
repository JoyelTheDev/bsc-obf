/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners.rules;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.internal.runners.rules.ValidationError;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runners.model.FrameworkMember;
import org.junit.runners.model.TestClass;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class RuleMemberValidator {
    public static final RuleMemberValidator CLASS_RULE_VALIDATOR = RuleMemberValidator.classRuleValidatorBuilder().withValidator(new DeclaringClassMustBePublic()).withValidator(new MemberMustBeStatic()).withValidator(new MemberMustBePublic()).withValidator(new FieldMustBeATestRule()).build();
    public static final RuleMemberValidator RULE_VALIDATOR = RuleMemberValidator.testRuleValidatorBuilder().withValidator(new MemberMustBeNonStaticOrAlsoClassRule()).withValidator(new MemberMustBePublic()).withValidator(new FieldMustBeARule()).build();
    public static final RuleMemberValidator CLASS_RULE_METHOD_VALIDATOR = RuleMemberValidator.classRuleValidatorBuilder().forMethods().withValidator(new DeclaringClassMustBePublic()).withValidator(new MemberMustBeStatic()).withValidator(new MemberMustBePublic()).withValidator(new MethodMustBeATestRule()).build();
    public static final RuleMemberValidator RULE_METHOD_VALIDATOR = RuleMemberValidator.testRuleValidatorBuilder().forMethods().withValidator(new MemberMustBeNonStaticOrAlsoClassRule()).withValidator(new MemberMustBePublic()).withValidator(new MethodMustBeARule()).build();
    private final Class<? extends Annotation> annotation;
    private final boolean methods;
    private final List<RuleValidator> validatorStrategies;

    RuleMemberValidator(Builder builder) {
        this.annotation = builder.annotation;
        this.methods = builder.methods;
        this.validatorStrategies = builder.validators;
    }

    public void validate(TestClass target, List<Throwable> errors) {
        List<FrameworkMember> members = this.methods ? target.getAnnotatedMethods(this.annotation) : target.getAnnotatedFields(this.annotation);
        for (FrameworkMember each : members) {
            this.validateMember(each, errors);
        }
    }

    private void validateMember(FrameworkMember<?> member, List<Throwable> errors) {
        for (RuleValidator strategy : this.validatorStrategies) {
            strategy.validate(member, this.annotation, errors);
        }
    }

    private static Builder classRuleValidatorBuilder() {
        return new Builder(ClassRule.class);
    }

    private static Builder testRuleValidatorBuilder() {
        return new Builder(Rule.class);
    }

    private static boolean isRuleType(FrameworkMember<?> member) {
        return RuleMemberValidator.isMethodRule(member) || RuleMemberValidator.isTestRule(member);
    }

    private static boolean isTestRule(FrameworkMember<?> member) {
        return TestRule.class.isAssignableFrom(member.getType());
    }

    private static boolean isMethodRule(FrameworkMember<?> member) {
        return MethodRule.class.isAssignableFrom(member.getType());
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class FieldMustBeATestRule
    implements RuleValidator {
        private FieldMustBeATestRule() {
        }

        @Override
        public void validate(FrameworkMember<?> member, Class<? extends Annotation> annotation, List<Throwable> errors) {
            if (!RuleMemberValidator.isTestRule(member)) {
                errors.add(new ValidationError(member, annotation, "must implement TestRule."));
            }
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class MethodMustBeATestRule
    implements RuleValidator {
        private MethodMustBeATestRule() {
        }

        @Override
        public void validate(FrameworkMember<?> member, Class<? extends Annotation> annotation, List<Throwable> errors) {
            if (!RuleMemberValidator.isTestRule(member)) {
                errors.add(new ValidationError(member, annotation, "must return an implementation of TestRule."));
            }
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class MethodMustBeARule
    implements RuleValidator {
        private MethodMustBeARule() {
        }

        @Override
        public void validate(FrameworkMember<?> member, Class<? extends Annotation> annotation, List<Throwable> errors) {
            if (!RuleMemberValidator.isRuleType(member)) {
                errors.add(new ValidationError(member, annotation, "must return an implementation of MethodRule or TestRule."));
            }
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class FieldMustBeARule
    implements RuleValidator {
        private FieldMustBeARule() {
        }

        @Override
        public void validate(FrameworkMember<?> member, Class<? extends Annotation> annotation, List<Throwable> errors) {
            if (!RuleMemberValidator.isRuleType(member)) {
                errors.add(new ValidationError(member, annotation, "must implement MethodRule or TestRule."));
            }
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class MemberMustBePublic
    implements RuleValidator {
        private MemberMustBePublic() {
        }

        @Override
        public void validate(FrameworkMember<?> member, Class<? extends Annotation> annotation, List<Throwable> errors) {
            if (!member.isPublic()) {
                errors.add(new ValidationError(member, annotation, "must be public."));
            }
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class DeclaringClassMustBePublic
    implements RuleValidator {
        private DeclaringClassMustBePublic() {
        }

        @Override
        public void validate(FrameworkMember<?> member, Class<? extends Annotation> annotation, List<Throwable> errors) {
            if (!this.isDeclaringClassPublic(member)) {
                errors.add(new ValidationError(member, annotation, "must be declared in a public class."));
            }
        }

        private boolean isDeclaringClassPublic(FrameworkMember<?> member) {
            return Modifier.isPublic(member.getDeclaringClass().getModifiers());
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class MemberMustBeStatic
    implements RuleValidator {
        private MemberMustBeStatic() {
        }

        @Override
        public void validate(FrameworkMember<?> member, Class<? extends Annotation> annotation, List<Throwable> errors) {
            if (!member.isStatic()) {
                errors.add(new ValidationError(member, annotation, "must be static."));
            }
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class MemberMustBeNonStaticOrAlsoClassRule
    implements RuleValidator {
        private MemberMustBeNonStaticOrAlsoClassRule() {
        }

        @Override
        public void validate(FrameworkMember<?> member, Class<? extends Annotation> annotation, List<Throwable> errors) {
            boolean isClassRuleAnnotated;
            boolean isMethodRuleMember = RuleMemberValidator.isMethodRule(member);
            boolean bl = isClassRuleAnnotated = member.getAnnotation(ClassRule.class) != null;
            if (member.isStatic() && (isMethodRuleMember || !isClassRuleAnnotated)) {
                String message = RuleMemberValidator.isMethodRule(member) ? "must not be static." : "must not be static or it must be annotated with @ClassRule.";
                errors.add(new ValidationError(member, annotation, message));
            }
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    static interface RuleValidator {
        public void validate(FrameworkMember<?> var1, Class<? extends Annotation> var2, List<Throwable> var3);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class Builder {
        private final Class<? extends Annotation> annotation;
        private boolean methods;
        private final List<RuleValidator> validators;

        private Builder(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
            this.methods = false;
            this.validators = new ArrayList<RuleValidator>();
        }

        Builder forMethods() {
            this.methods = true;
            return this;
        }

        Builder withValidator(RuleValidator validator) {
            this.validators.add(validator);
            return this;
        }

        RuleMemberValidator build() {
            return new RuleMemberValidator(this);
        }
    }
}

