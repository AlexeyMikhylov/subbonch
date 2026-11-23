package ru.sut.subbonch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Запрос регистрации. Пароль не попадает в toString/логи.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String passwordConfirm;

    @Override
    public String toString() {
        return "RegisterRequest{email='" + email + "', firstName='" + firstName
                + "', lastName='" + lastName + "'}";
    }
}
