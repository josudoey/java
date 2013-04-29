package fajoy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.UUID;
  

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.StringUtils;
import java.util.List;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import java.io.File;
import java.util.ArrayList;
public class S3put {
    public static void main(String[] args) throws IOException {
    	 String accessKey = "";
         String secretKey = "";
         String endpoint="";
         
         String configFile = System.getProperties().getProperty("user.dir")+"/s3.properties";
         System.out.print(configFile);
         Properties properties = new Properties();
         try {
             properties.load(new FileInputStream(configFile));
         } catch (FileNotFoundException ex) {
             ex.printStackTrace();
             return;
         } catch (IOException ex) {
             ex.printStackTrace();
             return;
         }
         

         accessKey=properties.getProperty("accessKey");
         secretKey=properties.getProperty("secretKey");
         endpoint=properties.getProperty("endpoint");

         System.out.println(accessKey);
         System.out.println(secretKey);
         System.out.println(endpoint);
         
         String bucketName="sample.bucket";
         
         AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
         AmazonS3 s3 = new AmazonS3Client(credentials);
         s3.setEndpoint(endpoint);
         S3ClientOptions clientOptions =new S3ClientOptions();
         clientOptions.setPathStyleAccess(true);
         s3.setS3ClientOptions(clientOptions );
         
		  // Directory path here
		  String path = "./src"; 
		  File dir = new File(path);
		  ArrayList<File> flist = getFlies(dir); 
		  for (int i = 0; i < flist.size(); i++) 
		  {
			  File f= flist.get(i);
			  String key=f.getPath();
			  System.out.format("copy %s %s\n",f.length(),key);
              InputStream in= getInputStreamFormLocal(key);
              copyToS3(in, s3, bucketName, key,f.length()) ;
		  }
		  
		  
         
    }
    public static ArrayList<File> getFlies(File dir){
    	File[] list=dir.listFiles();
    	ArrayList<File> a=new ArrayList<File>();
    	for (int i = 0; i < list.length; i++) 
		  {
		   if (list[i].isFile()) 
		   {
			   a.add(list[i]);
		   }
		   
		   if (list[i].isDirectory()) 
		   {
			   a.addAll(getFlies(list[i]));
		   }
		   
		  }
    	return a;
    }

	public static InputStream getInputStreamFormLocal(String path) throws IOException {
		return new FileInputStream(path);
	}
	public static void copyToS3(InputStream input,AmazonS3 s3,String bucketName,String key,long size) throws IOException {
		ObjectMetadata metadata=new ObjectMetadata();
		metadata.setContentLength(size);
		PutObjectRequest req=new PutObjectRequest(bucketName, key, input, metadata);
		s3.putObject(req);		
	}
}