package rs.igram.kiribi.service.util;

public interface CompletionListener<T> {
	void completed(T value);
	void failed(Throwable t);
}