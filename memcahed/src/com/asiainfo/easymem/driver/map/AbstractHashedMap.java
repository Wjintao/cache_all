package com.asiainfo.easymem.driver.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections.IterableMap;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.KeyValue;
import org.apache.commons.collections.MapIterator;

public class AbstractHashedMap implements IterableMap {
	protected static final String NO_NEXT_ENTRY = "No next() entry in the iteration";
	protected static final String NO_PREVIOUS_ENTRY = "No previous() entry in the iteration";
	protected static final String REMOVE_INVALID = "remove() can only be called once after next()";
	protected static final String GETKEY_INVALID = "getKey() can only be called after next() and before remove()";
	protected static final String GETVALUE_INVALID = "getValue() can only be called after next() and before remove()";
	protected static final String SETVALUE_INVALID = "setValue() can only be called after next() and before remove()";
	protected static final int DEFAULT_CAPACITY = 16;
	protected static final int DEFAULT_THRESHOLD = 12;
	protected static final float DEFAULT_LOAD_FACTOR = 0.75F;
	protected static final int MAXIMUM_CAPACITY = 1073741824;
	protected static final Object NULL = new Object();
	protected transient float loadFactor;
	protected transient int size;
	protected transient HashEntry[] data;
	protected transient int threshold;
	protected transient int modCount;
	protected transient EntrySet entrySet;
	protected transient KeySet keySet;
	protected transient Values values;

	protected AbstractHashedMap() {
	}

	protected AbstractHashedMap(int initialCapacity, float loadFactor, int threshold) {
		this.loadFactor = loadFactor;
		this.data = new HashEntry[initialCapacity];
		this.threshold = threshold;
		init();
	}

	protected AbstractHashedMap(int initialCapacity) {
		this(initialCapacity, 0.75F);
	}

	protected AbstractHashedMap(int initialCapacity, float loadFactor) {
		if (initialCapacity < 1) {
			throw new IllegalArgumentException("Initial capacity must be greater than 0");
		}
		if ((loadFactor <= 0.0F) || (Float.isNaN(loadFactor))) {
			throw new IllegalArgumentException("Load factor must be greater than 0");
		}
		this.loadFactor = loadFactor;
		this.threshold = calculateThreshold(initialCapacity, loadFactor);
		initialCapacity = calculateNewCapacity(initialCapacity);
		this.data = new HashEntry[initialCapacity];
		init();
	}

	protected AbstractHashedMap(Map map) {
		this(Math.max(2 * map.size(), 16), 0.75F);
		putAll(map);
	}

	protected void init() {
	}

	public Object get(Object key) {
		key = convertKey(key);
		int hashCode = hash(key);
		HashEntry entry = this.data[hashIndex(hashCode, this.data.length)];
		while (entry != null) {
			if ((entry.hashCode == hashCode) && (isEqualKey(key, entry.key))) {
				return entry.getValue();
			}
			entry = entry.next;
		}
		return null;
	}

	public int size() {
		return this.size;
	}

	public boolean isEmpty() {
		return this.size == 0;
	}

	public boolean containsKey(Object key) {
		key = convertKey(key);
		int hashCode = hash(key);
		HashEntry entry = this.data[hashIndex(hashCode, this.data.length)];
		while (entry != null) {
			if ((entry.hashCode == hashCode) && (isEqualKey(key, entry.key))) {
				return true;
			}
			entry = entry.next;
		}
		return false;
	}

	public boolean containsValue(Object value) {
		if (value == null) {
			int i = 0;
			for (int isize = this.data.length; i < isize; i++) {
				HashEntry entry = this.data[i];
				while (entry != null) {
					if (entry.getValue() == null) {
						return true;
					}
					entry = entry.next;
				}
			}
		} else {
			int i = 0;
			for (int isize = this.data.length; i < isize; i++) {
				HashEntry entry = this.data[i];
				while (entry != null) {
					if (isEqualValue(value, entry.getValue())) {
						return true;
					}
					entry = entry.next;
				}
			}
		}
		return false;
	}

