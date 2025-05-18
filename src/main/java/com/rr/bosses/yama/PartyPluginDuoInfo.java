package com.rr.bosses.yama;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.party.messages.PartyMemberMessage;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PartyPluginDuoInfo extends PartyMemberMessage
{
    boolean currentlyInsideYamasDomain;

    public void resetFields()
    {
        this.currentlyInsideYamasDomain = false;
        setMemberId(0L);
    }
}
