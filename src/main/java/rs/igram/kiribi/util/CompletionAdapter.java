package rs.igram.kiribi.service.util;

import java.util.function.Consumer;

public class CompletionAdapter<T> implements CompletionListener<T> {
	protected Consumer<T> onCompleted;
	protected Consumer<Throwable> onFailed;
	
	public CompletionAdapter() {}
	
	public CompletionAdapter(Consumer<T> onCompleted, Consumer<Throwable> onFailed) {
		this.onCompleted = onCompleted;
		this.onFailed = onFailed;
	}
	
	public CompletionAdapter<T> onCompleted(Consumer<T> value) {
		onCompleted = value;
		return this;
	}
	
	public CompletionAdapter<T> onFailed(Consumer<Throwable> value) {
		onFailed = value;
		return this;
	}
	
	@Override
	public void completed(T value) {
		if(onCompleted != null) onCompleted.accept(value);
	}
	
	@Override
	public void failed(Throwable t) {
		if(onFailed != null) onFailed.accept(t);
	}
}