	public Object put(Object key, Object value) {
		key = convertKey(key);
		int hashCode = hash(key);
		int index = hashIndex(hashCode, this.data.length);
		HashEntry entry = this.data[index];
		while (entry != null) {
			if ((entry.hashCode == hashCode) && (isEqualKey(key, entry.key))) {
				Object oldValue = entry.getValue();
				updateEntry(entry, value);
				return oldValue;
			}
			entry = entry.next;
		}

		addMapping(index, hashCode, key, value);
		return null;
	}

	public void putAll(Map map) {
		int mapSize = map.size();
		if (mapSize == 0) {
			return;
		}
		ensureCapacity(calculateNewCapacity(this.size + mapSize));
		for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	public Object remove(Object key) {
		key = convertKey(key);
		int hashCode = hash(key);
		int index = hashIndex(hashCode, this.data.length);
		HashEntry entry = this.data[index];
		HashEntry previous = null;
		while (entry != null) {
			if ((entry.hashCode == hashCode) && (isEqualKey(key, entry.key))) {
				Object oldValue = entry.getValue();
				removeMapping(entry, index, previous);
				return oldValue;
			}
			previous = entry;
			entry = entry.next;
		}
		return null;
	}

	public void clear() {
		this.modCount += 1;
		HashEntry[] data = this.data;
		for (int i = data.length - 1; i >= 0; i--) {
			data[i] = null;
		}
		this.size = 0;
	}

	protected Object convertKey(Object key) {
		return key == null ? NULL : key;
	}

	protected int hash(Object key) {
		int h = key.hashCode();
		h += (h << 9 ^ 0xFFFFFFFF);
		h ^= h >>> 14;
		h += (h << 4);
		h ^= h >>> 10;
		return h;
	}

	protected boolean isEqualKey(Object key1, Object key2) {
		return (key1 == key2) || (key1.equals(key2));
	}

	protected boolean isEqualValue(Object value1, Object value2) {
		return (value1 == value2) || (value1.equals(value2));
	}

	protected int hashIndex(int hashCode, int dataSize) {
		return hashCode & dataSize - 1;
	}

	protected HashEntry getEntry(Object key) {
		key = convertKey(key);
		int hashCode = hash(key);
		HashEntry entry = this.data[hashIndex(hashCode, this.data.length)];
		while (entry != null) {
			if ((entry.hashCode == hashCode) && (isEqualKey(key, entry.key))) {
				return entry;
			}
			entry = entry.next;
		}
		return null;
	}

	protected void updateEntry(HashEntry entry, Object newValue) {
		entry.setValue(newValue);
	}

	protected void reuseEntry(HashEntry entry, int hashIndex, int hashCode, Object key, Object value) {
		entry.next = this.data[hashIndex];
		entry.hashCode = hashCode;
		entry.key = key;
		entry.value = value;
	}

	protected void addMapping(int hashIndex, int hashCode, Object key, Object value) {
		this.modCount += 1;
		HashEntry entry = createEntry(this.data[hashIndex], hashCode, key, value);
		addEntry(entry, hashIndex);
		this.size += 1;
		checkCapacity();
	}

	protected HashEntry createEntry(HashEntry next, int hashCode, Object key, Object value) {
		return new HashEntry(next, hashCode, key, value);
	}

	protected void addEntry(HashEntry entry, int hashIndex) {
		this.data[hashIndex] = entry;
	}

	protected void removeMapping(HashEntry entry, int hashIndex, HashEntry previous) {
		this.modCount += 1;
		removeEntry(entry, hashIndex, previous);
		this.size -= 1;
		destroyEntry(entry);
	}

	protected void removeEntry(HashEntry entry, int hashIndex, HashEntry previous) {
		if (previous == null)
			this.data[hashIndex] = entry.next;
		else
			previous.next = entry.next;
	}

	protected void destroyEntry(HashEntry entry) {
		entry.next = null;
		entry.key = null;
		entry.value = null;
	}

	protected void checkCapacity() {
		if (this.size >= this.threshold)
			ensureCapacity(this.data.length * 2);
	}

	protected void ensureCapacity(int newCapacity) {
		int oldCapacity = this.data.length;
		if (newCapacity <= oldCapacity) {
			return;
		}
		HashEntry[] oldEntries = this.data;
		HashEntry[] newEntries = new HashEntry[newCapacity];

		this.modCount += 1;
		for (int i = oldCapacity - 1; i >= 0; i--) {
			HashEntry entry = oldEntries[i];
			if (entry != null) {
				oldEntries[i] = null;
				do {
					HashEntry next = entry.next;
					int index = hashIndex(entry.hashCode, newCapacity);
					entry.next = newEntries[index];
					newEntries[index] = entry;
					entry = next;
				} while (entry != null);
			}
		}
		this.threshold = calculateThreshold(newCapacity, this.loadFactor);
		this.data = newEntries;
	}

	protected int calculateNewCapacity(int proposedCapacity) {
		int newCapacity = 1;
		if (proposedCapacity > 1073741824) {
			newCapacity = 1073741824;
		} else {
			while (newCapacity < proposedCapacity) {
				newCapacity <<= 1;
			}
			if (proposedCapacity > 1073741824) {
				newCapacity = 1073741824;
			}
		}
		return newCapacity;
	}

	protected int calculateThreshold(int newCapacity, float factor) {
		return (int) (newCapacity * factor);
	}

	public MapIterator mapIterator() {
		if (this.size == 0) {
			return IteratorUtils.EMPTY_MAP_ITERATOR;
		}
		return new HashMapIterator(this);
	}

	public Set entrySet() {
		if (this.entrySet == null) {
			this.entrySet = new EntrySet(this);
		}
		return this.entrySet;
	}

	protected Iterator createEntrySetIterator() {
		if (size() == 0) {
			return IteratorUtils.EMPTY_ITERATOR;
		}
		return new EntrySetIterator(this);
	}

	public Set keySet() {
		if (this.keySet == null) {
			this.keySet = new KeySet(this);
		}
		return this.keySet;
	}

	protected Iterator createKeySetIterator() {
		if (size() == 0) {
			return IteratorUtils.EMPTY_ITERATOR;
		}
		return new KeySetIterator(this);
	}

	public Collection values() {
		if (this.values == null) {
			this.values = new Values(this);
		}
		return this.values;
	}

	protected Iterator createValuesIterator() {
		if (size() == 0) {
			return IteratorUtils.EMPTY_ITERATOR;
		}
		return new ValuesIterator(this);
	}

	protected void doWriteObject(ObjectOutputStream out) throws IOException {
		out.writeFloat(this.loadFactor);
		out.writeInt(this.data.length);
		out.writeInt(this.size);
		for (MapIterator it = mapIterator(); it.hasNext();) {
			out.writeObject(it.next());
			out.writeObject(it.getValue());
		}
	}

	protected void doReadObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.loadFactor = in.readFloat();
		int capacity = in.readInt();
		int size = in.readInt();
		init();
		this.data = new HashEntry[capacity];
		for (int i = 0; i < size; i++) {
			Object key = in.readObject();
			Object value = in.readObject();
			put(key, value);
		}
		this.threshold = calculateThreshold(this.data.length, this.loadFactor);
	}

