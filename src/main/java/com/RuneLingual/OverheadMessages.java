package com.RuneLingual;

// java libs
import java.util.List;
import java.util.stream.Collectors;

// runelite api
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;

public class OverheadMessages
{
    // manages overhead messages
    // requires a periodic update of actors nearby
    // TODO: test this
    
    private List<Player> playersNearby;
    private List<NPC> npcsNearby;
    private Client clientReference;
    private boolean debugPrints = true;
    
    OverheadMessages(Client clientReference, boolean debugPrints)
    {
        this.debugPrints = debugPrints;
        this.clientReference = clientReference;
    }
    
    public void replace(String oldMessage, String newMessage) throws Exception
    {
        // debug
        boolean messageFound = false;
        String senderName = "";

        if(oldMessage == null || newMessage == null)
        {
            throw new Exception("Can't translate(to) null messages!");
        }
        
        // iterates on a list of nearby players
        for(Player player : playersNearby)
        {
            // checks for message contents
            if(player.getOverheadText() == oldMessage)
            {
                // replaces overhead text
                player.setOverheadText(newMessage);
                
                if(debugPrints)
                {
                    messageFound = true;
                    senderName = player.getName();
                }
            }
        }
        
        // iterates on a list of nearby NPCs
        for(NPC actor : npcsNearby)
        {
            // checks for message contents
            if(actor.getOverheadText() == oldMessage)
            {
                // replaces overhead text
                actor.setOverheadText(newMessage);
                
                if(debugPrints)
                {
                    messageFound = true;
                    senderName = actor.getName();
                }
            }
        }

        if(debugPrints)
        {
            if(messageFound)
            {
                System.out.println("Message '" + oldMessage + "' was found and replaced for " + senderName);
            }
            else
            {
                System.out.println("Message '" + oldMessage + "' was not found!");
            }
        }
    }
    
    private void updateNearby()
    {
        int visionRadius = 30;
        int tileDistance = 128;

        LocalPoint playerPosition = clientReference.getLocalPlayer().getLocalLocation();
        playersNearby = clientReference.getPlayers()
            .stream()
            .filter(player -> (player.getLocalLocation().distanceTo(playerPosition) / tileDistance) <= visionRadius)
            .collect(Collectors.toList());
        npcsNearby = clientReference.getNpcs()
                .stream()
                .filter(player -> (player.getLocalLocation().distanceTo(playerPosition) / tileDistance) <= visionRadius)
                .collect(Collectors.toList());
    }
}
