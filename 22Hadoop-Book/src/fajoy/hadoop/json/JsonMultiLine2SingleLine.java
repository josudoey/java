package fajoy.hadoop.json;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import com.alexholmes.json.mapreduce.MultiLineJsonInputFormat;
public class JsonMultiLine2SingleLine {
    private JsonMultiLine2SingleLine() {
    }
    public static class JsonMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
        Text newValue=new Text();
        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            newValue.set(value.toString().replace("\n", ""));
            context.write(NullWritable.get(), newValue);
        }
    }
    public static void main(String... args) throws Exception {
        runJob(args[0], args[1],args[2]);
    }
    public static void runJob(String input, String output,String attr) throws Exception {
        Configuration conf = new Configuration();
        Job job = new Job(conf);
        job.setJobName("JsonMultiLine2SingleLine");
        job.setJarByClass(JsonMultiLine2SingleLine.class);

        job.setInputFormatClass(MultiLineJsonInputFormat.class);
        MultiLineJsonInputFormat.setInputJsonMember(job, attr);
        
        job.setMapperClass(JsonMapper.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(input));
        Path outPath = new Path(output);
        FileOutputFormat.setOutputPath(job, outPath);
        outPath.getFileSystem(conf).delete(outPath, true);

        job.waitForCompletion(true);
    }

}
