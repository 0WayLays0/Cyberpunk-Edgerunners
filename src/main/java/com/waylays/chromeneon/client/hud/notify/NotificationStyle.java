package com.waylays.chromeneon.client.hud.notify;

import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.hud.NotificationKind;

/** The look of each kind: colour, icon, technical code, and whether it arrives with a glitch. */
public record NotificationStyle(int color, HudIcon icon, String code, boolean glitch, boolean hazard) {
    public static NotificationStyle of(NotificationKind kind) {
        return switch (kind) {
            case INFO -> new NotificationStyle(NeonPalette.CYAN, HudIcon.INFO, "SYS.INFO", false, false);
            case SYSTEM_ON -> new NotificationStyle(NeonPalette.CYAN, HudIcon.POWER, "SYS.ON", false, false);
            case SYSTEM_OFF -> new NotificationStyle(NeonPalette.LAVENDER, HudIcon.POWER, "SYS.OFF", false, false);
            case CYBERWARE -> new NotificationStyle(NeonPalette.VIOLET_HOT, HudIcon.CHIP, "CYBR.DTC", false, false);
            case SCAN -> new NotificationStyle(NeonPalette.CYAN, HudIcon.SCAN, "SCAN", false, false);
            case WARNING -> new NotificationStyle(NeonPalette.ORANGE, HudIcon.BAD, "WARN", false, false);
            case ALERT -> new NotificationStyle(NeonPalette.RED, HudIcon.HAZARD, "ALERT", true, true);
            case ERROR -> new NotificationStyle(NeonPalette.RED, HudIcon.CROSS, "ERR", true, false);
        };
    }
}
