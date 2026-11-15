package com.vinamra.payflow.model;

/** A registered user. Only a salted hash of the password is ever stored. */
public record User(String id, String username, String displayName, String passwordHash, Role role) {
}
