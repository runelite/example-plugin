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
import net.runelite.api.gameval.VarPlayerID;

/**
 * VarPlayer IDs that represent persistent user preferences.
 * <p>
 * VarPlayers are whole integers synced with the server. We save the raw value
 * and restore it by writing directly into the client's varps array.
 */
@Getter
@RequiredArgsConstructor
public enum TrackedVarPlayer
{
	// ── Display ─────────────────────────────────────────────────────────────
	BRIGHTNESS("Brightness", VarPlayerID.OPTION_BRIGHTNESS),

	// ── Audio ────────────────────────────────────────────────────────────────
	JINGLES("Jingles Toggle", VarPlayerID.OPTION_JINGLES),
	MUSIC_VOLUME("Music Volume", VarPlayerID.OPTION_MUSIC),
	SOUND_FX_VOLUME("Sound FX Volume", VarPlayerID.OPTION_SOUNDS),
	AREA_SOUNDS_VOLUME("Area Sounds Volume", VarPlayerID.OPTION_AREASOUNDS),
	MASTER_VOLUME("Master Volume", VarPlayerID.OPTION_MASTER_VOLUME),

	// ── Input ────────────────────────────────────────────────────────────────
	MOUSE_BUTTON("Mouse Button Mode", VarPlayerID.OPTION_MOUSE),

	// ── Gameplay ─────────────────────────────────────────────────────────────
	CHAT_MODE("Chat Modes", VarPlayerID.OPTION_CHAT),
	ATTACK_OPTIONS("Attack Options", VarPlayerID.OPTION_NODEF),
	AUTO_RUN("Auto-Run Toggle", VarPlayerID.OPTION_RUN),
	PRIVATE_MESSAGES("Private Messages", VarPlayerID.OPTION_PM),
	ACCEPT_AID("Accept Aid", VarPlayerID.OPTION_AID),
	PROFANITY_FILTER("Profanity Filter Disabled", VarPlayerID.OPTION_CHATFILTER_DISABLED),
	ATTACK_PRIORITY("Player Attack Priority", VarPlayerID.OPTION_ATTACKPRIORITY),
	ATTACK_PRIORITY_NPC("NPC Attack Priority", VarPlayerID.OPTION_ATTACKPRIORITY_NPC),

	// ── XP Drops (packed options varplayer) ─────────────────────────────────
	XPDROPS_OPTIONS("XP Drops Options Pack", VarPlayerID.XPDROPS_OPTIONS),

	// ── Chat colours – opaque chatbox ───────────────────────────────────────
	CHAT_COLOUR_PUBLIC_OPAQUE("Chat: Public (Opaque)", VarPlayerID.OPTION_CHAT_COLOUR_PUBLIC_OPAQUE),
	CHAT_COLOUR_PRIVATE_OPAQUE("Chat: Private (Opaque)", VarPlayerID.OPTION_CHAT_COLOUR_PRIVATE_OPAQUE),
	CHAT_COLOUR_AUTOCHAT_OPAQUE("Chat: Auto-chat (Opaque)", VarPlayerID.OPTION_CHAT_COLOUR_AUTOCHAT_OPAQUE),
	CHAT_COLOUR_BROADCAST_OPAQUE("Chat: Broadcast (Opaque)", VarPlayerID.OPTION_CHAT_COLOUR_BROADCAST_OPAQUE),
	CHAT_COLOUR_FRIENDSCHAT_OPAQUE("Chat: Friends Chat (Opaque)", VarPlayerID.OPTION_CHAT_COLOUR_FRIENDSCHAT_OPAQUE),
	CHAT_COLOUR_CLANCHAT_OPAQUE("Chat: Clan Chat (Opaque)", VarPlayerID.OPTION_CHAT_COLOUR_CLANCHAT_OPAQUE),
	CHAT_COLOUR_TRADEREQ_OPAQUE("Chat: Trade Request (Opaque)", VarPlayerID.OPTION_CHAT_COLOUR_TRADEREQ_OPAQUE),
	CHAT_COLOUR_CHALLENGEREQ_OPAQUE("Chat: Challenge Req (Opaque)", VarPlayerID.OPTION_CHAT_COLOUR_CHALLENGEREQ_OPAQUE),

	// ── Chat colours – transparent chatbox ──────────────────────────────────
	CHAT_COLOUR_PUBLIC_TRANSPARENT("Chat: Public (Transparent)", VarPlayerID.OPTION_CHAT_COLOUR_PUBLIC_TRANSPARENT),
	CHAT_COLOUR_PRIVATE_TRANSPARENT("Chat: Private (Transparent)", VarPlayerID.OPTION_CHAT_COLOUR_PRIVATE_TRANSPARENT),
	CHAT_COLOUR_AUTOCHAT_TRANSPARENT("Chat: Auto-chat (Transparent)", VarPlayerID.OPTION_CHAT_COLOUR_AUTOCHAT_TRANSPARENT),
	CHAT_COLOUR_BROADCAST_TRANSPARENT("Chat: Broadcast (Transparent)", VarPlayerID.OPTION_CHAT_COLOUR_BROADCAST_TRANSPARENT),
	CHAT_COLOUR_FRIENDSCHAT_TRANSPARENT("Chat: Friends Chat (Transparent)", VarPlayerID.OPTION_CHAT_COLOUR_FRIENDSCHAT_TRANSPARENT),
	CHAT_COLOUR_CLANCHAT_TRANSPARENT("Chat: Clan Chat (Transparent)", VarPlayerID.OPTION_CHAT_COLOUR_CLANCHAT_TRANSPARENT),
	CHAT_COLOUR_TRADEREQ_TRANSPARENT("Chat: Trade Request (Transparent)", VarPlayerID.OPTION_CHAT_COLOUR_TRADEREQ_TRANSPARENT),
	CHAT_COLOUR_CHALLENGEREQ_TRANSPARENT("Chat: Challenge Req (Transparent)", VarPlayerID.OPTION_CHAT_COLOUR_CHALLENGEREQ_TRANSPARENT),
	CHAT_COLOUR_GUESTCLAN_OPAQUE("Chat: Guest Clan (Opaque)", VarPlayerID.OPTION_CHAT_COLOUR_GUESTCLAN_OPAQUE),
	CHAT_COLOUR_GUESTCLAN_TRANSPARENT("Chat: Guest Clan (Transparent)", VarPlayerID.OPTION_CHAT_COLOUR_GUESTCLAN_TRANSPARENT),

	// ── Chat colours – split private chat ────────────────────────────────────
	CHAT_COLOUR_PRIVATE_SPLIT("Chat: Private (Split)", VarPlayerID.OPTION_CHAT_COLOUR_PRIVATE_SPLIT),
	CHAT_COLOUR_BROADCAST_SPLIT("Chat: Broadcast (Split)", VarPlayerID.OPTION_CHAT_COLOUR_BROADCAST_SPLIT);

	private final String displayName;
	private final int id;
}
