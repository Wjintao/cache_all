package com.asiainfo.easymem.driver.map;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.collections.BoundedMap;

public class SizeLRUMap extends AbstractLinkedMap implements BoundedMap, Serializable, Cloneable {
	static final long serialVersionUID = -612114643488955218L;
	private int maxByteSize;
	private int overloadByteSize;
	private AtomicLong CURRENT_BYTE_SIZE = new AtomicLong(0);

	private AtomicLong MISS_COUNT = new AtomicLong(0);

	private AtomicLong HIT_COUNT = new AtomicLong(0);

	private AtomicLong EVICT_COUNT = new AtomicLong(0);

	private AtomicLong OVERLOAD_COUNT = new AtomicLong(0);

	private Object mutex = null;

	public SizeLRUMap(int initialCapacity, int maxByteSize) {
		this(initialCapacity, 0.75F, maxByteSize);
	}

	public SizeLRUMap(int initialCapacity, float loadFactor, int maxByteSize) {
		super(initialCapacity < 1 ? 16 : initialCapacity, loadFactor);
		if (initialCapacity < 1) {
			throw new IllegalArgumentException("map max size must be greater than 0");
		}

		this.maxByteSize = maxByteSize;
		this.overloadByteSize = (int) (maxByteSize * 1.2D);
		this.mutex = this;
	}

	public Object get(Object key) {
		synchronized (this.mutex) {
			AbstractLinkedMap.LinkEntry entry = (AbstractLinkedMap.LinkEntry) getEntry(key);
			if (entry == null) {
				this.MISS_COUNT.incrementAndGet();
				return null;
			}

			this.HIT_COUNT.incrementAndGet();
			moveToMRU(entry);
			return entry.getValue();
		}
	}

	public boolean containsKey(Object key) {
		boolean rtn = false;

		synchronized (this.mutex) {
			rtn = super.containsKey(key);
		}

		if (!rtn) {
			this.MISS_COUNT.incrementAndGet();
		}

		return rtn;
	}

	public Object put(Object key, Object value) {
		if (!(value instanceof SizeObject)) {
			throw new RuntimeException("value " + value.getClass() + " must be instanceof SizeObject");
		}

		synchronized (this.mutex) {
			return super.put(key, value);
		}
	}

	public void clear() {
		synchronized (this.mutex) {
			super.clear();
		}

		this.CURRENT_BYTE_SIZE.getAndSet(0);
		this.MISS_COUNT.getAndSet(0);
		this.HIT_COUNT.getAndSet(0);
		this.EVICT_COUNT.getAndSet(0);
		this.OVERLOAD_COUNT.getAndSet(0);
	}

	public boolean isEmpty() {
		synchronized (this.mutex) {
			return super.isEmpty();
		}
	}

	public int size() {
		synchronized (this.mutex) {
			return super.size();
		}
	}

	protected void moveToMRU(AbstractLinkedMap.LinkEntry entry) {
		if (entry.after != this.header) {
			this.modCount += 1;

			entry.before.after = entry.after;
			entry.after.before = entry.before;

			entry.after = this.header;
			entry.before = this.header.before;
			this.header.before.after = entry;
			this.header.before = entry;
		}
	}

	protected void updateEntry(AbstractHashedMap.HashEntry entry,
			Object newValue) {
		moveToMRU((AbstractLinkedMap.LinkEntry) entry);
		entry.setValue(newValue);
	}

	protected void addMapping(int hashIndex, int hashCode, Object key, Object value) {
		if (!(value instanceof SizeObject)) {
			throw new RuntimeException("value " + value.getClass() + " must be instanceof SizeObject");
		}

		int size = ((SizeObject) value).getSize();

		long curByteSize = this.CURRENT_BYTE_SIZE.addAndGet(size);

		if (curByteSize >= this.overloadByteSize) {
			boolean isOverLoad = true;
			if ((this.header != null) && (this.header.after != null)) {
				SizeObject tail = (SizeObject) this.header.after.value;
				if ((tail != null) && (tail.getSize() >= size)) {
					isOverLoad = false;
				}
			}

			if (isOverLoad) {
				this.CURRENT_BYTE_SIZE.addAndGet(-size);

				this.OVERLOAD_COUNT.incrementAndGet();
				return;
			}
		}

		if ((curByteSize >= this.maxByteSize) && (removeLRU(this.header.before))) {
			reuseMapping(this.header.after, hashIndex, hashCode, key, value);
			this.EVICT_COUNT.incrementAndGet();
		} else {
			super.addMapping(hashIndex, hashCode, key, value);
		}
	}

	protected void reuseMapping(AbstractLinkedMap.LinkEntry entry, int hashIndex, int hashCode, Object key, Object value) {
		int removeIndex = hashIndex(entry.hashCode, this.data.length);
		AbstractHashedMap.HashEntry loop = this.data[removeIndex];
		AbstractHashedMap.HashEntry previous = null;
		while (loop != entry) {
			previous = loop;
			loop = loop.next;
		}

		this.modCount += 1;

		if (entry != null) {
			this.CURRENT_BYTE_SIZE.addAndGet(-((SizeObject) entry.value).getSize());
		}

		removeEntry(entry, removeIndex, previous);
		reuseEntry(entry, hashIndex, hashCode, key, value);
		addEntry(entry, hashIndex);
	}

	protected boolean removeLRU(AbstractLinkedMap.LinkEntry entry) {
		return true;
	}

	public boolean isFull() {
		return this.CURRENT_BYTE_SIZE.get() >= this.maxByteSize;
	}

	public int maxSize() {
		return this.maxByteSize;
	}

	public Object clone() {
		return super.clone();
	}

	public long getHit() {
		return this.HIT_COUNT.get();
	}

	public long getMiss() {
		return this.MISS_COUNT.get();
	}

	public long getEvict() {
		return this.EVICT_COUNT.get();
	}

	public long getOverload() {
		return this.OVERLOAD_COUNT.get();
	}

	public long getCurrentByteSize() {
		return this.CURRENT_BYTE_SIZE.get();
	}
}