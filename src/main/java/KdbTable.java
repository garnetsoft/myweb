
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kx.*;
import kx.c.*;

public class KdbTable {

	Flip f;
	//String[] header;
	//Object[][] data;
	
	public KdbTable(Object x) throws Exception {
		try {
			this.f = c.td(x);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int columns() {
		return f.x.length;
	}
	
	public int rows() {
		return Array.getLength(f.y[0]);
	}
	
	public Object[] getColumn(String colName) {
		return unmashallList(f.at(colName));
	}
	
	public Map<String, Object> getRow(int i) {
		Map<String, Object> m = new HashMap<String, Object>();
		
		for (int j = 0; j < columns(); j++) {
			m.put(f.x[j], c.at(f.y[j], i));
		}
		
		return m;
	}
	
	public Object getEntry(String colKey, int i) {
		return c.at(f.at(colKey), i);
	}
	
	public Map<String, Object[]> asMap() {
		Map<String, Object[]> m = new HashMap<String, Object[]>();
		
		for (int j = 0; j < columns(); j++) {
			m.put(f.x[j], unmashallList(f.y[j]));
		}
		
		return m;
	}
	
	public static Object[] unmashallList(Object o) {		
		int l = Array.getLength(o);
		Object[] out = new Object[l];
		
		for (int i = 0; i < l; i++) {
			out[i] = c.at(o, i);
		}
		
		return out;		
	}

	public static Object mashallList(Object[] o) {		
		Object[] out = new Object[o.length];
		
		for (int i = 0; i < o.length; i++) {
			c.set(out, i, o[i]);
		}
		
		return out;		
	}
	
	public List<String> getKeys() {
		List<String> keys = new ArrayList<String>();
		
		for (int i = 0; i < f.x.length; i++) {
			keys.add(f.x[i]);
		}
		
		return keys;
	}
	
	public Object[] getValues() {
		return null;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		List<String> header = getKeys();
		int charCount = 0;
		for (String h : header) {
			sb.append(h).append("|");
			charCount += h.length()+1;
		}
 		sb.append("\n");
 		for (int i = 0; i < charCount; i++)
 			sb.append("-");
 		sb.append("\n");
 		
 		for (int j = 0; j < rows(); j++) {
 			Map<String, Object> row = getRow(j);
 			for (Map.Entry<String, Object> e : row.entrySet()) {
 				sb.append(row.get(e.getKey())).append("|");
 			}
 			sb.append("\n");
 		}
 		sb.append("\n");
 		
		return sb.toString();
	}
	

}
