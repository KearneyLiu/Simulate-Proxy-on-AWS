import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by liukaiyu on 9/19/14.
 */
public class test {

    public static void main(String[] args) throws IOException, InterruptedException {
        String x = "Please go <a href='/view-logs?name=result_kaiyul_pizza.txt'>here</a> to view  the logs";

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String dcDNS = br.readLine();
        String txt = br.readLine();

        System.out.println(dcDNS + " " + txt);
    }
}
