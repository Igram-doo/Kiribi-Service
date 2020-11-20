package rs.igram.kiribi.service.util;

import java.util.concurrent.TimeUnit;

public class Duration<T extends TimeUnit> {
	public final T unit;
	public final long value;
	
	public Duration(T unit, long value) {
		this.unit = unit;
		this.value = value;
	}
	
	public void sleep() throws InterruptedException {
		unit.sleep(value);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Duration){
			Duration d = (Duration)o;
			return unit == d.unit && value == d.value;
		}
		return false;
	}
	
	public String toString() {return "Duration["+unit+":"+value+"]";}
}