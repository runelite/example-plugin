package com.loadoutsaver.implementations;

import com.loadoutsaver.interfaces.IEquipment;
import com.loadoutsaver.interfaces.IInventory;
import com.loadoutsaver.interfaces.ILoadout;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;

import java.util.Base64;

public class LoadoutImpl implements ILoadout {

    public static ILoadout Deserializer = new LoadoutImpl();

    private LoadoutImpl() {

    }

    public LoadoutImpl(String name, Client client) {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);

        if (inventory == null || equipment == null) {
            // Bad client state.
            throw new IllegalArgumentException("Client state was unexpected in loadout parser.");
        }

        this.inventory = new InventoryImpl(inventory);
        this.equipment = new EquipmentImpl(equipment);
        this.name = name;
    }

    public LoadoutImpl(Client client) {
        this("Unnamed Loadout", client);
    }

    private LoadoutImpl(String name, IInventory inventory, IEquipment equipment) {
        if (name.contains("{") || name.contains("}") || name.contains(":") || name.contains(";")) {
            // Some high-level sanitization. The ":" is the one that will break the (de)-serialization here.
            throw new IllegalArgumentException("Name contained illegal characters: " + name);
        }
        this.name = name;
        this.inventory = inventory;
        this.equipment = equipment;
    }

    private String name;
    private IInventory inventory;
    private IEquipment equipment;

    @Override
    public String GetName() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return null;
    }

    @Override
    public IInventory GetInventory() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return null;
    }

    @Override
    public IEquipment GetEquipment() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return null;
    }

    // Serialization format:
    // {name}:{b64 inventory}:{b64 equipment}

    @Override
    public String SerializeString() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        String encodedInventory = Base64.getEncoder().encodeToString(inventory.SerializeString().getBytes());
        String encodedEquipment = Base64.getEncoder().encodeToString(equipment.SerializeString().getBytes());

        return this.name + ":" + encodedInventory + ":" + encodedEquipment;
    }

    @Override
    public ILoadout DeserializeString(String serialized) {
        String[] components = serialized.strip().split(":");
        if (components.length != 3) {
            // Violation of format.
            throw new IllegalArgumentException("Corrupted loadout: " + serialized);
        }
        String loadoutName = components[0];
        String decodedInventory = new String(Base64.getDecoder().decode(components[1]));
        String decodedEquipment = new String(Base64.getDecoder().decode(components[2]));

        IInventory inventory = InventoryImpl.Deserializer.DeserializeString(decodedInventory);
        IEquipment equipment = EquipmentImpl.Deserializer.DeserializeString(decodedEquipment);

        return new LoadoutImpl(loadoutName, inventory, equipment);
    }
}
