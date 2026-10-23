package com.solaria.notify.push;

import java.util.List;

public record PushResult(int sent, int failed, List<String> invalidTokens) {
}
