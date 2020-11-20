package rs.igram.kiribi.service.util;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import rs.igram.kiribi.net.NetworkExecutor;
import rs.igram.kiribi.service.ServiceAdmin;

public class ConsumerSupport<T> {
	protected final CopyOnWriteArrayList<Consumer<T>> consumers = new CopyOnWriteArrayList();

	public ConsumerSupport() {}

	public void addConsumer(Consumer<T> consumer) {consumers.addIfAbsent(consumer);}
	
	public void removeConsumer(Consumer<T> consumer) {consumers.remove(consumer);}
	
	public void consume(T item) {
		consumers.forEach(c -> c.accept(item));
	}
	
	public void consume(T item, NetworkExecutor executor) {
		executor.submit(() -> consumers.forEach(c -> c.accept(item)));
	}
	
	public static <T> Consumer<T> consumeIf(Predicate<T> predicate, Consumer<T> consumer) {
		return t -> {
			if(predicate.test(t)) consumer.accept(t);
		};
	}
}