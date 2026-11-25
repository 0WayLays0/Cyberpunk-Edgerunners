package com.waylays.chromeneon.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Chrome Néon's keys (Options > Controls > Chrome Néon). None of the defaults is used by vanilla. */
public final class ChromeKeys {
    private static final String CATEGORY = "key.categories.chromeneon";

    public static final KeyMapping SCAN = key("scan", GLFW.GLFW_KEY_V);
    public static final KeyMapping MARKER = key("marker", GLFW.GLFW_KEY_B);
    public static final KeyMapping ACCEPT_CALL = key("accept_call", GLFW.GLFW_KEY_Y);
    public static final KeyMapping DECLINE_CALL = key("decline_call", GLFW.GLFW_KEY_N);
    public static final KeyMapping TOGGLE_HUD = key("toggle_hud", GLFW.GLFW_KEY_H);
    public static final KeyMapping ACCELERATOR = key("accelerator", GLFW.GLFW_KEY_Z);
    public static final KeyMapping BLADES = key("blades", GLFW.GLFW_KEY_R);
    public static final KeyMapping ZOOM = key("zoom", GLFW.GLFW_KEY_G);
    public static final KeyMapping ANALYSIS = key("analysis", GLFW.GLFW_KEY_U);
    public static final KeyMapping CYBERWARE = key("cyberware", GLFW.GLFW_KEY_K);

    private ChromeKeys() {}

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.chromeneon." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SCAN);
        event.register(MARKER);
        event.register(ACCEPT_CALL);
        event.register(DECLINE_CALL);
        event.register(TOGGLE_HUD);
        event.register(ACCELERATOR);
        event.register(BLADES);
        event.register(ZOOM);
        event.register(ANALYSIS);
        event.register(CYBERWARE);
    }
}
