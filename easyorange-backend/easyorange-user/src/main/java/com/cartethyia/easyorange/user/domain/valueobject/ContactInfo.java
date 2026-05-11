package com.cartethyia.easyorange.user.domain.valueobject;

public record ContactInfo(String email, String phone) {
    private static final String EMAIL_REGEX = "^[\\w.-]+@[\\w.-]+\\.\\w+$";
    private static final String PHONE_REGEX = "^1\\d{10}$";

    public ContactInfo {
        if (email != null && !email.isBlank() && !email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        if (phone != null && !phone.isBlank() && !phone.matches(PHONE_REGEX)) {
            throw new IllegalArgumentException("Invalid phone format: " + phone);
        }
    }

    public static ContactInfo empty() {
        return new ContactInfo(null, null);
    }

    public ContactInfo withEmail(String newEmail) {
        return new ContactInfo(newEmail, phone);
    }

    public ContactInfo withPhone(String newPhone) {
        return new ContactInfo(email, newPhone);
    }
}
