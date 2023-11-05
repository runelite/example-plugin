package com.loadoutsaver.interfaces;

public interface ILoadout extends ISerializable<ILoadout> {

    String GetName();

    IInventory GetInventory();

    IEquipment GetEquipment();

}
