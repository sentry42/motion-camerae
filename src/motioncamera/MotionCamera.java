package motioncamera;

import java.io.*;
import java.util.Calendar;

public class MotionCamera {
	private static final String VERSION = "0.1";
	private static String errorsLogFileName, outputFileName, warningsLogFileName = null;
	private static int colourThreshold = 0, sizeThreshold = 0;
	private static PrintWriter errorWriter, outputWriter, warningWriter;
	private static boolean debug = false;
	
	private static final boolean PLATFORM_IS_WINDOWS = false;
	private static final String IMAGE_1_FILENAME;
	private static final String IMAGE_2_FILENAME;
	private static String outputFilename = ".log";
	private static String warningsFilename = ".warn";
	private static String errorsFilename = ".err";
	private static String diffImageFilename = "diffImage.jpg";
	private static String motionCameraOutputFilename = "MotionCamera_"+outputFilename;
	private static String motionCameraWarningsFilename = "MotionCamera_"+warningsFilename;
	private static String motionCameraErrorsFilename = "MotionCamera_"+errorsFilename;
	private static String imageComparatorOutputFilename = "ImageComparator_"+outputFilename;
	private static String imageComparatorWarningsFilename = "ImageComparator_"+warningsFilename;
	private static String imageComparatorErrorsFilename = "ImageComparator_"+errorsFilename;
	static
	{
		if(!PLATFORM_IS_WINDOWS)
		{
			IMAGE_1_FILENAME = "image1.jpg";
			IMAGE_2_FILENAME = "image2.jpg";
			diffImageFilename = "/home/pi/motion_capture/" + diffImageFilename;
			motionCameraOutputFilename = "/home/pi/motion_capture/" + motionCameraOutputFilename;
			motionCameraWarningsFilename = "/home/pi/motion_capture/" + motionCameraWarningsFilename;
			motionCameraErrorsFilename = "/home/pi/motion_capture/" + motionCameraErrorsFilename;
			imageComparatorOutputFilename = "/home/pi/motion_capture/" + imageComparatorOutputFilename;
			imageComparatorWarningsFilename = "/home/pi/motion_capture/" + imageComparatorWarningsFilename;
			imageComparatorErrorsFilename = "/home/pi/motion_capture/" + imageComparatorErrorsFilename;
		}
		else
		{
			IMAGE_1_FILENAME = "C:\\Users\\Sentry\\projects\\RPI\\motion_detection\\input_images\\image1.jpg";
			IMAGE_2_FILENAME = "C:\\Users\\Sentry\\projects\\RPI\\motion_detection\\input_images\\image3.jpg";
			diffImageFilename = "c:\\junk\\motion_capture\\" + diffImageFilename;
			motionCameraOutputFilename = "c:\\junk\\motion_capture\\" + motionCameraOutputFilename;
			motionCameraWarningsFilename = "c:\\junk\\motion_capture\\" + motionCameraWarningsFilename;
			motionCameraErrorsFilename = "c:\\junk\\motion_capture\\" + motionCameraErrorsFilename;
			imageComparatorOutputFilename = "c:\\junk\\motion_capture\\" + imageComparatorOutputFilename;
			imageComparatorWarningsFilename = "c:\\junk\\motion_capture\\" + imageComparatorWarningsFilename;
			imageComparatorErrorsFilename = "c:\\junk\\motion_capture\\" + imageComparatorErrorsFilename;
		}
	}
	private static String line = null;
	public static void main (String [] args) throws Exception
	{
		parseArgs(args);
		setupWriters();


		try
		{
			if(errorsLogFileName != null)
			{
				try
				{
					File f = new File(errorsLogFileName.substring(0,errorsLogFileName.lastIndexOf('\\')));
					if(!f.exists())
					{
						f.mkdirs();
					}
					f = null;
					errorWriter = new PrintWriter(new FileWriter(new File(errorsLogFileName),true));
				}
				catch(Exception e)
				{
					errorWriter = null;
					logWarning("Error logging to "+errorsLogFileName+" - error output defaulting to syserr");
					logException(e);
				}
			}
			System.out.println("Capturing image 1");
			captureImage(IMAGE_1_FILENAME);
			while(true)
			{
				if (motionDetected())
				{
					String timeStamp = getTimeStamp();
					String workingDir;
					if (PLATFORM_IS_WINDOWS)
					{
						workingDir = "c:\\junk\\motion_capture\\"+timeStamp;
					}
					else
					{
						workingDir = "/home/pi/motion_capture/"+timeStamp;
					}
					workingDir = workingDir.replace(' ', '_').replace(':', '-');
					log("Working directory is: "+workingDir);
					File newLocation = new File(workingDir);
					if(!newLocation.exists())
					{
						log("Creating working directory: "+workingDir);
						newLocation.mkdir();
					}
					if (PLATFORM_IS_WINDOWS)
					{
						workingDir += "\\";
					}
					else
					{
						workingDir += "/";
					}
					copyDetectedMotionImages(workingDir);
					log("recording video");
					recordMotion(workingDir, timeStamp);
				}

				File image2 = new File(IMAGE_2_FILENAME);
				image2.renameTo(new File(IMAGE_1_FILENAME));
			}
		}
		finally
		{
			if (errorWriter != null)
			{
				errorWriter.close();
			}
			if (warningWriter != null)
			{
				warningWriter.close();
			}
			if (outputWriter != null)
			{
				outputWriter.close();
			}
		}
	}
	
