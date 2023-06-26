import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PrivateMessaging extends Remote{
    String sendMessage(String name, String message) throws RemoteException;
    String sendMessageSecure(String name, String message, String signature) throws RemoteException;
}
