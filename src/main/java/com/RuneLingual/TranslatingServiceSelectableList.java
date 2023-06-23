package com.RuneLingual;

import lombok.Getter;

public enum TranslatingServiceSelectableList {
    GOOGLE_TRANSLATE ("google"),
    OPENAI_GPT ("gpt");

    @Getter
    private final String serviceName;

    TranslatingServiceSelectableList(String code){this.serviceName = code;}

    public String getService(){return this.serviceName;}


}
