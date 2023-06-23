package com.RuneLingual;

import lombok.Getter;

public enum LangCodeSelectableList {
    ENGLISH ("en"),
    PORTUGUÊS_BRASILEIRO ("pt-br");

    @Getter
    private final String langCode;

    LangCodeSelectableList(String langCode){this.langCode = langCode;}

    public String getCode(){return this.langCode;}


}