	protected Object clone() {
		try {
			AbstractHashedMap cloned = (AbstractHashedMap) super.clone();
			cloned.data = new HashEntry[this.data.length];
			cloned.entrySet = null;
			cloned.keySet = null;
			cloned.values = null;
			cloned.modCount = 0;
			cloned.size = 0;
			init();
			cloned.putAll(this);
			return cloned;
		} catch (CloneNotSupportedException ex) {
		}
		return null;
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Map)) {
			return false;
		}
		Map map = (Map) obj;
		if (map.size() != size()) {
			return false;
		}
		MapIterator it = mapIterator();
		try {
			while (it.hasNext()) {
				Object key = it.next();
				Object value = it.getValue();
				if (value == null) {
					if ((map.get(key) != null) || (!map.containsKey(key))) {
						return false;
					}
				} else if (!value.equals(map.get(key)))
					return false;
			}
		} catch (ClassCastException ignored) {
			return false;
		} catch (NullPointerException ignored) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int total = 0;
		Iterator it = createEntrySetIterator();
		while (it.hasNext()) {
			total += it.next().hashCode();
		}
		return total;
	}

	public String toString() {
		if (size() == 0) {
			return "{}";
		}
		StringBuffer buf = new StringBuffer(32 * size());
		buf.append('{');

		MapIterator it = mapIterator();
		boolean hasNext = it.hasNext();
		while (hasNext) {
			Object key = it.next();
			Object value = it.getValue();
			buf.append(key == this ? "(this Map)" : key).append('=').append(value == this ? "(this Map)" : value);

			hasNext = it.hasNext();
			if (hasNext) {
				buf.append(',').append(' ');
			}
		}

		buf.append('}');
		return buf.toString();
	}

	protected static abstract class HashIterator implements Iterator {
		protected final AbstractHashedMap parent;
		protected int hashIndex;
		protected AbstractHashedMap.HashEntry last;
		protected AbstractHashedMap.HashEntry next;
		protected int expectedModCount;

		protected HashIterator(AbstractHashedMap parent) {
			this.parent = parent;
			AbstractHashedMap.HashEntry[] data = parent.data;
			int i = data.length;
			AbstractHashedMap.HashEntry next = null;
			while ((i > 0) && (next == null)) {
				i--;
				next = data[i];
			}
			this.next = next;
			this.hashIndex = i;
			this.expectedModCount = parent.modCount;
		}

		public boolean hasNext() {
			return this.next != null;
		}

		protected AbstractHashedMap.HashEntry nextEntry() {
			if (this.parent.modCount != this.expectedModCount) {
				throw new ConcurrentModificationException();
			}
			AbstractHashedMap.HashEntry newCurrent = this.next;
			if (newCurrent == null) {
				throw new NoSuchElementException("No next() entry in the iteration");
			}
			AbstractHashedMap.HashEntry[] data = this.parent.data;
			int i = this.hashIndex;
			AbstractHashedMap.HashEntry n = newCurrent.next;
			while ((n == null) && (i > 0)) {
				i--;
				n = data[i];
			}
			this.next = n;
			this.hashIndex = i;
			this.last = newCurrent;
			return newCurrent;
		}

		protected AbstractHashedMap.HashEntry currentEntry() {
			return this.last;
		}

		public void remove() {
			if (this.last == null) {
				throw new IllegalStateException("remove() can only be called once after next()");
			}
			if (this.parent.modCount != this.expectedModCount) {
				throw new ConcurrentModificationException();
			}
			this.parent.remove(this.last.getKey());
			this.last = null;
			this.expectedModCount = this.parent.modCount;
		}

		public String toString() {
			if (this.last != null) {
				return "Iterator[" + this.last.getKey() + "=" + this.last.getValue() + "]";
			}
			return "Iterator[]";
		}
	}

	protected static class HashEntry implements Map.Entry, KeyValue {
		protected HashEntry next;
		protected int hashCode;
		protected Object key;
		protected Object value;

		protected HashEntry(HashEntry next, int hashCode, Object key, Object value) {
			this.next = next;
			this.hashCode = hashCode;
			this.key = key;
			this.value = value;
		}

		public Object getKey() {
			return this.key == AbstractHashedMap.NULL ? null : this.key;
		}

		public Object getValue() {
			return this.value;
		}

		public Object setValue(Object value) {
			Object old = this.value;
			this.value = value;
			return old;
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof KeyValue)) {
				return false;
			}
			KeyValue other = (KeyValue) obj;
			return (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey()))
					&& (getValue() == null ? other.getValue() == null : getValue().equals(other.getValue()));
		}

		public int hashCode() {
			return (getKey() == null ? 0 : getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode());
		}

		public String toString() {
			return getKey().toString() + '=' + getValue();
		}
	}

	protected static class ValuesIterator extends AbstractHashedMap.HashIterator {
		protected ValuesIterator(AbstractHashedMap parent) {
			super(parent);
		}

		public Object next() {
			return super.nextEntry().getValue();
		}
	}

	protected static class Values extends AbstractCollection {
		protected final AbstractHashedMap parent;

		protected Values(AbstractHashedMap parent) {
			this.parent = parent;
		}

		public int size() {
			return this.parent.size();
		}

		public void clear() {
			this.parent.clear();
		}

		public boolean contains(Object value) {
			return this.parent.containsValue(value);
		}

		public Iterator iterator() {
			return this.parent.createValuesIterator();
		}
	}

	protected static class KeySetIterator extends AbstractHashedMap.EntrySetIterator {
		protected KeySetIterator(AbstractHashedMap parent) {
			super(parent);
		}

		public Object next() {
			return super.nextEntry().getKey();
		}
	}

	protected static class KeySet extends AbstractSet {
		protected final AbstractHashedMap parent;

		protected KeySet(AbstractHashedMap parent) {
			this.parent = parent;
		}

		public int size() {
			return this.parent.size();
		}

		public void clear() {
			this.parent.clear();
		}

		public boolean contains(Object key) {
			return this.parent.containsKey(key);
		}

		public boolean remove(Object key) {
			boolean result = this.parent.containsKey(key);
			this.parent.remove(key);
			return result;
		}

		public Iterator iterator() {
			return this.parent.createKeySetIterator();
		}
	}

	protected static class EntrySetIterator extends AbstractHashedMap.HashIterator {
		protected EntrySetIterator(AbstractHashedMap parent) {
			super(parent);
		}

		public Object next() {
			return super.nextEntry();
		}
	}

	protected static class EntrySet extends AbstractSet {
		protected final AbstractHashedMap parent;

		protected EntrySet(AbstractHashedMap parent) {
			this.parent = parent;
		}

		public int size() {
			return this.parent.size();
		}

		public void clear() {
			this.parent.clear();
		}

		public boolean contains(Object entry) {
			if ((entry instanceof Map.Entry)) {
				return this.parent.containsKey(((Map.Entry) entry).getKey());
			}
			return false;
		}

		public boolean remove(Object obj) {
			if (!(obj instanceof Map.Entry)) {
				return false;
			}
			Map.Entry entry = (Map.Entry) obj;
			Object key = entry.getKey();
			boolean result = this.parent.containsKey(key);
			this.parent.remove(key);
			return result;
		}

		public Iterator iterator() {
			return this.parent.createEntrySetIterator();
		}
	}

	protected static class HashMapIterator extends AbstractHashedMap.HashIterator implements MapIterator {
		protected HashMapIterator(AbstractHashedMap parent) {
			super(parent);
		}

		public Object next() {
			return super.nextEntry().getKey();
		}

		public Object getKey() {
			AbstractHashedMap.HashEntry current = currentEntry();
			if (current == null) {
				throw new IllegalStateException("getKey() can only be called after next() and before remove()");
			}
			return current.getKey();
		}

		public Object getValue() {
			AbstractHashedMap.HashEntry current = currentEntry();
			if (current == null) {
				throw new IllegalStateException("getValue() can only be called after next() and before remove()");
			}
			return current.getValue();
		}

		public Object setValue(Object value) {
			AbstractHashedMap.HashEntry current = currentEntry();
			if (current == null) {
				throw new IllegalStateException("setValue() can only be called after next() and before remove()");
			}
			return current.setValue(value);
		}
	}
}