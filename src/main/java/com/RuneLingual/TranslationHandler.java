package com.RuneLingual;

@FunctionalInterface
public interface TranslationHandler
{
	public String translate(String originalMessage);
}
