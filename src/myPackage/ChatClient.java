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

	String serverAddress; //서버의 주소
    Scanner in; //서버로부터 전송된 스트림을 받는 변수
    PrintWriter out; //서버에 문자열을 전송하는 함수
    JFrame frame = new JFrame("Chatter"); //채팅창
    JTextField textField = new JTextField(50); //메세지를 입력받는 공간
    JTextArea messageArea = new JTextArea(16, 50); //입력된 메세지를 보여주는 공간
    
    
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

    private String getName() { //이름을 입력받도록 하는 창이 뜨게 하는 함수
        return JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE //이름을 입력받고 입력값이 없으면 null 반환
        );
    }

    private void run() throws IOException {
        try {
        	Socket socket = new Socket(serverAddress, 59001); //소켓을 생성한다
            in = new Scanner(socket.getInputStream());//서버로부터 전송되어서 온 스트림을 읽는다
            out = new PrintWriter(socket.getOutputStream(), true);//서버에 문자열을 보낸다
            
            int i=0;
            
            while (in.hasNextLine()) { //전송되어 온 스트림이 없을 때 까지 반복한다
                String line = in.nextLine(); //전송되어온 스트림을 line에 저장한다
                if (line.startsWith("SUBMITNAME")) { //전송되어온 스트림이 SUBMITNAME으로 시작한다면
                    out.println(getName()); //getName함수(클라이언트 이름을 입력받는 함수)를 호출한다
                    
                } else if (line.startsWith("NAMEACCEPTED")) { //전송되어온 스트림이 NAMEACCEPTED으로 시작한다면
                    this.frame.setTitle("Chatter - " + line.substring(13)); //채팅창 맨 위에 Chatter-입력한 이름이 뜨도록 한다
                    textField.setEditable(true); //메세지를 입력받는 공간을 클라이언트가 쓸 수 있도록 한다
                } else if (line.startsWith("MESSAGE")) { //전송되어온 스트림이 MESSAGE로 시작한다면
                    messageArea.append(line.substring(8) + "\n"); //입력된 메시지가 채팅창에 보이게 한다
                }
            }
        } finally {
            frame.setVisible(false); //채팅창을 보이지 않게 한다
            frame.dispose(); //채팅창을 없앤다
        }
    }

    public static void main(String[] args) throws Exception {
        
        ChatClient client = new ChatClient("127.0.0.1"); //서버의 주소를 전달한다
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //채팅창의 x표시를 누르면 채팅창이 닫히게 한다
        client.frame.setVisible(true); //채팅창이 보이게 한다
        client.run(); //run함수를 실행한다
    }
}