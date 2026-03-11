/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.util;

import com.google.common.eventbus.EventBus;
import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.BasicPropertyDictionary;
import org.mapleir.propertyframework.impl.BasicSynchronisedPropertyDictionary;
import org.mapleir.propertyframework.impl.FixedStoreDictionary;

public class PropertyHelper {
    private static final EventBus PROPERTY_FRAMEWORK_BUS = new EventBus();
    public static final String BASIC_SYNCHRONISED_DICTIONARY_OPT = "dictionary.property.threadsafe";
    public static final String IMMUTABLE_DICTIONARY_OPT = "dictionary.property.immutable";
    private static final IPropertyDictionary EMPTY_DICTIONARY = new BasicPropertyDictionary(){

        @Override
        public void put(IProperty<?> property) {
            throw new UnsupportedOperationException("Immutable dictionary");
        }
    };

    public static EventBus getFrameworkBus() {
        return PROPERTY_FRAMEWORK_BUS;
    }

    public static IPropertyDictionary getImmutableDictionary() {
        return EMPTY_DICTIONARY;
    }

    public static IPropertyDictionary makeFixedStoreDictionary(IPropertyDictionary dict) {
        return new FixedStoreDictionary(dict);
    }

    public static IPropertyDictionary createDictionary() {
        return PropertyHelper.createDictionary(null);
    }

    private static boolean __has_opt(IPropertyDictionary settings, String key) {
        IProperty<Boolean> opt = settings.find(Boolean.TYPE, key);
        return opt != null && opt.getValue() != false;
    }

    public static IPropertyDictionary createDictionary(IPropertyDictionary settings) {
        if (settings != null) {
            if (PropertyHelper.__has_opt(settings, BASIC_SYNCHRONISED_DICTIONARY_OPT)) {
                return new BasicSynchronisedPropertyDictionary();
            }
            if (PropertyHelper.__has_opt(settings, IMMUTABLE_DICTIONARY_OPT)) {
                return EMPTY_DICTIONARY;
            }
        }
        return new BasicPropertyDictionary();
    }

    public static Class<?> rebasePrimitiveType(Class<?> t) {
        if (t == Boolean.TYPE) {
            return Boolean.class;
        }
        if (t == Integer.TYPE) {
            return Integer.class;
        }
        if (t == Short.TYPE) {
            return Short.class;
        }
        if (t == Byte.TYPE) {
            return Byte.class;
        }
        if (t == Long.TYPE) {
            return Long.class;
        }
        if (t == Float.TYPE) {
            return Float.class;
        }
        if (t == Double.TYPE) {
            return Double.class;
        }
        if (t == Character.TYPE) {
            return Character.class;
        }
        return null;
    }

    public static boolean isSet(IPropertyDictionary dict, String key) {
        return PropertyHelper.__has_opt(dict, key);
    }
}

