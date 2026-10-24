package com.solaria.notify.push;

import java.util.List;

public record PushResult(int sent, int failed, List<String> invalidTokens) {

    // invalidTokens nunca é nulo, mesmo que o sender devolva null
    public PushResult {
        invalidTokens = invalidTokens == null ? List.of() : List.copyOf(invalidTokens);
    }
}
