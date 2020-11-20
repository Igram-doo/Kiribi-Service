package rs.igram.kiribi.service.util.retry;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import rs.igram.kiribi.service.util.Duration;

public abstract class RetryPolicy implements Iterator<Duration<?>> {
	public static class DefaultPolicy extends CompoundPolicy {
		public DefaultPolicy() {
			super(
				new SimplePolicy(4, new Duration(TimeUnit.SECONDS, 15)),
				new SimplePolicy(4, new Duration(TimeUnit.MINUTES, 1)),
				new SimplePolicy(5, new Duration(TimeUnit.MINUTES, 3)),
				new SimplePolicy(4, new Duration(TimeUnit.MINUTES, 5)),
				new SimplePolicy(3, new Duration(TimeUnit.MINUTES, 10)),
				new SimplePolicy(new Duration(TimeUnit.MINUTES, 15))
			);
		}
	}
	
	public static class SimplePolicy extends RetryPolicy {
		protected final Supplier<Duration<?>> supplier;
		protected final int max;
		protected int count = 0;
		
		public SimplePolicy(Duration<?> duration) {
			this(() -> duration);
		}
		
		public SimplePolicy(Supplier<Duration<?>> supplier) {
			this(-1, supplier);
		}
		
		public SimplePolicy(int max, Duration<?> duration) {
			this(max, () -> duration);
		}
		
		public SimplePolicy(int max, Supplier<Duration<?>> supplier) {
			this.max = max;
			this.supplier = supplier;
		}
		
		@Override
		public boolean hasNext() {return max != -1 && count < max;}
				
		@Override
		public Duration<?> next() {
			if(!hasNext()) return null;
			increment();
			return supplier.get();
		}
		
		protected void increment() {
			count++;
		}
	}
	public static class CompoundPolicy extends RetryPolicy {
		protected final RetryPolicy[] policies;
		protected final int max;
		protected int count = 0;
		
		public CompoundPolicy(RetryPolicy... policies) {
			this.policies = policies;
			
			max = policies == null ? 0 : policies.length;
		}
		
		@Override
		public boolean hasNext() {
			if(max == 0) return false;
			if(policies[count].hasNext()) return true;
			return count < max - 1 && policies[count +1].hasNext();
		}
				
		@Override
		public Duration<?> next() {
			if(policies[count].hasNext()) return policies[count].next();
			if(count < max){
				increment();
				return policies[count].hasNext() ? policies[count].next() : null;
			}else{
				return null;
			}
		}
				
		protected void increment() {
			count++;
		}
	}
}