package imagecomparator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Calendar;
//import java.util.Hashtable;

import javax.imageio.ImageIO;

public class ImageComparator {
	
	private static final String VERSION = "0.0";
	private static String diffImageFileName, errorsLogFileName, outputFileName, warningsLogFileName = null;
	private static String diffImageFileFormat;
	private static int colourThreshold = 0, sizeThreshold = 0;
	private static boolean highContrastDiffImage = false;
	private static PrintWriter errorWriter, outputWriter, warningWriter;
	private static boolean debug = false;
	
	private String input1Name, input2Name;
	private File inputFile1, inputFile2, diffImageFile;
	private BufferedImage image1, image2;
//	private BlobPixel[][] blobPixelArr;
//	private Hashtable<BlobPixel, String> blobNames;
//	private Hashtable<String, Integer> blobSizes;
	
	public ImageComparator(String input1, String input2) throws IOException
	{
		this.input1Name = input1;
		this.input2Name = input2;
		
		long time1 = System.currentTimeMillis();
		inputFile1 = new File(input1Name);
		image1 = ImageIO.read(inputFile1);
		long time2 = System.currentTimeMillis();
		
		debug("Time to load file 1: "+(time2-time1)+"ms");
		
		time1 = System.currentTimeMillis();
		inputFile2 = new File(input2Name);
		image2 = ImageIO.read(inputFile2);
		time2 = System.currentTimeMillis();
		
		debug("Time to load file 2: "+(time2-time1)+"ms");
		
		if(diffImageFileName != null)
		{
			diffImageFileFormat = "jpg";//input1Name.substring(input1Name.lastIndexOf('.')+1);

			if(diffImageFileName.lastIndexOf('\\') != -1)
			{
				diffImageFile = new File(diffImageFileName.substring(0,diffImageFileName.lastIndexOf('\\')));
				if(!diffImageFile.exists())
				{
					diffImageFile.mkdirs();
				}
			}
			diffImageFile = new File(diffImageFileName);
		}
	}
	
	public boolean compareInputs() throws IOException
	{
		long beforeCheckDimensions = System.currentTimeMillis();
		int image1Height = image1.getHeight(), image2Height = image2.getHeight();
		int image1Width = image1.getWidth(), image2Width = image2.getWidth();
		boolean imagesIdentical = true;
		int comparisonAreaHeight = image2Height, comparisonAreaWidth = image2Width;
		if (image1Height != image2Height ||
				image1Width != image2Width)
		{
			imagesIdentical = false;
			if(image1Height < image2Height)
			{
				comparisonAreaHeight = image1Height;
			}
			if(image1Width < image2Width)
			{
				comparisonAreaWidth = image1Width;
			}
			logWarning("Input images are of different sizes. The difference image will only show overlapping areas.");
		}
		long afterCheckDimensions = System.currentTimeMillis();
		debug("time taken to CheckDimensions: "+(afterCheckDimensions-beforeCheckDimensions)+"ms");
		
//		blobPixelArr = new BlobPixel[comparisonAreaWidth][comparisonAreaHeight];
//		blobNames = new Hashtable<BlobPixel, String> ();
//		blobSizes = new Hashtable<String, Integer> ();

		BufferedImage outputBuffer = null;
		if (diffImageFile == null)
		{
			log("Not creating difference image");
			long beforeCompareImages = System.currentTimeMillis();
			imagesIdentical = imagesIdentical && imageColoursIdentical(image1, image2, colourThreshold, sizeThreshold);
			long afterCompareImages = System.currentTimeMillis();
			debug("time taken to CompareImages: "+(afterCompareImages-beforeCompareImages)+"ms");
			return imagesIdentical;
		}
		else
		{
			log("Creating difference image");
			outputBuffer = new BufferedImage(comparisonAreaWidth, comparisonAreaHeight, image1.getType());
			imagesIdentical = getDiffImage(image1, image2, outputBuffer, comparisonAreaHeight, comparisonAreaWidth);
			if (!imagesIdentical && !ImageIO.write(outputBuffer, diffImageFileFormat, diffImageFile))
			{
				logWarning("Unable to save diff image of type "+diffImageFileFormat+". Consult error log for possible explanations.");
			}
		}
		return imagesIdentical;
	}
	
