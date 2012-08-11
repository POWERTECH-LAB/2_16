/*
 * Copyright (c) 2004, 2008, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation. Oracle designates this particular file as subject to the "Classpath" exception as provided by Oracle in the LICENSE file that
 * accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License version 2 for more details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com if you need additional information or have any questions.
 */

package sun.tools.jconsole.inspector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import sun.tools.jconsole.JConsole;
import sun.tools.jconsole.MBeansTab;
import sun.tools.jconsole.Resources;
import sun.tools.jconsole.inspector.XNodeInfo.Type;

@SuppressWarnings("serial")
public class XTree extends JTree {

    private static final List<String> orderedKeyPropertyList = new ArrayList<String>();

    static {
        String keyPropertyList = System.getProperty("com.sun.tools.jconsole.mbeans.keyPropertyList");
        if (keyPropertyList == null) {
            orderedKeyPropertyList.add("type");
            orderedKeyPropertyList.add("j2eeType");
        } else {
            StringTokenizer st = new StringTokenizer(keyPropertyList, ",");
            while (st.hasMoreTokens()) {
                orderedKeyPropertyList.add(st.nextToken());
            }
        }
    }
    private MBeansTab mbeansTab;
    private Map<String, DefaultMutableTreeNode> nodes = new HashMap<String, DefaultMutableTreeNode>();

    public XTree(MBeansTab mbeansTab) {
        this(new DefaultMutableTreeNode("MBeanTreeRootNode"), mbeansTab);
    }

    public XTree(TreeNode root, MBeansTab mbeansTab) {
        super(root, true);
        this.mbeansTab = mbeansTab;
        setRootVisible(false);
        setShowsRootHandles(true);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * This method removes the node from its parent
     */
    // Call on EDT
    private synchronized void removeChildNode(DefaultMutableTreeNode child) {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        model.removeNodeFromParent(child);
    }

    /**
     * This method adds the child to the specified parent node
     * at specific index.
     */
    // Call on EDT
    private synchronized void addChildNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode child, int index) {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        model.insertNodeInto(child, parent, index);
    }

