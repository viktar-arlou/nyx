package nyx.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class NyxMapTest {

	@Test
	public void testNyxMap() {
		new NyxMap<String, String>();
	}

	@Test
	public void testSize() {
		Map<String, String> map = new NyxMap<>();
		assertTrue(map.size()==0);
		map.put("test", "test");
		assertTrue(map.size()==1);
	}

	@Test
	public void testIsEmpty() {
		Map<String, String> map = new NyxMap<>();
		assertTrue(map.isEmpty());
		map.put("test", "test");
		assertFalse(map.isEmpty());
	}

	@Test
	public void testContainsKey() {
		Map<String, String> map = new NyxMap<>();
		map.put("test", "test");
		assertTrue(map.containsKey("test"));
	}

	@Test
	public void testNullValue() {
		Map<String, String> map = new NyxMap<>();
		map.put("test", null);
		assertTrue(map.containsKey("test"));
		assertTrue(map.get("test")==null);
	}
	
	@Test
	public void testContainsValue() {
		Map<String, String> map = new NyxMap<>();
		map.put("test", "test");
		map.put("test1", "test1");
		map.put("test2", "test2");
		map.put("test3", "test3");
		assertTrue(map.containsValue("test2"));
	}

	@Test
	public void testPutGet() {
		String[] astr=new String[]{"test","test1","test2","test3"};
		Map<String, String> map = new NyxMap<>();
		for (String str : astr) 
			map.put(str,str);
		for (String str : astr) 
			assertTrue(map.get(str).equals(str));
	}

	@Test
	public void testRemove() {
		String[] astr=new String[]{"test","test1","test2","test3"};
		Map<String, String> map = new NyxMap<>();
		for (String str : astr) 
			map.put(str,str);
		assertEquals(map.remove("test1"), "test1");
		assertTrue(map.size() == 3);
		
		
	}

	@Test
	public void testPutAll() {
		String[] astr=new String[]{"test","test1","test2","test3"};
		Map<String, String> map = new HashMap<>();
		for (String str : astr) 
			map.put(str,str);
		Map<String,String> map2 = new NyxMap<>();
		map2.putAll(map);
		for (String str : astr)
			assertEquals(map.get(str), map2.get(str));
	}

	@Test
	public void testClear() {
		String[] astr=new String[]{"test","test1","test2","test3"};
		Map<String, String> map = new NyxMap<>();
		for (String str : astr) 
			map.put(str,str);
		map.clear();
		assertTrue(map.isEmpty());
	}

	@Test
	public void testKeySet() {
		String[] astr=new String[]{"test","test1","test2","test3"};
		Map<String, String> map = new NyxMap<>();
		for (String str : astr) 
			map.put(str,str);
		assertTrue(map.keySet().containsAll(Arrays.asList(astr)));
	}

	@Test
	public void testValues() {
		String[] astr=new String[]{"test","test1","test2","test3"};
		Map<String, String> map = new NyxMap<>();
		for (String str : astr) 
			map.put(str,str);
		assertTrue(map.values().containsAll(Arrays.asList(astr)));
	}

	@Test
	public void testEntrySet() {
		String[] astr=new String[]{"test","test1","test2","test3"};
		Map<String, String> map = new NyxMap<>();
		for (String str : astr) 
			map.put(str,str);
		assertTrue(map.entrySet().size()==map.size());
	}

}
