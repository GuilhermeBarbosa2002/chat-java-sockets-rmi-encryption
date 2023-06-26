import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.Timer;

public class InterfaceCliente extends JFrame {
    private JLabel nicknameBtn;
    private JTextField usernameTf;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JButton sendBtn;
    private JTable table1;
    private JTable table2;
    private JScrollPane tabelaPresencas;
    private JButton verPresençasButton;
    private javax.swing.JPanel JPanel;
    private JTextField tfSendMessage;
    private JScrollPane tabelaMensagens;
    private JTable table3;
    private JTextField tfSendPrivateMessage;
    private JButton sendPrivateMessageBtn;
    private JScrollPane tabelaMensagensPrivadas;
    private JButton sendButton;
    private JTextArea taSendMessage;



    private DefaultTableModel model = new DefaultTableModel();
    private DefaultTableModel model2 = new DefaultTableModel();
    private DefaultTableModel model3 = new DefaultTableModel();

    static String SERVICE_NAME = "PrivateMessaging";
    static int DEFAULT_PORT = 2000;
    static String DEFAULT_HOST = "127.0.0.1";
    private BufferedReader in;
    private PrintWriter out;
    private Socket ligacao;
    private Scanner scanner;
    private String nickname;
    private Timer timer = new Timer();
    private boolean rmi= true;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private HashMap<String,PublicKey> listaNickPublicKey = new HashMap<>();
    private ArrayList<String> arrayList= new ArrayList<>();

    private static InterfaceCliente interfaceCliente;

