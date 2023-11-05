package com.loadoutsaver.implementations;

import net.runelite.api.EquipmentInventorySlot;

import com.loadoutsaver.interfaces.IEquipment;
import com.loadoutsaver.interfaces.IItemStack;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EquipmentImpl implements IEquipment {

    public static IEquipment Deserializer = new EquipmentImpl();

    private EquipmentImpl() {

    }

    private EquipmentImpl(Map<EquipmentInventorySlot, IItemStack> equipment) {
        this.equipment = equipment;
    }

    public EquipmentImpl(ItemContainer equipment) {
        this(ParseEquipment(equipment));
    }

    private static Map<EquipmentInventorySlot, IItemStack> ParseEquipment(ItemContainer equipment) {
        Map<EquipmentInventorySlot, IItemStack> mapping = new HashMap<>();

        int equipmentSize = equipment.size();

        for (int i = 0; i < equipmentSize; i++) {
            Item item = equipment.getItem(i);
            EquipmentInventorySlot slot = ID_TO_SLOT.get(i);
            if (item != null) {
                IItemStack itemStack = new ItemStackImpl(item.getId(), item.getQuantity());
                mapping.put(slot, itemStack);
            }
        }

        return mapping;
    }

    private static final Map<Integer, EquipmentInventorySlot> ID_TO_SLOT = Arrays.stream(
            EquipmentInventorySlot.values()
    ).collect(
            Collectors.toMap(EquipmentInventorySlot::getSlotIdx, e -> e)
    );

    private Map<EquipmentInventorySlot, IItemStack> equipment;

    @Override
    public Map<EquipmentInventorySlot, IItemStack> GetEquipment() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return equipment;
    }

    // Serialization format: semicolon-delimited key-value pairs. keys are ints, values are b64-encoded item stacks.
    // keys and values are separated by a colon.
    // 1:{stack};2:{stack2}

    @Override
    public String SerializeString() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return equipment.entrySet().stream().map(
                entry -> (
                        entry.getKey().getSlotIdx()
                                + ":"
                                + Base64.getEncoder().encodeToString(entry.getValue().SerializeString().getBytes())
                )
        ).collect(Collectors.joining(";"));
    }

    @Override
    public IEquipment DeserializeString(String serialized) {
        String[] items = serialized.split(";");
        Map<EquipmentInventorySlot, IItemStack> itemMap = new HashMap<>();

        for (String item : items) {
            String[] kvp = item.split(":");
            if (kvp.length != 2) {
                throw new IllegalArgumentException("Corrupted equipment: " + serialized);
            }
            EquipmentInventorySlot key = ID_TO_SLOT.get(Integer.parseInt(kvp[0]));
            String value = kvp[1];
            IItemStack deserializedValue = ItemStackImpl.Deserializer.DeserializeString(value);
            itemMap.put(key, deserializedValue);
        }

        return new EquipmentImpl(itemMap);
    }
}
