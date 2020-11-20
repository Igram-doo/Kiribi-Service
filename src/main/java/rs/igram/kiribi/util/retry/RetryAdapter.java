package rs.igram.kiribi.service.util.retry;

import java.util.function.Consumer;

import rs.igram.kiribi.service.util.CompletionAdapter;

public class RetryAdapter<T> extends CompletionAdapter<T> implements RetryListener<T> {
	protected Consumer<Boolean> onCanceled;
	
	public RetryAdapter() {}
	
	public RetryAdapter(Consumer<T> onCompleted, Consumer<Throwable> onFailed, Consumer<Boolean> onCanceled) {
		super(onCompleted, onFailed);
		
		this.onCanceled = onCanceled;
	}

	public RetryAdapter<T> onCanceled(Consumer<Boolean> value) {
		onCanceled = value;
		return this;
	}

	@Override
	public void canceled(boolean b) {
		if(onCanceled != null) onCanceled.accept(b);
	}
	
	public static <T> RetryAdapter<T> wrap(RetryListener<T> listener) {
		RetryAdapter<T> adapter = new RetryAdapter<>();
		adapter.onCompleted(t -> listener.completed(t))
			   .onFailed(t -> listener.failed(t));
		
		return adapter;
	}
}