    /**
     * This method adds the child to the specified parent node.
     * The index where the child is to be added depends on the
     * child node being Comparable or not. If the child node is
     * not Comparable then it is added at the end, i.e. right
     * after the current parent's children.
     */
    // Call on EDT
    private synchronized void addChildNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
        int childCount = parent.getChildCount();
        if (childCount == 0) {
            addChildNode(parent, child, 0);
            return;
        }
        if (child instanceof ComparableDefaultMutableTreeNode) {
            ComparableDefaultMutableTreeNode comparableChild = (ComparableDefaultMutableTreeNode) child;
            for (int i = childCount - 1; i >= 0; i--) {
                DefaultMutableTreeNode brother = (DefaultMutableTreeNode) parent.getChildAt(i);
                // expr1: child node must be inserted after metadata nodes
                // - OR -
                // expr2: "child >= brother"
                if ((i <= 2 && isMetadataNode(brother)) || comparableChild.compareTo(brother) >= 0) {
                    addChildNode(parent, child, i + 1);
                    return;
                }
            }
            // "child < all brothers", add at the beginning
            addChildNode(parent, child, 0);
            return;
        }
        // "child not comparable", add at the end
        addChildNode(parent, child, childCount);
    }

    /**
     * This method removes all the displayed nodes from the tree,
     * but does not affect actual MBeanServer contents.
     */
    // Call on EDT
    @Override
    public synchronized void removeAll() {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        root.removeAllChildren();
        model.nodeStructureChanged(root);
        this.nodes.clear();
    }

    // Call on EDT
    public synchronized void removeMBeanFromView(ObjectName mbean) {
        // We assume here that MBeans are removed one by one (on MBean
        // unregistered notification). Deletes the tree node associated
        // with the given MBean and recursively all the node parents
        // which are leaves and non XMBean.
        //
        DefaultMutableTreeNode node = null;
        Dn dn = new Dn(mbean);
        if (dn.getTokenCount() > 0) {
            DefaultTreeModel model = (DefaultTreeModel) getModel();
            Token token = dn.getToken(0);
            String hashKey = dn.getHashKey(token);
            node = this.nodes.get(hashKey);
            if ((node != null) && (!node.isRoot())) {
                if (hasNonMetadataNodes(node)) {
                    removeMetadataNodes(node);
                    String label = token.getValue();
                    XNodeInfo userObject = new XNodeInfo(Type.NONMBEAN, label, label, token.getTokenValue());
                    changeNodeValue(node, userObject);
                } else {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    model.removeNodeFromParent(node);
                    this.nodes.remove(hashKey);
                    removeParentFromView(dn, 1, parent);
                }
            }
        }
    }

    /**
     * Returns true if any of the children nodes is a non MBean metadata node.
     */
    private boolean hasNonMetadataNodes(DefaultMutableTreeNode node) {
        for (Enumeration e = node.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            Object uo = n.getUserObject();
            if (uo instanceof XNodeInfo) {
                switch (((XNodeInfo) uo).getType()) {
                    case ATTRIBUTES:
                    case NOTIFICATIONS:
                    case OPERATIONS:
                        break;
                    default:
                        return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any of the children nodes is an MBean metadata node.
     */
    public boolean hasMetadataNodes(DefaultMutableTreeNode node) {
        for (Enumeration e = node.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            Object uo = n.getUserObject();
            if (uo instanceof XNodeInfo) {
                switch (((XNodeInfo) uo).getType()) {
                    case ATTRIBUTES:
                    case NOTIFICATIONS:
                    case OPERATIONS:
                        return true;
                    default:
                        break;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Returns true if the given node is an MBean metadata node.
     */
    public boolean isMetadataNode(DefaultMutableTreeNode node) {
        Object uo = node.getUserObject();
        if (uo instanceof XNodeInfo) {
            switch (((XNodeInfo) uo).getType()) {
                case ATTRIBUTES:
                case NOTIFICATIONS:
                case OPERATIONS:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Remove the metadata nodes associated with a given MBean node.
     */
    // Call on EDT
    private void removeMetadataNodes(DefaultMutableTreeNode node) {
        Set<DefaultMutableTreeNode> metadataNodes = new HashSet<DefaultMutableTreeNode>();
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        for (Enumeration e = node.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            Object uo = n.getUserObject();
            if (uo instanceof XNodeInfo) {
                switch (((XNodeInfo) uo).getType()) {
                    case ATTRIBUTES:
                    case NOTIFICATIONS:
                    case OPERATIONS:
                        metadataNodes.add(n);
                        break;
                    default:
                        break;
                }
            }
        }
        for (DefaultMutableTreeNode n : metadataNodes) {
            model.removeNodeFromParent(n);
        }
    }

    /**
     * Removes only the parent nodes which are non MBean and leaf.
     * This method assumes the child nodes have been removed before.
     */
    // Call on EDT
    private DefaultMutableTreeNode removeParentFromView(Dn dn, int index, DefaultMutableTreeNode node) {
        if ((!node.isRoot()) && node.isLeaf() && (!(((XNodeInfo) node.getUserObject()).getType().equals(Type.MBEAN)))) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            removeChildNode(node);
            String hashKey = dn.getHashKey(dn.getToken(index));
            this.nodes.remove(hashKey);
            removeParentFromView(dn, index + 1, parent);
        }
        return node;
    }

    // Call on EDT
    public synchronized void addMBeansToView(Set<ObjectName> mbeans) {
        Set<Dn> dns = new TreeSet<Dn>();
        for (ObjectName mbean : mbeans) {
            Dn dn = new Dn(mbean);
            dns.add(dn);
        }
        for (Dn dn : dns) {
            ObjectName mbean = dn.getObjectName();
            XMBean xmbean = new XMBean(mbean, this.mbeansTab);
            addMBeanToView(mbean, xmbean, dn);
        }
    }

    // Call on EDT
    public synchronized void addMBeanToView(ObjectName mbean) {
        // Build XMBean for the given MBean
        //
        XMBean xmbean = new XMBean(mbean, this.mbeansTab);
        // Build Dn for the given MBean
        //
        Dn dn = new Dn(mbean);
        // Add the new nodes to the MBean tree from leaf to root
        //
        addMBeanToView(mbean, xmbean, dn);
    }

    // Call on EDT
    private synchronized void addMBeanToView(ObjectName mbean, XMBean xmbean, Dn dn) {

        DefaultMutableTreeNode childNode = null;
        DefaultMutableTreeNode parentNode = null;

        if ("chart".equals(mbean.getKeyProperty("type")))
            return;

        // Add the node or replace its user object if already added
        //
        Token token = dn.getToken(0);
        String hashKey = dn.getHashKey(token);
        if (this.nodes.containsKey(hashKey)) {
            // Found existing node previously created when adding another node
            //
            childNode = this.nodes.get(hashKey);
            // Replace user object to reflect that this node is an MBean
            //
            Object data = createNodeValue(xmbean, token);
            String label = data.toString();
            XNodeInfo userObject = new XNodeInfo(Type.MBEAN, data, label, mbean.toString());
            changeNodeValue(childNode, userObject);
            return;
        }

        // Create new leaf node
        //
        childNode = createDnNode(dn, token, xmbean);
        this.nodes.put(hashKey, childNode);

        // Add intermediate non MBean nodes
        //
        for (int i = 1; i < dn.getTokenCount(); i++) {
            token = dn.getToken(i);
            hashKey = dn.getHashKey(token);
            if (this.nodes.containsKey(hashKey)) {
                // Intermediate node already present, add new node as child
                //
                parentNode = this.nodes.get(hashKey);
                addChildNode(parentNode, childNode);
                return;
            } else {
                // Create new intermediate node
                //
                if ("domain".equals(token.getTokenType())) {
                    parentNode = createDomainNode(dn, token);
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
                    addChildNode(root, parentNode);
                } else {
                    parentNode = createSubDnNode(dn, token);
                }
                this.nodes.put(hashKey, parentNode);
                addChildNode(parentNode, childNode);
            }
            childNode = parentNode;
        }
    }

    // Call on EDT
    private synchronized void changeNodeValue(DefaultMutableTreeNode node, XNodeInfo nodeValue) {
        if (node instanceof ComparableDefaultMutableTreeNode) {
            // should it stay at the same place?
            DefaultMutableTreeNode clone = (DefaultMutableTreeNode) node.clone();
            clone.setUserObject(nodeValue);
            if (((ComparableDefaultMutableTreeNode) node).compareTo(clone) == 0) {
                // the order in the tree didn't change
                node.setUserObject(nodeValue);
                DefaultTreeModel model = (DefaultTreeModel) getModel();
                model.nodeChanged(node);
            } else {
                // delete the node and re-order it in case the
                // node value modifies the order in the tree
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                removeChildNode(node);
                node.setUserObject(nodeValue);
                addChildNode(parent, node);
            }
        } else {
            // not comparable stays at the same place
            node.setUserObject(nodeValue);
            DefaultTreeModel model = (DefaultTreeModel) getModel();
            model.nodeChanged(node);
        }
        // Load the MBean metadata if type is MBEAN
        if (nodeValue.getType().equals(Type.MBEAN)) {
            removeMetadataNodes(node);
            TreeNode[] treeNodes = node.getPath();
            TreePath path = new TreePath(treeNodes);
            if (isExpanded(path)) {
                addMetadataNodes(node);
            }
        }
        // Clear the current selection and set it
        // again so valueChanged() gets called
        if (node == getLastSelectedPathComponent()) {
            TreePath selectionPath = getSelectionPath();
            clearSelection();
            setSelectionPath(selectionPath);
        }
    }

    /**
     * Creates the domain node.
     */
    private DefaultMutableTreeNode createDomainNode(Dn dn, Token token) {
        DefaultMutableTreeNode node = new ComparableDefaultMutableTreeNode();
        String label = dn.getDomain();
        XNodeInfo userObject = new XNodeInfo(Type.NONMBEAN, label, label, label);
        node.setUserObject(userObject);
        return node;
    }

    /**
     * Creates the node corresponding to the whole Dn, i.e. an MBean.
     */
    private DefaultMutableTreeNode createDnNode(Dn dn, Token token, XMBean xmbean) {
        DefaultMutableTreeNode node = new ComparableDefaultMutableTreeNode();
        Object data = createNodeValue(xmbean, token);
        String label = data.toString();
        XNodeInfo userObject = new XNodeInfo(Type.MBEAN, data, label, xmbean.getObjectName().toString());
        node.setUserObject(userObject);
        return node;
    }

    /**
     * Creates the node corresponding to a subDn, i.e. a non-MBean
     * intermediate node.
     */
    private DefaultMutableTreeNode createSubDnNode(Dn dn, Token token) {
        DefaultMutableTreeNode node = new ComparableDefaultMutableTreeNode();
        String label = isKeyValueView() ? token.getTokenValue() : token.getValue();
        XNodeInfo userObject = new XNodeInfo(Type.NONMBEAN, label, label, token.getTokenValue());
        node.setUserObject(userObject);
        return node;
    }

    private Object createNodeValue(XMBean xmbean, Token token) {
        String label = isKeyValueView() ? token.getTokenValue() : token.getValue();
        xmbean.setText(label);
        return xmbean;
    }

    /**
     * Parses the MBean ObjectName comma-separated properties string and puts
     * the individual key/value pairs into the map. Key order in the properties
     * string is preserved by the map.
     */
    private static Map<String, String> extractKeyValuePairs(String props, ObjectName mbean) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        int eq = props.indexOf("=");
        while (eq != -1) {
            String key = props.substring(0, eq);
            String value = mbean.getKeyProperty(key);
            map.put(key, value);
            props = props.substring(key.length() + 1 + value.length());
            if (props.startsWith(",")) {
                props = props.substring(1);
            }
            eq = props.indexOf("=");
        }
        return map;
    }

    /**
     * Returns the ordered key property list that will be used to build the
     * MBean tree. If the "com.sun.tools.jconsole.mbeans.keyPropertyList" system
     * property is not specified, then the ordered key property list used
     * to build the MBean tree will be the one returned by the method
     * ObjectName.getKeyPropertyListString() with "type" as first key,
     * and "j2eeType" as second key, if present. If any of the keys specified
     * in the comma-separated key property list does not apply to the given
     * MBean then it will be discarded.
     */
    private static String getKeyPropertyListString(ObjectName mbean) {
        String props = mbean.getKeyPropertyListString();
        Map<String, String> map = extractKeyValuePairs(props, mbean);
        StringBuilder sb = new StringBuilder();
        // Add the key/value pairs to the buffer following the
        // key order defined by the "orderedKeyPropertyList"
        for (String key : orderedKeyPropertyList) {
            if (map.containsKey(key)) {
                sb.append(key + "=" + map.get(key) + ",");
                map.remove(key);
            }
        }
        // Add the remaining key/value pairs to the buffer
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue() + ",");
        }
        String orderedKeyPropertyListString = sb.toString();
        orderedKeyPropertyListString = orderedKeyPropertyListString.substring(0, orderedKeyPropertyListString.length() - 1);
        return orderedKeyPropertyListString;
    }

    // Call on EDT
    public void addMetadataNodes(DefaultMutableTreeNode node) {
        XMBean mbean = (XMBean) ((XNodeInfo) node.getUserObject()).getData();
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        MBeanInfoNodesSwingWorker sw = new MBeanInfoNodesSwingWorker(model, node, mbean);
        if (sw != null) {
            sw.execute();
        }
    }

    private static class MBeanInfoNodesSwingWorker extends SwingWorker<Object[], Void> {

        private final DefaultTreeModel model;
        private final DefaultMutableTreeNode node;
        private final XMBean mbean;

        public MBeanInfoNodesSwingWorker(DefaultTreeModel model, DefaultMutableTreeNode node, XMBean mbean) {
            this.model = model;
            this.node = node;
            this.mbean = mbean;
        }

        @Override
        public Object[] doInBackground() throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
            Object result[] = new Object[2];
            // Retrieve MBeanInfo for this MBean
            result[0] = this.mbean.getMBeanInfo();
            // Check if this MBean is a notification emitter
            result[1] = this.mbean.isBroadcaster();
            return result;
        }

        @Override
        protected void done() {
            try {
                Object result[] = get();
                MBeanInfo mbeanInfo = (MBeanInfo) result[0];
                Boolean isBroadcaster = (Boolean) result[1];
                if (mbeanInfo != null) {
                    addMBeanInfoNodes(this.model, this.node, this.mbean, mbeanInfo, isBroadcaster);
                }
            } catch (Exception e) {
                Throwable t = Utils.getActualException(e);
                if (JConsole.isDebug()) {
                    t.printStackTrace();
                }
            }
        }

        // Call on EDT
        private void addMBeanInfoNodes(DefaultTreeModel tree, DefaultMutableTreeNode node, XMBean mbean, MBeanInfo mbeanInfo, Boolean isBroadcaster) {
            MBeanAttributeInfo[] ai = mbeanInfo.getAttributes();
            MBeanOperationInfo[] oi = mbeanInfo.getOperations();
            MBeanNotificationInfo[] ni = mbeanInfo.getNotifications();

            // Insert the Attributes/Operations/Notifications metadata nodes as
            // the three first children of this MBean node. This is only useful
            // when this MBean node denotes an MBean but it's not a leaf in the
            // MBean tree
            //
            int childIndex = 0;

            // MBeanAttributeInfo node
            //
            if (ai != null && ai.length > 0) {
                DefaultMutableTreeNode attributes = new DefaultMutableTreeNode();
                XNodeInfo attributesUO = new XNodeInfo(Type.ATTRIBUTES, mbean, Resources.getText("Attributes"), null);
                attributes.setUserObject(attributesUO);
                node.insert(attributes, childIndex++);
                for (MBeanAttributeInfo mbai : ai) {
                    DefaultMutableTreeNode attribute = new DefaultMutableTreeNode();
                    XNodeInfo attributeUO = new XNodeInfo(Type.ATTRIBUTE, new Object[] { mbean, mbai }, mbai.getName(), null);
                    attribute.setUserObject(attributeUO);
                    attribute.setAllowsChildren(false);
                    attributes.add(attribute);
                }
            }
            // MBeanOperationInfo node
            //
            if (oi != null && oi.length > 0 && System.getProperty("readOnly") == null) {
                DefaultMutableTreeNode operations = new DefaultMutableTreeNode();
                XNodeInfo operationsUO = new XNodeInfo(Type.OPERATIONS, mbean, Resources.getText("Operations"), null);
                operations.setUserObject(operationsUO);
                node.insert(operations, childIndex++);
                for (MBeanOperationInfo mboi : oi) {
                    // Compute the operation's tool tip text:
                    // "operationname(param1type,param2type,...)"
                    //
                    StringBuilder sb = new StringBuilder();
                    for (MBeanParameterInfo mbpi : mboi.getSignature()) {
                        sb.append(mbpi.getType() + ",");
                    }
                    String signature = sb.toString();
                    if (signature.length() > 0) {
                        // Remove the trailing ','
                        //
                        signature = signature.substring(0, signature.length() - 1);
                    }
                    String toolTipText = mboi.getName() + "(" + signature + ")";
                    // Create operation node
                    //
                    DefaultMutableTreeNode operation = new DefaultMutableTreeNode();
                    XNodeInfo operationUO = new XNodeInfo(Type.OPERATION, new Object[] { mbean, mboi }, mboi.getName(), toolTipText);
                    operation.setUserObject(operationUO);
                    operation.setAllowsChildren(false);
                    operations.add(operation);
                }
            }
            // MBeanNotificationInfo node
            //
            if (isBroadcaster != null && isBroadcaster.booleanValue()) {
                DefaultMutableTreeNode notifications = new DefaultMutableTreeNode();
                XNodeInfo notificationsUO = new XNodeInfo(Type.NOTIFICATIONS, mbean, Resources.getText("Notifications"), null);
                notifications.setUserObject(notificationsUO);
                node.insert(notifications, childIndex++);
                if (ni != null && ni.length > 0) {
                    for (MBeanNotificationInfo mbni : ni) {
                        DefaultMutableTreeNode notification = new DefaultMutableTreeNode();
                        XNodeInfo notificationUO = new XNodeInfo(Type.NOTIFICATION, mbni, mbni.getName(), null);
                        notification.setUserObject(notificationUO);
                        notification.setAllowsChildren(false);
                        notifications.add(notification);
                    }
                }
            }
            // Update tree model
            //
            this.model.reload(node);
        }
    }

    //
    // Tree preferences
    //
    private static boolean treeView;
    private static boolean treeViewInit = false;

    private static boolean isTreeView() {
        if (!treeViewInit) {
            treeView = getTreeViewValue();
            treeViewInit = true;
        }
        return treeView;
    }

    private static boolean getTreeViewValue() {
        String tv = System.getProperty("treeView");
        return ((tv == null) ? true : !(tv.equals("false")));
    }

    //
    // MBean key-value preferences
    //
    private boolean keyValueView = Boolean.getBoolean("keyValueView");

    private boolean isKeyValueView() {
        return this.keyValueView;
    }

    //
    // Utility classes
    //
    private static class ComparableDefaultMutableTreeNode extends DefaultMutableTreeNode implements Comparable<DefaultMutableTreeNode> {

        @Override
        public int compareTo(DefaultMutableTreeNode node) {
            return (this.toString().compareTo(node.toString()));
        }
    }

    private static class Dn implements Comparable<Dn> {

        private ObjectName mbean;
        private String domain;
        private String keyPropertyList;
        private String hashDn;
        private List<Token> tokens = new ArrayList<Token>();

        public Dn(ObjectName mbean) {
            this.mbean = mbean;
            this.domain = mbean.getDomain();
            this.keyPropertyList = getKeyPropertyListString(mbean);

            if (isTreeView()) {
                // Tree view
                Map<String, String> map = extractKeyValuePairs(this.keyPropertyList, mbean);
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    this.tokens.add(new Token("key", entry.getKey() + "=" + entry.getValue()));
                }
            } else {
                // Flat view
                this.tokens.add(new Token("key", "properties=" + this.keyPropertyList));
            }

            // Add the domain as the first token in the Dn
            this.tokens.add(0, new Token("domain", "domain=" + this.domain));

            // Reverse the Dn (from leaf to root)
            Collections.reverse(this.tokens);

            // Compute hash for Dn
            computeHashDn();
        }

        public ObjectName getObjectName() {
            return this.mbean;
        }

        public String getDomain() {
            return this.domain;
        }

        public String getKeyPropertyList() {
            return this.keyPropertyList;
        }

        public Token getToken(int index) {
            return this.tokens.get(index);
        }

        public int getTokenCount() {
            return this.tokens.size();
        }

        public String getHashDn() {
            return this.hashDn;
        }

        public String getHashKey(Token token) {
            final int begin = this.hashDn.indexOf(token.getTokenValue());
            return this.hashDn.substring(begin, this.hashDn.length());
        }

        private void computeHashDn() {
            if (this.tokens.isEmpty()) {
                return;
            }
            final StringBuilder hdn = new StringBuilder();
            for (int i = 0; i < this.tokens.size(); i++) {
                hdn.append(this.tokens.get(i).getTokenValue());
                hdn.append(",");
            }
            this.hashDn = hdn.substring(0, hdn.length() - 1);
        }

        @Override
        public String toString() {
            return this.domain + ":" + this.keyPropertyList;
        }

        @Override
        public int compareTo(Dn dn) {
            return this.toString().compareTo(dn.toString());
        }
    }

    private static class Token {

        private String tokenType;
        private String tokenValue;
        private String key;
        private String value;

        public Token(String tokenType, String tokenValue) {
            this.tokenType = tokenType;
            this.tokenValue = tokenValue;
            buildKeyValue();
        }

        public String getTokenType() {
            return this.tokenType;
        }

        public String getTokenValue() {
            return this.tokenValue;
        }

        public String getKey() {
            return this.key;
        }

        public String getValue() {
            return this.value;
        }

        private void buildKeyValue() {
            int index = this.tokenValue.indexOf("=");
            if (index < 0) {
                this.key = this.tokenValue;
                this.value = this.tokenValue;
            } else {
                this.key = this.tokenValue.substring(0, index);
                this.value = this.tokenValue.substring(index + 1, this.tokenValue.length());
            }
        }
    }
}
