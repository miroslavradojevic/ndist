// imagej macro for running the 
// checks current folder for .swc files (or any other extension) and apply the set of parameters  
// command example:
// java -Xmx4g -jar ~/ImageJ/ij.jar -ijpath ~/ImageJ/plugins/ -batch run_phd.ijm parent_dir
// java -Xmx8g -jar ~/ImageJ/ij.jar -ijpath ~/ImageJ/plugins/ -batch run_phd_dir.ijm $d
// print("-------------------------------------------------------------------");
// sigmas 		= "3,5";
// th 			= "0.01,0.02,0.03,0.04,0.05,0.06";
// no 			= "20"; 
// ro 			= "10";
// ni 			= "10";
// step 		= "3"; 
// kappa 		= "3";
// ps          = "0.95";
// pd 			= "0.95";
// krad    	= "4";
// kc			= "50";
// maxiter  	= "200";
// maxepoch 	= "50"; // each 10 export tree (hardcoded)

main_folder = getArgument;

if (main_folder=="") exit ("Need argument: folder with .swc files to process.");
if (!File.isDirectory(main_folder)) exit("Argument is not a folder!");

// add "/" to the end if it was missing in the argument
if (!endsWith(main_folder, "/")) {
	main_folder = main_folder + "/";
}

setBatchMode(true);
t_start = getTime();

// list all files and detect with parameter grid
files = listFiles(main_folder);

cnt = 0;
for (i=0; i<files.length; i++ ) {
if (endsWith(files[i], ".swc")) { // "[0-9][0-9][0-9].zip" matches(files[i], "*.zip")

	// print(arg);

	// t1 = getTime();
	// run("PHD", arg);
	// t2 = getTime();

	cnt = cnt + 1;
	print("\n" + main_folder+files[i] + "\n    -> " + ((t2-t1)/60000) + " min.");

}}

t_end = getTime();
print("DONE. elapsed " + ((t_end-t_start)/60000) + " min.");

// go through all *.zip files in subfolders of some_dir
listFiles(main_folder);

function listFiles(dir) {
    list = getFileList(dir);
    for (i=0; i<list.length; i++) {
        if (endsWith(list[i], "/")){
           listFiles(""+dir+list[i]);
       	}
       	else {
       		if (endsWith(list[i],".zip")) {
				    // print(dir+list[i]);
				    open(dir+list[i]);
              Stack.getDimensions(width, height, channels, slices, frames)
              print("1,"+(width*height*slices/1000000) + "," + getTitle() + "," + width + "," + height + "," + slices);
        		run("Close All");
       		}
        }
    }
}
