package com.loadoutsaver.implementations;

import com.loadoutsaver.interfaces.IInventory;
import com.loadoutsaver.interfaces.IItemStack;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

public class InventoryImpl implements IInventory {
    public static IInventory Deserializer = new InventoryImpl();

    private InventoryImpl() {

    }

    private InventoryImpl(IItemStack[] items) {
        this.items = items;
    }

    public InventoryImpl(ItemContainer inventory) {
        this(ParseInventory(inventory));
    }

    private static IItemStack[] ParseInventory(ItemContainer inventory) {
        if (inventory == null) {
            return new IItemStack[0];
        }

        // Should be 28 but hey, you never know.
        int inventorySize = inventory.size();

        IItemStack[] parsed = new IItemStack[inventorySize];
        for (int i = 0; i < inventorySize; i++) {
            Item item = inventory.getItem(i);
            if (item == null) {
                parsed[i] = new ItemStackImpl(-1, 0);
            }
            else {
                parsed[i] = new ItemStackImpl(item.getId(), item.getQuantity());
            }
        }

        return parsed;
    }

    private IItemStack[] items;

    @Override
    public IItemStack[] GetItems() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return items;
    }

    // Serialization format: colon-delimited item stacks, b64 encoded.

    @Override
    public String SerializeString() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return Arrays.stream(items).map(
                IItemStack::SerializeString
        ).map(
                String::getBytes
        ).map(
                Base64.getEncoder()::encodeToString
        ).collect(
                Collectors.joining(":")
        );
    }

    @Override
    public IInventory DeserializeString(String serialized) {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        String[] encodedItems = serialized.split(":");
        IItemStack[] items = Arrays.stream(encodedItems).map(
                Base64.getDecoder()::decode
        ).map(
                String::new
        ).map(
                ItemStackImpl.Deserializer::DeserializeString
        ).toArray(IItemStack[]::new);

        return new InventoryImpl(items);
    }
}
