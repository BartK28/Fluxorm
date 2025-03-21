package nl.seyox.structure;

import lombok.Getter;

import java.util.HashMap;

public class DataTypeMap {

    @Getter
    private static HashMap<String, String> map = new HashMap<>();

    static {
        map.put("int", "int");
        map.put("long", "bigint");
        map.put("float", "decimal");
        map.put("double", "decimal");
        map.put("boolean", "tinyint");
        map.put("String", "text");
    }

}
