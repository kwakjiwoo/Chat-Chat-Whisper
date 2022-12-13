package myPackage;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server. Graphically it is a frame with a text
 * field for entering messages and a textarea to see the whole dialog.
 *
 * The client follows the following Chat Protocol. When the server sends "SUBMITNAME" the
 * client replies with the desired screen name. The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are already in use. When the
 * server sends a line beginning with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all chatters connected to the
 * server. When the server sends a line beginning with "MESSAGE" then all characters
 * following this string should be displayed in its message area.
 */

public class ChatClient{

	String serverAddress; //������ �ּ�
    Scanner in; //�����κ��� ���۵� ��Ʈ���� �޴� ����
    PrintWriter out; //������ ���ڿ��� �����ϴ� �Լ�
    JFrame frame = new JFrame("Chatter"); //ä��â
    JTextField textField = new JTextField(50); //�޼����� �Է¹޴� ����
    JTextArea messageArea = new JTextArea(16, 50); //�Էµ� �޼����� �����ִ� ����
    
    
    /**
     * Constructs the client by laying out the GUI and registering a listener with the
     * textfield so that pressing Return in the listener sends the textfield contents
     * to the server. Note however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED message from
     * the server.
     */
    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        }
        
       );
    }

    private String getName() { //�̸��� �Է¹޵��� �ϴ� â�� �߰� �ϴ� �Լ�
        return JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE //�̸��� �Է¹ް� �Է°��� ������ null ��ȯ
        );
    }

    private void run() throws IOException {
        try {
        	Socket socket = new Socket(serverAddress, 59001); //������ �����Ѵ�
            in = new Scanner(socket.getInputStream());//�����κ��� ���۵Ǿ �� ��Ʈ���� �д´�
            out = new PrintWriter(socket.getOutputStream(), true);//������ ���ڿ��� ������
            
            int i=0;
            
            while (in.hasNextLine()) { //���۵Ǿ� �� ��Ʈ���� ���� �� ���� �ݺ��Ѵ�
                String line = in.nextLine(); //���۵Ǿ�� ��Ʈ���� line�� �����Ѵ�
                if (line.startsWith("SUBMITNAME")) { //���۵Ǿ�� ��Ʈ���� SUBMITNAME���� �����Ѵٸ�
                    out.println(getName()); //getName�Լ�(Ŭ���̾�Ʈ �̸��� �Է¹޴� �Լ�)�� ȣ���Ѵ�
                    
                } else if (line.startsWith("NAMEACCEPTED")) { //���۵Ǿ�� ��Ʈ���� NAMEACCEPTED���� �����Ѵٸ�
                    this.frame.setTitle("Chatter - " + line.substring(13)); //ä��â �� ���� Chatter-�Է��� �̸��� �ߵ��� �Ѵ�
                    textField.setEditable(true); //�޼����� �Է¹޴� ������ Ŭ���̾�Ʈ�� �� �� �ֵ��� �Ѵ�
                } else if (line.startsWith("MESSAGE")) { //���۵Ǿ�� ��Ʈ���� MESSAGE�� �����Ѵٸ�
                    messageArea.append(line.substring(8) + "\n"); //�Էµ� �޽����� ä��â�� ���̰� �Ѵ�
                }
            }
        } finally {
            frame.setVisible(false); //ä��â�� ������ �ʰ� �Ѵ�
            frame.dispose(); //ä��â�� ���ش�
        }
    }

    public static void main(String[] args) throws Exception {
        
        ChatClient client = new ChatClient("127.0.0.1"); //������ �ּҸ� �����Ѵ�
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //ä��â�� xǥ�ø� ������ ä��â�� ������ �Ѵ�
        client.frame.setVisible(true); //ä��â�� ���̰� �Ѵ�
        client.run(); //run�Լ��� �����Ѵ�
    }
}