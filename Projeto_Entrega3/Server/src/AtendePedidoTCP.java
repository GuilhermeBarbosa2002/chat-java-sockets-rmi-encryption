import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class AtendePedidoTCP extends Thread {

    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private Socket ligacao;
    private PresencesServer presencesServer;
    private boolean rmi;
    private PublicKey publicKey;

    public BufferedReader getIn() {
        return in;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Socket getLigacao() {
        return ligacao;
    }

    public void setLigacao(Socket ligacao) {
        this.ligacao = ligacao;
    }

    public AtendePedidoTCP(Socket Ligacao, PresencesServer presencesServer) throws IOException { //inicializar as componentes do AtendePedidoTCP
        this.in = new BufferedReader(new InputStreamReader(Ligacao.getInputStream()));
        this.out = new PrintWriter(Ligacao.getOutputStream(), true);
        this.ligacao = Ligacao;
        this.presencesServer = presencesServer;
    }

    public String getNickname() {
        return nickname;
    }

    public void run() {
        String pedido;
        try {
            while (ligacao.isConnected()) {
                pedido = in.readLine();  //lê o pedido
                if(pedido != null) {
                    verificarTipomensagem(pedido); //filtra o pedido por tipo
                }
            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
    }

    private void verificarTipomensagem(String mensagemPortratar) throws IOException {
        String tipoMensagem, mensagem;
        String[] sp = mensagemPortratar.split(":"); // dividir as string num array de strings
        tipoMensagem = sp[0]; //separar o cabeçalho do nickname

        int indexOfSpace = mensagemPortratar.indexOf(':');  // buscar o index do primeiro :
        mensagem = mensagemPortratar.substring(indexOfSpace + 1); // buscar a string depois do primeiro :
        System.out.println(mensagem);
        responderTipoPedido(tipoMensagem, mensagem);  //responder ao pedido do utilizador conforme o tipo de pedido feito
    }


    public void responderTipoPedido(String tipoPedido, String mensagem) throws IOException { //temos dois tipos de mensagem
        switch (tipoPedido) {
            case "SESSION_UPDATE_REQUEST":
                sessionUpdateRequest(mensagem);
                break;
            case "AGENT_POST":
                agentPost(nickname + ":" + mensagem); // agent post é para enviar a mensagem para o feed na estrutura "nickname:post"
                break;
        }
    }

    private void atualizarPresencas(){
        presencesServer.getPresences().getPresences(String.valueOf(ligacao.getInetAddress()),nickname,rmi, publicKey); //atualizamos com a publick key
        System.out.println(ligacao.getInetAddress());
    }

    //adiciona a mensagem à lista das 10 mensagens mais recentes
    private void agentPost(String mensagem) throws IOException {
        adicionarMensagensMaisRecentes(mensagem);
         atualizarPresencas(); //cada vez que ele faz um AgentPost atualizamos o tempo.
        enviarSessionUpdateParaTodosUtilizadores();
    }

    private void sessionUpdateRequest(String mensagem) throws IOException {
        String[] sp = mensagem.split(":"); // dividir as string num array de strings

        int indexOfSpace = mensagem.indexOf(':');


        this.rmi = Boolean.valueOf(sp[1]);
        //fazer a mesma cena para ir buscar a public key

        //Recebemos a string publickey
        String publicKeyString = sp[2];

        try {
            //Vamos ter que apartir de uma string, obter os bytes e posteriormente a chave publica do cliente.
            KeyFactory factory = KeyFactory.getInstance("RSA","SunRsaSign");
            byte[] decodedBytes = Base64.getDecoder().decode(publicKeyString);
            publicKey = (PublicKey) factory.generatePublic(new X509EncodedKeySpec(decodedBytes));

            System.out.println(publicKey);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }


        if (nickname == null) {
            nickname = sp[0];; //se ele nao tiver nickname, entao o nickname é igual ao que ele mandou para o servidor
        }
        atualizarPresencas(); //cada vez que ele faz um sessionUpdateRequest atualizamos o tempo.
        enviarSessionUpdateParaTodosUtilizadores(); //passo 9
    }

    private void adicionarMensagensMaisRecentes(String mensagem) {
        presencesServer.getDezMensagensMaisRecentes().add(mensagem);
    }

    private void enviarSessionUpdateParaTodosUtilizadores() {
        for (AtendePedidoTCP atendePedidoTCP : presencesServer.getListaUtilizadores()) { //vamos buscar todos os utilizadores online
            atendePedidoTCP.out.println("SESSION_UPDATE");
            atendePedidoTCP.sessionUpdate(); //para cada utilizador, enviamos um SESSION_UPDATE
        }
    }

    private void sessionUpdate() {
        int tamanhoUtilizadores = (presencesServer.getPresences().getNicknameIpRmiPublicKey().size()); //size = numero de utilizadores que existem
        out.println(Integer.toString(tamanhoUtilizadores)); //mandar o numero de utilizadores para o utilizador a receber

        for (int i = 0; i < presencesServer.getPresences().getNicknameIpRmiPublicKey().size(); i++) {
            out.println(presencesServer.getPresences().getNicknameIpRmiPublicKey().get(i)); //enviar o nickname de cada cliente
        }

        int tamanhoMensagens = presencesServer.getDezMensagensMaisRecentes().size();
        if (tamanhoMensagens <= 10) {
            out.println(Integer.toString(tamanhoMensagens));
            for (int i = 0; i < tamanhoMensagens; i++) {
                out.println(presencesServer.getDezMensagensMaisRecentes().get(i));
            }

        } else {
            out.println(Integer.toString(10));
            int inicio = presencesServer.getDezMensagensMaisRecentes().size() - 10;  //vemos a primeira mensagem a enviar
            int fim = presencesServer.getDezMensagensMaisRecentes().size();          // vemos o fim

            for (int i = inicio; i < fim; i++) {                 //for para percorrer as mais recentes
                out.println(presencesServer.getDezMensagensMaisRecentes().get(i));
            }
        }
    }

}

