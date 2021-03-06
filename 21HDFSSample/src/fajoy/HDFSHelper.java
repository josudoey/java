package fajoy;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IOUtils;


import org.apache.hadoop.fs.*;


import java.util.*;
import java.io.FileNotFoundException;

import java.text.SimpleDateFormat;

public class HDFSHelper {
	public static final SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	public HDFSHelper() {

	}

	public static Configuration getConf() {
		return new Configuration();
	}

	public static void main(String[] args) throws Exception {
		FileSystem fs = FileSystem.get(getConf());
		Path path=new Path("./");
		HDFSAction ls =new HDFSAction() {
			@Override
			public void actionDir(FileSystem fs, Path path, FileStatus fileStatus) {
				System.out.format("%s %10s\t%s\n",fileStatus.getAccessTime(),fileStatus.getLen(),path);
			};
			
			
			@Override
			
			public void actionFile(FileSystem fs, Path path,FileStatus fileStatus) {

				System.out.format("%s %10s\t%s\n",fileStatus.getAccessTime(),fileStatus.getLen(),path);
			}
		};
		ls.run(fs, path, true);
		System.exit(0);
	}

	public static void mkdir(FileSystem fs, String dir) throws IOException {
		Path path = new Path(dir);
		if (fs.exists(path)) {
			System.err.println("Dir " + dir + " already exists!");
			return;
		}
		fs.mkdirs(path);
	}

	public void delete(String srcf, final boolean recursive,final boolean skipTrash) throws IOException {
		Path srcPattern = new Path(srcf);
		new DelayedExceptionThrowing() {
			@Override
			void process(Path p, FileSystem srcFs) throws IOException {
				delete(p, srcFs, recursive, skipTrash);
			}
		}.globAndProcess(srcPattern, srcPattern.getFileSystem(getConf()));
	}
	
	  /* delete a file */
	public void delete(Path src, FileSystem srcFs, boolean recursive,boolean skipTrash) throws IOException {
	    FileStatus fs = null;
	    try {
	      fs = srcFs.getFileStatus(src);
	    } catch (FileNotFoundException fnfe) {
	      // Have to re-throw so that console output is as expected
	      throw new FileNotFoundException("cannot remove " + src + ": No such file or directory.");
	    }

	    if (fs.isDir() && !recursive) {
	      throw new IOException("Cannot remove directory \"" + src +"\", use -rmr instead");
	    }

	    if(!skipTrash) {
	      try {
	          Trash trashTmp = new Trash(srcFs, getConf());
	        if (trashTmp.moveToTrash(src)) {
	          System.out.println("Moved to trash: " + src);
	          return;
	        }
	      } catch (IOException e) {
	        Exception cause = (Exception) e.getCause();
	        String msg = "";
	        if(cause != null) {
	          msg = cause.getLocalizedMessage();
	        }
	        System.err.println("Problem with Trash." + msg +". Consider using -skipTrash option");
	        throw e;
	      }
	    }

	    if (srcFs.delete(src, true)) {
	      System.out.println("Deleted " + src);
	    } else {
	      throw new IOException("Delete failed " + src);
	    }
	  }


	public void list(String srcf, boolean recursive) throws IOException {
		Path srcPath = new Path(srcf);
		FileSystem srcFs = srcPath.getFileSystem(this.getConf());
		FileStatus[] srcs = srcFs.globStatus(srcPath);
		if (srcs == null || srcs.length == 0) {
			return;
		}
		for (int i = 0; i < srcs.length; i++) {
			list(srcs[i], srcFs, recursive);
		}
	}

	public void list(FileStatus srcs, FileSystem srcFs, boolean recursive)	throws IOException {
		Path path = srcs.getPath();
		System.out.println(srcs.getLen() + "\t" + path);
		if (recursive && srcs.isDir()) {
			FileStatus[] stats = srcFs.listStatus(path);
			for (int i = 0; i < stats.length; i++) {
				list(stats[i], srcFs, recursive);
			}
		}
	}

	public void ls(String srcf, boolean recursive) throws IOException {
		Path srcPath = new Path(srcf);
		FileSystem srcFs = srcPath.getFileSystem(this.getConf());
		FileStatus[] srcs = srcFs.globStatus(srcPath);
		if (srcs == null || srcs.length == 0) {
			throw new FileNotFoundException("Cannot access " + srcf	+ ": No such file or directory.");
		}

		boolean printHeader = (srcs.length == 1) ? true : false;
		int numOfErrors = 0;
		for (int i = 0; i < srcs.length; i++) {
			numOfErrors += ls(srcs[i], srcFs, recursive, printHeader);
		}
	}

