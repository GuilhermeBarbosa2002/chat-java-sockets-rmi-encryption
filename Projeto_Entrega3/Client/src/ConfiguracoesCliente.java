import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ConfiguracoesCliente extends JFrame{
    private JButton button1;
    private JPanel panel1;
    private JTextField tfPorta;
    private JTextField tfEndereco;
    private JCheckBox checkBox1;

    public ConfiguracoesCliente() {
        setTitle("Configurações do Cliente");
        setSize(700, 300);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(panel1);

        tfPorta.setUI(new HintTextFieldUI("Porta", true));
        tfEndereco.setUI(new HintTextFieldUI("Endereço", true));


        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {

                    String enderecoIP = tfEndereco.getText();
                    String porta = tfPorta.getText();
                    InterfaceCliente interfaceCliente = new InterfaceCliente(enderecoIP, porta, checkBox1.isSelected());
                    dispose();

                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }
        });
    }

    public static void main(String[] args) {
        ConfiguracoesCliente configuracoesCliente = new ConfiguracoesCliente();
        configuracoesCliente.setVisible(true);
    }
}