	private boolean imageColoursIdentical(BufferedImage image1, BufferedImage image2, int colourThreshold, int sizeThreshold)
	{
		for(int r = 0; r < image1.getHeight(); r+=5)
		{
			int currentSize = 0;
			int dC = 1;
			for(int c = 0; c < image1.getWidth(); c+=5)
			{
				Color c1 = new Color(image1.getRGB(c, r));
				Color c2 = new Color(image2.getRGB(c, r));
				if(Math.abs(c1.getRed() - c2.getRed()) > colourThreshold ||
					Math.abs(c1.getGreen() - c2.getGreen()) > colourThreshold ||
					Math.abs(c1.getBlue() - c2.getBlue()) > colourThreshold)
				{
					currentSize++;
				}
				else
				{
					currentSize = 0;
				}
				
				if(currentSize > sizeThreshold)
				{
					log("(c, r): "+c+","+r+" - "+(c1.getRed() - c2.getRed())+";"+(c1.getBlue() - c2.getBlue())+":"+(c1.getGreen() - c2.getGreen()));
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean imageColoursIdentical(int [] image1Colours, int [] image2Colours, int threshold)
	{
		for(int i = 0; i < image1Colours.length; i+=250)
		{
			Color c1 = new Color(image1Colours[i]);
			Color c2 = new Color(image2Colours[i]);
			if(Math.abs(c1.getRed() - c2.getRed()) > threshold ||
				Math.abs(c1.getGreen() - c2.getGreen()) > threshold ||
				Math.abs(c1.getBlue() - c2.getBlue()) > threshold)
			{
				return false;
			}
		}
		return true;
	}
	
	private boolean getDiffImage(BufferedImage image1, BufferedImage image2, BufferedImage diffImage, int comparisonHeight, int comparisonWidth)
	{
		boolean imagesIdentical = true;
		for(int r = 0; r < comparisonHeight; r+=5)
		{
			int dC = 1;
			for(int c = 0; c < comparisonWidth; c+=5)
			{
				Color c1 = new Color(image1.getRGB(c, r));
				Color c2 = new Color(image2.getRGB(c, r));
				int redDiff = Math.abs(c1.getRed() - c2.getRed());
				int greenDiff = Math.abs(c1.getGreen() - c2.getGreen());
				int blueDiff = Math.abs(c1.getBlue() - c2.getBlue());
				if(colourDiffAboveThreshold(redDiff, greenDiff, blueDiff))
				{
//					log("diff threshold reached: "+r+":"+c);
					if(highContrastDiffImage)
					{
						debug("highContrastImage: true");
						redDiff = 255;//redDiff > (256-colourThreshold)/2 ? 255 : (256-colourThreshold)/2;
						greenDiff = 255;//greenDiff > (256-colourThreshold)/2 ? 255 : (256-colourThreshold)/2;
						blueDiff = 255;//blueDiff > (256-colourThreshold)/2 ? 255 : (256-colourThreshold)/2;
						debug("setting pixel to ("+redDiff+","+greenDiff+","+blueDiff+")");
					}
					debug("(r,c): ("+r+","+c+")");
					diffImage.setRGB(c, r, new Color(redDiff,greenDiff,blueDiff).getRGB());
//					BlobPixel top = null, left = null, right = null, current;
//					String blobName = ""+r+":"+c;
//					int blobSize = 1;
//					if (r != 0)
//					{
//						top = blobPixelArr[r-1][c];
//						blobName = blobNames.get(top);
//						blobSize = blobSizes.get(blobName) != null ? blobSizes.get(blobName) + 1 : 1;
//					}
//					
//					if(dC > 0 && c != 0)
//					{
//						left = blobPixelArr[r][c-1];
//						if(left != null)
//						{
//							String leftName = blobNames.get(left);
//							int leftSize = blobSizes.get(leftName);
//							if(!leftName.equals(blobName))
//							{
//								//join two blobs
//								if(leftSize >= blobSize -1)
//								{
//									blobName = leftName;
//								}
//								blobSize += leftSize;
//							}
//							else
//							{
//								if(leftSize >= blobSize)
//								{
//									blobSize = leftSize + 1;
//									blobName = leftName;
//								}
//							}
//						}
//					}
//					else if(dC < 0 && c != comparisonWidth-1)
//					{
//						right = blobPixelArr[r][c+1];
//						if(right != null)
//						{
//							String rightName = blobNames.get(right);
//							int rightSize = blobSizes.get(rightName);
//							if(!rightName.equals(blobName))
//							{
//								//join two blobs
//								if(rightSize >= blobSize -1)
//								{
//									blobName = rightName;
//								}
//								blobSize += rightSize;
//							}
//							else
//							{
//								if(rightSize >= blobSize)
//								{
//									blobSize = rightSize + 1;
//									blobName = rightName;
//								}
//							}
//						}
//					}
//					current = new BlobPixel(top, right, null, left, blobSize);
//					blobNames.put(current, blobName);
//					blobSizes.put(blobName, blobSize);
					imagesIdentical = false;
				}
				else
				{
					diffImage.setRGB(c, r, new Color(0, 0, 0).getRGB());
				}
				
//				//bounce back down the next row
//				if (c == comparisonWidth-1)
//				{
//					if (r < comparisonHeight-1)
//					{
//						r++;
//						c++;
//						dC = -1;
//					}
//					else
//					{
//						break;
//					}
//				}
			}
		}
		return imagesIdentical;
	}

	private boolean colourDiffAboveThreshold(int redDiff, int greenDiff, int blueDiff) {
		return redDiff > colourThreshold ||
			greenDiff > colourThreshold ||
			blueDiff > colourThreshold;
	}
	
	private static void printVersion()
	{
		System.out.println("ImageComparator v"+VERSION);
	}
	
	private static void printUsage()
	{
		System.out.println(
			"Usage: java imagecomparator.ImageComparator input_image1 input_image2 [-ddiff_image_file] [-eerrors_log_file] [-h] [-ooutput_file] [-tthreshold] [-wwarnings_log_file]\n"
				+ "where [options] are:\n"
				+ "\t--d or --D:\t Setting to \"true\" will log debug output to output file (optional)\n"
				+ 	"\t\tdefaults to \"false\"\n"
				+ "\t-d or -D:\t Create an image file at <diff_image_file> (optional)\n"
				+ 	"\t\t not created by default\n"
				+ "\t--dh or --DH:\t Setting to \"true\" make the diff image a higher contrast image (optional)\n"
				+ 	"\t\tdefaults to \"false\"\n"
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
	
	public static void main (String [] args) throws Exception
	{
		if (args == null || args.length <2)
		{
			printVersion();
			printUsage();
			System.exit(1);
		}
		
		String input1 = args[0];
		String input2 = args[1];
		
		long beforeParseArgs = System.currentTimeMillis();
		parseArgs(args);
		long afterParseArgs = System.currentTimeMillis();
		debug("time taken to parseArgs: "+(afterParseArgs-beforeParseArgs)+"ms");

		try
		{
			long beforeSetupWriters = System.currentTimeMillis();
			setupWriters();
			if(errorsLogFileName != null)
			{
				try
				{
					File f = new File(errorsLogFileName.substring(0,errorsLogFileName.lastIndexOf(File.separator)));
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
			long afterSetupWriters = System.currentTimeMillis();
			debug("time taken to SetupWriters: "+(afterSetupWriters-beforeSetupWriters)+"ms");
			
			try
			{
//				log("test");
				actuallyDoStuff(input1, input2);
				System.exit(0);
			}
			catch(Exception e)
			{
				logException(e);
			}
		}
		finally
		{
			long beforeClosingFiles = System.currentTimeMillis();
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
			long afterClosingFiles = System.currentTimeMillis();
			debug("time taken to close files: "+(afterClosingFiles-beforeClosingFiles)+"ms");
		}
		System.exit(1);
	}
	
	private static void setupWriters()
	{
		if(warningsLogFileName != null)
		{
			try
			{
				File f = new File(warningsLogFileName.substring(0,warningsLogFileName.lastIndexOf(File.separator)));
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
				File f = new File(outputFileName.substring(0,outputFileName.lastIndexOf(File.separator)));
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
		if(args.length > 2)
		{
			for(int i = 2; i < args.length; i++)
			{
				String arg = args[i];
				String lowerArg = arg.toLowerCase();
				debug("lower case argument: "+lowerArg);

				if(lowerArg.startsWith("--d"))
				{
					debug = Boolean.parseBoolean(arg.substring(3).toLowerCase());
					log("Debug argument <"+arg.substring(3)+"> - Setting debug flag to "+debug+".");
				}
				else if(lowerArg.startsWith("--hc"))
				{
					highContrastDiffImage = true;
				}
				else if(lowerArg.startsWith("-d"))
				{
					diffImageFileName = arg.substring(2);
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

	private static void actuallyDoStuff(String input1, String input2) throws IOException
	{
		ImageComparator ic = new ImageComparator(input1, input2);
		long beforeCompareInputs = System.currentTimeMillis();
		boolean result = ic.compareInputs();
		long afterCompareInputs = System.currentTimeMillis();
		debug("time taken to compareInputs: "+(afterCompareInputs-beforeCompareInputs)+"ms");
		System.out.println(result);
	}

	private static void debug(String msg)
	{
		if(debug)
		{
			log(outputWriter, msg);
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
	
	private static String getTimeStamp()
	{
		Calendar cal = Calendar.getInstance();
		String timeStamp = "";
		timeStamp += cal.get(Calendar.YEAR)+"-";
		timeStamp += cal.get(Calendar.MONTH)+"-";
		timeStamp += cal.get(Calendar.DAY_OF_MONTH)+" ";
		timeStamp += cal.get(Calendar.HOUR_OF_DAY)+":";
		timeStamp += cal.get(Calendar.MINUTE)+":";
		timeStamp += cal.get(Calendar.SECOND);
		return timeStamp;
	}
}
