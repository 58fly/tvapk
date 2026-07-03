package com.pingfeng.tvapk.chromium;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KernelInfoParser {
    private static final Pattern CHROMIUM_VERSION_PATTERN = Pattern.compile("Chrome/(\\d+)\\.");

    private KernelInfoParser() {
    }

    public static int extractChromiumMajor(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return -1;
        }
        Matcher matcher = CHROMIUM_VERSION_PATTERN.matcher(userAgent);
        if (!matcher.find()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
