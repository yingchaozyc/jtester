package org.testng.internal.thread.graph;

import java.util.List;

/**
 * 线程工厂。
 * 
 * A factory that creates workers used by {@code GraphThreadPoolExecutor}
 * 
 * @author nullin
 * 
 * @param <T>
 */
public interface IThreadWorkerFactory<T> {

	/**
	 * 为一组任务创建IWorker对象。不要求返回的IWorker数目要和任务数目保持一致。
	 * 
	 * Creates {@code IWorker} for specified set of tasks. It is not necessary
	 * that number of workers returned be same as number of tasks entered.
	 * 
	 * @param freeNodes
	 *            tasks that need to be executed
	 * @return list of workers
	 */
	List<IWorker<T>> createWorkers(List<T> freeNodes);
}
