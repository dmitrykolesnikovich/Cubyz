package io.cubyz.ndt;

import java.util.HashMap;

import io.cubyz.math.Bits;

public class NDTContainer extends NDTTag {

	HashMap<String, NDTTag> tags = new HashMap<>();
	
	public NDTContainer() {
		expectedLength = -1;
		type = NDTConstants.TYPE_CONTAINER;
	}
	
	public NDTContainer(byte[] bytes) {
		this();
		content = bytes;
		load();
		System.out.println("Loaded!");
	}
	
	byte[] sub(int s, int e) {
		byte[] arr = new byte[e - s];
		for (int i = 0; i < e - s; i++) {
			arr[i] = content[s + i];
		}
		return arr;
	}
	
	void load() {
		tags = new HashMap<>();
		short entries = Bits.getShort(content, 0);
		int addr = 2;
		for (int i = 0; i < entries; i++) {
			short len = Bits.getShort(content, addr);
			short size = Bits.getShort(content, addr+2);
			NDTString key = new NDTString();
			key.setBytes(sub(addr+4, addr+len+6));
			addr += len + 6;
			System.out.println("Load " + key.getValue());
			tags.put(key.getValue(), NDTTag.fromBytes(sub(addr, addr+size)));
			addr += size;
		}
	}
	
	void save() {
		int size = 2;
		for (String key : tags.keySet()) {
			size += 6;
			size += tags.get(key).getData().length+1;
			size += key.length() + 2;
		}
		content = new byte[size];
		
		Bits.putShort(content, 0, (short) tags.size());
		int addr = 2;
		for (String key : tags.keySet()) {
			NDTTag tag = tags.get(key);
			Bits.putShort(content, addr, (short) key.length());
			Bits.putShort(content, addr+2, (short) tag.getData().length);
			NDTString tagKey = new NDTString();
			tagKey.setValue(key);
			//System.out.println("Key length: " + tagKey.getLength());
			System.arraycopy(tagKey.getData(), 0, content, addr+4, tagKey.getData().length);
			addr += tagKey.getData().length + 4;
			
			byte[] tagContent = new byte[tag.getData().length + 1];
			tagContent[0] = tag.type;
			System.arraycopy(tag.getData(), 0, tagContent, 1, tag.getData().length);
			System.arraycopy(tagContent, 0, content, addr, tagContent.length);
			addr += tag.getData().length;
		}
	}
	
	public boolean hasKey(String key) {
		//load();
		return tags.containsKey(key);
	}
	
	public NDTTag getTag(String key) {
		if (!hasKey(key)) throw new IllegalArgumentException("No tag with key " + key);
		return tags.get(key);
	}
	
	public void setTag(String key, NDTTag tag) {
		tags.put(key, tag);
		save();
	}
	
	// Primitive types save/load
	public String getString(String key) {
		NDTString tag = (NDTString) getTag(key);
		return tag.getValue();
	}
	
	public void setString(String key, String str) {
		NDTString tag = new NDTString();
		tag.setValue(str);
		setTag(key, tag);
	}
	
	public int getInteger(String key) {
		NDTInteger tag = (NDTInteger) getTag(key);
		return tag.getValue();
	}
	
	public void setInteger(String key, int i) {
		NDTInteger tag = new NDTInteger();
		tag.setValue(i);
		setTag(key, tag);
	}
	
	public long getLong(String key) {
		NDTLong tag = (NDTLong) getTag(key);
		return tag.getValue();
	}
	
	public void setLong(String key, long i) {
		NDTLong tag = new NDTLong();
		tag.setValue(i);
		setTag(key, tag);
	}
	
	public NDTContainer getContainer(String key) {
		return (NDTContainer) getTag(key);
	}
	
	public void setContainer(String key, NDTContainer c) {
		setTag(key, c);
	}
	
	public boolean validate() {
		return true;
	}
	
}