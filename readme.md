# Functional Validator

The Functional Validator is a Java library that provides a fluent and functional way to validate objects and their nested properties. It allows you to define validation rules using lambda expressions and combine them to create complex validation scenarios.

## Features

- Fluent API for building validators
- Supports validation of nested properties
- Customizable error messages
- Functional programming style using lambda expressions
- Lightweight and easy to use
- Supports specifying nullability for entities and nested validators

## Usage

To use the Functional Validator in your project, follow these steps:

1. Add the `functional-validator` dependency to your project's build file (e.g., `pom.xml` for Maven).

2. Create a `Validator` instance using the `Validator.builder()` method.

3. Specify whether the entity to validate is nullable or not using `setNullable()` or `setNotNullable(Class<T> entityClass)`.

4. Define validation rules using the `addConstraint` method, providing a predicate and an error message function.

5. (Optional) Define nested validators for complex objects using the `addNestedValidator` method, specifying nullability for each nested validator.

6. Build the validator using the `build()` method.

7. Use the `getValidationError` method to validate an object and retrieve any validation errors.

Here's an example of how to create a validator:

```java
Validator<Person> personValidator = Validator.<Person>builder()
        .setNotNullable(Person.class)
        .addConstraint(
                person -> person.getAge() >= 18,
                person -> "Person must be at least 18 years old")
        .addNestedValidator(
                Person::getAddress,
                Validator.<Address>builder()
                        .setNullable()
                        .addConstraint(
                                address -> address.getCity() != null && !address.getCity().isEmpty(),
                                address -> "City cannot be null or empty")
                        .build())
        .build();

Person person = new Person("John", 25);
Optional<Throwable> validationError = personValidator.getValidationError(person);
if (validationError.isPresent()) {
    System.out.println(validationError.get().getMessage());
} else {
    System.out.println("Validation passed");
}
```
In this example, we create a personValidator that validates a Person object. We specify that the Person entity is not nullable using setNotNullable(Person.class). We also define a nested validator for the Address property, which is marked as nullable using setNullable().

If a null value is passed to the getValidationError method for a non-nullable entity, it will return an appropriate validation error message that includes the class name.

### Advanced Scenario: Email Validation
Let's consider an advanced scenario where we want to validate an email address using a regular expression. We can create a custom validator for the Email class and use it as a nested validator within the Person validator.

Here's an example of how to create an email validator using a regular expression:
```java
Validator<Email> emailValidator = Validator.<Email>builder()
        .setNotNullable(Email.class)
        .addConstraint(
                email -> email.getAddress().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"),
                email -> "Invalid email address format")
        .build();

Validator<Person> personValidator = Validator.<Person>builder()
        .setNotNullable(Person.class)
        .addConstraint(
                person -> person.getAge() >= 18,
                person -> "Person must be at least 18 years old")
        .addNestedValidator(
                Person::getEmail,
                emailValidator)
        .build();

Person person = new Person("John", 25, new Email("john@example.com"));
Optional<Throwable> validationError = personValidator.getValidationError(person);
if (validationError.isPresent()) {
    System.out.println(validationError.get().getMessage());
} else {
    System.out.println("Validation passed");
}
```

In this example, we create an emailValidator that checks if the email address matches a regular expression pattern. We mark the Email class as not nullable using setNotNullable(Email.class).

We then use the addNestedValidator method to include the emailValidator as a nested validator within the personValidator.

When we validate a Person object, the emailValidator will be applied to the Email property of the person, ensuring that the email address is in a valid format.

### Customizing Error Messages

The Functional Validator allows you to customize the error messages for each validation constraint. You can provide a function that takes the object being validated and returns a custom error message.

For example, you can customize the error message for the age constraint as follows:
```java
Validator<Person> personValidator = Validator.<Person>builder()
        .setNotNullable(Person.class)
        .addConstraint(
                person -> person.getAge() >= 18,
                person -> "Person " + person.getName() + " must be at least 18 years old")
        // ...
        .build();
```
In this case, the error message will include the person's name, providing a more personalized error message.

## Contributing
If you'd like to contribute to the Functional Validator project, please follow these steps:

Fork the repository on GitHub.
Create a new branch for your feature or bug fix.
Implement your changes and ensure that all tests pass.
Submit a pull request to the main repository.
We appreciate your contributions and feedback!

## License
The Functional Validator is open-source software released under the BSD License.

