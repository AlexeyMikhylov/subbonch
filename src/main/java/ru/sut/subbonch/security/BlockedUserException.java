package ru.sut.subbonch.security;

import org.springframework.security.authentication.AccountStatusException;

/**
 * Аккаунт заблокирован ({@code Users.is_blocked = true}). Несёт причину
 * ({@code block_reason}), чтобы показать её на странице входа.
 */
public class BlockedUserException extends AccountStatusException {

    private static final long serialVersionUID = 1L;

    private final String blockReason;

    public BlockedUserException(String blockReason) {
        super("User account is blocked");
        this.blockReason = blockReason;
    }

    public String getBlockReason() {
        return blockReason;
    }
}
