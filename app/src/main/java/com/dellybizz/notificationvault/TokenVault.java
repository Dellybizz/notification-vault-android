package com.dellybizz.notificationvault;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class TokenVault {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "notification_vault_device_token_v1";
    private static final String PREFS = "notification_vault_secure";
    private static final String PREF_IV = "token_iv";
    private static final String PREF_CIPHERTEXT = "token_ciphertext";

    private final SharedPreferences preferences;

    TokenVault(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void save(String token) throws Exception {
        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] ciphertext = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));

        preferences.edit()
                .putString(PREF_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString(PREF_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .apply();
    }

    String load() throws Exception {
        String ivValue = preferences.getString(PREF_IV, null);
        String ciphertextValue = preferences.getString(PREF_CIPHERTEXT, null);
        if (ivValue == null || ciphertextValue == null) return null;

        SecretKey key = getOrCreateKey();
        byte[] iv = Base64.decode(ivValue, Base64.NO_WRAP);
        byte[] ciphertext = Base64.decode(ciphertextValue, Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    boolean hasToken() {
        return preferences.contains(PREF_IV) && preferences.contains(PREF_CIPHERTEXT);
    }

    void clear() {
        preferences.edit().remove(PREF_IV).remove(PREF_CIPHERTEXT).apply();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }
}
