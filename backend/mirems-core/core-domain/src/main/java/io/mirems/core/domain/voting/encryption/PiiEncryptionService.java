package io.mirems.core.domain.voting.encryption;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-GCM service for personally identifiable voter data. */
public class PiiEncryptionService {
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom;

    public PiiEncryptionService(byte[] key) {
        this(key, new SecureRandom());
    }

    PiiEncryptionService(byte[] key, SecureRandom secureRandom) {
        byte[] defensiveKey = Objects.requireNonNull(key, "key is required").clone();
        if (defensiveKey.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("AES-256 key must be exactly 32 bytes");
        }
        this.keySpec = new SecretKeySpec(defensiveKey, KEY_ALGORITHM);
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom is required");
        Arrays.fill(defensiveKey, (byte) 0);
    }

    public String encrypt(String plaintext) {
        String value = requireText(plaintext, "plaintext");
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new PiiEncryptionException("Failed to encrypt PII", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        String value = requireText(encryptedValue, "encryptedValue");
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new PiiEncryptionException("Encrypted PII payload is not valid Base64", exception);
        }
        if (payload.length <= IV_LENGTH_BYTES) {
            throw new PiiEncryptionException("Encrypted PII payload is invalid");
        }
        byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new PiiEncryptionException("Failed to decrypt PII", exception);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
