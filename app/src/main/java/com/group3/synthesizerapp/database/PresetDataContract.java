package com.group3.synthesizerapp.database;

import android.provider.BaseColumns;


public class PresetDataContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private PresetDataContract() {}

    /* Inner class that defines the table contents */
    public static class PresetEntry implements BaseColumns {
        public static final String PRESET_NAME = "preset_name";
        public static final String PRESET_VALUE = "preset_value";
        public static final String TABLE_NAME = "preset_table";
    }
}
