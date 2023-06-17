package com.RuneLingual;

import lombok.Getter;
public enum SendUpdates {
    UPPON_LOGOUT ("logout"),
    ANYTIME ("any"),
    NEVER ("never");

    @Getter
    private final String updateSetting;

    SendUpdates(String updateSetting){this.updateSetting = updateSetting;}

    public String getSetting(){return this.updateSetting;}

}
