import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Z2_Consumer {

    public static void main(String[] argv) throws Exception {

        System.out.println("Z2 CONSUMER");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Musi byc zgodne z producentem.
        String EXCHANGE_NAME = "exchange_topic";
        // channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        String queueName = channel.queueDeclare().getQueue();
        System.out.println("created queue: " + queueName);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Podaj klucze routingu (oddzielone spacja), np. 'error' lub '*.error' lub 'pl.*':");
        String line = br.readLine();
        if (line == null || line.trim().isEmpty()) {
            System.out.println("Brak kluczy, konczenie.");
            return;
        }
        String[] keys = line.trim().split("\\s+");
        for (String key : keys) {
            channel.queueBind(queueName, EXCHANGE_NAME, key);
            System.out.println("Bound to key: " + key);
        }

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received [key=" + envelope.getRoutingKey() + "]: " + message);
            }
        };

        System.out.println("Waiting for messages...");
        channel.basicConsume(queueName, true, consumer);
    }
}
