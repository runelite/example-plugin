package com.loadoutsaver.interfaces;

public interface ILoadout extends ISerializable<ILoadout> {

    IInventory GetInventory();

    IEquipment GetEquipment();

}
