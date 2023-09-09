package com.loadoutsaver.interfaces;

public interface IInventory extends ISerializable<IInventory> {

    IItemStack[] GetItems();

}
