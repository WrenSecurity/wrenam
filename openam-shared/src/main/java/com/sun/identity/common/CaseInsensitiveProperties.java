/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CaseInsensitiveProperties.java,v 1.2 2008/06/25 05:42:25 qcheng Exp $
 *
 * Portions copyright 2022 Wren Security
 *
 */

package com.sun.identity.common;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

/**
 * A case-insensitive Properties with case preservation.
 */
public class CaseInsensitiveProperties extends Properties {

    static class CaseInsensitiveEnumeration implements Enumeration<Object> {

        Enumeration<?> mEnum;

        public CaseInsensitiveEnumeration(Enumeration<?> en) {
            mEnum = en;
        }

        public boolean hasMoreElements() {
            boolean ans = false;
            if (mEnum != null) {
                ans = mEnum.hasMoreElements();
            }
            return ans;
        }

        public Object nextElement() {
            Object ans = null;
            if (mEnum != null) {
                Object nextElem = mEnum.nextElement();
                if (nextElem instanceof CaseInsensitiveKey) {
                    ans = nextElem.toString();
                } else {
                    ans = nextElem;
                }
            }
            return ans;
        }
    }

    public CaseInsensitiveProperties() {
        super();
    }

    public CaseInsensitiveProperties(Properties defaults) {
        super(defaults);
    }

    public String getProperty(String key) {
        return (String) super.get(resolveOriginalKey(key));
    }

    public Object setProperty(String key, String value) {
        return super.put(resolveOriginalKey(key), value);
    }

    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return keySet().stream()
                    .map(Object::toString)
                    .anyMatch(originalKey -> originalKey.equalsIgnoreCase((String) key));
        } else {
            return super.containsKey(key);
        }
    }

    public Object get(Object key) {
        if (key instanceof String) {
            return super.get(resolveOriginalKey((String) key));
        } else {
            return super.get(key);
        }
    }

    /**
     * @return a case insensitive hash set of keys.
     */
    public Set<Object> keySet() {
        return new CaseInsensitiveHashSet<>(super.keySet());
    }

    /*
     * @return an Enumeration of keys as String objects even though they were
     * internally stored as case insensitive strings.
     */
    public Enumeration<Object> keys() {
        return new CaseInsensitiveEnumeration(super.keys());
    }

    /*
     * @return an Enumeration of property names as String objects even though
     * they were internally stored as case insensitive strings.
     */
    public Enumeration<Object> propertyNames() {
        return new CaseInsensitiveEnumeration(super.propertyNames());
    }

    public Object put(Object key, Object value) {
        if (key instanceof String) {
            return super.put(resolveOriginalKey((String) key), value);
        } else {
            return super.put(key, value);
        }
    }

    public Object remove(Object key) {
        if (key instanceof String) {
            return super.remove(resolveOriginalKey((String) key));
        } else {
            return super.remove(key);
        }
    }

    /**
     * Resolves already stored original key or the given key if none is found.
     * The search of key is case-insensitive.
     *
     * @return Stored key.
     */
    private String resolveOriginalKey(String key) {
        return keySet().stream()
                .map(Object::toString)
                .filter(originalKey -> originalKey.equalsIgnoreCase(key))
                .findAny().orElse(key);
    }

}
