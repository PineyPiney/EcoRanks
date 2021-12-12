package com.pineypiney.eco_ranks.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

// Event called whenever the players bal is changed

public class BalChangeEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();
    private final double oldBal;
    private final double newBal;

    public BalChangeEvent(Player who, double oldBal, double newBal) {
        super(who);
        this.oldBal = oldBal;
        this.newBal = newBal;
    }

    public double getOldBal() {
        return oldBal;
    }

    public double getNewBal() {
        return newBal;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    // This method MUST be here, and static, otherwise this
    // https://bukkit.org/threads/illegalpluginaccessexception-on-registering-events.141348/
    public static HandlerList getHandlerList(){
        return handlers;
    }
}
