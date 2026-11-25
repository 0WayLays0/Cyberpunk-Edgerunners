package com.waylays.chromeneon.client.hud.notify;

import net.minecraft.network.chat.Component;

/**
 * An incoming call to show. {@code playerName} lets the popup show that player's face when they are online;
 * otherwise a pattern generated from the caller's name stands in. The callbacks run when the player answers
 * or refuses with their keys; a call left ringing past {@code timeoutTicks} is missed (refused).
 */
public record IncomingCall(Component caller, String playerName, Component channel, Runnable onAccept, Runnable onDecline,
                           int timeoutTicks) {}
