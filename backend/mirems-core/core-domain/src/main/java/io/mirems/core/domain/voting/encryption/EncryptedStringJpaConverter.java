package io.mirems.core.domain.voting.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Base64;
import java.util.Objects;

/** JPA converter for String PII attributes annotated with {@link Encrypted}. */
@Converter
public class EncryptedStringJpaConverter implements AttributeConverter<String, String> {
    private static final String KEY_ENV = "MIREMS_PII_ENCRYPTION_KEY_BASE64";

    private final PiiEncryptionService encryptionService;

    public EncryptedStringJpaConverter() {
        this(new PiiEncryptionService(loadKeyFromEnvironment()));
    }

    public EncryptedStringJpaConverter(PiiEncryptionService encryptionService) {
        this.encryptionService = Objects.requireNonNull(encryptionService, "encryptionService is required");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptionService.decrypt(dbData);
    }

    private static byte[] loadKeyFromEnvironment() {
        String key = System.getenv(KEY_ENV);
        if (key == null || key.isBlank()) {
            throw new PiiEncryptionException(KEY_ENV + " is required for encrypted JPA conversion");
        }
        return Base64.getDecoder().decode(key);
    }
}
