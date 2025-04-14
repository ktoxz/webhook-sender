package dev.ktoxz.model;

import org.bukkit.entity.Player;

public class PendingPayRequest {
    private final Player sender;
    private final double amount;

    public PendingPayRequest(Player sender, double amount) {
        this.sender = sender;
        this.amount = amount;
    }

    public Player getSender() {
        return sender;
    }

    public double getAmount() {
        return amount;
    }
}
