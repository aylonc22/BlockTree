import com.sun.jdi.InvalidTypeException;
import org.example.BPlusTree.BPlusTree;
import org.example.BPlusTree.BPlusTreeNode;
import org.junit.Test;

public class BPlusTreeTest {
    @Test
    public void insertItemsDefault() throws InvalidTypeException {
        BPlusTree<Integer> tree = new BPlusTree<>(Integer.class);
        tree.insert(837411365,"test");
        tree.insert(837411394,"test");
        tree.insert(837411390,"test");
        tree.printTree();
    }
    @Test
    public void insertItems() throws InvalidTypeException {
        BPlusTree<Integer> tree = new BPlusTree<>(1,3,Integer.class);
        tree.insert(837411365,"test");
        tree.insert(837411394,"test");
        tree.insert(837411390,"test");
        tree.printTree();
    }
    @Test
    public void insertItemsMany() throws InvalidTypeException {
        BPlusTree<Integer> tree = new BPlusTree<>(1,3,Integer.class);
        tree.insert(837411365,"test");
        tree.insert(837411394,"test");
        tree.insert(837411390,"test");
        tree.insert(837411391,"test");
        tree.insert(837411392,"test");
        tree.insert(837411393,"test");
        tree.printTree();
    }
    @Test
    public void insertItemsString() throws InvalidTypeException {
        BPlusTree<String> tree = new BPlusTree<>(1,3,String.class);
        tree.insert("837411365","test");
        tree.insert("837411394","test");
        tree.insert("837411390","test");
        tree.printTree();
    }
    @Test
    public void insertItemsManyString() throws InvalidTypeException {
        BPlusTree<String> tree = new BPlusTree<>(1,3,String.class);
        tree.insert("837411365","test");
        tree.insert("837411394","test");
        tree.insert("837411390","test");
        tree.insert("837411391","test");
        tree.insert("837411392","test");
        tree.insert("837411393","test");
        tree.printTree();
    }
}
