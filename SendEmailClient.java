import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
public class SendEmailClient {
    private static String host = "smtp.kth.se";
    private static int port = 587;
    private static char[] buffer = new char[10000];
    private static String line;
    private static String command;
    private static int charsRead=-1;
    private static SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    private static Scanner scanner = new Scanner(System.in);    

    public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException{
        Socket socket = new Socket(host, port);
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // Setup: starting TLS connection

        System.out.println("\n\n-- Starting SMTP -- \n\n\tTo exit, type q\n\n");
        line = "";
        System.out.println(readFromBuffer(reader));
        line = "";
        toServer(writer, "ehlo smtp.kth.se");
        System.out.println(readFromBuffer(reader));
        line = "";
        
        toServer(writer, "starttls");
        if((line = readFromBuffer(reader)).contains("Ready to start TLS")){
            socket = (SSLSocket) sslSocketFactory.createSocket(socket, host, port, true); 
            writer = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        System.out.println(line);
        toServer(writer, "ehlo smtp.kth.se");
        System.out.println(readFromBuffer(reader));
        
        // TSL started
        writeFromCommandLine(writer);
        do {
           System.out.println(readFromBuffer(reader));
           line = "";
           writeFromCommandLine(writer);
           
        } while(!command.equals("q"));  
    }
    
    private static String readFromBuffer(BufferedReader reader) throws IOException{
        while((charsRead = reader.read(buffer)) != -1){
            if (charsRead < 1000) {
                line += String.valueOf(buffer).substring(0, charsRead);
                break;
            } else {   
                line += String.valueOf(buffer);
            }
        }
        return line;
    }

    private static void toServer(PrintWriter writer, String msg){
        writer.print(msg + "\r\n");
        writer.flush();
        // System.out.println(msg + "\n");
    }

    private static void writeFromCommandLine(PrintWriter writer){
        command = scanner.nextLine();
        if(isQ(command)){
            quit();
        }
        toServer(writer, command);
    }

    private static boolean isQ(String s){
        return s.equals("q");
    }

    private static void quit(){
        System.out.println("Exiting program");
        System.exit(0);
    }
}
