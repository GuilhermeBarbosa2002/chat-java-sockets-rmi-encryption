import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class PresencesServer {
    private static Presences presences = new Presences(120); //timeout de 2 minutos
    private static ArrayList<AtendePedidoTCP> listaUtilizadores = new ArrayList<>();
    private static ArrayList<String> dezMensagensMaisRecentes = new ArrayList<>();

    static int DEFAULT_PORT = 2000;

    public ArrayList<AtendePedidoTCP> getListaUtilizadores() {
        return listaUtilizadores;
    }

    public void setListaUtilizadores(ArrayList<AtendePedidoTCP> listaUtilizadores) {
        this.listaUtilizadores = listaUtilizadores;
    }

    public ArrayList<String> getDezMensagensMaisRecentes() {
        return dezMensagensMaisRecentes;
    }

    public void setDezMensagensMaisRecentes(ArrayList<String> dezMensagensMaisRecentes) {
        this.dezMensagensMaisRecentes = dezMensagensMaisRecentes;
    }

    public PresencesServer(Presences presences) {
        this.presences = presences;
    }

    public Presences getPresences() {
        return presences;
    }

    public void setPresences(Presences presences) {
        this.presences = presences;
    }

    public static void iniciar() throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Qual é a porta que deseja operar?");
        DEFAULT_PORT = scanner.nextInt();

        System.out.println("Qual é o valor do parametro SESSION_TIMEOUT que deseja?");
        int timeOut = scanner.nextInt();

        ServerSocket servidor = null;

        Presences presences = new Presences(timeOut);

        PresencesServer presencesServer = new PresencesServer(presences);

        servidor = new ServerSocket(DEFAULT_PORT);
        System.out.println("Servidor a' espera de ligacoes no porto " + DEFAULT_PORT);
        iniciarLoop();


        while (true) {
            try {
                Socket ligacao = servidor.accept();
                AtendePedidoTCP atendePedidoTCP = new AtendePedidoTCP(ligacao, presencesServer);
                atendePedidoTCP.start();
                listaUtilizadores.add(atendePedidoTCP);//adiciona um novo utilizador à lista de atendePedidoTCP

            } catch (IOException e) {
                System.out.println("Erro na execucao do servidor: " + e);
                System.exit(1);
            }

        }
    }

    private static void iniciarLoop() {
        Timer timer =new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sessionTimeOut();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }, 1*1000, 10 * 1000); //repete-se a cada segundo
    }

    public static void sessionTimeOut() throws IOException {
        Vector<String> utilizadoresAtivos = presences.getNicknameList();
        ArrayList<AtendePedidoTCP> utilizadoresInativos = new ArrayList<>();

        for (AtendePedidoTCP atendePedidoTCP : listaUtilizadores) {
            if (!utilizadoresAtivos.contains(atendePedidoTCP.getNickname())) { //utilizadores que  nao estiverem na lista dos ativos, mas estiverem na listaUtilizadores
                utilizadoresInativos.add(atendePedidoTCP);   //adicionamos à lista de utilizadores inativos
            }
        }
        for (AtendePedidoTCP a : utilizadoresInativos) {
            a.getOut().println("SESSION_TIME_OUT");
            a.getIn().close();
            a.getOut().close();
            a.getLigacao().close();
            listaUtilizadores.remove(a); //removemos da lista de todos os utilizadores
        }
    }

    public static void main(String[] args) {
        try {
            iniciar();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}