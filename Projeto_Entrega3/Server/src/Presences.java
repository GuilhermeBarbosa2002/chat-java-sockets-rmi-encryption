import java.security.PublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;


public class Presences {
    int time_Out;

    public Presences(int time_Out) {
        this.time_Out = time_Out;
    }

    private static Hashtable<String, IPInfo> presentIPs = new Hashtable<String, IPInfo>();
    private static int cont = 0;

    public Vector<String> getPresences(String IPAddress, String nickname, boolean rmi, PublicKey publicKey) {
        long actualTime = new Date().getTime();
        cont = cont + 1;

        //Assume-se que o IP e valido!!!!!
        synchronized (this) {
            if (presentIPs.containsKey(IPAddress)) {
                IPInfo newIp = presentIPs.get(IPAddress);
                newIp.setLastSeen(actualTime);
            } else {
                IPInfo newIP = new IPInfo(IPAddress, actualTime, nickname, rmi, publicKey);
                presentIPs.put(IPAddress, newIP);
            }
        }
        return getNicknameList();
    }

    public Vector<String> getNicknameList() {
        Vector<String> result = new Vector<String>();

        for(IPInfo element : presentIPs.values()){
            if (!element.timeOutPassed(time_Out * 1000)) {
                result.add(element.getNickname());  //enquanto os 2 minutos nao passarem, o utilizador continua a aparecer online
            }

        }
        return result;
    }

    public Vector<String> getNicknameIpRmiPublicKey() {
        Vector<String> result = new Vector<String>();
        for(IPInfo element : presentIPs.values()){
            if (!element.timeOutPassed(time_Out * 1000)) {
                //passar a publicKey para String
                String publicKeyString = Base64.getEncoder().encodeToString(element.getPublicKey().getEncoded());
                result.add(element.getNickname() + " " +element.getIP() + " " + element.getRmi() + " " + publicKeyString);  //enquanto os 2 minutos nao passarem, o utilizador continua a aparecer online
            }
        }
        return result;
    }
}

class IPInfo {
    private String nickname;  //guardar o nickname também
    private String ip;
    private long lastSeen;
    private boolean rmi;
    private PublicKey publicKey;


    public IPInfo(String ip, long lastSeen, String nickname, boolean rmi, PublicKey publicKey) {
        this.ip = ip;
        this.lastSeen = lastSeen;
        this.nickname = nickname;
        this.rmi = rmi;
        this.publicKey = publicKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getIP() {
        return this.ip;
    }

    public void setLastSeen(long time) {
        this.lastSeen = time;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean getRmi() {
        return rmi;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean timeOutPassed(int timeout) {
        boolean result = false;
        long timePassedSinceLastSeen = new Date().getTime() - this.lastSeen;
        if (timePassedSinceLastSeen >= timeout) result = true;
        return result;
    }
}

