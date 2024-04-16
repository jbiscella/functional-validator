package org.jacopobiscella.functional.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

public class ValidatorTest {

	@Test
	public void testValidationSuccess() {
		Validator<Address> addressValidator = Validator.<Address>builder()
				.setNotNullable(Address.class)
				.addConstraint(
						address -> address.getStreet() != null && !address.getStreet().isEmpty(),
						address -> "Address street cannot be null or empty")
				.addConstraint(address -> address.getCity() != null && !address.getCity().isEmpty(),
						address -> "Address city cannot be null or empty")
				.build();
		Validator<Person> validator = Validator.<Person>builder().setNotNullable(Person.class)
				.addConstraint(person -> person.getAge() >= 18,
						person -> "Person must be at least 18 years old")
				.addConstraint(person -> person.getName() != null && !person.getName().isEmpty(),
						person -> "Person name cannot be null or empty")
				.addNestedValidator(Person::getAddress, addressValidator).build();

		Address validAddress = new Address("test street", "test city");

		Person validPerson = new Person("John", 25, validAddress);
		Optional<Throwable> validationError = validator.getValidationError(validPerson);
		assertFalse(validationError.isPresent());
	}

	@Test
	public void testValidationFailure() {
		Validator<Person> validator = Validator.<Person>builder().setNotNullable(Person.class)
				.addConstraint(person -> person.getAge() >= 18,
						person -> "Person must be at least 18 years old")
				.addConstraint(person -> person.getName() != null && !person.getName().isEmpty(),
						person -> "Person name cannot be null or empty")
				.build();

		Person invalidPerson = new Person(null, 15);
		Optional<Throwable> validationError = validator.getValidationError(invalidPerson);
		assertTrue(validationError.isPresent());
		assertEquals("Person must be at least 18 years old, Person name cannot be null or empty",
				validationError.get().getMessage());
	}

	@Test
	public void testValidationWithPrefix() {
		Validator<Person> validator = Validator.<Person>builder().setNotNullable(Person.class)
				.setErrorMessagePrefix(
						person -> "Validation failed for person: " + person.getName() + " - ")
				.addConstraint(person -> person.getAge() >= 18,
						person -> "Person must be at least 18 years old")
				.build();

		Person invalidPerson = new Person("Alice", 15);
		Optional<Throwable> validationError = validator.getValidationError(invalidPerson);
		assertTrue(validationError.isPresent());
		assertEquals("Validation failed for person: Alice - Person must be at least 18 years old",
				validationError.get().getMessage());
	}

	@Test
	public void testNestedValidation() {
		Validator<Address> addressValidator = Validator.<Address>builder()
				.setNotNullable(Address.class)
				.setErrorMessagePrefix(address -> "Validation failed for address: ")
				.addConstraint(
						address -> address.getStreet() != null && !address.getStreet().isEmpty(),
						address -> "Address street cannot be null or empty")
				.addConstraint(address -> address.getCity() != null && !address.getCity().isEmpty(),
						address -> "Address city cannot be null or empty")
				.build();

		Validator<Person> personValidator = Validator.<Person>builder().setNotNullable(Person.class)
				.addConstraint(person -> person.getAge() >= 18,
						person -> "Person must be at least 18 years old")
				.addNestedValidator(Person::getAddress, addressValidator).build();

		Address invalidAddress = new Address(null, "");
		Person invalidPerson = new Person("Bob", 20, invalidAddress);
		Optional<Throwable> validationError = personValidator.getValidationError(invalidPerson);
		assertTrue(validationError.isPresent());
		assertEquals(
				"(Validation failed for address: Address street cannot be null or empty, Address city cannot be null or empty)",
				validationError.get().getMessage());
	}

	@Test
	public void testNestedValidationNullPrefix() {
		Validator<Address> addressValidator = Validator.<Address>builder()
				.setNotNullable(Address.class)
				.addConstraint(
						address -> address.getStreet() != null && !address.getStreet().isEmpty(),
						address -> "Address street cannot be null or empty")
				.addConstraint(address -> address.getCity() != null && !address.getCity().isEmpty(),
						address -> "Address city cannot be null or empty")
				.build();

		Validator<Person> personValidator = Validator.<Person>builder().setNotNullable(Person.class)
				.addConstraint(person -> person.getAge() >= 18,
						person -> "Person must be at least 18 years old")
				.addNestedValidator(Person::getAddress, addressValidator).build();

		Address invalidAddress = new Address(null, "");
		Person invalidPerson = new Person("Bob", 20, invalidAddress);
		Optional<Throwable> validationError = personValidator.getValidationError(invalidPerson);
		assertTrue(validationError.isPresent());
		assertEquals(
				"(Address street cannot be null or empty, Address city cannot be null or empty)",
				validationError.get().getMessage());
	}

	// Helper classes for testing
	private static class Person {
		private final String name;
		private final int age;
		private final Address address;

		public Person(String name, int age) {
			this(name, age, null);
		}

		public Person(String name, int age, Address address) {
			this.name = name;
			this.age = age;
			this.address = address;
		}

		public String getName() {
			return name;
		}

		public int getAge() {
			return age;
		}

		public Address getAddress() {
			return address;
		}
	}

	private static class Address {
		private final String street;
		private final String city;

		public Address(String street, String city) {
			this.street = street;
			this.city = city;
		}

		public String getStreet() {
			return street;
		}

		public String getCity() {
			return city;
		}
	}
}
