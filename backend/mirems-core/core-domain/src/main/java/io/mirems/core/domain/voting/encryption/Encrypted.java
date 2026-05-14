package io.mirems.core.domain.voting.encryption;

import jakarta.persistence.Convert;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a String persistence attribute as encrypted via the MiREMS encrypted string converter. */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Convert(converter = EncryptedStringJpaConverter.class)
public @interface Encrypted {}
