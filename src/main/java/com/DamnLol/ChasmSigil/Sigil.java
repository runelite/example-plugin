package com.DamnLol.ChasmSigil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Sigil
{
    GREEN ("Divine Severance", "Heavy ranged weapon"),
    BLUE  ("Sensory Clouding", "Spell (Not Powered)"),
    YELLOW("Glyphic Attenuation", "Special attack"),
    RED   ("Bloodied Blows", "Two-handed melee"),
    PURPLE("Forfeit Breath", "Kill demon when bound"),
    UNKNOWN("", "");

    private final String contractName;
    private final String description;
}