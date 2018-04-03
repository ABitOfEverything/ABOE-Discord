package me.glorantq.aboe.richpresence.i18n;

import java.util.*;

public class ResourceBundleWrapper implements Map<String, Object> {
    private final ResourceBundle resourceBundle;

    public ResourceBundleWrapper(ResourceBundle bundle) {
        this.resourceBundle = bundle;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return resourceBundle.containsKey(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        return true;
    }

    @Override
    public Object get(Object key) {
        try {
            return resourceBundle.getObject(key.toString());
        } catch (MissingResourceException e) {
            return "Missing key from language file (" + resourceBundle.getLocale().toLanguageTag() + ")! Report this to a developer!";
        }
    }

    @Override
    public Object put(String key, Object value) {
        return value;
    }

    @Override
    public Object remove(Object key) {
        return key;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<String> keySet() {
        return resourceBundle.keySet();
    }

    @Override
    public Collection<Object> values() {
        return new ArrayList<>();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new HashSet<>();
    }
}
