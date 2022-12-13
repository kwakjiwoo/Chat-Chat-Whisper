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
		private static Set<String> names = new HashSet<>(); //클라이언트가 입력한 중복되지 않은 이름을 저장한다 

		// The set of all the print writers for all the clients, used for broadcast.
		private static Set<PrintWriter> writers = new HashSet<>(); //클라이언트에게 문자열을 전송하는 중복되지 않은 변수를 저장한다
		
		private static HashMap<String, PrintWriter> hash = new HashMap<String, PrintWriter>(); //이름에 해당하는 printwriter type 변수를 저장(whisper에서 사용)
		
		static ExecutorService pool = Executors.newFixedThreadPool(500); //여러 클라이언트를 위한 멀티스레드
		
		public static void main(String[] args) throws Exception {
			System.out.println("The chat server is running..."); //콘솔창에 서버가 시작됬다는 것을 출력한다
			//ExecutorService pool = Executors.newFixedThreadPool(500); //스레드 500개를 생성해서 재사용한다
			try (ServerSocket listener = new ServerSocket(59001)) { //서버 소켓을 생성한다
				while (true) {
					pool.execute(new Handler(listener.accept(), hash)); //스레드에서  클라이언트 접속 요청 대기 및 허락
				}
			}
		}

		/**
		 * The client handler task.
		 */
		private static class Handler implements Runnable {
			private String name;//입력한 이름을 저장하는 변수
			private Socket socket; //소켓 변수
			private Scanner in; //전송되어진 스트림을 받는 변수
			private PrintWriter out; //문자열을 전송하는 변수
			
			String usernm; //whisper에서 전송할 이름 저장 변수
			HashMap<String, PrintWriter> hash; //이름과 이름에 보낼 printwriter를 mapping
			/**
			 * Constructs a handler thread, squirreling away the socket. All the interesting
			 * work is done in the run method. Remember the constructor is called from the
			 * server's main method, so this has to be as short as possible.
			 */
			public Handler(Socket socket, HashMap hash) {
				this.socket = socket;
				this.hash = hash; //whisper를 위해 추가
			}

			/**
			 * Services this thread's client by repeatedly requesting a screen name until a
			 * unique one has been submitted, then acknowledges the name and registers the
			 * output stream for the client in a global set, then repeatedly gets inputs and
			 * broadcasts them.
			 */
			public void run() {
				
				try {
					in = new Scanner(socket.getInputStream()); //클라이언트가 전송한 스트림을 받는다
					out = new PrintWriter(socket.getOutputStream(), true); //클라이언트에게 문자열을 보낸다
					
					// Keep requesting a name until we get a unique one.
					while (true) {
						out.println("SUBMITNAME"); //SUBMITNAME을 클라이언트에게 전송한다
						name = in.nextLine(); //클라이언트에게서 받은 문자열을 이름 변수에 저장한다
						if (name == null) { //클라이언트에게서 받은것이 없으면 반복문을 중단한다
							return;
						}
						synchronized (names) { //names가 다른부분에서는 실행되지 않도록 점유한다(동기화)
							if (name.length() > 0 && !names.contains(name)) { //name에 저장된 값이 있고 이미 입력된 이름이 아니면
								names.add(name); //names hashset에 이름을 저장한다
								hash.put(name,out); //hashmap에 이름과 클라이언트로 문자열을 전소할 수 있는 out을 저장한다
								break;
							}
						}
					}
					
					

					// Now that a successful name has been chosen, add the socket's print writer
					// to the set of all writers so this client can receive broadcast messages.
					// But BEFORE THAT, let everyone else know that the new person has joined!
					out.println("NAMEACCEPTED " + name); //클라이언트에서 전달된 이름을 받았다고 클라이언트에게 알려준다
					for (PrintWriter writer : writers) { 
						writer.println("MESSAGE " + name + " has joined"); //채팅창에 클라이언트 쪽에서 입력한 이름이 참가했다고 출력한다
						
					}
					writers.add(out); //클라이언트로부터 연결요청에 의해 생성된 소켓을 이용해 출력스트림을 생성하여 출력객체를 만든다
					
			
					
					// Accept messages from this client and broadcast them.
					while (true) {
						String input = in.nextLine(); //클라이언트로 받은 스트림을 저장한다
						if (input.toLowerCase().startsWith("/quit")) { //quit으로 시작한다면 반복문 탈출
							return;
						}
						
						//whisper
						if (input.toLowerCase().startsWith("<")) { //<로 시작한다면 whisper일 수도 있다
							usernm = input.substring(input.indexOf("<")+1, input.indexOf("/")); //whisper 대상의 이름만 추출한다
							String msgs =  input.substring(input.indexOf(">")+1); //whisper의 메세지만 추출한다
							if(names.contains(usernm)){ //이전에 입력된 이름 리스트에 whisper 대상의 이름이 저장되어있다면
								PrintWriter whisper = hash.get(usernm); //whisper 대상의 이름에 해당하는 printwriter를 가져오고
								whisper.println("MESSAGE " + name + ": " + msgs); //추출한 문자만 전송한다
							}
						}
						else{
							for (PrintWriter writer : writers) { //모든 출력객체를 통해 broadcasting 한다
								writer.println("MESSAGE " + name + ": " + input); //메세지 보낸 이름과 문자 내용을 클라이언트에게
							}
						}
						
						
					}
				} catch (Exception e) {
					System.out.println(e);
				} finally {
					if (out != null) {
						writers.remove(out); //출력객체에서 삭제한다
					}
					if (name != null) { 
						System.out.println(name + " is leaving");
						names.remove(name); //이름저장set에서 삭제한다
						for (PrintWriter writer : writers) {
							writer.println("MESSAGE " + name + " has left"); //모두에게 나간사람이 있다는 것을 채팅창에 출력한다
						}
					}
					try { socket.close(); } catch (IOException e) {}
				}
			}
		}
	}