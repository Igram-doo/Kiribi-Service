package rs.igram.kiribi.service.util.retry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import rs.igram.kiribi.net.NetworkExecutor;

public class RetryTask<T> {
	private static final Set<Future> futures = Collections.synchronizedSet(new HashSet<>());
	private static boolean terminating;
	
	protected final RetryPolicy policy;
	protected final Class[] retryOnExceptions;
	protected RetryFuture<T> future;
	protected Callable<T> callable;
	protected Future<?> timer;
	
	public RetryTask() {
		this(new RetryPolicy.DefaultPolicy(), null);
	}

	public RetryTask(Class... retryOnExceptions) {
		this(new RetryPolicy.DefaultPolicy(), retryOnExceptions);
	}

	public RetryTask(RetryPolicy policy) {
		this(policy, null);
	}

	public RetryTask(RetryPolicy policy, Class[] retryOnExceptions) {
		this.policy = policy;
		this.retryOnExceptions = retryOnExceptions;
	}

	public Future<T> execute(Callable<T> callable, NetworkExecutor executor) {
		return execute(callable, null, executor);
	}
	
	public Future<T> execute(Callable<T> callable, RetryListener<T> listener, NetworkExecutor executor) {
		if(this.callable != null) throw new IllegalStateException();

		this.callable = callable;
		future = new RetryFuture<>(listener);
		add(future);
		timer = executor.submit(run());

		return Future.class.cast(future);
	}
	
	protected Runnable run() {
		return () -> {
			while(!future.isDone()){
				try{
					T result = callable.call();
					future.complete(result);
					break;
				}catch(Exception e){
					if(retry(e)){
						try{
							policy.next().sleep();
						}catch(InterruptedException ex){
							future.completeExceptionally(ex);
							break;
						}
					}else{
						future.completeExceptionally(e);
						break;
					}
				}
			}
		};
	}
	
	protected boolean retry(Exception e){
		if(future.isDone() || !policy.hasNext()) return false;
		if(retryOnExceptions == null) return true;
		Class c = e.getClass();
		for(Class ex : retryOnExceptions){
			if(ex.isAssignableFrom(c)) return true;
		}
		return false;
	}
	
	protected static final void add(Future future) {futures.add(future);}
	protected static final void remove(Future future) {if(!terminating) futures.remove(future);}
	public static final void shutdown() {
		// only call when shutting down
		terminating = true;
		futures.forEach(f -> f.cancel(true));
		futures.clear();
	}
	
	protected class RetryFuture<T> extends CompletableFuture<T> {
		protected RetryListener<T> listener;
		
		protected RetryFuture(RetryListener<T> listener) {
			this.listener = listener;
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if(timer != null) timer.cancel(mayInterruptIfRunning);
			boolean result = super.cancel(mayInterruptIfRunning);
			remove(this);
			if(listener != null) listener.canceled(result);
			
			return result;
		}
		
		@Override
		public boolean complete(T value) {
			boolean result = super.complete(value);
			remove(this);
			if(listener != null) listener.completed(value);
			
			return result;
		}
		
		@Override
		public boolean completeExceptionally(Throwable t) {
			boolean result = super.completeExceptionally(t);
			remove(this);
			if(listener != null) listener.failed(t);
			
			return result;
		}
	}
}