    public InterfaceCliente(String enderecoIP, String porta, boolean rmi) throws IOException { //inicializar a nossa interface do cliente
        this.rmi = rmi;
        //panel representa toda a nossa interface
        setContentPane(JPanel);
        setTitle("Forum");
        setSize(700, 700);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        tfSendMessage.setUI(new HintTextFieldUI("Enviar para todos ", true));

        if (rmi == false) {
            table3.setVisible(false);
            sendPrivateMessageBtn.setVisible(false);
            tfSendPrivateMessage.setVisible(false);
            sendButton.setVisible(false);
        } else {
            table3.setModel(model3);
            model3.addColumn("Mensagens Privadas: ");
        }

        table1.setModel(model);
        model.addColumn("Mensagens"); //cabeçalho da tabela de mensagens

        table2.setModel(model2);
        model2.addColumn("Utilizadores Online:"); //cabeçalho da tabela de utilizadores online

        if (!enderecoIP.isEmpty() && !porta.isEmpty()) {
            DEFAULT_HOST = enderecoIP;
            DEFAULT_PORT = Integer.parseInt(porta);
        }

        connectBtn.addActionListener(new ActionListener() {  //botao para se conectar ao chat
            @Override
            public void actionPerformed(ActionEvent e) {
                nickname = usernameTf.getText();
                try {
                    iniciar(true);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (NoSuchAlgorithmException ex) {
                    ex.printStackTrace();
                }

                if (usernameTf.getText().isBlank()) { //erro se se tentar conectar com o campo do nickname vazio
                    JOptionPane.showMessageDialog(null, "Escolha um nickname!", "ERRO!", JOptionPane.ERROR_MESSAGE);

                } else {

                    inicarLoop(nickname, timer);
                    receberMensagem();
                }
            }
        });

        sendBtn.addActionListener(new ActionListener() { //botao para enviar mensagens para o chat
            @Override
            public void actionPerformed(ActionEvent e) {
                String mensagem = tfSendMessage.getText();
                enviarMensagem(mensagem); //enviar mensagem para o servidor
                tfSendMessage.setText(""); //mete o text field vazio depois de enviarmos a mensagem
            }
        });

        sendPrivateMessageBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int coluna = 0;
                int linha = table2.getSelectedRow();

                if (linha == -1) {
                    JOptionPane.showMessageDialog(null, "Selecione um utilizador para enviar mensagem privada!", "ERRO!", JOptionPane.ERROR_MESSAGE);
                } else {
                    separarNicknameIpRmi(linha, coluna, true); // temos de separar os argumentos dos clientes.
                    String mensagemPorTratar = model2.getValueAt(linha, coluna).toString();
                    String[] sp = mensagemPorTratar.split(" "); // dividir as string num array de strings
                    String nick = sp[0]; //nickame
                    tfSendPrivateMessage.setUI(new HintTextFieldUI("Enviar para " + nick, true));
                }
            }
        });
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int coluna = 0;
                int linha = table2.getSelectedRow();

                if (linha == -1) {
                    JOptionPane.showMessageDialog(null, "Selecione um utilizador para enviar mensagem privada!", "ERRO!", JOptionPane.ERROR_MESSAGE);
                } else {
                    separarNicknameIpRmi(linha, coluna, false); // temos de separar os argumentos dos clientes.
                    String mensagemPorTratar = model2.getValueAt(linha, coluna).toString();
                    String[] sp = mensagemPorTratar.split(" "); // dividir as string num array de strings
                    String nick = sp[0]; //nickame
                    tfSendPrivateMessage.setUI(new HintTextFieldUI("Enviar para " + nick, true));
                }

            }
        });
    }

    //vamos separar o nickname do ip e do rmi. (Esta tudo junto na tabela)
    private void separarNicknameIpRmi(int linha, int coluna, boolean secure) {
        String mensagemPorTratar = model2.getValueAt(linha, coluna).toString();
        String nickname, ip;
        boolean rmi;

        String[] sp = mensagemPorTratar.split(" "); // dividir as string num array de strings
        nickname = sp[0]; //nickame

        int indexOfSpace = mensagemPorTratar.indexOf(" ");  // buscar o index do primeiro :
        mensagemPorTratar = mensagemPorTratar.substring(indexOfSpace + 1); // buscar a string depois do primeiro :

        sp = mensagemPorTratar.split(" ");
        ip = sp[0].substring(1);
        indexOfSpace = mensagemPorTratar.indexOf(" ");
        rmi = Boolean.valueOf(mensagemPorTratar.substring(indexOfSpace + 1));

        if (nickname.equalsIgnoreCase(this.nickname)) { //ele não pode mandar mensagens para si proprio
            JOptionPane.showMessageDialog(null, "Não pode enviar mensagem para si próprio!", "ERRO!", JOptionPane.ERROR_MESSAGE);
        } else if (rmi == false) { //não podemos enviar mensagem para quem não aceita receber mensagens.
            JOptionPane.showMessageDialog(null, "O utilizador não aceita mensagens privadas!", "ERRO!", JOptionPane.ERROR_MESSAGE);
        } else {
            String mensagemEnviar = tfSendPrivateMessage.getText();
            if(secure==true) {
                enviarMensagemPrivadaSegura(nickname, ip, mensagemEnviar);
            }else{
                enviarMensagemPrivada(nickname,ip, mensagemEnviar);
            }

            System.out.println(ip);
        }
    }

    private void enviarMensagemPrivada(String nicknameParaQuemVamosEnviar, String ip, String mensagemEnviar) {
        try {
            PrivateMessaging privateMessaging = (PrivateMessaging) LocateRegistry.getRegistry(ip).lookup(SERVICE_NAME); //acedemos à interface de quem vai receber

            String clienteQueRecebeu = privateMessaging.sendMessage(nickname, mensagemEnviar);// escrever na tabela para quem vamos enviar a mensagem a enviar
            //adicionamos diretamente na tabela o que vamos enviar

            model3.addRow(new String[]{"Enviou para:" + nicknameParaQuemVamosEnviar + ": " + mensagemEnviar});
            tfSendPrivateMessage.setText("");

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
    private void enviarMensagemPrivadaSegura(String nicknameParaQuemVamosEnviar, String ip, String mensagemEnviar){
        try {
            //Criar o Digest
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(mensagemEnviar.getBytes()); //passamos os bytes do que escrevemos para o messagedigest
            byte[] digest = md.digest();
            //Creating a Cipher object
            Cipher cipher = Cipher.getInstance("RSA");
            //Initializing a Cipher object
            cipher.init(Cipher.ENCRYPT_MODE,privateKey);
            //Adding data to the cipher
            cipher.update(digest);
            //Encrypting the data
            byte[] cipherText = cipher.doFinal();

            String encodedString = Base64.getEncoder().encodeToString(cipherText); //passar o sumario enctiptado para uma string atraves do base64
            PrivateMessaging privateMessaging = (PrivateMessaging) LocateRegistry.getRegistry(ip).lookup(SERVICE_NAME); //acedemos à interface de quem vai receber
            String clienteQueRecebeu = privateMessaging.sendMessageSecure(nickname,mensagemEnviar,encodedString);// escrever na tabela para quem vamos enviar a mensagem a enviar

            model3.addRow(new String[]{"Enviou uma mensagem segura para:" + nicknameParaQuemVamosEnviar + ": " + mensagemEnviar});
            tfSendPrivateMessage.setText("");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private void inicarLoop(String nickname, Timer timer) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    //removemos para que nao tenhamos repetições quando executamos o metodo
                    model.getDataVector().removeAllElements(); //remove todos os elementos da tabela das mensagens
                    model2.getDataVector().removeAllElements(); //remove todos os elementos da tabela dos utilizadores online
                    sessionUpdateRequest(nickname, rmi, publicKey); //executa o metodo
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 120 * 1000); //repete a cada 120 segundos
    }


    public void iniciar(boolean primeiraVez) throws IOException, NoSuchAlgorithmException { //botao para iniciar as componentes todas
        InetAddress serverAdress = InetAddress.getByName(DEFAULT_HOST);
        ligacao = new Socket(serverAdress, DEFAULT_PORT);
        in = new BufferedReader(new InputStreamReader(ligacao.getInputStream()));
        out = new PrintWriter(ligacao.getOutputStream(), true);
        criarRegisto();
        if(primeiraVez==true){
            generarChaves();
        }

    }

    private void generarChaves() throws NoSuchAlgorithmException {
        //Creating KeyPair generator object
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");

        //Initializing the KeyPairGenerator
        keyPairGen.initialize(2048);  // valor é indiferente

        //Generate the pair of keys
        KeyPair pair = keyPairGen.generateKeyPair();

        //Getting the public key from the key pair
        publicKey = pair.getPublic();
        privateKey = pair.getPrivate();

        System.out.println(nickname.toUpperCase(Locale.ROOT) +"\n" + publicKey);
    }

    private void criarRegisto() throws RemoteException, MalformedURLException {
        String ip = ligacao.getInetAddress().toString().substring(1);
        try {
            LocateRegistry.createRegistry(1099);
            EnviarMensagem enviarReceberMensagens = new EnviarMensagem(model3, nickname,listaNickPublicKey);
            LocateRegistry.getRegistry("127.0.0.1", 1099).rebind(SERVICE_NAME, enviarReceberMensagens);
        }catch (Exception e){
            System.err.println("Erro");
        }

    }


    public void sessionUpdateRequest(String nickname, boolean rmi, PublicKey publicKey) throws IOException {
        //Vamos converter a chave publica para String para ser enviada pelo socket.(Vamos esperar que a string não tenha ":" ehehehe)
        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        out.println("SESSION_UPDATE_REQUEST:" + nickname + ":" + rmi + ":" + publicKeyString); //envia para o servidor o nickname do utilizador quando ele se conecta
    }


    public void enviarMensagem(String mensagemEnviar) {
        out.println("AGENT_POST:" + mensagemEnviar);  //envia para o servidor a mensagem que o utilizador pretende enviar
    }

    public void receberMensagem() { //se a mensagem recebida for session_update, remove todos os elementos da tabela das mensagens e da tabela dos utilizadores online
        new Thread(new Runnable() {
            @Override
            public void run() {
                String mensagemRecebida;

                while (ligacao.isConnected()) {
                    try {
                        mensagemRecebida = in.readLine();
                        if (mensagemRecebida.equals("SESSION_UPDATE")) {
                            model.getDataVector().removeAllElements();
                            model2.getDataVector().removeAllElements();
                            receberSessionUpdate();

                        } else if (mensagemRecebida.equals("SESSION_TIME_OUT")) {
                            in.close();
                            out.close();
                            ligacao.close();
                            timer.cancel();
                            model.getDataVector().removeAllElements();
                            model2.getDataVector().removeAllElements();
                            model.fireTableDataChanged();
                            model2.fireTableDataChanged();

                            int dialogButton = JOptionPane.showConfirmDialog(null, "Gostaria de se reconectar? ", "voce foi desconectado", JOptionPane.YES_NO_OPTION);
                            if (dialogButton == JOptionPane.YES_OPTION) {
                                iniciar(false);
                                Timer timer2 = new Timer(); // inicializamos o timer que vai fazer o SESSION_Update_request a cada 20s
                                inicarLoop(nickname, timer2);
                                receberMensagem();

                            } else {
                                dispose();
                            }

                            break;
                        } else {

                        }
                    } catch (IOException | NoSuchAlgorithmException e) {
                        System.out.println("Erro ao comunicar com o servidor ");
                    }
                }

            }
        }).start();

    }

    private void receberSessionUpdate() {
        listaNickPublicKey.clear(); //sempre que recebermos um SessionUpdate damos clear ao cenas.
        int tamanhoPresencas = 0;
        try {
            tamanhoPresencas = Integer.parseInt(in.readLine());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //percorre o array de presenças e imprime o nickname dos utilizadores online / o metodo conectar(...) retorna um array c os nicknames todos
        for (int i = 0; i < tamanhoPresencas; i++) {
            try {
                String mensagem = in.readLine(); // ler o que vêm do socket

                String[] splitMensagem = mensagem.split(" "); //dividir em array de strings
                String nick = splitMensagem[0]; // o [0] é o nickname
                String publicKeyString = splitMensagem[3]; // [3] é a publicKey

                atualizarNicknamePublicKey(nick, publicKeyString);

                model2.addRow(new String[]{splitMensagem[0] + " " + splitMensagem[1] + " " + splitMensagem[2]}); //listar as infos dos clientes

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        int tamanhoMensagens = 0;
        try {
            tamanhoMensagens = Integer.parseInt(in.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < tamanhoMensagens; i++) {
            try {
                model.addRow(new String[]{in.readLine()}); //listar na tabela cada mensagem
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        connectBtn.setEnabled(false);
    }

    private void atualizarNicknamePublicKey(String nickname, String publickey) {
        KeyFactory factory = null;
        PublicKey publicKeyy = null;
        try {
            factory = KeyFactory.getInstance("RSA", "SunRsaSign");  //algoritmo para recriar a chave publica
            byte[] decodedBytes = Base64.getDecoder().decode(publickey);            // passar de String base64 para bytes
            publicKeyy = (PublicKey) factory.generatePublic(new X509EncodedKeySpec(decodedBytes)); //construir a chave publica a partir dos bytes usando o algoritmo
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        listaNickPublicKey.put(nickname, publicKeyy);  //adicionar o nickname, e a respetiva chave publica do cliente ao hashmap
    }
}
