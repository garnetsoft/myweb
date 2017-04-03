

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ChildOrderContainer {
	private static final Logger log = LogManager.getLogger(ChildOrderContainer.class);
	
	public final String EMPTY = "";
	public final Map<String, ChildOrder> orders;
	
	public ChildOrderContainer() {
		this.orders = new ConcurrentHashMap<String, ChildOrder>();
	}
	
	public double getSize() {
		double size = 0.0;
		for (ChildOrder o: orders.values()) {
			size += o.size;
		}
		
		return size;
	}
	
	public List<ChildOrder> cloneOrders() {
		List<ChildOrder> listClone = new ArrayList<ChildOrder>(orders.size());
		for (ChildOrder o : orders.values()) {
			listClone.add(o.clone());
		}
		
		return listClone;
	}
	
	public boolean insertChildOrder(ChildOrder order) {
		boolean result = false;
		
		if (order!= null && order.orderId != null && order.orderId.length() > 0) 
		{
			orders.put(order.orderId, order);
			result = orders.containsKey(order.orderId);
		}
		else 
		{
			log.warn("Null/Empty orderId - Insert fail");
		}
		
		return result;
	}

	public boolean deleteChildOrder(String ordID) {
		boolean result = false;
		
		if (ordID != null && !ordID.equals("") && orders.containsKey(ordID)) 
		{
			orders.remove(ordID);
			result = true;
		}
		else 
		{
			//log.warn("Can't find order to delete: " + ordID);
		}
		
		return result;
	}
	
	public boolean updateChildOrder(String ordID, ChildOrder newOrd) {
		boolean result = false;
		
		if (ordID != null && !ordID.equals("") && orders.containsKey(ordID)) {
			result = deleteChildOrder(ordID) && insertChildOrder(newOrd);
		}
		
		return result;
	}
	
	public ChildOrder findChildOrder(String ordID) {
		ChildOrder ordRes = null;
		
		if (ordID != null && !ordID.equals("") && orders.containsKey(ordID)) {
			ordRes = orders.get(ordID);
		}
		
		return ordRes;
	}
	
	public List<ChildOrder> findChildOrders(double price) {
		List<ChildOrder> ordList = new ArrayList<ChildOrder>();
		
		for (ChildOrder o : orders.values()) {
			if (o.price == price) {
				ordList.add(o.clone());
			}
		}
		
		return ordList;
	}
	
	public List<ChildOrder> getChildOrders() {
		return new ArrayList<ChildOrder>(orders.values());
	}
	
	public static void sortOrderList(Order.EnSide side, List<ChildOrder> ordList, boolean resverse) {
		if (side == Order.EnSide.buy) {
			Collections.sort(ordList, Collections.reverseOrder());
		}
		else {
			Collections.sort(ordList);
		}		
	}
	
	
	public boolean clearOrders() {
		orders.clear();
		return orders.isEmpty();
	}
	
	
	public void logOrders() {
		for (ChildOrder o : this.orders.values()) {
			o.logOrder();
		}		
	}
	
}
