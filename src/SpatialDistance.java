import java.io.File;

/**
 * Created by miroslav on 8/23/15.
 */
public class SpatialDistance {

    public static void main(String[] args) {

//        System.out.println(args.length+" arguments:");

        if (args.length==1 && args[0].equals("help")) {
            System.out.println("java -jar pathA.swc pathB.swc dist");
        }
        else if (args.length==3) {

            String pathA = args[0];
            File fileA = new File(pathA);
            if (!fileA.exists()) return;
            if (!getFileExtension(pathA).equals("swc")) {
                System.out.println(pathA + " needs to be .swc");
                return;
            }

            String pathB = args[1];
            File fileB = new File(pathB);
            if (!fileB.exists()) return;
            if (!getFileExtension(pathB).equals("swc")) {
                System.out.println(pathB + " needs to be .swc");
                return;
            }

            float dist = Float.valueOf(args[2]);


        }
        else
            return;


        for (String s: args) {
            System.out.println(s);
        }

        ReadSWC rswc_A = new ReadSWC(rec_swc_path);
        ReadSWC rswc_B = new ReadSWC(gndtth_path);


    }

    public static String getFileExtension(String file_path)
    {
        String extension = "";

        int i = file_path.lastIndexOf('.');
        if (i >= 0) {
            extension = file_path.substring(i+1);
        }

        return extension;
    }

}
