import com.sun.jdi.InvalidTypeException;
import org.example.BlockChain.BlockChain;
import org.example.Transaction.Transaction;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class BlockChainTest {
    @Test
    public void  testInit() throws InvalidTypeException, NoSuchAlgorithmException {
        var chain = new BlockChain();
        var transactions = new ArrayList<Transaction>();
        for(int i=0;i<3;i++){
            transactions.add(new Transaction("aylon","daniel",0.01,System.currentTimeMillis()));
        }
        for(Transaction transaction:transactions){
            chain.addTransaction(transaction);
        }
        System.out.println(chain);
    }
}
