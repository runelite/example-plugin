package com.loadoutsaver.interfaces;

import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.kit.KitType;

import java.util.Dictionary;
import java.util.Optional;

public interface IEquipment extends ISerializable<IEquipment> {

    Dictionary<EquipmentInventorySlot, IItemStack> GetEquipment();

}
