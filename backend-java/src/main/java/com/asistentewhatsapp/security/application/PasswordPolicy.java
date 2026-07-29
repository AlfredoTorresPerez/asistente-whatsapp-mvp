package com.asistentewhatsapp.security.application;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

	public void validate(String password) {
		if (password == null || password.length() < 8 || password.length() > 72) {
			throw new IllegalArgumentException("La contrasena debe tener entre 8 y 72 caracteres.");
		}
		if (password.chars().noneMatch(Character::isUpperCase)) {
			throw new IllegalArgumentException("La contrasena debe incluir al menos una mayuscula.");
		}
		if (password.chars().noneMatch(Character::isDigit)) {
			throw new IllegalArgumentException("La contrasena debe incluir al menos un numero.");
		}
	}
}
