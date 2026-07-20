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
import net.runelite.api.gameval.VarClientID;

/**
 * VarClientInt IDs that represent client-side user preferences.
 * <p>
 * These are stored locally (not synced with the server) and are restored via
 * {@code client.setVarcIntValue(id, value)}.
 */
@Getter
@RequiredArgsConstructor
public enum TrackedVarClientInt
{
	/** Camera zoom level in fixed-size viewport mode. */
	CAMERA_ZOOM_FIXED("Camera Zoom (Fixed Viewport)", VarClientID.CAMERA_ZOOM_SMALL),

	/** Camera zoom level in resizable viewport mode. */
	CAMERA_ZOOM_RESIZABLE("Camera Zoom (Resizable Viewport)", VarClientID.CAMERA_ZOOM_BIG),

	/** Draw distance slider value. */
	DRAW_DISTANCE("Draw Distance", VarClientID.SETTINGS_DRAW_DISTANCE);

	private final String displayName;
	private final int id;
}
