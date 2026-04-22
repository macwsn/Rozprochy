import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Z2_Producer {

    public static void main(String[] argv) throws Exception {

        System.out.println("Z2 PRODUCER");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Aby przetestowac Direct -> "exchange_direct", Topic -> "exchange_topic".
        // Raz zadeklarowanego exchange nie mozna zmienic typu; zmien nazwe lub usun stary.
        String EXCHANGE_NAME = "exchange_topic";
        // channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.println("Enter routing key: ");
            String key = br.readLine();
            if (key == null || "exit".equals(key)) break;

            System.out.println("Enter message: ");
            String message = br.readLine();
            if (message == null || "exit".equals(message)) break;

            channel.basicPublish(EXCHANGE_NAME, key, null, message.getBytes("UTF-8"));
            System.out.println("Sent [key=" + key + "]: " + message);
        }

        channel.close();
        connection.close();
    }
}
