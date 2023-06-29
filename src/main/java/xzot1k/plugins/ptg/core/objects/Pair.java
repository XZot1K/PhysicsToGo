package xzot1k.plugins.ptg.core.objects;

public class Pair<K, V> {

    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {this.key = key;}

    public V getValue() {
        return value;
    }

    public void setValue(V value) {this.value = value;}

    public void update(K key, V value) {
        setKey(key);
        setValue(value);
    }

}