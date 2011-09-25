package com.qcadoo.model.api.utils;

import static com.google.common.collect.Lists.newLinkedList;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityTree;
import com.qcadoo.model.api.EntityTreeNode;
import com.qcadoo.model.internal.EntityTreeImpl;

/**
 * Helper service for automatically generating numbers for EntityTree nodes
 * 
 * @since 0.4.8
 */
@Service
public class TreeNumberingService {
    
    private static TreeNodesNumberComparator treeNodesNumberComparator = null;
    /**
     * Generate new numbers for all nodes of the tree
     * 
     * @param tree
     *            tree to be numbered 
     */
    public void generateTreeNumbers(final EntityTree tree) {
        if(tree.getRoot() == null) {
            return;
        }
        assignNumberToTreeNode(tree.getRoot(), "1.");
    }
    
    /**
     * Generate new numbers for all sub-nodes of given tree node
     * 
     * @param treeNode
     *            tree node to be numbered (with sub-nodes) 
     */
    public void generateTreeNumbers(final EntityTreeNode treeNode) {
        assignNumberToTreeNode(treeNode, "1.");
    }
    
    /**
     * Generate new numbers for all nodes of the tree
     * 
     * @param dd
     *            node component DataDefinition
     * @param joinFieldName
     *            name of tree field
     * @param belongsToEntityId
     *            id of owning tree entity
     */
    public void generateNumbersAndUpdateTree(final DataDefinition dd, final String joinFieldName, final Long belongsToEntityId) {
        EntityTree tree = new EntityTreeImpl(dd, joinFieldName, belongsToEntityId);
        if (tree.getRoot().getField("nodeNumber") != null) {
            return;
        }
        generateTreeNumbers(tree);
        for (Entity treeNode : tree) {
            dd.save(treeNode);
        }
    }

    /**
     * Getter for tree node numbers comparator
     * 
     * @return instance of TreeNodesNumberComparator 
     */
    public Comparator<Entity> getTreeNodesNumberComparator() {
        if (treeNodesNumberComparator == null) {
            treeNodesNumberComparator = new TreeNodesNumberComparator();
        }
        return treeNodesNumberComparator;
    }
    
    private void assignNumberToTreeNode(final EntityTreeNode treeNode, final String prefix) {
        treeNode.setField("nodeNumber", prefix);
        List<EntityTreeNode> childrens = newLinkedList(treeNode.getChildren());
        
        Collections.sort(childrens, new Comparator<EntityTreeNode>() {
            @Override
            public int compare(final EntityTreeNode n1, final EntityTreeNode n2) {
                Integer p1 = (Integer) n1.getField("priority");
                Integer p2 = (Integer) n2.getField("priority");
                return p1.compareTo(p2);
            }
        });

        Long childsCounter = 1L; 
        for (EntityTreeNode children : childrens) {
            assignNumberToTreeNode(children, prefix + childsCounter + '.');
            childsCounter++;
        }
    }
    
    private final class TreeNodesNumberComparator implements Comparator<Entity> {
        @Override
        public int compare(final Entity e1, final Entity e2) {
            String n1 = e1.getStringField("nodeNumber");
            String n2 = e2.getStringField("nodeNumber");
            return n1.compareTo(n2);
        }
    }
}
