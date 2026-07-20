/*
 * Copyright (c) 2024, Settings Profiles Plugin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.settingsprofile.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.VarbitID;

/**
 * Varbit IDs that represent persistent user preferences.
 * <p>
 * Varbits are bit-fields packed within VarPlayers. They are saved individually
 * and restored via {@code client.setVarbitValue(varps, id, value)}.
 */
@Getter
@RequiredArgsConstructor
public enum TrackedVarbit
{
	// ── Interface appearance ─────────────────────────────────────────────────
	CHATBOX_TRANSPARENCY("Transparent Chatbox", VarbitID.CHATBOX_TRANSPARENCY),
	SIDE_TRANSPARENCY("Transparent Side Panels", VarbitID.SIDE_TRANSPARENCY),
	TRANSPARENT_CHATBOX_BLOCKCLICK("Transparent Chatbox: Block Click-Through", VarbitID.TRANSPARENT_CHATBOX_BLOCKCLICK),
	RESIZABLE_STONE_ARRANGEMENT("Resizable Sidebar Layout", VarbitID.RESIZABLE_STONE_ARRANGEMENT),
	CHAT_SCROLLBAR_SIDE("Chat Scrollbar Side", VarbitID.CHATBOX_SCROLLBARSIDE),
	MINIMENU_SCROLLBAR("Mini-Menu Scrollbar Toggle", VarbitID.SETTINGS_MINIMENU_SCROLLBAR_TOGGLE),

	// ── World rendering ──────────────────────────────────────────────────────
	HIDE_ROOFTOPS("Hide Rooftops", VarbitID.OPTION_HIDE_ROOFTOPS),
	FOV_CLAMP("Field of View Clamp", VarbitID.FOV_CLAMP),
	MULTIWAY_INDICATOR("Multicombat Area Indicator", VarbitID.MULTIWAY_INDICATOR),

	// ── Audio ────────────────────────────────────────────────────────────────
	MUSIC_LOOP("Music Loop", VarbitID.MUSIC_ENABLELOOP),
	HIT_SOUNDS("Hit Sounds", VarbitID.SETTINGS_HIT_SOUNDS),

	// ── Chat settings ────────────────────────────────────────────────────────
	CHAT_TIMESTAMPS("Chat Timestamps", VarbitID.SETTINGS_CHAT_TIMESTAMPS),

	// ── Keyboard shortcuts ───────────────────────────────────────────────────
	HOTKEY_CANNOT_CLOSE_SIDEPANEL("Hotkey: Cannot Close Side Panel", VarbitID.HOTKEY_CANNOT_CLOSE_SIDEPANEL),
	KEYBINDING_ESC_TO_CLOSE("Escape to Close Interface", VarbitID.KEYBINDING_ESC_TO_CLOSE),
	KEYBINDING_STONE_SELECTION("Keybind Stone Selection Mode", VarbitID.KEYBINDING_STONE_SELECTION),

	// Sidebar tab hotkeys
	STONE_COMBAT_KEY("Hotkey: Combat Tab", VarbitID.STONE_COMBAT_KEY),
	STONE_STATS_KEY("Hotkey: Stats Tab", VarbitID.STONE_STATS_KEY),
	STONE_JOURNAL_KEY("Hotkey: Quest/Achievement Tab", VarbitID.STONE_JOURNAL_KEY),
	STONE_INV_KEY("Hotkey: Inventory Tab", VarbitID.STONE_INV_KEY),
	STONE_WORN_KEY("Hotkey: Equipment Tab", VarbitID.STONE_WORN_KEY),
	STONE_PRAYER_KEY("Hotkey: Prayer Tab", VarbitID.STONE_PRAYER_KEY),
	STONE_MAGIC_KEY("Hotkey: Magic Tab", VarbitID.STONE_MAGIC_KEY),
	STONE_CLANCHAT_KEY("Hotkey: Clan Chat Tab", VarbitID.STONE_CLANCHAT_KEY),
	STONE_FRIENDS_KEY("Hotkey: Friends Tab", VarbitID.STONE_FRIENDS_KEY),
	STONE_OPTIONS1_KEY("Hotkey: Options 1 Tab", VarbitID.STONE_OPTIONS1_KEY),
	STONE_OPTIONS2_KEY("Hotkey: Options 2 Tab", VarbitID.STONE_OPTIONS2_KEY),
	STONE_MUSIC_KEY("Hotkey: Music Tab", VarbitID.STONE_MUSIC_KEY),
	STONE_LOGOUT_KEY("Hotkey: Logout Tab", VarbitID.STONE_LOGOUT_KEY),

	// ── XP Drops ─────────────────────────────────────────────────────────────
	XPDROPS_ENABLED("XP Drops Enabled", VarbitID.XPDROPS_ENABLED),
	XPDROPS_POSITION("XP Drops Position", VarbitID.XPDROPS_POSITION),
	XPDROPS_SIZE("XP Drops Size", VarbitID.XPDROPS_SIZE),
	XPDROPS_DURATION("XP Drops Duration", VarbitID.XPDROPS_DURATION),
	XPDROPS_COLOUR("XP Drops Colour", VarbitID.XPDROPS_COLOUR),
	XPDROPS_GROUPSKILLS("XP Drops Group Skills", VarbitID.XPDROPS_GROUPSKILLS),
	XPDROPS_COUNTER_TYPE("XP Counter Type", VarbitID.XPDROPS_COUNTER_TYPE),
	XPDROPS_PROGRESS_TYPE("XP Progress Type", VarbitID.XPDROPS_PROGRESS_TYPE),
	XPDROPS_SPEED("XP Drops Speed", VarbitID.XPDROPS_SPEED),

	// ── Ground items ─────────────────────────────────────────────────────────
	GROUND_ITEMS_ENABLED("Ground Items Enabled", VarbitID.GROUND_ITEMS_ENABLED),
	GROUND_ITEMS_PRICE_TYPE("Ground Items Price Type", VarbitID.GROUND_ITEMS_PRICE_TYPE),
	GROUND_ITEMS_MODIFIER_KEY("Ground Items Modifier Key", VarbitID.GROUND_ITEMS_MODIFIER_KEY),

	// ── Miscellaneous ────────────────────────────────────────────────────────
	SETTINGS_NEW_MENU_TRANSPARENT("New Menu: Transparent Interface Disabled", VarbitID.SETTINGS_NEW_MENU_TRANSPARENT_INTERFACE_DISABLED);

	private final String displayName;
	private final int id;
}
