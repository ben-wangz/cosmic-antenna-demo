package com.example.distributed.consensus.utils;

import java.util.Optional;

public class ParameterUtil {

    public static <T> T  readEnvironmentVariable(String identifier, Class<T> clazz){
        String rawValue = Optional.ofNullable(System.getenv(identifier)).orElseThrow(
                () -> new IllegalArgumentException(
                        String.format("%s is not set in the environment", identifier))
        );
        return clazz.cast(rawValue);
    }
}

