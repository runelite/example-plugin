package com.loadoutsaver.implementations;

import com.loadoutsaver.interfaces.IItemStack;

public class ItemStackImpl implements IItemStack {
    public static IItemStack Deserializer = new ItemStackImpl();

    private ItemStackImpl() {

    }

    private int itemID;
    private int quantity;

    public ItemStackImpl(int itemID, int quantity) {
        this.itemID = itemID;
        this.quantity = quantity;
    }

    @Override
    public int ItemID() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return itemID;
    }

    @Override
    public int Quantity() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return quantity;
    }

    @Override
    public String SerializeString() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return itemID + ":" + quantity;
    }

    @Override
    public IItemStack DeserializeString(String serialized) {
        String[] split = serialized.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("Corrupted item stack: " + serialized);
        }
        return new ItemStackImpl(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }
}
