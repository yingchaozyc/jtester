package org.testng.internal;

import org.testng.collections.ListMultiMap;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.internal.annotations.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 
 * 感觉和下边的东西有关  DOT画图语言 &　Graphviz
 * 
 * http://zh.wikipedia.org/zh/Graphviz
 * http://www.cnblogs.com/sld666666/archive/2010/06/25/1765510.html
 * 
 * TODO 这个类，很重要。
 * 
 * 有向图的概念
 * http://zh.wikipedia.org/wiki/%E6%9C%89%E5%90%91%E5%9B%BE#.E6.9C.AF.E8.AF.AD
 * 
 * 其实要执行的测试方法因为有各种各样的依赖关系，
 * 所以组成成了有向图，给作者点赞~
 *  
 * Representation of the graph of methods.
 */
public class DynamicGraph<T> {
	private static final boolean DEBUG = false;
	
	// 节点列表类型设置为set的原因就是不用在添加节点考虑节点重复的问题
	private Set<T> m_nodesReady = Sets.newLinkedHashSet();
	private Set<T> m_nodesRunning = Sets.newLinkedHashSet();
	private Set<T> m_nodesFinished = Sets.newLinkedHashSet();

	// 是否为空会影响到free node的排序。
	private Comparator<? super T> m_nodeComparator = null;

	/**
	 *  从from 到 to的关系维护在m_dependedUpon. 
	 */
	private ListMultiMap<T, T> m_dependedUpon = Maps.newListMultiMap();
	
	/** 
	 *  从to 到 from的关系维护在m_dependingOn.
	 */
	private ListMultiMap<T, T> m_dependingOn = Maps.newListMultiMap();

	public static enum Status {
		READY, RUNNING, FINISHED
	}

	/**
	 * Define a comparator for the nodes of this graph, which will be used to
	 * order the free nodes when they are asked.
	 */
	public void setComparator(Comparator<? super T> c) {
		m_nodeComparator = c;
	}

	/**
	 * 新加入的节点都是准备状态。
	 * 
	 * Add a node to the graph.   
	 */
	public void addNode(T node) {
		m_nodesReady.add(node);
	}

	/**
	 * addEdge是加一条边的意思。
	 * 
	 * 因为Node的List是Set类型的。所以大胆的addNode。反正如果有就等于啥都没做。
	 * 维持两个关系
	 * 1. 从from 到 to的关系维护在m_dependedUpon.
	 * 2. 从to 到 from的关系维护在m_dependingOn.
	 * 
	 * Add an edge between two nodes, which don't have to already be in the
	 * graph (they will be added by this method).
	 */
	public void addEdge(T from, T to) {
		addNode(from);
		addNode(to);
		m_dependingOn.put(to, from);
		m_dependedUpon.put(from, to);
	}

	/**
	 * 返回不依赖任何其他节点的列表。
	 * 
	 * 如果A项目依赖于B项目， B项目依赖于C项目， C项目依赖于D项目。
	 * 那么D项目就是最底层最核心的东西。 
	 * C做了改变，D不需要知道
	 * A做了改变，其他任何人不需要知道。
	 * 
	 * @return a set of all the nodes that don't depend on any other nodes.
	 */
	public List<T> getFreeNodes() {
		List<T> result = Lists.newArrayList();
		for (T m : m_nodesReady) {
			// A node is free if...
			// 返回当前方法被别人依赖的上层列表
			List<T> du = m_dependedUpon.get(m);
			// - no other nodes depend on it 
			if (!m_dependedUpon.containsKey(m)) {
				// 如果没人依赖我，可以放心调用，当前方法是FreeNode
				result.add(m);
			} else if (getUnfinishedNodes(du).size() == 0) {
				// 如果有人依赖我，但是依赖我的方法都跑完了
				// 我就可以放心调用了，现在我就是FreeNode了
				result.add(m);
			}
		}

		// Sort the free nodes if requested (e.g. priorities)
		if (result != null && !result.isEmpty()) {
			if (m_nodeComparator != null) {
				Collections.sort(result, m_nodeComparator);
				ppp("Nodes after sorting:" + result.get(0));
			}
		}

		return result;
	}

