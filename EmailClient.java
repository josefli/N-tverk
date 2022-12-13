import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class EmailClient {
    private static String host ="smtp.kth.se";
    private static int port = 587;

    public static void main(String[] args) throws UnknownHostException, IOException{
       // SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
       // SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);
       Socket socket = new Socket(host, port);
       PrintWriter writer = new PrintWriter(socket.getOutputStream());
     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
       try {
       
        
        String line="";
        String command ="";
        char[] buffer = new char[10000];
        int charsRead=-1;
        Scanner scanner = new Scanner(System.in);

        do {
            while((charsRead = reader.read(buffer)) != -1){
                if (charsRead < 1000) {
                    line += String.valueOf(buffer).substring(0, charsRead);
                    break;
                } else {   
                    line += String.valueOf(buffer);
                }
            }
            System.out.println(line);
            line = "";
            command = scanner.nextLine();
            System.out.println("Sending to server: " + command);
            writer.println(command);
            writer.flush();
            
        } while(!command.equals("exit"));

       } catch (SocketException e){
        socket = new Socket(host, port);
        writer = new PrintWriter(socket.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        e.printStackTrace();
       }
       



       
        

        
        
       

        



       
    }
    

}
