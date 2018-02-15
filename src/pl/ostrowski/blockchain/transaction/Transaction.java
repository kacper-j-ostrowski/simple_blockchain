package pl.ostrowski.blockchain.transaction;

import pl.ostrowski.blockchain.SimpleBlockchainMain;
import pl.ostrowski.blockchain.block.StringUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {

    public String transactionId;
    public PublicKey sender;
    public PublicKey reciepent;
    public float value;
    public byte[] signature;

    public ArrayList<TransactionInput> inputs = new ArrayList<>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0;


    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.reciepent = to;
        this.value = value;
        this.inputs = inputs;
    }

    private String calculateHash() {
        sequence++;
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(reciepent) +
                        Float.toString(value)
        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepent) + Float.toString(value);
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepent) + Float.toString(value);
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    public boolean processTransaction() {

        if(verifySignature() == false) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        for(TransactionInput i : inputs) {
            i.UTXO = SimpleBlockchainMain.UTXOs.get(i.transactionOutputId);
        }

        if(getInputValue() < SimpleBlockchainMain.minimumTransaction) {
            System.out.println("#Transaction Inputs too small: " + getInputValue());
            return false;
        }

        float leftOver = getInputValue();
        transactionId = calculateHash();
        outputs.add(new TransactionOutput(this.reciepent, value, transactionId));
        outputs.add(new TransactionOutput(this.sender, leftOver, transactionId));

        for(TransactionOutput o : outputs) {
            SimpleBlockchainMain.UTXOs.put(o.id, o);
        }

        for(TransactionInput i : inputs) {
            if(i.UTXO == null) continue;
            SimpleBlockchainMain.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    public float getInputValue() {
        float total = 0;
        for(TransactionInput i : inputs) {
            if(i.UTXO == null) continue;
            total += i.UTXO.value;
        }
        return total;
    }

    public float getOutputsValue() {
        float total = 0;
        for(TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }
}
