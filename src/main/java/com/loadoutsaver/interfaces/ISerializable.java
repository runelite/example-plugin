package com.loadoutsaver.interfaces;

import java.util.Dictionary;
import java.util.List;

public interface ISerializable<T> {
    String SerializeString();

    T DeserializeString(String serialized);
}
