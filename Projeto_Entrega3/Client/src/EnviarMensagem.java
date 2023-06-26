import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.table.DefaultTableModel;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

public class EnviarMensagem extends UnicastRemoteObject implements PrivateMessaging {
    private DefaultTableModel tableModel;
    private String nickname;
    private HashMap<String,PublicKey> listaNickPublicKey = new HashMap<>();


    public EnviarMensagem(DefaultTableModel defaultTableModel, String nickname,HashMap<String,PublicKey> listaNickPublicKey) throws RemoteException {
        super();
        this.tableModel = defaultTableModel;
        this.nickname = nickname;
        this.listaNickPublicKey=listaNickPublicKey;
    }

    @Override
    public String sendMessage(String name, String message) throws RemoteException {
        tableModel.addRow(new String[]{name + ": " + message});
        return nickname; // temos de retornar quem recebe a mensagem
    }

    public String sendMessageSecure(String name, String message, String signature) throws RemoteException {
        //Temos o name, temos de arranjar forma de ir buscar a key
        PublicKey publicKey = listaNickPublicKey.get(name); // para acedermos à chave publica do gajo que enviou o nome :

        //decifrar o sumario que recebemos //Vamos receber a assinatura em base64 -> passamos paa bytes -> deciframos com a chave publica -> E obtemos o sumario
        byte[] decodedBytes = Base64.getDecoder().decode(signature);
        Cipher cipher = null;
        byte[] decipheredDigest = new byte[0];
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE,publicKey);
            //Decrypting the text
            decipheredDigest = cipher.doFinal(decodedBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        // temos de criar o sumario por nos e comparar//Vamos receber a mensagem e vamos fazer o sumario dela
        MessageDigest md = null;
        byte[] digest = new byte[0];
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(message.getBytes()); //passamos os bytes do que escrevemos para o messagedigest
            digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        //Comparamos os sumarios para verificar a integridade da mensagem
        if (Arrays.equals(decipheredDigest, digest)) {
            System.out.println(true);  //não houve alteração nenhuma
            tableModel.addRow(new String[]{"(Secure) "+ name + ": " + message});
        } else {
            System.out.println(false); //houve alteração
        }

        return nickname;
    };

}
