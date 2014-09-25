import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.ec2.model.*;

import static java.lang.Thread.sleep;
/**
 *  This class is for the Project 2.1
 *  Created by Kaiyu Liu (kaiyul@andrew.cmu.edu)
 *
 */

public class MSB {

    public static void main(String[] args) throws Exception {
        int LoadGenerator = 0;
        int DataCenter = 1;
        String loadGenDNS = null;
        String dataCenterDNS = null;
        String testID = "PIAZZA";
        float sum;

        MSB MSB = new MSB();

        /* Load the Properties File with AWS Credentials */
        Properties properties = new Properties();
        properties.load(Main.class.getResourceAsStream("/AwsCredentials.properties"));

        BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));

        /* Create an Amazon EC2 Client */
        AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);

        /* Launch a new Instance and return public dns address */
        loadGenDNS = MSB.launchInstance(ec2, LoadGenerator);
        /* Launch a new Data Center Instance and return public dns address */
        dataCenterDNS = MSB.launchInstance(ec2, DataCenter);

        /* Register with Andrew ID */
        System.out.println("sleep 50s");
        sleep(50000);
        MSB.registerURL(loadGenDNS);
        MSB.registerURL(dataCenterDNS);

        /* Send rps */
        MSB.sendRPS(loadGenDNS, dataCenterDNS, testID);
        sleep(30000);// wait to generate log txt file

        /* Get the sum rps by reading log */
        sum = MSB.readLog(loadGenDNS, testID);

        System.out.println("sleep 41s more to launch the second data center");
        sleep(41000);

        /* Go into the loop until the sum > 3600 */
        while(sum < 3600){
            String dataCenterDNS2 = MSB.launchInstance(ec2, DataCenter);
            System.out.println("sleep 50s to wait for registering");
            sleep(50000);
            MSB.registerURL(dataCenterDNS2);
            MSB.sendRPS(loadGenDNS, dataCenterDNS2, testID);
            System.out.println("sleep 80s to wait for reading log");
            sleep(80000);
            sum = MSB.readLog(loadGenDNS, testID);
        }


        /* Terminate all the Instance*/
        System.out.println("Terminate Instance?(y/n)");

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String userResponse = bufferedReader.readLine();

        if(userResponse.toLowerCase().equals("y")){
            /* Terminate Instance List*/
            TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
            List<String> termInstances = new ArrayList<String>();

            List<Reservation> reservations = ec2.describeInstances().getReservations();
            int reservationCount = reservations.size();

            for(int i = 0; i < reservationCount; i++) {
                List<Instance> instances = reservations.get(i).getInstances();
                int instanceCount = instances.size();
                //Print the instance IDs of every instance in the reservation.
                for(int j = 0; j < instanceCount; j++) {
                    Instance instance = instances.get(j);
                    termInstances.add(instance.getInstanceId());
                }
            }

            terminateInstancesRequest.setInstanceIds(termInstances);
            ec2.terminateInstances(terminateInstancesRequest);
        }
    }

    /*
    *  This function is used to create and launch Instance by the given type.
    *  Return the new created Instance's DNS.
    */
    private String launchInstance(AmazonEC2Client ec2, int type) throws Exception{
        /* Create Instance Request */
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
        String ImageID = null;
        if(type == 0){ //Load Generator
            ImageID = "ami-1810b270";
        }
        if(type == 1){ //Data Center
            ImageID = "ami-324ae85a";
        }

        /* Configure Instance Request */
        runInstancesRequest.withImageId(ImageID)
                .withInstanceType("m3.medium")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName("p1")
                .withSecurityGroups("launch-wizard-7");

        /* Launch Instance */
        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);

        /* Return the Object Reference of the Instance just Launched */
        Instance instance=runInstancesResult.getReservation().getInstances().get(0);

        /* Add a Tag */
        CreateTagsRequest createTagsRequest = new CreateTagsRequest();
        createTagsRequest.withResources(instance.getInstanceId()).withTags(new Tag("Project","2.1"));
        ec2.createTags(createTagsRequest);

        /* Get Instance DNS */
        ArrayList<String> inst = new ArrayList<String>();
        inst.add(instance.getInstanceId());

        String dns = null;
        while(dns == null || dns.equals("")) {
            System.out.println("wait for getting dns, so sleep 1s");
            sleep(1000);
            //update Instance state
            DescribeInstancesRequest dir =new DescribeInstancesRequest();
            dir.setInstanceIds(inst);
            DescribeInstancesResult dirResult =ec2.describeInstances(dir);
            Instance in2 = dirResult.getReservations().get(0).getInstances().get(0);
            dns = in2.getPublicDnsName();
        }
        System.out.println("Instance type: " + Integer.toString(type) + " DNS: "+ dns);
        return dns;

    }

    /*
    *  This function is used to register the instance with own andrew ID.
    */
    private void registerURL(String dns) throws Exception {
        dns = "http://" + dns;
        System.out.println("DNS URL: " + dns);
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

    /*
        *  This function is used to make the load generator send the request to the new created data center.
        */
    private void sendRPS(String loadGenDNS, String dataCenDNS, String testID) throws Exception{
        String url = "http://" + loadGenDNS + "/part/one/i/want/more?dns=" + dataCenDNS + "&testId=" + testID;

        URL urlRPS = new URL(url);
        BufferedReader br1 = new BufferedReader(new InputStreamReader(urlRPS.openStream(),"gbk"));
        String input;

        //Get the address of log/
        while((input = br1.readLine()) != null){
            System.out.println(input);
        }
        System.out.println("Begin to send rps");
    }

    /*
        *  This function is used to read the log file, and calculate the sum of requests per second.
        *  and return the sum number.
        */
    private float readLog(String loadGenDNS, String testID) throws Exception{
        ArrayList<String> rpsList = new ArrayList<String>();
        Pattern pattern = Pattern.compile("\\s.\\d.*\\d"); //to filter the number
        Matcher matcher;
        float rpsSum = 0;

        String url = "http://" + loadGenDNS + "/view-logs?name=result_kaiyul_" + testID + ".txt";
        URL urlRPS = new URL(url);
        String input;
        BufferedReader br1 = new BufferedReader(new InputStreamReader(urlRPS.openStream(),"gbk"));
        while((input = br1.readLine()) != null){
            if(input.contains("minute")){
                rpsList.clear(); //a new minute
                System.out.println("a new minute, clear all the front");
                continue;
            }
            matcher = pattern.matcher(input);
            if(matcher.find()) {
                rpsList.add(matcher.group(0).trim());
                System.out.println("get " + matcher.group(0).trim());
            }
            else{
                System.out.println(input);
            }
        }
        /* Get the sum of RPS in one minute */
        if(rpsList.size()>0) {
            for (int i = 0; i < rpsList.size(); i++) {
                rpsSum = rpsSum + Float.parseFloat(rpsList.get(i));
            }
            System.out.println("rps sum: " + Float.toString(rpsSum));
        }
        return rpsSum;
    }

}
