package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * A multithreaded chat room server. When a client connects the server requests a screen
 * name by sending the client the text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received. After a client submits a unique name, the server acknowledges
 * with "NAMEACCEPTED". Then all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name. The broadcast messages are prefixed
 * with "MESSAGE".
 *
 * This is just a teaching example so it can be enhanced in many ways, e.g., better
 * logging. Another is to accept a lot of fun commands, like Slack.
 */

public class ChatServer{

	// All client names, so we can check for duplicates upon registration.
		private static Set<String> names = new HashSet<>(); //Ŭ���̾�Ʈ�� �Է��� �ߺ����� ���� �̸��� �����Ѵ� 

		// The set of all the print writers for all the clients, used for broadcast.
		private static Set<PrintWriter> writers = new HashSet<>(); //Ŭ���̾�Ʈ���� ���ڿ��� �����ϴ� �ߺ����� ���� ������ �����Ѵ�
		
		private static HashMap<String, PrintWriter> hash = new HashMap<String, PrintWriter>(); //�̸��� �ش��ϴ� printwriter type ������ ����(whisper���� ���)
		
		static ExecutorService pool = Executors.newFixedThreadPool(500); //���� Ŭ���̾�Ʈ�� ���� ��Ƽ������
		
		public static void main(String[] args) throws Exception {
			System.out.println("The chat server is running..."); //�ܼ�â�� ������ ���ۉ�ٴ� ���� ����Ѵ�
			//ExecutorService pool = Executors.newFixedThreadPool(500); //������ 500���� �����ؼ� �����Ѵ�
			try (ServerSocket listener = new ServerSocket(59001)) { //���� ������ �����Ѵ�
				while (true) {
					pool.execute(new Handler(listener.accept(), hash)); //�����忡��  Ŭ���̾�Ʈ ���� ��û ��� �� ���
				}
			}
		}

		/**
		 * The client handler task.
		 */
		private static class Handler implements Runnable {
			private String name;//�Է��� �̸��� �����ϴ� ����
			private Socket socket; //���� ����
			private Scanner in; //���۵Ǿ��� ��Ʈ���� �޴� ����
			private PrintWriter out; //���ڿ��� �����ϴ� ����
			
			String usernm; //whisper���� ������ �̸� ���� ����
			HashMap<String, PrintWriter> hash; //�̸��� �̸��� ���� printwriter�� mapping
			/**
			 * Constructs a handler thread, squirreling away the socket. All the interesting
			 * work is done in the run method. Remember the constructor is called from the
			 * server's main method, so this has to be as short as possible.
			 */
			public Handler(Socket socket, HashMap hash) {
				this.socket = socket;
				this.hash = hash; //whisper�� ���� �߰�
			}

			/**
			 * Services this thread's client by repeatedly requesting a screen name until a
			 * unique one has been submitted, then acknowledges the name and registers the
			 * output stream for the client in a global set, then repeatedly gets inputs and
			 * broadcasts them.
			 */
			public void run() {
				
				try {
					in = new Scanner(socket.getInputStream()); //Ŭ���̾�Ʈ�� ������ ��Ʈ���� �޴´�
					out = new PrintWriter(socket.getOutputStream(), true); //Ŭ���̾�Ʈ���� ���ڿ��� ������
					
					// Keep requesting a name until we get a unique one.
					while (true) {
						out.println("SUBMITNAME"); //SUBMITNAME�� Ŭ���̾�Ʈ���� �����Ѵ�
						name = in.nextLine(); //Ŭ���̾�Ʈ���Լ� ���� ���ڿ��� �̸� ������ �����Ѵ�
						if (name == null) { //Ŭ���̾�Ʈ���Լ� �������� ������ �ݺ����� �ߴ��Ѵ�
							return;
						}
						synchronized (names) { //names�� �ٸ��κп����� ������� �ʵ��� �����Ѵ�(����ȭ)
							if (name.length() > 0 && !names.contains(name)) { //name�� ����� ���� �ְ� �̹� �Էµ� �̸��� �ƴϸ�
								names.add(name); //names hashset�� �̸��� �����Ѵ�
								hash.put(name,out); //hashmap�� �̸��� Ŭ���̾�Ʈ�� ���ڿ��� ������ �� �ִ� out�� �����Ѵ�
								break;
							}
						}
					}
					
					

					// Now that a successful name has been chosen, add the socket's print writer
					// to the set of all writers so this client can receive broadcast messages.
					// But BEFORE THAT, let everyone else know that the new person has joined!
					out.println("NAMEACCEPTED " + name); //Ŭ���̾�Ʈ���� ���޵� �̸��� �޾Ҵٰ� Ŭ���̾�Ʈ���� �˷��ش�
					for (PrintWriter writer : writers) { 
						writer.println("MESSAGE " + name + " has joined"); //ä��â�� Ŭ���̾�Ʈ �ʿ��� �Է��� �̸��� �����ߴٰ� ����Ѵ�
						
					}
					writers.add(out); //Ŭ���̾�Ʈ�κ��� �����û�� ���� ������ ������ �̿��� ��½�Ʈ���� �����Ͽ� ��°�ü�� �����
					
			
					
					// Accept messages from this client and broadcast them.
					while (true) {
						String input = in.nextLine(); //Ŭ���̾�Ʈ�� ���� ��Ʈ���� �����Ѵ�
						if (input.toLowerCase().startsWith("/quit")) { //quit���� �����Ѵٸ� �ݺ��� Ż��
							return;
						}
						
						//whisper
						if (input.toLowerCase().startsWith("<")) { //<�� �����Ѵٸ� whisper�� ���� �ִ�
							usernm = input.substring(input.indexOf("<")+1, input.indexOf("/")); //whisper ����� �̸��� �����Ѵ�
							String msgs =  input.substring(input.indexOf(">")+1); //whisper�� �޼����� �����Ѵ�
							if(names.contains(usernm)){ //������ �Էµ� �̸� ����Ʈ�� whisper ����� �̸��� ����Ǿ��ִٸ�
								PrintWriter whisper = hash.get(usernm); //whisper ����� �̸��� �ش��ϴ� printwriter�� ��������
								whisper.println("MESSAGE " + name + ": " + msgs); //������ ���ڸ� �����Ѵ�
							}
						}
						else{
							for (PrintWriter writer : writers) { //��� ��°�ü�� ���� broadcasting �Ѵ�
								writer.println("MESSAGE " + name + ": " + input); //�޼��� ���� �̸��� ���� ������ Ŭ���̾�Ʈ����
							}
						}
						
						
					}
				} catch (Exception e) {
					System.out.println(e);
				} finally {
					if (out != null) {
						writers.remove(out); //��°�ü���� �����Ѵ�
					}
					if (name != null) { 
						System.out.println(name + " is leaving");
						names.remove(name); //�̸�����set���� �����Ѵ�
						for (PrintWriter writer : writers) {
							writer.println("MESSAGE " + name + " has left"); //��ο��� ��������� �ִٴ� ���� ä��â�� ����Ѵ�
						}
					}
					try { socket.close(); } catch (IOException e) {}
				}
			}
		}
	}