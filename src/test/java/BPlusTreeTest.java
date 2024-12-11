import org.example.BPlusTree.BPlusTree;
import org.junit.Test;

public class BPlusTreeTest {
    @Test
    public void insertItemsDefault(){
        var tree = new BPlusTree();
        tree.insert(837411365,"test");
        tree.insert(837411394,"test");
        tree.insert(837411390,"test");
        tree.printTree();
    }
    @Test
    public void insertItems(){
        var tree = new BPlusTree(1,3);
        tree.insert(837411365,"test");
        tree.insert(837411394,"test");
        tree.insert(837411390,"test");
        tree.printTree();
    }
}
