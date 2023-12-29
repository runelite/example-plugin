package com.RuneLingual;

@FunctionalInterface
public interface MessageReplacer
{
	String replace(String oldMessage, String newMessage);
}