	private static void printVersion()
	{
		System.out.println("MotionCamera v"+VERSION);
	}
	
	private static void printUsage()
	{
		System.out.println(
			"Usage: java motioncamera.MotionCamera [-eerrors_log_file] [-h] [-ooutput_file] [-tthreshold] [-wwarnings_log_file]\n"
				+ "where [options] are:\n"
				+ "\t-d or -D:\t Log debug output to output file (optional)\n"
				+ 	"\t\tdefaults to FALSE\n"
				+ "\t-e or -E:\t Log errors to file <errors_log_file> (optional)\n"
				+ 	"\t\tdefaults to sysout\n"
				+ "\t-h or -H:\t Print this message\n"
				+ "\t-o or -O:\t Output to file <output_file> (optional)\n"
				+ 	"\t\tdefaults to sysout\n"
				+ "\t-tc or -TC:\t Colour threshold value for comparison (0-256) (optional)\n"
				+ 	"\t\tdefaults to 0 - most strict\n"
				+ "\t-ts or -TS:\t Size threshold value for comparison (0-inf) (optional)\n"
				+ 	"\t\tdefaults to 0 - most strict\n"
				+ "\t-w or -W:\t Log warnings to <warnings_log_file> (optional)\n"
				+ 	"\t\tdefaults to sysout");
	}
	
	private static String getTimeStamp()
	{
		Calendar cal = Calendar.getInstance();
		String timeStamp = "";
		timeStamp += cal.get(Calendar.YEAR)+"-";
		timeStamp += cal.get(Calendar.MONTH)+"-";
		timeStamp += cal.get(Calendar.DAY_OF_MONTH)+" ";
		timeStamp += cal.get(Calendar.HOUR_OF_DAY)+":";
		timeStamp += cal.get(Calendar.MINUTE)+":";
		timeStamp += cal.get(Calendar.SECOND)+".";
		timeStamp += cal.get(Calendar.MILLISECOND);
		return timeStamp;
	}
	
	private static void recordMotion(String workingDir, String timestamp) throws Exception
	{
		if(!PLATFORM_IS_WINDOWS)
		{
			String cmd = "raspivid -o "+(workingDir+timestamp).replace(' ', '_').replace(':', '-')+".h264 -t 10000";
			runCmdAndGetOutput(cmd);
		}
		else
		{
			log("On RPI I'd now capture video and save it to "+workingDir);
		}
	}
	
	private static void copyDetectedMotionImages(String workingDir) throws Exception
	{
		runCmdAndGetOutput("cp "+IMAGE_1_FILENAME+" "+workingDir+"image1"+IMAGE_1_FILENAME.substring(IMAGE_1_FILENAME.lastIndexOf('.')));
		runCmdAndGetOutput("cp "+IMAGE_2_FILENAME+" "+workingDir+"image2"+IMAGE_2_FILENAME.substring(IMAGE_2_FILENAME.lastIndexOf('.')));
//		File image1 = new File(IMAGE_1_FILENAME);
//		image1.renameTo(new File(workingDir+"image1"+IMAGE_1_FILENAME.substring(IMAGE_1_FILENAME.lastIndexOf('.'))));
//		File image2 = new File(IMAGE_2_FILENAME);
//		image2.renameTo(new File(workingDir+"image2"+IMAGE_2_FILENAME.substring(IMAGE_2_FILENAME.lastIndexOf('.'))));
//		File diffImage = new File(diffImageFilename);
//		diffImage.renameTo(new File(workingDir+"motion"+diffImageFilename.substring(diffImageFilename.lastIndexOf('.'))));
	}
	
	private static boolean motionDetected() throws Exception
	{
		System.out.println("Capturing image 2");
		captureImage(IMAGE_2_FILENAME);
		return compareImages();
	}
	
	private static void captureImage(String filename) throws Exception
	{
		if(!PLATFORM_IS_WINDOWS)
		{
			String cmd = "raspistill -t 500 -o "+filename;
			runCmdAndGetOutput(cmd);
		}
		else
		{
			log("On RPI I'd now capture an image and save to "+filename);
		}
	}
	
