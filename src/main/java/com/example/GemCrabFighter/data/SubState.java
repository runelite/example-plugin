package com.example.GemCrabFighter.data;

public enum SubState {
    // Idle,
    IDLE,
    // Banking
    FIND_BANK,
    WITHDRAW,
    DEPOSIT,
    // Traveling
    TELE_GE,
    TELE_EDGE,
    TELE_CIVITAS,
    MOVE_DOWNSTAIRS,
    OPEN_DOOR,
    WALK_DOOR,
    ENTER_LAIR,
    DRINK_POOL,
    // Consume,
    ACTIVATE_PRAYER,
    DEACTIVATE_PRAYER,
    DRINK_POTIONS,
    EAT_FOOD,
    // Combat,
    ATTACK_CRAB,
    MINE_CRAB,
    EQUIP_GEAR,
    USE_SPECIAL,
}
