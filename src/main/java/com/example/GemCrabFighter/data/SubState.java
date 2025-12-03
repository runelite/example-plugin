package com.example.GemCrabFighter.data;

public enum SubState {
    // Idle,
    IDLE,
    // Banking
    FIND_BANK,
    WITHDRAW,
    DEPOSIT,

    // Traveling
    TELE_FEROX,
    TELE_CIVITAS,
    MOVE_TO_CRAB,
    ENTER_CRAB_TUNNEL,
    MOVE_TO_BIRD,

    // Combat,
    ATTACK_CRAB,
    MINE_CRAB,
    EQUIP_GEAR,
    USE_SPECIAL,
}
