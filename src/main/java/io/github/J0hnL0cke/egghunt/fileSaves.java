/*package io.github.J0hnL0cke.egghunt;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.util.Scanner; // Import the Scanner class to read text files

public class fileSaves {

	public static String filename = "EggHuntData.json";
	
	public static void main(String[] args) {
		
	}
	
	public static void save(Gson data) {
		
	}
	
	public static Gson load() {
		//figure out what types are needing before fixing this
		String data= new String();
		try {
		      //get a string from file
			  File myObj = new File(filename);
		      Scanner myReader = new Scanner(myObj);
		      while (myReader.hasNextLine()) {
		        data.concat(myReader.nextLine());
		      }
		      myReader.close();
		      //convert string to ????
		      String res= new String();
		      Gson gson = new Gson();
		      res=gson.toString();
		    } catch (FileNotFoundException e) {
		    	//return ??? if file doesn't exist
		    	return new Gson();
		    }
		return null;
		
	}
}

*/