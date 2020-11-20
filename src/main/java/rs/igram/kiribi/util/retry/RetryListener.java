package rs.igram.kiribi.service.util.retry;

import rs.igram.kiribi.service.util.CompletionListener;

public interface RetryListener<T> extends CompletionListener<T> {
	void canceled(boolean b);
}