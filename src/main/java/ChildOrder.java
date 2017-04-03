

import java.util.Comparator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ChildOrder implements Cloneable, Comparable {
	private static final Logger log = LogManager.getLogger(ChildOrder.class);
	
	public String orderId;
	public Order.EnSide side;
	public double price;
	public double size;
	public Order.EnType type;	
	private Order.EnState state; 
	
	public ChildOrder(String _id, Order.EnSide _s, double _px, double _size) {
		this.orderId = _id;
		this.side = _s;
		this.price = _px;
		this.size = _size;
		this.type = Order.EnType.SNAPSHOT;
	}
	
	public ChildOrder(String _id, Order.EnSide _s, double _px, double _size, Order.EnType _type) {
		this.orderId = _id;
		this.side = _s;
		this.price = _px;
		this.size = _size;
		this.type = _type;
	}
	
	public String getOrderID() 
	{
		return this.orderId;
	}
	
	public double getPrice() 
	{
		return this.price;
	}
	
	public double getSize()
	{
		return this.size;
	}
	
	public Order.EnSide getSide() 
	{
		return this.side;
	}
	
	public Order.EnType getType()
	{
		return this.type;
	}
	
	public Order.EnState getState() 
	{
		return this.state;
	}
	
	public void setState(Order.EnState state)
	{
		this.state = state;
	}
	
	
	public ChildOrder clone() {
		//return this.clone();
		return this;
	}

	public void logOrder() {
		log.debug(String.format("ChildOrder:[%f, %f, %s, %s, %s, %s]", price, size, side, orderId, type, state));
	}
	
	@Override
	public String toString()
	{
		return String.format("ChildOrder:[%f, %f, %s, %s, %s, %s]", price, size, side, orderId, type, state);
	}
	
	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public class ChildOrderSorterByPrice implements Comparator<ChildOrder> {
		@Override
		public int compare(ChildOrder x, ChildOrder y) {
			if (x.price == y.price)
				return 0;
			return x.price > y.price ? 1 : -1;
		}
		
	}
	
	public class ChildOrderSorterByPriceSize implements Comparator<ChildOrder> {
		@Override
		public int compare(ChildOrder x, ChildOrder y) {
			int result = x.compareTo(y);
			
			if ( result == 0) {
				
			}
			
			return 0;
		}
		
	}
	
	public class ChildOrderSorterByPriceTime implements Comparator<ChildOrder> {
		@Override
		public int compare(ChildOrder o1, ChildOrder o2) {
			return 0;
		}
		
	}
	
}
