package com.asiainfo.easymem.driver.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ConcurrentSizeLRUMap {
	private int bucketCount = 1;

	private int limitBytes = 0;

	private SizeLRUMap[] bucket = null;

	private long startTime = System.currentTimeMillis();

	public ConcurrentSizeLRUMap(int bucketCount, int limitBytes) {
		this.bucketCount = bucketCount;
		this.limitBytes = limitBytes;

		this.bucket = new SizeLRUMap[bucketCount];
		for (int i = 0; i < this.bucket.length; i++) {
			this.bucket[i] = new SizeLRUMap(100, limitBytes / bucketCount);
		}
	}

	private SizeLRUMap getBucket(int hashCode) {
		return this.bucket[(java.lang.Math.abs(hashCode) % this.bucketCount)];
	}

	public Object get(Object key) {
		return getBucket(key.hashCode()).get(key);
	}

	public boolean containsKey(Object key) {
		return getBucket(key.hashCode()).containsKey(key);
	}

	public Object put(Object key, Object value) {
		return getBucket(key.hashCode()).put(key, value);
	}

	public void clear() {
		for (int i = 0; i < this.bucket.length; i++) {
			this.bucket[i].clear();
		}

		this.startTime = System.currentTimeMillis();
	}

	public List getHotKeys(long num) {
		List list = new ArrayList();

		long perCount = num / this.bucket.length + 1L;
		long j;
		Iterator iter;
		for (int i = 0; i < this.bucket.length; i++) {
			j = 0L;
			Set keys = this.bucket[i].keySet();
			for (iter = keys.iterator(); iter.hasNext();) {
				Object item = iter.next();
				list.add(item);
				j += 1L;

				if (j >= perCount) {
					break;
				}
			}
		}
		return list;
	}

	public int size() {
		int size = 0;
		for (int i = 0; i < this.bucket.length; i++) {
			size += this.bucket[i].size();
		}
		return size;
	}

	public long getHit() {
		long hit = 0L;
		for (int i = 0; i < this.bucket.length; i++) {
			hit += this.bucket[i].getHit();
		}
		return hit;
	}

	public long getMiss() {
		long miss = 0L;
		for (int i = 0; i < this.bucket.length; i++) {
			miss += this.bucket[i].getMiss();
		}
		return miss;
	}

	public long getEvict() {
		long evict = 0L;
		for (int i = 0; i < this.bucket.length; i++) {
			evict += this.bucket[i].getEvict();
		}
		return evict;
	}

	public long getOverload() {
		long overload = 0L;
		for (int i = 0; i < this.bucket.length; i++) {
			overload += this.bucket[i].getOverload();
		}
		return overload;
	}

	public long getCurrentByteSize() {
		int byteSize = 0;
		for (int i = 0; i < this.bucket.length; i++) {
			byteSize = (int) (byteSize + this.bucket[i].getCurrentByteSize());
		}
		return byteSize;
	}

	public int getBucketCount() {
		return this.bucketCount;
	}

	public int getLimitBytes() {
		return this.limitBytes;
	}

	public long getStartTime() {
		return this.startTime;
	}
	
	public static void main(String[] args) {
		System.out.println(101%10);
	}
}