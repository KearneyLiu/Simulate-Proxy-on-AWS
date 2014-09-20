import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.ec2.model.*;

import static java.lang.Thread.sleep;


public class MSB {

    public static void main(String[] args) throws Exception {
        // write your code here
        //Load the Properties File with AWS Credentials

        MSB MSB = new MSB();

        Properties properties = new Properties();
        properties.load(Main.class.getResourceAsStream("/AwsCredentials.properties"));

        BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));

        //Create an Amazon EC2 Client
        AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);

        //Create Instance Request
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        //Configure Instance Request
        runInstancesRequest.withImageId("ami-1810b270")
                .withInstanceType("t1.micro")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName("p1")
                .withSecurityGroups("launch-wizard-7");

        //Launch Instance
        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);

        /* Return the Object Reference of the Instance just Launched */
        Instance instance=runInstancesResult.getReservation().getInstances().get(0);

        /* Add a Tag */
        CreateTagsRequest createTagsRequest = new CreateTagsRequest();
        createTagsRequest.withResources(instance.getInstanceId()).withTags(new Tag("Project","2.1"));
        ec2.createTags(createTagsRequest);



        /* Instance List */
        ArrayList<String> inst = new ArrayList<String>();
        inst.add(instance.getInstanceId());

        String dns = null;
        while(dns == null || dns.equals("")) {
            System.out.println("sleep 1s");
            sleep(1000);
            //update Instance state
            DescribeInstancesRequest dis =new DescribeInstancesRequest();
            dis.setInstanceIds(inst);
            DescribeInstancesResult disresult =ec2.describeInstances(dis);
            Instance in2 = disresult.getReservations().get(0).getInstances().get(0);


            dns = in2.getPublicDnsName();
        }
        dns = "http://" + dns;
       // dns = dns + "/username/";

        System.out.println("DNS: " + dns);

        //URL url = new URL(dns);

        /*BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(),"gbk"));
        String input;
        while((input = br.readLine()) != null){
            System.out.println(input);
        }
        br.close();
*/

        /* Register */
        System.out.println("sleep 60s");
        sleep(60000);
        MSB.registerURL(dns);

        /* Send rps */
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String dcDNS = br.readLine();
        String words = br.readLine();
        br.close();
        String url = dns + "/part/one/i/want/more?dns=" + dcDNS + "&testId=" + words;
        //http://<your-load-generator-instance-dns-name>/part/one/i/want/more?dns=ec2-54-165-107-16.compute.amazonaws.com&testId=Pizza

        URL urlRPS = new URL(url);

        BufferedReader br1 = new BufferedReader(new InputStreamReader(urlRPS.openStream(),"gbk"));
        String input;
        /*dont sure if sleep*/
        System.out.println("Sleep 5s");
        sleep(5000);

       /*every minute*/
        while((input = br1.readLine()) != null){

            System.out.println(input);
        }

        br1.close();



        /* Terminate*/
        System.out.println("Terminate Instance?(y/n)");

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String userResponse = bufferedReader.readLine();

        if(userResponse.toLowerCase().equals("y")){
             /* Terminate Instance */
            TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
            List<String> instances = new ArrayList<String>();
            instances.add(instance.getInstanceId());

            terminateInstancesRequest.setInstanceIds(instances);
            ec2.terminateInstances(terminateInstancesRequest);
        }



    }

    private void registerURL(String dns) throws Exception {

        URL urlRegister = new URL(dns + "/username?username=kaiyul");
        URLConnection urlConnection = urlRegister.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String input;
        while((input = br.readLine()) != null){
            System.out.println(input);
        }
        br.close();
        System.out.println("register finish");
    }

}
