package com.pedrodalben.bigbangeventos.data;

import com.pedrodalben.bigbangeventos.platform.StoredLocation;
import java.time.Instant;
import java.util.*;

public final class DataCodecs {
    private DataCodecs() {}
    public static final DataCodec<String> STRING = simple("string", String.class, Object::toString);
    public static final DataCodec<Integer> INTEGER = simple("integer", Number.class, v -> ((Number)v).intValue());
    public static final DataCodec<Long> LONG = simple("long", Number.class, v -> ((Number)v).longValue());
    public static final DataCodec<Double> DOUBLE = simple("double", Number.class, v -> ((Number)v).doubleValue());
    public static final DataCodec<Boolean> BOOLEAN = simple("boolean", Boolean.class, v -> (Boolean)v);
    public static final DataCodec<UUID> UUID_CODEC = simple("uuid", String.class, v -> UUID.fromString(v.toString()));
    public static final DataCodec<Instant> INSTANT = simple("instant", String.class, v -> Instant.parse(v.toString()));
    public static final DataCodec<StoredLocation> STORED_LOCATION = new DataCodec<>() {
        public String type(){return "stored_location";}
        public Object encode(StoredLocation v){return Map.of("server",v.serverId(),"dimension",v.dimension(),"x",v.x(),"y",v.y(),"z",v.z(),"yaw",v.yaw(),"pitch",v.pitch());}
        public StoredLocation decode(Object raw){
            if (!(raw instanceof Map<?,?> m)) throw new IllegalArgumentException("localização inválida");
            return new StoredLocation(String.valueOf(m.get("server")),String.valueOf(m.get("dimension")),number(m,"x"),number(m,"y"),number(m,"z"), (float)number(m,"yaw"),(float)number(m,"pitch"));
        }
    };
    public static final DataCodec<List<String>> STRING_LIST = new DataCodec<>() {
        public String type(){return "list_string";} public Object encode(List<String> v){return List.copyOf(v);}
        public List<String> decode(Object raw){if(!(raw instanceof List<?> l))throw new IllegalArgumentException("lista inválida");return l.stream().map(Object::toString).toList();}
    };
    public static final DataCodec<Set<String>> STRING_SET = new DataCodec<>() {
        public String type(){return "set_string";} public Object encode(Set<String> v){return List.copyOf(v);}
        public Set<String> decode(Object raw){if(!(raw instanceof List<?> l))throw new IllegalArgumentException("conjunto inválido");return Set.copyOf(l.stream().map(Object::toString).toList());}
    };
    public static final DataCodec<Map<String,String>> STRING_MAP = new DataCodec<>() {
        public String type(){return "map_string";} public Object encode(Map<String,String> v){return Map.copyOf(v);}
        public Map<String,String> decode(Object raw){if(!(raw instanceof Map<?,?> m))throw new IllegalArgumentException("mapa inválido");var out=new LinkedHashMap<String,String>();m.forEach((k,v)->out.put(k.toString(),v.toString()));return Map.copyOf(out);}
    };
    private static double number(Map<?,?> m,String k){Object v=m.get(k);if(!(v instanceof Number n))throw new IllegalArgumentException("campo numérico ausente: "+k);return n.doubleValue();}
    private static <T> DataCodec<T> simple(String type, Class<?> expected, java.util.function.Function<Object,T> decode) { return new DataCodec<>() { public String type(){return type;} public Object encode(T v){return v;} public T decode(Object raw){if(!expected.isInstance(raw))throw new IllegalArgumentException("tipo inválido: "+type);return decode.apply(raw);} }; }
}