	/**
	 * @return a list of all the nodes that have a status other than FINISHED.
	 */
	private Collection<? extends T> getUnfinishedNodes(List<T> nodes) {
		Set<T> result = Sets.newHashSet();
		for (T node : nodes) {
			if (m_nodesReady.contains(node) || m_nodesRunning.contains(node)) {
				result.add(node);
			}
		}
		return result;
	}

	/**
	 * Set the status for a set of nodes.
	 */
	public void setStatus(Collection<T> nodes, Status status) {
		for (T n : nodes) {
			setStatus(n, status);
		}
	}

	/**
	 * Set the status for a node.
	 */
	public void setStatus(T node, Status status) {
		removeNode(node);
		switch (status) {
		case READY:
			m_nodesReady.add(node);
			break;
		case RUNNING:
			m_nodesRunning.add(node);
			break;
		case FINISHED:
			m_nodesFinished.add(node);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	private void removeNode(T node) {
		if (!m_nodesReady.remove(node)) {
			if (!m_nodesRunning.remove(node)) {
				m_nodesFinished.remove(node);
			}
		}
	}

	/**
	 * @return the number of nodes in this graph.
	 */
	public int getNodeCount() {
		int result = m_nodesReady.size() + m_nodesRunning.size()
				+ m_nodesFinished.size();
		return result;
	}

	public int getNodeCountWithStatus(Status status) {
		switch (status) {
		case READY:
			return m_nodesReady.size();
		case RUNNING:
			return m_nodesRunning.size();
		case FINISHED:
			return m_nodesFinished.size();
		default:
			throw new IllegalArgumentException();
		}
	}

	private static void ppp(String string) {
		if (DEBUG) {
			System.out.println("   [GroupThreadPoolExecutor] "
					+ Thread.currentThread().getId() + " " + string);
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("[DynamicGraph ");
		result.append("\n  Ready:" + m_nodesReady);
		result.append("\n  Running:" + m_nodesRunning);
		result.append("\n  Finished:" + m_nodesFinished);
		result.append("\n  Edges:\n");
		for (Map.Entry<T, List<T>> es : m_dependingOn.getEntrySet()) {
			result.append("     " + es.getKey() + "\n");
			for (T t : es.getValue()) {
				result.append("        " + t + "\n");
			}
		}
		result.append("]");
		return result.toString();
	}

	private String getName(T t) {
		String s = t.toString();
		int n1 = s.lastIndexOf('.') + 1;
		int n2 = s.indexOf('(');
		return s.substring(n1, n2);
	}

	/**
	 * @return a .dot file (GraphViz) version of this graph.
	 */
	public String toDot() {
		String FREE = "[style=filled color=yellow]";
		String RUNNING = "[style=filled color=green]";
		String FINISHED = "[style=filled color=grey]";
		StringBuilder result = new StringBuilder("digraph g {\n");
		List<T> freeNodes = getFreeNodes();
		String color;
		for (T n : m_nodesReady) {
			color = freeNodes.contains(n) ? FREE : "";
			result.append("  " + getName(n) + color + "\n");
		}
		for (T n : m_nodesRunning) {
			color = freeNodes.contains(n) ? FREE : RUNNING;
			result.append("  " + getName(n) + color + "\n");
		}
		for (T n : m_nodesFinished) {
			result.append("  " + getName(n) + FINISHED + "\n");
		}
		result.append("\n");

		for (T k : m_dependingOn.getKeys()) {
			List<T> nodes = m_dependingOn.get(k);
			for (T n : nodes) {
				String dotted = m_nodesFinished.contains(k) ? "style=dotted"
						: "";
				result.append("  " + getName(k) + " -> " + getName(n)
						+ " [dir=back " + dotted + "]\n");
			}
		}
		result.append("}\n");

		return result.toString();
	}

	public ListMultiMap<T, T> getEdges() {
		return m_dependingOn;
	}

}
