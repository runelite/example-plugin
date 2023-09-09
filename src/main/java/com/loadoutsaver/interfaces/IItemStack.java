package com.loadoutsaver.interfaces;

import java.util.Optional;

public interface IItemStack extends ISerializable<IItemStack> {

    int ItemID();

    Optional<Integer> Quantity();

}
