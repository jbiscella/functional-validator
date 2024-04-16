package org.jacopobiscella.functional.validator;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A functional validator for validating objects and their nested properties.
 *
 * @param <T> the type of the object being validated
 */
@SuppressWarnings("java:S4276")
public class Validator<T> {
    private final String logEntityToValidateIsNull;
    private final Function<ValidationContext<T>, ValidationContext<T>> check;
    private final Function<T, String> logPrefixMapper;
    private final BiFunction<ValidationContext<T>, Function<T, String>, String> logMessageMapper =
            (validationContext, prefixFunction) -> prefixFunction.apply(validationContext.entity)
                    + String.join(", ", validationContext.logMessages);

    /**
     * Creates a new instance of the {@link Builder} class for building a validator.
     *
     * @param <T> the type of the object being validated
     * @return a new {@link Builder} instance
     */
    public static <T> MandatoryClassField<T> builder() {
        return new Builder<>();
    }

    /**
     * Validates the given entity and returns an {@link Optional} containing the validation error,
     * if any.
     *
     * @param entityToValidate the entity to validate
     * @return an {@link Optional} containing the validation error, or an empty {@link Optional} if
     *         validation succeeds
     */
    public Optional<Throwable> getValidationError(T entityToValidate) {
        ValidationContext<T> result = new ValidationContext<>(new LinkedList<>(), entityToValidate);
        if (entityToValidate == null) {
            return Optional.of(new IllegalArgumentException(logEntityToValidateIsNull));
        }
        check.apply(result);
        return result.logMessages.isEmpty() ? Optional.empty()
                : Optional.of(new IllegalArgumentException(
                        logMessageMapper.apply(result, logPrefixMapper)));
    }

    private Validator(String logEntityToValidateIsNull, Function<T, String> logPrefixMapper,
            List<Function<ValidationContext<T>, ValidationContext<T>>> validationFunctions) {
        this.logEntityToValidateIsNull = logEntityToValidateIsNull;
        this.logPrefixMapper = logPrefixMapper != null ? logPrefixMapper : entity -> "";
        this.check = validationFunctions.stream().reduce(c -> c, Function::andThen);
    }

    /**
     * An interface representing the mandatory field for specifying the class of the entity to validate.
     *
     * @param <T> the type of the object being validated
     */
    public interface MandatoryClassField<T> {
        /**
         * Specifies that the entity to validate is not nullable and requires the class information.
         *
         * @param entityClass the class of the entity to validate
         * @return the {@link Builder} instance for chaining
         */
        Builder<T> setNotNullable(Class<T> entityClass);

        /**
         * Specifies that the entity to validate can be null.
         *
         * @return the {@link Builder} instance for chaining
         */
        Builder<T> setNullable();
    }

    /**
     * A builder class for creating {@link Validator} instances.
     *
     * @param <T> the type of the object being validated
     */
    public static class Builder<T> implements MandatoryClassField<T> {
        private Builder() {}

        private Function<ValidationContext<T>, ValidationContext<T>> toValidationAction(
                Predicate<T> check, Function<T, String> logAction) {
            return validationContext -> {
                T entityToValidate = validationContext.getEntity();
                if (!check.test(entityToValidate)) {
                    validationContext.getLogMessages().add(logAction.apply(entityToValidate));
                }
                return validationContext;
            };
        }

        private <U> Function<ValidationContext<T>, ValidationContext<T>> toValidationAction(
                Function<T, U> fieldAccessor, Validator<U> nestedValidator) {
            return parentValidationContext -> {
                U nestedEntity = fieldAccessor.apply(parentValidationContext.entity);
                if (nestedEntity == null) {
                    if (logEntityToValidateIsNull != null) {
                        parentValidationContext.getLogMessages().add(logEntityToValidateIsNull);
                    }
                    parentValidationContext.logMessages.add(logEntityToValidateIsNull);
                } else {
                    ValidationContext<U> validationContext =
                            nestedValidator.check.apply(new ValidationContext<>(new LinkedList<>(),
                                    fieldAccessor.apply(parentValidationContext.entity)));
                    String nestedLogMessage = nestedValidator.logMessageMapper
                            .apply(validationContext, nestedValidator.logPrefixMapper);
                    if (!nestedLogMessage.isEmpty()) {
                        parentValidationContext.getLogMessages().add("(" + nestedLogMessage + ")");
                    }
                }
                return parentValidationContext;
            };
        }

        private final List<Function<ValidationContext<T>, ValidationContext<T>>> validationFunctions =
                new LinkedList<>();

        private Function<T, String> logPrefixMapper;
        private String logEntityToValidateIsNull;

        public Builder<T> setNullable() {
            return this;
        }

        public Builder<T> setNotNullable(Class<T> entityClass) {
            Objects.nonNull(entityClass);
            this.logEntityToValidateIsNull =
                    "Entity to validate cannot be null for class " + entityClass.getName();
            return this;
        }

        /**
         * Sets the error message prefix for the validator.
         *
         * @param prefix the function that maps the validated entity to the error message prefix
         * @return the {@link Builder} instance for chaining
         */
        public Builder<T> setErrorMessagePrefix(Function<T, String> prefix) {
            this.logPrefixMapper = prefix;
            return this;
        }

        /**
         * Adds a validation constraint to the validator.
         *
         * @param check the predicate that checks the validation condition
         * @param logAction the function that maps the validated entity to the error message
         * @return the {@link Builder} instance for chaining
         */
        public Builder<T> addConstraint(Predicate<T> check, Function<T, String> logAction) {
            validationFunctions.add(toValidationAction(check, logAction));
            return this;
        }

        /**
         * Adds a nested validator for validating a nested property.
         *
         * @param fieldAccessor the function that maps the validated entity to the nested property
         * @param nestedValidator the validator for the nested property
         * @param <U> the type of the nested property
         * @return the {@link Builder} instance for chaining
         */
        public <U> Builder<T> addNestedValidator(Function<T, U> fieldAccessor,
                Validator<U> nestedValidator) {
            validationFunctions.add(toValidationAction(fieldAccessor, nestedValidator));
            return this;
        }

        /**
         * Builds the {@link Validator} instance with the configured validationFunctions and error
         * message prefix.
         *
         * @return the built {@link Validator} instance
         */
        public Validator<T> build() {
            return new Validator<>(logEntityToValidateIsNull, logPrefixMapper, validationFunctions);
        }
    }

    private static class ValidationContext<T> {
        private final List<String> logMessages;
        private final T entity;

        private ValidationContext(List<String> logMessages, T entity) {
            this.logMessages = logMessages;
            this.entity = entity;
        }

        public List<String> getLogMessages() {
            return logMessages;
        }

        public T getEntity() {
            return entity;
        }
    }
}
