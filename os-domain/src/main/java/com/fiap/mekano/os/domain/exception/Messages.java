package com.fiap.mekano.os.domain.exception;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/** Acesso às mensagens de erro de domínio ({@code messages.properties}). */
public final class Messages {

    private static final ResourceBundle BUNDLE =
            ResourceBundle.getBundle("com.fiap.mekano.os.domain.exception.messages", Locale.ROOT);

    private Messages() {
    }

    /**
     * Resolve a mensagem da chave informada, aplicando {@link MessageFormat} nos argumentos.
     *
     * @param key  chave em messages.properties
     * @param args argumentos posicionais ({0}, {1}, ...)
     * @return mensagem formatada
     */
    public static String get(String key, Object... args) {
        String pattern = BUNDLE.getString(key);
        return args == null || args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }
}