	private static boolean compareImages() throws Exception
	{
		String cmd = "java imagecomparator.ImageComparator "
				+ IMAGE_1_FILENAME+" "
				+ IMAGE_2_FILENAME+" "
//				+ "-d"+diffImageFilename+" "
				+ "-w"+imageComparatorWarningsFilename.replace(' ', '_')+" "
				+ "-e"+imageComparatorErrorsFilename.replace(' ', '_')+" "
				+ "-o"+imageComparatorOutputFilename.replace(' ', '_')+" "
				+ "-tc"+colourThreshold+" "
				+ "-ts"+sizeThreshold;
		try
		{
//			line = runCmdAndGetOutput("cd");
//			System.out.println(line);
			line = runCmdAndGetOutput(cmd);
			log("ImageComparator returned: '"+line+"'");
		}
		catch(Exception e)
		{
			logException(e);
			return false;
		}
		return "false".equals(line.toLowerCase());
	}

	private static String runCmdAndGetOutput(String cmd) throws Exception
	{
		if (PLATFORM_IS_WINDOWS)
		{
			cmd = "cmd /C "+cmd;
		}
		
		log("Running command: "+cmd);
		
		final Process p = Runtime.getRuntime().exec(cmd);
		
		new Thread(new Runnable()
		{
			public void run()
			{
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

				try
				{
					line = input.readLine();
					String prevLine = null;
					while(line != null)
					{
						log("command output: "+line);
						prevLine = line;
						line = input.readLine();
					}
					line = prevLine;
					log("command output: "+line);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}).start();

		int exitCode = p.waitFor();
		if(exitCode != 0)
		{
			throw new Exception("Command returned exit code: "+exitCode);
		}
		return line;
	}
	
	private static void setupWriters()
	{
		if(warningsLogFileName != null)
		{
			try
			{
				File f = new File(warningsLogFileName.substring(0,warningsLogFileName.lastIndexOf('\\')));
				if(!f.exists())
				{
					f.mkdirs();
				}
				f = null;
				warningWriter = new PrintWriter(new FileWriter(new File(warningsLogFileName), true));
			}
			catch(Exception e)
			{
				warningWriter = null;
				logWarning("Error logging warnings to "+warningsLogFileName+" - warnings will be logged to sysout");
				logException(e);
			}
		}
		
		if(outputFileName != null)
		{
			try
			{
				File f = new File(outputFileName.substring(0,outputFileName.lastIndexOf('\\')));
				if(!f.exists())
				{
					f.mkdirs();
				}
				f = null;
				outputWriter = new PrintWriter(new FileWriter(new File(outputFileName),true));
			}
			catch(Exception e)
			{
				outputWriter = null;
				logWarning("Error logging output to "+outputFileName+" - output will be logged to sysout");
				logException(e);
			}
		}
	}

	private static void parseArgs(String[] args)
	{
		if(args.length > 0)
		{
			for(int i = 0; i < args.length; i++)
			{
				String arg = args[i];
				String lowerArg = arg.toLowerCase();
				if(lowerArg.startsWith("-d"))
				{
					debug = Boolean.parseBoolean(arg.substring(2).toLowerCase());
				}
				else if(lowerArg.startsWith("-e"))
				{
					errorsLogFileName = arg.substring(2);
				}
				else if(lowerArg.startsWith("-h"))
				{
					printVersion();
					printUsage();
					System.exit(0);
				}
				else if(lowerArg.startsWith("-o"))
				{
					outputFileName = arg.substring(2);
				}
				else if(lowerArg.startsWith("-tc"))
				{
					colourThreshold = Integer.parseInt(arg.substring("-tc".length()));
				}
				else if(lowerArg.startsWith("-ts"))
				{
					sizeThreshold = Integer.parseInt(arg.substring("-ts".length()));
				}
				else if(lowerArg.startsWith("-w"))
				{
					warningsLogFileName = arg.substring(2);
				}
				else
				{
					printVersion();
					log("Unrecognised option: "+arg);
					printUsage();
					System.exit(1);
				}
			}
		}
	}

	private static void log(String msg)
	{
		log(outputWriter, msg);
	}

	private static void logWarning(String error)
	{
		log(warningWriter, error);
	}

	private static void logError(String error)
	{
		if(errorWriter == null)
		{
			logToStream(System.err, error);
		}
		else
		{
			log(errorWriter,error);
		}
	}

	private static void logException(Exception e)
	{
		String exceptionDescription = e.getClass().getName()+": "+e.getMessage()+"\n";
		for(StackTraceElement ste : e.getStackTrace())
		{
			exceptionDescription += ste.toString()+"\n";
		}
		logError(exceptionDescription.substring(0,exceptionDescription.length()-1));
	}

	private static void log(PrintWriter pw, String stringToLog)
	{
		if(pw == null)
		{
			logToStream(System.out, stringToLog);
		}
		else
		{
			pw.println("["+getTimeStamp()+"]: "+stringToLog);
		}
	}

	private static void logToStream(PrintStream ps, String stringToLog)
	{
		ps.println("["+getTimeStamp()+"]: "+stringToLog);
	}
}