	private int ls(FileStatus src, FileSystem srcFs, boolean recursive,	boolean printHeader) throws IOException {
		final String cmd = recursive ? "lsr" : "ls";
		final FileStatus[] items = shellListStatus(cmd, srcFs, src);
		if (items == null) {
			return 1;
		} else {
			int numOfErrors = 0;
			if (!recursive && printHeader) {
				if (items.length != 0) {
					System.out.println("Found " + items.length + " items");
				}
			}

			int maxReplication = 3, maxLen = 10, maxOwner = 0, maxGroup = 0;

			for (int i = 0; i < items.length; i++) {
				FileStatus stat = items[i];
				int replication = String.valueOf(stat.getReplication())	.length();
				int len = String.valueOf(stat.getLen()).length();
				int owner = String.valueOf(stat.getOwner()).length();
				int group = String.valueOf(stat.getGroup()).length();

				if (replication > maxReplication)
					maxReplication = replication;
				if (len > maxLen)
					maxLen = len;
				if (owner > maxOwner)
					maxOwner = owner;
				if (group > maxGroup)
					maxGroup = group;
			}

			for (int i = 0; i < items.length; i++) {
				FileStatus stat = items[i];
				Path cur = stat.getPath();
				String mdate = dateForm.format(new Date(stat.getModificationTime()));

				System.out.print((stat.isDir() ? "d" : "-")	+ stat.getPermission() + " ");
				System.out.printf("%" + maxReplication + "s ",(!stat.isDir() ? stat.getReplication() : "-"));
				if (maxOwner > 0)
					System.out.printf("%-" + maxOwner + "s ", stat.getOwner());
				if (maxGroup > 0)
					System.out.printf("%-" + maxGroup + "s ", stat.getGroup());
				System.out.printf("%" + maxLen + "d ", stat.getLen());
				System.out.print(mdate + " ");
				System.out.println(cur.toUri().getPath());
				if (recursive && stat.isDir()) {
					numOfErrors += ls(stat, srcFs, recursive, printHeader);
				}
			}
			return numOfErrors;
		}
	}

	private static FileStatus[] shellListStatus(String cmd, FileSystem srcFs,FileStatus src) {
		if (!src.isDir()) {
			FileStatus[] files = { src };
			return files;
		}
		Path path = src.getPath();
		try {
			FileStatus[] files = srcFs.listStatus(path);
			if (files == null) {
				System.err.println(cmd + ": could not get listing for '" + path	+ "'");
			}
			return files;
		} catch (IOException e) {
			System.err.println(cmd + ": could not get get listing for '" + path	+ "' : " + e.getMessage().split("\n")[0]);
		}
		return null;
	}

	public void cat(final String src, boolean verifyChecksum) throws IOException {
		Path srcPattern = new Path(src);
		new DelayedExceptionThrowing() {
			@Override
			void process(Path p, FileSystem srcFs) throws IOException {
				printToStdout(srcFs.open(p));
			}
		}.globAndProcess(srcPattern,getSrcFileSystem(srcPattern, verifyChecksum));
	}

	
	private void printToStdout(InputStream in) throws IOException {
		try {
			IOUtils.copyBytes(in, System.out, getConf(), false);
		} finally {
			in.close();
		}
	}

	public static  void copyToHDFS(InputStream in,FileSystem fs, String path) throws IOException {
		Path dst = new Path(path);
		if (fs.exists(dst)) {
			throw new IOException("Target " + dst.toString()+ " already exists.");
		}
		FSDataOutputStream out = fs.create(dst);
		try {
			IOUtils.copyBytes(in, out, getConf(), false);
		} finally {
			out.close();
		}
	}

	/*
	 * private void createSampleFile() throws IOException { File file =
	 * File.createTempFile("aws-java-sdk-", ".txt"); file.deleteOnExit();
	 * 
	 * FSDataOutputStream out = dstFs.create(dst); Writer writer = new
	 * OutputStreamWriter(new FileOutputStream(file));
	 * writer.write("abcdefghijklmnopqrstuvwxyz\n");
	 * writer.write("01234567890112345678901234\n");
	 * writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
	 * writer.write("01234567890112345678901234\n");
	 * writer.write("abcdefghijklmnopqrstuvwxyz\n"); writer.close();
	 * 
	 * }
	 */

	/**
	 * Return the {@link FileSystem} specified by src and the conf. It the
	 * {@link FileSystem} supports checksum, set verifyChecksum.
	 */
	private FileSystem getSrcFileSystem(Path src, boolean verifyChecksum)throws IOException {
		FileSystem srcFs = src.getFileSystem(getConf());
		srcFs.setVerifyChecksum(verifyChecksum);
		return srcFs;
	}

	/**
	 * Accumulate exceptions if there is any. Throw them at last.
	 */
	private abstract class DelayedExceptionThrowing {
		abstract void process(Path p, FileSystem srcFs) throws IOException;
		final void globAndProcess(Path srcPattern, FileSystem srcFs)throws IOException { List<IOException> exceptions = new ArrayList<IOException>();
			for (Path p : FileUtil.stat2Paths(srcFs.globStatus(srcPattern),	srcPattern))
				try {
					process(p, srcFs);
				} catch (IOException ioe) {
					exceptions.add(ioe);
				}

			if (!exceptions.isEmpty())
				if (exceptions.size() == 1)
					throw exceptions.get(0);
				else
					throw new IOException("Multiple IOExceptions: "	+ exceptions);
		}
	}

}
