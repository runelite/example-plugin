package com.RuneLingual;

import lombok.Getter;

public enum LangCode{
    ENGLISH ("en"),
    PORTUGUÊS_BRASILEIRO ("pt-br");

    @Getter
    private final String langCode;

    LangCode(String langCode){this.langCode = langCode;}

    public String getCode(){return this.langCode;}


}
