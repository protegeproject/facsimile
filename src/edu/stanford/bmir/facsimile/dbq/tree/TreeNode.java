package edu.stanford.bmir.facsimile.dbq.tree;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Rafael S. Goncalves <br>
 * Stanford Center for Biomedical Informatics Research (BMIR) <br>
 * School of Medicine, Stanford University <br>
 */
public class TreeNode<T> implements Iterable<TreeNode<T>> {
	private List<TreeNode<T>> elementsIndex;
	public T data;
	public TreeNode<T> parent;
	public List<TreeNode<T>> children;

	
	/**
	 * Constructor
	 * @param data	Data in the tree node
	 */
	public TreeNode(T data) {
		this.data = data;
		this.children = new LinkedList<TreeNode<T>>();
		this.elementsIndex = new LinkedList<TreeNode<T>>();
		this.elementsIndex.add(this);
	}
	
	
	/**
	 * Check if this node is the root node
	 * @return true if this node is a root node, false otherwise
	 */
	public boolean isRoot() {
		return parent == null;
	}

	
	/**
	 * Check if this node is a leaf node
	 * @return true if this node is a leaf node, false otherwise
	 */
	public boolean isLeaf() {
		return children.size() == 0;
	}
	

	/**
	 * Add a child node with the given value to the current node
	 * @param child	Child node data
	 * @return Child tree node
	 */
	public TreeNode<T> addChild(T child) {
		TreeNode<T> childNode = new TreeNode<T>(child);
		childNode.parent = this;
		this.children.add(childNode);
		this.registerChildForSearch(childNode);
		return childNode;
	}
	
	
	public void addChild(TreeNode<T> node) {
		node.parent = this;
		this.children.add(node);
		this.registerChildForSearch(node);
	}

	
	/**
	 * Get the depth of this node
	 * @return Depth of this node
	 */
	public int getLevel() {
		if(this.isRoot())
			return 0;
		else
			return parent.getLevel() + 1;
	}

	
	/**
	 * Register a given node for search 
	 * @param node	Tree node
	 */
	private void registerChildForSearch(TreeNode<T> node) {
		elementsIndex.add(node);
		if(parent != null)
			parent.registerChildForSearch(node);
	}

	
	/**
	 * Find and retrieve a node with the given data
	 * @param cmp	Data
	 * @return Tree node
	 */
	public TreeNode<T> findTreeNode(Comparable<T> cmp) {
		for (TreeNode<T> element : this.elementsIndex) {
			T elData = element.data;
			if (cmp.compareTo(elData) == 0)
				return element;
		}
		return null;
	}

	
	@Override
	public String toString() {
		return data != null ? data.toString() : "[data null]";
	}

	
	@Override
	public Iterator<TreeNode<T>> iterator() {
		TreeNodeIterator<T> iter = new TreeNodeIterator<T>(this);
		return iter;
	}
}