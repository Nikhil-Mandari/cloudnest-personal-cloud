package com.cloudnest.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the {@link DeviceInfoParser}.
 */
class DeviceInfoParserTest {

    @Test
    @DisplayName("Classifies a desktop Chrome on Windows")
    void chromeOnWindows() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

        DeviceInfo info = DeviceInfoParser.parse(ua, "dev-1");

        assertEquals("Chrome", info.browser());
        assertEquals("Windows", info.os());
        assertEquals("DESKTOP", info.deviceType());
        assertEquals("Chrome on Windows", info.deviceName());
        assertEquals("dev-1", info.deviceId());
    }

    @Test
    @DisplayName("Classifies an iPhone running Safari as mobile")
    void iPhoneSafari() {
        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) "
                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1";

        DeviceInfo info = DeviceInfoParser.parse(ua, "dev-2");

        assertEquals("Safari", info.browser());
        assertEquals("iOS", info.os());
        assertEquals("MOBILE", info.deviceType());
    }

    @Test
    @DisplayName("Classifies an Android tablet")
    void androidTablet() {
        String ua = "Mozilla/5.0 (Linux; Android 14; SM-X700) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

        DeviceInfo info = DeviceInfoParser.parse(ua, "dev-3");

        assertEquals("Android", info.os());
        assertEquals("TABLET", info.deviceType());
    }

    @Test
    @DisplayName("Falls back gracefully for unknown agents")
    void unknownAgent() {
        DeviceInfo info = DeviceInfoParser.parse(null, "dev-4");

        assertEquals(DeviceInfo.UNKNOWN, info.browser());
        assertEquals(DeviceInfo.UNKNOWN, info.os());
        assertEquals("OTHER", info.deviceType());
        assertEquals("Unknown device", info.deviceName());
    }

    @Test
    @DisplayName("Edge and Firefox are not mistaken for Chrome")
    void edgeAndFirefox() {
        String edge = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0";
        assertEquals("Edge", DeviceInfoParser.parse(edge, "e").browser());

        String firefox = "Mozilla/5.0 (X11; Linux x86_64; rv:127.0) Gecko/20100101 Firefox/127.0";
        assertEquals("Firefox", DeviceInfoParser.parse(firefox, "f").browser());
        assertEquals("Linux", DeviceInfoParser.parse(firefox, "f").os());
    }
}
