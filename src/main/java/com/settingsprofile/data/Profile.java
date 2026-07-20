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

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a saved snapshot of game settings.
 * <p>
 * Maps use String keys (numeric IDs as strings) to avoid Gson type-erasure issues
 * with Map&lt;Integer, Integer&gt;.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Profile
{
	/** Human-readable name chosen by the user. */
	private String name;

	/** Player name this profile belongs to, or {@code null} for global profiles. */
	private String accountName;

	/** VarPlayer values keyed by their integer ID (as String). */
	private Map<String, Integer> varpValues;

	/** Varbit values keyed by their integer ID (as String). */
	private Map<String, Integer> varbitValues;

	/** VarClientInt values keyed by their integer ID (as String). */
	private Map<String, Integer> varClientIntValues;

	/** Epoch millis when this profile was first created. */
	private long createdAt;

	/** Epoch millis when this profile was last saved. */
	private long updatedAt;
}
