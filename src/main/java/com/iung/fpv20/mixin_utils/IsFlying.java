package com.iung.fpv20.mixin_utils;

public interface IsFlying {
    default boolean get_is_flying(){
        return false;
    }
    default void set_is_flying(boolean v){

    }

    default int get_frame_index() {
        return 2; // 5" by default
    }
    default void set_frame_index(int index) {}

    Object get_obj();
    void set_obj(Object obj);
}
